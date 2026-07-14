package me.srrapero720.waterconfig.formats;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.formats.JSON5Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class JSON5FormatTest {

    @TempDir
    Path tempDir;

    // UNICODE SAMPLE (ACCENT, CJK, EMOJI) BUILT FROM CODEPOINTS SO THIS SOURCE FILE STAYS PURE ASCII
    private static final String UNICODE = "caf" + (char) 0xE9 + " " + (char) 0x4F60 + (char) 0x597D + " " + new String(Character.toChars(0x1F600));

    // WRITES THE SHARED REPRESENTATIVE SPEC: COMMENTS, SCALARS, NESTED GROUP AND STRING ARRAY
    private void writeSpec(IFormatWriter writer) throws IOException {
        writer.write("Test spec");
        writer.write("With multiple comments");
        writer.push("test_spec");

        writer.write("count", "42", Integer.class, null);
        writer.write("label", "hello world", String.class, null);
        writer.write("enabled", "true", Boolean.class, null);
        writer.write("ratio", "3.14", Double.class, null);

        writer.write("Nested section");
        writer.push("nested");
        writer.write("description", "inner", String.class, null);
        writer.write("weight", "0.5", Double.class, null);
        writer.pop();

        writer.write("tags", new String[]{"alpha", "beta"}, String[].class, String.class);
        writer.pop();
        writer.close();
    }

    // WRITES RAW CONTENT TO A TEMP FILE AND OPENS A READER OVER IT
    private IFormatReader open(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return new JSON5Format().createReader(file);
    }

    // WRITES A SINGLE STRING FIELD THROUGH THE WRITER AND READS IT BACK
    private String roundTrip(String name, String value) throws IOException {
        Path file = tempDir.resolve(name);
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("field", value, String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        String result = reader.read("field");
        reader.close();
        return result;
    }

    // ========================================================================
    // BASE CASES
    // ========================================================================

    @Test
    void testWriterOutput() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        String output = Files.readString(file, StandardCharsets.UTF_8);
        String expected = """
                // Test spec
                // With multiple comments
                {
                \t"count": 42,
                \t"label": "hello world",
                \t"enabled": true,
                \t"ratio": 3.14,

                \t// Nested section
                \t"nested": {
                \t\t"description": "inner",
                \t\t"weight": 0.5
                \t},
                \t"tags": [
                \t\t"alpha",
                \t\t"beta"
                \t]
                }
                """;
        assertEquals(expected, output);
    }

    @Test
    void testReaderScalars() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));
        reader.close();
    }

    @Test
    void testReaderNested() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        IFormatReader reader = new JSON5Format().createReader(file);
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();
        reader.close();
    }

    @Test
    void testReaderArray() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        IFormatReader reader = new JSON5Format().createReader(file);
        String[] tags = reader.readArray("tags");
        assertNotNull(tags);
        assertArrayEquals(new String[]{"alpha", "beta"}, tags);
        reader.close();
    }

    @Test
    void testFullRoundTrip() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));

        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();

        String[] tags = reader.readArray("tags");
        assertNotNull(tags);
        assertArrayEquals(new String[]{"alpha", "beta"}, tags);
        reader.close();
    }

    @Test
    void testGroupsOnlyRoundTrip() throws IOException {
        // GROUPS-ONLY SPECS (NO ROOT SCALARS) EXPOSED ROOT-EMISSION BUGS IN OTHER FORMATS, KEEP COVERAGE HERE
        Path file = tempDir.resolve("groups.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("watermedia");
        writer.write("DecodersAPI settings");
        writer.push("decoders");
        writer.write("failOnCorrupted", "true", Boolean.class, null);
        writer.pop();
        writer.write("NetworkAPI settings");
        writer.push("network");
        writer.write("serverPort", "25580", Integer.class, null);
        writer.write("remoteHost", "http://localhost:25580/", String.class, null);
        writer.pop();
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        reader.push("decoders");
        assertEquals("true", reader.read("failOnCorrupted"));
        reader.pop();
        reader.push("network");
        assertEquals("25580", reader.read("serverPort"));
        assertEquals("http://localhost:25580/", reader.read("remoteHost"));
        reader.pop();
        reader.close();
    }

    // ========================================================================
    // COMMENTS
    // ========================================================================

    @Test
    void testLineComments() throws IOException {
        IFormatReader reader = open("line_comments.json5", """
                // Root line comment
                {
                    // Comment before value
                    "count": 42,
                    "label": "hello world",
                    // Comment between values
                    "enabled": "true",
                    // Comment before nested group
                    "nested": {
                        // Nested line comment
                        "description": "inner"
                    }
                }
                // Trailing comment after root
                """);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        reader.pop();
        reader.close();
    }

    @Test
    void testBlockComments() throws IOException {
        IFormatReader reader = open("block_comments.json5", """
                /* Root block comment */
                {
                    /* Single-line block comment */
                    "count": 42,
                    /*
                     * Multi-line
                     * block comment
                     */
                    "label": "hello world",
                    "enabled": "true"
                }
                """);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        reader.close();
    }

    @Test
    void testMixedComments() throws IOException {
        IFormatReader reader = open("mixed_comments.json5", """
                // Line comment before root
                /* Block comment before root */
                {
                    // Line comment
                    "count": 42,
                    /* Block comment */
                    "label": "hello world"
                }
                """);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        reader.close();
    }

    @Test
    void testCommentsEverywhere() throws IOException {
        // JSON5 ALLOWS COMMENTS WHEREVER WHITESPACE IS ALLOWED
        IFormatReader reader = open("everywhere.json5", """
                // LINE BEFORE ROOT
                /* BLOCK BEFORE ROOT */
                { // AFTER ROOT BRACE
                    /* BEFORE KEY */ "count" /* AFTER KEY */ : /* BEFORE VALUE */ 42 /* AFTER VALUE */,
                    "label": "hello" /* AFTER STRING VALUE */,
                    "tags": [ /* AFTER ARRAY START */
                        // BEFORE ELEMENT
                        "a",
                        // AFTER ELEMENT COMMA
                        "b"
                    ],
                    "nested": { // AFTER GROUP START
                        "inner": true // BEFORE GROUP END
                    } /* AFTER GROUP END */
                } // AFTER ROOT END
                """);
        assertEquals("42", reader.read("count"));
        assertEquals("hello", reader.read("label"));
        assertArrayEquals(new String[]{"a", "b"}, reader.readArray("tags"));
        reader.push("nested");
        assertEquals("true", reader.read("inner"));
        reader.pop();
        reader.close();
    }

    @Test
    void testCommentBetweenArrayElementAndComma() throws IOException {
        IFormatReader reader = open("array_comment.json5", """
                {
                    "tags": ["a" /* BETWEEN ELEMENT AND COMMA */, "b"]
                }
                """);
        assertArrayEquals(new String[]{"a", "b"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testUnterminatedBlockCommentThrows() throws IOException {
        Path file = tempDir.resolve("unterminated.json5");
        Files.writeString(file, "{\"a\": 1} /* NEVER CLOSED");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    @Test
    void testSlashInStringValues() throws IOException {
        IFormatReader reader = open("slash_strings.json5", """
                {
                    "url": "https://example.com",
                    "path": "a/b/c"
                }
                """);
        assertEquals("https://example.com", reader.read("url"));
        assertEquals("a/b/c", reader.read("path"));
        reader.close();
    }

    // ========================================================================
    // STRING CONTENT EDGE CASES (READER)
    // ========================================================================

    @Test
    void testReadEscapedQuote() throws IOException {
        IFormatReader reader = open("esc_quote.json5", "{\"v\": \"say \\\"hi\\\"\"}");
        assertEquals("say \"hi\"", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEscapedBackslash() throws IOException {
        IFormatReader reader = open("esc_backslash.json5", "{\"v\": \"a\\\\b\"}");
        assertEquals("a\\b", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEscapedNewlineAndTab() throws IOException {
        IFormatReader reader = open("esc_ctrl.json5", "{\"v\": \"l1\\nl2\\tend\"}");
        assertEquals("l1\nl2\tend", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadUnicodeEscape() throws IOException {
        // FILE GETS LITERAL BACKSLASH-U SEQUENCES, EXPECTED VALUE IS THE SPEC-DECODED TEXT
        IFormatReader reader = open("esc_unicode.json5", "{\"v\": \"caf\\u00e9 \\u4f60\\u597d \\uD83D\\uDE00\"}");
        assertEquals(UNICODE, reader.read("v"));
        reader.close();
    }

    @Test
    void testReadRawUnicode() throws IOException {
        // FILE IS WRITTEN AND READ AS UTF-8 WITH REAL UNICODE CHARS, INCLUDING SURROGATE PAIRS
        IFormatReader reader = open("raw_unicode.json5", "{\"v\": \"" + UNICODE + "\"}");
        assertEquals(UNICODE, reader.read("v"));
        reader.close();
    }

    @Test
    void testReadCommentMarkersInString() throws IOException {
        // COMMENT DETECTION MUST BE DISABLED INSIDE QUOTED STRINGS
        IFormatReader reader = open("markers.json5", """
                {
                    "url": "https://example.com",
                    "note": "not /* a comment */ nor // one"
                }
                """);
        assertEquals("https://example.com", reader.read("url"));
        assertEquals("not /* a comment */ nor // one", reader.read("note"));
        reader.close();
    }

    @Test
    void testReadStructuralCharsInString() throws IOException {
        IFormatReader reader = open("structural.json5", "{\"v\": \"a{b}c[d]e,f:g\"}");
        assertEquals("a{b}c[d]e,f:g", reader.read("v"));
        reader.close();
    }

    // ========================================================================
    // VALUE EDGE CASES (READER)
    // ========================================================================

    @Test
    void testReadEmptyString() throws IOException {
        IFormatReader reader = open("empty_string.json5", "{\"v\": \"\"}");
        assertEquals("", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEmptyArray() throws IOException {
        IFormatReader reader = open("empty_array.json5", "{\"v\": []}");
        assertArrayEquals(new String[0], reader.readArray("v"));
        reader.close();
    }

    @Test
    void testReadSingleElementArray() throws IOException {
        IFormatReader reader = open("single_array.json5", "{\"v\": [\"only\"]}");
        assertArrayEquals(new String[]{"only"}, reader.readArray("v"));
        reader.close();
    }

    @Test
    void testReadNumberFormats() throws IOException {
        // READER IS STRING-BASED: NUMERIC TOKENS ARE RETURNED RAW AND UNPARSED
        IFormatReader reader = open("numbers.json5", """
                {
                    "neg": -42,
                    "exp": 1e5,
                    "expSigned": -2.5e-3,
                    "max": 9223372036854775807,
                    "precise": 3.14159265358979323846264338327,
                    "padded": 007
                }
                """);
        assertEquals("-42", reader.read("neg"));
        assertEquals("1e5", reader.read("exp"));
        assertEquals("-2.5e-3", reader.read("expSigned"));
        assertEquals("9223372036854775807", reader.read("max"));
        assertEquals("3.14159265358979323846264338327", reader.read("precise"));
        assertEquals("007", reader.read("padded"));
        reader.close();
    }

    @Test
    void testReadBooleansAndNull() throws IOException {
        IFormatReader reader = open("bools.json5", "{\"t\": true, \"f\": false, \"n\": null}");
        assertEquals("true", reader.read("t"));
        assertEquals("false", reader.read("f"));
        assertEquals("null", reader.read("n"));
        reader.close();
    }

    @Test
    void testReadKeysWithDotsAndSpaces() throws IOException {
        IFormatReader reader = open("keys.json5", "{\"a.b\": 1, \"my key\": \"x\"}");
        assertEquals("1", reader.read("a.b"));
        assertEquals("x", reader.read("my key"));
        reader.close();
    }

    @Test
    void testMissingKeyReturnsNull() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        IFormatReader reader = new JSON5Format().createReader(file);
        assertNull(reader.read("nonexistent"));
        reader.push("nested");
        assertNull(reader.read("nonexistent"));
        reader.pop();
        assertNull(reader.readArray("nonexistent"));
        reader.close();
    }

    @Test
    void testTypeMismatchReturnsNull() throws IOException {
        Path file = tempDir.resolve("test.json5");
        writeSpec(new JSON5Format().createWriter(file));

        // SCALAR READ OF AN ARRAY FIELD AND ARRAY READ OF A SCALAR FIELD BOTH RETURN NULL
        IFormatReader reader = new JSON5Format().createReader(file);
        assertNull(reader.read("tags"));
        assertNull(reader.readArray("count"));
        reader.close();
    }

    // ========================================================================
    // JSON5-ONLY SYNTAX
    // ========================================================================

    @Test
    void testSingleQuotedValue() throws IOException {
        IFormatReader reader = open("single_quote_value.json5", "{\"key\": 'single'}");
        assertEquals("single", reader.read("key"));
        reader.close();
    }

    @Test
    void testSingleQuotedKey() throws IOException {
        IFormatReader reader = open("single_quote_key.json5", "{'key': \"val\"}");
        assertEquals("val", reader.read("key"));
        reader.close();
    }

    @Test
    void testUnquotedKey() throws IOException {
        IFormatReader reader = open("unquoted_key.json5", "{key: \"val\"}");
        assertEquals("val", reader.read("key"));
        reader.close();
    }

    @Test
    void testTrailingCommaInObject() throws IOException {
        IFormatReader reader = open("trailing_object.json5", """
                {
                    "a": 1,
                    "b": "x",
                }
                """);
        assertEquals("1", reader.read("a"));
        assertEquals("x", reader.read("b"));
        reader.close();
    }

    @Test
    void testTrailingCommaInArray() throws IOException {
        IFormatReader reader = open("trailing_array.json5", "{\"tags\": [\"a\", \"b\",]}");
        assertArrayEquals(new String[]{"a", "b"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testHexNumbers() throws IOException {
        // HEX TOKENS ARE VALID JSON5 AND MUST SURVIVE AS RAW STRINGS
        IFormatReader reader = open("hex.json5", "{\"mask\": 0xFF, \"neg\": -0x1A}");
        assertEquals("0xFF", reader.read("mask"));
        assertEquals("-0x1A", reader.read("neg"));
        reader.close();
    }

    @Test
    void testInfinityAndNaN() throws IOException {
        IFormatReader reader = open("special_numbers.json5", "{\"pos\": Infinity, \"neg\": -Infinity, \"nan\": NaN}");
        assertEquals("Infinity", reader.read("pos"));
        assertEquals("-Infinity", reader.read("neg"));
        assertEquals("NaN", reader.read("nan"));
        reader.close();
    }

    @Test
    void testSignedAndLeadingDecimalNumbers() throws IOException {
        // JSON5 ALLOWS EXPLICIT PLUS SIGNS AND LEADING/TRAILING DECIMAL POINTS
        IFormatReader reader = open("loose_numbers.json5", "{\"plus\": +5, \"dot\": .5, \"trail\": 5.}");
        assertEquals("+5", reader.read("plus"));
        assertEquals(".5", reader.read("dot"));
        assertEquals("5.", reader.read("trail"));
        reader.close();
    }

    // ========================================================================
    // STRUCTURE EDGE CASES
    // ========================================================================

    @Test
    void testDeepNestingRoundTrip() throws IOException {
        Path file = tempDir.resolve("deep.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("root");
        writer.write("top", "1", Integer.class, null);
        writer.push("l1");
        writer.push("l2");
        writer.push("l3");
        writer.push("l4");
        writer.push("l5");
        writer.write("deep", "found", String.class, null);
        writer.pop();
        writer.pop();
        writer.pop();
        writer.pop();
        writer.pop();
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("1", reader.read("top"));
        reader.push("l1");
        reader.push("l2");
        reader.push("l3");
        reader.push("l4");
        reader.push("l5");
        assertEquals("found", reader.read("deep"));
        reader.pop();
        reader.pop();
        reader.pop();
        reader.pop();
        reader.pop();
        reader.close();
    }

    // ========================================================================
    // MALFORMED INPUT
    // ========================================================================

    @Test
    void testTruncatedFileThrows() throws IOException {
        Path file = tempDir.resolve("truncated.json5");
        Files.writeString(file, "{\"count\": 42");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    @Test
    void testUnclosedBraceThrows() throws IOException {
        Path file = tempDir.resolve("unclosed.json5");
        Files.writeString(file, "{\"outer\": {\"inner\": 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    @Test
    void testExtraClosingBraceThrows() throws IOException {
        Path file = tempDir.resolve("extra_brace.json5");
        Files.writeString(file, "{\"a\": 1}}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    @Test
    void testMissingColonThrows() throws IOException {
        Path file = tempDir.resolve("no_colon.json5");
        Files.writeString(file, "{\"a\" 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    @Test
    void testGarbageBeforeRootThrows() throws IOException {
        Path file = tempDir.resolve("garbage.json5");
        Files.writeString(file, "garbage {\"a\": 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSON5Format().createReader(file)));
    }

    // ========================================================================
    // WRITER + READER ROUND-TRIPS
    // ========================================================================

    @Test
    void testRoundTripQuote() throws IOException {
        assertEquals("say \"hi\"", roundTrip("rt_quote.json5", "say \"hi\""));
    }

    @Test
    void testRoundTripBackslash() throws IOException {
        assertEquals("C:\\path\\file", roundTrip("rt_backslash.json5", "C:\\path\\file"));
    }

    @Test
    void testRoundTripTrailingBackslash() throws IOException {
        assertEquals("trail\\", roundTrip("rt_trailing.json5", "trail\\"));
    }

    @Test
    void testRoundTripNewlineAndTab() throws IOException {
        // WRITER EMITS RAW CONTROL CHARS AND THE READER PRESERVES THEM INSIDE QUOTED STRINGS
        assertEquals("l1\nl2\tend", roundTrip("rt_ctrl.json5", "l1\nl2\tend"));
    }

    @Test
    void testRoundTripUnicode() throws IOException {
        assertEquals(UNICODE, roundTrip("rt_unicode.json5", UNICODE));
    }

    @Test
    void testRoundTripCommentMarkers() throws IOException {
        assertEquals("see // this and /* that */", roundTrip("rt_markers.json5", "see // this and /* that */"));
    }

    @Test
    void testRoundTripStructuralChars() throws IOException {
        assertEquals("a{b}c[d]e,f:g", roundTrip("rt_structural.json5", "a{b}c[d]e,f:g"));
    }

    @Test
    void testRoundTripEmptyString() throws IOException {
        assertEquals("", roundTrip("rt_empty.json5", ""));
    }

    @Test
    void testRoundTripEmptyArray() throws IOException {
        Path file = tempDir.resolve("rt_empty_array.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("empty", new String[0], String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertArrayEquals(new String[0], reader.readArray("empty"));
        reader.close();
    }

    @Test
    void testRoundTripSingleElementArray() throws IOException {
        Path file = tempDir.resolve("rt_single_array.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("one", new String[]{"only"}, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertArrayEquals(new String[]{"only"}, reader.readArray("one"));
        reader.close();
    }

    @Test
    void testRoundTripLargeArray() throws IOException {
        String[] many = new String[120];
        for (int i = 0; i < many.length; i++) {
            many[i] = "item" + i;
        }

        Path file = tempDir.resolve("rt_large_array.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("many", many, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertArrayEquals(many, reader.readArray("many"));
        reader.close();
    }

    @Test
    void testRoundTripNumericArray() throws IOException {
        Path file = tempDir.resolve("rt_num_array.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("nums", new String[]{"1", "2", "3"}, Integer[].class, Integer.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertArrayEquals(new String[]{"1", "2", "3"}, reader.readArray("nums"));
        reader.close();
    }

    @Test
    void testRoundTripNumbers() throws IOException {
        Path file = tempDir.resolve("rt_numbers.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("neg", "-42", Integer.class, null);
        writer.write("exp", "1e5", Double.class, null);
        writer.write("max", String.valueOf(Long.MAX_VALUE), Long.class, null);
        writer.write("precise", "3.14159265358979323846264338327", Double.class, null);
        writer.write("padded", "007", Integer.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("-42", reader.read("neg"));
        assertEquals("1e5", reader.read("exp"));
        assertEquals(String.valueOf(Long.MAX_VALUE), reader.read("max"));
        assertEquals("3.14159265358979323846264338327", reader.read("precise"));
        assertEquals("007", reader.read("padded"));
        reader.close();
    }

    @Test
    void testRoundTripBooleansAndNull() throws IOException {
        Path file = tempDir.resolve("rt_bools.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("on", "true", Boolean.class, null);
        writer.write("off", "false", Boolean.class, null);
        writer.write("nothing", "null", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("true", reader.read("on"));
        assertEquals("false", reader.read("off"));
        assertEquals("null", reader.read("nothing"));
        reader.close();
    }

    @Test
    void testRoundTripKeysWithDotsAndSpaces() throws IOException {
        Path file = tempDir.resolve("rt_keys.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.push("spec");
        writer.write("a.b", "1", Integer.class, null);
        writer.write("my key", "x", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("1", reader.read("a.b"));
        assertEquals("x", reader.read("my key"));
        reader.close();
    }

    @Test
    void testRoundTripWithInterleavedComments() throws IOException {
        // COMMENTS EMITTED BY THE WRITER MUST NOT DISTURB VALUES ON READ-BACK
        Path file = tempDir.resolve("rt_comments.json5");
        IFormatWriter writer = new JSON5Format().createWriter(file);
        writer.write("Header comment");
        writer.push("spec");
        writer.write("Before first");
        writer.write("first", "1", Integer.class, null);
        writer.write("Before second");
        writer.write("second", "two", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSON5Format().createReader(file);
        assertEquals("1", reader.read("first"));
        assertEquals("two", reader.read("second"));
        reader.close();
    }
}
