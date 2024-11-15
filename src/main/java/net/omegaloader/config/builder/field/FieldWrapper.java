package net.omegaloader.config.builder.field;

import java.lang.reflect.Field;

public class FieldWrapper {
    private static final FieldWrapper NO_WRAPPER = new FieldWrapper();
    public FieldWrapper(Field field) {

    }

    private FieldWrapper() {}
}
