package net.omegaloader.config.core.deserializer;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import net.omegaloader.config.builder.ConfigSpec;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PROPDeserializer implements IDeserializer {
    public static final String EXTENSION = ".properties";
    public static final char KEY_VAR_SPLIT = '=';
    public static final char GROUP_SPLIT = '.';
    public static final char LINE_SPLIT = '\n';


    @Override
    public void deserialize(ConfigSpec spec, InputStream inputStream) {
        final Map<String, String> values = new HashMap<>();
        String parent = "";
        inputStream.readLine();
    }

    @Override
    public String serialize(ConfigSpec spec) {

    }

    @Override
    public void create2line() {

    }
}
