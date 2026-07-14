package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.impl.codecs.RecordCodec;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.ListField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the API-polish features: backup rotation, record codec, math truncation,
 * build-time rejections and the stringify collection sugar.
 */
public class ApiPolishTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() {
        WaterConfig.init();
        WaterConfig.setPath(tempDir);
    }
    @AfterAll
    static void teardown() {
        // DROP EVERY REGISTERED SPEC SO THE WORKER NEVER WRITES INTO THE DELETED TEMP DIR
        WaterConfig.unloadAll();
    }

    public record Point(int x, int y) {
        public Point {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("Coordinates must be non-negative");
            }
        }
    }

    // ========================================================================
    // BACKUP ROTATION
    // ========================================================================

    @Test
    void backupsRotateOnChangedSave() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("polish_backups", "cfg", "", 2);
        IntField v = b.defineInt("v", 1).end();
        ConfigSpec spec = b.build();
        spec.save();

        Path backup1 = spec.path().resolveSibling(spec.path().getFileName() + "_backup1");
        Path backup2 = spec.path().resolveSibling(spec.path().getFileName() + "_backup2");

        // AN UNCHANGED RE-SAVE MUST NOT CREATE BACKUPS
        spec.save();
        assertFalse(Files.exists(backup1), "identical content must not rotate a backup");

        v.set(2);
        spec.save();
        assertTrue(Files.exists(backup1), "changed content must rotate a backup");
        String firstBackup = Files.readString(backup1, StandardCharsets.UTF_8);
        assertTrue(firstBackup.contains("v: 1"), "backup1 must hold the previous content");

        v.set(3);
        spec.save();
        assertTrue(Files.exists(backup2), "older backups must shift to higher indexes");
        assertEquals(firstBackup, Files.readString(backup2, StandardCharsets.UTF_8));
        assertTrue(Files.readString(backup1, StandardCharsets.UTF_8).contains("v: 2"));

        // THE COUNT IS HONORED: WITH backups=2 THERE IS NEVER A _backup3
        v.set(4);
        spec.save();
        assertFalse(Files.exists(spec.path().resolveSibling(spec.path().getFileName() + "_backup3")));
    }

    // ========================================================================
    // RECORD CODEC
    // ========================================================================

    @Test
    void recordCodecRoundTrip() {
        RecordCodec codec = new RecordCodec();
        Point point = new Point(3, 7);

        String encoded = codec.encode(point, Point.class);
        assertEquals(point, codec.decode(encoded, (Class) Point.class));
    }

    @Test
    void recordCodecValidatesThroughCanonicalConstructor() {
        RecordCodec codec = new RecordCodec();
        assertNull(codec.decode("-1; 5", (Class) Point.class), "constructor checks must reject invalid values");
        assertNull(codec.decode("1; 2; 3", (Class) Point.class), "component count mismatches must decode to null");
        assertNull(codec.decode("garbage", (Class) Point.class));
    }

    @Test
    void recordListFieldRoundTrip() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("polish_records", "cfg", "", 0);
        ListField<Point> points = b.defineList("points", new ArrayList<>(List.of(new Point(1, 2))), Point.class).end();
        ConfigSpec spec = b.build();

        points.setArray(new Point[]{new Point(3, 4), new Point(5, 6)});
        spec.save();
        points.setArray(new Point[]{new Point(0, 0)});

        assertTrue(spec.load());
        assertEquals(List.of(new Point(3, 4), new Point(5, 6)), points.get());
    }

    // ========================================================================
    // MATH TRUNCATION
    // ========================================================================

    @Spec(value = "polish_truncate", format = WaterConfig.FORMAT_CFG, backups = 0)
    static class TruncateCfg {
        @Spec.Field @NumberConditions(math = true) public static int half = 10;
    }

    @Test
    void mathTruncatesFractionalResultsOnIntegralFields() throws IOException {
        ConfigSpec spec = WaterConfig.registerBlocking(TruncateCfg.class);

        spec.setDirty(false);
        Files.writeString(spec.path(), """
                {
                  half: 5 / 2
                }
                """, StandardCharsets.UTF_8);
        assertTrue(spec.load());
        assertEquals(2, TruncateCfg.half, "fractional math results must truncate on int fields, not reset");
    }

    // ========================================================================
    // BUILD-TIME REJECTIONS
    // ========================================================================

    @Test
    void duplicateFieldNamesRejected() {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("polish_dup", "cfg", "", 0);
        b.defineInt("x", 1).end();
        assertThrows(IllegalArgumentException.class, () -> b.defineInt("x", 2).end());
    }

    @Test
    void nullDefaultsRejectedWithClearError() {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("polish_nulldef", "cfg", "", 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> b.defineString("s", null).end());
        assertTrue(e.getMessage().contains("non-null default"), "the error must state the null-default cause");
    }

    @Spec(value = "polish_prim_array", format = WaterConfig.FORMAT_CFG, backups = 0)
    static class PrimitiveArrayCfg {
        @Spec.Field public static int[] nums = {1, 2};
    }

    @Test
    void primitiveArrayComponentsRejectedWithMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> WaterConfig.register(PrimitiveArrayCfg.class));
        assertTrue(e.getMessage().contains("boxed"), "the error must point to the boxed alternative");
    }

    // ========================================================================
    // STRINGIFY SUGAR
    // ========================================================================

    @Test
    void stringifyCollectionsRoundTrip() throws IOException {
        ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("polish_stringify", "cfg", "", 0);
        ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(1, 2)), Integer.class).stringify(true).end();
        ConfigSpec spec = b.build();

        nums.setArray(new Integer[]{7, 8, 9});
        spec.save();

        // STRINGIFIED COLLECTIONS ARE WRITTEN AS ONE PLAIN STRING, NOT A NATIVE ARRAY
        String content = Files.readString(spec.path(), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"7, 8, 9\""), "stringify must serialize as a single string: " + content);

        nums.setArray(new Integer[]{0});
        assertTrue(spec.load());
        assertEquals(List.of(7, 8, 9), nums.get());
    }
}
