package me.srrapero720.waterconfig.formats;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.formats.CFGFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class CFGFormatTest {

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
        Path file = tempDir.resolve("test.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        String output = Files.readString(file, StandardCharsets.UTF_8);
        String expected = """
                # Test spec
                # With multiple comments
                {
                  count: 42
                  label: "hello world"
                  enabled: true
                  ratio: 3.14

                  # Nested section
                  nested: {
                    description: "inner"
                    weight: 0.5

                  }
                  tags: [
                    "alpha",
                    "beta"
                  ]

                }
                """;
        assertEquals(expected, output);
    }

    @Test
    void testReaderScalars() throws IOException {
        Path file = tempDir.resolve("test.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        assertEquals("3.14", reader.read("ratio"));
        reader.close();
    }

    @Test
    void testReaderNestedGroups() throws IOException {
        Path file = tempDir.resolve("test.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        IFormatReader reader = new CFGFormat().createReader(file);
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        assertEquals("0.5", reader.read("weight"));
        reader.pop();
        reader.close();
    }

    @Test
    void testReaderArrayValues() throws IOException {
        Path file = tempDir.resolve("test.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(new String[]{"alpha", "beta"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTrip() throws IOException {
        Path file = tempDir.resolve("test.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        IFormatReader reader = new CFGFormat().createReader(file);
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
        Path file = tempDir.resolve("watermedia.cfg");

        // NESTED-GROUPS-ONLY PATTERN: NO ROOT SCALARS, MIRRORS WaterMediaConfig
        IFormatWriter writer = new CFGFormat().createWriter(file);
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

        IFormatReader reader = new CFGFormat().createReader(file);
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
        Path file = tempDir.resolve("comments.cfg");
        Files.writeString(file, """
                # Root comment
                {
                  # Comment before value
                  count: 42
                  label: "hello world"
                  # Comment between values
                  enabled: true
                  # Comment before group
                  nested: {
                    # Nested comment
                    description: "inner"
                  }
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hello world", reader.read("label"));
        assertEquals("true", reader.read("enabled"));
        reader.push("nested");
        assertEquals("inner", reader.read("description"));
        reader.pop();
        reader.close();
    }

    @Test
    void testInlineComments() throws IOException {
        Path file = tempDir.resolve("inlinecomments.cfg");
        Files.writeString(file, """
                {
                  count: 42 # inline note
                  label: "hash # inside" # after string
                  flag: true # trailing
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        assertEquals("hash # inside", reader.read("label"));
        assertEquals("true", reader.read("flag"));
        reader.close();
    }

    @Test
    void testMathExpressionCapture() throws IOException {
        Path file = tempDir.resolve("math.cfg");
        Files.writeString(file, """
                {
                  count: 2 + 3
                  ratio: 1.5 * 4
                  power: 5 ^ 2
                  root: ~25
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("2 + 3", reader.read("count"));
        assertEquals("1.5 * 4", reader.read("ratio"));
        assertEquals("5 ^ 2", reader.read("power"));
        assertEquals("~25", reader.read("root"));
        reader.close();
    }

    @Test
    void testRoundTripQuotesInString() throws IOException {
        Path file = tempDir.resolve("quotes.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "he said \"hi\" loudly", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("he said \"hi\" loudly", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripBackslashes() throws IOException {
        Path file = tempDir.resolve("backslashes.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("path", "C:\\Users\\test", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("C:\\Users\\test", reader.read("path"));
        reader.close();
    }

    @Test
    void testRoundTripHashInValue() throws IOException {
        Path file = tempDir.resolve("hash.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "value # not a comment", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("value # not a comment", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripSeparatorsInValue() throws IOException {
        Path file = tempDir.resolve("separators.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("query", "key=value", String.class, null);
        writer.write("time", "12:30:45", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("key=value", reader.read("query"));
        assertEquals("12:30:45", reader.read("time"));
        reader.close();
    }

    @Test
    void testRoundTripUnicode() throws IOException {
        Path file = tempDir.resolve("unicode.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("text", "café 你好 🌊", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("café 你好 🌊", reader.read("text"));
        reader.close();
    }

    @Test
    void testRoundTripPaddedSpaces() throws IOException {
        Path file = tempDir.resolve("padded.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "  padded  ", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("  padded  ", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripEmptyString() throws IOException {
        Path file = tempDir.resolve("empty.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripControlChars() throws IOException {
        Path file = tempDir.resolve("control.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("value", "line1\nline2\ttab", String.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("line1\nline2\ttab", reader.read("value"));
        reader.close();
    }

    @Test
    void testRoundTripEmptyArray() throws IOException {
        Path file = tempDir.resolve("emptyarray.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", new String[0], String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(new String[0], reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripSingleElementArray() throws IOException {
        Path file = tempDir.resolve("singlearray.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", new String[]{"only"}, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(new String[]{"only"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripArraySpecialElements() throws IOException {
        String[] special = {"a,b", "c d", "e\"f", "g\\h"};

        Path file = tempDir.resolve("specialarray.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", special, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(special, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripLargeArray() throws IOException {
        String[] many = new String[128];
        for (int i = 0; i < many.length; i++) many[i] = "item" + i;

        Path file = tempDir.resolve("largearray.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("tags", many, String[].class, String.class);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(many, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testRoundTripNumbers() throws IOException {
        Path file = tempDir.resolve("numbers.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.write("negative", "-42", Integer.class, null);
        writer.write("maxLong", "9223372036854775807", Long.class, null);
        writer.write("pi", "3.141592653589793", Double.class, null);
        writer.write("negDouble", "-2.718281828459045", Double.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("-42", reader.read("negative"));
        assertEquals("9223372036854775807", reader.read("maxLong"));
        assertEquals("3.141592653589793", reader.read("pi"));
        assertEquals("-2.718281828459045", reader.read("negDouble"));
        reader.close();
    }

    @Test
    void testDeepNestingRoundTrip() throws IOException {
        Path file = tempDir.resolve("deep.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
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

        IFormatReader reader = new CFGFormat().createReader(file);
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
        Path file = tempDir.resolve("missing.cfg");
        writeSpec(new CFGFormat().createWriter(file));

        IFormatReader reader = new CFGFormat().createReader(file);
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
    void testUnquotedValuesWithSpaces() throws IOException {
        Path file = tempDir.resolve("unquoted.cfg");
        Files.writeString(file, """
                {
                  plain: hello world
                  quoted: "hello world"
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("hello world", reader.read("plain"));
        assertEquals("hello world", reader.read("quoted"));
        reader.close();
    }

    @Test
    void testEqualsSeparator() throws IOException {
        Path file = tempDir.resolve("equals.cfg");
        Files.writeString(file, """
                {
                  count = 42
                  nested = {
                    inner = "x"
                  }
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertEquals("42", reader.read("count"));
        reader.push("nested");
        assertEquals("x", reader.read("inner"));
        reader.pop();
        reader.close();
    }

    @Test
    void testBraceOnNextLine() throws IOException {
        Path file = tempDir.resolve("nextline.cfg");
        Files.writeString(file, """
                {
                  nested:
                  {
                    x: 1
                  }
                  count: 2
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        reader.push("nested");
        assertEquals("1", reader.read("x"));
        reader.pop();
        assertEquals("2", reader.read("count"));
        reader.close();
    }

    @Test
    void testEmptyGroup() throws IOException {
        Path file = tempDir.resolve("emptygroup.cfg");
        IFormatWriter writer = new CFGFormat().createWriter(file);
        writer.push("spec");
        writer.push("empty");
        writer.pop();
        writer.write("after", "1", Integer.class, null);
        writer.pop();
        writer.close();

        IFormatReader reader = new CFGFormat().createReader(file);
        reader.push("empty");
        assertNull(reader.read("anything"));
        reader.pop();
        assertEquals("1", reader.read("after"));
        reader.close();
    }

    @Test
    void testArrayElementsWithCommasAndSpaces() throws IOException {
        Path file = tempDir.resolve("commas.cfg");
        Files.writeString(file, """
                {
                  tags: ["a,b", "c d", 'e']
                }
                """);
        IFormatReader reader = new CFGFormat().createReader(file);
        assertArrayEquals(new String[]{"a,b", "c d", "e"}, reader.readArray("tags"));
        reader.close();
    }

    @Test
    void testMissingRootBraceThrows() throws IOException {
        Path file = tempDir.resolve("noroot.cfg");
        Files.writeString(file, "count: 42\n");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new CFGFormat().createReader(file)));
    }

    @Test
    void testEmptyFileThrows() throws IOException {
        Path file = tempDir.resolve("empty.cfg");
        Files.writeString(file, "");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new CFGFormat().createReader(file)));
    }

    @Test
    void testUnclosedStringThrows() throws IOException {
        Path file = tempDir.resolve("badstring.cfg");
        Files.writeString(file, "{\n  label: \"abc");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new CFGFormat().createReader(file)));
    }

    @Test
    void testUnclosedArrayThrows() throws IOException {
        Path file = tempDir.resolve("badarray.cfg");
        Files.writeString(file, "{\n  tags: [\"a\"");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new CFGFormat().createReader(file)));
    }

    @Test
    void testUnclosedRootBraceThrows() throws IOException {
        Path file = tempDir.resolve("unbalanced.cfg");
        Files.writeString(file, "{\n  count: 42\n");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(Exception.class, () -> new CFGFormat().createReader(file)));
    }
}
