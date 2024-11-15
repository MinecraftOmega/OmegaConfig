package net.omegaloader.config;

import net.omegaloader.config.annotations.field.Config;
import net.omegaloader.config.annotations.field.ConfigField;
import net.omegaloader.config.builder.ConfigFileFormat;
import net.omegaloader.config.builder.ConfigSpec;
import net.omegaloader.config.builder.ConfigSpecBuilder;
import net.omegaloader.config.builder.field.*;
import net.omegaloader.config.core.InvalidFieldException;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class OmegaConfig {
    public static final Pattern PATTERN = Pattern.compile("^[a-zA-Z][a-z1-9_]*$");
    public static final Pattern DOT = Pattern.compile("\\.");

    /**
     * Register your config class or instance to OmegaConfig deserializers.
     *
     * <p>Register means OmegaConfig locks the fields and runs serializers to read an update field values</p>
     *
     * @param container config container of the fields which can be a class (staticContext = true) or an instance
     * @param staticContext anti-"i forgot to make my fields static" barrier.
     */
    public static void register(Object container, boolean staticContext) { // assumming is annotation-based
        // if the container is a class then we are running on static context
        Class<?> containerClass = container instanceof Class<?> c ? c : container.getClass();

        // validate container info
        Config config = containerClass.getAnnotation(Config.class);
        if (config == null)
            throw new IllegalArgumentException("Container class must be annotated with Config.class");

        int backups = config.backups();
        if (backups < 0 || backups > 10)
            throw new IllegalArgumentException("Backup number must be between 0 and 10");

        String identifier = config.value();
        if (!PATTERN.matcher(identifier).find())
            throw new IllegalArgumentException("Config ID must follow the next pattern: " + PATTERN);

        ConfigFileFormat format = config.format();
        if (format == null)
            throw new IllegalArgumentException("Format must not be null");

        // Start building
        ConfigSpecBuilder builder = new ConfigSpecBuilder(identifier, format);

        for (Field f: containerClass.getDeclaredFields()) {
            // Validate field info (skip not annotated fields)
            ConfigField configField = f.getAnnotation(ConfigField.class);
            if (configField == null) continue;

            String fieldName = configField.value();
            if (fieldName == null || fieldName.isEmpty())
                fieldName = f.getName();

            // run declaration
            Class<?> type = f.getType();
            if (type.isAssignableFrom(Number.class)) // swap
                type = Util.swapNumberClass((Class<? extends Number>) type);

            if (type.equals(byte.class)) {
                builder.defineByte(fieldName, f);
            } else if (type.equals(short.class)) {
                builder.defineShort(fieldName, f);
            } else if (type.equals(int.class)) {
                builder.defineInt(fieldName, f);
            } else if (type.equals(long.class)) {
                builder.defineLong(fieldName, f);
            } else if (type.equals(float.class)) {
                builder.defineFloat(fieldName, f);
            } else if (type.equals(double.class)) {
                builder.defineDouble(fieldName, f);
            } else if (type.equals(String.class)) {
                builder.defineString(fieldName, f);
            } else if (type.isAssignableFrom(Enum.class)) {
                builder.defineEnum(fieldName, f);
            } else if (type.isAssignableFrom(List.class)) {
                builder.defineList(fieldName, f);
            } else if (type.isAssignableFrom(Path.class)) {
                builder.definePath(fieldName, f);
            } else {
                throw new IllegalArgumentException("Type is not supported");
            }
        }

        for (Class<?> c: containerClass.getDeclaredClasses()) {
            Config subConfig = c.getAnnotation(Config.class);
            if (subConfig == null) continue; // skip not exposed stuff

            // validate sub-container identifier
            String subIdentifier = subConfig.value();
            if (!PATTERN.matcher(subIdentifier).find())
                throw new IllegalArgumentException("Config ID must follow the next pattern: " + PATTERN);

            // start building
            builder.push(subIdentifier);

        }
    }

    public static final

    /**
     * Registers your config spec to OmegaConfig's serializers
     *
     * <p>Register means OmegaConfig locks the fields nd runs serializers to read an update field values</p>
     *
     * @param spec config spec previously builded using {@link net.omegaloader.config.builder.ConfigSpecBuilder ConfigSpecBuilder}
     * @param configContainer config container which can be a class (staticContext = true) or an instance
     * @param staticContext anti-"i forgot to make my fields static" barrier.
     */
    public static void register(ConfigSpec spec, Object configContainer, boolean staticContext) {

    }
}
