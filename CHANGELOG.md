# VERSION 0.6.1.beta
- Fixed `triggerPanic()` no longer attempts save when I/O is stuck — only logs lost data and shuts down pools
- Fixed `doProcess()` dirty/reload flags now cleared AFTER successful I/O instead of before
- Fixed `doProcess()` save and reload now have separate try/catch — a save failure no longer blocks reload
- Fixed `unload()` now waits for overflow to release the spec before doing the final save
- Fixed `checkPanic()` now runs every tick in the main loop instead of only when a slow spec needs work
- Made `WaterConfig.init()` public and idempotent — no longer auto-invoked via static initializer

# VERSION 0.6.0.beta
- Refactored worker thread into a 3-phase pipeline: Registry (IO_POOL) → Loop (RT_WORKER) → Unload
- Added overflow/panic system for slow config specs (5s overflow threshold, 10s panic)
- Fixed dirty/reload race condition: flags are now cleared before I/O, re-set on failure
- Fixed resource leak in ConfigSpec.load()/save() using try-with-resources
- Fixed JSON5Format comment parser: `//` line comments, `/* */` block comments now work correctly
- Fixed JSON5Format parser skipping characters due to `data[i++]` bug
- Fixed JSON5Format `/` inside quoted strings no longer triggers comment mode
- Fixed JSON5Format block comment end detection (`*/`) which was completely missing
- Fixed JSON5Format writer isString logic for scalar and array values
- Fixed ArrayField.validate() truncation incorrectly applying distinct()
- Fixed StringField.validate() NPE when value is null
- Fixed FloatField.getAsFloat() returning double instead of float
- Removed dead putValue() method from JSON5Format reader
- Added comment test cases for JSON5 (line, block, mixed, slashes in strings)
- Added comment test cases for CFG, TOML, and Properties formats

# VERSION 0.5.0.beta
- Added TOML format
- Added CFG format
- Added nested spec support
- fixed boolean primitives aren't never initialized
- added proper JUNIT test cases for formats and specs
- added math operation support 

# VERSION 0.1.0.alpha
- Initial release of the project.