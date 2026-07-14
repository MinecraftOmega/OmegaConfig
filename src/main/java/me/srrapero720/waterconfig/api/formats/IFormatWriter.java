package me.srrapero720.waterconfig.api.formats;

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
     * Writes a value that is a small matrix of named numeric cells as a native group
     * ({@code name { col = cell }}), reusing the format's own group syntax. Lets a scalar
     * value (e.g. a split color) appear as a group without any per-format code.
     *
     * @param name    the field name
     * @param columns the cell names, e.g. {@code {"r","g","b"}}
     * @param row     the cell values aligned with {@code columns}
     */
    default void writeMatrix(String name, String[] columns, String[] row) {
        this.push(name);
        for (int i = 0; i < columns.length; i++) {
            this.write(columns[i], row[i], Integer.class, null);
        }
        this.pop();
    }

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
