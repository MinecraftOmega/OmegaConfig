package net.omegaloader.config.core.serializer;

import net.omegaloader.config.builder.ConfigSpec;

public interface ISerializer {
    String getName();
    String getExtension();

    String serialize(ConfigSpec spec);
}
