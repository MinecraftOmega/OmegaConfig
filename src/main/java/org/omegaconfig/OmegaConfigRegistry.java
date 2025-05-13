package org.omegaconfig;

import org.omegaconfig.api.ICodec;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class OmegaConfigRegistry {
    // FORMATS AND CODECS
    static final Map<Class<?>, ICodec<?>> CODECS = new HashMap<>();
    static final Map<String, IFormatCodec> FORMATS = new HashMap<>();

    // SPECS
    static final Map<String, ConfigSpec> SPECS = new HashMap<>();

    // PATH; TODO: should be implemented as a service?
    static Path CONFIG_PATH = new File("config").toPath();

    static void init() {
        for (IFormatCodec f: ServiceLoader.load(IFormatCodec.class))
            FORMATS.put(f.id(), f);

        for (ICodec<?> c: ServiceLoader.load(ICodec.class))
            CODECS.put(c.type(), c);
    }
}
