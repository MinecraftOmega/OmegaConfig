package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Settings recovery through {@link Spec#old()}: when the current-format file is missing,
 * the same spec file in the previous format is searched, its settings recovered through
 * root-resolving aliases, validated and migrated into the current format.
 */
public class SettingsRecoveryTest {

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

    // COPIES A CLASSPATH RESOURCE INTO THE ACTIVE CONFIG DIRECTORY
    private static Path copyResource(String resource, String target) throws IOException {
        Path path = WaterConfig.getPath().resolve(target);
        try (InputStream in = SettingsRecoveryTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(in, "Missing test resource " + resource);
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return path;
    }

    // ========================================================================
    // CHLORIDE SPEC — THE NEW (GROUPED) STRUCTURE; ALIASES MAP THE OLD FLAT KEYS
    // ========================================================================

    enum WindowMode { WINDOWED, BORDERLESS, FULLSCREEN }
    enum AttachMode { ATTACH, REPLACE, OFF }
    enum FpsMode { OFF, SIMPLE, ADVANCED }
    enum HAlign { LEFT, CENTER, RIGHT }
    enum VAlign { TOP, CENTER, BOTTOM }
    enum SystemDetails { OFF, ALL, GPU_ONLY, RAM_ONLY }
    enum FogShape { SPHERE, CYLINDER }
    enum FadeSpeed { OFF, FAST, SLOW }
    enum LeavesCulling { ALL, OFF }
    enum DarknessMode { VANILLA, DIM, DARK, DARKNESS, BLACK, BLACKNESS }
    enum SettingsScreen { SODIUM, CHLORIDE, TEXTRUE, POPULI }

    @Spec(value = "chloride", suffix = "client", format = WaterConfig.FORMAT_JSON5, old = WaterConfig.FORMAT_JSON, backups = 0)
    static class ChlorideCfg {
        @Spec.Field public static boolean modpackMode = false;

        @Spec.Field public static final Fullscreen fullscreen = new Fullscreen();
        @Spec.Field public static final FpsDisplay fpsDisplay = new FpsDisplay();
        @Spec.Field public static final Fog fog = new Fog();
        @Spec.Field public static final World world = new World();
        @Spec.Field public static final Nametags nametags = new Nametags();
        @Spec.Field public static final Darkness darkness = new Darkness();
        @Spec.Field public static final Interface iface = new Interface();
        @Spec.Field public static final FastBlocks fastBlocks = new FastBlocks();
        @Spec.Field public static final Particles particles = new Particles();
        @Spec.Field public static final Culling culling = new Culling();
        @Spec.Field public static final Zoom zoom = new Zoom();

        @Spec(value = "fullscreen", disableStatic = true)
        static class Fullscreen {
            @Spec.Field(aliases = "fullScreen") public WindowMode mode = WindowMode.WINDOWED;
            @Spec.Field(aliases = "disableBorderlessOptimizations") public boolean disableBorderlessOptimizations = false;
            @Spec.Field(aliases = "borderlessAttachModeF11") public AttachMode attachModeF11 = AttachMode.ATTACH;
        }

        @Spec(value = "fpsDisplay", disableStatic = true)
        static class FpsDisplay {
            @Spec.Field(aliases = "fpsDisplayMode") public FpsMode mode = FpsMode.ADVANCED;
            @Spec.Field(aliases = "fpsDisplayAlign") public HAlign align = HAlign.LEFT;
            @Spec.Field(aliases = "fpsDisplayVAlign") public VAlign verticalAlign = VAlign.TOP;
            @Spec.Field(aliases = "fpsDisplaySystemMode") public SystemDetails systemDetails = SystemDetails.OFF;
            @Spec.Field(aliases = "fpsDisplayMargin") @NumberConditions(minInt = 0) public int margin = 12;
            @Spec.Field(aliases = "fpsDisplayVMargin") @NumberConditions(minInt = 0) public int verticalMargin = 12;
            @Spec.Field(aliases = "fpsDisplayShadow") public boolean shadow = false;
        }

        @Spec(value = "fog", disableStatic = true)
        static class Fog {
            @Spec.Field(aliases = "fog") public boolean enabled = true;
            @Spec.Field(aliases = "fogOnOverworld") public boolean onOverworld = true;
            @Spec.Field(aliases = "fogOnNether") public boolean onNether = true;
            @Spec.Field(aliases = "fogOnEnd") public boolean onEnd = true;
            @Spec.Field(aliases = "blueBand") public boolean blueBand = true;
            @Spec.Field(aliases = "customFog") public boolean custom = false;
            @Spec.Field(aliases = "fogStart") @NumberConditions(minInt = -1000, maxInt = 1000) public int start = 0;
            @Spec.Field(aliases = "fogEnd") @NumberConditions(minInt = 100, maxInt = 10000) public int end = 192;
            @Spec.Field(aliases = "fogShape") public FogShape shape = FogShape.CYLINDER;
        }

        @Spec(value = "world", disableStatic = true)
        static class World {
            @Spec.Field(aliases = "cloudsHeight") @NumberConditions(minInt = 64, maxInt = 364) public int cloudsHeight = 192;
            @Spec.Field(aliases = "chunkFadeSpeed") public FadeSpeed chunkFadeSpeed = FadeSpeed.SLOW;
            @Spec.Field(aliases = "leavesCulling") public LeavesCulling leavesCulling = LeavesCulling.OFF;
            @Spec.Field @NumberConditions(minInt = -64, maxInt = 256) public int lowerVoidHorizon = 63;
            @Spec.Field public boolean farSkybox = true;
        }

        @Spec(value = "nametags", disableStatic = true)
        static class Nametags {
            @Spec.Field(aliases = "entityNametagRendering") public boolean entities = true;
            @Spec.Field(aliases = "playerNametagRendering") public boolean players = true;
            @Spec.Field(aliases = "itemNametagRendering") public boolean items = true;
        }

        @Spec(value = "darkness", disableStatic = true)
        static class Darkness {
            @Spec.Field(aliases = "darknessMode") public DarknessMode mode = DarknessMode.VANILLA;
            @Spec.Field(aliases = "darknessOnOverworld") public boolean onOverworld = true;
            @Spec.Field(aliases = "darknessOnNether") public boolean onNether = false;
            @Spec.Field(aliases = "darknessNetherFogBright") @NumberConditions(minDouble = 0.0, maxDouble = 1.0) public double netherFogBright = 0.5;
            @Spec.Field(aliases = "darknessOnEnd") public boolean onEnd = false;
            @Spec.Field(aliases = "darknessEndFogBright") @NumberConditions(minDouble = 0.0, maxDouble = 1.0) public double endFogBright = 0.5;
            @Spec.Field(aliases = "darknessByDefault") public boolean byDefault = false;
            @Spec.Field(aliases = "darknessDimensionWhiteList") public List<String> dimensionWhitelist = new ArrayList<>();
            @Spec.Field(aliases = "darknessOnNoSkyLight") public boolean onNoSkyLight = false;
            @Spec.Field(aliases = "darknessBlockLightOnly") public boolean blockLightOnly = false;
            @Spec.Field(aliases = "darknessAffectedByMoonPhase") public boolean affectedByMoonPhase = true;
            @Spec.Field(aliases = "darknessNewMoonBright") @NumberConditions(minDouble = 0.0, maxDouble = 1.0) public double newMoonBright = 0.0;
            @Spec.Field(aliases = "darknessFullMoonBright") @NumberConditions(minDouble = 0.0, maxDouble = 1.0) public double fullMoonBright = 0.25;
        }

        @Spec(value = "interface", disableStatic = true)
        static class Interface {
            @Spec.Field(aliases = "hideJREMI") public boolean hideJREMI = false;
            @Spec.Field(aliases = "hideJREMIHint") public boolean hideJREMIHint = false;
            @Spec.Field(aliases = "fontShadows") public boolean fontShadows = true;
            @Spec.Field(aliases = "fastLanguageReload") public boolean fastLanguageReload = true;
            @Spec.Field public SettingsScreen settingsScreen = SettingsScreen.SODIUM;
        }

        @Spec(value = "fastBlocks", disableStatic = true)
        static class FastBlocks {
            @Spec.Field(aliases = "fastChests") public boolean chests = false;
            @Spec.Field(aliases = "fastBeds") public boolean beds = false;
        }

        @Spec(value = "particles", disableStatic = true)
        static class Particles {
            @Spec.Field(aliases = "rainParticles") public boolean rain = true;
            @Spec.Field(aliases = "rainDropParticles") public boolean rainDrops = true;
            @Spec.Field(aliases = "crackingBlockParticles") public boolean blockCracking = true;
            @Spec.Field(aliases = "destroyedBlockParticles") public boolean blockDestroyed = true;
            @Spec.Field(aliases = "disabledParticles") public List<String> disabled = new ArrayList<>();
        }

        @Spec(value = "culling", disableStatic = true)
        static class Culling {
            @Spec.Field(aliases = "tileEntityDistanceCulling") public boolean tileEntities = true;
            @Spec.Field(aliases = "tileEntityCullingDistanceX") @NumberConditions(minInt = 0, maxInt = 16384) public int tileEntityDistanceX = 4096;
            @Spec.Field(aliases = "tileEntityCullingDistanceY") @NumberConditions(minInt = 0, maxInt = 256) public int tileEntityDistanceY = 32;
            @Spec.Field(aliases = "entityDistanceCulling") public boolean entities = true;
            @Spec.Field(aliases = "entityLimit") @NumberConditions(minInt = 0, maxInt = 512) public int entityLimit = 512;
            @Spec.Field(aliases = "entityCullingDistanceX") @NumberConditions(minInt = 0, maxInt = 16384) public int entityDistanceX = 4096;
            @Spec.Field(aliases = "entityCullingDistanceY") @NumberConditions(minInt = 0, maxInt = 256) public int entityDistanceY = 32;
            @Spec.Field(aliases = "monsterDistanceCulling") public boolean monsters = false;
            @Spec.Field(aliases = "monsterCullingDistanceX") @NumberConditions(minInt = 0, maxInt = 16384) public int monsterDistanceX = 16384;
            @Spec.Field(aliases = "monsterCullingDistanceY") @NumberConditions(minInt = 0, maxInt = 256) public int monsterDistanceY = 64;
            @Spec.Field(aliases = "entityWhitelist") public List<String> entityWhitelist = new ArrayList<>(List.of(
                    "minecraft:ghast", "minecraft:ender_dragon", "iceandfire:all", "create:all"));
            @Spec.Field(aliases = "monsterWhitelist") public List<String> monsterWhitelist = new ArrayList<>();
            @Spec.Field(aliases = "tileEntityWhitelist") public List<String> tileEntityWhitelist = new ArrayList<>(List.of("waterframes:all"));
        }

        @Spec(value = "zoom", disableStatic = true)
        static class Zoom {
            @Spec.Field(aliases = "enableZoom") public boolean enabled = true;
            @Spec.Field(aliases = "maxZoom") @NumberConditions(minDouble = 1.0, maxDouble = 100.0) public double max = 50.0;
        }
    }

    // ========================================================================
    // FULL LIFECYCLE: OLD FLAT JSON → RECOVERY → JSON5 MIGRATION → JSON5 WINS
    // ========================================================================

    @Test
    void chlorideSettingsRecoveredFromOldJsonAndMigratedToJson5() throws IOException {
        Path oldFile = copyResource("chloride-client.json", "chloride-client.json");
        ConfigSpec spec = WaterConfig.registerBlocking(ChlorideCfg.class);

        // ─── PHASE 1: ONLY THE OLD FLAT JSON EXISTS, ITS SETTINGS MUST BE RECOVERED ───
        assertEquals(WindowMode.BORDERLESS, ChlorideCfg.fullscreen.mode);
        assertEquals(AttachMode.ATTACH, ChlorideCfg.fullscreen.attachModeF11);
        assertEquals(HAlign.CENTER, ChlorideCfg.fpsDisplay.align);
        assertEquals(VAlign.BOTTOM, ChlorideCfg.fpsDisplay.verticalAlign);
        assertEquals(SystemDetails.ALL, ChlorideCfg.fpsDisplay.systemDetails);
        assertEquals(240, ChlorideCfg.fpsDisplay.margin);
        assertEquals(180, ChlorideCfg.fpsDisplay.verticalMargin);
        assertFalse(ChlorideCfg.fog.onNether);
        assertFalse(ChlorideCfg.fog.blueBand);
        assertEquals(310, ChlorideCfg.fog.start);
        assertEquals(3650, ChlorideCfg.fog.end);
        assertEquals(FogShape.CYLINDER, ChlorideCfg.fog.shape);
        assertEquals(DarknessMode.DARKNESS, ChlorideCfg.darkness.mode);
        assertTrue(ChlorideCfg.darkness.onNoSkyLight);
        assertTrue(ChlorideCfg.iface.hideJREMI);
        assertTrue(ChlorideCfg.iface.hideJREMIHint);
        assertTrue(ChlorideCfg.fastBlocks.chests);
        assertTrue(ChlorideCfg.fastBlocks.beds);
        assertTrue(ChlorideCfg.culling.monsters);
        assertEquals(6, ChlorideCfg.culling.entityLimit);
        assertEquals(80, ChlorideCfg.culling.tileEntityDistanceY);
        assertEquals(64, ChlorideCfg.culling.entityDistanceY);
        assertEquals(4096, ChlorideCfg.culling.monsterDistanceX);
        assertEquals(List.of("minecraft:ghast", "minecraft:ender_dragon", "iceandfire:all", "create:all", "minecraft:pig"),
                ChlorideCfg.culling.entityWhitelist);
        assertEquals(50.0, ChlorideCfg.zoom.max);
        // SAME-NAME ROOT KEYS LOAD WITHOUT ANY ALIAS
        assertFalse(ChlorideCfg.modpackMode);

        // FIELDS WITHOUT AN OLD COUNTERPART KEEP THEIR DEFAULTS
        assertEquals(63, ChlorideCfg.world.lowerVoidHorizon);
        assertTrue(ChlorideCfg.world.farSkybox);
        assertEquals(SettingsScreen.SODIUM, ChlorideCfg.iface.settingsScreen);

        // MIGRATION PERSISTED IN THE CURRENT FORMAT, OLD FILE CLEANED UP BY THE SPEC
        assertEquals("chloride-client.json5", spec.path().getFileName().toString());
        assertTrue(Files.exists(spec.path()), "Migrated json5 file should be written");
        assertFalse(spec.isDirty(), "Migration must end persisted, not pending");
        String migrated = Files.readString(spec.path(), StandardCharsets.UTF_8);
        assertTrue(migrated.contains("BORDERLESS"), "Migrated file should carry the recovered settings");
        assertTrue(migrated.contains("minecraft:pig"), "Migrated file should carry the recovered lists");
        assertFalse(Files.exists(oldFile), "Old file must be deleted once the migration is persisted");

        // ─── PHASE 2: BOTH FILES EXIST → THE CURRENT FORMAT WINS AND THE OLD IS ONLY IGNORED ───
        oldFile = copyResource("chloride-client.json", "chloride-client.json");
        copyResource("chloride-client.json5", "chloride-client.json5");
        spec.load();

        assertEquals(WindowMode.WINDOWED, ChlorideCfg.fullscreen.mode);
        assertEquals(HAlign.LEFT, ChlorideCfg.fpsDisplay.align);
        assertEquals(VAlign.TOP, ChlorideCfg.fpsDisplay.verticalAlign);
        assertEquals(12, ChlorideCfg.fpsDisplay.margin);
        assertTrue(ChlorideCfg.fog.onNether);
        assertEquals(0, ChlorideCfg.fog.start);
        assertEquals(192, ChlorideCfg.fog.end);
        assertEquals(DarknessMode.VANILLA, ChlorideCfg.darkness.mode);
        assertFalse(ChlorideCfg.iface.hideJREMI);
        assertEquals(512, ChlorideCfg.culling.entityLimit);
        assertEquals(List.of("minecraft:ghast", "minecraft:ender_dragon", "iceandfire:all", "create:all"),
                ChlorideCfg.culling.entityWhitelist);
        assertTrue(Files.exists(oldFile), "With a current-format file present the old file is ignored, not deleted");
    }

    // ========================================================================
    // EMPTY old MEANS NOTHING OLD TO RECOVER
    // ========================================================================

    @Spec(value = "recovery_noold", suffix = "client", format = WaterConfig.FORMAT_JSON5, backups = 0)
    static class NoOldCfg {
        @Spec.Field public static int count = 1;
    }

    @Test
    void emptyOldIgnoresPreviousFormatFiles() throws IOException {
        // A MATCHING OLD-STYLE JSON EXISTS, BUT WITH old() EMPTY IT MUST NEVER BE SEARCHED
        Files.writeString(tempDir.resolve("recovery_noold-client.json"), "{\"count\": 99}", StandardCharsets.UTF_8);
        ConfigSpec spec = WaterConfig.registerBlocking(NoOldCfg.class);

        assertEquals(1, NoOldCfg.count, "Old-format file must be ignored when old() is empty");
        assertTrue(Files.exists(spec.path()), "Defaults should be saved in the current format");
    }

    // ========================================================================
    // BUILDER API: RECOVERED VALUES GO THROUGH THE SAME VALIDATION AS ANY LOAD
    // ========================================================================

    @Test
    void builderOldRecoversAndValidatesValues() throws IOException {
        // OLD JSON CARRIES ONE VALID AND ONE OUT-OF-RANGE VALUE
        Path oldFile = Files.writeString(tempDir.resolve("recovery_builder.json"),
                "{\"count\": 999, \"label\": \"from_old\"}", StandardCharsets.UTF_8);

        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder("recovery_builder", "cfg", "", 0)
                .old(WaterConfig.FORMAT_JSON);
        builder.defineInt("count", 7).setMin(0).setMax(100).end();
        builder.defineString("label", "fresh").end();
        ConfigSpec spec = builder.build();

        assertFalse(spec.load(), "Recovery must report the current-format file as missing");
        assertTrue(spec.isDirty(), "Recovered settings must stay pending until persisted");
        assertEquals("from_old", spec.findField("label").get());
        assertEquals(7, spec.findField("count").get(), "Out-of-range recovered value must reset to default");
        assertNotEquals(ConfigSpec.Status.LOADED, spec.status(), "Recovery is not terminal until persisted");
        assertTrue(Files.exists(oldFile), "Old file must survive until the migration is persisted");

        spec.save();
        assertTrue(Files.exists(spec.path()), "Migrated file should be written");
        assertFalse(Files.exists(oldFile), "Old file must be deleted once the migration is persisted");
    }

    @Test
    void oldFileAbsentFallsBackToDefaults() throws IOException {
        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder("recovery_absent", "cfg", "", 0)
                .old(WaterConfig.FORMAT_JSON);
        builder.defineInt("count", 3).end();
        ConfigSpec spec = builder.build();

        assertFalse(spec.load(), "No file in any format: nothing to load");
        assertEquals(3, spec.findField("count").get());
        assertEquals(ConfigSpec.Status.UNLOADED, spec.status());
    }

    // ========================================================================
    // BUILDER GUARDS
    // ========================================================================

    @Test
    void unknownOrSelfOldFormatThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigSpec.SpecBuilder("recovery_bad", "cfg", "", 0).old("yaml"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigSpec.SpecBuilder("recovery_self", "cfg", "", 0).old(WaterConfig.FORMAT_CFG));
    }
}
