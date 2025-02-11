package org.omegaconfig;

import org.omegaconfig.api.IConfigField;

import java.util.LinkedHashSet;
import java.util.Set;

public sealed class ConfigGroup implements IConfigField<Void, Void> permits ConfigSpec {
    public final String name;
    public final ConfigGroup group;
    Set<String> comments = new LinkedHashSet<>();
    Set<IConfigField<?, ?>> fields = new LinkedHashSet<>();

    public ConfigGroup(String name, ConfigGroup group) {
        this.name = name;
        this.group = group;
        if (this.group != null) {
            this.group.append(this);
        }
    }

    public IConfigField<?, ?> getField(String id) {
        for (IConfigField<?,?> field: this.fields) {
            if (field.id().equals(id)) {
                return field;
            }
        }
        return null;
    }

    public void markDirty(IConfigField<?, ?> field) {
        this.spec().markDirty(field);
    }

    @Override
    public String id() {
        String parentName = this.group != null ? (this.group.id() + ".") : ""; // spec:parent.config or spec:config
        return parentName + name + (this.group != null ? "" : ":");
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ConfigGroup group() {
        return this.group;
    }

    @Override
    public ConfigSpec spec() {
        return (this.group instanceof ConfigSpec spec) ? spec : this.group.spec();
    }


    @Override
    public Class<Void> type() {
        throw new UnsupportedOperationException("Groups cannot handle types");
    }

    @Override
    public Class<Void> subType() {
        throw new UnsupportedOperationException("Groups cannot handle subTypes");
    }

    @Override
    public String[] comments() {
        return comments.toArray(new String[0]);
    }

    public void append(IConfigField<?, ?> field) {
        this.fields.add(field);
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Groups cannot handle values");
    }

    @Override
    public boolean reflected() {
        throw new UnsupportedOperationException("Groups cannot handle values");
    }

    @Override
    public void set0(Object object) {
        throw new UnsupportedOperationException("Groups cannot handle values");
    }

    @Override
    public void accept(Void unused) {
        throw new UnsupportedOperationException("Groups cannot handle values");
    }

    @Override
    public Void get() {
        throw new UnsupportedOperationException("Groups cannot give values");
    }
}
