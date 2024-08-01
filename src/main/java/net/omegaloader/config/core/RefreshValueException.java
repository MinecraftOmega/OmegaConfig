package net.omegaloader.config.core;

import java.lang.reflect.Field;

public class RefreshValueException extends UnsupportedOperationException {
    public RefreshValueException(Field field, Exception e) {
        super("Failed to set new value to byte field '" + field.getName() + "'", e);
    }
}
