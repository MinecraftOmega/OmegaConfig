package net.omegaloader.config.builder.field;

import java.util.function.LongSupplier;

public class LongField extends BaseConfigField<Long> implements LongSupplier {
    @Override
    public long getAsLong() {
        return 0L;
    }

    @Override
    public Long get() {
        return 0L;
    }
}
