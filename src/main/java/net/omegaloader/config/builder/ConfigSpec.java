package net.omegaloader.config.builder;

import net.omegaloader.config.annotations.field.Config;
import net.omegaloader.config.builder.field.GroupField;

import java.lang.annotation.Annotation;

public class ConfigSpec extends GroupField {
    private String filename;
    private ConfigFileFormat format;
    private Object context;

    ConfigSpec(String filename, ConfigFileFormat format, Object context) {
        super(null, filename, "org.omegaloader.config.spec." + filename);
    }

    public void save() {

    }

    public static class Builder {
        private Object configClassContext;
        private String filename;
        private ConfigFileFormat format;
        private boolean built;

        public Builder() {}


        public Builder setClassContext(Object context) {
            assert !built;
            this.configClassContext = context;
            return this;
        }

        public Builder setFilenamae(String filename) {
            assert !built;
            this.filename = filename;
            return this;
        }

        public Builder setFormat(ConfigFileFormat format) {
            assert !built;
            this.format = format;
            return this;
        }

        public ConfigSpec build() {
            if (this.format == null)
                throw new IllegalArgumentException("Format cannot be null");
            if (this.configClassContext == null)
                throw new IllegalArgumentException("Context cannot be null");
            if (this.filename == null)
                throw new IllegalArgumentException("Filename cannot be null");

            // validate root annotation or instance
            GroupField asField = null;
            Config asAnnotation = null;
            if (configClassContext instanceof GroupField field) {
                asField = field;
            }
            if (configClassContext.getClass().isAnnotationPresent(Config.class)) {
                asAnnotation = configClassContext.getClass().getAnnotation(Config.class);
            }

            String cname;
            String ci18n;

            if (asField == null || asAnnotation == null)
                throw new IllegalArgumentException("Context object is not a group field");


        }
    }
}