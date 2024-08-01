package net.omegaloader.config.core;

import java.lang.reflect.Field;

public class InvalidFieldException extends RuntimeException {
    public InvalidFieldException(Field f, Exception e) {
        super("Failed to validate field" + (f != null ? " " + f.getName() : ", field is null"), e);

    }
}
