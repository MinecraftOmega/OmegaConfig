package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.TimeConditions;
import me.srrapero720.waterconfig.impl.codecs.*;
import me.srrapero720.waterconfig.impl.fields.BaseConfigField;
import me.srrapero720.waterconfig.impl.fields.TemporalField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Tier 1 / Tier 2 codecs (temporal, big numbers, pattern, locale, charset, zone, url) and
 * the {@code @TimeConditions} from/to bounds.
 */
public class NewCodecsTest {

    @TempDir
    static java.nio.file.Path tempDir;

    @BeforeAll
    static void setup() {
        WaterConfig.init();
        WaterConfig.setPath(tempDir);
    }

    @AfterAll
    static void teardown() {
        WaterConfig.unloadAll();
    }

    // ========================================================================
    // DIRECT CODEC ROUND-TRIPS
    // ========================================================================
    @Nested
    class CodecRoundTrips {

        @Test
        void temporalTypes() {
            assertEquals(Duration.ofSeconds(90), new DurationCodec().decode(new DurationCodec().encode(Duration.ofSeconds(90))));
            assertEquals(Period.of(1, 2, 3), new PeriodCodec().decode("P1Y2M3D"));
            Instant now = Instant.parse("2024-01-15T10:30:00Z");
            assertEquals(now, new InstantCodec().decode(new InstantCodec().encode(now)));
            assertEquals(LocalDate.of(2024, 1, 15), new LocalDateCodec().decode("2024-01-15"));
            assertEquals(LocalTime.of(10, 30, 15), new LocalTimeCodec().decode("10:30:15"));
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30), new LocalDateTimeCodec().decode("2024-01-15T10:30"));
            OffsetDateTime odt = OffsetDateTime.parse("2024-01-15T10:30+02:00");
            assertEquals(odt, new OffsetDateTimeCodec().decode(new OffsetDateTimeCodec().encode(odt)));
            ZonedDateTime zdt = ZonedDateTime.parse("2024-01-15T10:30+01:00[Europe/Paris]");
            assertEquals(zdt, new ZonedDateTimeCodec().decode(new ZonedDateTimeCodec().encode(zdt)));
            assertEquals(ZoneId.of("America/Mexico_City"), new ZoneIdCodec().decode("America/Mexico_City"));
        }

        @Test
        void bigNumbers() {
            BigInteger big = new BigInteger("123456789012345678901234567890");
            assertEquals(big, new BigIntegerCodec().decode(new BigIntegerCodec().encode(big)));
            BigDecimal dec = new BigDecimal("3.14159265358979323846");
            assertEquals(dec, new BigDecimalCodec().decode(new BigDecimalCodec().encode(dec)));
            assertEquals(new BigInteger("-42"), new BigIntegerCodec().decode("-42"));
        }

        @Test
        void tier2Types() {
            assertEquals("a.*b", new PatternCodec().decode("a.*b").pattern());
            assertEquals("x+", new PatternCodec().encode(Pattern.compile("x+")));
            assertEquals(Locale.US, new LocaleCodec().decode("en-US"));
            assertEquals("en-US", new LocaleCodec().encode(Locale.US));
            assertEquals(StandardCharsets.UTF_8, new CharsetCodec().decode("UTF-8"));
            assertEquals("UTF-8", new CharsetCodec().encode(StandardCharsets.UTF_8));
            assertEquals("https://example.com/path?q=1", new URLCodec().decode("https://example.com/path?q=1").toString());
        }

        @Test
        void malformedInputDecodesToNull() {
            assertNull(new DurationCodec().decode("not-a-duration"));
            assertNull(new InstantCodec().decode("2024-99-99"));
            assertNull(new ZoneIdCodec().decode("Nowhere/Nope"));
            assertNull(new BigIntegerCodec().decode("12.5"));
            assertNull(new CharsetCodec().decode("FAKE-9000"));
            assertNull(new PatternCodec().decode("[unclosed"));
            assertNull(new URLCodec().decode("not a url"));
        }
    }

    // ========================================================================
    // FIELD ROUND-TRIP THROUGH THE FORMATS
    // ========================================================================
    @Nested
    class FieldRoundTrip {

        @ParameterizedTest
        @ValueSource(strings = {"properties", "cfg", "json", "json5", "toml"})
        @SuppressWarnings("unchecked")
        void temporalAndBigDecimalRoundTrip(String fmt) throws IOException {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("codec_rt_" + fmt, fmt, "", 0);
            BaseConfigField<Duration, Object> dur = (BaseConfigField<Duration, Object>) b.<Duration, Object>define("dur", Duration.ofSeconds(30), Duration.class, null).end();
            BaseConfigField<Instant, Object> inst = (BaseConfigField<Instant, Object>) b.<Instant, Object>define("inst", Instant.EPOCH, Instant.class, null).end();
            BaseConfigField<BigDecimal, Object> dec = (BaseConfigField<BigDecimal, Object>) b.<BigDecimal, Object>define("dec", BigDecimal.ZERO, BigDecimal.class, null).end();
            ConfigSpec spec = b.build();

            dur.set(Duration.ofMinutes(5));
            inst.set(Instant.parse("2024-06-01T12:00:00Z"));
            dec.set(new BigDecimal("9.99"));
            spec.save();
            dur.set(Duration.ZERO);
            inst.set(Instant.EPOCH);
            dec.set(BigDecimal.ONE);

            assertTrue(spec.load());
            assertEquals(Duration.ofMinutes(5), dur.get(), fmt);
            assertEquals(Instant.parse("2024-06-01T12:00:00Z"), inst.get(), fmt);
            assertEquals(new BigDecimal("9.99"), dec.get(), fmt);
        }
    }

    // ========================================================================
    // @TimeConditions from/to BOUNDS
    // ========================================================================
    @Nested
    class TimeBounds {

        private ConfigSpec buildBounded(TemporalField<Duration>[] out) {
            ConfigSpec.SpecBuilder b = new ConfigSpec.SpecBuilder("time_bounds", "cfg", "", 0);
            out[0] = b.defineTemporal("timeout", Duration.ofSeconds(30), Duration.class)
                    .from(Duration.ofSeconds(10))
                    .to(Duration.ofSeconds(60))
                    .end();
            return b.build();
        }

        @SuppressWarnings("unchecked")
        private void loadValue(ConfigSpec spec, String iso) throws IOException {
            Files.writeString(spec.path(), "{\n  timeout: \"" + iso + "\"\n}\n", StandardCharsets.UTF_8);
            assertTrue(spec.load());
        }

        @Test
        void inRangeSurvives() throws IOException {
            @SuppressWarnings("unchecked")
            TemporalField<Duration>[] f = new TemporalField[1];
            ConfigSpec spec = buildBounded(f);
            loadValue(spec, "PT45S");
            assertEquals(Duration.ofSeconds(45), f[0].get());
        }

        @Test
        void belowFromResetsToDefault() throws IOException {
            @SuppressWarnings("unchecked")
            TemporalField<Duration>[] f = new TemporalField[1];
            ConfigSpec spec = buildBounded(f);
            loadValue(spec, "PT5S");
            assertEquals(Duration.ofSeconds(30), f[0].get(), "below the lower bound resets to default");
        }

        @Test
        void aboveToResetsToDefault() throws IOException {
            @SuppressWarnings("unchecked")
            TemporalField<Duration>[] f = new TemporalField[1];
            ConfigSpec spec = buildBounded(f);
            loadValue(spec, "PT2M");
            assertEquals(Duration.ofSeconds(30), f[0].get(), "above the upper bound resets to default");
        }
    }
}
