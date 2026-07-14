package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.fields.*;
import me.srrapero720.waterconfig.impl.formats.*;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Numeric edge cases across the formats: NaN/Infinity representation, negative values,
 * type-range overflow and the float/double default-min regression guard.
 */
public class NumericEdgeCasesTest {

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

    private static IFormatCodec formatFor(String fmt) {
        return switch (fmt) {
            case "json" -> new JSONFormat();
            case "json5" -> new JSON5Format();
            case "toml" -> new TOMLFormat();
            case "cfg" -> new CFGFormat();
            case "properties" -> new PROPFormat();
            default -> throw new IllegalArgumentException(fmt);
        };
    }

    // WRITES ONE ALREADY-ENCODED SCALAR THROUGH THE LOW-LEVEL WRITER AND RETURNS THE FILE TEXT
    private String writeScalar(String fmt, String encoded, Class<?> type) throws IOException {
        Path file = tempDir.resolve("scalar_" + fmt + "." + fmt);
        IFormatWriter w = formatFor(fmt).createWriter(file);
        w.push("spec");
        w.write("d", encoded, type, null);
        w.pop();
        w.close();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    // ========================================================================
    // NaN / INFINITY REPRESENTATION
    // ========================================================================
    @Nested
    class SpecialFloats {

        // PLAIN JSON HAS NO NaN/Infinity LITERAL: THE WRITER MUST EMIT null, NEVER A BARE TOKEN
        @ParameterizedTest
        @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
        void jsonEmitsNullForNonRepresentableFloats(String special) throws IOException {
            String out = writeScalar("json", special, Double.class);
            assertTrue(out.contains("\"d\": null"), "plain JSON must emit null for " + special + ": " + out);
            assertFalse(out.contains(special), "plain JSON must not emit the bare token " + special + ": " + out);
        }

        // A FILE THE JSON WRITER PRODUCED MUST ALWAYS BE READABLE BY THE JSON READER
        @Test
        void jsonNanOutputIsSelfReadable() throws IOException {
            Path file = tempDir.resolve("selfread.json");
            IFormatWriter w = new JSONFormat().createWriter(file);
            w.push("spec");
            w.write("d", "NaN", Double.class, null);
            w.pop();
            w.close();
            // THE OLD BUG WROTE A BARE NaN THAT THE READER THREW ON; null MUST PARSE CLEANLY
            assertEquals("null", new JSONFormat().createReader(file).read("d"));
        }

        @Test
        void json5KeepsNanAndInfinityTokens() throws IOException {
            assertTrue(writeScalar("json5", "NaN", Double.class).contains("NaN"));
            assertTrue(writeScalar("json5", "Infinity", Double.class).contains("Infinity"));
        }

        @Test
        void tomlSpellsSpecialFloatsLowercase() throws IOException {
            assertTrue(writeScalar("toml", "NaN", Double.class).contains("nan"));
            assertTrue(writeScalar("toml", "Infinity", Double.class).contains("inf"));
            assertTrue(writeScalar("toml", "-Infinity", Double.class).contains("-inf"));
        }

        // NaN SURVIVES A FULL FIELD ROUND-TRIP ON EVERY FORMAT THAT CAN SPELL IT; PLAIN JSON RESETS
        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json5", "toml"})
        void nanSurvivesRoundTrip(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_nan_" + fmt, fmt, "", 0);
            DoubleField d = b.defineDouble("d", 1.0).end();
            ConfigSpec spec = b.build();

            d.set(Double.NaN);
            spec.save();
            d.set(0.0);
            assertTrue(spec.load());
            assertTrue(Double.isNaN(d.getAsDouble()), fmt + " must round-trip NaN");
        }

        @Test
        void plainJsonResetsNanToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_nan_json", "json", "", 0);
            DoubleField d = b.defineDouble("d", 1.0).end();
            ConfigSpec spec = b.build();

            d.set(Double.NaN);
            spec.save();
            d.set(0.0);
            assertTrue(spec.load());
            // PLAIN JSON CANNOT STORE NaN, SO THE HONEST BEST-EFFORT IS A RESET TO DEFAULT
            assertEquals(1.0, d.getAsDouble());
        }
    }

