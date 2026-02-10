package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.IComplexCodec;

public class EnumCodec implements IComplexCodec<Enum, Enum> {
    @Override
    public Enum decode(String value, Class<Enum> subType) {
        try {
            return Enum.valueOf((Class<? extends Enum>) subType, value);
        } catch (Exception e) {
            // Handle the case where the enum value is not found
            return null;
        }
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