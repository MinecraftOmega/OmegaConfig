package org.omegaconfig.api.formats;

import java.io.Closeable;
import java.io.IOException;

public interface IFormatWriter extends Closeable {
    /**
     * Write a comment on the current field writing position.
     * Writing is sequential, so the order of the write calls fields is important.
     *
     * @param comment the comment to write
     */
    void write(String comment);

    /**
     * Write a field on the current position.
     * Writing is sequential, so the order of the write calls fields is important.
     *
     * @param fieldName the name of the field
     * @param value     the boolean value to write
     */
    void write(String fieldName, String value, Class<?> type, Class<?> subType);

    /**
     * Write a field on the current position.
     * Writing is sequential, so the order of the write calls fields is important.
     *
     * @param fieldName the name of the field
     * @param values     values to write
     */
    void write(String fieldName, String[] values, Class<?> type, Class<?> subType);

    /**
     * Creates a new group on the current position of the file.
     *
     * @param groupName the name of the group
     */
    void push(String groupName);

    /**
     * Closes the current group.
     * This will close into the last opened group.
     */
    void pop();
}
