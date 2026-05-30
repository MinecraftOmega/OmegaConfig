package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;

public class PathField extends BaseConfigField<Path, Void> {
    public final boolean runtimePath;
    public final boolean fileExists;

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, Path defaultValue, Control control) {
        super(name, group, comments, defaultValue, coherent(name, control));
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
    }

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, Field field, Object context, Control control) {
        super(name, group, comments, field, context, coherent(name, control));
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
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
        if (this.fileExists && !this.get().toFile().exists()) {
            this.reset();
        }

        if (this.runtimePath && this.get().toFile().isAbsolute()) {
            this.reset();
        }
    }
}
