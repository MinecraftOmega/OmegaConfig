package org.omegaconfig;

import org.omegaconfig.api.IConfigField;

import java.util.Collection;
import java.util.Collections;
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

    public Collection<IConfigField<?, ?>> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    // TODO: this is not EFFICIENT, but ok for now
    public IConfigField<?, ?> getField(String id) {
        for (IConfigField<?,?> field: this.fields) {
            if (field instanceof ConfigGroup group) {
                return group.getField(id);
            }
            if (field.id().equals(id)) {
                return field;
            }
        }
        return null;
    }

    public IConfigField<?, ?> findField(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        // STRIP THE ID
        int specSeparator = id.indexOf(":");
        String specId = specSeparator != -1 ? id.substring(0, specSeparator) : null;
        String path = specSeparator != -1 ? id.substring(specSeparator + 1) : id;

        // CHECK IF THE ID ITS OF ANOTHER SPEC (GO TO HELL)
        if (specId != null && !specId.equals(this.spec().id())) {
            return null;
        }

        // STRIP THE PATH
        String[] parts = path.split("\\.");

        // RUN FROM THIS
        ConfigGroup current = this;

        // ITERATE ALL ID PARTS
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isLast = i == parts.length - 1;

            // LOOKUP FOR THE PART
            IConfigField<?, ?> field = null;
            for (IConfigField<?, ?> f: current.fields) {
                if (f.name().equals(part)) {
                    field = f;
                    break;
                }
            }

            // NOT FOUND
            if (field == null) {
                return null;
            }

            // IF WAS LAST, RETURN IT
            if (isLast) {
                return field;
            }

            // NEXT LEVEL
            if (field instanceof ConfigGroup group) {
                current = group;
            } else {
                return null; // MALFORMED ID
            }
        }

        return null;
    }

    public void markDirty(IConfigField<?, ?> field) {
        this.spec().markDirty(field);
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

    public <T extends IConfigField<?, ?>> void append(T field) {
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
    public void validate() {
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
