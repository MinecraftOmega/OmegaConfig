package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.impl.fields.BooleanField;
import me.srrapero720.waterconfig.impl.fields.DoubleField;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.ListField;
import me.srrapero720.waterconfig.impl.fields.LongField;
import me.srrapero720.waterconfig.impl.fields.StringField;
import me.srrapero720.waterconfig.impl.formats.CFGFormat;
import me.srrapero720.waterconfig.impl.formats.JSONFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static me.srrapero720.waterconfig.WaterConfigRegistry.FORMATS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Extreme and human-error cases: single-bit corruption, unexpected value types,
 * math edge cases, emoji/unicode payloads, hostile keys, files mangled by the
 * user's editor (BOM, CRLF, casing, stray spaces) and depth bombs.
 * This source deliberately contains raw UTF-8 literals (emoji, RTL, CJK): the file
 * itself must survive the UTF-8 toolchain the same way config files must.
 */
public class ExtremeCasesTest {
    private static final Duration LIMIT = Duration.ofSeconds(60);

    // EMOJI AND UNICODE SAMPLES (SURROGATE PAIRS, ZWJ SEQUENCES, FLAGS, SKIN TONES, RTL, COMBINING)
    private static final String COW = new String(Character.toChars(0x1F404));
    private static final String MILK = new String(Character.toChars(0x1F95B));
    private static final String FAMILY = "👨‍👩‍👧‍👦";
    private static final String FLAG = "🇪🇸";
    private static final String THUMB_TONE = "👍🏽";
    private static final String RTL = "مرحبا שלום";
    private static final String CJK = "配置文件";
    private static final String ACCENTS = "áéíóúñü";
    private static final String COMBINING = "Źäļg͂o̵";
    private static final String SYMBOLS = "©®™°±×÷§¶†‡…";

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