    // ========================================================================
    // NEGATIVE VALUES
    // ========================================================================
    @Nested
    class Negatives {

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void negativeScalarsRoundTrip(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_neg_" + fmt, fmt, "", 0);
            IntField i = b.defineInt("i", 0).setMin(Integer.MIN_VALUE).setMax(Integer.MAX_VALUE).end();
            LongField l = b.defineLong("l", 0L).setMin(Long.MIN_VALUE).setMax(Long.MAX_VALUE).end();
            DoubleField d = b.defineDouble("d", 0.0).end();
            FloatField f = b.defineFloat("f", 0F).end();
            ConfigSpec spec = b.build();

            i.set(-2147483648);   // Integer.MIN_VALUE
            l.set(-9000000000L);  // BEYOND int RANGE, NEGATIVE
            d.set(-3.14159);
            f.set(-0.5F);
            spec.save();
            i.set(0); l.set(0L); d.set(0.0); f.set(0F);

            assertTrue(spec.load());
            assertEquals(Integer.MIN_VALUE, i.getAsInt(), fmt);
            assertEquals(-9000000000L, l.getAsLong(), fmt);
            assertEquals(-3.14159, d.getAsDouble(), fmt);
            assertEquals(-0.5F, f.getAsFloat(), fmt);
        }
    }

    // ========================================================================
    // TYPE-RANGE OVERFLOW
    // ========================================================================
    @Nested
    class Overflows {

        private void writeCfg(Path p, String body) throws IOException {
            Files.writeString(p, "{\n" + body + "\n}\n", StandardCharsets.UTF_8);
        }

        // BOUNDARY VALUES MUST SURVIVE INTACT
        @Test
        void boundaryValuesRoundTrip() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_bounds", "cfg", "", 0);
            IntField i = b.defineInt("i", 0).setMin(Integer.MIN_VALUE).setMax(Integer.MAX_VALUE).end();
            LongField l = b.defineLong("l", 0L).setMin(Long.MIN_VALUE).setMax(Long.MAX_VALUE).end();
            ByteField by = b.defineByte("by", (byte) 0).setMin(Byte.MIN_VALUE).setMax(Byte.MAX_VALUE).end();
            ShortField sh = b.defineShort("sh", (short) 0).setMin(Short.MIN_VALUE).setMax(Short.MAX_VALUE).end();
            ConfigSpec spec = b.build();

            i.set(Integer.MAX_VALUE);
            l.set(Long.MAX_VALUE);
            by.set(Byte.MIN_VALUE);
            sh.set(Short.MAX_VALUE);
            spec.save();
            i.set(0); l.set(0L); by.set((byte) 0); sh.set((short) 0);

            assertTrue(spec.load());
            assertEquals(Integer.MAX_VALUE, i.getAsInt());
            assertEquals(Long.MAX_VALUE, l.getAsLong());
            assertEquals(Byte.MIN_VALUE, by.getAsByte());
            assertEquals(Short.MAX_VALUE, sh.getAsShort());
        }

