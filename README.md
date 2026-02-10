# WaterConfig
Greddy implementation of a Config based on annotation types and field types.
Works with TOML, JSON5 and CFG

## Purpose
designed to be easy to use, have high performance, and a huge flexibility on syntax and usage
this is a independent library.

## Ideas
- Replace reflection usage with ASM
- Usage of UNSAFE to get/put values
- Async reading and storing

## Planned
- Add an event firing for field updates
- Serializer/Deserializer registration
- Custom Field registration
- ConfigFixers registration (fixes old spec fields into the new spec)

## Basics on coding this library
- Spec can be defined using annotations or spec builder + fields.
- Builder fields can work with annotations to define extra metadata
- Annotations have high priority over builders on metadata
- always work with primitives, avoid boxing/unboxing
- Never trust on JIT (sometimes it might not help you)