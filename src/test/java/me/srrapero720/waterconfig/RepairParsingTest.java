package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.ListField;
import me.srrapero720.waterconfig.impl.fields.StringField;
import me.srrapero720.waterconfig.impl.formats.CFGFormat;
import me.srrapero720.waterconfig.impl.formats.JSON5Format;
import me.srrapero720.waterconfig.impl.formats.JSONFormat;
import me.srrapero720.waterconfig.impl.formats.TOMLFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers repair (error-tolerant) parsing: resync points per format, repair reports,
 * spec-level recovery, field aliases and unknown-key detection.
 */
public class RepairParsingTest {

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

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    // ========================================================================
    // PER-FORMAT RESYNC
    // ========================================================================

    @Test
    void repairJsonResyncsBrokenEntries() throws IOException {
        Path file = write("repair.json", """
                {
                    "good": 1,
                    "broken" %%%,
                    "alsoGood": "two"
                }
                """);

        // STRICT MODE KEEPS THE CLEAN EXCEPTION
        assertThrows(Exception.class, () -> new JSONFormat().createReader(file));

        IFormatReader reader = new JSONFormat().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("1", reader.read("good"));
        assertEquals("two", reader.read("alsoGood"));
        assertFalse(reader.report().isEmpty(), "the recovery must be recorded in the report");
        assertTrue(reader.report().get(0).line() > 0, "report entries carry the line of the problem");
        reader.close();
    }

    @Test
    void repairJson5ResyncsBrokenArrayElement() throws IOException {
        Path file = write("repair.json5", """
                {
                    tags: ["a", {bad: 1}, "b"],
                    count: 42,
                }
                """);

        IFormatReader reader = new JSON5Format().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("42", reader.read("count"));
        assertArrayEquals(new String[]{"a", "b"}, reader.readArray("tags"), "the array must survive a broken element");
        assertFalse(reader.report().isEmpty());
        reader.close();
    }

    @Test
    void repairJsonTruncatedFileKeepsValidPrefix() throws IOException {
        Path file = write("repair_trunc.json", "{\"kept\": 7, \"cut\": \"oops");

        IFormatReader reader = new JSONFormat().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("7", reader.read("kept"));
        assertFalse(reader.report().isEmpty());
        reader.close();
    }

    @Test
    void repairTomlSkipsBrokenLines() throws IOException {
        Path file = write("repair.toml", """
                good = 1
                broken line without equals !!!
                after = "kept"
                """);

        assertThrows(Exception.class, () -> new TOMLFormat().createReader(file));

        IFormatReader reader = new TOMLFormat().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("1", reader.read("good"));
        assertEquals("kept", reader.read("after"));
        assertFalse(reader.report().isEmpty());
        reader.close();
    }

    @Test
    void repairCfgSkipsBrokenEntries() throws IOException {
        Path file = write("repair.cfg", """
                {
                  good: 1
                  %%% broken entry
                  after: "kept"
                }
                """);

        assertThrows(Exception.class, () -> new CFGFormat().createReader(file));

        IFormatReader reader = new CFGFormat().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("1", reader.read("good"));
        assertEquals("kept", reader.read("after"));
        assertFalse(reader.report().isEmpty());
        reader.close();
    }

    @Test
    void repairCfgUnclosedRootRecovers() throws IOException {
        Path file = write("repair_unclosed.cfg", "{\n  count: 42\n");

        IFormatReader reader = new CFGFormat().createReader(file, IFormatCodec.ParseMode.REPAIR);
        assertEquals("42", reader.read("count"));
        assertFalse(reader.report().isEmpty());
        reader.close();
    }

    // ========================================================================
    // SPEC-LEVEL RECOVERY
    // ========================================================================

    @Test
    void specLoadRecoversTruncatedFile() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("repair_spec", "json", "", 0);
        IntField num = b.defineInt("num", 5).end();
        StringField txt = b.defineString("txt", "fallback").end();
        ConfigSpec spec = b.build();

        num.set(42);
        txt.set("persisted");
        spec.save();

        // TRUNCATE AFTER THE FIRST ENTRY: num SURVIVES, txt FALLS BACK TO ITS CURRENT VALUE
        String valid = Files.readString(spec.path(), StandardCharsets.UTF_8);
        Files.writeString(spec.path(), valid.substring(0, valid.indexOf("\"txt\"")), StandardCharsets.UTF_8);

        num.set(0);
        assertTrue(spec.load(), "a recoverable file must still load");
        assertEquals(42, num.getAsInt(), "the valid prefix must be recovered");
    }

    // ========================================================================
    // ALIASES
    // ========================================================================

    @Test
    void builderAliasesPickUpOldNames() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("repair_alias", "cfg", "", 0);
        IntField renamed = b.defineInt("newName", 1).aliases("oldName", "olderName").end();
        ListField<Integer> renamedList = b.defineList("newList", new ArrayList<>(List.of(0)), Integer.class).aliases("oldList").end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  oldName: 77
                  oldList: [4, 5]
                }
                """, StandardCharsets.UTF_8);

        assertTrue(spec.load());
        assertEquals(77, renamed.getAsInt(), "scalar aliases must pick up values from old files");
        assertEquals(List.of(4, 5), renamedList.get(), "collection aliases must pick up values from old files");
        assertArrayEquals(new String[]{"oldName", "olderName"}, renamed.aliases());
    }

    @Spec(value = "repair_alias_annot", format = WaterConfig.FORMAT_CFG, backups = 0)
    static class AliasCfg {
        @Spec.Field(aliases = {"legacyCount"}) public static int count = 1;
    }

    @Test
    void annotationAliasesPickUpOldNames() throws IOException {
        ConfigSpec spec = WaterConfig.registerBlocking(AliasCfg.class);

        spec.setDirty(false);
        Files.writeString(spec.path(), """
                {
                  legacyCount: 33
                }
                """, StandardCharsets.UTF_8);
        assertTrue(spec.load());
        assertEquals(33, AliasCfg.count);
    }

    // ========================================================================
    // UNKNOWN KEYS
    // ========================================================================

    @Test
    void unknownFileKeysAreReported() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("repair_unknown", "cfg", "", 0);
        b.defineInt("known", 1).end();
        ConfigSpec spec = b.build();

        Files.writeString(spec.path(), """
                {
                  known: 2
                  ghost: 3
                }
                """, StandardCharsets.UTF_8);

        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            assertTrue(spec.load());
        } finally {
            System.setErr(original);
        }

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("unknown key 'ghost'"),
                "unknown file keys must be reported through the load logging");
    }

    // ========================================================================
    // ENTRIES EXPOSURE
    // ========================================================================

    @Test
    void readersExposeParsedEntries() throws IOException {
        Path file = write("entries.json", "{\"a\": 1, \"g\": {\"b\": \"x\"}, \"list\": [\"i\", \"j\"]}");

        IFormatReader reader = new JSONFormat().createReader(file);
        assertEquals("1", reader.entries().get("a"));
        assertEquals("x", reader.entries().get("g.b"));
        assertArrayEquals(new String[]{"i", "j"}, (String[]) reader.entries().get("list"));
        assertThrows(UnsupportedOperationException.class, () -> reader.entries().put("c", "2"), "entries must be read-only");
        reader.close();
    }
}
