package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.IConfigField;
import me.srrapero720.waterconfig.impl.fields.BooleanField;
import me.srrapero720.waterconfig.impl.fields.DoubleField;
import me.srrapero720.waterconfig.impl.fields.IntField;
import me.srrapero720.waterconfig.impl.fields.ListField;
import me.srrapero720.waterconfig.impl.fields.LongField;
import me.srrapero720.waterconfig.impl.fields.StringField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers reverse parsing: building a runtime spec from a config file without a class,
 * type inference by value shape, comment round-trip and the strict-only parse rule.
 */
public class ReverseSpecTest {

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

    @Test
    void buildsSpecWithInferredTypes() throws IOException {
        Path file = write("inferred.json5", """
                {
                    flag: true,
                    count: 42,
                    big: 5000000000,
                    ratio: 0.5,
                    name: "water",
                    nums: [1, 2, 3],
                    words: ["a", "b"],
                    mixed: [1, "x"],
                    nested: {
                        inner: 7
                    }
                }
                """);

        ConfigSpec spec = WaterConfig.reverseSpec(file);
        assertNotNull(spec);
        assertEquals("inferred", spec.name(), "the spec takes the file stem as synthetic name");
        assertEquals(file, spec.path(), "save() must write back to the same file");

        // TYPE INFERENCE BY VALUE SHAPE
        assertInstanceOf(BooleanField.class, spec.findField("flag"));
        assertInstanceOf(IntField.class, spec.findField("count"));
        assertInstanceOf(LongField.class, spec.findField("big"));
        assertInstanceOf(DoubleField.class, spec.findField("ratio"));
        assertInstanceOf(StringField.class, spec.findField("name"));
        assertInstanceOf(IntField.class, spec.findField("nested.inner"), "dotted keys split into groups");

        // PARSED VALUES ARE THE FIELD DEFAULTS
        assertTrue(((BooleanField) spec.findField("flag")).getAsBoolean());
        assertEquals(42, ((IntField) spec.findField("count")).getAsInt());
        assertEquals(5_000_000_000L, ((LongField) spec.findField("big")).getAsLong());
        assertEquals(0.5, ((DoubleField) spec.findField("ratio")).getAsDouble());
        assertEquals("water", spec.findField("name").get());

        // LISTS INFER THE NARROWEST COMMON ELEMENT TYPE
        ListField<?> nums = (ListField<?>) spec.findField("nums");
        assertEquals(Integer.class, nums.subType());
        assertEquals(List.of(1, 2, 3), nums.get());
        assertEquals(String.class, ((ListField<?>) spec.findField("words")).subType());
        assertEquals(String.class, ((ListField<?>) spec.findField("mixed")).subType(), "mixed element kinds widen to String");

        // NOT WORKER-MANAGED, NOTHING PENDING
        assertFalse(spec.isDirty());
        assertTrue(spec.isLoaded());
    }

    @Test
    void editAndSaveBackRoundTrips() throws IOException {
        Path file = write("editable.cfg", """
                {
                  # HOW MANY UNITS
                  count: 10
                  label: "old"
                }
                """);

        ConfigSpec spec = WaterConfig.reverseSpec(file);
        assertNotNull(spec);

        IConfigField<?, ?> count = spec.findField("count");
        ((IntField) count).set(99);
        spec.save();

        // THE EDIT LANDS IN THE ORIGINAL FILE, THE COMMENT SURVIVES THE ROUND-TRIP
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("count: 99"), "edited value must be saved back: " + content);
        assertTrue(content.contains("# HOW MANY UNITS"), "human comments must round-trip: " + content);

        ConfigSpec reloaded = WaterConfig.reverseSpec(file);
        assertNotNull(reloaded);
        assertEquals(99, ((IntField) reloaded.findField("count")).getAsInt());
        assertEquals("old", reloaded.findField("label").get());
    }

    @Test
    void brokenFilesReturnNullNeverGuess() throws IOException {
        // STRICT-ONLY RULE: WITHOUT A SPEC THERE IS NOTHING TO VALIDATE A REPAIR AGAINST
        Path broken = write("broken.json", "{\"a\": 1");
        assertNull(WaterConfig.reverseSpec(broken));

        Path garbage = write("garbage.cfg", "%%% not a config");
        assertNull(WaterConfig.reverseSpec(garbage));
    }

    @Test
    void unknownExtensionsReturnNull() throws IOException {
        Path unknown = write("data.bin", "whatever");
        assertNull(WaterConfig.reverseSpec(unknown));

        Path noExtension = tempDir.resolve("noext");
        Files.writeString(noExtension, "x=1");
        assertNull(WaterConfig.reverseSpec(noExtension));
    }

    @Test
    void fileOverloadDelegates() throws IOException {
        Path file = write("overload.properties", "count=5\n");
        ConfigSpec spec = WaterConfig.reverseSpec(file.toFile());
        assertNotNull(spec);
        assertEquals(5, ((IntField) spec.findField("count")).getAsInt());
    }

    @Test
    void tomlCommentsAndTablesRoundTrip() throws IOException {
        Path file = write("tables.toml", """
                # TOP LEVEL NOTE
                speed = 1.5

                [engine]
                # CYLINDER COUNT
                cylinders = 8
                """);

        ConfigSpec spec = WaterConfig.reverseSpec(file);
        assertNotNull(spec);
        assertEquals(1.5, ((DoubleField) spec.findField("speed")).getAsDouble());
        assertEquals(8, ((IntField) spec.findField("engine.cylinders")).getAsInt());

        String[] comments = spec.findField("engine.cylinders").comments();
        assertEquals(1, comments.length);
        assertEquals("CYLINDER COUNT", comments[0]);
    }
}
