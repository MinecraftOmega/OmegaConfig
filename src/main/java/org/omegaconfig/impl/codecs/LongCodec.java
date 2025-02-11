package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class LongCodec implements ICodec<Long> {
    @Override
    public String encode(Long instance) {
        return instance.toString();
    }

    @Override
    public Long decode(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Long> type() {
        return Long.class;
    }
}