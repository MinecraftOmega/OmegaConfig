package net.omegaloader.config.core.formats;

import net.omegaloader.config.ConfigSpec;

public class JSONFormat implements IConfigFormat {
    @Override
    public String id() {
        return "json";
    }

    @Override
    public String name() {
        return "JSON";
    }

    @Override
    public void serialize(ConfigSpec spec) {

    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        return false;
    }
}
