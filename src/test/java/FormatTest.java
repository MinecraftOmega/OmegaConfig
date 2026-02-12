import me.srrapero720.waterconfig.impl.formats.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FormatTest {

    @TempDir
    Path tempDir;

    /**
     * Writes a shared test structure to any format writer:
     * - Root comments, root push
     * - int, String, boolean, double scalars
     * - Nested group with comment + String + double
     * - String array
     */
    private void writeTestSpec(IFormatWriter writer) throws IOException {
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

    // ========================================================================
    // CFG Format Tests
    // ========================================================================
    @Nested
    class CFGTest {

        @Test
        void testWriterOutput() throws IOException {
            Path file = tempDir.resolve("test.cfg");
            IFormatWriter writer = new CFGFormat().createWriter(file);
            writeTestSpec(writer);

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
        void testReaderScalarValues() throws IOException {
            Path file = tempDir.resolve("test.cfg");
            IFormatWriter writer = new CFGFormat().createWriter(file);
            writeTestSpec(writer);

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
            IFormatWriter writer = new CFGFormat().createWriter(file);
            writeTestSpec(writer);

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
            IFormatWriter writer = new CFGFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new CFGFormat().createReader(file);
            String[] tags = reader.readArray("tags");
            assertNotNull(tags);
            assertArrayEquals(new String[]{"alpha", "beta"}, tags);
            reader.close();
        }

        @Test
        void testRoundTrip() throws IOException {
            Path file = tempDir.resolve("test.cfg");
            IFormatWriter writer = new CFGFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new CFGFormat().createReader(file);
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
    }

    // ========================================================================
    // JSON5 Format Tests
    // ========================================================================
    @Nested
    class JSON5Test {

        @Test
        void testWriterOutput() throws IOException {
            Path file = tempDir.resolve("test.json5");
            IFormatWriter writer = new JSON5Format().createWriter(file);
            writeTestSpec(writer);

            String output = Files.readString(file, StandardCharsets.UTF_8);
            // JSON5: booleans are quoted (isString check), root pop adds }\n, close adds \n
            String expected = """
                    // Test spec
                    // With multiple comments
                    {
                    \t"count": 42,
                    \t"label": "hello world",
                    \t"enabled": "true",
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
        void testReaderScalarValues() throws IOException {
            Path file = tempDir.resolve("test.json5");
            IFormatWriter writer = new JSON5Format().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new JSON5Format().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            assertEquals("3.14", reader.read("ratio"));
            reader.close();
        }

        @Test
        void testReaderNestedGroups() throws IOException {
            Path file = tempDir.resolve("test.json5");
            IFormatWriter writer = new JSON5Format().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new JSON5Format().createReader(file);
            reader.push("nested");
            assertEquals("inner", reader.read("description"));
            assertEquals("0.5", reader.read("weight"));
            reader.pop();
            reader.close();
        }

        @Test
        void testRoundTrip() throws IOException {
            Path file = tempDir.resolve("test.json5");
            IFormatWriter writer = new JSON5Format().createWriter(file);
            writeTestSpec(writer);

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
        void testLineComments() throws IOException {
            Path file = tempDir.resolve("line_comments.json5");
            Files.writeString(file, """
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
            IFormatReader reader = new JSON5Format().createReader(file);
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
            Path file = tempDir.resolve("block_comments.json5");
            Files.writeString(file, """
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
            IFormatReader reader = new JSON5Format().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            reader.close();
        }

        @Test
        void testMixedComments() throws IOException {
            Path file = tempDir.resolve("mixed_comments.json5");
            Files.writeString(file, """
                    // Line comment before root
                    /* Block comment before root */
                    {
                        // Line comment
                        "count": 42,
                        /* Block comment */
                        "label": "hello world"
                    }
                    """);
            IFormatReader reader = new JSON5Format().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            reader.close();
        }

        @Test
        void testSlashInStringValues() throws IOException {
            Path file = tempDir.resolve("slash_strings.json5");
            Files.writeString(file, """
                    {
                        "url": "https://example.com",
                        "path": "a/b/c"
                    }
                    """);
            IFormatReader reader = new JSON5Format().createReader(file);
            assertEquals("https://example.com", reader.read("url"));
            assertEquals("a/b/c", reader.read("path"));
            reader.close();
        }
    }

    // ========================================================================
    // JSON Format Tests
    // ========================================================================
    @Nested
    class JSONTest {

        @Test
        void testWriterOutput() throws IOException {
            Path file = tempDir.resolve("test.json");
            IFormatWriter writer = new JSONFormat().createWriter(file);
            writeTestSpec(writer);

            String output = Files.readString(file, StandardCharsets.UTF_8);
            // JSON: no comments, booleans unquoted, root pop adds }\n, close adds \n
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
        void testReaderScalarValues() throws IOException {
            Path file = tempDir.resolve("test.json");
            IFormatWriter writer = new JSONFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new JSONFormat().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            assertEquals("3.14", reader.read("ratio"));
            reader.close();
        }

        @Test
        void testReaderNestedGroups() throws IOException {
            Path file = tempDir.resolve("test.json");
            IFormatWriter writer = new JSONFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new JSONFormat().createReader(file);
            reader.push("nested");
            assertEquals("inner", reader.read("description"));
            assertEquals("0.5", reader.read("weight"));
            reader.pop();
            reader.close();
        }

        @Test
        void testRoundTrip() throws IOException {
            Path file = tempDir.resolve("test.json");
            IFormatWriter writer = new JSONFormat().createWriter(file);
            writeTestSpec(writer);

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
        void testMathExpressionCapture() throws IOException {
            Path file = tempDir.resolve("math.json");
            Files.writeString(file, """
                    {
                      "count": 2 + 3,
                      "ratio": 1.5 * 4
                    }
                    """);
            IFormatReader reader = new JSONFormat().createReader(file);
            // JSON reader strips whitespace from unquoted values, but operators remain
            String count = reader.read("count");
            assertNotNull(count);
            assertTrue(count.contains("+"), "Expression operator '+' should survive JSON reader");
            String ratio = reader.read("ratio");
            assertNotNull(ratio);
            assertTrue(ratio.contains("*"), "Expression operator '*' should survive JSON reader");
            reader.close();
        }
    }

    // ========================================================================
    // TOML Format Tests
    // ========================================================================
    @Nested
    class TOMLTest {

        @Test
        void testWriterOutput() throws IOException {
            Path file = tempDir.resolve("test.toml");
            IFormatWriter writer = new TOMLFormat().createWriter(file);
            writeTestSpec(writer);

            String output = Files.readString(file, StandardCharsets.UTF_8);
            // TOML: root push writes [test_spec], nested becomes [test_spec.nested].
            // After pop from nested, [test_spec] is re-emitted for subsequent fields.
            String expected = """
                    [test_spec]
                    # Test spec
                    # With multiple comments
                    count = 42
                    label = "hello world"
                    enabled = true
                    ratio = 3.14

                    [test_spec.nested]
                    # Nested section
                    description = "inner"
                    weight = 0.5

                    [test_spec]
                    tags = [
                      "alpha",
                      "beta"
                    ]
                    """;
            assertEquals(expected, output);
        }

        @Test
        void testReaderScalarValues() throws IOException {
            Path file = tempDir.resolve("test.toml");
            IFormatWriter writer = new TOMLFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new TOMLFormat().createReader(file);
            reader.push("test_spec");
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            assertEquals("3.14", reader.read("ratio"));
            reader.pop();
            reader.close();
        }

        @Test
        void testReaderNestedGroups() throws IOException {
            Path file = tempDir.resolve("test.toml");
            IFormatWriter writer = new TOMLFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new TOMLFormat().createReader(file);
            reader.push("test_spec");
            reader.push("nested");
            assertEquals("inner", reader.read("description"));
            assertEquals("0.5", reader.read("weight"));
            reader.pop();
            reader.pop();
            reader.close();
        }

        @Test
        void testRoundTrip() throws IOException {
            Path file = tempDir.resolve("test.toml");
            IFormatWriter writer = new TOMLFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new TOMLFormat().createReader(file);
            reader.push("test_spec");
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
            reader.pop();
            reader.close();
        }

        @Test
        void testHashComments() throws IOException {
            Path file = tempDir.resolve("comments.toml");
            Files.writeString(file, """
                    # Root comment
                    [test_spec]
                    # Comment before value
                    count = 42
                    label = "hello world"
                    # Comment between values
                    enabled = true
                    """);
            IFormatReader reader = new TOMLFormat().createReader(file);
            reader.push("test_spec");
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            reader.pop();
            reader.close();
        }

        @Test
        void testMathExpressionCapture() throws IOException {
            Path file = tempDir.resolve("math.toml");
            Files.writeString(file, """
                    [test]
                    count = 2 + 3
                    ratio = 1.5 * 4
                    power = 5 ^ 2
                    root = ~25
                    """);
            IFormatReader reader = new TOMLFormat().createReader(file);
            reader.push("test");
            assertEquals("2 + 3", reader.read("count"));
            assertEquals("1.5 * 4", reader.read("ratio"));
            assertEquals("5 ^ 2", reader.read("power"));
            assertEquals("~25", reader.read("root"));
            reader.pop();
            reader.close();
        }
    }

    // ========================================================================
    // Properties Format Tests
    // ========================================================================
    @Nested
    class PropertiesTest {

        @Test
        void testWriterOutput() throws IOException {
            Path file = tempDir.resolve("test.properties");
            IFormatWriter writer = new PROPFormat().createWriter(file);
            writeTestSpec(writer);

            String output = Files.readString(file, StandardCharsets.UTF_8);
            // Properties: root push is no-op, comments written inline,
            // nested push writes blank line + "# Begin of group nested" comment,
            // values are unquoted, arrays use [v1, v2], pop adds two newlines
            String expected = """
                    # Test spec
                    # With multiple comments
                    count=42
                    label=hello world
                    enabled=true
                    ratio=3.14
                    # Nested section

                    # Begin of group nested
                    nested.description=inner
                    nested.weight=0.5


                    tags=[alpha, beta]
                    """;
            assertEquals(expected, output);
        }

        @Test
        void testReaderScalarValues() throws IOException {
            Path file = tempDir.resolve("test.properties");
            IFormatWriter writer = new PROPFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new PROPFormat().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            assertEquals("3.14", reader.read("ratio"));
            reader.close();
        }

        @Test
        void testReaderNestedGroups() throws IOException {
            Path file = tempDir.resolve("test.properties");
            IFormatWriter writer = new PROPFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new PROPFormat().createReader(file);
            reader.push("nested");
            assertEquals("inner", reader.read("description"));
            assertEquals("0.5", reader.read("weight"));
            reader.pop();
            reader.close();
        }

        @Test
        void testReaderArrayValues() throws IOException {
            Path file = tempDir.resolve("test.properties");
            IFormatWriter writer = new PROPFormat().createWriter(file);
            writeTestSpec(writer);

            IFormatReader reader = new PROPFormat().createReader(file);
            String[] tags = reader.readArray("tags");
            assertNotNull(tags);
            assertArrayEquals(new String[]{"alpha", "beta"}, tags);
            reader.close();
        }

        @Test
        void testHashComments() throws IOException {
            Path file = tempDir.resolve("comments.properties");
            Files.writeString(file, """
                    # Root comment
                    # Comment before value
                    count=42
                    label=hello world
                    # Comment between values
                    enabled=true
                    """);
            IFormatReader reader = new PROPFormat().createReader(file);
            assertEquals("42", reader.read("count"));
            assertEquals("hello world", reader.read("label"));
            assertEquals("true", reader.read("enabled"));
            reader.close();
        }

        @Test
        void testMathExpressionCapture() throws IOException {
            Path file = tempDir.resolve("math.properties");
            Files.writeString(file, """
                    count=2 + 3
                    ratio=1.5 * 4
                    power=5 ^ 2
                    root=~25
                    """);
            IFormatReader reader = new PROPFormat().createReader(file);
            assertEquals("2 + 3", reader.read("count"));
            assertEquals("1.5 * 4", reader.read("ratio"));
            assertEquals("5 ^ 2", reader.read("power"));
            assertEquals("~25", reader.read("root"));
            reader.close();
        }
    }
}
