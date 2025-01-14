package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

public class GroupField extends BaseConfigField<Void> {
    private final Set<BaseConfigField<?>> fields = new HashSet<>();

    public GroupField(String name, GroupField parent) {
        super(name, parent);
    }

    public void refreshAll() {

    }


    public <T extends BaseConfigField<?>> T putField(T configField) {
        this.assertUnlocked();

        if (this.fields.stream().anyMatch(configField1 -> configField1.id().equals(configField.id()))) {
            throw new IllegalArgumentException("Config field ID '" + configField.id() + "' its already registered");
        }
        this.fields.add(configField);

        return configField;
    }

    public <T extends BaseConfigField<?>> T getField(String fieldId) {
        return getField(Util.split(fieldId));
    }

    public GroupField getGroup(String groupId) {
        return getField(Util.split(groupId));
    }

    public <T extends BaseConfigField<?>> T getField(String[] ids) {
        BaseConfigField<?> f = null;
        for (String id: ids) {
            if (f != null && !(f instanceof GroupField g))
                throw new IllegalArgumentException("Parent ID is not a group");

            for (BaseConfigField<?> group: f instanceof GroupField g ? g.fields : this.fields) {
                if (group.name().equals(id)) {
                    f = group;
                    break;
                }
            }
        }

        return (T) f;
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
