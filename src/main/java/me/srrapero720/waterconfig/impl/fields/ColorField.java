package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;
import me.srrapero720.waterconfig.api.IStructuredField;
import me.srrapero720.waterconfig.api.annotations.ColorConditions;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.impl.codecs.ColorCodec;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * A {@link Color} field that serializes itself per its {@code @ColorConditions} radix: hex string
 * for AUTO/OPAQUE/ALPHA, or a {@code { r, g, b(, a) }} group for the byte-split radixes. On load it
 * accepts whichever form the file holds, so the radix can change without breaking old files.
 */
public final class ColorField extends BaseConfigField<Color, Void> implements IStructuredField {
    public final ColorConditions.Radix radix;

    public ColorField(String name, ConfigGroup group, Set<String> comments, ColorConditions.Radix radix, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, resolve(control), suffix);
        this.radix = radix;
    }

    public ColorField(String name, ConfigGroup group, Set<String> comments, ColorConditions.Radix radix, Color defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, resolve(control), suffix);
        this.radix = radix;
    }

    // COLOR FIELDS PREFER THE COLOR PICKER WHEN NO CONTROL WAS CHOSEN
    private static Control resolve(Control control) {
        return (control == null || control == Control.DEFAULT) ? Control.COLOR_PICKER : control;
    }

    @Override
    public Class<Color> type() {
        return Color.class;
    }

    @Override
    public void validate() {
        // ANY COLOR IS VALID
    }

    @Override
    public void writeSelf(IFormatWriter writer) {
        Color c = this.get();
        if (c == null) {
            c = this.defaultValue;
        }
        switch (this.radix) {
            case BYTE_SPLIT -> writer.writeMatrix(this.name(), ColorCodec.columns(false), ColorCodec.split(c, false));
            case BYTE_SPLIT_ALPHA -> writer.writeMatrix(this.name(), ColorCodec.columns(true), ColorCodec.split(c, true));
            case OPAQUE -> writer.write(this.name(), ColorCodec.hex(c, false), Color.class, null);
            case ALPHA -> writer.write(this.name(), ColorCodec.hex(c, true), Color.class, null);
            case AUTO -> writer.write(this.name(), ColorCodec.hex(c, c.getAlpha() != 255), Color.class, null);
        }
    }

    @Override
    public void readSelf(IFormatReader reader) {
        // A HEX SCALAR WINS WHEN PRESENT (COVERS AUTO/OPAQUE/ALPHA AND ANY OLD HEX FILE)
        String hex = reader.read(this.name());
        for (int a = 0; hex == null && a < this.aliases().length; a++) {
            hex = reader.read(this.aliases()[a]);
        }
        if (hex != null) {
            this.set0(ColorCodec.parseHex(hex));
            return;
        }

        // OTHERWISE TRY THE SPLIT GROUP; READING 4 COLUMNS ALSO COVERS THE 3-COLUMN (NO ALPHA) FORM
        String[] cells = reader.readMatrix(this.name(), ColorCodec.columns(true));
        for (int a = 0; cells == null && a < this.aliases().length; a++) {
            cells = reader.readMatrix(this.aliases()[a], ColorCodec.columns(true));
        }
        if (cells != null) {
            this.set0(ColorCodec.merge(cells));
        }
        // NEITHER FORM PRESENT: KEEP THE CURRENT VALUE, LIKE A MISSING SCALAR
    }
}
