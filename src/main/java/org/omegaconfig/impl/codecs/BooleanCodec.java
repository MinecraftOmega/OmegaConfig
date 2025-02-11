package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class BooleanCodec implements ICodec<Boolean> {
    @Override
    public String encode(Boolean instance) {
        return instance.toString();
    }

    @Override
    public Boolean decode(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }
}