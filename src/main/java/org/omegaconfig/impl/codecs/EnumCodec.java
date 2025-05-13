package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.IComplexCodec;

public class EnumCodec implements IComplexCodec<Enum, Enum> {
    @Override
    public Enum decode(String value, Class<Enum> subType) {
        return Enum.valueOf((Class<? extends Enum>) subType, value);
    }

    @Override
    public String encode(Enum instance, Class<?> subType) {
        return instance.name();
    }

    @Override
    public Class<Enum> type() {
        return Enum.class;
    }
}