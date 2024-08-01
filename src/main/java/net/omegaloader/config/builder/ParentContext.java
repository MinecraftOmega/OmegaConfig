package net.omegaloader.config.builder;

public class ParentContext {
    private final ParentContext parentContext;
    private final Object fieldContext;
    private final Class<?> fieldContextClass;
    private final String name;
    private final String i18n;

    public ParentContext(Object fieldContext, String name, String i18n) {
        this(null, fieldContext, (Class<?>) fieldContext, name, i18n);
    }

    public ParentContext(ParentContext parentContext, Object fieldContext, String name, String i18n) {
        this(parentContext, fieldContext, (Class<?>) fieldContext, name, i18n);
    }

    public ParentContext(Object fieldContext, Class<?> fieldContextClass, String name, String i18n) {
        this(null, fieldContext, fieldContextClass, name, i18n);
    }

    public ParentContext(ParentContext parentContext, Object fieldContext, Class<?> fieldContextClass, String name, String i18n) {
        this.parentContext = parentContext;
        this.fieldContext = fieldContext;
        this.fieldContextClass = fieldContextClass;
        this.name = name;
        this.i18n = i18n;
    }

    public ParentContext getParent() {
        return parentContext;
    }
}
