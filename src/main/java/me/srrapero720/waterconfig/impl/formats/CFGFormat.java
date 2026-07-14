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
import java.util.regex.Pattern;

/**
 * CFG is WaterConfig's own libconfig-like format:
 * a braced root mapping with {@code #} comments, string or identifier keys,
 * {@code :} or {@code =} separators, optional commas, nested mappings and lists,
 * and backslash escapes (including {@code \\uXXXX}) in double or single quoted strings.
 */
public class CFGFormat extends BaseFormat {
    @Override public String id() { return WaterConfig.FORMAT_CFG; }
    @Override public String extension() { return "." + this.id(); }
    @Override public String mimeType() { return "text/x-cfg"; }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath, ParseMode.STRICT);
    }

    @Override
    public IFormatReader createReader(Path filePath, ParseMode mode) throws IOException {
        return new FormatReader(filePath, mode);
    }

    @Override
    public IFormatWriter createWriter(Path filePath) throws IOException {
        return new FormatWriter(filePath);
    }

    public static class FormatWriter implements IFormatWriter {
        private final Deque<String> group = new ArrayDeque<>();
        private final BufferedWriter writer;
        private final StringBuilder buffer = new StringBuilder();
        private final List<String> comments = new ArrayList<>();
        private boolean firstInMapping = true;
        private int indentLevel = 0;

        public FormatWriter(Path path) throws IOException {
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        }

        @Override
        public void write(String comment) {
            this.comments.add(comment);
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append('\n');
            }
            writeComments();

            indent();
            this.buffer.append(formatKey(fieldName)).append(": ");
            this.buffer.append(formatValue(value, type));
            this.buffer.append('\n');

            firstInMapping = false;
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append('\n');
            }
            writeComments();

            indent();
            this.buffer.append(formatKey(fieldName)).append(": ");

            this.buffer.append('[');
            if (values.length > 0) {
                this.buffer.append('\n');
                for (int i = 0; i < values.length; i++) {
                    indent();
                    this.buffer.append("  ").append(formatValue(values[i], subType));
                    if (i < values.length - 1) {
                        this.buffer.append(',');
                    }
                    this.buffer.append('\n');
                }
                indent();
            }
            this.buffer.append(']');
            this.buffer.append('\n');

            firstInMapping = false;
        }

        @Override
        public void push(String groupName) {
            if (this.group.isEmpty()) {
                // ROOT PUSH: WRITE PENDING COMMENTS BEFORE THE OPENING BRACE
                writeComments();
                this.buffer.append("{\n");
                this.group.addLast(groupName);
                this.indentLevel++;
                this.firstInMapping = true;
                return;
            }

            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append('\n');
            }
            writeComments();

            indent();
            this.buffer.append(formatKey(groupName)).append(": {\n");

            this.group.addLast(groupName);
            this.indentLevel++;
            this.firstInMapping = true;
        }

        @Override
        public void pop() {
            if (!this.group.isEmpty()) {
                this.group.pollLast();
                this.indentLevel--;
                this.buffer.append('\n');
                indent();
                this.buffer.append("}\n");
                this.firstInMapping = false;
            }
        }

        @Override
        public void close() throws IOException {
            this.writer.write(this.buffer.toString());
            this.writer.flush();
            this.writer.close();
        }

        private void writeComments() {
            for (String comment: this.comments) {
                indent();
                this.buffer.append("# ").append(comment).append('\n');
            }
            this.comments.clear();
        }

        private void indent() {
            this.buffer.append(pads(this.indentLevel));
        }

        private static final Pattern IDENTIFIER_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

        private static String formatKey(String key) {
            // IDENTIFIER KEYS NEED NO QUOTES
            if (IDENTIFIER_KEY.matcher(key).matches()) {
                return key;
            }
            return "\"" + escape(key) + "\"";
        }

        private static String formatValue(String value, Class<?> type) {
            if (type == null) {
                return "\"" + escape(value) + "\"";
            }

            if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                return value.toLowerCase();
            }

            if (value == null || value.equals("null")) {
                return "null";
            }

            if (Number.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                double.class.isAssignableFrom(type) ||
                float.class.isAssignableFrom(type) ||
                byte.class.isAssignableFrom(type) ||
                short.class.isAssignableFrom(type)) {
                return value;
            }

            return "\"" + escape(value) + "\"";
        }
    }

    public static class FormatReader extends BaseFormatReader {
        // BOUNDS RECURSION SO A MALICIOUSLY DEEP FILE FAILS CLEANLY INSTEAD OF BLOWING THE STACK
        private static final int MAX_DEPTH = 512;

        private final boolean repair;
        private final List<String> pending = new ArrayList<>();
        private int depth;

        public FormatReader(Path path, ParseMode mode) throws IOException {
            this.repair = mode == ParseMode.REPAIR;
            char[] data = read(path);
            parseCfg(data);
        }

        private void parseCfg(char[] data) throws IOException {
            int i = skipCapturing(data, 0, data.length);

            // CFG TOP LEVEL IS A MAPPING
            if (i >= data.length || data[i] != '{') {
                if (repair) {
                    report.add(new RepairEntry(1, "CFG file must start with '{'", "treated as an empty spec"));
                    return;
                }
                throw new IOException("CFG file must start with '{'");
            }

            attachPending("");
            i = parseMapping(data, i + 1, data.length, "");

            // ONLY WHITESPACE AND COMMENTS MAY FOLLOW THE ROOT MAPPING
            i = skipWhitespaceAndComments(data, i, data.length);
            if (i < data.length) {
                if (repair) {
                    report.add(new RepairEntry(lineOf(data, i), "unexpected trailing content", "ignored"));
                    return;
                }
                throw new IOException("Unexpected trailing content at position " + i);
            }
        }

        private int parseMapping(char[] data, int start, int len, String prefix) throws IOException {
            if (++depth > MAX_DEPTH) {
                depth--;
                throw new IOException("Mappings nested deeper than " + MAX_DEPTH + " levels");
            }
            try {
                return parseMapping0(data, start, len, prefix);
            } finally {
                depth--;
            }
        }

        private int parseMapping0(char[] data, int start, int len, String prefix) throws IOException {
            int i = start;
            boolean firstEntry = true;

            while (i < len) {
                i = skipCapturing(data, i, len);
                if (i >= len) break;

                if (data[i] == '}') {
                    return i + 1;
                }

                // COMMA OR NEWLINE SEPARATOR BETWEEN ENTRIES
                if (!firstEntry && (data[i] == ',' || data[i] == '\n')) {
                    i++;
                    i = skipCapturing(data, i, len);
                    if (i >= len) break;
                    if (data[i] == '}') {
                        return i + 1;
                    }
                }

                firstEntry = false;

                try {
                    StringBuilder key = new StringBuilder();
                    i = parseKey(data, i, len, key);

                    i = skipWhitespaceAndComments(data, i, len);

                    if (i >= len || (data[i] != ':' && data[i] != '=')) {
                        throw new IOException("Expected ':' or '=' after key at position " + i);
                    }
                    i++; // SKIP SEPARATOR

                    i = skipWhitespaceAndComments(data, i, len);

                    String fullKey = prefix.isEmpty() ? key.toString() : prefix + "." + key;
                    attachPending(fullKey);
                    i = parseValue(data, i, len, fullKey);
                } catch (IOException e) {
                    if (!repair) throw e;
                    // REPAIR MODE RESYNCS AT THE NEXT NEWLINE OR BRACE BOUNDARY
                    report.add(new RepairEntry(lineOf(data, i), e.getMessage(), "skipped to the next boundary"));
                    i = resync(data, i, len);
                }
            }

            if (repair) {
                report.add(new RepairEntry(lineOf(data, i), "unclosed mapping (EOF before '}')", "accepted as closed"));
                return i;
            }
            throw new IOException("Unclosed mapping (EOF before '}')");
        }

        // SKIPS THE BROKEN REGION; STOPS AT A '}' (UNCONSUMED, THE MAPPING LOOP CLOSES IT) OR PAST A NEWLINE
        private static int resync(char[] data, int i, int len) {
            while (i < len) {
                char c = data[i];
                if (c == '\n') return i + 1;
                if (c == '}') return i;
                i++;
            }
            return i;
        }

        private int parseKey(char[] data, int start, int len, StringBuilder key) throws IOException {
            int i = start;
            char c = data[i];

            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < len && data[i] != quote) {
                    if (data[i] == '\\' && i + 1 < len) {
                        i = unescape(data, i + 1, key, false);
                    } else {
                        key.append(data[i]);
                        i++;
                    }
                }
                if (i >= len) {
                    throw new IOException("Unclosed quoted key");
                }
                i++; // SKIP CLOSING QUOTE
            } else {
                if (!Character.isLetter(c) && c != '_') {
                    throw new IOException("Invalid key start character at position " + i);
                }
                while (i < len && (Character.isLetterOrDigit(data[i]) || data[i] == '_')) {
                    key.append(data[i]);
                    i++;
                }
            }

            return i;
        }

        private int parseValue(char[] data, int start, int len, String key) throws IOException {
            if (start >= len) {
                throw new IOException("Expected value after separator at position " + start);
            }

            char c = data[start];

            if (c == '"' || c == '\'') {
                return parseString(data, start, len, key);
            }
            if (c == '[') {
                return parseArray(data, start, len, key);
            }
            if (c == '{') {
                return parseMapping(data, start + 1, len, key);
            }
            return parseLiteral(data, start, len, key);
        }

        private int parseString(char[] data, int start, int len, String key) throws IOException {
            int i = start;
            char quote = data[i];
            i++;

            StringBuilder value = new StringBuilder();
            while (i < len && data[i] != quote) {
                if (data[i] == '\\' && i + 1 < len) {
                    i = unescape(data, i + 1, value, false);
                } else {
                    value.append(data[i]);
                    i++;
                }
            }

            if (i >= len) {
                throw new IOException("Unclosed string at position " + i);
            }

            i++; // SKIP CLOSING QUOTE
            values.put(key, value.toString());
            return i;
        }

        private int parseArray(char[] data, int start, int len, String key) throws IOException {
            int i = start + 1;
            List<String> array = new ArrayList<>();
            boolean firstElement = true;

            while (i < len) {
                i = skipWhitespaceAndComments(data, i, len);
                if (i >= len) {
                    throw new IOException("Unclosed array");
                }

                if (data[i] == ']') {
                    i++;
                    values.put(key, array.toArray(new String[0]));
                    return i;
                }

                if (!firstElement && (data[i] == ',' || data[i] == '\n')) {
                    i++;
                    i = skipWhitespaceAndComments(data, i, len);
                    if (i >= len) break;
                    if (data[i] == ']') {
                        i++;
                        values.put(key, array.toArray(new String[0]));
                        return i;
                    }
                }

                firstElement = false;

                StringBuilder element = new StringBuilder();
                i = parseArrayElement(data, i, len, element);
                array.add(element.toString());
            }

            throw new IOException("Unclosed array");
        }

        private int parseArrayElement(char[] data, int start, int len, StringBuilder element) throws IOException {
            int i = start;
            char c = data[i];

            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < len && data[i] != quote) {
                    if (data[i] == '\\' && i + 1 < len) {
                        i = unescape(data, i + 1, element, false);
                    } else {
                        element.append(data[i]);
                        i++;
                    }
                }
                if (i >= len) {
                    throw new IOException("Unclosed string in array");
                }
                i++;
                return i;
            }

            // LITERAL (NUMBER, BOOLEAN, NULL)
            while (i < len && !Character.isWhitespace(data[i]) && data[i] != ',' && data[i] != ']' && data[i] != '#') {
                element.append(data[i]);
                i++;
            }

            return i;
        }

        private int parseLiteral(char[] data, int start, int len, String key) {
            int i = start;
            StringBuilder value = new StringBuilder();

            while (i < len && data[i] != '\n' && data[i] != '\r' && data[i] != ',' && data[i] != '}' && data[i] != ']' && data[i] != '#') {
                value.append(data[i]);
                i++;
            }

            values.put(key, value.toString().trim());
            return i;
        }

        private int skipWhitespaceAndComments(char[] data, int start, int len) {
            int i = start;
            while (i < len) {
                if (Character.isWhitespace(data[i])) {
                    i++;
                    continue;
                }
                if (data[i] == '#') {
                    while (i < len && data[i] != '\n') {
                        i++;
                    }
                    continue;
                }
                break;
            }
            return i;
        }

        // LIKE skipWhitespaceAndComments, BUT COMMENT TEXT IS KEPT FOR THE UPCOMING ENTRY
        private int skipCapturing(char[] data, int start, int len) {
            int i = start;
            while (i < len) {
                if (Character.isWhitespace(data[i])) {
                    i++;
                    continue;
                }
                if (data[i] == '#') {
                    int textStart = i + 1;
                    while (i < len && data[i] != '\n') {
                        i++;
                    }
                    pending.add(new String(data, textStart, i - textStart).trim());
                    continue;
                }
                break;
            }
            return i;
        }

        // COMMENTS SEEN ABOVE THE UPCOMING ENTRY ATTACH TO ITS DOTTED KEY ("" = FILE LEVEL)
        private void attachPending(String key) {
            if (!pending.isEmpty()) {
                comments.put(key, List.copyOf(pending));
                pending.clear();
            }
        }
    }
}
