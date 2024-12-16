package net.omegaloader.config.core;

public enum Format {
    JSON("JSON", "json"),
    JSON5("JSON5", "json5"),
    TOML("TOML", ".toml"),
    CFG("CFG", ".cfg"),
    PROP("PROPERTIES", ".properties"),
    ;

    private final String name;
    private final String extension;
    Format(String name, String ext) {
        this.name = name;
        this.extension = ext;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }
}