package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;

public class PathField extends BaseConfigField<Path, Void> {
    public final boolean runtimePath;
    public final boolean fileExists;
    public final boolean hardfail;

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, boolean hardfail, Path defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, coherent(name, control), suffix);
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
        this.hardfail = hardfail;
    }

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, boolean hardfail, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, coherent(name, control), suffix);
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
        this.hardfail = hardfail;
    }

    private static Control coherent(String name, Control control) {
        return switch (control) {
            case DEFAULT, INPUT_FILE -> Control.INPUT_FILE;
            case INPUT_FOLDER -> Control.INPUT_FOLDER;
            case INPUT -> Control.INPUT;
            case INPUT_PASTE -> Control.INPUT_PASTE;
            default -> throw incoherentControl(name, control);
        };
    }

    @Override
    public Class<Path> type() {
        return Path.class;
    }

    @Override
    public void validate() {
        Path value = this.get();
        boolean invalid = (this.fileExists && !value.toFile().exists()) || (this.runtimePath && value.isAbsolute());
        if (!invalid) {
            return;
        }
        // HARD FAIL THROWS ABOUT THE WRONG CONFIG VALUE; SOFT FAIL RESETS TO DEFAULT
        if (this.hardfail) {
            throw new IllegalStateException("Path field '" + this.id() + "' failed validation with value '" + value + "'");
        }
        this.reset();
    }
}
