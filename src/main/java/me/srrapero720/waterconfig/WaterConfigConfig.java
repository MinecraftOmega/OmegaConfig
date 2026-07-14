package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.Comment;
import me.srrapero720.waterconfig.api.annotations.Spec;

/**
 * WaterConfig's own configuration, managed by WaterConfig itself (dogfooding). Read once at
 * bootstrap; it governs cross-cutting behavior like config-file lockdown.
 */
@Spec(value = "waterconfig", format = WaterConfig.FORMAT_TOML, backups = 0)
public class WaterConfigConfig {

    @Spec.Field
    @Comment({
            "File lockdown protects config files from external edits taking effect at runtime.",
            "OFF: files can be hot-edited and reloaded while running (default).",
            "I_READ_THE_COMMENT_SOFT: runtime reload is disabled — external edits are ignored and",
            "  overwritten on the next save, so hot-editing no longer takes effect.",
            "I_READ_THE_COMMENT_HARD: not implemented yet (falls back to SOFT); will additionally hold an",
            "  OS lock so external programs cannot write the files at all.",
            "The values are verbose on purpose: turning lockdown on must be a deliberate, informed choice."
    })
    public static Lockdown lockdownConfigFiles = Lockdown.OFF;

    public enum Lockdown {
        OFF,
        I_READ_THE_COMMENT_SOFT,
        I_READ_THE_COMMENT_HARD
    }
}
