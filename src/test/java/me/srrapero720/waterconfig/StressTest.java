package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.fields.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests hammering WaterConfig's real usage paths: rapid save/load cycles,
 * concurrent field writers, register/unload churn, huge payloads, deep nesting and
 * corrupted-file recovery. Every test asserts the library neither throws unexpectedly,
 * corrupts data nor hangs. Workloads are deliberately kept fast enough to never
 * saturate the overflow pool (the PANIC threshold must never trigger).
 */
public class StressTest {
    private static final Duration LIMIT = Duration.ofSeconds(60);

    @TempDir
    static Path tempDir;

    // VOLATILE STOP FLAG FOR BACKGROUND READER THREADS (NO ATOMICS BY PROJECT POLICY)
    private volatile boolean running;

    @BeforeAll
    static void setup() {
        WaterConfig.init();
        WaterConfig.setPath(tempDir);
    }
    @AfterAll
    static void teardown() {
        // DROP EVERY REGISTERED SPEC SO THE WORKER NEVER WRITES INTO THE DELETED TEMP DIR
        WaterConfig.unloadAll();
    }

    enum Mood { CALM, BUSY, WILD }

    // POLLS THE ASYNC LOAD FLAG — REGISTRATION RUNS ON THE SINGLE-THREAD IO POOL
    private static void waitLoaded(ConfigSpec spec) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!spec.isLoaded()) {
            assertTrue(System.currentTimeMillis() < deadline, "Spec '" + spec.name() + "' never finished loading");
            Thread.sleep(20);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  1. RAPID SEQUENTIAL SAVE/LOAD — 30 MIXED FIELDS x 200 CYCLES PER FORMAT
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void rapidSaveLoad(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_rapid_" + format, format, "", 0);
            IntField[] ints = new IntField[6];
            for (int n = 0; n < ints.length; n++) ints[n] = b.defineInt("int" + n, n).end();
            DoubleField[] doubles = new DoubleField[4];
            for (int n = 0; n < doubles.length; n++) doubles[n] = b.defineDouble("dbl" + n, n).end();
            FloatField[] floats = new FloatField[2];
            for (int n = 0; n < floats.length; n++) floats[n] = b.defineFloat("flt" + n, n).end();
            LongField[] longs = new LongField[2];
            for (int n = 0; n < longs.length; n++) longs[n] = b.defineLong("lng" + n, n).end();
            ShortField[] shorts = new ShortField[2];
            for (int n = 0; n < shorts.length; n++) shorts[n] = b.defineShort("sht" + n, (short) n).end();
            ByteField[] bytes = new ByteField[2];
            for (int n = 0; n < bytes.length; n++) bytes[n] = b.defineByte("byt" + n, (byte) n).end();
            StringField[] strs = new StringField[4];
            for (int n = 0; n < strs.length; n++) strs[n] = b.defineString("str" + n, "seed" + n).end();
            BooleanField[] flags = new BooleanField[4];
            for (int n = 0; n < flags.length; n++) flags[n] = b.defineBoolean("flag" + n, false).end();
            EnumField<Mood> mood = b.defineEnum("mood", Mood.CALM).end();
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            // RELATIVE PATHS ROUND-TRIP AS-IS: PathCodec NO LONGER FORCES ABSOLUTE PATHS
            PathField file = b.definePath("file", Path.of("data", "seed.bin")).end();
            CharField letter = b.defineChar("letter", 'a').end();
            ConfigSpec spec = b.build();

            for (int i = 0; i < 200; i++) {
                for (int n = 0; n < ints.length; n++) ints[n].set(n * 1000 + i);
                for (int n = 0; n < doubles.length; n++) doubles[n].set(n + i * 0.25);
                for (int n = 0; n < floats.length; n++) floats[n].set(n + i * 0.5f);
                for (int n = 0; n < longs.length; n++) longs[n].set(1_000_000L * (n + 1) + i);
                for (int n = 0; n < shorts.length; n++) shorts[n].set((short) (n * 100 + i % 100));
                for (int n = 0; n < bytes.length; n++) bytes[n].set((byte) ((n * 10 + i) % 100));
                for (int n = 0; n < strs.length; n++) strs[n].set("value_" + n + "_" + i);
                for (int n = 0; n < flags.length; n++) flags[n].set((n + i) % 2 == 0);
                Mood expectedMood = Mood.values()[i % 3];
                mood.set(expectedMood);
                List<Integer> expectedNums = List.of(i, i + 1, i + 2);
                nums.set(expectedNums);
                Path expectedFile = Path.of("data", "file_" + i + ".bin");
                file.set(expectedFile);
                char expectedLetter = (char) ('a' + i % 26);
                letter.set(expectedLetter);

                spec.save();
                assertTrue(spec.load(), "Config file must exist after save");

                for (int n = 0; n < ints.length; n++) assertEquals(n * 1000 + i, ints[n].getAsInt());
                for (int n = 0; n < doubles.length; n++) assertEquals(n + i * 0.25, doubles[n].getAsDouble());
                for (int n = 0; n < floats.length; n++) assertEquals(n + i * 0.5f, floats[n].getAsFloat());
                for (int n = 0; n < longs.length; n++) assertEquals(1_000_000L * (n + 1) + i, longs[n].getAsLong());
                for (int n = 0; n < shorts.length; n++) assertEquals((short) (n * 100 + i % 100), shorts[n].getAsShort());
                for (int n = 0; n < bytes.length; n++) assertEquals((byte) ((n * 10 + i) % 100), bytes[n].getAsByte());
                for (int n = 0; n < strs.length; n++) assertEquals("value_" + n + "_" + i, strs[n].get());
                for (int n = 0; n < flags.length; n++) assertEquals((n + i) % 2 == 0, flags[n].getAsBoolean());
                assertEquals(expectedMood, mood.get());
                assertEquals(expectedNums, nums.get());
                assertEquals(expectedFile, file.get());
                assertEquals(expectedLetter, letter.getAsChar());
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  2. CONCURRENT WRITERS ON DISTINCT FIELDS WHILE MAIN THREAD SAVES
    // ══════════════════════════════════════════════════════════
    @Test
    void concurrentWriters() {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_writers", WaterConfig.FORMAT_CFG, "", 0);
            IntField[] fields = new IntField[8];
            for (int t = 0; t < fields.length; t++) fields[t] = b.defineInt("w" + t, 0).end();
            ConfigSpec spec = WaterConfig.register(b.build());
            waitLoaded(spec);

            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch done = new CountDownLatch(fields.length);
            Thread[] workers = new Thread[fields.length];
            for (int t = 0; t < workers.length; t++) {
                final IntField field = fields[t];
                final int id = t;
                workers[t] = new Thread(() -> {
                    try {
                        for (int i = 0; i < 500; i++) {
                            field.set(id * 1000 + i);
                            // PACE THE WRITES SO MAIN-THREAD SAVES OVERLAP THE MUTATIONS
                            if (i % 50 == 0) Thread.sleep(1);
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                }, "stress-writer-" + t);
                workers[t].setDaemon(true);
            }
            for (Thread w : workers) w.start();

            // SAVE EVERY 50MS UNTIL EVERY WRITER FINISHES
            while (!done.await(50, TimeUnit.MILLISECONDS)) {
                try {
                    spec.save();
                } catch (Throwable e) {
                    errors.add(e);
                }
            }
            for (Thread w : workers) {
                w.join(10_000);
                assertFalse(w.isAlive(), "Writer thread hung: " + w.getName());
            }
            assertDoesNotThrow(spec::save, "Final save must not throw");

            assertTrue(errors.isEmpty(), "Unexpected exceptions: " + errors);
            assertTrue(Files.exists(spec.path()), "Config file must exist");
            for (int t = 0; t < fields.length; t++) {
                assertEquals(t * 1000 + 499, fields[t].getAsInt(), "Field w" + t + " lost its final value");
            }
            WaterConfig.unload("stress_writers");
        });
    }

    // ══════════════════════════════════════════════════════════
    //  3. 8 THREADS HAMMERING THE SAME INT FIELD
    // ══════════════════════════════════════════════════════════
    @Test
    void sameFieldHammer() {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_shared", WaterConfig.FORMAT_CFG, "", 0);
            IntField shared = b.defineInt("shared", 0).end();
            ConfigSpec spec = b.build();

            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            Thread[] writers = new Thread[8];
            for (int t = 0; t < writers.length; t++) {
                final int id = t;
                writers[t] = new Thread(() -> {
                    try {
                        for (int i = 0; i < 1000; i++) {
                            shared.set((id + 1) * 1000 + i);
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                }, "stress-shared-" + t);
                writers[t].setDaemon(true);
            }
            for (Thread w : writers) w.start();
            for (Thread w : writers) {
                w.join(10_000);
                assertFalse(w.isAlive(), "Writer thread hung: " + w.getName());
            }
            assertTrue(errors.isEmpty(), "Unexpected exceptions: " + errors);

            // EVERY WRITTEN VALUE IS (T+1)*1000+I WITH T IN [0,7] AND I IN [0,999] -> RANGE [1000, 8999]
            int last = shared.getAsInt();
            assertTrue(last >= 1000 && last <= 8999, "Final value is not one of the written values: " + last);

            spec.save();
            spec.load();
            assertEquals(last, shared.getAsInt(), "Persisted value must survive the save/load round-trip");
        });
    }

    // ══════════════════════════════════════════════════════════
    //  4. REGISTER/UNLOAD CHURN — 50 SHORT-LIVED SPECS
    // ══════════════════════════════════════════════════════════
    @Test
    void registerUnloadChurn() {
        assertTimeoutPreemptively(LIMIT, () -> {
            List<Path> files = new ArrayList<>();
            for (int r = 0; r < 50; r++) {
                String name = "stress_churn_" + r;
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder(name, WaterConfig.FORMAT_CFG, "", 0);
                IntField num = b.defineInt("num", r).end();
                StringField txt = b.defineString("txt", "seed").end();
                ConfigSpec spec = WaterConfig.register(b.build());
                waitLoaded(spec);
                num.set(r * 10);
                txt.set("mut_" + r);
                WaterConfig.unload(name);
                assertFalse(WaterConfig.isRegistered(name), "Spec must be unregistered immediately: " + name);
                files.add(spec.path());
            }

            // FINAL SAVES RUN ASYNC ON THE IO POOL — POLL UNTIL EVERY FILE EXISTS
            long deadline = System.currentTimeMillis() + 20_000;
            for (Path f : files) {
                while (!Files.exists(f)) {
                    assertTrue(System.currentTimeMillis() < deadline, "Config file was never written: " + f);
                    Thread.sleep(20);
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  5. 40 SPECS REGISTERED AT ONCE (ASYNC) ACROSS ALL FORMATS
    // ══════════════════════════════════════════════════════════
    @Test
    void manySpecs() {
        assertTimeoutPreemptively(LIMIT, () -> {
            String[] formats = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML};
            ConfigSpec[] specs = new ConfigSpec[40];
            for (int n = 0; n < specs.length; n++) {
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_many_" + n, formats[n % formats.length], "", 0);
                b.defineInt("m_int", n).end();
                b.defineString("m_txt", "spec_" + n).end();
                b.defineBoolean("m_flag", n % 2 == 0).end();
                specs[n] = WaterConfig.register(b.build());
            }

            for (ConfigSpec spec : specs) waitLoaded(spec);
            for (ConfigSpec spec : specs) {
                assertTrue(Files.exists(spec.path()), "Config file missing: " + spec.path());
                String content = Files.readString(spec.path(), StandardCharsets.UTF_8);
                assertTrue(content.contains("m_int"), "Config file unreadable or incomplete: " + spec.path());
            }
            for (ConfigSpec spec : specs) WaterConfig.unload(spec.name());
        });
    }

    // ══════════════════════════════════════════════════════════
    //  6A. 100KB STRING WITH NEWLINES, QUOTES, TABS AND UNICODE
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void bigString(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            StringBuilder sb = new StringBuilder(110_000);
            int line = 0;
            while (sb.length() < 100_000) {
                sb.append("line ").append(line++).append(": \"quoted\" unicode áéíñü★ with \\backslash\\ and\ttab\n");
            }
            String big = sb.toString();

            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_bigstr_" + format, format, "", 0);
            StringField blob = b.defineString("blob", "seed").end();
            IntField sentinel = b.defineInt("sentinel", 1).end();
            ConfigSpec spec = b.build();

            blob.set(big);
            sentinel.set(777);
            spec.save();
            spec.load();

            assertEquals(777, sentinel.getAsInt(), "Field written after the blob must survive");
            assertTrue(big.equals(blob.get()), "100KB string was corrupted by the " + format + " round-trip");
        });
    }

    // ══════════════════════════════════════════════════════════
    //  6B. LIST<STRING> WITH 10K UNICODE/QUOTED ELEMENTS
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void bigStringList(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            List<String> items = new ArrayList<>(10_000);
            for (int n = 0; n < 10_000; n++) {
                items.add("item_" + n + "_\"q\"_áñ★_" + "x".repeat(30));
            }

            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_biglist_" + format, format, "", 0);
            ListField<String> list = b.defineList("items", new ArrayList<>(List.of("seed")), String.class).end();
            ConfigSpec spec = b.build();

            list.set(items);
            spec.save();
            spec.load();

            assertEquals(10_000, list.size(), "List size must survive the round-trip");
            assertTrue(items.equals(list.get()), "10k-element list was corrupted by the " + format + " round-trip");
        });
    }

    // ══════════════════════════════════════════════════════════
    //  7. 10 NESTED GROUPS, EACH HOLDING FIELDS
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void deepNesting(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_deep_" + format, format, "", 0);
            IntField[] nums = new IntField[10];
            StringField[] txts = new StringField[10];
            for (int l = 0; l < 10; l++) {
                b.push("level" + l);
                nums[l] = b.defineInt("num" + l, l).end();
                txts[l] = b.defineString("txt" + l, "seed" + l).end();
            }
            b.pop(10);
            ConfigSpec spec = b.build();

            for (int l = 0; l < 10; l++) {
                nums[l].set(l * 7 + 1);
                txts[l].set("deep_" + l);
            }
            spec.save();
            assertTrue(spec.load(), "Config file must exist after save");

            for (int l = 0; l < 10; l++) {
                assertEquals(l * 7 + 1, nums[l].getAsInt(), "Nested int lost at depth " + l);
                assertEquals("deep_" + l, txts[l].get(), "Nested string lost at depth " + l);
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  8. RELOAD FLOOD WHILE A READER THREAD SPINS ON GET()
    // ══════════════════════════════════════════════════════════
    @Test
    void reloadFlood() {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_reload", WaterConfig.FORMAT_CFG, "", 0);
            IntField num = b.defineInt("num", 0).end();
            StringField txt = b.defineString("txt", "start").end();
            ConfigSpec spec = b.build();

            num.set(1);
            txt.set("alpha");
            spec.save();

            List<String> violations = Collections.synchronizedList(new ArrayList<>());
            running = true;
            Thread reader = new Thread(() -> {
                try {
                    while (running) {
                        Integer v = num.get();
                        String s = txt.get();
                        // OBSERVED VALUES MUST ALWAYS BE ONE OF THE TWO WRITTEN STATES, NEVER NULL
                        if (v == null || (v != 1 && v != 2)) violations.add("num=" + v);
                        if (s == null || (!s.equals("alpha") && !s.equals("beta"))) violations.add("txt=" + s);
                    }
                } catch (Throwable e) {
                    violations.add("reader threw: " + e);
                }
            }, "stress-reload-reader");
            reader.setDaemon(true);
            reader.start();

            for (int i = 0; i < 100; i++) {
                boolean even = i % 2 == 0;
                num.set(even ? 2 : 1);
                txt.set(even ? "beta" : "alpha");
                spec.save();
                spec.setReload(true);
                assertTrue(spec.load(), "File must remain loadable during the flood");
                assertFalse(spec.isReload(), "load() must clear the reload flag");
            }

            running = false;
            reader.join(5_000);
            assertFalse(reader.isAlive(), "Reader thread hung");
            assertTrue(violations.isEmpty(), "Corrupt values observed: " + violations);

            // LAST ITERATION IS i=99 (ODD) -> 1 / "alpha"
            assertEquals(1, num.getAsInt());
            assertEquals("alpha", txt.get());
        });
    }

    // ══════════════════════════════════════════════════════════
    //  9. LOAD OF TRUNCATED/GARBAGE/EMPTY FILES — RECOVERY BEHAVIOR MAP
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void garbageLoad(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("stress_garbage_" + format, format, "", 0);
            IntField num = b.defineInt("num", 5).end();
            StringField txt = b.defineString("txt", "fallback").end();
            BooleanField flag = b.defineBoolean("flag", true).end();
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(1, 2)), Integer.class).end();
            ConfigSpec spec = b.build();

            num.set(42);
            txt.set("persisted");
            flag.set(false);
            nums.set(List.of(7, 8, 9));
            spec.save();
            String valid = Files.readString(spec.path(), StandardCharsets.UTF_8);

            String[] garbage = {
                    valid.substring(0, valid.length() / 2),  // TRUNCATED FILE
                    "%%%### not a config {{{ \"broken",      // STRUCTURAL GARBAGE
                    ""                                        // EMPTY FILE
            };
            StringBuilder behavior = new StringBuilder("[garbage-load] " + format + ":");
            for (String content : garbage) {
                Files.writeString(spec.path(), content, StandardCharsets.UTF_8);
                try {
                    spec.load();
                    behavior.append(" recovered;");
                } catch (Exception e) {
                    // A CLEAN FAILURE IS ACCEPTABLE FOR NOW — ERRORS AND HANGS ARE NOT
                    assertTrue(e instanceof IOException || e instanceof RuntimeException,
                            "Unexpected exception kind: " + e.getClass().getName());
                    behavior.append(" ").append(e.getClass().getSimpleName()).append(";");
                }

                // FIELDS MUST NEVER BE LEFT NULL, NO MATTER HOW BROKEN THE FILE IS
                assertNotNull(num.get(), "int field left null after garbage load");
                assertNotNull(txt.get(), "string field left null after garbage load");
                assertNotNull(flag.get(), "boolean field left null after garbage load");
                assertNotNull(nums.get(), "list field left null after garbage load");
                for (Integer element : nums.get()) assertNotNull(element, "list element left null after garbage load");
            }

            // RESTORING A VALID FILE MUST FULLY RECOVER THE PERSISTED STATE
            Files.writeString(spec.path(), valid, StandardCharsets.UTF_8);
            spec.load();
            assertEquals(42, num.getAsInt());
            assertEquals("persisted", txt.get());
            assertFalse(flag.getAsBoolean());
            assertEquals(List.of(7, 8, 9), nums.get());
            System.out.println(behavior);
        });
    }
}