    // ══════════════════════════════════════════════════════════
    //  1. SINGLE-BIT CORRUPTION — THE 11000001 -> 11000000 CLASS OF FAILURES
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void singleBitCorruptionNeverHangsNorNulls(String format) {
        assertTimeoutPreemptively(LIMIT, () -> {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_bits_" + format, format, "", 0);
            IntField num = b.defineInt("num", 5).end();
            StringField txt = b.defineString("txt", "fallback").end();
            BooleanField flag = b.defineBoolean("flag", true).end();
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(1, 2)), Integer.class).end();
            ConfigSpec spec = b.build();

            num.set(42);
            txt.set("caf" + ACCENTS); // MULTI-BYTE UTF-8 SO LEAD BYTES (0xC3...) GET BIT-FLIPPED TOO
            flag.set(false);
            nums.set(List.of(7, 8, 9));
            spec.save();
            byte[] valid = Files.readAllBytes(spec.path());

            // FLIP THE LOWEST BIT OF EVERY SINGLE BYTE, ONE AT A TIME
            for (int pos = 0; pos < valid.length; pos++) {
                byte[] corrupted = valid.clone();
                corrupted[pos] ^= 0x01;
                Files.write(spec.path(), corrupted);
                try {
                    spec.load();
                } catch (Exception e) {
                    // A CLEAN FAILURE IS ACCEPTABLE — ERRORS AND HANGS ARE NOT
                    assertTrue(e instanceof IOException || e instanceof RuntimeException,
                            "Unexpected exception kind at byte " + pos + ": " + e.getClass().getName());
                }

                assertNotNull(num.get(), "int left null after flipping byte " + pos);
                assertNotNull(txt.get(), "string left null after flipping byte " + pos);
                assertNotNull(flag.get(), "boolean left null after flipping byte " + pos);
                assertNotNull(nums.get(), "list left null after flipping byte " + pos);
            }

            // ALSO FLIP A HIGH BIT MID-FILE (CORRUPTS UTF-8 LEAD BYTES INTO CONTINUATIONS)
            byte[] corrupted = valid.clone();
            corrupted[valid.length / 2] ^= (byte) 0x80;
            Files.write(spec.path(), corrupted);
            try {
                spec.load();
            } catch (Exception ignored) {}

            // RESTORING THE VALID FILE MUST FULLY RECOVER THE PERSISTED STATE
            Files.write(spec.path(), valid);
            assertTrue(spec.load());
            assertEquals(42, num.getAsInt());
            assertEquals("caf" + ACCENTS, txt.get());
            assertFalse(flag.getAsBoolean());
            assertEquals(List.of(7, 8, 9), nums.get());
        });
    }

    // ══════════════════════════════════════════════════════════
    //  2. UNEXPECTED VALUE TYPES — EXPECTED boolean, GOT int (AND EVERY OTHER SWAP)
    // ══════════════════════════════════════════════════════════
    @Test
    void unexpectedTypesResetToDefaults() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_types", "cfg", "", 0);
        BooleanField flag = b.defineBoolean("flag", true).end();
        IntField numFromText = b.defineInt("numFromText", 5).end();
        IntField numFromBool = b.defineInt("numFromBool", 6).end();
        IntField numFromArray = b.defineInt("numFromArray", 7).end();
        DoubleField ratio = b.defineDouble("ratio", 0.5).end();
        ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(1, 2)), Integer.class).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  flag: 42
                  numFromText: "abc"
                  numFromBool: true
                  numFromArray: [1, 2]
                  ratio: "not a number"
                  nums: 5
                }
                """, StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertTrue(flag.getAsBoolean(), "boolean receiving an int must reset, never silently become false");
        assertEquals(5, numFromText.getAsInt(), "int receiving text must reset to default");
        assertEquals(6, numFromBool.getAsInt(), "int receiving a boolean must reset to default");
        assertEquals(7, numFromArray.getAsInt(), "scalar receiving an array must keep its value");
        assertEquals(0.5, ratio.getAsDouble(), "double receiving text must reset to default");
        assertEquals(List.of(1, 2), nums.get(), "list receiving a scalar must keep its value");
    }

    // ══════════════════════════════════════════════════════════
    //  3. MATH EDGE CASES — DIVISION BY ZERO, OVERFLOW, COWS
    // ══════════════════════════════════════════════════════════
    @Test
    void mathEdgeCases() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_math", "cfg", "", 0);
        IntField divZero = b.defineInt("divZero", 10).math(true).end();
        IntField zeroOverZero = b.defineInt("zeroOverZero", 11).math(true).end();
        IntField underflow = b.defineInt("underflow", 12).math(true).end();
        LongField underflowAsLong = b.defineLong("underflowAsLong", 12).math(true).end();
        IntField cowSum = b.defineInt("cowSum", 13).math(true).end();
        IntField powBomb = b.defineInt("powBomb", 14).math(true).end();
        DoubleField infinity = b.defineDouble("infinity", 1.5).math(true).end();
        DoubleField nan = b.defineDouble("nan", 2.5).math(true).end();
        DoubleField sqrtNegative = b.defineDouble("sqrtNegative", 3.5).math(true).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  divZero: 10 / 0
                  zeroOverZero: 0 / 0
                  underflow: -2147483648 - 1
                  underflowAsLong: -2147483648 - 1
                  cowSum: 3 + %s
                  powBomb: 2 ^ 99999
                  infinity: 10 / 0
                  nan: 0 / 0
                  sqrtNegative: ~-4
                }
                """.formatted(COW), StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertEquals(10, divZero.getAsInt(), "int division by zero (Infinity) must reset");
        assertEquals(11, zeroOverZero.getAsInt(), "int 0/0 (NaN) must reset");
        assertEquals(12, underflow.getAsInt(), "Integer.MIN_VALUE - 1 overflows int and must reset");
        assertEquals(-2147483649L, underflowAsLong.getAsLong(), "the same expression fits a long field");
        assertEquals(13, cowSum.getAsInt(), "3 + cow emoji is not math and must reset");
        assertEquals(14, powBomb.getAsInt(), "2^99999 (Infinity) must reset");
        assertEquals(1.5, infinity.getAsDouble(), "Infinity exceeds the finite double range and must reset");
        assertTrue(Double.isNaN(nan.getAsDouble()), "NaN passes range checks and is stored as-is");
        assertTrue(Double.isNaN(sqrtNegative.getAsDouble()), "sqrt of a negative is NaN");
    }

    @Test
    void strictMathThrowsOnCowArithmetic() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_math_strict", "cfg", "", 0);
        b.defineInt("v", 1).math(true).strictMath(true).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), "{\n  v: 3 + " + COW + "\n}\n", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, spec::load, "strict math must hard-fail on cows");
    }

    // ══════════════════════════════════════════════════════════
    //  4. EMOJI AND UNICODE PAYLOADS — EVERY FORMAT, EVERY SCRIPT
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void emojiAndUnicodeValuesRoundTrip(String format) throws IOException {
        String[] samples = {
                COW,
                FAMILY,                        // ZWJ SEQUENCE
                FLAG,                          // REGIONAL INDICATORS
                THUMB_TONE,                    // SKIN TONE MODIFIER
                RTL,                           // RIGHT-TO-LEFT SCRIPTS
                CJK,
                ACCENTS,
                COMBINING,                     // COMBINING DIACRITICS
                SYMBOLS,
                "3 + " + COW + " = " + MILK,   // MATH-LOOKING STRING STAYS A STRING
        };

        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_uni_" + format, format, "", 0);
        StringField[] fields = new StringField[samples.length];
        for (int i = 0; i < samples.length; i++) {
            fields[i] = b.defineString("v" + i, "seed").end();
        }
        ListField<String> mixed = b.defineList("mixed", new ArrayList<>(List.of("seed")), String.class).end();
        ConfigSpec spec = b.build();

        for (int i = 0; i < samples.length; i++) {
            fields[i].set(samples[i]);
        }
        mixed.set(List.of(COW, FAMILY, RTL, SYMBOLS));

        spec.save();
        assertTrue(spec.load());

        for (int i = 0; i < samples.length; i++) {
            assertEquals(samples[i], fields[i].get(), "sample " + i + " corrupted by the " + format + " round-trip");
        }
        assertEquals(List.of(COW, FAMILY, RTL, SYMBOLS), mixed.get());
    }

    // ══════════════════════════════════════════════════════════
    //  5. HOSTILE KEYS — SPACES, SEPARATORS, COMMENT MARKERS, EMOJI
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void hostileKeysRoundTrip(String format) throws IOException {
        String[] keys = {
                "my key",                       // SPACES
                " padded ",                     // LEADING/TRAILING SPACES
                "key:with=seps",                // THE FORMAT'S OWN SEPARATORS
                "key#hash!bang",                // COMMENT MARKERS
                "key,comma",                    // THE ARRAY DELIMITER
                "[brackets]{braces}",           // STRUCTURAL CHARS
                "ключ_键", // CYRILLIC + CJK
                "🔑key",              // EMOJI KEY
                "50%",
        };

        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_keys_" + format, format, "", 0);
        StringField[] fields = new StringField[keys.length];
        for (int i = 0; i < keys.length; i++) {
            fields[i] = b.defineString(keys[i], "seed" + i).end();
        }
        ConfigSpec spec = b.build();

        for (int i = 0; i < keys.length; i++) {
            fields[i].set("value" + i);
        }
        spec.save();
        assertTrue(spec.load());

        for (int i = 0; i < keys.length; i++) {
            assertEquals("value" + i, fields[i].get(), "key '" + keys[i] + "' corrupted by the " + format + " round-trip");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  6. THE CHAIR-KEYBOARD INTERFACE — FILES MANGLED BY HUMANS AND THEIR EDITORS
    // ══════════════════════════════════════════════════════════
    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void notepadBomIsTolerated(String format) throws IOException {
        // WINDOWS NOTEPAD PREPENDS A UTF-8 BOM; EVEN THE STRICT READER MUST COPE
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_bom_" + format, format, "", 0);
        IntField num = b.defineInt("num", 1).end();
        ConfigSpec spec = b.build();
        num.set(77);
        spec.save();

        byte[] content = Files.readAllBytes(spec.path());
        byte[] withBom = new byte[content.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(content, 0, withBom, 3, content.length);
        Files.write(spec.path(), withBom);

        IFormatCodec codec = FORMATS.get(format);
        try (IFormatReader reader = codec.createReader(spec.path())) {
            assertEquals("77", reader.read("num"), "the BOM must be stripped even in strict mode");
        }
    }

    @Test
    void windowsLineEndingsAreTolerated() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_crlf", "cfg", "", 0);
        IntField num = b.defineInt("num", 1).end();
        StringField txt = b.defineString("txt", "x").end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), "{\r\n  num: 33\r\n  txt: \"win\"\r\n}\r\n", StandardCharsets.UTF_8);
        assertTrue(spec.load());
        assertEquals(33, num.getAsInt());
        assertEquals("win", txt.get());
    }

    @Test
    void humanBooleanSpellings() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_bools", "cfg", "", 0);
        BooleanField upper = b.defineBoolean("upper", false).end();
        BooleanField title = b.defineBoolean("title", false).end();
        BooleanField padded = b.defineBoolean("padded", false).end();
        BooleanField yes = b.defineBoolean("yes", true).end();
        BooleanField one = b.defineBoolean("one", true).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  upper: "TRUE"
                  title: "True"
                  padded: " true "
                  yes: "yes"
                  one: 1
                }
                """, StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertTrue(upper.getAsBoolean(), "TRUE must be accepted (case-insensitive)");
        assertTrue(title.getAsBoolean(), "True must be accepted (case-insensitive)");
        assertTrue(padded.getAsBoolean(), "accidental padding around true must be tolerated");
        assertTrue(yes.getAsBoolean(), "'yes' is not a boolean: reset to default, never silently false");
        assertTrue(one.getAsBoolean(), "'1' is not a boolean: reset to default, never silently false");
    }

    @Test
    void humanNumberSpellings() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_nums", "cfg", "", 0);
        IntField padded = b.defineInt("padded", 1).end();
        IntField overflow = b.defineInt("overflow", 2).end();
        DoubleField euroComma = b.defineDouble("euroComma", 0.25).end();
        IntField negZero = b.defineInt("negZero", 3).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  padded: " 42 "
                  overflow: 2147483648
                  euroComma: "3,14"
                  negZero: -0
                }
                """, StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertEquals(42, padded.getAsInt(), "accidental padding around a number must be tolerated");
        assertEquals(2, overflow.getAsInt(), "an int overflow must reset, never wrap around");
        assertEquals(0.25, euroComma.getAsDouble(), "a European decimal comma is not parseable: reset");
        assertEquals(0, negZero.getAsInt());
    }

    @Test
    void duplicateKeysLastOneWins() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_dupes", "cfg", "", 0);
        IntField num = b.defineInt("num", 1).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  num: 10
                  num: 20
                }
                """, StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertEquals(20, num.getAsInt(), "when the user pastes a key twice, the last value wins");
    }

    @Test
    void homoglyphKeyTypoKeepsDefault() throws IOException {
        // "c(omicron)unt" LOOKS IDENTICAL TO "count" BUT IS A DIFFERENT KEY
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_homoglyph", "cfg", "", 0);
        IntField count = b.defineInt("count", 5).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), "{\n  \"cοunt\": 99\n}\n", StandardCharsets.UTF_8);
        assertTrue(spec.load());
        assertEquals(5, count.getAsInt(), "a homoglyph key must not feed the real field");
    }

    @ParameterizedTest
    @ValueSource(strings = {WaterConfig.FORMAT_PROPERTIES, WaterConfig.FORMAT_CFG, WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5, WaterConfig.FORMAT_TOML})
    void emptyAndCommentOnlyFilesKeepDefaults(String format) throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("extreme_empty_" + format, format, "", 0);
        IntField num = b.defineInt("num", 5).end();
        StringField txt = b.defineString("txt", "fallback").end();
        ConfigSpec spec = b.build();

        // TOTALLY EMPTY FILE
        Files.writeString(spec.path(), "", StandardCharsets.UTF_8);
        try {
            spec.load();
        } catch (Exception e) {
            assertTrue(e instanceof IOException || e instanceof RuntimeException);
        }
        assertEquals(5, num.getAsInt());
        assertEquals("fallback", txt.get());

        // WHITESPACE + COMMENT-ONLY FILE
        String comment = switch (format) {
            case WaterConfig.FORMAT_JSON, WaterConfig.FORMAT_JSON5 -> "\n\n  \n";
            default -> "\n# just a comment\n\n";
        };
        Files.writeString(spec.path(), comment, StandardCharsets.UTF_8);
        try {
            spec.load();
        } catch (Exception e) {
            assertTrue(e instanceof IOException || e instanceof RuntimeException);
        }
        assertEquals(5, num.getAsInt());
        assertEquals("fallback", txt.get());
    }

    // ══════════════════════════════════════════════════════════
    //  7. DEPTH BOMBS — DEEP NESTING MUST FAIL CLEANLY, NEVER BLOW THE STACK
    // ══════════════════════════════════════════════════════════
    @Test
    void reasonableNestingParses() throws IOException {
        int depth = 300;
        Path file = tempDir.resolve("deep_ok.json");
        Files.writeString(file, "{\"k\":".repeat(depth) + "1" + "}".repeat(depth), StandardCharsets.UTF_8);

        try (IFormatReader reader = new JSONFormat().createReader(file)) {
            assertEquals("1", reader.entries().get(("k." ).repeat(depth - 1) + "k"));
        }
    }

    @Test
    void jsonDepthBombFailsCleanly() throws IOException {
        int depth = 5000;
        Path file = tempDir.resolve("deep_bomb.json");
        Files.writeString(file, "{\"k\":".repeat(depth) + "1" + "}".repeat(depth), StandardCharsets.UTF_8);

        assertTimeoutPreemptively(LIMIT, () ->
                assertThrows(IOException.class, () -> new JSONFormat().createReader(file),
                        "a depth bomb must fail with a clean exception, not a StackOverflowError"));
    }

    @Test
    void cfgDepthBombFailsCleanly() throws IOException {
        int depth = 5000;
        Path file = tempDir.resolve("deep_bomb.cfg");
        Files.writeString(file, "{\n" + "k: {\n".repeat(depth) + "}\n".repeat(depth) + "}\n", StandardCharsets.UTF_8);

        assertTimeoutPreemptively(LIMIT, () ->
                assertThrows(IOException.class, () -> new CFGFormat().createReader(file, IFormatCodec.ParseMode.STRICT),
                        "a depth bomb must fail with a clean exception, not a StackOverflowError"));
    }
}
