# OmegaConfig
Greddy implementation of a Config based on annotation types and field types.
Works with TOML, JSON5 and CFG

## Purpose
designed to be easy to use, have high performance, and a huge flexibility on syntax and usage

## Ruleset for codding this library
- Builder fields works with annotations too
- Annotations have high priority over builders on a metadata set
- always work with primitives, avoid boxing/unboxing
- JIT is not your friend