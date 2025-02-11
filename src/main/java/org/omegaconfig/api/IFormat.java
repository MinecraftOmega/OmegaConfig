package org.omegaconfig.api;

import org.omegaconfig.ConfigSpec;

import java.nio.file.Path;

public interface IFormat {

    /**
     * Format identifier
     */
    String id();

    /**
     * Format extension
     */
    default String extension() {
        return "." + id();
    }

    /**
     * Converts spec into the current format
     * @param spec locked spec
     */
    boolean serialize(ConfigSpec spec);

    /**
     * Updates the current spec using the existing file (if exists)
     * @param spec locked spec
     */
    boolean deserialize(ConfigSpec spec, Path path);

    void release();
}
