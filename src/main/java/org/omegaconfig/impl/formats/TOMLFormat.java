package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.IFormat;

import java.nio.file.Path;

public class TOMLFormat implements IFormat {
    @Override
    public String id() {
        return OmegaConfig.FORMAT_TOML;
    }

    @Override
    public boolean serialize(org.omegaconfig.ConfigSpec spec) {
        return false;
    }

    @Override
    public boolean deserialize(org.omegaconfig.ConfigSpec spec, Path path) {
        return false;
    }

    @Override
    public void release() {

    }
}
