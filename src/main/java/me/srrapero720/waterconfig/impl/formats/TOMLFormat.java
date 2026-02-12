package me.srrapero720.waterconfig.impl.formats;

import me.srrapero720.waterconfig.WaterConfig;
import me.srrapero720.waterconfig.Tools;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class TOMLFormat implements IFormatCodec {
    @Override public String id() { return WaterConfig.FORMAT_TOML; }
    @Override public String extension() { return "." + this.id(); }
    @Override public String mimeType() { return "text/toml"; }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath);
    }

    @Override
    public IFormatWriter createWriter(Path filePath) throws IOException {
        return new FormatWriter(filePath);
    }

    public static class FormatWriter implements IFormatWriter {
        private final Stack<String> group = new Stack<>();
        private final BufferedWriter writer;
        private final StringBuilder buffer = new StringBuilder();
        private final List<String> comments = new ArrayList<>();
        private String currentTable = "";
        private boolean tableHeaderWritten = false;
        private boolean firstInSection = true;

        public FormatWriter(Path path) throws IOException {
            if (!path.toFile().getParentFile().exists() && !path.toFile().getParentFile().mkdirs()) {
                throw new IOException("Failed to create parent directories for " + path);
            }
            this.writer = new BufferedWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8));
        }

        @Override
        public void write(String comment) {
            this.comments.add(comment);
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            ensureTableHeader();

            // Add blank line before comments for readability
            if (!firstInSection && !this.comments.isEmpty()) {
                this.buffer.append("\n");
            }

            // Write comments
            for (String comment : this.comments) {
                this.buffer.append("# ").append(comment).append("\n");
            }
            this.comments.clear();

            // Write key = value
            this.buffer.append(escapeKey(fieldName)).append(" = ");
            this.buffer.append(formatValue(value, type));
            this.buffer.append("\n");
            this.firstInSection = false;
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            ensureTableHeader();

            // Add blank line before comments for readability
            if (!firstInSection && !this.comments.isEmpty()) {
                this.buffer.append("\n");
            }

            // Write comments
            for (String comment: this.comments) {
                this.buffer.append("# ").append(comment).append("\n");
            }
            this.comments.clear();

            // Write array
            this.buffer.append(escapeKey(fieldName)).append(" = [");

            if (values.length > 0) {
                this.buffer.append("\n");
                for (int i = 0; i < values.length; i++) {
                    this.buffer.append("  ").append(formatValue(values[i], subType));
                    if (i < values.length - 1) {
                        this.buffer.append(",");
                    }
                    this.buffer.append("\n");
                }
            }

            this.buffer.append("]\n");
        }

        @Override
        public void push(String groupName) {
            this.group.push(groupName);
            this.tableHeaderWritten = false;
        }

        @Override
        public void pop() {
            if (this.group.isEmpty()) return;
            this.group.pop();
            this.tableHeaderWritten = false;
        }

        @Override
        public void close() throws IOException {
            this.writer.write(this.buffer.toString());
            this.writer.flush();
            this.writer.close();
        }

        private void ensureTableHeader() {
            String tableName = buildTableName();
            if (!tableName.equals(currentTable) || !tableHeaderWritten) {
                if (!buffer.isEmpty()) {
                    buffer.append("\n");
                }
                if (!tableName.isEmpty()) {
                    buffer.append("[").append(tableName).append("]\n");
                }
                currentTable = tableName;
                tableHeaderWritten = true;
                firstInSection = true;
            }
        }

        private String buildTableName() {
            if (group.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = group.iterator();
            while (it.hasNext()) {
                sb.append(escapeKey(it.next()));
                if (it.hasNext()) {
                    sb.append(".");
                }
            }
            return sb.toString();
        }

        private String escapeKey(String key) {
            // Simple keys don't need quotes
            if (key.matches("[A-Za-z0-9_-]+")) {
                return key;
            }
            // Quote and escape if needed
            return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        private String formatValue(String value, Class<?> type) {
            if (type == null) {
                return "\"" + escapeString(value) + "\"";
            }

            // Boolean
            if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                return value.toLowerCase();
            }

            // Numbers
            if (Number.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                double.class.isAssignableFrom(type) ||
                float.class.isAssignableFrom(type)) {
                return value;
            }

            // String (default)
            return "\"" + escapeString(value) + "\"";
        }

        private String escapeString(String str) {
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t")
                     .replace("\b", "\\b")
                     .replace("\f", "\\f");
        }
    }

    public static class FormatReader implements IFormatReader {
        private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        private final Stack<String> group = new Stack<>();
        private String currentTable = "";

        public FormatReader(Path path) throws IOException {
            char[] data = new String(Tools.readAllBytes(path), StandardCharsets.UTF_8).toCharArray();
            parseToml(data);
        }

        private void parseToml(char[] data) throws IOException {
            int i = 0;
            int len = data.length;

            while (i < len) {
                char c = data[i];

                // Skip whitespace
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                // Skip comments
                if (c == '#') {
                    i = skipToEndOfLine(data, i);
                    continue;
                }

                // Table header
                if (c == '[') {
                    i = parseTableHeader(data, i);
                    continue;
                }

                // Key-value pair
                if (isKeyStart(c)) {
                    i = parseKeyValue(data, i);
                    continue;
                }

                i++;
            }
        }

        private int parseTableHeader(char[] data, int start) throws IOException {
            int i = start + 1;
            int len = data.length;

            // Check for array of tables [[...]]
            boolean isArray = false;
            if (i < len && data[i] == '[') {
                isArray = true;
                i++;
            }

            // Skip whitespace
            while (i < len && Character.isWhitespace(data[i])) {
                i++;
            }

            // Parse table name
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

            // Skip closing bracket
            i++;
            if (isArray) {
                if (i >= len || data[i] != ']') {
                    throw new IOException("Expected ]] for array of tables");
                }
                i++;
            }

            currentTable = tableName.toString().trim();

            // Skip to end of line
            return skipToEndOfLine(data, i);
        }

        private int parseKeyValue(char[] data, int start) throws IOException {
            int i = start;
            int len = data.length;

            // Parse key
            StringBuilder key = new StringBuilder();
            i = parseKey(data, i, key);

            // Skip whitespace
            while (i < len && Character.isWhitespace(data[i])) {
                i++;
            }

            // Expect '='
            if (i >= len || data[i] != '=') {
                throw new IOException("Expected '=' after key");
            }

            // Skip whitespace
            do {
                i++;
            } while (i < len && Character.isWhitespace(data[i]));

            // Parse value
            String fullKey = buildFullKey(key.toString());
            i = parseValue(data, i, fullKey);

            return i;
        }

        private int parseKey(char[] data, int start, StringBuilder key) throws IOException {
            int i = start;
            int len = data.length;
            char c = data[i];

            // Quoted key
            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < len && data[i] != quote) {
                    if (data[i] == '\\' && i + 1 < len) {
                        i++;
                        key.append(unescapeChar(data[i]));
                    } else {
                        key.append(data[i]);
                    }
                    i++;
                }
                if (i >= len) {
                    throw new IOException("Unclosed quoted key");
                }
                i++; // Skip closing quote
            } else {
                // Bare key
                while (i < len && (Character.isLetterOrDigit(data[i]) || data[i] == '_' || data[i] == '-')) {
                    key.append(data[i]);
                    i++;
                }
            }

            return i;
        }

        private int parseValue(char[] data, int start, String key) throws IOException {
            int i = start;
            char c = data[i];

            // String
            if (c == '"' || c == '\'') {
                return parseString(data, i, key);
            }

            // Array
            if (c == '[') {
                return parseArray(data, i, key);
            }

            // Inline table
            if (c == '{') {
                return parseInlineTable(data, i, key);
            }

            // Boolean, number, or datetime
            return parseLiteral(data, i, key);
        }

        private int parseString(char[] data, int start, String key) throws IOException {
            int i = start;
            int len = data.length;
            char quote = data[i];
            i++;

            // Check for multi-line string
            if (i + 1 < len && data[i] == quote && data[i + 1] == quote) {
                i += 2;
                return parseMultilineString(data, i, key, quote);
            }

            StringBuilder value = new StringBuilder();
            while (i < len && data[i] != quote) {
                if (data[i] == '\\' && i + 1 < len) {
                    i++;
                    value.append(unescapeChar(data[i]));
                } else if (data[i] == '\n') {
                    throw new IOException("Newline not allowed in single-line string");
                } else {
                    value.append(data[i]);
                }
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed string");
            }

            i++; // Skip closing quote
            values.put(key, value.toString());
            return skipToEndOfLine(data, i);
        }

        private int parseMultilineString(char[] data, int start, String key, char quote) throws IOException {
            int i = start;
            int len = data.length;
            StringBuilder value = new StringBuilder();

            // Skip newline immediately after opening quotes
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
                        // Line ending backslash - trim whitespace
                        while (i < len && Character.isWhitespace(data[i])) {
                            i++;
                        }
                        continue;
                    }
                    value.append(unescapeChar(data[i]));
                } else {
                    value.append(data[i]);
                }
                i++;
            }

            throw new IOException("Unclosed multi-line string");
        }

        private int parseArray(char[] data, int start, String key) throws IOException {
            int i = start + 1;
            int len = data.length;
            List<String> array = new ArrayList<>();

            while (i < len) {
                // Skip whitespace and newlines
                while (i < len && (Character.isWhitespace(data[i]) || data[i] == '\n')) {
                    i++;
                }

                if (i >= len) {
                    throw new IOException("Unclosed array");
                }

                // Check for end of array
                if (data[i] == ']') {
                    i++;
                    values.put(key, array.toArray(new String[0]));
                    return skipToEndOfLine(data, i);
                }

                // Skip comments
                if (data[i] == '#') {
                    i = skipToEndOfLine(data, i);
                    continue;
                }

                // Parse array element
                StringBuilder element = new StringBuilder();
                i = parseArrayElement(data, i, element);
                array.add(element.toString());

                // Skip whitespace
                while (i < len && (Character.isWhitespace(data[i]) || data[i] == '\n')) {
                    i++;
                }

                // Check for comma
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

            // String
            if (c == '"' || c == '\'') {
                i++;
                while (i < len && data[i] != c) {
                    if (data[i] == '\\' && i + 1 < len) {
                        i++;
                        element.append(unescapeChar(data[i]));
                    } else {
                        element.append(data[i]);
                    }
                    i++;
                }
                if (i >= len) {
                    throw new IOException("Unclosed string in array");
                }
                i++;
                return i;
            }

            // Literal (number, boolean, etc.)
            while (i < len && data[i] != ',' && data[i] != ']' && data[i] != '\n' && data[i] != '#') {
                element.append(data[i]);
                i++;
            }

            // Trim trailing whitespace
            while (!element.isEmpty() && Character.isWhitespace(element.charAt(element.length() - 1))) {
                element.setLength(element.length() - 1);
            }

            return i;
        }

        private int parseInlineTable(char[] data, int start, String key) throws IOException {
            int i = start + 1;
            int len = data.length;

            while (i < len) {
                // Skip whitespace
                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                if (i >= len) {
                    throw new IOException("Unclosed inline table");
                }

                // Check for end of table
                if (data[i] == '}') {
                    i++;
                    return skipToEndOfLine(data, i);
                }

                // Parse key
                StringBuilder subKey = new StringBuilder();
                i = parseKey(data, i, subKey);

                // Skip whitespace
                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                // Expect '='
                if (i >= len || data[i] != '=') {
                    throw new IOException("Expected '=' in inline table");
                }

                // Skip whitespace
                do {
                    i++;
                } while (i < len && Character.isWhitespace(data[i]));

                // Parse value
                String fullKey = key + "." + subKey;
                i = parseValue(data, i, fullKey);

                // Skip whitespace
                while (i < len && Character.isWhitespace(data[i])) {
                    i++;
                }

                // Check for comma
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

            String literal = value.toString().trim();
            values.put(key, literal);
            return skipToEndOfLine(data, i);
        }

        private int skipToEndOfLine(char[] data, int start) {
            int i = start;
            int len = data.length;
            while (i < len && data[i] != '\n') {
                if (data[i] == '#') {
                    // Skip comment
                    while (i < len && data[i] != '\n') {
                        i++;
                    }
                    break;
                }
                if (!Character.isWhitespace(data[i])) {
                    // Allow only whitespace and comments after value
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

        private char unescapeChar(char c) {
            return switch (c) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case '\\' -> '\\';
                case '"' -> '"';
                case '\'' -> '\'';
                default -> c;
            };
        }

        private String buildFullKey(String key) {
            if (currentTable.isEmpty()) {
                return key;
            }
            return currentTable + "." + key;
        }

        @Override
        public String read(String fieldName) {
            String key = Tools.concat("", (!group.isEmpty() ? "." : "") + fieldName, '.', group);
            Object value = values.get(key);
            if (value instanceof String s) {
                return s;
            }
            return null;
        }

        @Override
        public String[] readArray(String fieldName) {
            String key = Tools.concat("", (!group.isEmpty() ? "." : "") + fieldName, '.', group);
            Object value = values.get(key);
            if (value instanceof String[] s) {
                return s;
            }
            return null;
        }

        @Override
        public void push(String group) {
            this.group.push(group);
        }

        @Override
        public void pop() {
            if (!this.group.isEmpty()) {
                this.group.pop();
            }
        }

        @Override
        public void close() {
            this.values.clear();
            this.group.clear();
        }
    }
}
