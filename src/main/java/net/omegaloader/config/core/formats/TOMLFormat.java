package net.omegaloader.config.core.formats;

import net.omegaloader.config.ConfigSpec;

public class TOMLFormat implements IConfigFormat {
    @Override
    public String id() {
        return "toml";
    }

    @Override
    public String name() {
        return "TOML";
    }

    @Override
    public void serialize(ConfigSpec spec) {

    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        return false;
    }
}
