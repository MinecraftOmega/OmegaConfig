# HARD lockdown — deferred design

SOFT lockdown is implemented. HARD is not. This file captures the proposal, the required
change, and what must move, so it is not lost.

## Where it plugs in (already built for SOFT)

- `WaterConfigConfig.lockdownConfigFiles` — enum `Lockdown { OFF, I_READ_THE_COMMENT_SOFT, I_READ_THE_COMMENT_HARD }`
  (verbose values on purpose: enabling lockdown must be a deliberate, informed choice).
- `WaterConfig.applyLockdown(Lockdown)` sets the global `LOCKDOWN`; `WaterConfig.isLockdown()` is true for SOFT or HARD.
- Bootstrap is lazy: `WaterConfig.ensureSelfConfig()` runs on the first `register(...)`, registers
  `WaterConfigConfig` (TOML at `<configPath>/waterconfig.toml`) and applies the lockdown.
- **SOFT today** = `ConfigSpec.setReload(true)` is ignored while locked, so runtime reload is disabled;
  external edits are never read and get overwritten on the next save.
- **HARD today** = falls back to SOFT and logs a warning (`applyLockdown` handles `I_READ_THE_COMMENT_HARD`).

## What HARD must add

An OS-level exclusive lock (`FileChannel.lock`) held per managed file for the spec's lifetime, so an
**external program** cannot write the file. Windows enforces it (mandatory locks); Unix is advisory
(a program that ignores the lock can still write — document this).

## The blocker: you cannot hold an exclusive lock AND open a second write handle

On Windows the lock is enforced against every handle, including our own. Today each save opens its
own stream via `Files.newBufferedWriter(path)` (the format writers own their file). Holding an
exclusive lock on channel A while `save()` opens channel B to the same path **fails on Windows**.

So HARD requires writing **through the locked channel**. That means a format-writer contract change.

## What must move / change

1. **`IFormatWriter` / `IFormatCodec.createWriter`** — let a writer target a provided sink instead of
   opening its own file by `Path`. Two options:
    - Add `createWriter(OutputStream)` (or `WritableByteChannel`) alongside `createWriter(Path)`, OR
    - Have writers build their content in memory (they already build a `StringBuilder` and flush once
      at `close()`) and expose the bytes so `ConfigSpec` writes them through the locked channel.
      Prefer the second (smaller surface): writers already buffer everything; only `close()`'s
      `Files.newBufferedWriter` call is the file coupling.
2. **`ConfigSpec`** — when `LOCKDOWN == HARD`:
    - Open a long-lived `FileChannel` (READ+WRITE) per file, `tryLock()` it, keep it open.
    - Route `save()` through that channel: truncate + write the format's produced bytes.
    - Route `load()` reads through the same channel (or a shared-lock read).
    - On lock-acquisition failure (another process holds it) → `failSpec(...)` / log.
3. **Lock lifecycle** — release the lock + close the channel on `unload(name)`, `shutdown()`, and
   `triggerPanic()`. Track channels alongside `LOOP_SPECS` (or on the spec). Never leak a lock.
4. **`WaterConfigConfig` is EXEMPT from the HARD lock** — otherwise you could not edit
   `waterconfig.toml` to turn lockdown back off. Skip locking for the self-config spec.
5. **reload stays disabled** under HARD (same guard as SOFT) in addition to the OS lock.
6. `applyLockdown`: wire `I_READ_THE_COMMENT_HARD` to the real OS-lock path and drop the fallback
   warning.

## Platform note

`FileChannel.lock` is the most the JVM gives without native tricks: mandatory on Windows, advisory on
Unix. Acceptable — the modpack author's intent (block hot edits for stability/security) is met on
Windows and best-effort on Unix.