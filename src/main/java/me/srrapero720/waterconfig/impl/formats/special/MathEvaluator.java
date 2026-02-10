package me.srrapero720.waterconfig.impl.formats.special;

/**
 * Evaluates simple math expressions with standard operator precedence.
 * <p>Supported operators:</p>
 * <ul>
 *   <li>{@code +} addition</li>
 *   <li>{@code -} subtraction (and unary negation)</li>
 *   <li>{@code *} multiplication</li>
 *   <li>{@code /} division</li>
 *   <li>{@code ^} power (right-associative)</li>
 *   <li>{@code ~} square root (unary prefix)</li>
 * </ul>
 *
 * <p>Precedence (highest to lowest): {@code ~}, unary {@code -}, {@code ^}, {@code * /}, {@code + -}</p>
 */
public final class MathEvaluator {

    private MathEvaluator() {}

    /**
     * Evaluates a math expression string and returns the result.
     * @param expr the expression to evaluate
     * @return the evaluated result
     * @throws IllegalArgumentException on invalid syntax or empty expression
     */
    public static double evaluate(String expr) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }
        Parser parser = new Parser(expr.trim());
        double result = parser.parseExpression();
        parser.skipWhitespace();
        if (parser.pos < parser.chars.length) {
            throw new IllegalArgumentException(
                    "Unexpected character '" + parser.chars[parser.pos] + "' at position " + parser.pos);
        }
        return result;
    }

    /**
     * Attempts to evaluate a value string as a math expression.
     * <p>If the value is a plain number (no operators), returns it unchanged.
     * If the value contains operators, evaluates and returns the result as a string.
     * Whole number results are formatted without decimal point ("5" not "5.0").</p>
     *
     * @param value  the raw value string from the config reader
     * @param strict if true, throws on invalid expression; if false, returns null
     * @return the evaluated result as a string, the original value if plain number, or null on error (non-strict)
     * @throws IllegalArgumentException if strict is true and expression is invalid
     */
    public static String tryEvaluate(String value, boolean strict) {
        if (value == null || value.isBlank()) {
            if (strict) {
                throw new IllegalArgumentException("Expression cannot be null or empty");
            }
            return null;
        }

        String trimmed = value.trim();

        try {
            double result = evaluate(trimmed);
            // If the result equals the input parsed as a plain number, return original string
            // to preserve formatting (e.g., "3.14" stays "3.14" not re-formatted)
            if (!containsOperators(trimmed)) {
                return trimmed;
            }
            return formatResult(result);
        } catch (IllegalArgumentException e) {
            if (strict) {
                throw e;
            }
            return null;
        }
    }

    private static boolean containsOperators(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+' || c == '*' || c == '/' || c == '^' || c == '~') return true;
            // '-' is an operator only when preceded by a digit (not a leading negative sign)
            if (c == '-' && i > 0 && (Character.isDigit(value.charAt(i - 1)) || value.charAt(i - 1) == '.')) return true;
        }
        return false;
    }

    private static String formatResult(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result) && !Double.isNaN(result)) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    private static class Parser {
        final char[] chars;
        int pos;

        Parser(String expr) {
            this.chars = expr.toCharArray();
            this.pos = 0;
        }

        // expression = additive
        double parseExpression() {
            return parseAdditive();
        }

        // additive = multiplicative (('+' | '-') multiplicative)*
        double parseAdditive() {
            double left = parseMultiplicative();
            while (true) {
                skipWhitespace();
                if (pos >= chars.length) break;
                char op = chars[pos];
                if (op != '+' && op != '-') break;
                pos++;
                double right = parseMultiplicative();
                left = (op == '+') ? left + right : left - right;
            }
            return left;
        }

        // multiplicative = power (('*' | '/') power)*
        double parseMultiplicative() {
            double left = parsePower();
            while (true) {
                skipWhitespace();
                if (pos >= chars.length) break;
                char op = chars[pos];
                if (op != '*' && op != '/') break;
                pos++;
                double right = parsePower();
                left = (op == '*') ? left * right : left / right;
            }
            return left;
        }

        // power = unary ('^' unary)*  (right-associative)
        double parsePower() {
            double base = parseUnary();
            skipWhitespace();
            if (pos < chars.length && chars[pos] == '^') {
                pos++;
                double exponent = parsePower(); // right-associative recursion
                return Math.pow(base, exponent);
            }
            return base;
        }

        // unary = '~' unary | '-' unary | number
        double parseUnary() {
            skipWhitespace();
            if (pos < chars.length) {
                if (chars[pos] == '~') {
                    pos++;
                    return Math.sqrt(parseUnary());
                }
                if (chars[pos] == '-') {
                    pos++;
                    return -parseUnary();
                }
            }
            return parseNumber();
        }

        // number = [0-9]+ ('.' [0-9]+)?
        double parseNumber() {
            skipWhitespace();
            int start = pos;
            while (pos < chars.length && Character.isDigit(chars[pos])) pos++;
            if (pos < chars.length && chars[pos] == '.') {
                pos++;
                while (pos < chars.length && Character.isDigit(chars[pos])) pos++;
            }
            if (pos == start) {
                String context = pos < chars.length
                        ? "but found '" + chars[pos] + "'"
                        : "but reached end of expression";
                throw new IllegalArgumentException("Expected number at position " + pos + " " + context);
            }
            return Double.parseDouble(new String(chars, start, pos - start));
        }

        void skipWhitespace() {
            while (pos < chars.length && Character.isWhitespace(chars[pos])) pos++;
        }
    }
}
