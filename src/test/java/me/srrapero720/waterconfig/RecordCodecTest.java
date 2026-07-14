package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.codecs.RecordCodec;
import me.srrapero720.waterconfig.impl.fields.ListField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Record codec across many component-type variations: primitives, strings with the codec's
 * own separators, enums, UUIDs, nested records, canonical-constructor validation and
 * List&lt;Record&gt; round-trip / convergence across every format.
 */
public class RecordCodecTest {

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

    // UNICODE STRING BUILT FROM CODE POINTS SO THIS SOURCE STAYS PURE ASCII ("café 你好")
    private static final String UNICODE = "caf" + (char) 0xE9 + " " + (char) 0x4F60 + (char) 0x597D;

    public enum Color { RED, GREEN, BLUE }

    public record Mixed(int i, long l, double d, boolean b) {}
    public record Named(String name, int age) {}
    public record WithEnum(Color color, int shade) {}
    public record WithId(UUID id, String label) {}
    public record Inner(int p, int q) {}
    public record Outer(Inner inner, String tag) {}
    public record Single(String only) {}

    public record Ranged(int lo, int hi) {
        public Ranged {
            if (lo > hi) {
                throw new IllegalArgumentException("lo must not exceed hi");
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Record> T roundTrip(RecordCodec codec, T value, Class<T> type) {
        String encoded = codec.encode(value, type);
        return (T) codec.decode(encoded, (Class) type);
    }

    // ========================================================================
    // COMPONENT-TYPE VARIATIONS (DIRECT CODEC)
    // ========================================================================
    @Nested
    class ComponentTypes {
        private final RecordCodec codec = new RecordCodec();

        @Test
        void mixedPrimitives() {
            Mixed v = new Mixed(-7, 9_000_000_000L, -3.5, true);
            assertEquals(v, roundTrip(codec, v, Mixed.class));
        }

        @Test
        void stringAndInt() {
            Named v = new Named("hello world", 30);
            assertEquals(v, roundTrip(codec, v, Named.class));
        }

        // THE COMPONENT SEPARATOR ('; '), BACKSLASH, COMMAS AND UNICODE MUST SURVIVE ESCAPING
        @Test
        void stringWithSeparatorsBackslashAndUnicode() {
            Named v = new Named("a; b, c \\ d " + UNICODE, 1);
            assertEquals(v, roundTrip(codec, v, Named.class));
        }

        @Test
        void enumComponent() {
            WithEnum v = new WithEnum(Color.BLUE, 42);
            assertEquals(v, roundTrip(codec, v, WithEnum.class));
        }

        @Test
        void uuidComponent() {
            WithId v = new WithId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "primary");
            assertEquals(v, roundTrip(codec, v, WithId.class));
        }

        @Test
        void nestedRecord() {
            Outer v = new Outer(new Inner(3, 4), "tagged");
            assertEquals(v, roundTrip(codec, v, Outer.class));
        }

        @Test
        void singleComponent() {
            Single v = new Single("lonely; value");
            assertEquals(v, roundTrip(codec, v, Single.class));
        }
    }

    // ========================================================================
    // VALIDATION AND MALFORMED INPUT
    // ========================================================================
    @Nested
    class ValidationAndMalformed {
        private final RecordCodec codec = new RecordCodec();

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void canonicalConstructorRejectionDecodesToNull() {
            // THE RECORD'S OWN CONSTRUCTOR CHECK (lo > hi) ACTS AS VALIDATION
            assertNull(codec.decode("5; 2", (Class) Ranged.class));
            assertEquals(new Ranged(2, 5), codec.decode("2; 5", (Class) Ranged.class));
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void arityMismatchDecodesToNull() {
            assertNull(codec.decode("1", (Class) Mixed.class));
            assertNull(codec.decode("1; 2; 3; 4; 5", (Class) Mixed.class));
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void unparseablePrimitiveComponentDecodesToNull() {
            // A NON-NUMERIC PART FOR A PRIMITIVE int COMPONENT CANNOT BE null-ASSIGNED
            assertNull(codec.decode("notint; 5", (Class) Ranged.class));
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void nonRecordSubtypeDecodesToNull() {
            assertNull(codec.decode("anything", (Class) String.class));
        }
    }

    // ========================================================================
    // List<Record> ACROSS FORMATS + CONVERGENCE
    // ========================================================================
    @Nested
    class ListAcrossFormats {

        private final List<Mixed> sample = List.of(
                new Mixed(1, 2L, 3.5, true),
                new Mixed(-4, 5_000_000_000L, -6.25, false),
                new Mixed(0, 0L, 0.0, true)
        );

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void listOfRecordsRoundTrips(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rec_list_" + fmt, fmt, "", 0);
            ListField<Mixed> items = b.defineList("items", new ArrayList<>(sample), Mixed.class).end();
            ConfigSpec spec = b.build();

            spec.save();
            items.setArray(new Mixed[]{new Mixed(99, 99L, 99.0, false)});
            assertTrue(spec.load());
            assertEquals(sample, items.get(), fmt + " must round-trip a List<Record>");
        }

        // EVERY FORMAT MUST CONVERGE ON THE SAME DECODED LIST
        @Test
        void allFormatsConvergeOnTheSameList() throws IOException {
            for (String fmt : new String[]{"properties", "cfg", "json", "json5", "toml"}) {
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("rec_conv_" + fmt, fmt, "", 0);
                ListField<Mixed> items = b.defineList("items", new ArrayList<>(sample), Mixed.class).end();
                ConfigSpec spec = b.build();

                spec.save();
                items.setArray(new Mixed[0]);
                assertTrue(spec.load());
                assertEquals(sample, items.get(), fmt + " diverged from the shared expected list");
            }
        }
    }
}
