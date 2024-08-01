package net.omegaloader.config.builder;

import net.omegaloader.config.builder.field.BaseConfigField;

import java.util.HashMap;
import java.util.Map;

public class ConfigSpec {
    private String filename;
    private Format format;
    private final Map<String, BaseConfigField<?>> fields = new HashMap<>();

    ConfigSpec(String filename, Format format, Class<?> context) {

    }

    public void save() {

    }

    public void refresh() {
        fields.values().forEach(BaseConfigField::refresh);
    }

    public static class Builder {

    }

    public enum Format {
        JSON, JSON5, TOML, CFG
    }
}