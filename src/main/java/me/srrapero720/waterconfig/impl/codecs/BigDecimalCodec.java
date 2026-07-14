package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.math.BigDecimal;

public class BigDecimalCodec implements ICodec<BigDecimal> {
    @Override
    public String encode(BigDecimal instance) {
        return instance.toString();
    }

    @Override
    public BigDecimal decode(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<BigDecimal> type() {
        return BigDecimal.class;
    }
}
