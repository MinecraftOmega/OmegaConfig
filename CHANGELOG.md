# VERSION 0.6.4.beta
- TOML: make root push transparent to avoid creating a redundant root table

# VERSION 0.6.3.beta
- Fixed TOML format reader was not reading yet fields properly

# VERSION 0.6.2.beta
- Added 'registerBlocking' for config specs that requires the spec begin loaded for bootstrap (e.g. for use in static initializers)
- Fixed static spec class instances checkes wrongly if the class was final and not if the field was final
- Fixed TOML format reader not reading fields properly
- Fixed JSON5Format writer now properly writes booleans as primitives instead of strings

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