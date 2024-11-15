package net.omegaloader.config.builder.field;

import java.util.HashMap;
import java.util.Map;

public class GroupField extends BaseConfigField<Void> {
    public final GroupField parent;
    public final String name;
    public final String i18n;
    final Map<String, BaseConfigField<?>> fields = new HashMap<>();

    public GroupField(GroupField parent, String name, String i18n) {
        this.parent = parent;
        this.name = name;
        this.i18n = i18n;
    }

    public GroupField parent() {
        return parent == null ? this : parent;
    }

    public void refresh()

    @Override
    public boolean isGroup() {
        return true;
    }

    @Override
    public Map<String, BaseConfigField<?>> getFields() {
        return fields;
    }

    public int backups() {
        return 2;
    }

    @Override
    public void accept(Void unused) {
        throw new UnsupportedOperationException("Groups are not real options");
    }

    @Override
    public Void get() {
        throw new UnsupportedOperationException("Groups are not real options");
    }
}
