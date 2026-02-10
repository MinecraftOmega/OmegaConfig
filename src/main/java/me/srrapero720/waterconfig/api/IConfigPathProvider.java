package me.srrapero720.waterconfig.api;

import me.srrapero720.waterconfig.ConfigSpec;

public interface IConfigPathProvider {

    /**
     * Get the provider name
     *
     * @return provider name
     */
    String name();

    /**
     * Checks if the provider is valid for the given spec.
     *
     * @return true if the provider is valid for the given spec, saving the file using the provided path,
     * false otherwise
     */
    boolean validSpec(ConfigSpec spec);

    /**
     * Get the path of the config file
     *
     * @return path of the config file
     */
    String getPath();
}
