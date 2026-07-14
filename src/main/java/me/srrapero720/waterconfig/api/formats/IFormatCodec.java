package me.srrapero720.waterconfig.api.formats;

import java.io.IOException;
import java.nio.file.Path;

public interface IFormatCodec {
    /**
     * Get the format id, usually is the extension of the file without the dot.
     * @return format id
     */
    String id();

    /**
     * Get the format extension
     * @return extension of the file, usually is the format id with a dot at the start
     */
    String extension();

    /**
     * Get the mime type of the format
     * @return mimetype in the form of "type/subtype", lowercase and starts with "text/"
     */
    String mimeType();

    /**
     * creates a new reader for the given filepath
     * reading is pre-made instead of a sequential read
     *
     * @param filePath path of the file to read
     * @return a new reader of the current format
     */
    IFormatReader createReader(Path filePath) throws IOException;

    /**
     * Creates a new reader with the given parse mode. Formats without strict failure
     * points (or without repair support) fall back to the strict reader.
     *
     * @param filePath path of the file to read
     * @param mode strictness; {@link ParseMode#REPAIR} recovers from format errors
     * @return a new reader of the current format
     */
    default IFormatReader createReader(Path filePath, ParseMode mode) throws IOException {
        return this.createReader(filePath);
    }

    /**
     * creates a new writer for the given filepath
     * writing is sequential.
     *
     * @param filePath path of the file to write
     * @return a new writer of the current format
     */
    IFormatWriter createWriter(Path filePath) throws IOException;

    /**
     * Parsing strictness for {@link IFormatCodec#createReader(Path, ParseMode)}.
     */
    enum ParseMode {
        /**
         * Format errors throw a clean exception and abort the read.
         */
        STRICT,
        /**
         * Format errors become resync points: the parser repairs the broken region,
         * records what happened as a {@link RepairEntry} and keeps going. Only valid when a
         * real spec exists to validate the recovered values against.
         */
        REPAIR
    }

    /**
     * One entry of a reader's repair report: a problem the parser recovered from in repair mode.
     *
     * @param line     1-based line of the problem, 0 when it has no file position
     * @param what     short description of what was wrong
     * @param recovery how the parser recovered
     */
    record RepairEntry(int line, String what, String recovery) {
        @Override
        public String toString() {
            return (line > 0 ? "line " + line + ": " : "") + what + " -> " + recovery;
        }
    }
}
