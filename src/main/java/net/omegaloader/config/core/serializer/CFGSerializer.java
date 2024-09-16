package net.omegaloader.config.core.serializer;

import net.omegaloader.config.builder.ConfigSpec;

public class CFGSerializer implements ISerializer {
    @Override
    public String getName() {
        return "CFG";
    }

    @Override
    public String getExtension() {
        return ".cfg";
    }

    @Override
    public String serialize(ConfigSpec spec) {
        return "";
    }
}
