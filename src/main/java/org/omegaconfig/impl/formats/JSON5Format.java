package org.omegaconfig.impl.formats;

import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.IFormat;

import java.nio.file.Path;

public class JSON5Format implements IFormat {
    @Override
    public String id() {
        return OmegaConfig.FORMAT_JSON5;
    }

    @Override
    public boolean serialize(ConfigSpec spec) {
        return false;
    }

    @Override
    public boolean deserialize(ConfigSpec spec, Path path) {
        return false;
    }

    @Override
    public void release() {

    }

}
