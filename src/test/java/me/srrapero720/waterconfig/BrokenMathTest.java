package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.fields.DoubleField;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.formats.special.MathEvaluator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Broken and abusive math expressions: emoji operands, incomplete syntax, alphabetic
 * operands and division by zero, at both the evaluator and the spec-load level.
 */
public class BrokenMathTest {

    // EMOJI BUILT FROM ITS CODE POINT SO THIS SOURCE STAYS PURE ASCII
    private static final String EMOJI = new String(Character.toChars(0x1F600));

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

    private void writeCfg(Path p, String body) throws IOException {
        Files.writeString(p, "{\n" + body + "\n}\n", StandardCharsets.UTF_8);
    }

    // ========================================================================
    // EVALUATOR UNIT BEHAVIOR
    // ========================================================================
    @Nested
    class EvaluatorUnit {

        @Test
        void emojiOperandsThrow() {
            assertThrows(IllegalArgumentException.class, () -> MathEvaluator.evaluate(EMOJI + " + " + EMOJI));
            assertThrows(IllegalArgumentException.class, () -> MathEvaluator.evaluate("2 + " + EMOJI));
        }

        @Test
        void tryEvaluateSwallowsOrRethrowsByStrictness() {
            // NON-STRICT RETURNS null ON GARBAGE; STRICT RETHROWS
            assertNull(MathEvaluator.tryEvaluate(EMOJI + " * 3", false));
            assertNull(MathEvaluator.tryEvaluate("abc + 2", false));
            assertNull(MathEvaluator.tryEvaluate("2 +", false));
            assertThrows(IllegalArgumentException.class, () -> MathEvaluator.tryEvaluate(EMOJI + " * 3", true));
            assertThrows(IllegalArgumentException.class, () -> MathEvaluator.tryEvaluate("2 +", true));
        }

        @Test
        void divisionByZeroYieldsInfinityString() {
            // 5/0 IS Infinity IN IEEE DOUBLE; THE EVALUATOR FORMATS IT AS THE Infinity TOKEN
            assertEquals("Infinity", MathEvaluator.tryEvaluate("5 / 0", false));
        }

        @Test
        void validExpressionsStillEvaluate() {
            assertEquals(14.0, MathEvaluator.evaluate("2 + 3 * 4"));
            assertEquals(25.0, MathEvaluator.evaluate("5 ^ 2"));
            assertEquals(4.0, MathEvaluator.evaluate("~16"));
            assertEquals("14", MathEvaluator.tryEvaluate("2 + 3 * 4", false));
        }
    }

    // ========================================================================
    // SPEC-LOAD BEHAVIOR
    // ========================================================================
    @Nested
    class SpecLoad {

        @Test
        void nonStrictEmojiMathResetsToDefault() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("math_emoji_soft", "cfg", "", 0);
            IntField n = b.defineInt("n", 10).math(true).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: " + EMOJI + " + " + EMOJI);
            assertTrue(spec.load());
            assertEquals(10, n.getAsInt(), "broken non-strict math must reset to default");
        }

        @Test
        void strictEmojiMathAbortsTheLoad() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("math_emoji_strict", "cfg", "", 0);
            b.defineInt("n", 10).math(true).strictMath(true).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: " + EMOJI + " + " + EMOJI);
            // STRICT MATH IS A HARD-FAIL CONTRACT: THE EXCEPTION MUST PROPAGATE OUT OF load()
            assertThrows(IllegalArgumentException.class, () -> { spec.load(); });
        }

        @Test
        void divisionByZeroResetsIntegralField() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("math_divzero_int", "cfg", "", 0);
            IntField n = b.defineInt("n", 7).math(true).end();
            ConfigSpec spec = b.build();

            // Infinity IS NOT A LEGAL int: THE FIELD RESETS RATHER THAN CORRUPTING
            writeCfg(spec.path(), "  n: 5 / 0");
            assertTrue(spec.load());
            assertEquals(7, n.getAsInt());
        }

        @Test
        void divisionByZeroResetsDoubleFieldViaRangeCheck() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("math_divzero_dbl", "cfg", "", 0);
            DoubleField d = b.defineDouble("d", 1.5).math(true).end();
            ConfigSpec spec = b.build();

            // Infinity EXCEEDS THE DEFAULT MAX (Double.MAX_VALUE) SO validate() RESETS IT
            writeCfg(spec.path(), "  d: 5 / 0");
            assertTrue(spec.load());
            assertEquals(1.5, d.getAsDouble());
        }

        @Test
        void garbageInNonMathFieldIsIsolatedAndReset() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("math_nonmath_garbage", "cfg", "", 0);
            IntField n = b.defineInt("n", 3).end();       // NO math: THE EXPRESSION IS JUST AN UNPARSEABLE int
            IntField keep = b.defineInt("keep", 99).end();
            ConfigSpec spec = b.build();

            writeCfg(spec.path(), "  n: " + EMOJI + " + " + EMOJI + "\n  keep: 55");
            assertTrue(spec.load());
            assertEquals(3, n.getAsInt(), "one bad field resets");
            assertEquals(55, keep.getAsInt(), "a sibling field still loads (per-field isolation)");
        }
    }
}
