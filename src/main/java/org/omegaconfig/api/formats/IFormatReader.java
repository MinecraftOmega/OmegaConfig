package org.omegaconfig.api.formats;

import java.io.Closeable;

public interface IFormatReader extends Closeable {
    /**
     * Reads a value from the configuration file
     * @param fieldName the field name
     * @return the value read from the configuration file, parsed into the given type by the codecs
     */
    String read(String fieldName);

    /**
     * Pushes a group to the stack, so that the next read will be relative to this group
     * @param group the group name
     */
    void push(String group);

    /**
     * Pops the last group from the stack, so that the next read will be relative to the previous group
     */
    void pop();
}
