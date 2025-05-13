package org.omegaconfig.api;

public interface IComplexCodec<T, S> extends ICodec<T> {
    T decode(String value, Class<S> subType);

    String encode(T instance, Class<?> subType);

    @Override
    default T decode(String value) {
        throw new UnsupportedOperationException("Complex decode required");
    }

    @Override
    default String encode(T instance) {
        throw new UnsupportedOperationException("Complex encode required");
    }
}
