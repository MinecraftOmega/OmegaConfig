package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class IntCodec implements ICodec<Integer> {
    @Override
    public String encode(Integer instance) {
        return instance.toString();
    }

    @Override
    public Integer decode(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }
}
