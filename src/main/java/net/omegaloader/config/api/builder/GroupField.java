package net.omegaloader.config.api.builder;

import java.util.HashMap;
import java.util.Map;

public class GroupField extends BaseConfigField<Void> {
    private final Map<String, BaseConfigField<?>> fields = new HashMap<>();

    public GroupField(String name, GroupField parent) {
        super(name, parent);
    }

    public Map<String, BaseConfigField<?>> getFields() {
        return fields;
    }

    @Override
    public void set(Void unused) {
        throw new UnsupportedOperationException("Groups are not real options");
    }

    @Override
    public Void get() {
        throw new UnsupportedOperationException("Groups are not real options");
    }
}
