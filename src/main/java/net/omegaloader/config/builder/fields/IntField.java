package net.omegaloader.config.builder.fields;

import java.util.function.IntSupplier;

public class IntField extends BaseConfigField<Integer> implements IntSupplier {
    @Override
    public int getAsInt() {
        return 0;
    }

    @Override
    public Integer get() {
        return 0;
    }
}
