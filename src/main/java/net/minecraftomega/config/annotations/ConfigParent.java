package net.minecraftomega.config.annotations;

/**
 * Defines the base class of a config class
 */
public @interface ConfigParent {
    /**
     * Config identifier
     * Outside any "modding like" id system, this accepts any type of symbol,
     * but the appropriate way to define your config ids
     * is using your project identifier (<code>myprojectid</code>)
     * and a suffix about for what is your config like <code>-rendering</code>
     * result will look like <code>myprojectid-rendering.toml</code>
     * @return config container id
     */
    String value();
}
