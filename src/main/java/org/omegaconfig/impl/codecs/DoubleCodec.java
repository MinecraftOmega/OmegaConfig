package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class DoubleCodec implements ICodec<Double> {
    @Override
    public String encode(Double instance) {
        return instance.toString();
    }

    @Override
    public Double decode(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Double> type() {
        return Double.class;
    }
}