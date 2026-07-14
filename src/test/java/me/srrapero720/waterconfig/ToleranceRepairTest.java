package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.fields.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Per-element array tolerance (keep decodable entries, drop the rest) and repair-mode value
 * coercion (convert a codec-rejected value to the target type: "3.14" -> 3, "1" -> true, 1 -> "1").
 */
public class ToleranceRepairTest {

    public enum Color { RED, GREEN, BLUE }

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() {
        WaterConfig.init();
        WaterConfig.setPath(tempDir);
    }

    @AfterAll
    static void teardown() {
        WaterConfig.unloadAll();
    }

    // WRITES A ONE-FIELD FILE IN THE GIVEN FORMAT'S SYNTAX (VALUE VERBATIM, UNESCAPED)
    private void writeScalar(Path p, String fmt, String key, String val) throws IOException {
        String content = switch (fmt) {
            case "properties" -> key + "=" + val + "\n";
            case "toml" -> key + " = " + val + "\n";
            case "cfg" -> "{\n  " + key + ": " + val + "\n}\n";
            case "json", "json5" -> "{\n  \"" + key + "\": " + val + "\n}\n";
            default -> throw new IllegalArgumentException(fmt);
        };
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }

    private void writeCfg(Path p, String body) throws IOException {
        Files.writeString(p, "{\n" + body + "\n}\n", StandardCharsets.UTF_8);
    }

    // ========================================================================
    // PER-ELEMENT ARRAY TOLERANCE
    // ========================================================================
    @Nested
    class PerElementTolerance {

        @Test
        void oneBadElementDroppedRestKept() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_one", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"nums\": [1, 2, \"hello\", 4]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 4), nums.get());
        }

