package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.Control;
import me.srrapero720.waterconfig.api.IConfigField;
import me.srrapero720.waterconfig.api.annotations.Spec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ControlsTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() {
        WaterConfig.init();
        WaterConfig.setPath(tempDir);
    }

    enum Sample { A, B, C }

    // ========================================================================
    // BUILDER API
    // ========================================================================
    @Nested
    class BuilderControlsTest {

        @Test
        void unsetControlResolvesToTypeDefault() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_unset", "cfg", "", 0);
            b.defineBoolean("flag", true).end();
            b.defineInt("count", 0).end();
            b.defineDouble("ratio", 0.0).end();
            b.defineString("text", "x").end();
            b.defineChar("c", 'a').end();
            b.defineEnum("e", Sample.A).end();
            b.defineList("l", new ArrayList<>(List.of("x")), String.class).end();
            b.definePath("p", Paths.get("a")).end();
            ConfigSpec spec = b.build();

            assertEquals(Control.SWITCH, spec.findField("flag").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("count").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("ratio").control());
            assertEquals(Control.INPUT, spec.findField("text").control());
            assertEquals(Control.INPUT, spec.findField("c").control());
            assertEquals(Control.DROPDOWN, spec.findField("e").control());
            assertEquals(Control.LIST_EDITOR, spec.findField("l").control());
            assertEquals(Control.INPUT_FILE, spec.findField("p").control());
        }

        @Test
        void explicitDefaultResolvesToTypeDefault() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_default", "cfg", "", 0);
            b.defineBoolean("flag", true).control(Control.DEFAULT).end();
            b.defineInt("count", 0).control(Control.DEFAULT).end();
            b.defineEnum("e", Sample.A).control(Control.DEFAULT).end();
            ConfigSpec spec = b.build();

            assertEquals(Control.SWITCH, spec.findField("flag").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("count").control());
            assertEquals(Control.DROPDOWN, spec.findField("e").control());
        }

        @Test
        void resolvedControlIsNeverDefaultSentinel() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_never_default", "cfg", "", 0);
            b.defineString("text", "x").end();
            b.defineInt("count", 0).end();
            ConfigSpec spec = b.build();

            assertNotEquals(Control.DEFAULT, spec.findField("text").control());
            assertNotEquals(Control.DEFAULT, spec.findField("count").control());
        }

        @Test
        void validControlsAreApplied() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_valid", "cfg", "", 0);
            b.defineBoolean("flag", true).control(Control.CHECKBOX).end();
            b.defineInt("count", 0).control(Control.SEEKBAR).end();
            b.defineDouble("ratio", 0.0).control(Control.KNOB).end();
            b.defineEnum("e", Sample.A).control(Control.RADIO_BUTTON).end();
            b.defineList("l", new ArrayList<>(List.of("x")), String.class).control(Control.TAG_INPUT).end();
            b.definePath("p", Paths.get("a")).control(Control.INPUT_FOLDER).end();
            ConfigSpec spec = b.build();

            assertEquals(Control.CHECKBOX, spec.findField("flag").control());
            assertEquals(Control.SEEKBAR, spec.findField("count").control());
            assertEquals(Control.KNOB, spec.findField("ratio").control());
            assertEquals(Control.RADIO_BUTTON, spec.findField("e").control());
            assertEquals(Control.TAG_INPUT, spec.findField("l").control());
            assertEquals(Control.INPUT_FOLDER, spec.findField("p").control());
        }

        @Test
        void incoherentControlThrows() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_invalid", "cfg", "", 0);

            assertThrows(IllegalArgumentException.class,
                    () -> b.defineBoolean("bad_flag", true).control(Control.SEEKBAR).end(),
                    "Boolean field must reject SEEKBAR");
            assertThrows(IllegalArgumentException.class,
                    () -> b.defineInt("bad_count", 0).control(Control.SWITCH).end(),
                    "Numeric field must reject SWITCH");
            assertThrows(IllegalArgumentException.class,
                    () -> b.defineEnum("bad_enum", Sample.A).control(Control.SWITCH).end(),
                    "Enum field must reject SWITCH");
            assertThrows(IllegalArgumentException.class,
                    () -> b.defineList("bad_list", new ArrayList<>(List.of("x")), String.class).control(Control.SWITCH).end(),
                    "Collection field must reject SWITCH");
            assertThrows(IllegalArgumentException.class,
                    () -> b.definePath("bad_path", Paths.get("a")).control(Control.SWITCH).end(),
                    "Path field must reject SWITCH");
        }

        @Test
        void groupReportsFolderControl() {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("ctrl_b_group", "cfg", "", 0);
            b.push("section");
            b.defineInt("x", 0).end();
            b.pop();
            ConfigSpec spec = b.build();

            IConfigField<?, ?> group = spec.findField("section");
            assertNotNull(group, "Group should be reachable via findField");
            assertEquals(Control.FOLDER, group.control(), "Groups must report FOLDER as control");
        }
    }

    // ========================================================================
    // ANNOTATION API
    // ========================================================================
    @Nested
    class AnnotatedControlsTest {

        @Spec(value = "ctrl_a_unset", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class Defaults {
            @Spec.Field public static boolean flag = true;
            @Spec.Field public static int count = 0;
            @Spec.Field public static double ratio = 0.0;
            @Spec.Field public static String text = "x";
            @Spec.Field public static Sample e = Sample.A;
        }

        @Spec(value = "ctrl_a_valid", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class Valids {
            @Spec.Field(control = Control.CHECKBOX) public static boolean flag = true;
            @Spec.Field(control = Control.SEEKBAR) public static int count = 0;
            @Spec.Field(control = Control.KNOB) public static double ratio = 0.0;
            @Spec.Field(control = Control.RADIO_BUTTON) public static Sample e = Sample.A;
            @Spec.Field(control = Control.TEXT_AREA) public static String text = "x";
        }

        @Spec(value = "ctrl_a_default", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class ExplicitDefault {
            @Spec.Field(control = Control.DEFAULT) public static boolean flag = true;
            @Spec.Field(control = Control.DEFAULT) public static int count = 0;
        }

        @Spec(value = "ctrl_a_invalid_bool", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class InvalidBool {
            @Spec.Field(control = Control.SEEKBAR) public static boolean flag = true;
        }

        @Spec(value = "ctrl_a_invalid_int", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class InvalidInt {
            @Spec.Field(control = Control.SWITCH) public static int count = 0;
        }

        @Spec(value = "ctrl_a_invalid_enum", format = WaterConfig.FORMAT_CFG, backups = 0)
        static class InvalidEnum {
            @Spec.Field(control = Control.NUMBER_SPINNER) public static Sample e = Sample.A;
        }

        @Test
        void unsetAnnotationResolvesToTypeDefault() {
            ConfigSpec spec = WaterConfig.register(Defaults.class);
            assertEquals(Control.SWITCH, spec.findField("flag").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("count").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("ratio").control());
            assertEquals(Control.INPUT, spec.findField("text").control());
            assertEquals(Control.DROPDOWN, spec.findField("e").control());
        }

        @Test
        void validAnnotationControlsAreApplied() {
            ConfigSpec spec = WaterConfig.register(Valids.class);
            assertEquals(Control.CHECKBOX, spec.findField("flag").control());
            assertEquals(Control.SEEKBAR, spec.findField("count").control());
            assertEquals(Control.KNOB, spec.findField("ratio").control());
            assertEquals(Control.RADIO_BUTTON, spec.findField("e").control());
            assertEquals(Control.TEXT_AREA, spec.findField("text").control());
        }

        @Test
        void explicitDefaultAnnotationResolvesToTypeDefault() {
            ConfigSpec spec = WaterConfig.register(ExplicitDefault.class);
            assertEquals(Control.SWITCH, spec.findField("flag").control());
            assertEquals(Control.NUMBER_SPINNER, spec.findField("count").control());
        }

        @Test
        void incoherentBooleanAnnotationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> WaterConfig.register(InvalidBool.class),
                    "Boolean field annotated with SEEKBAR must fail registration");
        }

        @Test
        void incoherentIntAnnotationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> WaterConfig.register(InvalidInt.class),
                    "Int field annotated with SWITCH must fail registration");
        }

        @Test
        void incoherentEnumAnnotationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> WaterConfig.register(InvalidEnum.class),
                    "Enum field annotated with NUMBER_SPINNER must fail registration");
        }
    }
}
