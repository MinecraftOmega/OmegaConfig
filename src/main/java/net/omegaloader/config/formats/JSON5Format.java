package net.omegaloader.config.formats;

import net.omegaloader.config.ConfigSpec;

public class JSON5Format implements IConfigFormat {
    @Override
    public String id() {
        return "json5";
    }

    @Override
    public String name() {
        return "JSON5";
    }

    @Override
    public void serialize(ConfigSpec spec) {

    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        return false;
    }
}
