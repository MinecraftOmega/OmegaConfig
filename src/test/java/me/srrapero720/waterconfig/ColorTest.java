package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.ColorConditions.Radix;
import me.srrapero720.waterconfig.impl.codecs.ColorCodec;
import me.srrapero720.waterconfig.impl.fields.ColorField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Color codec and field: the five {@code @ColorConditions} radixes (hex AUTO/OPAQUE/ALPHA and the
 * byte-split groups), radix migration on load, and cross-format convergence.
 */
public class ColorTest {

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

    private ColorField colorField(String name, String fmt, Radix radix, Color def) {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder(name, fmt, "", 0);
        ColorField f = b.defineColor("col", def).radix(radix).end();
        b.build();
        return f;
    }

    // ========================================================================
    // CODEC UNIT
    // ========================================================================
    @Nested
    class CodecUnit {

        @Test
        void parseHexAllForms() {
            assertEquals(new Color(255, 0, 0, 255), ColorCodec.parseHex("#f00"));
            assertEquals(new Color(255, 128, 0, 255), ColorCodec.parseHex("#ff8000"));
            assertEquals(new Color(255, 0, 0, 255), ColorCodec.parseHex("#f00f"));
            assertEquals(new Color(255, 0, 0, 128), ColorCodec.parseHex("#ff000080"));
            assertEquals(new Color(255, 128, 0, 255), ColorCodec.parseHex("ff8000"), "leading # is optional");
            assertNull(ColorCodec.parseHex("#12"), "unsupported length");
            assertNull(ColorCodec.parseHex("#gggggg"), "non-hex digits");
        }

        @Test
        void hexPicksShortestLosslessForm() {
            assertEquals("#f00", ColorCodec.hex(new Color(255, 0, 0, 255), false));
            assertEquals("#ff8000", ColorCodec.hex(new Color(255, 128, 0, 255), false));
            assertEquals("#f000", ColorCodec.hex(new Color(255, 0, 0, 0), true));
            assertEquals("#ff000080", ColorCodec.hex(new Color(255, 0, 0, 128), true));
        }

        @Test
        void splitAndMergeRoundTrip() {
            Color c = new Color(10, 20, 30, 40);
            assertArrayEquals(new String[]{"10", "20", "30", "40"}, ColorCodec.split(c, true));
            assertArrayEquals(new String[]{"10", "20", "30"}, ColorCodec.split(c, false));
            assertEquals(c, ColorCodec.merge(new String[]{"10", "20", "30", "40"}));
            assertEquals(new Color(10, 20, 30, 255), ColorCodec.merge(new String[]{"10", "20", "30", null}));
        }
    }

    // ========================================================================
    // HEX RADIXES
    // ========================================================================
    @Nested
    class HexModes {

        @Test
        void autoRoundTripsOpaqueAndAlpha() throws IOException {
            ColorField f = colorField("col_auto", "cfg", Radix.AUTO, Color.WHITE);
            f.set(new Color(255, 128, 0, 200));
            f.spec().save();
            f.set(Color.WHITE);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 128, 0, 200), f.get());
        }

        @Test
        void opaqueDropsAlphaOnSave() throws IOException {
            ColorField f = colorField("col_opaque", "cfg", Radix.OPAQUE, Color.WHITE);
            f.set(new Color(255, 0, 0, 128));
            f.spec().save();

            String content = Files.readString(f.spec().path(), StandardCharsets.UTF_8);
            assertTrue(content.contains("#f00") && !content.contains("#f00f") && !content.contains("80"), "opaque hex, no alpha: " + content);

            f.set(Color.WHITE);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 0, 0, 255), f.get(), "alpha is forced opaque");
        }

        @Test
        void alphaAlwaysKeepsAlpha() throws IOException {
            ColorField f = colorField("col_alpha", "cfg", Radix.ALPHA, Color.WHITE);
            f.set(new Color(255, 0, 0, 255));
            f.spec().save();

            String content = Files.readString(f.spec().path(), StandardCharsets.UTF_8);
            assertTrue(content.contains("#f00f"), "alpha hex keeps the alpha nibble: " + content);

            f.set(Color.WHITE);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 0, 0, 255), f.get());
        }
    }

    // ========================================================================
    // BYTE-SPLIT RADIXES (RENDERED AS A GROUP)
    // ========================================================================
    @Nested
    class ByteSplit {

        @Test
        void byteSplitRendersGroupAndRoundTrips() throws IOException {
            ColorField f = colorField("col_split", "cfg", Radix.BYTE_SPLIT, Color.WHITE);
            f.set(new Color(255, 128, 0));
            f.spec().save();

            String content = Files.readString(f.spec().path(), StandardCharsets.UTF_8);
            // A GROUP OF DECIMAL CHANNELS, NOT A HEX STRING
            assertFalse(content.contains("#"), "must not be hex: " + content);
            assertTrue(content.contains("255") && content.contains("128"), "channels as a group: " + content);

            f.set(Color.WHITE);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 128, 0, 255), f.get());
        }

        @Test
        void byteSplitAlphaRoundTripsExactly() throws IOException {
            ColorField f = colorField("col_split_a", "cfg", Radix.BYTE_SPLIT_ALPHA, Color.WHITE);
            f.set(new Color(255, 128, 0, 64));
            f.spec().save();
            f.set(Color.WHITE);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 128, 0, 64), f.get());
        }

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        void byteSplitAlphaConvergesAcrossFormats(String fmt) throws IOException {
            ColorField f = colorField("col_conv_" + fmt, fmt, Radix.BYTE_SPLIT_ALPHA, Color.WHITE);
            Color target = new Color(12, 200, 44, 90);
            f.set(target);
            f.spec().save();
            f.set(Color.BLACK);
            assertTrue(f.spec().load());
            assertEquals(target, f.get(), fmt + " must round-trip a byte-split color");
        }
    }

    // ========================================================================
    // RADIX MIGRATION (READ IS TOLERANT TO ANY SUPPORTED FORM)
    // ========================================================================
    @Nested
    class Migration {

        @Test
        void splitFieldReadsAnOldHexFile() throws IOException {
            ColorField f = colorField("col_mig_hex", "cfg", Radix.BYTE_SPLIT, Color.WHITE);
            // AN OLD FILE STILL STORED THE COLOR AS HEX
            Files.writeString(f.spec().path(), "{\n  col: \"#ff8000\"\n}\n", StandardCharsets.UTF_8);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 128, 0, 255), f.get());
        }

        @Test
        void hexFieldReadsAnOldSplitGroup() throws IOException {
            ColorField f = colorField("col_mig_split", "cfg", Radix.AUTO, Color.WHITE);
            // AN OLD FILE STORED THE COLOR AS A CHANNEL GROUP
            Files.writeString(f.spec().path(), "{\n  col: {\n    r: 255\n    g: 128\n    b: 0\n  }\n}\n", StandardCharsets.UTF_8);
            assertTrue(f.spec().load());
            assertEquals(new Color(255, 128, 0, 255), f.get());
        }
    }
}
