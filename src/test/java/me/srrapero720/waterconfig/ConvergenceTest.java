package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.fields.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavior convergence: the same operations on the same spec must produce identical field
 * state across every format. Covers the full scalar/list type matrix plus rich strings,
 * embedded newlines, empty strings and empty lists.
 */
public class ConvergenceTest {

    private static final String[] FORMATS = {"properties", "cfg", "json", "json5", "toml"};

    // UNICODE BUILT FROM CODE POINTS SO THIS SOURCE STAYS PURE ASCII ("café 你好")
    private static final String UNICODE = "caf" + (char) 0xE9 + " " + (char) 0x4F60 + (char) 0x597D;

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

    // EVERY SCALAR AND LIST TYPE MUST SURVIVE A SAVE/LOAD IDENTICALLY ON ALL FIVE FORMATS
    @Test
    void fullValueMatrixConvergesAcrossFormats() throws IOException {
        for (String fmt : FORMATS) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("conv_full_" + fmt, fmt, "", 0);
            BooleanField bo = b.defineBoolean("bo", true).end();
            ByteField by = b.defineByte("by", (byte) 0).setMin(Byte.MIN_VALUE).setMax(Byte.MAX_VALUE).end();
            ShortField sh = b.defineShort("sh", (short) 0).setMin(Short.MIN_VALUE).setMax(Short.MAX_VALUE).end();
            IntField in = b.defineInt("in", 0).setMin(Integer.MIN_VALUE).setMax(Integer.MAX_VALUE).end();
            LongField lo = b.defineLong("lo", 0L).setMin(Long.MIN_VALUE).setMax(Long.MAX_VALUE).end();
            FloatField fl = b.defineFloat("fl", 0F).end();
            DoubleField db = b.defineDouble("db", 0.0).end();
            CharField ch = b.defineChar("ch", 'A').end();
            StringField st = b.defineString("st", "x").end();
            EnumField<Color> en = b.defineEnum("en", Color.RED).end();
            ListField<Integer> li = b.defineList("li", new ArrayList<>(List.of(0)), Integer.class).end();
            ListField<String> ls = b.defineList("ls", new ArrayList<>(List.of("x")), String.class).end();
            ConfigSpec spec = b.build();

            bo.set(false);
            by.set(Byte.MIN_VALUE);
            sh.set((short) -12345);
            in.set(-2000000000);
            lo.set(Long.MIN_VALUE);
            fl.set(-0.5F);
            db.set(-3.14159265);
            ch.set('Z');
            st.set("rich " + UNICODE);
            en.set(Color.BLUE);
            li.setArray(new Integer[]{-1, 0, 7});
            ls.setArray(new String[]{"a", UNICODE, "c"});
            spec.save();

            // WIPE TO SENTINELS SO A PASSING ASSERTION PROVES load() ACTUALLY WROTE EACH FIELD
            bo.set(true);
            in.set(0);
            st.set("wiped");
            en.set(Color.RED);
            li.setArray(new Integer[0]);
            ls.setArray(new String[0]);

            assertTrue(spec.load(), fmt);
            assertFalse(bo.getAsBoolean(), fmt);
            assertEquals(Byte.MIN_VALUE, by.getAsByte(), fmt);
            assertEquals((short) -12345, sh.getAsShort(), fmt);
            assertEquals(-2000000000, in.getAsInt(), fmt);
            assertEquals(Long.MIN_VALUE, lo.getAsLong(), fmt);
            assertEquals(-0.5F, fl.getAsFloat(), fmt);
            assertEquals(-3.14159265, db.getAsDouble(), fmt);
            assertEquals('Z', ch.getAsChar(), fmt);
            assertEquals("rich " + UNICODE, st.get(), fmt);
            assertEquals(Color.BLUE, en.get(), fmt);
            assertEquals(List.of(-1, 0, 7), li.get(), fmt);
            assertEquals(List.of("a", UNICODE, "c"), ls.get(), fmt);
        }
    }

    // QUOTES, BACKSLASHES, A TAB AND UNICODE MUST ROUND-TRIP THE SAME EVERYWHERE
    @Test
    void richStringConvergesAcrossFormats() throws IOException {
        String rich = "say \"hi\" \\back\\ \t" + UNICODE;
        for (String fmt : FORMATS) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("conv_rich_" + fmt, fmt, "", 0);
            StringField st = b.defineString("st", "x").end();
            ConfigSpec spec = b.build();

            st.set(rich);
            spec.save();
            st.set("wiped");
            assertTrue(spec.load(), fmt);
            assertEquals(rich, st.get(), fmt + " must round-trip a rich string");
        }
    }

    // AN EMBEDDED NEWLINE MUST NOT SPLIT THE ENTRY ON ANY FORMAT (THE OLD PROPERTIES/JSON BUG)
    @Test
    void embeddedNewlineConvergesAcrossFormats() throws IOException {
        String multi = "line1\nline2\ttabbed";
        for (String fmt : FORMATS) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("conv_nl_" + fmt, fmt, "", 0);
            StringField st = b.defineString("st", "x").end();
            ConfigSpec spec = b.build();

            st.set(multi);
            spec.save();
            st.set("wiped");
            assertTrue(spec.load(), fmt);
            assertEquals(multi, st.get(), fmt + " must round-trip an embedded newline");
        }
    }

    @Test
    void emptyStringConvergesAcrossFormats() throws IOException {
        for (String fmt : FORMATS) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("conv_empty_" + fmt, fmt, "", 0);
            StringField st = b.defineString("st", "default").allowEmpty(true).end();
            ConfigSpec spec = b.build();

            st.set("");
            spec.save();
            st.set("wiped");
            assertTrue(spec.load(), fmt);
            assertEquals("", st.get(), fmt + " must round-trip an empty string");
        }
    }

    // AN EMPTY LIST MUST COME BACK EMPTY, NOT AS A ONE-ELEMENT [""] (THE OLD JSON/JSON5 BUG)
    @Test
    void emptyListConvergesAcrossFormats() throws IOException {
        for (String fmt : FORMATS) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("conv_emptylist_" + fmt, fmt, "", 0);
            ListField<String> ls = b.defineList("ls", new ArrayList<>(List.of("seed")), String.class).allowEmpty(true).end();
            ConfigSpec spec = b.build();

            ls.setArray(new String[0]);
            spec.save();
            ls.setArray(new String[]{"dirty"});
            assertTrue(spec.load(), fmt);
            assertTrue(ls.get().isEmpty(), fmt + " must round-trip an empty list, not inject an element: " + ls.get());
        }
    }
}
