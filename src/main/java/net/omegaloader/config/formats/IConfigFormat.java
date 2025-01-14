package net.omegaloader.config.formats;

import net.omegaloader.config.ConfigSpec;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public interface IConfigFormat {
    String JSON = "json";
    String JSON5 = "json5";
    String PROPERTIES = "properties";
    String TOML = "toml";
    String CONFIG = "cfg";

    static IConfigFormat getByName(String name) {
        // TODO
    }

    /**
     * Identifier of the current IConfigFormat
     */
    String id();

    /**
     * Name of the current IConfigFormat
     * formerly used in places where requires a moore "styled" typing
     */
    String name();

    /**
     * File extension of the current IConfigFormat
     * @return by default, returns the ID with a dot at the start
     */
    default String extension() {
        return "." + id();
    }

    /**
     * Converts spec into the current format
     * @param spec locked spec
     */
    void serialize(ConfigSpec spec); // convert into format file

    /**
     * Updates the current spec using the existing file (if exists)
     * @param spec locked spec
     */
    boolean deserialize(ConfigSpec spec); // just updates config spec

    default RandomAccessFile generateFile(ConfigSpec spec) {
        try {
            return new RandomAccessFile(spec.name() + (spec.suffix != null ? "-" + spec.suffix : "") + extension(), "rws"); // Read and write sync
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
