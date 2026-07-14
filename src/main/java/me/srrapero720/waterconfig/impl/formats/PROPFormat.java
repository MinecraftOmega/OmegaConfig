package me.srrapero720.waterconfig.impl.formats;

import me.srrapero720.waterconfig.WaterConfig;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Properties format following {@code java.util.Properties} rules with the encoding
 * pinned to UTF-8: {@code =}, {@code :} and whitespace separators, {@code #} and
 * {@code !} comment lines, backslash line continuations and escapes in both directions.
 */
public class PROPFormat extends BaseFormat {
    public static final String FORMAT_KEY_DEF_SPLIT = "=";
    public static final String FORMAT_KEY_BREAKLINE = "\n";
    public static final String FORMAT_KEY_COMMENT_LINE = "#";
    public static final char FORMAT_KEY_GROUP_SPLIT = '.';

    @Override public String id() { return WaterConfig.FORMAT_PROPERTIES; }
    @Override public String extension() { return "." + id(); }
    @Override public String mimeType() { return "text/x-java-properties"; }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath);
    }

    @Override
    public IFormatWriter createWriter(Path filePath) throws IOException {
        return new FormatWriter(filePath);
    }

    private static class FormatReader extends BaseFormatReader {
        public FormatReader(Path filePath) throws IOException {
            char[] data = read(filePath);
            final int len = data.length;
            final StringBuilder key = new StringBuilder();
            final StringBuilder value = new StringBuilder();
            final List<String> pending = new ArrayList<>();
            int i = 0;

            while (i < len) {
                char c = data[i];

                // SKIP BLANK SPACE BETWEEN LOGICAL LINES
                if (c == '\n' || c == '\r' || c == ' ' || c == '\t' || c == '\f') {
                    i++;
                    continue;
                }

                // COMMENT LINES START WITH # OR !; TEXT ATTACHES TO THE NEXT ENTRY
                if (c == '#' || c == '!') {
                    int start = i + 1;
                    while (i < len && data[i] != '\n') i++;
                    pending.add(new String(data, start, i - start).trim());
                    continue;
                }

                key.setLength(0);
                value.setLength(0);

                // KEY: UP TO THE FIRST UNESCAPED SEPARATOR (=, : OR WHITESPACE)
                boolean assigned = false;
                while (i < len) {
                    c = data[i];
                    if (c == '\n' || c == '\r') break;
                    if (c == '\\') {
                        i = unescape(data, i, key);
                        continue;
                    }
                    if (c == '=' || c == ':') {
                        assigned = true;
                        i++;
                        break;
                    }
                    if (c == ' ' || c == '\t' || c == '\f') {
                        i++;
                        break;
                    }
                    key.append(c);
                    i++;
                }

                // AFTER A WHITESPACE SEPARATOR AN OPTIONAL SINGLE = OR : MAY FOLLOW
                if (!assigned) {
                    while (i < len && (data[i] == ' ' || data[i] == '\t' || data[i] == '\f')) i++;
                    if (i < len && (data[i] == '=' || data[i] == ':')) i++;
                }

                // WHITESPACE BEFORE THE VALUE IS NOT PART OF IT
                while (i < len && (data[i] == ' ' || data[i] == '\t' || data[i] == '\f')) i++;

                // VALUE: REST OF THE LOGICAL LINE, TRAILING WHITESPACE PRESERVED
                while (i < len) {
                    c = data[i];
                    if (c == '\n' || c == '\r') break;
                    if (c == '\\') {
                        i = unescape(data, i, value);
                        continue;
                    }
                    value.append(c);
                    i++;
                }

                this.values.put(key.toString(), value.toString());
                if (!pending.isEmpty()) {
                    this.comments.put(key.toString(), List.copyOf(pending));
                    pending.clear();
                }
            }
        }

        @Override
        public String[] readArray(String fieldName) {
            String value = this.read(fieldName);
            // MISSING OR NON-ARRAY KEYS YIELD NULL, NEVER A CRASH
            if (value == null || value.length() < 2 || value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']') {
                return null;
            }

            value = value.substring(1, value.length() - 1).trim();
            if (value.isEmpty()) {
                return new String[0];
            }

            // SPLIT ON UNESCAPED COMMAS, THEN DECODE THE ELEMENT-LAYER \, AND \\ ESCAPES
            List<String> parts = new ArrayList<>();
            StringBuilder element = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\' && i + 1 < value.length()) {
                    element.append(value.charAt(++i));
                    continue;
                }
                if (c == ',') {
                    parts.add(decodeElement(element));
                    element.setLength(0);
                    continue;
                }
                element.append(c);
            }
            parts.add(decodeElement(element));
            return parts.toArray(new String[0]);
        }

        // TRIMS THE ", " JOIN PADDING; ESCAPED CHARS WERE ALREADY UNWRAPPED DURING THE SPLIT
        private static String decodeElement(StringBuilder element) {
            int start = 0, end = element.length();
            while (start < end && element.charAt(start) == ' ') start++;
            while (end > start && element.charAt(end - 1) == ' ') end--;
            return element.substring(start, end);
        }

        // DECODES THE ESCAPE AT data[i] (A BACKSLASH); BACKSLASH-NEWLINE IS A LINE CONTINUATION
        private static int unescape(char[] data, int i, StringBuilder out) {
            int len = data.length;
            if (i + 1 >= len) {
                return i + 1; // LONE TRAILING BACKSLASH AT EOF: DROPPED
            }
            char c = data[i + 1];
            if (c == '\n' || c == '\r') {
                // LINE CONTINUATION: DROP THE BREAK AND THE CONTINUATION LINE'S LEADING WHITESPACE
                i += (c == '\r' && i + 2 < len && data[i + 2] == '\n') ? 3 : 2;
                while (i < len && (data[i] == ' ' || data[i] == '\t' || data[i] == '\f')) i++;
                return i;
            }
            return BaseFormat.unescape(data, i + 1, out, false);
        }
    }

    private static class FormatWriter implements IFormatWriter {
        private final Deque<String> groups = new ArrayDeque<>();
        private final BufferedWriter out;
        private final StringBuilder data = new StringBuilder();
        private boolean rootPushed = false;

        public FormatWriter(Path filePath) throws IOException {
            this.out = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
        }

        @Override
        public void write(String comment) {
            this.data.append(FORMAT_KEY_COMMENT_LINE + " ")
                    .append(comment)
                    .append(FORMAT_KEY_BREAKLINE);
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            this.writeKey(fieldName);
            this.data.append(escapeValue(value)).append(FORMAT_KEY_BREAKLINE);
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            // ELEMENT LAYER: ESCAPE BACKSLASHES AND THE COMMA DELIMITER SO ELEMENTS ROUND-TRIP
            StringBuilder array = new StringBuilder(values.length * 12 + 2);
            array.append('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    array.append(", ");
                }
                array.append(values[i].replace("\\", "\\\\").replace(",", "\\,"));
            }
            array.append(']');

            this.writeKey(fieldName);
            this.data.append(escapeValue(array.toString())).append(FORMAT_KEY_BREAKLINE);
        }

        @Override
        public void push(String groupName) {
            if (!rootPushed) {
                rootPushed = true;
                return; // PROPERTIES ROOT HAS NO GROUP PREFIX
            }
            this.groups.addLast(groupName);
            this.data.append(FORMAT_KEY_BREAKLINE);
            this.write("Begin of group " + String.join(".", this.groups));
        }

        @Override
        public void pop() {
            if (this.groups.isEmpty()) return; // ROOT POP
            this.groups.pollLast();
            this.data.append(FORMAT_KEY_BREAKLINE.repeat(2));
        }

        @Override
        public void close() throws IOException {
            String data = this.data.toString();
            if (data.endsWith(FORMAT_KEY_BREAKLINE.repeat(2))) {
                data = data.substring(0, data.length() - FORMAT_KEY_BREAKLINE.length() * 2);
            }
            this.out.write(data);
            this.out.close();
        }

        // EMITS THE DOTTED GROUP PREFIX, THE ESCAPED KEY AND THE SEPARATOR
        private void writeKey(String fieldName) {
            for (String g: this.groups) {
                this.data.append(escapeKey(g)).append(FORMAT_KEY_GROUP_SPLIT);
            }
            this.data.append(escapeKey(fieldName)).append(FORMAT_KEY_DEF_SPLIT);
        }

        // KEYS ESCAPE SEPARATORS, WHITESPACE, COMMENT MARKERS AND BACKSLASHES
        private static String escapeKey(String s) {
            StringBuilder out = null;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                String escaped = switch (c) {
                    case '\\', '=', ':', ' ', '#', '!' -> "\\" + c;
                    case '\n' -> "\\n";
                    case '\r' -> "\\r";
                    case '\t' -> "\\t";
                    case '\f' -> "\\f";
                    default -> null;
                };
                if (escaped != null && out == null) {
                    out = new StringBuilder(s.length() + 8).append(s, 0, i);
                }
                if (out != null) {
                    out.append(escaped != null ? escaped : String.valueOf(c));
                }
            }
            return out != null ? out.toString() : s;
        }

        // VALUES ESCAPE BACKSLASHES, LINE BREAKS AND A LEADING WHITESPACE CHAR
        private static String escapeValue(String s) {
            StringBuilder out = null;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                String escaped = switch (c) {
                    case '\\' -> "\\\\";
                    case '\n' -> "\\n";
                    case '\r' -> "\\r";
                    case '\f' -> "\\f";
                    case ' ', '\t' -> (i == 0) ? (c == ' ' ? "\\ " : "\\t") : null;
                    default -> null;
                };
                if (escaped != null && out == null) {
                    out = new StringBuilder(s.length() + 8).append(s, 0, i);
                }
                if (out != null) {
                    out.append(escaped != null ? escaped : String.valueOf(c));
                }
            }
            return out != null ? out.toString() : s;
        }
    }
}
