package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.math.BigInteger;

public class BigIntegerCodec implements ICodec<BigInteger> {
    @Override
    public String encode(BigInteger instance) {
        return instance.toString();
    }

    @Override
    public BigInteger decode(String value) {
        try {
            return new BigInteger(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<BigInteger> type() {
        return BigInteger.class;
    }
}
