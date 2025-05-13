package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;

public class PathField extends BaseConfigField<Path, Void> {
    public final boolean runtimePath;
    public final boolean fileExists;

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, Path defaultValue) {
        super(name, group, comments, defaultValue);
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
    }

    public PathField(String name, ConfigGroup group, Set<String> comments, boolean runtimePath, boolean fileExists, Field field, Object context) {
        super(name, group, comments, field, context);
        this.runtimePath = runtimePath;
        this.fileExists = fileExists;
    }

    @Override
    public Class<Path> type() {
        return Path.class;
    }
}
