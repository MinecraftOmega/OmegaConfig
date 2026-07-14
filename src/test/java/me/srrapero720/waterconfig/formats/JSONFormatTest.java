package me.srrapero720.waterconfig.formats;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.formats.JSONFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class JSONFormatTest {

    @TempDir
    Path tempDir;

    // UNICODE SAMPLE (ACCENT, CJK, EMOJI) BUILT FROM CODEPOINTS SO THIS SOURCE FILE STAYS PURE ASCII
    private static final String UNICODE = "caf" + (char) 0xE9 + " " + (char) 0x4F60 + (char) 0x597D + " " + new String(Character.toChars(0x1F600));

    // WRITES THE SHARED REPRESENTATIVE SPEC: SCALARS, NESTED GROUP AND STRING ARRAY
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
        return new JSONFormat().createReader(file);
    }

    // WRITES A SINGLE STRING FIELD THROUGH THE WRITER AND READS IT BACK
    private String roundTrip(String name, String value) throws IOException {
        Path file = tempDir.resolve(name);
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("field", value, String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        String result = reader.read("field");
        reader.close();
        return result;
    }

    // ========================================================================
    // BASE CASES
    // ========================================================================

    @Test
    void testWriterOutput() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        String output = Files.readString(file, StandardCharsets.UTF_8);
        // JSON: NO COMMENTS, BOOLEANS UNQUOTED, ROOT POP ADDS }\n, CLOSE ADDS \n
        String expected = """
                {
                \t"count": 42,
                \t"label": "hello world",
                \t"enabled": true,
                \t"ratio": 3.14,
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
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        IFormatReader reader = new JSONFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));
        reader.close();
    }

    @Test
    void testReaderNested() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        IFormatReader reader = new JSONFormat().createReader(file);
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();
        reader.close();
    }

    @Test
    void testReaderArray() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        IFormatReader reader = new JSONFormat().createReader(file);
        String[] tags = reader.readArray("tags");
        assertNotNull(tags);
        assertArrayEquals(new String[]{"alpha", "beta"}, tags);
        reader.close();
    }

    @Test
    void testFullRoundTrip() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        IFormatReader reader = new JSONFormat().createReader(file);
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
        Path file = tempDir.resolve("groups.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("watermedia");
        writer.push("decoders");
        writer.write("failOnCorrupted", "true", Boolean.class, null);
        writer.pop();
        writer.push("network");
        writer.write("serverPort", "25580", Integer.class, null);
        writer.write("remoteHost", "http://localhost:25580/", String.class, null);
        writer.pop();
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        reader.push("decoders");
        assertEquals("true", reader.read("failOnCorrupted"));
        reader.pop();
        reader.push("network");
        assertEquals("25580", reader.read("serverPort"));
        assertEquals("http://localhost:25580/", reader.read("remoteHost"));
        reader.pop();
        reader.close();
    }

    @Test
    void testMathExpressionCapture() throws IOException {
        IFormatReader reader = open("math.json", """
                {
                  "count": 2 + 3,
                  "ratio": 1.5 * 4
                }
                """);
        // JSON READER STRIPS WHITESPACE FROM UNQUOTED VALUES, BUT OPERATORS REMAIN
        String count = reader.read("count");
        assertNotNull(count);
        assertTrue(count.contains("+"), "Expression operator '+' should survive JSON reader");
        String ratio = reader.read("ratio");
        assertNotNull(ratio);
        assertTrue(ratio.contains("*"), "Expression operator '*' should survive JSON reader");
        reader.close();
    }

    // ========================================================================
    // STRING CONTENT EDGE CASES (READER)
    // ========================================================================

    @Test
    void testReadEscapedQuote() throws IOException {
        IFormatReader reader = open("esc_quote.json", "{\"v\": \"say \\\"hi\\\"\"}");
        assertEquals("say \"hi\"", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEscapedBackslash() throws IOException {
        IFormatReader reader = open("esc_backslash.json", "{\"v\": \"a\\\\b\"}");
        assertEquals("a\\b", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEscapedNewlineAndTab() throws IOException {
        IFormatReader reader = open("esc_ctrl.json", "{\"v\": \"l1\\nl2\\tend\"}");
        assertEquals("l1\nl2\tend", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadUnicodeEscape() throws IOException {
        // FILE GETS LITERAL BACKSLASH-U SEQUENCES, EXPECTED VALUE IS THE SPEC-DECODED TEXT
        IFormatReader reader = open("esc_unicode.json", "{\"v\": \"caf\\u00e9 \\u4f60\\u597d \\uD83D\\uDE00\"}");
        assertEquals(UNICODE, reader.read("v"));
        reader.close();
    }

    @Test
    void testReadRawUnicode() throws IOException {
        // FILE IS WRITTEN AND READ AS UTF-8 WITH REAL UNICODE CHARS, INCLUDING SURROGATE PAIRS
        IFormatReader reader = open("raw_unicode.json", "{\"v\": \"" + UNICODE + "\"}");
        assertEquals(UNICODE, reader.read("v"));
        reader.close();
    }

    @Test
    void testReadCommentMarkersInString() throws IOException {
        // JSON HAS NO COMMENTS, SO // AND /* ARE PLAIN CHARS INSIDE QUOTED STRINGS
        IFormatReader reader = open("markers.json", """
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
    void testReadDelimitersInString() throws IOException {
        IFormatReader reader = open("delimiters.json", "{\"v\": \"a[b,c:d\"}");
        assertEquals("a[b,c:d", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadOpenBraceInString() throws IOException {
        IFormatReader reader = open("open_brace.json", "{\"v\": \"a{b\"}");
        assertEquals("a{b", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadCloseBraceInString() throws IOException {
        IFormatReader reader = open("close_brace.json", "{\"v\": \"a}b\"}");
        assertEquals("a}b", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadCloseBracketInString() throws IOException {
        IFormatReader reader = open("close_bracket.json", "{\"v\": \"a]b\"}");
        assertEquals("a]b", reader.read("v"));
        reader.close();
    }

    // ========================================================================
    // VALUE EDGE CASES (READER)
    // ========================================================================

    @Test
    void testReadEmptyString() throws IOException {
        IFormatReader reader = open("empty_string.json", "{\"v\": \"\"}");
        assertEquals("", reader.read("v"));
        reader.close();
    }

    @Test
    void testReadEmptyArray() throws IOException {
        IFormatReader reader = open("empty_array.json", "{\"v\": []}");
        assertArrayEquals(new String[0], reader.readArray("v"));
        reader.close();
    }

    @Test
    void testReadSingleElementArray() throws IOException {
        IFormatReader reader = open("single_array.json", "{\"v\": [\"only\"]}");
        assertArrayEquals(new String[]{"only"}, reader.readArray("v"));
        reader.close();
    }

    @Test
    void testReadNumberFormats() throws IOException {
        // READER IS STRING-BASED: NUMERIC TOKENS ARE RETURNED RAW AND UNPARSED
        IFormatReader reader = open("numbers.json", """
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
        IFormatReader reader = open("bools.json", "{\"t\": true, \"f\": false, \"n\": null}");
        assertEquals("true", reader.read("t"));
        assertEquals("false", reader.read("f"));
        assertEquals("null", reader.read("n"));
        reader.close();
    }

    @Test
    void testReadKeysWithDotsAndSpaces() throws IOException {
        IFormatReader reader = open("keys.json", "{\"a.b\": 1, \"my key\": \"x\"}");
        assertEquals("1", reader.read("a.b"));
        assertEquals("x", reader.read("my key"));
        reader.close();
    }

    @Test
    void testMissingKeyReturnsNull() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        IFormatReader reader = new JSONFormat().createReader(file);
        assertNull(reader.read("nonexistent"));
        reader.push("nested");
        assertNull(reader.read("nonexistent"));
        reader.pop();
        assertNull(reader.readArray("nonexistent"));
        reader.close();
    }

    @Test
    void testTypeMismatchReturnsNull() throws IOException {
        Path file = tempDir.resolve("test.json");
        writeSpec(new JSONFormat().createWriter(file));

        // SCALAR READ OF AN ARRAY FIELD AND ARRAY READ OF A SCALAR FIELD BOTH RETURN NULL
        IFormatReader reader = new JSONFormat().createReader(file);
        assertNull(reader.read("tags"));
        assertNull(reader.readArray("count"));
        reader.close();
    }

    // ========================================================================
    // STRUCTURE EDGE CASES
    // ========================================================================

    @Test
    void testDeepNestingRoundTrip() throws IOException {
        Path file = tempDir.resolve("deep.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
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

        IFormatReader reader = new JSONFormat().createReader(file);
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

    @Test
    void testCommentsRejected() throws IOException {
        // PLAIN JSON MUST NOT ACCEPT COMMENTS
        Path file = tempDir.resolve("comments.json");
        Files.writeString(file, """
                {
                    // NOT VALID IN PLAIN JSON
                    "count": 42
                }
                """);
        assertThrows(Exception.class, () -> new JSONFormat().createReader(file));
    }

    // ========================================================================
    // MALFORMED INPUT
    // ========================================================================

    @Test
    void testTruncatedFileThrows() throws IOException {
        Path file = tempDir.resolve("truncated.json");
        Files.writeString(file, "{\"count\": 42");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSONFormat().createReader(file)));
    }

    @Test
    void testUnclosedBraceThrows() throws IOException {
        Path file = tempDir.resolve("unclosed.json");
        Files.writeString(file, "{\"outer\": {\"inner\": 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSONFormat().createReader(file)));
    }

    @Test
    void testExtraClosingBraceThrows() throws IOException {
        Path file = tempDir.resolve("extra_brace.json");
        Files.writeString(file, "{\"a\": 1}}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSONFormat().createReader(file)));
    }

    @Test
    void testMissingColonThrows() throws IOException {
        Path file = tempDir.resolve("no_colon.json");
        Files.writeString(file, "{\"a\" 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSONFormat().createReader(file)));
    }

    @Test
    void testGarbageBeforeRootThrows() throws IOException {
        Path file = tempDir.resolve("garbage.json");
        Files.writeString(file, "garbage {\"a\": 1}");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new JSONFormat().createReader(file)));
    }

    // ========================================================================
    // WRITER + READER ROUND-TRIPS
    // ========================================================================

    @Test
    void testRoundTripQuote() throws IOException {
        assertEquals("say \"hi\"", roundTrip("rt_quote.json", "say \"hi\""));
    }

    @Test
    void testRoundTripBackslash() throws IOException {
        assertEquals("C:\\path\\file", roundTrip("rt_backslash.json", "C:\\path\\file"));
    }

    @Test
    void testRoundTripTrailingBackslash() throws IOException {
        assertEquals("trail\\", roundTrip("rt_trailing.json", "trail\\"));
    }

    @Test
    void testRoundTripNewlineAndTab() throws IOException {
        // WRITER EMITS RAW CONTROL CHARS AND THE READER PRESERVES THEM INSIDE QUOTED STRINGS
        assertEquals("l1\nl2\tend", roundTrip("rt_ctrl.json", "l1\nl2\tend"));
    }

    @Test
    void testRoundTripUnicode() throws IOException {
        assertEquals(UNICODE, roundTrip("rt_unicode.json", UNICODE));
    }

    @Test
    void testRoundTripCommentMarkers() throws IOException {
        assertEquals("see // this and /* that */", roundTrip("rt_markers.json", "see // this and /* that */"));
    }

    @Test
    void testRoundTripDelimiters() throws IOException {
        assertEquals("a,b:c[d", roundTrip("rt_delimiters.json", "a,b:c[d"));
    }

    @Test
    void testRoundTripBraces() throws IOException {
        assertEquals("{obj} and [arr]", roundTrip("rt_braces.json", "{obj} and [arr]"));
    }

    @Test
    void testRoundTripEmptyString() throws IOException {
        assertEquals("", roundTrip("rt_empty.json", ""));
    }

    @Test
    void testRoundTripEmptyArray() throws IOException {
        Path file = tempDir.resolve("rt_empty_array.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("empty", new String[0], String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertArrayEquals(new String[0], reader.readArray("empty"));
        reader.close();
    }

    @Test
    void testRoundTripSingleElementArray() throws IOException {
        Path file = tempDir.resolve("rt_single_array.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("one", new String[]{"only"}, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertArrayEquals(new String[]{"only"}, reader.readArray("one"));
        reader.close();
    }

    @Test
    void testRoundTripLargeArray() throws IOException {
        String[] many = new String[120];
        for (int i = 0; i < many.length; i++) {
            many[i] = "item" + i;
        }

        Path file = tempDir.resolve("rt_large_array.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("many", many, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertArrayEquals(many, reader.readArray("many"));
        reader.close();
    }

    @Test
    void testRoundTripNumericArray() throws IOException {
        Path file = tempDir.resolve("rt_num_array.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("nums", new String[]{"1", "2", "3"}, Integer[].class, Integer.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertArrayEquals(new String[]{"1", "2", "3"}, reader.readArray("nums"));
        reader.close();
    }

    @Test
    void testRoundTripNumbers() throws IOException {
        Path file = tempDir.resolve("rt_numbers.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("neg", "-42", Integer.class, null);
        writer.write("exp", "1e5", Double.class, null);
        writer.write("max", String.valueOf(Long.MAX_VALUE), Long.class, null);
        writer.write("precise", "3.14159265358979323846264338327", Double.class, null);
        writer.write("padded", "007", Integer.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertEquals("-42", reader.read("neg"));
        assertEquals("1e5", reader.read("exp"));
        assertEquals(String.valueOf(Long.MAX_VALUE), reader.read("max"));
        assertEquals("3.14159265358979323846264338327", reader.read("precise"));
        assertEquals("007", reader.read("padded"));
        reader.close();
    }

    @Test
    void testRoundTripBooleansAndNull() throws IOException {
        Path file = tempDir.resolve("rt_bools.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("on", "true", Boolean.class, null);
        writer.write("off", "false", Boolean.class, null);
        writer.write("nothing", "null", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertEquals("true", reader.read("on"));
        assertEquals("false", reader.read("off"));
        assertEquals("null", reader.read("nothing"));
        reader.close();
    }

    @Test
    void testRoundTripKeysWithDotsAndSpaces() throws IOException {
        Path file = tempDir.resolve("rt_keys.json");
        IFormatWriter writer = new JSONFormat().createWriter(file);
        writer.push("spec");
        writer.write("a.b", "1", Integer.class, null);
        writer.write("my key", "x", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new JSONFormat().createReader(file);
        assertEquals("1", reader.read("a.b"));
        assertEquals("x", reader.read("my key"));
        reader.close();
    }
}
