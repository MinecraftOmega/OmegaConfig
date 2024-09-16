package net.omegaloader.config.core.serializer;

import net.omegaloader.config.builder.ConfigSpec;

public class TOMLSerializer implements ISerializer {
    @Override
    public String getName() {
        return "TOML";
    }

    @Override
    public String getExtension() {
        return ".toml";
    }

    @Override
    public String serialize(ConfigSpec spec) {
        return "";
    }
}