        // A VALUE BEYOND THE TYPE RANGE FAILS TO DECODE AND RESETS TO DEFAULT, NEVER CORRUPTS
        @Test
        void beyondIntRangeResetsToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_ovf_int", "cfg", "", 0);
            IntField i = b.defineInt("i", 42).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  i: 9999999999");  // > Integer.MAX_VALUE
            assertTrue(spec.load());
            assertEquals(42, i.getAsInt(), "int overflow must reset to default");
        }

        @Test
        void beyondByteRangeResetsToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_ovf_byte", "cfg", "", 0);
            ByteField by = b.defineByte("by", (byte) 7).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  by: 999");  // > Byte.MAX_VALUE
            assertTrue(spec.load());
            assertEquals((byte) 7, by.getAsByte(), "byte overflow must reset to default");
        }

        // A VALUE INSIDE THE TYPE BUT OUTSIDE THE CONFIGURED [min,max] RESETS ON VALIDATE
        @Test
        void beyondConfiguredMaxResetsToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_ovf_cfgmax", "cfg", "", 0);
            IntField i = b.defineInt("i", 50).setMin(0).setMax(100).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  i: 500");
            assertTrue(spec.load());
            assertEquals(50, i.getAsInt(), "value above configured max must reset to default");
        }
    }

    // ========================================================================
    // FLOAT/DOUBLE DEFAULT-MIN REGRESSION GUARD
    // ========================================================================
    @Nested
    class DefaultMinRegression {

        @Spec(value = "num_defaultmin", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class OnlyMaxCfg {
            // ONLY A MAX IS SET: THE DEFAULT MIN MUST BE -MAX_VALUE, NOT THE TINY POSITIVE MIN_VALUE
            @Spec.Field @NumberConditions(maxDouble = 100.0) public static double gain = 0.0;
            @Spec.Field @NumberConditions(maxFloat = 100.0F) public static float trim = 0.0F;
        }

        @Test
        void zeroAndNegativeSurviveWhenOnlyMaxIsSet() throws IOException {
            ConfigSpec spec = WaterConfig.registerBlocking(OnlyMaxCfg.class);
            spec.setDirty(false);

            Files.writeString(spec.path(), """
                    {
                      gain: -50.0
                      trim: -12.5
                    }
                    """, StandardCharsets.UTF_8);
            assertTrue(spec.load());

            // WITH A WRONG POSITIVE DEFAULT MIN THESE WOULD RESET TO 0.0
            assertEquals(-50.0, OnlyMaxCfg.gain, "negative double must survive when only a max is set");
            assertEquals(-12.5F, OnlyMaxCfg.trim, "negative float must survive when only a max is set");
        }

        @Test
        void zeroSurvivesWhenOnlyMaxIsSet() throws IOException {
            ConfigSpec spec = WaterConfig.registerBlocking(OnlyMaxCfg.class);
            spec.setDirty(false);

            Files.writeString(spec.path(), """
                    {
                      gain: 0.0
                      trim: 0.0
                    }
                    """, StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(0.0, OnlyMaxCfg.gain);
            assertEquals(0.0F, OnlyMaxCfg.trim);
        }
    }

    // ========================================================================
    // NaN AS A DEFAULT (NULL-SENTINEL STYLE)
    // ========================================================================
    @Nested
    class NanAsDefault {

        @Test
        void nanDefaultIsReadableAndResettable() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_nandef", "cfg", "", 0);
            DoubleField d = b.defineDouble("d", Double.NaN).end();
            b.build();

            assertTrue(Double.isNaN(d.getAsDouble()), "a NaN default is readable as NaN");
            d.set(5.0);
            assertEquals(5.0, d.getAsDouble());
            d.reset();
            assertTrue(Double.isNaN(d.getAsDouble()), "reset restores the NaN sentinel");
        }

        // A NaN DEFAULT CONVERGES BACK TO NaN ON EVERY FORMAT: THE CAPABLE ONES STORE IT,
        // PLAIN JSON STORES null AND RESETS TO THE NaN DEFAULT — EITHER WAY THE FIELD ENDS NaN
        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void nanDefaultRoundTripsToNanEverywhere(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_nandef_" + fmt, fmt, "", 0);
            DoubleField d = b.defineDouble("d", Double.NaN).end();
            ConfigSpec spec = b.build();

            spec.save();
            d.set(0.0);
            assertTrue(spec.load());
            assertTrue(Double.isNaN(d.getAsDouble()), fmt + " with a NaN default must load back as NaN");
        }

        @Test
        void realValueOverridesTheNanSentinel() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("num_nandef_real", "cfg", "", 0);
            DoubleField d = b.defineDouble("d", Double.NaN).end();
            ConfigSpec spec = b.build();

            Files.writeString(spec.path(), "{\n  d: 42.5\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
            assertEquals(42.5, d.getAsDouble(), "a real value overrides the NaN sentinel");
        }
    }
}
