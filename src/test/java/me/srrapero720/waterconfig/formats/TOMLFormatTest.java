package me.srrapero720.waterconfig.formats;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.formats.TOMLFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TOMLFormatTest {

    @TempDir
    Path tempDir;

    // SHARED BASE SPEC: ROOT SCALARS + NESTED GROUP + STRING ARRAY (SAME SHAPE AS FormatTest)
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

    @Test
    void testWriterOutput() throws IOException {
        Path file = tempDir.resolve("test.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        String output = Files.readString(file, StandardCharsets.UTF_8);
        // ROOT PUSH IS TRANSPARENT; ROOT SCALARS ARE BUFFERED SO THEY ALL PRECEDE THE FIRST [table]
        String expected = """
                # Test spec
                # With multiple comments
                count = 42
                label = "hello world"
                enabled = true
                ratio = 3.14
                tags = [
                  "alpha",
                  "beta"
                ]

                [nested]
                  # Nested section
                  description = "inner"
                  weight = 0.5
                """;
        assertEquals(expected, output);
    }

    @Test
    void testReaderScalars() throws IOException {
        Path file = tempDir.resolve("test.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));
        reader.close();
    }

    @Test
    void testReaderNestedGroups() throws IOException {
        Path file = tempDir.resolve("test.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        IFormatReader reader = new TOMLFormat().createReader(file);
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();
        reader.close();
    }

    @Test
    void testReaderArrayValues() throws IOException {
        Path file = tempDir.resolve("test.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertArrayEquals(new String[]{"alpha", "beta"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTrip() throws IOException {
        Path file = tempDir.resolve("test.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));

        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();

        assertArrayEquals(new String[]{"alpha", "beta"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testWaterMediaRoundTrip() throws IOException {
        Path file = tempDir.resolve("watermedia.toml");

        // NESTED-GROUPS-ONLY PATTERN: NO ROOT SCALARS, THE SHAPE THAT EXPOSED THE TOML ROOT-TABLE BUG
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("watermedia");

        writer.write("DecodersAPI settings");
        writer.push("decoders");
        writer.write("pngFailOnCorruptedData", "true", Boolean.class, null);
        writer.write("pngUseBKGDChunk", "false", Boolean.class, null);
        writer.pop();

        writer.write("NetworkAPI settings");
        writer.push("network");
        writer.write("enableServer", "true", Boolean.class, null);
        writer.write("forceEnableServer", "false", Boolean.class, null);
        writer.write("serverPort", "25580", Integer.class, null);
        writer.write("remoteHost", "http://localhost:25580/", String.class, null);
        writer.write("token", "secret", String.class, null);
        writer.pop();

        writer.write("MediaAPI settings");
        writer.push("media");
        writer.write("defaultQuality", "HIGHEST", String.class, null);
        writer.write("disableFFMPEG", "false", Boolean.class, null);
        writer.pop();

        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        reader.push("decoders");
        assertEquals("true", reader.read("pngFailOnCorruptedData"));
        assertEquals("false", reader.read("pngUseBKGDChunk"));
        reader.pop();

        reader.push("network");
        assertEquals("true", reader.read("enableServer"));
        assertEquals("false", reader.read("forceEnableServer"));
        assertEquals("25580", reader.read("serverPort"));
        assertEquals("http://localhost:25580/", reader.read("remoteHost"));
        assertEquals("secret", reader.read("token"));
        reader.pop();

        reader.push("media");
        assertEquals("HIGHEST", reader.read("defaultQuality"));
        assertEquals("false", reader.read("disableFFMPEG"));
        reader.pop();
        reader.close();
    }

    @Test
    void testHashComments() throws IOException {
        Path file = tempDir.resolve("comments.toml");
        Files.writeString(file, """
                # Root comment
                # Comment before value
                count = 42
                label = "hello world"
                # Comment between values
                enabled = true
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        reader.close();
    }

    @Test
    void testMathExpressionCapture() throws IOException {
        Path file = tempDir.resolve("math.toml");
        Files.writeString(file, """
                count = 2 + 3
                ratio = 1.5 * 4
                power = 5 ^ 2
                root = ~25
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("2 + 3", reader.read("count"));
        assertEquals("1.5 * 4", reader.read("ratio"));
        assertEquals("5 ^ 2", reader.read("power"));
        assertEquals("~25", reader.read("root"));
        reader.close();
    }

    @Test
    void testRoundTripQuotesInString() throws IOException {
        Path file = tempDir.resolve("quotes.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "he said \"hi\" loudly", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("he said \"hi\" loudly", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripBackslashes() throws IOException {
        Path file = tempDir.resolve("backslashes.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("path", "C:\\Users\\test", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("C:\\Users\\test", reader.read("path"));
        reader.close();
    }

    @Test
    void testRoundTripHashInValue() throws IOException {
        Path file = tempDir.resolve("hash.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "value # not a comment", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("value # not a comment", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripSeparatorsInValue() throws IOException {
        Path file = tempDir.resolve("separators.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("query", "key=value", String.class, null);
        writer.write("time", "12:30:45", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("key=value", reader.read("query"));
        assertEquals("12:30:45", reader.read("time"));
        reader.close();
    }

    @Test
    void testRoundTripUnicode() throws IOException {
        Path file = tempDir.resolve("unicode.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("text", "café 你好 🌊", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("café 你好 🌊", reader.read("text"));
        reader.close();
    }

    @Test
    void testRoundTripPaddedSpaces() throws IOException {
        Path file = tempDir.resolve("padded.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "  padded  ", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("  padded  ", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripEmptyString() throws IOException {
        Path file = tempDir.resolve("empty.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripControlChars() throws IOException {
        Path file = tempDir.resolve("control.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "line1\nline2\ttab", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("line1\nline2\ttab", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripEmptyArray() throws IOException {
        Path file = tempDir.resolve("emptyarray.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", new String[0], String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertArrayEquals(new String[0], reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripSingleElementArray() throws IOException {
        Path file = tempDir.resolve("singlearray.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", new String[]{"only"}, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertArrayEquals(new String[]{"only"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripArraySpecialElements() throws IOException {
        String[] special = {"a,b", "c d", "e\"f", "g\\h"};

        Path file = tempDir.resolve("specialarray.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", special, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertArrayEquals(special, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripLargeArray() throws IOException {
        String[] many = new String[128];
        for (int i = 0; i < many.length; i++) many[i] = "item" + i;

        Path file = tempDir.resolve("largearray.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", many, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertArrayEquals(many, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripNumbers() throws IOException {
        Path file = tempDir.resolve("numbers.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("negative", "-42", Integer.class, null);
        writer.write("maxLong", "9223372036854775807", Long.class, null);
        writer.write("pi", "3.141592653589793", Double.class, null);
        writer.write("negDouble", "-2.718281828459045", Double.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("-42", reader.read("negative"));
        assertEquals("9223372036854775807", reader.read("maxLong"));
        assertEquals("3.141592653589793", reader.read("pi"));
        assertEquals("-2.718281828459045", reader.read("negDouble"));
        reader.close();
    }

    @Test
    void testDeepNestingRoundTrip() throws IOException {
        Path file = tempDir.resolve("deep.toml");
        IFormatWriter writer = new TOMLFormat().createWriter(file);
        writer.push("spec");
        writer.write("top", "0", Integer.class, null);
        writer.push("l1");
        writer.write("a", "1", Integer.class, null);
        writer.push("l2");
        writer.write("b", "2", Integer.class, null);
        writer.push("l3");
        writer.write("c", "3", Integer.class, null);
        writer.push("l4");
        writer.write("d", "4", Integer.class, null);
        writer.pop();
        writer.pop();
        writer.pop();
        writer.pop();
        writer.write("bottom", "5", Integer.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("0", reader.read("top"));
        reader.push("l1");
        assertEquals("1", reader.read("a"));
        reader.push("l2");
        assertEquals("2", reader.read("b"));
        reader.push("l3");
        assertEquals("3", reader.read("c"));
        reader.push("l4");
        assertEquals("4", reader.read("d"));
        reader.pop();
        reader.pop();
        reader.pop();
        reader.pop();
        assertEquals("5", reader.read("bottom"));
        reader.close();
    }

    @Test
    void testMissingKeyReturnsNull() throws IOException {
        Path file = tempDir.resolve("missing.toml");
        writeSpec(new TOMLFormat().createWriter(file));

        IFormatReader reader = new TOMLFormat().createReader(file);
        assertNull(reader.read("ghost"));
        assertNull(reader.readArray("ghost"));
        // TYPE MISMATCH ALSO RETURNS NULL
        assertNull(reader.readArray("count"));
        assertNull(reader.read("tags"));
        reader.push("nowhere");
        assertNull(reader.read("ghost"));
        reader.pop();
        reader.close();
    }

    @Test
    void testLiteralStrings() throws IOException {
        Path file = tempDir.resolve("literal.toml");
        Files.writeString(file, """
                msg = 'hello "world"'
                hash = 'a # b'
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("hello \"world\"", reader.read("msg"));
        assertEquals("a # b", reader.read("hash"));
        reader.close();
    }

    @Test
    void testLiteralStringBackslashes() throws IOException {
        Path file = tempDir.resolve("literalbs.toml");
        Files.writeString(file, """
                path = 'C:\\Users\\test'
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("C:\\Users\\test", reader.read("path"));
        reader.close();
    }

    @Test
    void testMultilineBasicString() throws IOException {
        Path file = tempDir.resolve("multiline.toml");
        Files.writeString(file, """
                text = \"\"\"
                line1
                line2\"\"\"
                tail = 1
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        // TOML SPEC: NEWLINE RIGHT AFTER THE OPENING DELIMITER IS TRIMMED
        assertEquals("line1\nline2", reader.read("text"));
        assertEquals("1", reader.read("tail"));
        reader.close();
    }

    @Test
    void testMultilineStringLineEndingBackslash() throws IOException {
        Path file = tempDir.resolve("multilinebs.toml");
        Files.writeString(file, """
                text = \"\"\"\\
                    joined\"\"\"
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        // TOML SPEC: LINE-ENDING BACKSLASH TRIMS THE NEWLINE AND FOLLOWING WHITESPACE
        assertEquals("joined", reader.read("text"));
        reader.close();
    }

    @Test
    void testNumberUnderscores() throws IOException {
        Path file = tempDir.resolve("underscores.toml");
        Files.writeString(file, """
                count = 1_000
                big = 5_349_221
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("1000", reader.read("count"));
        assertEquals("5349221", reader.read("big"));
        reader.close();
    }

    @Test
    void testInlineCommentAfterValue() throws IOException {
        Path file = tempDir.resolve("inlinecomment.toml");
        Files.writeString(file, """
                count = 42 # the answer
                name = "water" # inline after string
                flag = true # trailing note
                tags = [
                  "a", # first
                  "b"
                ]
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("water", reader.read("name"));
        assertEquals("true", reader.read("flag"));
        assertArrayEquals(new String[]{"a", "b"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testWhitespaceVariantsAroundEquals() throws IOException {
        Path file = tempDir.resolve("whitespace.toml");
        Files.writeString(file, """
                a=1
                b   =   2
                c\t=\t3
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("1", reader.read("a"));
        assertEquals("2", reader.read("b"));
        assertEquals("3", reader.read("c"));
        reader.close();
    }

    @Test
    void testKeysOutOfTableOrder() throws IOException {
        Path file = tempDir.resolve("order.toml");
        // TOML SPEC: TABLES MAY BE DEFINED IN ANY ORDER, SUB-TABLES MAY APPEAR AFTER UNRELATED TABLES
        Files.writeString(file, """
                root = 0

                [zeta]
                z = 1

                [alpha]
                a = 2

                [zeta.inner]
                zi = 3
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        assertEquals("0", reader.read("root"));
        reader.push("zeta");
        assertEquals("1", reader.read("z"));
        reader.push("inner");
        assertEquals("3", reader.read("zi"));
        reader.pop();
        reader.pop();
        reader.push("alpha");
        assertEquals("2", reader.read("a"));
        reader.pop();
        reader.close();
    }

    @Test
    void testInlineTable() throws IOException {
        Path file = tempDir.resolve("inlinetable.toml");
        Files.writeString(file, """
                point = { x = 1, y = "two" }
                """);
        IFormatReader reader = new TOMLFormat().createReader(file);
        reader.push("point");
        assertEquals("1", reader.read("x"));
        assertEquals("two", reader.read("y"));
        reader.pop();
        reader.close();
    }

    @Test
    void testUnclosedTableHeaderThrows() throws IOException {
        Path file = tempDir.resolve("badtable.toml");
        Files.writeString(file, "[never closed\ncount = 42\n");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new TOMLFormat().createReader(file)));
    }

    @Test
    void testUnclosedStringThrows() throws IOException {
        Path file = tempDir.resolve("badstring.toml");
        Files.writeString(file, "label = \"abc");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new TOMLFormat().createReader(file)));
    }

    @Test
    void testUnclosedArrayThrows() throws IOException {
        Path file = tempDir.resolve("badarray.toml");
        Files.writeString(file, "tags = [\"a\", \"b\"");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new TOMLFormat().createReader(file)));
    }

    @Test
    void testUnclosedMultilineStringThrows() throws IOException {
        Path file = tempDir.resolve("badmultiline.toml");
        Files.writeString(file, "text = \"\"\"abc\ndef\n");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new TOMLFormat().createReader(file)));
    }
}