        @Test
        void multipleScatteredBadElementsDropped() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_multi", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"nums\": [1, \"a\", 2, \"b\", 3]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 3), nums.get());
        }

        @Test
        void allBadElementsYieldEmptyListWhenAllowed() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_allbad", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).allowEmpty(true).end();
            ConfigSpec spec = b.build();

            nums.setArray(new Integer[]{9});
            Files.writeString(spec.path(), "{\n\t\"nums\": [\"a\", \"b\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertTrue(nums.get().isEmpty(), "all elements dropped leaves an empty list when empty is allowed");
        }

        @Test
        void allBadElementsResetToDefaultWhenEmptyDisallowed() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_allbad_noempty", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(1, 2)), Integer.class).allowEmpty(false).end();
            ConfigSpec spec = b.build();

            nums.setArray(new Integer[]{9});
            Files.writeString(spec.path(), "{\n\t\"nums\": [\"a\", \"b\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            // AN EMPTY RESULT WITH allowEmpty=false RESETS TO THE DEFAULT LIST
            assertEquals(List.of(1, 2), nums.get());
        }

        @Test
        void toleranceThenUniqueValidationCompose() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_unique", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).unique(true).end();
            ConfigSpec spec = b.build();

            // DROP THE BAD ELEMENT, THEN DEDUPLICATE THE SURVIVORS
            Files.writeString(spec.path(), "{\n\t\"nums\": [1, 2, \"x\", 2, 3]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 3), nums.get());
        }

        @Test
        void badEnumElementDropped() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_enum", "json", "", 0);
            ListField<Color> colors = b.defineList("colors", new ArrayList<>(List.of(Color.RED)), Color.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"colors\": [\"RED\", \"PURPLE\", \"BLUE\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(Color.RED, Color.BLUE), colors.get(), "an unknown enum constant is dropped");
        }

        @Test
        void badDoubleElementDropped() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("tol_double", "json", "", 0);
            ListField<Double> vals = b.defineList("vals", new ArrayList<>(List.of(0.0)), Double.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"vals\": [1.5, \"hello\", 2.5]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1.5, 2.5), vals.get());
        }
    }

    // ========================================================================
    // REPAIR-MODE VALUE COERCION
    // ========================================================================
    @Nested
    class RepairCoercion {

        // THE USER'S EXAMPLE: A NUMBER FIELD READS "1" AS 1
        @Test
        void numberFieldParsesIntString() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_numstr", "cfg", "", 0);
            IntField n = b.defineInt("n", 99).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: 1");
            assertTrue(spec.load());
            assertEquals(1, n.getAsInt());
        }

        // THE USER'S EXAMPLE: A STRING FIELD CONVERTS THE NUMERIC TOKEN 1 INTO "1"
        @Test
        void stringFieldTakesNumericTokenVerbatim() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_strnum", "cfg", "", 0);
            StringField s = b.defineString("s", "def").end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  s: 1");
            assertTrue(spec.load());
            assertEquals("1", s.get());
        }

        @Test
        void intFieldTruncatesDecimalString() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_inttrunc", "cfg", "", 0);
            IntField n = b.defineInt("n", 99).setMin(-1000).setMax(1000).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: 3.14");
            assertTrue(spec.load());
            assertEquals(3, n.getAsInt(), "a decimal string is repaired to a truncated int");
        }

        @Test
        void longAndShortAndByteTruncate() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_narrow", "cfg", "", 0);
            LongField l = b.defineLong("l", 0L).setMin(-100L).setMax(100L).end();
            ShortField sh = b.defineShort("sh", (short) 0).setMin((short) -100).setMax((short) 100).end();
            ByteField by = b.defineByte("by", (byte) 0).setMin((byte) -100).setMax((byte) 100).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  l: 9.99\n  sh: 3.9\n  by: 5.5");
            assertTrue(spec.load());
            assertEquals(9L, l.getAsLong());
            assertEquals((short) 3, sh.getAsShort());
            assertEquals((byte) 5, by.getAsByte());
        }

        @Test
        void booleanAcceptsTruthyAndFalsySpellings() throws IOException {
            for (String truthy : new String[]{"true", "1", "yes", "on"}) {
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_booltrue_" + truthy, "cfg", "", 0);
                BooleanField flag = b.defineBoolean("flag", false).end();
                ConfigSpec spec = b.build();
                writeCfg(spec.path(), "  flag: \"" + truthy + "\"");
                assertTrue(spec.load());
                assertTrue(flag.getAsBoolean(), "'" + truthy + "' must coerce to true");
            }
            for (String falsy : new String[]{"false", "0", "no", "off"}) {
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_boolfalse_" + falsy, "cfg", "", 0);
                BooleanField flag = b.defineBoolean("flag", true).end();
                ConfigSpec spec = b.build();
                writeCfg(spec.path(), "  flag: \"" + falsy + "\"");
                assertTrue(spec.load());
                assertFalse(flag.getAsBoolean(), "'" + falsy + "' must coerce to false");
            }
        }

        @Test
        void uncoercibleValueResetsToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_uncoercible", "cfg", "", 0);
            IntField n = b.defineInt("n", 42).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: \"notanumber\"");
            assertTrue(spec.load());
            assertEquals(42, n.getAsInt(), "a value with no numeric meaning resets to default");
        }

        @Test
        void coercionThenRangeValidation() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_rangeafter", "cfg", "", 0);
            IntField inRange = b.defineInt("inRange", 0).setMin(0).setMax(10).end();
            IntField outRange = b.defineInt("outRange", 5).setMin(0).setMax(10).end();
            ConfigSpec spec = b.build();

            // 3.14 -> 3 IS IN RANGE; 50.9 -> 50 EXCEEDS max AND RESETS ON validate()
            writeCfg(spec.path(), "  inRange: 3.14\n  outRange: 50.9");
            assertTrue(spec.load());
            assertEquals(3, inRange.getAsInt());
            assertEquals(5, outRange.getAsInt(), "a coerced value still obeys the configured range");
        }

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void decimalCoercesToIntAcrossFormats(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_conv_" + fmt, fmt, "", 0);
            IntField n = b.defineInt("n", 0).setMin(-100).setMax(100).end();
            ConfigSpec spec = b.build();

            writeScalar(spec.path(), fmt, "n", "7.5");
            assertTrue(spec.load());
            assertEquals(7, n.getAsInt(), fmt + " must repair 7.5 to 7");
        }

        @Test
        void listElementsAreRepairCoerced() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_listcoerce", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            ConfigSpec spec = b.build();

            // "2.7" IS REPAIRED TO 2, NOT DROPPED
            Files.writeString(spec.path(), "{\n\t\"nums\": [\"1\", \"2.7\", \"3\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 3), nums.get());
        }

        @Test
        void booleanListRepairsAndDrops() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rep_boollist", "json", "", 0);
            ListField<Boolean> flags = b.defineList("flags", new ArrayList<>(List.of(false)), Boolean.class).end();
            ConfigSpec spec = b.build();

            // true/1 -> true, no -> false, xyz -> dropped
            Files.writeString(spec.path(), "{\n\t\"flags\": [\"true\", \"1\", \"no\", \"xyz\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(true, true, false), flags.get());
        }
    }
}
