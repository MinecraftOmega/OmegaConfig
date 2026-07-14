package me.srrapero720.waterconfig.api.formats;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface IFormatReader extends Closeable {
    /**
     * Reads a value from the configuration file
     * @param fieldName the field name
     * @return the value read from the configuration file, parsed into the given type by the codecs
     */
    String read(String fieldName);

    /**
     * Reads a value from the configuration file
     * @param fieldName the field name
     * @return the value read from the configuration file
     */
    String[] readArray(String fieldName);

    /**
     * Reads a matrix of named cells previously written as a group by
     * {@link IFormatWriter#writeMatrix}. Returns the row aligned with {@code columns}, or
     * {@code null} when the group is absent (so the caller can try another representation).
     *
     * @param name    the field name
     * @param columns the cell names to read
     * @return the row, or null when none of the cells are present
     */
    default String[] readMatrix(String name, String[] columns) {
        this.push(name);
        String[] row = new String[columns.length];
        boolean any = false;
        for (int i = 0; i < columns.length; i++) {
            row[i] = this.read(columns[i]);
            if (row[i] != null) {
                any = true;
            }
        }
        this.pop();
        return any ? row : null;
    }

    /**
     * Pushes a group to the stack, so that the next read will be relative to this group
     * @param group the group name
     */
    void push(String group);

    /**
     * Pops the last group from the stack, so that the next read will be relative to the previous group
     */
    void pop();

    /**
     * Every parsed entry keyed by dotted path, insertion-ordered. Values are String
     * scalars or String[] arrays. Read-only view; empty when the reader does not pre-parse.
     */
    default Map<String, Object> entries() {
        return Map.of();
    }

    /**
     * Repair-mode recovery entries. Empty when the file parsed cleanly or the mode was strict.
     */
    default List<IFormatCodec.RepairEntry> report() {
        return List.of();
    }

    /**
     * Comments captured above each entry, keyed by dotted path; the empty key holds the
     * file-level comments. Empty when the format has no comments or none were captured.
     */
    default Map<String, java.util.List<String>> comments() {
        return Map.of();
    }
}
