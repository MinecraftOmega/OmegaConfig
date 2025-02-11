package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

public class CharCodec implements ICodec<Character> {
    @Override
    public String encode(Character instance) {
        return instance.toString();
    }

    @Override
    public Character decode(String value) {
        return value.length() == 1 ? value.charAt(0) : null;
    }

    @Override
    public Class<Character> type() {
        return Character.class;
    }
}