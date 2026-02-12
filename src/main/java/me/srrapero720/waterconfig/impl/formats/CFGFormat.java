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

/**
 * CFG Format implementation - a superset of JSON with additional features.
 * CFG supports:
 * - Comments with #
 * - Keys as strings or identifiers
 * - Separators: : or =
 * - Newlines as separators (trailing commas optional)
 * - Nested mappings and lists
 * - Cross-references ${...}
 * - Include directives @'file.cfg'
 */
public class CFGFormat implements IFormatCodec {
    @Override public String id() { return WaterConfig.FORMAT_CFG; }
    @Override public String extension() { return "." + this.id(); }
    @Override public String mimeType() { return "text/x-cfg"; }

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
        private boolean firstInMapping = true;
        private int indentLevel = 0;

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
            // Add blank line before comments (if not first and has comments)
            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append("\n");
            }

            writeComments();

            // Write key
            indent();
            this.buffer.append(formatKey(fieldName)).append(": ");
            this.buffer.append(formatValue(value, type));
            this.buffer.append("\n");

            firstInMapping = false;
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            // Add blank line before comments (if not first and has comments)
            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append("\n");
            }

            writeComments();

            // Write key
            indent();
            this.buffer.append(formatKey(fieldName)).append(": ");

            // Write array
            this.buffer.append("[");
            if (values.length > 0) {
                this.buffer.append("\n");
                for (int i = 0; i < values.length; i++) {
                    indent();
                    this.buffer.append("  ").append(formatValue(values[i], subType));
                    if (i < values.length - 1) {
                        this.buffer.append(",");
                    }
                    this.buffer.append("\n");
                }
                indent();
            }
            this.buffer.append("]");
            this.buffer.append("\n");

            firstInMapping = false;
        }

        @Override
        public void push(String groupName) {
            if (this.group.isEmpty()) {
                // Root push: write pending comments before opening brace
                writeComments();
                this.buffer.append("{\n");
                this.group.push(groupName);
                this.indentLevel++;
                this.firstInMapping = true;
                return;
            }

            // Add blank line before comments (if not first and has comments)
            if (!firstInMapping && !comments.isEmpty()) {
                this.buffer.append("\n");
            }

            writeComments();

            indent();
            this.buffer.append(formatKey(groupName)).append(": {\n");

            this.group.push(groupName);
            this.indentLevel++;
            this.firstInMapping = true;
        }

        @Override
        public void pop() {
            if (!this.group.isEmpty()) {
                this.group.pop();
                this.indentLevel--;
                this.buffer.append("\n");
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
            for (String comment : this.comments) {
                indent();
                this.buffer.append("# ").append(comment).append("\n");
            }
            this.comments.clear();
        }

        private void indent() {
            this.buffer.append("  ".repeat(this.indentLevel));
        }

        private String formatKey(String key) {
            // Use identifier if possible (alphanumeric + underscore)
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return key;
            }
            // Otherwise use quoted string
            return "\"" + escapeString(key) + "\"";
        }

        private String formatValue(String value, Class<?> type) {
            if (type == null) {
                return "\"" + escapeString(value) + "\"";
            }

            // Boolean
            if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                return value.toLowerCase();
            }

            // Null
            if (value == null || value.equals("null")) {
                return "null";
            }

            // Numbers
            if (Number.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                double.class.isAssignableFrom(type) ||
                float.class.isAssignableFrom(type) ||
                byte.class.isAssignableFrom(type) ||
                short.class.isAssignableFrom(type)) {
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
        private final Map<String, Object> rawParsedData = new LinkedHashMap<>();

        public FormatReader(Path path) throws IOException {
            char[] data = new String(Tools.readAllBytes(path), StandardCharsets.UTF_8).toCharArray();
            parseCfg(data);
        }

        private void parseCfg(char[] data) throws IOException {
            int i = skipWhitespaceAndComments(data, 0, data.length);

            // CFG top-level should be a mapping
            if (i >= data.length || data[i] != '{') {
                throw new IOException("CFG file must start with '{'");
            }

            i++; // Skip opening brace
            parseMapping(data, i, data.length, "");
        }

        private int parseMapping(char[] data, int start, int len, String prefix) throws IOException {
            int i = start;
            boolean firstEntry = true;

            while (i < len) {
                i = skipWhitespaceAndComments(data, i, len);
                if (i >= len) break;

                // Check for end of mapping
                if (data[i] == '}') {
                    return i + 1;
                }

                // Skip comma or newline separator
                if (!firstEntry && (data[i] == ',' || data[i] == '\n')) {
                    i++;
                    i = skipWhitespaceAndComments(data, i, len);
                    if (i >= len) break;
                    if (data[i] == '}') {
                        return i + 1;
                    }
                }

                firstEntry = false;

                // Parse key
                StringBuilder key = new StringBuilder();
                i = parseKey(data, i, len, key);

                // Skip whitespace
                i = skipWhitespaceAndComments(data, i, len);

                // Expect ':' or '='
                if (i >= len || (data[i] != ':' && data[i] != '=')) {
                    throw new IOException("Expected ':' or '=' after key at position " + i);
                }
                i++; // Skip separator

                // Skip whitespace
                i = skipWhitespaceAndComments(data, i, len);

                // Parse value
                String fullKey = prefix.isEmpty() ? key.toString() : prefix + "." + key.toString();
                i = parseValue(data, i, len, fullKey);
            }

            return i;
        }

        private int parseKey(char[] data, int start, int len, StringBuilder key) throws IOException {
            int i = start;
            char c = data[i];

            // Quoted key (single or double quotes)
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
                // Identifier key
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
            int i = start;
            if (i >= len) {
                throw new IOException("Expected value after separator at position " + i);
            }

            char c = data[i];

            // String (single or double quotes)
            if (c == '"' || c == '\'') {
                return parseString(data, i, len, key);
            }

            // Array
            if (c == '[') {
                return parseArray(data, i, len, key);
            }

            // Nested mapping
            if (c == '{') {
                i++; // Skip opening brace
                return parseMapping(data, i, len, key);
            }

            // Cross-reference ${...}
            if (c == '$' && i + 1 < len && data[i + 1] == '{') {
                return parseReference(data, i, len, key);
            }

            // Include @'file'
            if (c == '@') {
                return parseInclude(data, i, len, key);
            }

            // Special values `...`
            if (c == '`') {
                return parseSpecialValue(data, i, len, key);
            }

            // Literal (boolean, number, null)
            return parseLiteral(data, i, len, key);
        }

        private int parseString(char[] data, int start, int len, String key) throws IOException {
            int i = start;
            char quote = data[i];
            i++;

            StringBuilder value = new StringBuilder();
            while (i < len && data[i] != quote) {
                if (data[i] == '\\' && i + 1 < len) {
                    i++;
                    value.append(unescapeChar(data[i]));
                } else {
                    value.append(data[i]);
                }
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed string at position " + i);
            }

            i++; // Skip closing quote
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

                // Check for end of array
                if (data[i] == ']') {
                    i++;
                    values.put(key, array.toArray(new String[0]));
                    return i;
                }

                // Skip comma or newline separator
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

                // Parse array element
                StringBuilder element = new StringBuilder();
                i = parseArrayElement(data, i, len, element);
                array.add(element.toString());
            }

            throw new IOException("Unclosed array");
        }

        private int parseArrayElement(char[] data, int start, int len, StringBuilder element) throws IOException {
            int i = start;
            char c = data[i];

            // String
            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < len && data[i] != quote) {
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

            // Literal (number, boolean, null)
            while (i < len && !Character.isWhitespace(data[i]) && data[i] != ',' && data[i] != ']' && data[i] != '#') {
                element.append(data[i]);
                i++;
            }

            return i;
        }

        private int parseLiteral(char[] data, int start, int len, String key) throws IOException {
            int i = start;
            StringBuilder value = new StringBuilder();

            while (i < len && data[i] != '\n' && data[i] != '\r' && data[i] != ',' && data[i] != '}' && data[i] != ']' && data[i] != '#') {
                value.append(data[i]);
                i++;
            }

            String literal = value.toString().trim();
            values.put(key, literal);
            return i;
        }

        private int parseReference(char[] data, int start, int len, String key) throws IOException {
            // Parse ${...} cross-reference
            // For now, store as string for basic support
            int i = start + 2; // Skip ${
            StringBuilder ref = new StringBuilder("${");

            while (i < len && data[i] != '}') {
                ref.append(data[i]);
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed reference");
            }

            ref.append('}');
            i++; // Skip closing }

            values.put(key, ref.toString());
            return i;
        }

        private int parseInclude(char[] data, int start, int len, String key) throws IOException {
            // Parse @'file.cfg' include directive
            // For now, store as string for basic support
            int i = start + 1; // Skip @

            i = skipWhitespaceAndComments(data, i, len);
            if (i >= len || (data[i] != '"' && data[i] != '\'')) {
                throw new IOException("Expected quoted filename after @ at position " + i);
            }

            char quote = data[i];
            i++;
            StringBuilder filename = new StringBuilder("@");
            filename.append(quote);

            while (i < len && data[i] != quote) {
                filename.append(data[i]);
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed include filename");
            }

            filename.append(quote);
            i++; // Skip closing quote

            values.put(key, filename.toString());
            return i;
        }

        private int parseSpecialValue(char[] data, int start, int len, String key) throws IOException {
            // Parse `...` special values
            // For now, store as string for basic support
            int i = start + 1; // Skip opening `
            StringBuilder special = new StringBuilder("`");

            while (i < len && data[i] != '`') {
                special.append(data[i]);
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed special value");
            }

            special.append('`');
            i++; // Skip closing `

            values.put(key, special.toString());
            return i;
        }

        private int skipWhitespaceAndComments(char[] data, int start, int len) {
            int i = start;
            while (i < len) {
                // Skip whitespace
                if (Character.isWhitespace(data[i])) {
                    i++;
                    continue;
                }

                // Skip comments
                if (data[i] == '#') {
                    i = skipToEndOfLine(data, i, len);
                    continue;
                }

                break;
            }
            return i;
        }

        private int skipToEndOfLine(char[] data, int start, int len) {
            int i = start;
            while (i < len && data[i] != '\n') {
                i++;
            }
            if (i < len && data[i] == '\n') {
                i++;
            }
            return i;
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
