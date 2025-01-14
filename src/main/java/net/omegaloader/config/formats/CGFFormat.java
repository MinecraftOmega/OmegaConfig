package net.omegaloader.config.formats;

import net.omegaloader.config.ConfigSpec;

public class CGFFormat implements IConfigFormat {
    @Override
    public String id() {
        return "cfg";
    }

    @Override
    public String name() {
        return "CFG";
    }

    @Override
    public void serialize(ConfigSpec spec) {

    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        return false;
    }
}
