# 📦 UPDATE 1.0.0.2 - BETA
- ✨Implemented `Spec#old` and `SpecBuilder#old(String)`: when the current-format file is missing.
  - The same spec file is searched in the previous format, its settings recovered, validated and migrated into the current format.
  - The old file is deleted once the migration is persisted, and only ignored when the current-format file already exists.
- 🛠 Field aliases now also resolve from the file root, so restructured specs recover values from flat old files

# 📦 UPDATE 1.0.0.1 - BETA
- ✨Added codecs: `BigDecimal`, `BigInteger`, `java.nio.charset.Charset`, `java.awt.Color`, `PatternCodec`, `URLCodec`
- ✨Added ``java.time`` codecs: `Duration`, `Instant`, `LocalDate`, `LocalDateTime`, `Locale`, `OffsetDateTime`, `Period`, `ZoneDateTime`, `ZoneId`
- ✨Added support for records as a Codec (params must have codecs)
- ✨Config repair implemented: tries to fix the config file and recover all settings much as possible when any value is broken, also handles when chars are missing or illegal values are introduced
- ✨Added `ConfigSpec#refresh()`: refreshes specs when using reflection mode
- ✨Added `WaterConfig.specs()`, `WaterConfig.specs(boolean refresh)` to get all specs
- ✨Reverse Specs: Added ``WaterConfig.reverseSpec(Path)`` to create a spec from a file and `WaterConfig.specOf` to lookup
- 🛠 Codecs now recover from malformed inputs instead or throw an exception
- 🛠 Format codec JSON and JSON5 is abstracted in JSONX
- 🐛 Fixed string escaping
- 🐛 Fixed NaN handling on all format codecs