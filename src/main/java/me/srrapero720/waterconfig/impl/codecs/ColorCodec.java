package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.awt.Color;

/**
 * Codec for {@link Color}. As a plain codec it reads any supported hex form and writes the
 * shortest lossless hex (AUTO). The static helpers back {@code ColorField}, which picks the
 * hex-vs-split representation and alpha handling from its {@code @ColorConditions} radix.
 */
public class ColorCodec implements ICodec<Color> {
    private static final String[] RGB = {"r", "g", "b"};
    private static final String[] RGBA = {"r", "g", "b", "a"};

    @Override
    public String encode(Color instance) {
        // AUTO: KEEP ALPHA ONLY WHEN THE COLOR IS NOT OPAQUE
        return hex(instance, instance.getAlpha() != 255);
    }

    @Override
    public Color decode(String value) {
        return parseHex(value);
    }

    @Override
    public Class<Color> type() {
        return Color.class;
    }

    // COLUMN NAMES FOR THE SPLIT (MATRIX) FORM
    public static String[] columns(boolean alpha) {
        return alpha ? RGBA : RGB;
    }

    // SHORTEST LOSSLESS HEX: 3/4-DIGIT WHEN EVERY CHANNEL IS NIBBLE-COLLAPSIBLE, ELSE 6/8-DIGIT
    public static String hex(Color c, boolean alpha) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        boolean shortForm = collapsible(r) && collapsible(g) && collapsible(b) && (!alpha || collapsible(a));
        StringBuilder sb = new StringBuilder(9).append('#');
        if (shortForm) {
            sb.append(nibble(r)).append(nibble(g)).append(nibble(b));
            if (alpha) sb.append(nibble(a));
        } else {
            sb.append(two(r)).append(two(g)).append(two(b));
            if (alpha) sb.append(two(a));
        }
        return sb.toString();
    }

    // PARSES #RGB / #RRGGBB / #RGBA / #RRGGBBAA (LEADING # OPTIONAL); NULL WHEN MALFORMED
    public static Color parseHex(String value) {
        String v = value.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        try {
            return switch (v.length()) {
                case 3 -> new Color(nib(v, 0), nib(v, 1), nib(v, 2), 255);
                case 4 -> new Color(nib(v, 0), nib(v, 1), nib(v, 2), nib(v, 3));
                case 6 -> new Color(oct(v, 0), oct(v, 2), oct(v, 4), 255);
                case 8 -> new Color(oct(v, 0), oct(v, 2), oct(v, 4), oct(v, 6));
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // BYTE CHANNELS AS DECIMAL STRINGS FOR THE SPLIT FORM
    public static String[] split(Color c, boolean alpha) {
        return alpha
                ? new String[]{str(c.getRed()), str(c.getGreen()), str(c.getBlue()), str(c.getAlpha())}
                : new String[]{str(c.getRed()), str(c.getGreen()), str(c.getBlue())};
    }

    // REBUILDS A COLOR FROM DECIMAL r,g,b(,a) CELLS; NULL WHEN ANY CELL IS MISSING OR INVALID
    public static Color merge(String[] cells) {
        try {
            int r = clamp(Integer.parseInt(cells[0].trim()));
            int g = clamp(Integer.parseInt(cells[1].trim()));
            int b = clamp(Integer.parseInt(cells[2].trim()));
            int a = (cells.length > 3 && cells[3] != null) ? clamp(Integer.parseInt(cells[3].trim())) : 255;
            return new Color(r, g, b, a);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean collapsible(int v) {
        return (v >> 4) == (v & 0xF);
    }

    private static char nibble(int v) {
        return Character.forDigit(v & 0xF, 16);
    }

    private static String two(int v) {
        return String.format("%02x", v);
    }

    private static int nib(String v, int i) {
        int d = Character.digit(v.charAt(i), 16);
        if (d < 0) throw new NumberFormatException("Not a hex digit: " + v.charAt(i));
        return d * 17; // 0xN -> 0xNN
    }

    private static int oct(String v, int i) {
        return Integer.parseInt(v.substring(i, i + 2), 16);
    }

    private static String str(int v) {
        return Integer.toString(v);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
