package net.omegaloader.config.builder.fields;

import java.util.function.DoubleSupplier;

public class DoubleField extends BaseConfigField<Double> implements DoubleSupplier {
    @Override
    public double getAsDouble() {
        return 0;
    }

    @Override
    public Double get() {
        return 0.0;
    }
}
