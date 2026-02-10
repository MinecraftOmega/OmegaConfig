package me.srrapero720.waterconfig.api;

public interface ICodec<T> {

    /**
     * Encodes an object instance into a string
     * @param instance java object
     * @return encoded string
     */
    String encode(T instance);

    /**
     * Attempts to parse the string into a valid object instance
     * @param value
     * @return
     */
    T decode(String value);

    /**
     * target codec type
     * @return
     */
    Class<T> type();
}
