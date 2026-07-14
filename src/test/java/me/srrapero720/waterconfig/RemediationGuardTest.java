package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.impl.codecs.FileCodec;
import me.srrapero720.waterconfig.impl.codecs.PathCodec;
import me.srrapero720.waterconfig.impl.codecs.UUIDCodec;
import me.srrapero720.waterconfig.impl.fields.BaseConfigField;
import me.srrapero720.waterconfig.impl.fields.EnumField;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.ListField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guards for defects whose fixes were previously exercised only where they made no
 * difference: enums with constant-specific bodies, the collection filter actually dropping
 * entries, and the UUID/Path/File codecs recovering to null instead of throwing.
 */
public class RemediationGuardTest {

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

    // AN ENUM WHOSE CONSTANTS HAVE BODIES: EACH CONSTANT IS AN ANONYMOUS SUBCLASS AT RUNTIME
    public enum Op {
        ADD { public int apply(int a) { return a + 1; } },
        SUB { public int apply(int a) { return a - 1; } };
        public abstract int apply(int a);
    }

    public static final class EvenOnly implements Predicate<Integer> {
        @Override
        public boolean test(Integer n) {
            return n % 2 == 0;
        }
    }

    // ========================================================================
    // ENUM WITH CONSTANT-SPECIFIC BODIES
    // ========================================================================
    @Nested
    class EnumWithBody {

        @Test
        void typeResolvesToDeclaringClassNotAnonymousSubclass() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_enumbody_type", "cfg", "", 0);
            EnumField<Op> op = b.defineEnum("op", Op.ADD).end();
            b.build();
            // Op.ADD.getClass() IS THE ANONYMOUS Op$1; type() MUST REPORT THE DECLARING Op
            assertEquals(Op.class, op.type(), "body enum must resolve its declaring class");
            assertNotNull(Op.class.getEnumConstants(), "declaring class exposes its constants");
        }

        @Test
        void bodyEnumRoundTripsThroughFile() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_enumbody_rt", "cfg", "", 0);
            EnumField<Op> op = b.defineEnum("op", Op.ADD).end();
            ConfigSpec spec = b.build();

            op.set(Op.SUB);
            spec.save();
            op.set(Op.ADD);
            assertTrue(spec.load());
            // WITH THE OLD getClass() BUG THE DECODE WOULD FAIL AND RESET TO ADD
            assertEquals(Op.SUB, op.get(), "body enum must decode back to the saved constant");
        }
    }

    // ========================================================================
    // COLLECTION FILTER ACTUALLY DROPPING ENTRIES
    // ========================================================================
    @Nested
    class CollectionFilter {

        @Test
        void filterDropsRejectedEntriesOnValidate() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_filter_validate", "cfg", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(2)), Integer.class).filter(EvenOnly.class).end();
            b.build();

            nums.setArray(new Integer[]{1, 2, 3, 4, 5, 6});
            nums.validate();
            assertEquals(List.of(2, 4, 6), nums.get(), "the filter must drop the odd entries");
        }

        @Test
        void filterDropsRejectedEntriesOnLoad() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_filter_load", "cfg", "", 0);
            ListField<Integer> nums = b.defineList("nums", new ArrayList<>(List.of(2)), Integer.class).filter(EvenOnly.class).end();
            ConfigSpec spec = b.build();

            // setArray DOES NOT VALIDATE, SO THE FILE HOLDS ALL FOUR; LOAD MUST FILTER ON THE WAY IN
            nums.setArray(new Integer[]{1, 2, 3, 4});
            spec.save();
            nums.setArray(new Integer[]{0});
            assertTrue(spec.load());
            assertEquals(List.of(2, 4), nums.get(), "load must apply the filter and drop odds");
        }
    }

    // ========================================================================
    // UUID / PATH / FILE CODEC NULL RECOVERY
    // ========================================================================
    @Nested
    class CodecNullRecovery {

        private static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final UUID MARKER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        @Test
        void codecsReturnNullOnMalformedInput() {
            // A NUL CHARACTER IS AN ILLEGAL PATH CHAR ON EVERY PLATFORM
            String illegalPath = "bad" + ((char) 0) + "path";
            assertNull(new UUIDCodec().decode("not-a-uuid"), "bad UUID must decode to null");
            assertNull(new PathCodec().decode(illegalPath), "illegal path must decode to null");
            assertNull(new FileCodec().decode(illegalPath), "illegal file path must decode to null");
        }

        // BUILDER SPEC WITH A CUSTOM UUID FIELD: THE DEFAULT IS FIXED, NO SHARED STATIC STATE
        @Test
        void badUuidResetsOnlyItsFieldAndKeepsSiblings() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_uuid_bad", "cfg", "", 0);
            BaseConfigField<UUID, Object> id = b.<UUID, Object>define("id", DEFAULT_ID, UUID.class, null).end();
            IntField keep = b.defineInt("keep", 5).end();
            ConfigSpec spec = b.build();

            // MARKER PROVES THE RESET ACTUALLY CHANGES THE VALUE BACK TO DEFAULT
            id.set(MARKER_ID);
            Files.writeString(spec.path(), """
                    {
                      id: "not-a-uuid"
                      keep: 7
                    }
                    """, StandardCharsets.UTF_8);
            assertTrue(spec.load());

            assertEquals(DEFAULT_ID, id.get(), "a bad UUID resets to default without aborting the load");
            assertEquals(7, keep.getAsInt(), "a sibling field still loads (per-field isolation)");
        }

        @Test
        void validUuidRoundTrips() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("guard_uuid_ok", "cfg", "", 0);
            BaseConfigField<UUID, Object> id = b.<UUID, Object>define("id", DEFAULT_ID, UUID.class, null).end();
            IntField keep = b.defineInt("keep", 5).end();
            ConfigSpec spec = b.build();

            id.set(MARKER_ID);
            keep.set(9);
            spec.save();
            id.set(DEFAULT_ID);
            keep.set(0);
            assertTrue(spec.load());
            assertEquals(MARKER_ID, id.get());
            assertEquals(9, keep.getAsInt());
        }
    }
}
