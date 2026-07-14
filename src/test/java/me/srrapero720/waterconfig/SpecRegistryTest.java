package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.impl.fields.IntField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spec registry surface: {@link ConfigSpec#refresh()}, the WaterConfig spec-exposure lookups
 * (with default refresh), reverseSpec collision with an already-managed file, and intra-process
 * file exclusivity.
 */
public class SpecRegistryTest {

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

    @Spec(value = "reg_refresh", format = WaterConfig.FORMAT_CFG, backups = 0)
    static class RefreshCfg {
        @Spec.Field @NumberConditions(minInt = 0, maxInt = 100) public static int count = 10;
    }

    // ========================================================================
    // REFRESH (RE-VALIDATE FROM THE BASE)
    // ========================================================================
    @Nested
    class Refresh {

        @Test
        void refreshResetsOutOfRangeExternalMutation() {
            RefreshCfg.count = 10;
            ConfigSpec spec = WaterConfig.registerBlocking(RefreshCfg.class);

            // EXTERNAL (REFLECT-MODE) MUTATION THE SPEC IS UNAWARE OF
            RefreshCfg.count = 999;
            spec.refresh();
            assertEquals(10, RefreshCfg.count, "refresh resets an out-of-range external value to default");
        }

        @Test
        void refreshLeavesValidExternalMutation() {
            RefreshCfg.count = 10;
            ConfigSpec spec = WaterConfig.registerBlocking(RefreshCfg.class);

            RefreshCfg.count = 50;
            spec.refresh();
            assertEquals(50, RefreshCfg.count, "a valid external value survives refresh");
        }

        // THE POINT OF refresh(): getAsX() SERVES THE PRIMITIVE CACHE (FAST), REFRESHED ON DEMAND
        @Test
        void getAsIntServesCacheUntilRefreshResyncs() {
            RefreshCfg.count = 10;
            ConfigSpec spec = WaterConfig.registerBlocking(RefreshCfg.class);
            IntField f = (IntField) spec.findField("count");

            RefreshCfg.count = 42; // EXTERNAL MUTATION, BYPASSES THE FIELD API
            assertEquals(42, f.get(), "get() always reflects the live field");
            assertEquals(10, f.getAsInt(), "getAsInt() serves the cache until refreshed");

            spec.refresh();
            assertEquals(42, f.getAsInt(), "refresh() re-syncs the cache from the field");
        }
    }

    // ========================================================================
    // WATERCONFIG SPEC EXPOSURE
    // ========================================================================
    @Nested
    class Exposure {

        @Test
        void specByNameRefreshesByDefault() {
            RefreshCfg.count = 10;
            ConfigSpec spec = WaterConfig.registerBlocking(RefreshCfg.class);

            RefreshCfg.count = 999;
            ConfigSpec fetched = WaterConfig.spec("reg_refresh");
            assertSame(spec, fetched);
            assertEquals(10, RefreshCfg.count, "spec(name) refreshes on fetch");
        }

        @Test
        void specByNameCanSkipRefresh() {
            RefreshCfg.count = 10;
            WaterConfig.registerBlocking(RefreshCfg.class);

            RefreshCfg.count = 999;
            WaterConfig.spec("reg_refresh", false);
            assertEquals(999, RefreshCfg.count, "spec(name, false) does not refresh");
        }

        @Test
        void missingNameIsNull() {
            assertNull(WaterConfig.spec("does_not_exist"));
        }

        @Test
        void specsContainsRegisteredAndSpecOfFindsByPath() {
            ConfigSpec spec = WaterConfig.registerBlocking(RefreshCfg.class);
            assertTrue(WaterConfig.specs().contains(spec));
            assertSame(spec, WaterConfig.specOf(spec.path()));
            assertNull(WaterConfig.specOf(tempDir.resolve("unrelated.cfg")));
        }
    }

    // ========================================================================
    // reverseSpec COLLISION WITH AN ALREADY-MANAGED FILE
    // ========================================================================
    @Nested
    class ReverseSpecCollision {

        @Test
        void reverseSpecReturnsTheManagedSpec() throws IOException {
            ConfigSpec managed = WaterConfig.registerBlocking(RefreshCfg.class);
            managed.save();

            // THE FILE IS ALREADY MANAGED: reverseSpec HANDS BACK THE REAL SPEC, NOT A DUPLICATE
            assertSame(managed, WaterConfig.reverseSpec(managed.path()));
        }
    }

    // ========================================================================
    // INTRA-PROCESS FILE EXCLUSIVITY
    // ========================================================================
    @Nested
    class IntraProcessExclusivity {

        @Test
        void twoDifferentSpecsOnTheSameFileAreRefused() {
            // "excl_a" + suffix "x" AND "excl_a-x" + no suffix BOTH RESOLVE TO excl_a-x.cfg
            ConfigSpec first = new ConfigSpec.SpecBuilder("excl_a", "cfg", "x", 0).build();
            ConfigSpec second = new ConfigSpec.SpecBuilder("excl_a-x", "cfg", "", 0).build();
            assertEquals(first.path(), second.path(), "the two names must collide on one file");

            WaterConfig.register(first);
            ConfigSpec result = WaterConfig.register(second);

            assertEquals(ConfigSpec.Status.FAILED, result.status(), "a second spec on the same file is refused");
            assertNotNull(result.loadError());
            assertTrue(result.loadError().getMessage().contains("already managed"));
        }

        @Test
        void reRegisteringTheSameNameIsAllowed() {
            ConfigSpec a = new ConfigSpec.SpecBuilder("excl_same", "cfg", "", 0).build();
            ConfigSpec b = new ConfigSpec.SpecBuilder("excl_same", "cfg", "", 0).build();

            WaterConfig.register(a);
            ConfigSpec result = WaterConfig.register(b);
            assertNotEquals(ConfigSpec.Status.FAILED, result.status(), "re-registering the same name overwrites, not refused");
        }
    }

    // ========================================================================
    // SOFT LOCKDOWN (RELOAD DISABLED)
    // ========================================================================
    @Nested
    class SoftLockdown {

        @AfterEach
        void resetLockdown() {
            // LOCKDOWN IS GLOBAL: RESTORE OFF SO IT NEVER LEAKS TO OTHER TESTS
            WaterConfig.applyLockdown(WaterConfigConfig.Lockdown.OFF);
        }

        @Test
        void softLockdownIgnoresReloadRequests() {
            ConfigSpec spec = new ConfigSpec.SpecBuilder("lock_soft", "cfg", "", 0).build();

            WaterConfig.applyLockdown(WaterConfigConfig.Lockdown.I_READ_THE_COMMENT_SOFT);
            assertTrue(WaterConfig.isLockdown());
            spec.setReload(true);
            assertFalse(spec.isReload(), "soft lockdown disables runtime reload, so hot-edits never take effect");
        }

        @Test
        void offHonorsReloadRequests() {
            ConfigSpec spec = new ConfigSpec.SpecBuilder("lock_off", "cfg", "", 0).build();

            WaterConfig.applyLockdown(WaterConfigConfig.Lockdown.OFF);
            assertFalse(WaterConfig.isLockdown());
            spec.setReload(true);
            assertTrue(spec.isReload(), "without lockdown, reload requests are honored");
        }
    }
}
