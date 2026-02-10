package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.annotations.StringConditions;
import me.srrapero720.waterconfig.impl.fields.BooleanField;
import me.srrapero720.waterconfig.impl.fields.DoubleField;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.StringField;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import me.srrapero720.waterconfig.api.annotations.*;
import me.srrapero720.waterconfig.impl.fields.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SpecTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() {
        WaterConfig.setPath(tempDir);
    }

    // ========================================================================
    // Builder Spec Tests (NATIVE mode)
    // ========================================================================
    @Nested
    class BuilderSpecTest {

        private ConfigSpec buildSpec() {
            ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder("builder_test", "cfg", "", 0);
            builder.defineInt("count", 10).math(true).setMin(0).setMax(100).end();
            builder.defineDouble("ratio", 1.5).math(true).setMin(0.0).setMax(50.0).end();
            builder.defineInt("strict_count", 5).math(true).strictMath(true).setMin(0).setMax(100).end();
            builder.defineString("label", "default_value").allowEmpty(false).end();
            builder.defineString("prefix_field", "hello_world").startsWith("hello").end();
            builder.defineString("contains_field", "foo_bar_baz").condition("bar").mode(StringField.Mode.CONTAINS).end();
            builder.defineBoolean("enabled", true).end();
            builder.defineInt("plain_int", 42).setMin(0).setMax(200).end();

            builder.push("nested");
            builder.defineInt("inner_count", 7).math(true).setMin(0).setMax(50).end();
            builder.defineString("inner_label", "nested_default").end();
            builder.pop();

            return builder.build();
        }

        @Test
        void testSaveAndLoadDefaults() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            assertTrue(spec.path().toFile().exists(), "Config file should be created");

            IntField count = (IntField) spec.findField("count");
            assertEquals(10, count.getAsInt());
            DoubleField ratio = (DoubleField) spec.findField("ratio");
            assertEquals(1.5, ratio.getAsDouble());
            StringField label = (StringField) spec.findField("label");
            assertEquals("default_value", label.get());
            BooleanField enabled = (BooleanField) spec.findField("enabled");
            assertTrue(enabled.getAsBoolean());
        }

        @Test
        void testMathExpressionEvaluation() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 2 + 3 * 4
                      ratio: 1.5 * 4
                      strict_count: 5 ^ 2
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42

                      nested: {
                        inner_count: ~16
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // 2 + 3*4 = 14
            IntField count = (IntField) spec.findField("count");
            assertEquals(14, count.getAsInt());

            // 1.5 * 4 = 6.0
            DoubleField ratio = (DoubleField) spec.findField("ratio");
            assertEquals(6.0, ratio.getAsDouble());

            // 5^2 = 25
            IntField strictCount = (IntField) spec.findField("strict_count");
            assertEquals(25, strictCount.getAsInt());

            // sqrt(16) = 4
            IntField innerCount = (IntField) spec.findField("nested.inner_count");
            assertEquals(4, innerCount.getAsInt());
        }

        @Test
        void testMathResultExceedsMax() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10 ^ 3
                      ratio: 1.5
                      strict_count: 5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // 10^3 = 1000 > max(100), should reset to default 10
            IntField count = (IntField) spec.findField("count");
            assertEquals(10, count.getAsInt());
        }

        @Test
        void testStrictMathInvalidExpression() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      strict_count: abc + 2
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            // strict math throws → load() catches exception → returns false
            boolean loaded = spec.load();
            assertFalse(loaded, "Load should fail on strict math invalid expression");
        }

        @Test
        void testNonStrictMathInvalidExpression() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: abc + 2
                      ratio: 1.5
                      strict_count: 5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // invalid expression, non-strict → reset to default 10
            IntField count = (IntField) spec.findField("count");
            assertEquals(10, count.getAsInt());
        }

        @Test
        void testStringValidation() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      strict_count: 5
                      label: ""
                      prefix_field: "goodbye_world"
                      contains_field: "no_match"
                      enabled: true
                      plain_int: 42

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            StringField label = (StringField) spec.findField("label");
            assertEquals("default_value", label.get(), "Empty string should reset to default (allowEmpty=false)");

            StringField prefix = (StringField) spec.findField("prefix_field");
            assertEquals("hello_world", prefix.get(), "Should reset to default (doesn't start with 'hello')");

            StringField contains = (StringField) spec.findField("contains_field");
            assertEquals("foo_bar_baz", contains.get(), "Should reset to default (doesn't contain 'bar')");
        }

        @Test
        void testIntMinMaxValidation() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      strict_count: 5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 999

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            IntField plainInt = (IntField) spec.findField("plain_int");
            assertEquals(42, plainInt.getAsInt(), "Value 999 > max(200) should reset to default 42");
        }

        @Test
        void testPlainNumberNoMath() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      strict_count: 5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 150

                      nested: {
                        inner_count: 7
                        inner_label: "nested_default"
                      }
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            IntField plainInt = (IntField) spec.findField("plain_int");
            assertEquals(150, plainInt.getAsInt());
        }

        @Test
        void testNestedGroupAccess() throws IOException {
            ConfigSpec spec = buildSpec();
            WaterConfig.register(spec);
            spec.save();

            IntField innerCount = (IntField) spec.findField("nested.inner_count");
            assertNotNull(innerCount, "Nested field should be accessible via dot notation");
            assertEquals(7, innerCount.getAsInt());

            StringField innerLabel = (StringField) spec.findField("nested.inner_label");
            assertNotNull(innerLabel);
            assertEquals("nested_default", innerLabel.get());
        }
    }

    // ========================================================================
    // Annotated Spec Tests (REFLECT mode)
    // ========================================================================
    @Nested
    class AnnotatedSpecTest {

        @Spec(value = "annotated_test", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class TestConfig {
            @Spec.Field
            @NumberConditions(math = true, minInt = 0, maxInt = 100)
            public static int count = 10;

            @Spec.Field
            @NumberConditions(math = true, strictMath = true, minDouble = 0.0, maxDouble = 50.0)
            public static double ratio = 1.5;

            @Spec.Field
            @StringConditions(allowEmpty = false)
            public static String label = "default_value";

            @Spec.Field
            @StringConditions(startsWith = "hello")
            public static String prefix_field = "hello_world";

            @Spec.Field
            @StringConditions(value = "bar", mode = StringField.Mode.CONTAINS)
            public static String contains_field = "foo_bar_baz";

            @Spec.Field
            public static boolean enabled = true;

            @Spec.Field
            @NumberConditions(minInt = 0, maxInt = 200)
            public static int plain_int = 42;
        }

        @BeforeEach
        void resetFields() {
            TestConfig.count = 10;
            TestConfig.ratio = 1.5;
            TestConfig.label = "default_value";
            TestConfig.prefix_field = "hello_world";
            TestConfig.contains_field = "foo_bar_baz";
            TestConfig.enabled = true;
            TestConfig.plain_int = 42;
        }

        @Test
        void testRegistrationAndSave() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            assertTrue(spec.path().toFile().exists(), "Config file should be created");

            String content = Files.readString(spec.path(), StandardCharsets.UTF_8);
            assertTrue(content.contains("count"), "File should contain 'count' field");
            assertTrue(content.contains("label"), "File should contain 'label' field");
        }

        @Test
        void testAnnotatedMathExpression() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 2 + 3 * 4
                      ratio: 1.5 * 4
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // 2 + 3*4 = 14, reflected back to static field
            assertEquals(14, TestConfig.count);
            // 1.5 * 4 = 6.0
            assertEquals(6.0, TestConfig.ratio);
        }

        @Test
        void testAnnotatedMathExceedsMax() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10 ^ 3
                      ratio: 1.5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // 1000 > max(100), should reset to default 10
            assertEquals(10, TestConfig.count);
        }

        @Test
        void testAnnotatedStrictMathInvalid() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: abc + 2
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            boolean loaded = spec.load();
            assertFalse(loaded, "Strict math invalid expression should cause load to fail");
        }

        @Test
        void testAnnotatedStringValidation() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      label: ""
                      prefix_field: "goodbye_world"
                      contains_field: "no_match"
                      enabled: true
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            assertEquals("default_value", TestConfig.label, "Empty string should reset (allowEmpty=false)");
            assertEquals("hello_world", TestConfig.prefix_field, "Should reset (doesn't start with 'hello')");
            assertEquals("foo_bar_baz", TestConfig.contains_field, "Should reset (doesn't contain 'bar')");
        }

        @Test
        void testAnnotatedIntMinMax() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 999
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            assertEquals(42, TestConfig.plain_int, "999 > max(200) should reset to default 42");
        }

        @Test
        void testAnnotatedBooleanLoad() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: 10
                      ratio: 1.5
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: false
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            assertFalse(TestConfig.enabled, "Boolean should be loaded as false");
        }

        @Test
        void testAnnotatedReflectMode() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            // Directly modify the static field
            TestConfig.count = 77;

            // Verify the spec field reflects the static field value
            IntField countField = (IntField) spec.findField("count");
            assertEquals(77, countField.get(), "REFLECT mode should read from the static field");
        }

        @Test
        void testAnnotatedSqrtExpression() throws IOException {
            ConfigSpec spec = WaterConfig.register(TestConfig.class);
            spec.save();

            Files.writeString(spec.path(), """
                    {
                      count: ~81
                      ratio: ~25
                      label: "default_value"
                      prefix_field: "hello_world"
                      contains_field: "foo_bar_baz"
                      enabled: true
                      plain_int: 42
                    }
                    """, StandardCharsets.UTF_8);

            spec.load();

            // sqrt(81) = 9
            assertEquals(9, TestConfig.count);
            // sqrt(25) = 5.0
            assertEquals(5.0, TestConfig.ratio);
        }
    }
}
