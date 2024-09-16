package net.omegaloader.config.core.serializer;

import net.omegaloader.config.builder.ConfigSpec;

public class JSON5Serializer implements ISerializer {

    public JSON5Serializer() {

    }

    @Override
    public String getName() {
        return "JSON5";
    }

    @Override
    public String getExtension() {
        return ".json5";
    }

    @Override
    public String serialize(ConfigSpec spec) {
        return "";
    }

    public static class Schema implements ISerializer {

        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getExtension() {
            return "";
        }

        @Override
        public String serialize(ConfigSpec spec) {
            return "";
        }
    }
}
