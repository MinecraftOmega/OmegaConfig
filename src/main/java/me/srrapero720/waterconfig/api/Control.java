package me.srrapero720.waterconfig.api;

/**
 * Helper enum used to express a UI control preference for a config value.
 *
 * <p>It does not render anything by itself; it is only a hint that a universal config screen can read
 * (through {@link IConfigField#control()}) to pick the most appropriate widget for each value.</p>
 *
 * <p>Every field type already prefers a sensible default control and only accepts the controls that
 * make sense for it, so choosing one explicitly is only needed when you want to override that
 * preference with another valid control. Assigning a control a field does not support is rejected.</p>
 */
public enum Control {
    // ═══════════════════════════
    //  GENERIC — TEXT & FREE-FORM VALUES
    // ═══════════════════════════

    /** Sentinel: let the field decide. Resolves to the field type's preferred control. */
    DEFAULT,

    /** Read-only line of text, the value is shown but cannot be edited. */
    LABEL,

    /** Single-line text box where the user types freely. */
    INPUT,

    /** Multi-line, resizable text box for long or paragraph-like values. */
    TEXT_AREA,

    /** Single-line text box accompanied by a "paste" button that fills it from the clipboard. */
    INPUT_PASTE,

    /** Single-line text box that masks every character (••••) for secrets and tokens. */
    PASSWORD,

    /** Text box with a "browse…" button that opens a system file chooser. */
    INPUT_FILE,

    /** Text box with a "browse…" button that opens a system directory chooser. */
    INPUT_FOLDER,

    /** Small swatch that opens a color wheel / hex picker and previews the chosen color. */
    COLOR_PICKER,

    /** Button that captures a pressed key/combo and shows the bound key (e.g. "Ctrl + S"). */
    KEYBIND,

    // ═══════════════════════════
    //  STRUCTURE — NESTED GROUPS
    // ═══════════════════════════

    /** Expandable folder/section that groups and nests the controls under it. Used by config groups. */
    FOLDER,

    // ═══════════════════════════
    //  BOOLEAN — ON/OFF VALUES
    // ═══════════════════════════

    /** Sliding pill that animates between OFF (left) and ON (right). */
    SWITCH,

    /** Square box that draws a check mark when enabled and is empty when disabled. */
    CHECKBOX,

    // ═══════════════════════════
    //  SINGLE CHOICE — PICK ONE OPTION (ENUMS / CLOSED LISTS)
    // ═══════════════════════════

    /** Vertical group of circles where exactly one can be filled in at a time. */
    RADIO_BUTTON,

    /** Collapsed box that expands into a scrollable menu of options when clicked. */
    DROPDOWN,

    /** Single field flanked by ‹ › arrows that cycle through the available options. */
    LIST_SPINNER,

    /** Row of joined buttons (like tabs) where the selected one stays highlighted. */
    SEGMENTED,

    // ═══════════════════════════
    //  MULTI CHOICE / COLLECTIONS — PICK OR EDIT MANY VALUES
    // ═══════════════════════════

    /** Vertical list of checkboxes where several entries can be ticked at once. */
    CHECKBOX_LIST,

    /** Text box that converts each typed entry into a removable chip/tag. */
    TAG_INPUT,

    /** Editable list of rows with add, remove and reorder controls. */
    LIST_EDITOR,

    // ═══════════════════════════
    //  NUMBER — NUMERIC VALUES
    // ═══════════════════════════

    /** Horizontal track with a handle dragged between a minimum and maximum. */
    SEEKBAR,

    /** Number box with tiny up/down arrows that step the value. */
    NUMBER_SPINNER,

    /** Slider with two handles that select a [min, max] span. */
    RANGE_SLIDER,

    /** Rotary dial turned clockwise/counter-clockwise to change the value. */
    KNOB,
}
