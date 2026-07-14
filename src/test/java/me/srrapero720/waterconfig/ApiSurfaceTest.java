package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.Control;
import me.srrapero720.waterconfig.api.IConfigField;
import me.srrapero720.waterconfig.api.annotations.Comment;
import me.srrapero720.waterconfig.api.annotations.ListConditions;
import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.PathConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.annotations.StringConditions;
import me.srrapero720.waterconfig.impl.codecs.*;
import me.srrapero720.waterconfig.impl.fields.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates every function declared by the WaterConfig public API surface.
 */
public class ApiSurfaceTest {

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

    enum Sample { A, B, C }

    public static class NonEmptyFilter implements Predicate<String> {
        @Override
        public boolean test(String s) {
            return !s.isEmpty();
        }
    }

    // ========================================================================
    // REGISTRATION API
    // ========================================================================
    @Nested
    class RegistrationTest {

        @Spec(value = "api_reg_static", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class StaticCfg {
            @Spec.Field public static int number = 7;
            @Spec.Field public static String text = "seven";
            @Spec.Field public static boolean flag = false;
            @Spec.Field public static char letter = 'g';
            @Spec.Field public static Sample mode = Sample.B;
            @Spec.Field public static UUID uid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            @Spec.Field public static String[] tags = {"a", "b"};
            @Spec.Field public static Integer[] nums = {1, 2};
            @Spec.Field public static List<Integer> sizes = new ArrayList<>(List.of(3, 4));
        }

        @Spec(value = "api_reg_inst", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class InstCfg {
            @Spec.Field public int number = 3;
            @Spec.Field public String text = "three";
            @Spec.Field public final SubCfg sub = new SubCfg();

            @Spec(value = "sub", disableStatic = true)
            static class SubCfg {
                @Spec.Field public double ratio = 1.25;
            }
        }

        @Spec(value = "api_reg_pass", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class PassCfg {
            @Spec.Field public static boolean enabled = true;
        }

        @Test
        void asyncRegisterLoadsAndClearsDirty() throws InterruptedException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_reg_async", "cfg", "", 0);
            IntField v = b.defineInt("v", 1).end();
            ConfigSpec spec = b.build();

            assertSame(spec, WaterConfig.register(spec));
            assertTrue(WaterConfig.isRegistered("api_reg_async"));

            // ASYNC LOAD PATH: THE IO POOL LOADS (OR SAVES DEFAULTS) SHORTLY AFTER register()
            long deadline = System.currentTimeMillis() + 5000;
            while (!spec.isLoaded() && System.currentTimeMillis() < deadline) {
                Thread.sleep(25);
            }

            assertTrue(spec.isLoaded(), "register() must load the spec asynchronously");
            assertFalse(spec.isDirty(), "dirty must be cleared once the initial load/save finishes");
            assertTrue(Files.exists(spec.path()), "missing files must be created with defaults");
            assertEquals(1, v.getAsInt());
        }

        @Test
        void registerBlockingClassWithStaticFields() throws IOException {
            ConfigSpec spec = WaterConfig.registerBlocking(StaticCfg.class);

            assertTrue(spec.isLoaded());
            assertTrue(WaterConfig.isRegistered("api_reg_static"));
            assertEquals("api_reg_static", spec.name());

            // REFLECT MODE: FIELD READS GO STRAIGHT TO THE STATIC FIELD
            StaticCfg.number = 21;
            assertEquals(21, spec.findField("number").get());

            // FULL ROUND TRIP FOR EVERY ANNOTATED FIELD KIND
            StaticCfg.text = "twenty one";
            StaticCfg.flag = true;
            StaticCfg.letter = 'q';
            StaticCfg.mode = Sample.C;
            StaticCfg.uid = UUID.fromString("00000000-0000-0000-0000-000000000002");
            StaticCfg.tags = new String[]{"x", "y", "z"};
            StaticCfg.nums = new Integer[]{5, 6};
            StaticCfg.sizes = new ArrayList<>(List.of(7));
            spec.save();

            StaticCfg.number = 0;
            StaticCfg.text = "?";
            StaticCfg.flag = false;
            StaticCfg.letter = 'x';
            StaticCfg.mode = Sample.A;
            StaticCfg.uid = UUID.fromString("00000000-0000-0000-0000-000000000001");
            StaticCfg.tags = new String[]{"a"};
            StaticCfg.nums = new Integer[]{1};
            StaticCfg.sizes = new ArrayList<>(List.of(3));
            spec.load();

            assertEquals(21, StaticCfg.number);
            assertEquals("twenty one", StaticCfg.text);
            assertTrue(StaticCfg.flag);
            assertEquals('q', StaticCfg.letter);
            assertEquals(Sample.C, StaticCfg.mode);
            assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), StaticCfg.uid);
            assertArrayEquals(new String[]{"x", "y", "z"}, StaticCfg.tags);
            assertArrayEquals(new Integer[]{5, 6}, StaticCfg.nums);
            assertEquals(List.of(7), StaticCfg.sizes);
        }

        @Test
        void registerBlockingInstanceWithInstanceFields() {
            InstCfg cfg = new InstCfg();
            ConfigSpec spec = WaterConfig.registerBlocking(cfg);

            assertTrue(spec.isLoaded());
            assertTrue(WaterConfig.isRegistered("api_reg_inst"));

            // FINAL FIELD OF A @Spec TYPE BECOMES A NESTED GROUP
            IConfigField<?, ?> ratio = spec.findField("sub.ratio");
            assertNotNull(ratio, "final nested spec instance field must register a group");
            assertEquals(1.25, ratio.get());

            // REFLECT MODE ON INSTANCE FIELDS, BOTH DIRECTIONS
            cfg.number = 33;
            assertEquals(33, spec.findField("number").get());
            spec.findField("text").set0("thirty three");
            assertEquals("thirty three", cfg.text);
        }

        @Test
        void registerObjectClassPassthrough() {
            // register(Object) MUST DELEGATE TO THE CLASS PATH WHEN GIVEN A Class INSTANCE
            Object clazzAsObject = PassCfg.class;
            ConfigSpec spec = WaterConfig.register(clazzAsObject);

            assertTrue(WaterConfig.isRegistered("api_reg_pass"));
            assertNotNull(spec.findField("enabled"), "static fields prove the class path was taken");
            assertEquals(Boolean.TRUE, spec.findField("enabled").get());
        }

