package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class StringField extends BaseConfigField<String> {


    public StringField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public StringField(String name, GroupField parent, String defaultValue) {
        super(name, parent, defaultValue);
    }
}
