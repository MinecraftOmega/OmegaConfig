package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.impl.fields.ListField;
import me.srrapero720.waterconfig.impl.formats.JSONFormat;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Array/list edge cases: very large element counts, heterogeneous element types interspersed
 * in the file, and excessively long individual values. Guards against O(n^2) blow-ups,
 * parser stack overflow, silent corruption on type mismatch and memory pathologies.
 */
public class ArrayEdgeCasesTest {

    // UNICODE BUILT FROM CODE POINTS SO THIS SOURCE STAYS PURE ASCII ("café 你好")
    private static final String UNICODE = "caf" + (char) 0xE9 + " " + (char) 0x4F60 + (char) 0x597D;

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

    // ========================================================================
    // VERY LARGE ARRAYS
    // ========================================================================
    @Nested
    class VeryLargeArrays {

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void largeIntListRoundTrips(String fmt) {
            assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
                final int n = 50_000;
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_big_" + fmt, fmt, "", 0);
                ListField<Integer> items = b.defineList("items", new ArrayList<>(List.of(0)), Integer.class).end();
                ConfigSpec spec = b.build();

                Integer[] huge = new Integer[n];
                for (int i = 0; i < n; i++) {
                    huge[i] = i;
                }
                items.setArray(huge);
                spec.save();
                items.setArray(new Integer[]{-1});
                assertTrue(spec.load());

                List<Integer> loaded = items.get();
                assertEquals(n, loaded.size(), fmt + " must preserve every element");
                assertEquals(0, loaded.get(0));
                assertEquals(n / 2, loaded.get(n / 2));
                assertEquals(n - 1, loaded.get(n - 1));
            });
        }

        // ONE VERY, VERY LONG ARRAY MUST NOT BLOW THE PARSER STACK OR DEGRADE TO O(n^2)
        @Test
        void extremelyLongIntListRoundTrips() {
            assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
                final int n = 500_000;
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_extreme", "cfg", "", 0);
                ListField<Integer> items = b.defineList("items", new ArrayList<>(List.of(0)), Integer.class).end();
                ConfigSpec spec = b.build();

                Integer[] huge = new Integer[n];
                for (int i = 0; i < n; i++) {
                    huge[i] = i;
                }
                items.setArray(huge);
                spec.save();
                items.setArray(new Integer[]{-1});
                assertTrue(spec.load());
                assertEquals(n, items.get().size());
                assertEquals(n - 1, items.get().get(n - 1));
            });
        }

        @Test
        void largeStringListRoundTrips() {
            assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
                final int n = 100_000;
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_bigstr", "cfg", "", 0);
                ListField<String> items = b.defineList("items", new ArrayList<>(List.of("seed")), String.class).end();
                ConfigSpec spec = b.build();

                String[] huge = new String[n];
                for (int i = 0; i < n; i++) {
                    huge[i] = "value-" + i;
                }
                items.setArray(huge);
                spec.save();
                items.setArray(new String[]{"wiped"});
                assertTrue(spec.load());
                assertEquals(n, items.get().size());
                assertEquals("value-0", items.get().get(0));
                assertEquals("value-" + (n - 1), items.get().get(n - 1));
            });
        }
    }

    // ========================================================================
    // HETEROGENEOUS ELEMENT TYPES INTERSPERSED
    // ========================================================================
    @Nested
    class MixedElementTypes {

        private IFormatReader jsonWith(String name, String content) throws IOException {
            Path file = tempDir.resolve(name);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return new JSONFormat().createReader(file);
        }

        // QUOTED AND UNQUOTED NUMERIC TOKENS BOTH DECODE: (1, "2", 3, "4") -> [1,2,3,4]
        @Test
        void mixedQuotedAndUnquotedIntsAllParse() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_mixq", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"nums\": [1, \"2\", 3, \"4\"]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 3, 4), nums.get(), "mixed quoting must still decode as ints");
        }

        // PER-ELEMENT TOLERANCE: A GENUINELY WRONG-TYPE ELEMENT IS DROPPED, THE REST SURVIVE
        @Test
        void wrongTypeElementIsDroppedKeepingTheRest() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_wrongtype", "json", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(0)), Integer.class).end();
            ConfigSpec spec = b.build();

            nums.setArray(new Integer[]{7, 8});
            Files.writeString(spec.path(), "{\n\t\"nums\": [1, 2, \"hello\", 4]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of(1, 2, 4), nums.get(), "the non-numeric element is dropped, the rest are kept");
        }

        // FOR A String LIST EVERY TOKEN IS A STRING, SO HETEROGENEOUS TOKENS ALL SURVIVE
        @Test
        void stringListAbsorbsHeterogeneousTokens() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_hetstr", "json", "", 0);
            ListField<String> items = b.defineList("items", new ArrayList<>(List.of("x")), String.class).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n\t\"items\": [\"a\", 1, true]\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(List.of("a", "1", "true"), items.get(), "a string list turns every token into a string");
        }

        // A DIRECTLY READ ARRAY WITH MIXED TOKENS PRESERVES ORDER AND EVERY RAW STRING
        @Test
        void readerReturnsEveryMixedTokenInOrder() throws IOException {
            IFormatReader reader = jsonWith("mixraw.json", "{\n\t\"a\": [1, \"two\", 3, \"four\", 5]\n}\n");
            assertArrayEquals(new String[]{"1", "two", "3", "four", "5"}, reader.readArray("a"));
            reader.close();
        }
    }

    // ========================================================================
    // EXCESSIVELY LONG VALUES (MEMORY PRESSURE)
    // ========================================================================
    @Nested
    class ExcessivelyLongValues {

        @ParameterizedTest
        @ValueSource(strings = {"cfg", "json", "json5", "toml", "properties"})
        void multiMegabyteStringValueRoundTrips(String fmt) {
            assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
                // ~4 MB VALUE PACKED WITH QUOTES, BACKSLASHES AND UNICODE TO STRESS ESCAPING
                String chunk = "abc\"def\\ghi\t" + UNICODE + " ";
                StringBuilder sb = new StringBuilder(4_200_000);
                while (sb.length() < 4_000_000) {
                    sb.append(chunk);
                }
                String big = sb.toString();

                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_bigval_" + fmt, fmt, "", 0);
                me.srrapero720.waterconfig.impl.fields.StringField st = b.defineString("st", "x").end();
                ConfigSpec spec = b.build();

                st.set(big);
                spec.save();
                st.set("wiped");
                assertTrue(spec.load());
                assertEquals(big, st.get(), fmt + " must round-trip a multi-megabyte value");
            });
        }

        // MANY LARGE ELEMENTS AT ONCE: ~10 MB TOTAL SPREAD ACROSS 500 x 20 KB ENTRIES
        @Test
        void arrayOfLargeStringElementsRoundTrips() {
            assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
                final int count = 500;
                final int size = 20_000;
                String base = ("x" + UNICODE).repeat(size / 5);
                String[] elements = new String[count];
                for (int i = 0; i < count; i++) {
                    elements[i] = i + ":" + base;
                }

                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("arr_biglist", "cfg", "", 0);
                ListField<String> items = b.defineList("items", new ArrayList<>(List.of("seed")), String.class).end();
                ConfigSpec spec = b.build();

                items.setArray(elements);
                spec.save();
                items.setArray(new String[]{"wiped"});
                assertTrue(spec.load());

                assertEquals(count, items.get().size());
                assertEquals(elements[0], items.get().get(0));
                assertEquals(elements[count - 1], items.get().get(count - 1));
            });
        }
    }
}
