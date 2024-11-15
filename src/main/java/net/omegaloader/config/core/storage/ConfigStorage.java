package net.omegaloader.config.core.storage;

import net.omegaloader.config.builder.ConfigSpec;
import net.omegaloader.config.builder.field.BaseConfigField;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ConfigStorage {

    public abstract String extension();

    public abstract void deserialize(ConfigSpec spec, String string);

    public abstract String serializer(ConfigSpec spec);

    public abstract FieldStorage createFieldStorage(ConfigSpec spec, BaseConfigField<?> field);

    public static abstract class FieldStorage<T> {
        protected boolean dirty; // when is true it requires write on disk
        protected AtomicReference<T> ATT = new AtomicReference<>();

        public void f() {
            ATT.getPlain()
        }
    }
}