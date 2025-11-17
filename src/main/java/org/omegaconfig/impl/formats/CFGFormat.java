package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.Tools;
import org.omegaconfig.api.formats.IFormatCodec;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class CFGFormat implements IFormatCodec {
    @Override public String id() { return OmegaConfig.FORMAT_CFG; }
    @Override public String extension() { return "." + this.id(); }
    @Override public String mimeType() { return "text/x-cfg"; }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath);
    }

    @Override
    public IFormatWriter createWritter(Path filePath) throws IOException {
        return new FormatWriter(filePath);
    }

    public static class FormatWriter implements IFormatWriter {
        private final Stack<String> group = new Stack<>();
        private final BufferedWriter writer;
        private final StringBuilder buffer = new StringBuilder();
        private final List<String> comments = new ArrayList<>();
        private String currentSection = "";
        private boolean sectionHeaderWritten = false;

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
            ensureSectionHeader();

            // Write comments
            for (String comment : this.comments) {
                this.buffer.append("# ").append(comment).append("\n");
            }
            this.comments.clear();

            // Write key = value
            this.buffer.append(escapeKey(fieldName)).append(" = ");
            this.buffer.append(formatValue(value, type));
            this.buffer.append("\n");
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            ensureSectionHeader();

            // Write comments
            for (String comment : this.comments) {
                this.buffer.append("# ").append(comment).append("\n");
            }
            this.comments.clear();

            // Write array
            this.buffer.append(escapeKey(fieldName)).append(" = [");

            if (values.length > 0) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        this.buffer.append(", ");
                    }
                    this.buffer.append(formatValue(values[i], subType));
                }
            }

            this.buffer.append("]\n");
        }

        @Override
        public void push(String groupName) {
            this.group.push(groupName);
            this.sectionHeaderWritten = false;
        }

        @Override
        public void pop() {
            if (!this.group.isEmpty()) {
                this.group.pop();
                this.sectionHeaderWritten = false;
            }
        }

        @Override
        public void close() throws IOException {
            this.writer.write(this.buffer.toString());
            this.writer.flush();
            this.writer.close();
        }

        private void ensureSectionHeader() {
            String sectionName = buildSectionName();
            if (!sectionName.equals(currentSection) || !sectionHeaderWritten) {
                if (!buffer.isEmpty()) {
                    buffer.append("\n");
                }
                if (!sectionName.isEmpty()) {
                    buffer.append("[").append(sectionName).append("]\n");
                }
                currentSection = sectionName;
                sectionHeaderWritten = true;
            }
        }

        private String buildSectionName() {
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
                     .replace("\t", "\\t");
        }
    }

    public static class FormatReader implements IFormatReader {
        private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        private final Stack<String> group = new Stack<>();
        private String currentSection = "";

        public FormatReader(Path path) throws IOException {
            char[] data = new String(Tools.readAllBytes(path), StandardCharsets.UTF_8).toCharArray();
            parseCfg(data);
        }

        private void parseCfg(char[] data) throws IOException {
            int i = 0;
            int len = data.length;

            while (i < len) {
                i = skipWhitespace(data, i, len);
                if (i >= len) break;

                char c = data[i];

                // Skip comments
                if (c == '#' || (c == '/' && i + 1 < len && data[i + 1] == '/')) {
                    i = skipToEndOfLine(data, i, len);
                    continue;
                }

                // Section header
                if (c == '[') {
                    i = parseSectionHeader(data, i, len);
                    continue;
                }

                // Key-value pair
                if (isKeyStart(c)) {
                    i = parseKeyValue(data, i, len);
                    continue;
                }

                i++;
            }
        }

        private int parseSectionHeader(char[] data, int start, int len) throws IOException {
            int i = start + 1;

            // Skip whitespace
            i = skipWhitespace(data, i, len);

            // Parse section name
            StringBuilder sectionName = new StringBuilder();
            while (i < len && data[i] != ']') {
                if (data[i] == '\n') {
                    throw new IOException("Unclosed section header at position " + i);
                }
                sectionName.append(data[i]);
                i++;
            }

            if (i >= len) {
                throw new IOException("Unclosed section header");
            }

            // Skip closing bracket
            i++;

            currentSection = sectionName.toString().trim();

            // Skip to end of line
            return skipToEndOfLine(data, i, len);
        }

        private int parseKeyValue(char[] data, int start, int len) throws IOException {
            int i = start;

            // Parse key
            StringBuilder key = new StringBuilder();
            i = parseKey(data, i, len, key);

            // Skip whitespace
            i = skipWhitespace(data, i, len);

            // Expect '=' or ':'
            if (i >= len || (data[i] != '=' && data[i] != ':')) {
                throw new IOException("Expected '=' or ':' after key at position " + i);
            }

            // Skip separator
            i++;

            // Skip whitespace
            i = skipWhitespace(data, i, len);

            // Parse value
            String fullKey = buildFullKey(key.toString());
            i = parseValue(data, i, len, fullKey);

            return i;
        }

        private int parseKey(char[] data, int start, int len, StringBuilder key) throws IOException {
            int i = start;
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
                while (i < len && (Character.isLetterOrDigit(data[i]) || data[i] == '_' || data[i] == '-' || data[i] == '.')) {
                    key.append(data[i]);
                    i++;
                }
            }

            return i;
        }

        private int parseValue(char[] data, int start, int len, String key) throws IOException {
            int i = start;
            if (i >= len) {
                throw new IOException("Expected value after '=' at position " + i);
            }

            char c = data[i];

            // String
            if (c == '"' || c == '\'') {
                return parseString(data, i, len, key);
            }

            // Array
            if (c == '[') {
                return parseArray(data, i, len, key);
            }

            // Literal (boolean, number)
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
                } else if (data[i] == '\n') {
                    throw new IOException("Newline not allowed in single-line string at position " + i);
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
            return skipToEndOfLine(data, i, len);
        }

        private int parseArray(char[] data, int start, int len, String key) throws IOException {
            int i = start + 1;
            List<String> array = new ArrayList<>();

            while (i < len) {
                i = skipWhitespace(data, i, len);
                if (i >= len) {
                    throw new IOException("Unclosed array");
                }

                // Check for end of array
                if (data[i] == ']') {
                    i++;
                    values.put(key, array.toArray(new String[0]));
                    return skipToEndOfLine(data, i, len);
                }

                // Skip comments
                if (data[i] == '#' || (data[i] == '/' && i + 1 < len && data[i + 1] == '/')) {
                    i = skipToEndOfLine(data, i, len);
                    continue;
                }

                // Parse array element
                StringBuilder element = new StringBuilder();
                i = parseArrayElement(data, i, len, element);
                array.add(element.toString());

                // Skip whitespace
                i = skipWhitespace(data, i, len);

                // Check for comma
                if (i < len && data[i] == ',') {
                    i++;
                }
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

            // Literal (number, boolean)
            while (i < len && data[i] != ',' && data[i] != ']' && data[i] != '\n' && data[i] != '#' && !Character.isWhitespace(data[i])) {
                element.append(data[i]);
                i++;
            }

            return i;
        }

        private int parseLiteral(char[] data, int start, int len, String key) {
            int i = start;
            StringBuilder value = new StringBuilder();

            while (i < len && data[i] != '\n' && data[i] != '#' && !(data[i] == '/' && i + 1 < len && data[i + 1] == '/')) {
                if (!Character.isWhitespace(data[i])) {
                    value.append(data[i]);
                } else if (value.length() > 0) {
                    // Stop at first whitespace after non-whitespace content
                    break;
                }
                i++;
            }

            String literal = value.toString().trim();
            values.put(key, literal);
            return skipToEndOfLine(data, i, len);
        }

        private int skipWhitespace(char[] data, int start, int len) {
            int i = start;
            while (i < len && Character.isWhitespace(data[i]) && data[i] != '\n') {
                i++;
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
            if (currentSection.isEmpty()) {
                return key;
            }
            return currentSection + "." + key;
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