        @Test
        void duplicateNameReplacesSilently() {
            ConfigSpec.SpecBuilder b1 = new ConfigSpec.SpecBuilder("api_reg_dup", "cfg", "", 0);
            b1.defineInt("v", 1).end();
            WaterConfig.register(b1.build());

            ConfigSpec.SpecBuilder b2 = new ConfigSpec.SpecBuilder("api_reg_dup", "cfg", "", 0);
            b2.defineInt("v", 2).end();
            ConfigSpec replacement = b2.build();

            assertDoesNotThrow(() -> WaterConfig.register(replacement));
            assertTrue(WaterConfig.isRegistered("api_reg_dup"));
        }

        @Test
        void unloadRemovesAndSavesPending() throws InterruptedException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_reg_unload", "cfg", "", 0);
            IntField v = b.defineInt("v", 1).end();
            ConfigSpec spec = b.build();
            WaterConfig.register(spec);

            long deadline = System.currentTimeMillis() + 5000;
            while (!spec.isLoaded() && System.currentTimeMillis() < deadline) {
                Thread.sleep(25);
            }
            assertTrue(spec.isLoaded());

            v.set(777);
            assertTrue(spec.isDirty());

            WaterConfig.unload("api_reg_unload");
            assertFalse(WaterConfig.isRegistered("api_reg_unload"));

