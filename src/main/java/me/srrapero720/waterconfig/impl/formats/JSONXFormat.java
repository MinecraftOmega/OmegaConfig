package me.srrapero720.waterconfig.impl.formats;

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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Shared JSON-family codec. {@link JSONFormat} (strict JSON) and {@link JSON5Format} extend it
 * and only flip {@link #json5()}: JSON5 keeps comments and NaN/Infinity tokens and tolerates
 * unquoted keys, single quotes and trailing commas, while strict JSON drops comments and emits
 * {@code null} for non-representable floats.
 */
abstract class JSONXFormat extends BaseFormat {
    private static final char JSON_OBJECT_START = '{';
    private static final char JSON_OBJECT_END = '}';
    private static final char JSON_CONTINUE = ',';
    private static final char JSON_STRING_LINE = '"';
    private static final char JSON_ENTRY_SPLIT = ':';
    private static final char JSON_ARRAY_START = '[';
    private static final char JSON_ARRAY_END = ']';

    // TRUE FOR JSON5: KEEPS COMMENTS, RAW NaN/Infinity AND THE LENIENT PARSER EXTENSIONS
    protected abstract boolean json5();

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath, ParseMode.STRICT, json5());
    }

    @Override
    public IFormatReader createReader(Path filePath, ParseMode mode) throws IOException {
        return new FormatReader(filePath, mode, json5());
    }

    @Override
    public IFormatWriter createWriter(Path filePath) throws IOException {
        return new FormatWriter(filePath, json5());
    }

    static class FormatWriter implements IFormatWriter {
        private final Deque<String> group = new ArrayDeque<>();
        private final BufferedWriter writer;
        private final StringBuilder buffer = new StringBuilder();
        private final List<String> comments = new ArrayList<>();
        private final boolean json5;
        private boolean beginned = false;

        public FormatWriter(Path path, boolean json5) throws IOException {
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            this.json5 = json5;
        }

        @Override
        public void write(String comment) {
            // STRICT JSON HAS NO COMMENTS: ONLY JSON5 KEEPS THEM
            if (this.json5) {
                this.comments.add(comment);
            }
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            this.separate();
            this.flushComments();

            boolean isString = !Number.class.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type) && !boolean.class.isAssignableFrom(type);

            this.buffer.append(tabs(this.group.size()));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(escape(fieldName));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(' ');
            if (isString) {
                this.buffer.append(JSON_STRING_LINE);
                this.buffer.append(escape(value));
                this.buffer.append(JSON_STRING_LINE);
            } else {
                this.buffer.append(number(value));
            }
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            this.separate();
            this.flushComments();

            boolean isString = !Number.class.isAssignableFrom(subType) && !Boolean.class.isAssignableFrom(subType) && !boolean.class.isAssignableFrom(subType);

            this.buffer.append(tabs(this.group.size()));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(escape(fieldName));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(' ');
            this.buffer.append(JSON_ARRAY_START);
            this.buffer.append('\n');
            for (int i = 0; i < values.length; i++) {
                this.buffer.append(tabs(this.group.size() + 1));
                if (isString) {
                    this.buffer.append(JSON_STRING_LINE);
                    this.buffer.append(escape(values[i]));
                    this.buffer.append(JSON_STRING_LINE);
                } else {
                    this.buffer.append(number(values[i]));
                }
                if (i < values.length - 1) {
                    this.buffer.append(JSON_CONTINUE);
                }
                this.buffer.append('\n');
            }
            this.buffer.append(tabs(this.group.size()));
            this.buffer.append(JSON_ARRAY_END);
        }

        // STRICT JSON HAS NO NaN/Infinity LITERALS: EMIT null SO THE FILE STAYS VALID AND
        // SELF-READABLE. JSON5 SPELLS THEM DIRECTLY, SO IT KEEPS THE RAW TOKEN.
        private String number(String value) {
            if (this.json5) {
                return value;
            }
            return switch (value) {
                case "NaN", "Infinity", "+Infinity", "-Infinity" -> "null";
                default -> value;
            };
        }

        @Override
        public void push(String groupName) {
            if (this.group.isEmpty()) {
                // ROOT PUSH: WRITE PENDING COMMENTS BEFORE OPENING THE ROOT OBJECT
                for (String comment: this.comments) {
                    this.buffer.append("// ").append(comment).append('\n');
                }
                this.comments.clear();
                this.buffer.append(JSON_OBJECT_START);
                this.buffer.append('\n');
                this.group.addLast(groupName);
                this.beginned = false;
                return;
            }

            this.separate();
            this.flushComments();
            this.buffer.append(tabs(this.group.size()));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(escape(groupName));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(' ');
            this.buffer.append(JSON_OBJECT_START);
            this.buffer.append('\n');
            this.group.addLast(groupName);
            this.beginned = false;
        }

        @Override
        public void pop() {
            this.group.pollLast();
            this.buffer.append('\n');
            this.buffer.append(tabs(this.group.size()));
            this.buffer.append(JSON_OBJECT_END);
            // A CLOSED GROUP IS AN EMITTED ENTRY: THE NEXT SIBLING NEEDS A COMMA
            this.beginned = true;
        }

        // EMITS THE SEPARATOR BETWEEN SIBLING ENTRIES, WITH A BLANK LINE BEFORE COMMENTED ONES
        private void separate() {
            if (this.beginned) {
                this.buffer.append(JSON_CONTINUE);
                this.buffer.append('\n');
                if (!this.comments.isEmpty()) {
                    this.buffer.append('\n');
                }
            } else {
                this.beginned = true;
            }
        }

        private void flushComments() {
            for (String comment: this.comments) {
                this.buffer.append(tabs(this.group.size()));
                this.buffer.append("// ").append(comment).append('\n');
            }
            this.comments.clear();
        }

        @Override
        public void close() throws IOException {
            this.buffer.append('\n');
            this.writer.write(this.buffer.toString());
            this.writer.flush();
            this.writer.close();
        }
    }

    static class FormatReader extends BaseFormatReader {
        public FormatReader(Path path, ParseMode mode, boolean json5) throws IOException {
            Parser.parse(read(path), json5, this.values, this.comments, mode, this.report);
        }
    }

    /**
     * Recursive-descent parser shared by JSON and JSON5. Fills a dotted-path map with
     * String scalars (raw tokens for non-strings) and String[] arrays, insertion-ordered.
     * Strict mode throws on truncated files, unclosed braces and trailing content; repair
     * mode records the problem and resyncs to the next {@code ,}/{@code '}'}/{@code ]}
     * at the same depth.
     */
    private static final class Parser {
        // BOUNDS RECURSION SO A MALICIOUSLY DEEP FILE FAILS CLEANLY INSTEAD OF BLOWING THE STACK
        private static final int MAX_DEPTH = 512;

        private final char[] data;
        private final boolean json5;
        private final boolean repair;
        private final List<RepairEntry> report;
        private final LinkedHashMap<String, Object> values;
        private final LinkedHashMap<String, List<String>> comments;
        private final List<String> pending = new ArrayList<>();
        private int i;
        private int depth;

        private Parser(char[] data, boolean json5, boolean repair, List<RepairEntry> report, LinkedHashMap<String, Object> values, LinkedHashMap<String, List<String>> comments) {
            this.data = data;
            this.json5 = json5;
            this.repair = repair;
            this.report = report;
            this.values = values;
            this.comments = comments;
        }

        static void parse(char[] data, boolean json5, LinkedHashMap<String, Object> into, LinkedHashMap<String, List<String>> comments, ParseMode mode, List<RepairEntry> report) throws IOException {
            Parser p = new Parser(data, json5, mode == ParseMode.REPAIR, report, into, comments);

            if (p.repair) {
                try {
                    p.skipVoid();
                    p.expect('{');
                    p.attachPending("");
                } catch (IOException e) {
                    report.add(new RepairEntry(p.line(), e.getMessage(), "treated as an empty spec"));
                    return;
                }
                p.parseObject("");
                try {
                    p.skipVoid();
                    if (p.i < data.length) {
                        throw new IOException("Unexpected trailing content at position " + p.i);
                    }
                } catch (IOException e) {
                    report.add(new RepairEntry(p.line(), e.getMessage(), "trailing content ignored"));
                }
                return;
            }

            p.skipVoid();
            p.expect('{');
            p.attachPending("");
            p.parseObject("");
            p.skipVoid();
            if (p.i < data.length) {
                throw new IOException("Unexpected trailing content at position " + p.i);
            }
        }

        // COMMENTS SEEN ABOVE THE UPCOMING ENTRY ATTACH TO ITS DOTTED KEY ("" = FILE LEVEL)
        private void attachPending(String key) {
            if (!pending.isEmpty()) {
                comments.put(key, List.copyOf(pending));
                pending.clear();
            }
        }

        private void parseObject(String prefix) throws IOException {
            if (++depth > MAX_DEPTH) {
                depth--;
                throw new IOException("Objects nested deeper than " + MAX_DEPTH + " levels");
            }
            try {
                parseObject0(prefix);
            } finally {
                depth--;
            }
        }

        private void parseObject0(String prefix) throws IOException {
            while (true) {
                try {
                    skipVoid();
                    if (i >= data.length) {
                        throw new IOException("Unclosed object (EOF)");
                    }
                    if (data[i] == '}') {
                        i++;
                        return;
                    }

                    String key = parseKey();
                    skipVoid();
                    expect(':');
                    skipVoid();
                    parseValue(prefix + key);
                    skipVoid();
                    if (i >= data.length) {
                        throw new IOException("Unclosed object (EOF)");
                    }

                    char c = data[i];
                    if (c == ',') {
                        i++;
                        skipVoid();
                        // TRAILING COMMAS ARE JSON5-ONLY
                        if (i < data.length && data[i] == '}' && !json5) {
                            throw new IOException("Trailing comma at position " + i);
                        }
                        continue;
                    }
                    if (c == '}') {
                        i++;
                        return;
                    }
                    throw new IOException("Expected ',' or '}' at position " + i + " but found '" + c + "'");
                } catch (IOException e) {
                    if (!repair) {
                        throw e;
                    }
                    report.add(new RepairEntry(line(), e.getMessage(), "skipped to the next entry"));
                    if (!resync('}')) {
                        return;
                    }
                }
            }
        }

        private String parseKey() throws IOException {
            char c = data[i];
            if (c == '"' || (json5 && c == '\'')) {
                return parseQuoted(c);
            }
            // JSON5 UNQUOTED IDENTIFIER KEYS
            if (json5 && (Character.isLetter(c) || c == '_' || c == '$')) {
                StringBuilder key = new StringBuilder();
                while (i < data.length && (Character.isLetterOrDigit(data[i]) || data[i] == '_' || data[i] == '$')) {
                    key.append(data[i]);
                    i++;
                }
                return key.toString();
            }
            throw new IOException("Expected a key at position " + i + " but found '" + c + "'");
        }

        private String parseQuoted(char quote) throws IOException {
            i++; // SKIP OPENING QUOTE
            StringBuilder out = new StringBuilder();
            while (i < data.length) {
                char c = data[i];
                if (c == quote) {
                    i++;
                    return out.toString();
                }
                if (c == '\\') {
                    if (i + 1 >= data.length) {
                        break;
                    }
                    i = unescape(data, i + 1, out, false);
                    continue;
                }
                out.append(c);
                i++;
            }
            throw new IOException("Unclosed string (EOF)");
        }

        private void parseValue(String key) throws IOException {
            attachPending(key);
            char c = data[i];
            if (c == '"' || (json5 && c == '\'')) {
                values.put(key, parseQuoted(c));
            } else if (c == '{') {
                i++;
                parseObject(key + ".");
            } else if (c == '[') {
                i++;
                parseArray(key);
            } else {
                values.put(key, parseToken());
            }
        }

        private void parseArray(String key) throws IOException {
            List<String> elements = new ArrayList<>();
            boolean open = true;
            while (open) {
                try {
                    skipVoid();
                    if (i >= data.length) {
                        throw new IOException("Unclosed array (EOF)");
                    }
                    if (data[i] == ']') {
                        i++;
                        break;
                    }

                    char c = data[i];
                    if (c == '{' || c == '[') {
                        throw new IOException("Nested structures in arrays are not supported (position " + i + ")");
                    }
                    elements.add((c == '"' || (json5 && c == '\'')) ? parseQuoted(c) : parseToken());

                    skipVoid();
                    if (i >= data.length) {
                        throw new IOException("Unclosed array (EOF)");
                    }
                    c = data[i];
                    if (c == ',') {
                        i++;
                        skipVoid();
                        // TRAILING COMMAS ARE JSON5-ONLY
                        if (i < data.length && data[i] == ']' && !json5) {
                            throw new IOException("Trailing comma at position " + i);
                        }
                        continue;
                    }
                    if (c == ']') {
                        i++;
                        break;
                    }
                    throw new IOException("Expected ',' or ']' at position " + i + " but found '" + c + "'");
                } catch (IOException e) {
                    if (!repair) {
                        throw e;
                    }
                    report.add(new RepairEntry(line(), e.getMessage(), "skipped to the next element"));
                    open = resync(']');
                }
            }
            values.put(key, elements.toArray(new String[0]));
        }

        // REPAIR-MODE RECOVERY: SKIPS THE BROKEN REGION UP TO THE NEXT ',' (RETURNS TRUE TO CONTINUE)
        // OR THE CLOSING CHAR AT THIS DEPTH (RETURNS FALSE TO STOP). TRACKS NESTED BRACES, BRACKETS
        // AND STRINGS SO STRUCTURAL CHARS INSIDE THEM NEVER RESYNC EARLY
        private boolean resync(char closing) {
            int depth = 0;
            boolean inString = false;
            char quote = 0;
            while (i < data.length) {
                char c = data[i];
                if (inString) {
                    if (c == '\\') {
                        i += 2;
                        continue;
                    }
                    if (c == quote) {
                        inString = false;
                    }
                    i++;
                    continue;
                }
                switch (c) {
                    case '"' -> {
                        inString = true;
                        quote = c;
                    }
                    case '\'' -> {
                        if (json5) {
                            inString = true;
                            quote = c;
                        }
                    }
                    case '{', '[' -> depth++;
                    case '}', ']' -> {
                        if (depth == 0) {
                            if (c == closing) {
                                i++;
                                return false;
                            }
                            // A FOREIGN CLOSER AT THIS DEPTH BELONGS TO THE PARENT: STOP WITHOUT CONSUMING
                            return false;
                        }
                        depth--;
                    }
                    case ',' -> {
                        if (depth == 0) {
                            i++;
                            return true;
                        }
                    }
                }
                i++;
            }
            return false;
        }

        // COLLECTS A RAW UNQUOTED TOKEN UP TO THE NEXT STRUCTURAL DELIMITER; VALUES STAY UNPARSED
        private String parseToken() throws IOException {
            StringBuilder out = new StringBuilder();
            while (i < data.length) {
                char c = data[i];
                if (c == ',' || c == '}' || c == ']' || c == '{' || c == '[' || c == '"' || c == '\'') {
                    break;
                }
                if (json5 && c == '/' && i + 1 < data.length && (data[i + 1] == '/' || data[i + 1] == '*')) {
                    break;
                }
                out.append(c);
                i++;
            }

            // THE TOKEN ENDS AT THE DELIMITER: DROP THE WHITESPACE RUN BEFORE IT
            int end = out.length();
            while (end > 0 && Character.isWhitespace(out.charAt(end - 1))) {
                end--;
            }
            out.setLength(end);

            if (out.isEmpty()) {
                throw new IOException("Expected a value at position " + i);
            }

            String token = out.toString();
            if (!json5 && (token.equals("NaN") || token.equals("Infinity") || token.equals("+Infinity") || token.equals("-Infinity"))) {
                throw new IOException("'" + token + "' is not a legal JSON token");
            }
            return token;
        }

        // SKIPS WHITESPACE, PLUS LINE AND BLOCK COMMENTS IN JSON5 MODE
        private void skipVoid() throws IOException {
            while (i < data.length) {
                char c = data[i];
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }
                if (json5 && c == '/' && i + 1 < data.length) {
                    if (data[i + 1] == '/') {
                        i += 2;
                        int start = i;
                        while (i < data.length && data[i] != '\n') i++;
                        pending.add(new String(data, start, i - start).trim());
                        continue;
                    }
                    if (data[i + 1] == '*') {
                        i += 2;
                        int start = i;
                        while (true) {
                            if (i + 1 >= data.length) {
                                throw new IOException("Unterminated block comment");
                            }
                            if (data[i] == '*' && data[i + 1] == '/') {
                                pending.add(new String(data, start, i - start).trim());
                                i += 2;
                                break;
                            }
                            i++;
                        }
                        continue;
                    }
                }
                break;
            }
        }

        private void expect(char c) throws IOException {
            if (i >= data.length) {
                throw new IOException("Expected '" + c + "' but reached EOF");
            }
            if (data[i] != c) {
                throw new IOException("Expected '" + c + "' at position " + i + " but found '" + data[i] + "'");
            }
            i++;
        }

        private int line() {
            return BaseFormatReader.lineOf(data, i);
        }
    }
}
