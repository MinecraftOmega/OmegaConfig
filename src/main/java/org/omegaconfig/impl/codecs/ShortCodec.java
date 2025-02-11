package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class ShortCodec implements ICodec<Short> {
    @Override
    public String encode(Short instance) {
        return instance.toString();
    }

    @Override
    public Short decode(String value) {
        try {
            return Short.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Short> type() {
        return Short.class;
    }
}