            // FINAL ASYNC SAVE MUST FLUSH THE PENDING DIRTY VALUE
            String content = "";
            deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (Files.exists(spec.path())) {
                        content = Files.readString(spec.path(), StandardCharsets.UTF_8);
                        if (content.contains("777")) break;
                    }
                } catch (IOException ignored) {
                    // FILE MAY BE MID-WRITE, RETRY
                }
                Thread.sleep(25);
            }
            assertTrue(content.contains("777"), "unload must flush pending dirty values to disk");
        }

        @Test
        void unloadUnknownNameIsNoop() {
            assertDoesNotThrow(() -> WaterConfig.unload("api_reg_ghost"));
            assertFalse(WaterConfig.isRegistered("api_reg_ghost"));
        }

        @Test
        void invalidRegistrationsThrow() {
            assertThrows(NullPointerException.class, () -> WaterConfig.register((Class<?>) null));
            assertThrows(NullPointerException.class, () -> WaterConfig.register((Object) null));
            assertThrows(IllegalArgumentException.class, () -> WaterConfig.register(String.class), "class without @Spec must be rejected");
        }

        @Test
        void pathAccessors() {
            Path original = WaterConfig.getPath();
            assertEquals(tempDir, original);

            Path sub = tempDir.resolve("api_alt");
            try {
                WaterConfig.setPath(sub);
                assertEquals(sub, WaterConfig.getPath());
                ConfigSpec spec = new ConfigSpec.SpecBuilder("api_reg_pathed", "cfg", "", 0).build();
                assertTrue(spec.path().startsWith(sub.toAbsolutePath()), "builder must resolve against the active config path");
            } finally {
                WaterConfig.setPath(original);
            }
        }
    }

    // ========================================================================
    // NESTED SPECS
    // ========================================================================
    @Nested
    class NestedSpecTest {

        @Spec(value = "api_nested_inner", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class OuterCfg {
            @Spec.Field public static int top = 1;

            @Spec("child")
            static class Child {
                @Spec.Field public int inner = 5;
            }
        }

        @Spec(value = "api_nested_field", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class HolderCfg {
            @Spec.Field public static int top = 1;
            @Spec.Field public static final Sub sub = new Sub();

            @Spec(value = "subgroup", disableStatic = true)
            static class Sub {
                @Spec.Field public int inner = 9;
            }
        }

        @Spec(value = "api_nested_nonfinal", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class BadHolderCfg {
            @Spec.Field public static Sub2 sub2 = new Sub2();

            @Spec(value = "sub2", disableStatic = true)
            static class Sub2 {
                @Spec.Field public int inner = 1;
            }
        }

        @Spec(value = "api_nested_disable", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class DisabledCfg {
            @Spec.Field public static int top = 2;

            @Spec(value = "ghost", disableStatic = true)
            static class Ghost {
                @Spec.Field public int inner = 1;
            }
        }

        @Test
        void innerAnnotatedClassBecomesGroup() {
            ConfigSpec spec = WaterConfig.register(OuterCfg.class);

            IConfigField<?, ?> group = spec.findField("child");
            assertNotNull(group);
            assertEquals(Control.FOLDER, group.control());

            IConfigField<?, ?> inner = spec.findField("child.inner");
            assertNotNull(inner);
            assertEquals(5, inner.get());
            assertEquals("api_nested_inner:child.inner", inner.id());
            assertEquals(2, inner.groupCount());
        }

        @Test
        void finalFieldNestedSpecInstance() {
            ConfigSpec spec = WaterConfig.register(HolderCfg.class);

            assertEquals(9, spec.findField("subgroup.inner").get());

            // disableStatic PREVENTS THE DECLARED CLASS FROM REGISTERING A SECOND GROUP
            int groups = 0;
            for (IConfigField<?, ?> f : spec.getFields()) {
                if (f instanceof ConfigGroup) groups++;
            }
            assertEquals(1, groups, "the nested spec must only register once (via the final field)");
        }

        @Test
        void nonFinalNestedSpecFieldThrows() {
            assertThrows(IllegalArgumentException.class, () -> WaterConfig.register(BadHolderCfg.class));
        }

        @Test
        void disableStaticInnerClassSkipped() {
            ConfigSpec spec = WaterConfig.register(DisabledCfg.class);
            assertNull(spec.findField("ghost"), "disableStatic inner classes must not auto-register");
            assertNotNull(spec.findField("top"));
        }
    }

    // ========================================================================
    // SPEC BUILDER
    // ========================================================================
    @Nested
    class BuilderTest {

        @Test
        void allDefineTypesSaveLoadRoundTrip() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_all", "cfg", "", 0);
            BooleanField flag = b.defineBoolean("flag", false).end();
            ByteField by = b.defineByte("by", (byte) 1).end();
            ShortField sh = b.defineShort("sh", (short) 2).end();
            CharField ch = b.defineChar("ch", 'a').end();
            IntField in = b.defineInt("in", 3).end();
            LongField lo = b.defineLong("lo", 4L).end();
            FloatField fl = b.defineFloat("fl", 0.5F).end();
            DoubleField db = b.defineDouble("db", 1.5).end();
            StringField st = b.defineString("st", "default").end();
            EnumField<Sample> en = b.defineEnum("en", Sample.A).end();
            ListField<Integer> li = b.defineList("li", new ArrayList<>(List.of(3, 4)), Integer.class).end();
            PathField pa = b.definePath("pa", tempDir.resolve("default.bin")).runtimePath(false).end();
            var uid = b.define("uid", UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.class, null).end();
            ConfigSpec spec = b.build();

            flag.set(true);
            by.set((byte) 12);
            sh.set((short) 1234);
            ch.set('z');
            in.set(56789);
            lo.set(9876543210L);
            fl.set(2.5F);
            db.set(3.25);
            st.set("changed value");
            en.set(Sample.C);
            li.set(new ArrayList<>(List.of(8, 9)));
            pa.set(tempDir.resolve("other.bin"));
            uid.set(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            spec.save();

            flag.reset(); by.reset(); sh.reset(); ch.reset(); in.reset(); lo.reset();
            fl.reset(); db.reset(); st.reset(); en.reset(); li.reset(); pa.reset(); uid.reset();
            assertEquals(3, in.getAsInt());
            assertEquals("default", st.get());

            spec.load();

            assertTrue(flag.getAsBoolean());
            assertEquals((byte) 12, by.getAsByte());
            assertEquals(12, by.getAsInt());
            assertEquals((short) 1234, sh.getAsShort());
            assertEquals(1234, sh.getAsInt());
            assertEquals('z', ch.getAsChar());
            assertEquals(56789, in.getAsInt());
            assertEquals(9876543210L, lo.getAsLong());
            assertEquals(2.5F, fl.getAsFloat());
            assertEquals(2.5, fl.getAsDouble());
            assertEquals(3.25, db.getAsDouble());
            assertEquals("changed value", st.get());
            assertEquals(Sample.C, en.get());
            assertTrue(en.compareTo(Sample.B) > 0);
            assertEquals(Sample.class, en.type());
            assertEquals(Sample.class, en.subType());
            assertEquals(List.of(8, 9), li.get());
            assertThrows(UnsupportedOperationException.class, () -> li.get().add(1), "ListField.get() must be unmodifiable");
            assertEquals(tempDir.resolve("other.bin"), pa.get());
            assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), uid.get());
        }

        @Test
        void stringListSaveLoadRoundTrip() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_strlist", "cfg", "", 0);
            ListField<String> names = b.defineList("names", new ArrayList<>(List.of("alpha", "beta")), String.class).end();
            ConfigSpec spec = b.build();

            names.set(new ArrayList<>(List.of("gamma", "delta")));
            spec.save();
            names.reset();
            spec.load();

            assertEquals(List.of("gamma", "delta"), names.get());
        }

        @Test
        void groupNavigationPushPopPopAll() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_nav", "cfg", "", 0);
            b.push("a.b");
            IntField leaf = b.defineInt("leaf", 1).end();
            b.pop(); // A DOTTED PUSH IS UNDONE BY A SINGLE SYMMETRIC pop()
            b.push("c");
            IntField inner = b.defineInt("inner", 2).end();
            b.popAll();
            b.push("ghost");
            b.pop(); // EMPTY GROUP MUST BE DISPOSED
            IntField root = b.defineInt("root", 3).end();
            ConfigSpec spec = b.build();

            assertSame(leaf, spec.findField("a.b.leaf"), "push must split dotted names into nested groups");
            assertEquals("api_b_nav:a.b.leaf", leaf.id());
            assertEquals(3, leaf.groupCount());
            assertSame(inner, spec.findField("c.inner"));
            assertSame(root, spec.findField("root"));
            assertEquals(1, root.groupCount());
            assertNull(spec.findField("ghost"), "empty groups must be disposed on pop()");

            ConfigGroup a = (ConfigGroup) spec.findField("a");
            assertEquals("a", a.name());
            assertEquals(Control.FOLDER, a.control());

            assertThrows(IllegalArgumentException.class,
                    () -> new ConfigSpec.SpecBuilder("api_b_nav2", "cfg", "", 0).pop(0));
        }

        @Test
        void builderAndGroupComments() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_comments", "cfg", "", 0);
            b.comments("spec first", "spec second").comment("spec third");
            b.push("g");
            b.comment("group note");
            b.defineInt("x", 1).comments("field a", "field b").end();
            b.pop();
            ConfigSpec spec = b.build();

            assertArrayEquals(new String[]{"spec first", "spec second", "spec third"}, spec.comments());
            assertArrayEquals(new String[]{"group note"}, spec.findField("g").comments());
            assertArrayEquals(new String[]{"field a", "field b"}, spec.findField("g.x").comments());
        }

        @Test
        void builderArgumentValidation() {
            assertThrows(IllegalArgumentException.class, () -> new ConfigSpec.SpecBuilder(null, "cfg", "", 0));
            assertThrows(IllegalArgumentException.class, () -> new ConfigSpec.SpecBuilder("", "cfg", "", 0));
            assertThrows(IllegalArgumentException.class, () -> new ConfigSpec.SpecBuilder("api_b_badfmt", "nope", "", 0));
            assertThrows(IllegalArgumentException.class, () -> new ConfigSpec.SpecBuilder("api_b_badbak", "cfg", "", -1));
        }

        @Test
        void fileSuffixAppendedToFileName() {
            ConfigSpec spec = new ConfigSpec.SpecBuilder("api_b_sfx", "cfg", "dev", 0).build();
            assertEquals("api_b_sfx-dev.cfg", spec.path().getFileName().toString());
        }

        @Test
        @SuppressWarnings("removal") // singleline IS DEPRECATED BUT STILL PART OF THE DECLARED BUILDER API
        void listOptionsWiringAndValidate() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_list", "cfg", "", 0);
            ListField<String> lf = b.defineList("lf", new ArrayList<>(List.of("a", "b")), String.class)
                    .allowEmpty(false).unique(true).limit(2)
                    .stringify(true).singleline(false)
                    .filter(NonEmptyFilter.class)
                    .end();
            b.build();

            assertFalse(lf.allowEmpty);
            assertTrue(lf.unique);
            assertEquals(2, lf.limit);
            assertTrue(lf.stringify);
            assertFalse(lf.singleline);
            assertEquals(NonEmptyFilter.class, lf.filter);
            assertEquals(String.class, lf.subType());

            // EMPTY LIST RESETS WHEN allowEmpty IS FALSE
            lf.clear();
            lf.validate();
            assertEquals(List.of("a", "b"), lf.get());

            // UNIQUE DEDUPLICATES
            lf.setArray(new String[]{"x", "x"});
            lf.validate();
            assertEquals(List.of("x"), lf.get());

            // LIMIT TRUNCATES
            lf.setArray(new String[]{"p", "q", "r"});
            lf.validate();
            assertEquals(List.of("p", "q"), lf.get());

            // COLLECTION OPERATIONS
            lf.add("s");
            assertEquals(3, lf.size());
            assertTrue(lf.contains("s"));
            lf.remove("s");
            assertFalse(lf.contains("s"));
            assertEquals("q", lf.remove(1));
            assertEquals(1, lf.size());
        }

        @Test
        void pathOptionsValidate() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_b_path", "cfg", "", 0);
            PathField runtime = b.definePath("runtime", Path.of("data", "cfg.bin")).end();
            Path existing = Files.createFile(tempDir.resolve("api_existing.txt"));
            PathField checked = b.definePath("checked", existing).runtimePath(false).fileExists(true).end();
            b.build();

            assertTrue(runtime.runtimePath, "runtimePath defaults to true");
            assertFalse(runtime.fileExists);
            assertFalse(checked.runtimePath);
            assertTrue(checked.fileExists);

            // RUNTIME PATHS REJECT ABSOLUTE VALUES
            runtime.set(tempDir.resolve("abs.bin"));
            runtime.validate();
            assertEquals(Path.of("data", "cfg.bin"), runtime.get());

            // fileExists RESETS MISSING TARGETS
            checked.set(tempDir.resolve("missing_file.bin"));
            checked.validate();
            assertEquals(existing, checked.get());
        }
    }

    // ========================================================================
    // FIELD API
    // ========================================================================
    @Nested
    class FieldApiTest {

        @Spec(value = "api_a_meta", format = WaterConfig.FORMAT_CFG, backups = 0)
        @Comment("Root spec comment")
        static class SuffixCfg {
            @Spec.Field(suffix = "MB") public static int cache = 512;
            @Spec.Field public static int plain = 1;
            @Comment("first line")
            @Comment("second line")
            @Spec.Field public static int commented = 1;
            @Spec.Field("renamed") public static int oddName = 2;
        }

        @Spec(value = "api_ctrl_ok", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class ControlCfg {
            @Spec.Field(control = Control.CHECKBOX) public static boolean box = true;
            @Spec.Field public static boolean plain = true;
        }

        @Spec(value = "api_ctrl_bad", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class BadControlCfg {
            @Spec.Field(control = Control.SWITCH) public static int x = 1;
        }

        @Test
        void idAndGroupCount() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_id", "cfg", "", 0);
            IntField top = b.defineInt("top", 1).end();
            b.push("g1.g2");
            IntField leaf = b.defineInt("leaf", 2).end();
            b.popAll();
            ConfigSpec spec = b.build();

            assertEquals("api_f_id:", spec.id());
            assertEquals("api_f_id:top", top.id());
            assertEquals("api_f_id:g1.g2.leaf", leaf.id());

            ConfigGroup g1 = (ConfigGroup) spec.findField("g1");
            ConfigGroup g2 = (ConfigGroup) spec.findField("g1.g2");
            assertEquals("api_f_id:g1", g1.id());
            assertEquals("api_f_id:g1.g2", g2.id());

            assertEquals(0, spec.groupCount());
            assertEquals(1, top.groupCount());
            assertEquals(1, g1.groupCount());
            assertEquals(3, leaf.groupCount());

            assertSame(spec, leaf.spec());
            assertSame(g2, leaf.group());
            assertSame(spec, top.group());
            assertEquals("leaf", leaf.name());
            assertEquals("api_f_id", spec.name());
        }

        @Test
        void specQualifiedIdsResolve() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_qid", "cfg", "", 0);
            IntField top = b.defineInt("top", 1).end();
            b.push("g");
            IntField leaf = b.defineInt("leaf", 2).end();
            b.popAll();
            ConfigSpec spec = b.build();

            assertSame(top, spec.findField(top.id()));
            assertSame(leaf, spec.findField(leaf.id()));
        }

        @Test
        void findFieldEdgeCases() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_find", "cfg", "", 0);
            IntField top = b.defineInt("top", 1).end();
            b.push("g1.g2");
            IntField leaf = b.defineInt("leaf", 2).end();
            b.popAll();
            ConfigSpec spec = b.build();

            assertNull(spec.findField(null));
            assertNull(spec.findField(""));
            assertNull(spec.findField("missing"));
            assertNull(spec.findField("top.bogus"), "non-group segments must abort the lookup");
            assertSame(top, spec.findField("top"));
            assertSame(leaf, spec.findField("g1.g2.leaf"));

            ConfigGroup g1 = (ConfigGroup) spec.findField("g1");
            assertSame(leaf, g1.findField("g2.leaf"), "lookups are relative to the group");
            assertNull(g1.findField("other_spec:g2.leaf"), "foreign spec ids must return null");
        }

        @Test
        void controlDefaultsResolvePerType() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_ctrl", "cfg", "", 0);
            var flag = b.defineBoolean("flag", true).end();
            var count = b.defineInt("count", 1).end();
            var text = b.defineString("text", "x").end();
            var letter = b.defineChar("letter", 'x').end();
            var mode = b.defineEnum("mode", Sample.A).end();
            var list = b.defineList("list", new ArrayList<>(List.of(1)), Integer.class).end();
            var path = b.definePath("path", Path.of("rel")).end();
            var custom = b.define("custom", UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.class, null).end();
            b.push("grp");
            b.defineInt("gx", 1).end();
            b.pop();
            ConfigSpec spec = b.build();

            assertEquals(Control.SWITCH, flag.control());
            assertEquals(Control.NUMBER_SPINNER, count.control());
            assertEquals(Control.INPUT, text.control());
            assertEquals(Control.INPUT, letter.control());
            assertEquals(Control.DROPDOWN, mode.control());
            assertEquals(Control.LIST_EDITOR, list.control());
            assertEquals(Control.INPUT_FILE, path.control());
            assertEquals(Control.INPUT, custom.control());
            assertEquals(Control.FOLDER, spec.findField("grp").control());
        }

        @Test
        void explicitControlsHonored() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_ctrl2", "cfg", "", 0);
            assertEquals(Control.CHECKBOX, b.defineBoolean("c1", true).control(Control.CHECKBOX).end().control());
            assertEquals(Control.SEEKBAR, b.defineInt("c2", 1).control(Control.SEEKBAR).end().control());
            assertEquals(Control.KNOB, b.defineLong("c3", 1L).control(Control.KNOB).end().control());
            assertEquals(Control.RANGE_SLIDER, b.defineDouble("c4", 1D).control(Control.RANGE_SLIDER).end().control());
            assertEquals(Control.PASSWORD, b.defineString("c5", "x").control(Control.PASSWORD).end().control());
            assertEquals(Control.SEGMENTED, b.defineEnum("c6", Sample.B).control(Control.SEGMENTED).end().control());
            assertEquals(Control.LIST_SPINNER, b.defineEnum("c7", Sample.B).control(Control.LIST_SPINNER).end().control());
            assertEquals(Control.TAG_INPUT, b.defineList("c8", new ArrayList<>(List.of(1)), Integer.class).control(Control.TAG_INPUT).end().control());
            assertEquals(Control.CHECKBOX_LIST, b.defineList("c9", new ArrayList<>(List.of(1)), Integer.class).control(Control.CHECKBOX_LIST).end().control());
            assertEquals(Control.INPUT_FOLDER, b.definePath("c10", Path.of("r")).control(Control.INPUT_FOLDER).end().control());
        }

        @Test
        void incoherentControlsThrow() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_ctrl_bad", "cfg", "", 0);
            assertThrows(IllegalArgumentException.class, () -> b.defineInt("x1", 1).control(Control.SWITCH).end());
            assertThrows(IllegalArgumentException.class, () -> b.defineBoolean("x2", true).control(Control.SEEKBAR).end());
            assertThrows(IllegalArgumentException.class, () -> b.defineEnum("x3", Sample.A).control(Control.SWITCH).end());
            assertThrows(IllegalArgumentException.class, () -> b.defineList("x4", new ArrayList<>(List.of(1)), Integer.class).control(Control.SWITCH).end());
            assertThrows(IllegalArgumentException.class, () -> b.definePath("x5", Path.of("r")).control(Control.SEEKBAR).end());

            // ANNOTATION PATH: INCOHERENT CONTROL ABORTS REGISTRATION, COHERENT ONES ARE APPLIED
            assertThrows(IllegalArgumentException.class, () -> WaterConfig.register(BadControlCfg.class));
            ConfigSpec ok = WaterConfig.register(ControlCfg.class);
            assertEquals(Control.CHECKBOX, ok.findField("box").control());
            assertEquals(Control.SWITCH, ok.findField("plain").control());
        }

        @Test
        void suffixPropagation() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_sfx", "cfg", "", 0);
            var mb = b.defineInt("mb", 1).suffix("MB").end();
            var plain = b.defineInt("plain", 1).end();
            var nulled = b.defineInt("nulled", 1).suffix(null).end();
            b.push("grp");
            b.defineInt("gx", 1).end();
            b.pop();
            ConfigSpec spec = b.build();

            assertEquals("MB", mb.suffix());
            assertEquals("", plain.suffix());
            assertEquals("", nulled.suffix(), "null suffix must normalize to empty");
            assertEquals("", spec.findField("grp").suffix());
            assertEquals("", spec.suffix());
        }

        @Test
        void annotatedMetadata() {
            ConfigSpec spec = WaterConfig.register(SuffixCfg.class);

            assertEquals("MB", spec.findField("cache").suffix());
            assertEquals("", spec.findField("plain").suffix());
            assertArrayEquals(new String[]{"first line", "second line"}, spec.findField("commented").comments());
            assertNotNull(spec.findField("renamed"), "@Spec.Field value must override the java field name");
            assertNull(spec.findField("oddName"));
            assertArrayEquals(new String[]{"Root spec comment"}, spec.comments());
        }

        @Test
        void resetSet0AndValidateSemantics() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_val", "cfg", "", 0);
            IntField v = b.defineInt("v", 5).setMin(0).setMax(10).end();
            b.build();

            // set/accept DO NOT VALIDATE
            v.set(50);
            assertEquals(50, v.getAsInt());
            v.validate();
            assertEquals(5, v.getAsInt(), "validate must reset out-of-range values");

            // set0 VALIDATES INLINE
            v.set0(7);
            assertEquals(7, v.getAsInt());
            v.set0(99);
            assertEquals(5, v.getAsInt());

            // set0(null) RESETS
            v.set(8);
            v.set0(null);
            assertEquals(5, v.getAsInt());

            v.set(9);
            v.reset();
            assertEquals(5, v.getAsInt());

            assertEquals(Integer.class, v.type());
            assertNull(v.subType());
        }

        @Test
        void markDirtyForeignSpecThrows() {
            ConfigSpec.SpecBuilder ba = new ConfigSpec.SpecBuilder("api_f_dirty_a", "cfg", "", 0);
            IntField fa = ba.defineInt("fa", 1).end();
            ConfigSpec specA = ba.build();

            ConfigSpec.SpecBuilder bb = new ConfigSpec.SpecBuilder("api_f_dirty_b", "cfg", "", 0);
            IntField fb = bb.defineInt("fb", 1).end();
            bb.build();

            assertThrows(IllegalArgumentException.class, () -> specA.markDirty(fb));
            specA.setDirty(false);
            specA.markDirty(fa);
            assertTrue(specA.isDirty());
        }

        @Test
        void groupUnsupportedOps() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_f_group", "cfg", "", 0);
            b.push("g");
            b.defineInt("x", 1).end();
            b.pop();
            ConfigSpec spec = b.build();

            ConfigGroup g = (ConfigGroup) spec.findField("g");
            assertEquals(Control.FOLDER, g.control());
            assertEquals("", g.suffix());
            assertEquals(1, g.getFields().size());
            assertThrows(UnsupportedOperationException.class, g::type);
            assertThrows(UnsupportedOperationException.class, g::subType);
            assertThrows(UnsupportedOperationException.class, g::reset);
            assertThrows(UnsupportedOperationException.class, g::validate);
            assertThrows(UnsupportedOperationException.class, () -> g.set0("x"));
            assertThrows(UnsupportedOperationException.class, () -> g.accept(null));
            assertThrows(UnsupportedOperationException.class, g::get);
            assertThrows(UnsupportedOperationException.class, () -> g.getFields().clear(), "getFields must be unmodifiable");
        }
    }

    // ========================================================================
    // CONDITION ANNOTATIONS
    // ========================================================================
    @Nested
    class ConditionsTest {

        @Spec(value = "api_cond_num", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class NumCfg {
            @Spec.Field @NumberConditions(minByte = 1, maxByte = 10) public static byte byteVal = 5;
            @Spec.Field @NumberConditions(minShort = 1, maxShort = 100) public static short shortVal = 50;
            @Spec.Field @NumberConditions(minInt = 0, maxInt = 1000, math = true) public static int intVal = 500;
            @Spec.Field @NumberConditions(minLong = -10, maxLong = 10, math = true, strictMath = true) public static long longVal = 0;
            @Spec.Field @NumberConditions(minFloat = 0F, maxFloat = 1F) public static float floatVal = 0.5F;
            @Spec.Field @NumberConditions(minDouble = 0D, maxDouble = 2D) public static double doubleVal = 1.0;
        }

        @Spec(value = "api_cond_str", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class StrCfg {
            @Spec.Field @StringConditions(startsWith = "pre") public static String sw = "prefix";
            @Spec.Field @StringConditions(endsWith = "end") public static String ew = "the_end";
            @Spec.Field @StringConditions(allowEmpty = false) public static String ne = "filled";
            @Spec.Field @StringConditions("core") public static String ct = "has_core_inside";
            @Spec.Field @StringConditions(value = "exact", mode = StringField.Mode.EQUALS) public static String eq = "exact";
            @Spec.Field @StringConditions(value = "^[a-z]+$", mode = StringField.Mode.REGEX, regexFlags = Pattern.CASE_INSENSITIVE) public static String rxci = "Word";
            @Spec.Field @StringConditions(value = "^[a-z]+$", mode = StringField.Mode.REGEX, regexFlags = 0) public static String rxcs = "word";
            @Spec.Field @StringConditions(value = "banned", mode = StringField.Mode.NOT_CONTAINS) public static String nc = "clean";
            @Spec.Field @StringConditions(value = "taken", mode = StringField.Mode.NOT_EQUALS) public static String nq = "free";
            @Spec.Field @StringConditions(value = "^\\d+$", mode = StringField.Mode.NOT_REGEX) public static String nr = "letters";
        }

        @Spec(value = "api_cond_list", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class ListCfg {
            @Spec.Field @ListConditions(stringify = true, allowEmpty = false, unique = true, limit = 3)
            public static List<Integer> entries = new ArrayList<>(List.of(1, 2));
        }

        @Spec(value = "api_cond_path", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class PathCfg {
            @Spec.Field @PathConditions(runtimePath = false, existsFile = true)
            public static Path target = Path.of("data", "file.bin");
        }

        @Test
        void numberConditionsPerType() throws IOException {
            ConfigSpec spec = WaterConfig.registerBlocking(NumCfg.class);

            ByteField byteField = (ByteField) spec.findField("byteVal");
            ShortField shortField = (ShortField) spec.findField("shortVal");
            IntField intField = (IntField) spec.findField("intVal");
            LongField longField = (LongField) spec.findField("longVal");
            FloatField floatField = (FloatField) spec.findField("floatVal");
            DoubleField doubleField = (DoubleField) spec.findField("doubleVal");

            // WIRING: MIN/MAX, MATH AND STRICT MATH TAKEN FROM THE ANNOTATION
            assertEquals((byte) 1, byteField.min);
            assertEquals((byte) 10, byteField.max);
            assertEquals("1", byteField.minValueString());
            assertEquals("10", byteField.maxValueString());
            assertEquals((short) 1, shortField.min);
            assertEquals((short) 100, shortField.max);
            assertEquals(0, intField.min);
            assertEquals(1000, intField.max);
            assertEquals("0", intField.minValueString());
            assertEquals("1000", intField.maxValueString());
            assertEquals(-10L, longField.min);
            assertEquals(10L, longField.max);
            assertEquals("-10", longField.minValueString());
            assertEquals(0F, floatField.min);
            assertEquals(1F, floatField.max);
            assertEquals(0D, doubleField.min);
            assertEquals(2D, doubleField.max);
            assertTrue(intField.math());
            assertFalse(intField.strictMath());
            assertTrue(longField.math());
            assertTrue(longField.strictMath());

            // FULL-RANGE FIELDS REPORT NULL BOUND STRINGS
            IntField free = new ConfigSpec.SpecBuilder("api_cond_free", "cfg", "", 0).defineInt("free", 0).end();
            assertNull(free.minValueString());
            assertNull(free.maxValueString());

            // VALIDATE RESETS OUT-OF-RANGE VALUES PER TYPE
            byteField.set((byte) 99);
            byteField.validate();
            assertEquals((byte) 5, byteField.getAsByte());
            shortField.set((short) 999);
            shortField.validate();
            assertEquals((short) 50, shortField.getAsShort());
            intField.set(-5);
            intField.validate();
            assertEquals(500, intField.getAsInt());
            longField.set(11L);
            longField.validate();
            assertEquals(0L, longField.getAsLong());
            floatField.set(5F);
            floatField.validate();
            assertEquals(0.5F, floatField.getAsFloat());
            doubleField.set(-1D);
            doubleField.validate();
            assertEquals(1.0, doubleField.getAsDouble());

            // MATH EXPRESSIONS ARE EVALUATED ON LOAD
            // CLEAR PENDING CHANGES SO THE WORKER AUTOSAVE CANNOT CLOBBER THE EXTERNAL EDIT
            spec.setDirty(false);
            Files.writeString(spec.path(), """
                    {
                      byteVal: 5
                      shortVal: 50
                      intVal: 40 * 5
                      longVal: 3 + 4
                      floatVal: 0.5
                      doubleVal: 1.0
                    }
                    """, StandardCharsets.UTF_8);
            spec.load();
            assertEquals(200, NumCfg.intVal);
            assertEquals(7L, NumCfg.longVal);

            // STRICT MATH THROWS ON INVALID EXPRESSIONS
            spec.setDirty(false);
            Files.writeString(spec.path(), """
                    {
                      byteVal: 5
                      shortVal: 50
                      intVal: 500
                      longVal: abc + 1
                      floatVal: 0.5
                      doubleVal: 1.0
                    }
                    """, StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class, spec::load);
        }

        @Test
        void stringConditionsAllModes() {
            ConfigSpec spec = WaterConfig.registerBlocking(StrCfg.class);

            StringField sw = (StringField) spec.findField("sw");
            StringField ew = (StringField) spec.findField("ew");
            StringField ne = (StringField) spec.findField("ne");
            StringField ct = (StringField) spec.findField("ct");
            StringField eq = (StringField) spec.findField("eq");
            StringField rxci = (StringField) spec.findField("rxci");
            StringField rxcs = (StringField) spec.findField("rxcs");
            StringField nc = (StringField) spec.findField("nc");
            StringField nq = (StringField) spec.findField("nq");
            StringField nr = (StringField) spec.findField("nr");

            // WIRING
            assertEquals("pre", sw.startsWith);
            assertEquals("end", ew.endsWith);
            assertFalse(ne.allowEmpty);
            assertEquals("exact", eq.condition);
            assertEquals(StringField.Mode.EQUALS, eq.mode);
            assertEquals(Pattern.CASE_INSENSITIVE, rxci.regexFlags);
            assertEquals(0, rxcs.regexFlags);

            // startsWith
            sw.set("nope");
            sw.validate();
            assertEquals("prefix", sw.get());
            sw.set("prelude");
            sw.validate();
            assertEquals("prelude", sw.get());

            // endsWith
            ew.set("endless");
            ew.validate();
            assertEquals("the_end", ew.get());
            ew.set("bend");
            ew.validate();
            assertEquals("bend", ew.get());

            // allowEmpty = false
            ne.set("");
            ne.validate();
            assertEquals("filled", ne.get());

            // CONTAINS
            ct.set("nothing");
            ct.validate();
            assertEquals("has_core_inside", ct.get());
            ct.set("hardcore");
            ct.validate();
            assertEquals("hardcore", ct.get());

            // EQUALS
            eq.set("other");
            eq.validate();
            assertEquals("exact", eq.get());

            // REGEX + CASE_INSENSITIVE FLAG
            rxci.set("MixedCase");
            rxci.validate();
            assertEquals("MixedCase", rxci.get());
            rxci.set("abc123");
            rxci.validate();
            assertEquals("Word", rxci.get());

            // REGEX WITH FLAGS = 0 IS CASE SENSITIVE
            rxcs.set("UPPER");
            rxcs.validate();
            assertEquals("word", rxcs.get());
            rxcs.set("lower");
            rxcs.validate();
            assertEquals("lower", rxcs.get());

            // NOT_CONTAINS
            nc.set("banned_content");
            nc.validate();
            assertEquals("clean", nc.get());

            // NOT_EQUALS
            nq.set("taken");
            nq.validate();
            assertEquals("free", nq.get());

            // NOT_REGEX
            nr.set("12345");
            nr.validate();
            assertEquals("letters", nr.get());
            nr.set("abc");
            nr.validate();
            assertEquals("abc", nr.get());
        }

        @Test
        void listConditionsApplied() {
            ConfigSpec spec = WaterConfig.register(ListCfg.class);
            ListField<?> entries = (ListField<?>) spec.findField("entries");

            assertTrue(entries.stringify, "@ListConditions.stringify must reach the field");
            assertFalse(entries.allowEmpty, "@ListConditions.allowEmpty must reach the field");
            assertTrue(entries.unique, "@ListConditions.unique must reach the field");
            assertEquals(3, entries.limit, "@ListConditions.limit must reach the field");
        }

        @Test
        void pathConditionsApplied() {
            ConfigSpec spec = WaterConfig.register(PathCfg.class);
            PathField target = (PathField) spec.findField("target");

            assertFalse(target.runtimePath, "@PathConditions.runtimePath must reach the field");
            assertTrue(target.fileExists, "@PathConditions.existsFile must reach the field");
        }
    }

    // ========================================================================
    // CODECS
    // ========================================================================
    @Nested
    class CodecTest {

        @Test
        void numberCodecs() {
            ByteCodec bytes = new ByteCodec();
            assertEquals(Byte.class, bytes.type());
            assertEquals(Byte.MIN_VALUE, bytes.decode(bytes.encode(Byte.MIN_VALUE)));
            assertEquals((byte) 127, bytes.decode(bytes.encode((byte) 127)));
            assertNull(bytes.decode("999"));

            ShortCodec shorts = new ShortCodec();
            assertEquals(Short.class, shorts.type());
            assertEquals(Short.MIN_VALUE, shorts.decode(shorts.encode(Short.MIN_VALUE)));
            assertEquals(Short.MAX_VALUE, shorts.decode(shorts.encode(Short.MAX_VALUE)));
            assertNull(shorts.decode("word"));

            IntCodec ints = new IntCodec();
            assertEquals(Integer.class, ints.type());
            assertEquals(Integer.MIN_VALUE, ints.decode(ints.encode(Integer.MIN_VALUE)));
            assertEquals(42, ints.decode(ints.encode(42)));
            assertNull(ints.decode("4.2"));

            LongCodec longs = new LongCodec();
            assertEquals(Long.class, longs.type());
            assertEquals(Long.MAX_VALUE, longs.decode(longs.encode(Long.MAX_VALUE)));
            assertEquals(-7L, longs.decode(longs.encode(-7L)));
            assertNull(longs.decode("nope"));

            FloatCodec floats = new FloatCodec();
            assertEquals(Float.class, floats.type());
            assertEquals(-1.25F, floats.decode(floats.encode(-1.25F)));
            assertNull(floats.decode("x"));

            DoubleCodec doubles = new DoubleCodec();
            assertEquals(Double.class, doubles.type());
            assertEquals(Double.MAX_VALUE, doubles.decode(doubles.encode(Double.MAX_VALUE)));
            assertEquals(-3.14159, doubles.decode(doubles.encode(-3.14159)));
            assertNull(doubles.decode("y"));
        }

        @Test
        void booleanAndCharCodecs() {
            BooleanCodec bools = new BooleanCodec();
            assertEquals(Boolean.class, bools.type());
            assertTrue(bools.decode(bools.encode(true)));
            assertFalse(bools.decode(bools.encode(false)));

            CharCodec chars = new CharCodec();
            assertEquals(Character.class, chars.type());
            assertEquals('a', chars.decode(chars.encode('a')));
            char bolt = (char) 0x26A1; // UNICODE HIGH-BLOCK CHAR
            assertEquals(bolt, chars.decode(chars.encode(bolt)));
            assertNull(chars.decode("ab"));
            assertNull(chars.decode(""));
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void enumCodecComplexOnly() {
            EnumCodec codec = new EnumCodec();
            assertEquals(Enum.class, codec.type());

            Class<Enum> sample = (Class<Enum>) (Class<?>) Sample.class;
            assertEquals("B", codec.encode(Sample.B, Sample.class));
            assertSame(Sample.B, codec.decode("B", sample));
            assertNull(codec.decode("MISSING", sample));

            // SIMPLE ICodec ENTRY POINTS ARE EXPLICITLY UNSUPPORTED
            assertThrows(UnsupportedOperationException.class, () -> codec.decode("A"));
            assertThrows(UnsupportedOperationException.class, () -> codec.encode(Sample.A));
        }

        @Test
        void ioCodecs() {
            FileCodec files = new FileCodec();
            assertEquals(File.class, files.type());
            File absFile = tempDir.resolve("codec dir").resolve("data file.bin").toFile().getAbsoluteFile();
            assertEquals(absFile, files.decode(files.encode(absFile)));

            PathCodec paths = new PathCodec();
            assertEquals(Path.class, paths.type());
            Path spaced = tempDir.resolve("spaced dir").resolve("file name.txt");
            assertEquals(spaced.toAbsolutePath(), paths.decode(paths.encode(spaced)));

            URICodec uris = new URICodec();
            assertEquals(URI.class, uris.type());
            URI uri = URI.create("https://example.com/search?q=hello%20world&lang=en#frag");
            assertEquals(uri, uris.decode(uris.encode(uri)));
            assertNull(uris.decode("http://exa mple.com"));

            UUIDCodec uuids = new UUIDCodec();
            assertEquals(UUID.class, uuids.type());
            UUID id = UUID.randomUUID();
            assertEquals(id, uuids.decode(uuids.encode(id)));
        }
    }

    // ========================================================================
    // SAVE / LOAD LIFECYCLE
    // ========================================================================
    @Nested
    class LifecycleTest {

        @Test
        void dirtyFlagLifecycle() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_life_dirty", "cfg", "", 0);
            IntField v = b.defineInt("v", 1).end();
            ConfigSpec spec = b.build();

            assertTrue(spec.isDirty(), "fresh specs start dirty");
            spec.setDirty(false);
            assertFalse(spec.isDirty());

            v.set(2);
            assertTrue(spec.isDirty(), "field writes must mark the spec dirty");

            spec.setDirty(false);
            spec.markDirty(v);
            assertTrue(spec.isDirty());
        }

        @Test
        void reloadFlagLifecycle() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_life_reload", "cfg", "", 0);
            b.defineInt("v", 1).end();
            ConfigSpec spec = b.build();
            spec.save();

            assertFalse(spec.isReload());
            spec.setReload(true);
            assertTrue(spec.isReload());

            assertTrue(spec.load());
            assertFalse(spec.isReload(), "load must clear the reload flag");
            assertTrue(spec.isLoaded());
        }

        @Test
        void loadReturnsFalseWhenFileMissing() throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_life_missing", "cfg", "", 0);
            b.defineInt("v", 1).end();
            ConfigSpec spec = b.build();

            assertFalse(spec.load());
            assertFalse(spec.isLoaded());
        }
    }

    // ========================================================================
    // FORMATS REGISTRY
    // ========================================================================
    @Nested
    class FormatsTest {

        @Test
        void allFormatsRoundTrip() throws IOException {
            assertEquals("properties", WaterConfig.FORMAT_PROPERTIES);
            assertEquals("cfg", WaterConfig.FORMAT_CFG);
            assertEquals("json", WaterConfig.FORMAT_JSON);
            assertEquals("json5", WaterConfig.FORMAT_JSON5);
            assertEquals("toml", WaterConfig.FORMAT_TOML);

            String[] formats = {
                    WaterConfig.FORMAT_PROPERTIES,
                    WaterConfig.FORMAT_CFG,
                    WaterConfig.FORMAT_JSON,
                    WaterConfig.FORMAT_JSON5,
                    WaterConfig.FORMAT_TOML,
            };

            for (String format : formats) {
                ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("api_fmt_" + format, format, "", 0);
                IntField num = b.defineInt("num", 11).end();
                StringField text = b.defineString("text", "initial").end();
                BooleanField flag = b.defineBoolean("flag", true).end();
                DoubleField ratio = b.defineDouble("ratio", 1.5).end();
                b.push("grp");
                IntField inner = b.defineInt("inner", 3).end();
                b.pop();
                ConfigSpec spec = b.build();

                num.set(77);
                text.set("hello world");
                flag.set(false);
                ratio.set(6.25);
                inner.set(9);
                spec.save();
                assertTrue(spec.path().getFileName().toString().endsWith("." + format), format);

                num.reset();
                text.reset();
                flag.reset();
                ratio.reset();
                inner.reset();
                assertTrue(spec.load(), format);

                assertEquals(77, num.getAsInt(), format);
                assertEquals("hello world", text.get(), format);
                assertFalse(flag.getAsBoolean(), format);
                assertEquals(6.25, ratio.getAsDouble(), format);
                assertEquals(9, inner.getAsInt(), format);
            }
        }
    }
}
