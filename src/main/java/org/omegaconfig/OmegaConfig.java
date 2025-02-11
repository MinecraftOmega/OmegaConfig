package org.omegaconfig;

import org.omegaconfig.api.ICodec;
import org.omegaconfig.api.IComplexCodec;
import org.omegaconfig.api.IFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static org.omegaconfig.Tools.toBoxed;

public class OmegaConfig {
    public static final String FORMAT_PROPERTIES = "properties";
    public static final String FORMAT_CFG = "cfg";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_JSON5 = "json5";
    public static final String FORMAT_TOML = "toml";

    private static final Map<Class<?>, ICodec<?>> CODECS = new HashMap<>();
    private static final Map<String, IFormat> FORMATS = new HashMap<>();
    private static final Map<String, ConfigSpec> SPECS = new HashMap<>();
    private static final Thread WORKER = new Thread(OmegaConfig::run);

    static {
        for (ICodec<?> c: ServiceLoader.load(ICodec.class))
            CODECS.put(c.type(), c);

        for (IFormat f: ServiceLoader.load(IFormat.class))
            FORMATS.put(f.id(), f);

        WORKER.setName("OmegaConfig-Worker-0");
        WORKER.setDaemon(true);
        WORKER.setPriority(3);
        WORKER.start();
    }

    public static void register(ConfigSpec spec) {
        synchronized (SPECS) {
            SPECS.put(spec.name(), spec);
        }
    }

    private static void run() {
        while (!WORKER.isInterrupted()) {
            synchronized (SPECS) {
                for (ConfigSpec spec: SPECS.values()) {
                    FORMATS.get(spec.format()).deserialize(spec, spec.path());
                }
            }
        }
    }

    public static <T, T2> T tryParse(String value, Class<T> type, Class<T2> type2) {
        var codec = CODECS.get(toBoxed(type));
        if (codec instanceof IComplexCodec<?, T2> complexCodec) {
            return (T) complexCodec.decode(value, type2);
        }
        return (T) codec.decode(value);
    }
}
