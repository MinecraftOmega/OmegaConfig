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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class TOMLFormat extends BaseFormat {
    @Override public String id() { return WaterConfig.FORMAT_TOML; }
    @Override public String extension() { return "." + this.id(); }
    @Override public String mimeType() { return "text/toml"; }

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
        // ROOT SCALARS ARE BUFFERED APART: TOML REQUIRES ROOT KEYS BEFORE ANY [table]
        private final StringBuilder root = new StringBuilder();
        private final StringBuilder tables = new StringBuilder();
        private final List<String> comments = new ArrayList<>();
        private String currentTable = "";
        private boolean tableHeaderWritten = false;
        private boolean firstInSection = true;

        public FormatWriter(Path path) throws IOException {
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        }

        @Override
        public void write(String comment) {
            this.comments.add(comment);
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            StringBuilder target = ensureTableHeader();
            String indent = indent();

            if (!firstInSection && !this.comments.isEmpty()) {
                target.append('\n');
            }
            for (String comment: this.comments) {
                target.append(indent).append("# ").append(comment).append('\n');
            }
            this.comments.clear();

            target.append(indent).append(escapeKey(fieldName)).append(" = ");
            target.append(formatValue(value, type));
            target.append('\n');
            this.firstInSection = false;
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            StringBuilder target = ensureTableHeader();
            String indent = indent();

            if (!firstInSection && !this.comments.isEmpty()) {
                target.append('\n');
            }
            for (String comment: this.comments) {
                target.append(indent).append("# ").append(comment).append('\n');
            }
            this.comments.clear();

            target.append(indent).append(escapeKey(fieldName)).append(" = [");
            if (values.length > 0) {
                target.append('\n');
                for (int i = 0; i < values.length; i++) {
                    target.append(indent).append("  ").append(formatValue(values[i], subType));
                    if (i < values.length - 1) {
                        target.append(',');
                    }
                    target.append('\n');
                }
                target.append(indent);
            }
            target.append("]\n");
            this.firstInSection = false;
        }

        @Override
        public void push(String groupName) {
            // ROOT PUSH IS TRANSPARENT: THE SPEC NAME IS NOT PART OF THE FILE STRUCTURE
            this.group.addLast(groupName);
            this.tableHeaderWritten = false;
        }

        @Override
        public void pop() {
            this.group.pollLast();
            this.tableHeaderWritten = false;
        }

        @Override
        public void close() throws IOException {
            this.writer.write(this.root.toString());
            if (!this.tables.isEmpty()) {
                this.writer.write("\n");
                this.writer.write(this.tables.toString());
            }
            this.writer.flush();
            this.writer.close();
        }

        // PICKS THE TARGET BUFFER AND EMITS THE [table] HEADER WHEN ENTERING A TABLE
        private StringBuilder ensureTableHeader() {
            String tableName = buildTableName();
            if (tableName.isEmpty()) {
                this.currentTable = "";
                return this.root;
            }
            if (!tableName.equals(this.currentTable) || !this.tableHeaderWritten) {
                if (!this.tables.isEmpty()) {
                    this.tables.append('\n');
                }
                this.tables.append('[').append(tableName).append("]\n");
                this.currentTable = tableName;
                this.tableHeaderWritten = true;
                this.firstInSection = true;
            }
            return this.tables;
        }

        // DOTTED TABLE NAME WITHOUT THE TRANSPARENT ROOT ELEMENT
        private String buildTableName() {
            if (this.group.size() <= 1) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = this.group.iterator();
            it.next(); // SKIP ROOT
            while (it.hasNext()) {
                sb.append(escapeKey(it.next()));
                if (it.hasNext()) {
                    sb.append('.');
                }
            }
            return sb.toString();
        }

        private String indent() {
            return pads(Math.max(0, this.group.size() - 1));
        }

        private static final Pattern SIMPLE_KEY = Pattern.compile("[A-Za-z0-9_-]+");

        private static String escapeKey(String key) {
            // SIMPLE KEYS NEED NO QUOTES
            if (SIMPLE_KEY.matcher(key).matches()) {
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

            // TOML SPELLS SPECIAL FLOATS AS nan/inf
            if (Float.class == type || Double.class == type || float.class == type || double.class == type) {
                return switch (value) {
                    case "NaN" -> "nan";
                    case "Infinity" -> "inf";
                    case "-Infinity" -> "-inf";
                    default -> value;
                };
            }

            if (Number.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                byte.class.isAssignableFrom(type) ||
                short.class.isAssignableFrom(type)) {
                return value;
            }

            return "\"" + escape(value) + "\"";
        }
    }

    public static class FormatReader extends BaseFormatReader {
        private final boolean repair;
        private final List<String> pending = new ArrayList<>();
        private String currentTable = "";

        public FormatReader(Path path, ParseMode mode) throws IOException {
            this.repair = mode == ParseMode.REPAIR;
            char[] data = read(path);
            parseToml(data);
        }

        private void parseToml(char[] data) throws IOException {
            int i = 0;
            int len = data.length;

            while (i < len) {
                char c = data[i];

                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // LINE-LEADING COMMENTS ATTACH TO THE NEXT ENTRY
                if (c == '#') {
                    int start = i + 1;
                    while (i < len && data[i] != '\n') i++;
                    pending.add(new String(data, start, i - start).trim());
                    continue;
                }

                // REPAIR MODE RESYNCS BY SKIPPING THE BROKEN LINE
                if (c == '[') {
                    try {
                        i = parseTableHeader(data, i);
                    } catch (IOException e) {
                        if (!repair) throw e;
                        report.add(new RepairEntry(lineOf(data, i), e.getMessage(), "line skipped"));
                        i = skipLineRaw(data, i);
                    }
                    continue;
                }

                if (isKeyStart(c)) {
                    try {
                        i = parseKeyValue(data, i);
                    } catch (IOException e) {
                        if (!repair) throw e;
                        report.add(new RepairEntry(lineOf(data, i), e.getMessage(), "line skipped"));
                        i = skipLineRaw(data, i);
                    }
                    continue;
                }

                i++;
            }
        }

        // ALWAYS-ADVANCING RAW SKIP PAST THE NEXT LINE BREAK
        private static int skipLineRaw(char[] data, int i) {
            while (i < data.length && data[i] != '\n') i++;
            return i < data.length ? i + 1 : i;
        }

        private int parseTableHeader(char[] data, int start) throws IOException {
            int i = start + 1;
            int len = data.length;

            // ARRAY OF TABLES [[...]]
            boolean isArray = false;
            if (i < len && data[i] == '[') {
                isArray = true;
                i++;
            }

            while (i < len && Character.isWhitespace(data[i])) {
                i++;
            }

            StringBuilder tableName = new StringBuilder();
            while (i < len && data[i] != ']') {
                if (data[i] == '#') {
                    throw new IOException("Comment not allowed in table header");
                }
                tableName.append(data[i]);
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed table header");
            }

            i++; // SKIP CLOSING BRACKET
            if (isArray) {
                if (i >= len || data[i] != ']') {
                    throw new IOException("Expected ]] for array of tables");
                }
                i++;
            }

            currentTable = tableName.toString().trim();
            attachPending(currentTable);
            return skipToEndOfLine(data, i);
        }

        // COMMENTS SEEN ABOVE THE UPCOMING ENTRY ATTACH TO ITS DOTTED KEY
        private void attachPending(String key) {
            if (!pending.isEmpty()) {
                comments.put(key, List.copyOf(pending));
                pending.clear();
            }
        }

        private int parseKeyValue(char[] data, int start) throws IOException {
            int i = start;
            int len = data.length;

            // KEY, WITH DOTTED-KEY SUPPORT (a.b.c = 1)
            StringBuilder key = new StringBuilder();
            i = parseKey(data, i, key);
            while (true) {
                int j = i;
                while (j < len && (data[j] == ' ' || data[j] == '\t')) j++;
                if (j >= len || data[j] != '.') break;
                j++;
                while (j < len && (data[j] == ' ' || data[j] == '\t')) j++;
                key.append('.');
                i = parseKey(data, j, key);
            }

            while (i < len && Character.isWhitespace(data[i])) {
                i++;
            }

            if (i >= len || data[i] != '=') {
                throw new IOException("Expected '=' after key");
            }

            do {
                i++;
            } while (i < len && Character.isWhitespace(data[i]));

            String fullKey = buildFullKey(key.toString());
            attachPending(fullKey);
            return parseValue(data, i, fullKey);
        }

        private int parseKey(char[] data, int start, StringBuilder key) throws IOException {
            int i = start;
            int len = data.length;
            char c = data[i];

            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < len && data[i] != quote) {
                    // BASIC (") KEYS DECODE ESCAPES; LITERAL (') KEYS ARE VERBATIM
                    if (quote == '"' && data[i] == '\\' && i + 1 < len) {
                        i = unescape(data, i + 1, key, true);
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
                while (i < len && (Character.isLetterOrDigit(data[i]) || data[i] == '_' || data[i] == '-')) {
                    key.append(data[i]);
                    i++;
                }
            }

            return i;
        }

        private int parseValue(char[] data, int start, String key) throws IOException {
            char c = data[start];

            if (c == '"' || c == '\'') {
                return parseString(data, start, key);
            }
            if (c == '[') {
                return parseArray(data, start, key);
            }
            if (c == '{') {
                return parseInlineTable(data, start, key);
            }
            return parseLiteral(data, start, key);
        }

        private int parseString(char[] data, int start, String key) throws IOException {
            int i = start;
            int len = data.length;
            char quote = data[i];
            i++;

            // MULTI-LINE STRING (TRIPLE QUOTES)
            if (i + 1 < len && data[i] == quote && data[i + 1] == quote) {
                i += 2;
                return parseMultilineString(data, i, key, quote);
            }

            StringBuilder value = new StringBuilder();
            while (i < len && data[i] != quote) {
                // BASIC (") STRINGS DECODE ESCAPES; LITERAL (') STRINGS ARE VERBATIM
                if (quote == '"' && data[i] == '\\' && i + 1 < len) {
                    i = unescape(data, i + 1, value, true);
                } else if (data[i] == '\n') {
                    throw new IOException("Newline not allowed in single-line string");
                } else {
                    value.append(data[i]);
                    i++;
                }
            }

            if (i >= len) {
                throw new IOException("Unclosed string");
            }

            i++; // SKIP CLOSING QUOTE
            values.put(key, value.toString());
            return skipToEndOfLine(data, i);
        }

        private int parseMultilineString(char[] data, int start, String key, char quote) throws IOException {
            int i = start;
            int len = data.length;
            StringBuilder value = new StringBuilder();

            // NEWLINE RIGHT AFTER THE OPENING DELIMITER IS TRIMMED
            if (i < len && data[i] == '\n') {
                i++;
            }

            while (i < len) {
                if (i + 2 < len && data[i] == quote && data[i + 1] == quote && data[i + 2] == quote) {
                    i += 3;
                    values.put(key, value.toString());
                    return skipToEndOfLine(data, i);
                }

                if (quote == '"' && data[i] == '\\' && i + 1 < len) {
                    i++;
                    if (data[i] == '\n' || (data[i] == '\r' && i + 1 < len && data[i + 1] == '\n')) {
                        // LINE-ENDING BACKSLASH TRIMS THE NEWLINE AND FOLLOWING WHITESPACE
                        while (i < len && Character.isWhitespace(data[i])) {
                            i++;
                        }
                        continue;
                    }
                    i = unescape(data, i, value, true);
                } else {
                    value.append(data[i]);
                    i++;
                }
            }

            throw new IOException("Unclosed multi-line string");
        }

        private int parseArray(char[] data, int start, String key) throws IOException {
            int i = start + 1;
            int len = data.length;
            List<String> array = new ArrayList<>();

            while (i < len) {
                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                if (i >= len) {
                    throw new IOException("Unclosed array");
                }

                if (data[i] == ']') {
                    i++;
                    values.put(key, array.toArray(new String[0]));
                    return skipToEndOfLine(data, i);
                }

                if (data[i] == '#') {
                    i = skipToEndOfLine(data, i);
                    continue;
                }

                StringBuilder element = new StringBuilder();
                i = parseArrayElement(data, i, element);
                array.add(element.toString());

                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                // INLINE COMMENT AFTER AN ELEMENT
                if (i < len && data[i] == '#') {
                    i = skipToEndOfLine(data, i);
                    while (i < len && Character.isWhitespace(data[i])) {
                        i++;
                    }
                }

                if (i < len && data[i] == ',') {
                    i++;
                }
            }

            throw new IOException("Unclosed array");
        }

        private int parseArrayElement(char[] data, int start, StringBuilder element) throws IOException {
            int i = start;
            int len = data.length;
            char c = data[i];

            if (c == '"' || c == '\'') {
                i++;
                while (i < len && data[i] != c) {
                    // BASIC (") STRINGS DECODE ESCAPES; LITERAL (') STRINGS ARE VERBATIM
                    if (c == '"' && data[i] == '\\' && i + 1 < len) {
                        i = unescape(data, i + 1, element, true);
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

            // LITERAL (NUMBER, BOOLEAN, ETC.)
            while (i < len && data[i] != ',' && data[i] != ']' && data[i] != '\n' && data[i] != '#') {
                element.append(data[i]);
                i++;
            }

            while (!element.isEmpty() && Character.isWhitespace(element.charAt(element.length() - 1))) {
                element.setLength(element.length() - 1);
            }

            String normalized = normalizeLiteral(element.toString());
            element.setLength(0);
            element.append(normalized);
            return i;
        }

        private int parseInlineTable(char[] data, int start, String key) throws IOException {
            int i = start + 1;
            int len = data.length;

            while (i < len) {
                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                if (i >= len) {
                    throw new IOException("Unclosed inline table");
                }

                if (data[i] == '}') {
                    i++;
                    return skipToEndOfLine(data, i);
                }

                StringBuilder subKey = new StringBuilder();
                i = parseKey(data, i, subKey);

                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                if (i >= len || data[i] != '=') {
                    throw new IOException("Expected '=' in inline table");
                }

                do {
                    i++;
                } while (i < len && Character.isWhitespace(data[i]));

                i = parseValue(data, i, key + "." + subKey);

                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                if (i < len && data[i] == ',') {
                    i++;
                }
            }

            throw new IOException("Unclosed inline table");
        }

        private int parseLiteral(char[] data, int start, String key) {
            int i = start;
            int len = data.length;
            StringBuilder value = new StringBuilder();

            while (i < len && data[i] != '\n' && data[i] != '\r' && data[i] != '#' && data[i] != ',' && data[i] != ']' && data[i] != '}') {
                value.append(data[i]);
                i++;
            }

            values.put(key, normalizeLiteral(value.toString().trim()));
            return skipToEndOfLine(data, i);
        }

        private static final Pattern DECIMAL_NUMBER = Pattern.compile("[+-]?[0-9][0-9_]*(\\.[0-9_]+)?([eE][+-]?[0-9_]+)?");
        private static final Pattern HEX_NUMBER = Pattern.compile("[+-]?0[xX][0-9a-fA-F_]+");
        private static final Pattern OCTAL_NUMBER = Pattern.compile("[+-]?0[oO][0-7_]+");
        private static final Pattern BINARY_NUMBER = Pattern.compile("[+-]?0[bB][01_]+");

        // MAPS TOML NUMERIC SPELLINGS (nan/inf, 1_000, 0x/0o/0b) TO CODEC-PARSEABLE DECIMAL FORM
        private static String normalizeLiteral(String literal) {
            switch (literal) {
                case "nan", "+nan", "-nan": return "NaN";
                case "inf", "+inf": return "Infinity";
                case "-inf": return "-Infinity";
            }

            String s = literal;
            if (s.indexOf('_') >= 0 && DECIMAL_NUMBER.matcher(s).matches()) {
                s = s.replace("_", "");
            }
            try {
                if (HEX_NUMBER.matcher(s).matches()) return radix(s, 16);
                if (OCTAL_NUMBER.matcher(s).matches()) return radix(s, 8);
                if (BINARY_NUMBER.matcher(s).matches()) return radix(s, 2);
            } catch (NumberFormatException e) {
                return literal; // OUT-OF-RANGE PREFIXED INTEGER: KEEP THE RAW TOKEN
            }
            return s;
        }

        private static String radix(String s, int radix) {
            boolean negative = s.charAt(0) == '-';
            int prefix = (negative || s.charAt(0) == '+') ? 3 : 2;
            long value = Long.parseLong(s.substring(prefix).replace("_", ""), radix);
            return negative ? "-" + value : String.valueOf(value);
        }

        private int skipToEndOfLine(char[] data, int start) {
            int i = start;
            int len = data.length;
            while (i < len && data[i] != '\n') {
                if (data[i] == '#') {
                    while (i < len && data[i] != '\n') {
                        i++;
                    }
                    break;
                }
                if (!Character.isWhitespace(data[i])) {
                    // ONLY WHITESPACE AND COMMENTS MAY FOLLOW A VALUE
                    break;
                }
                i++;
            }
            if (i < len && data[i] == '\n') {
                i++;
            }
            return i;
        }

        private boolean isKeyStart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '"' || c == '\'';
        }

        private String buildFullKey(String key) {
            return currentTable.isEmpty() ? key : currentTable + "." + key;
        }
    }
}
