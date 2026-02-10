import org.junit.jupiter.api.Test;
import org.omegaconfig.MathEvaluator;

import static org.junit.jupiter.api.Assertions.*;

public class MathEvaluatorTest {

    // --- Basic arithmetic ---
    @Test void testAddition()       { assertEquals(5.0, MathEvaluator.evaluate("2 + 3")); }
    @Test void testSubtraction()    { assertEquals(1.0, MathEvaluator.evaluate("3 - 2")); }
    @Test void testMultiplication() { assertEquals(6.0, MathEvaluator.evaluate("2 * 3")); }
    @Test void testDivision()       { assertEquals(2.5, MathEvaluator.evaluate("5 / 2")); }

    // --- Operator precedence ---
    @Test void testPrecedenceMulOverAdd()   { assertEquals(14.0, MathEvaluator.evaluate("2 + 3 * 4")); }
    @Test void testPrecedenceDivOverSub()   { assertEquals(1.0, MathEvaluator.evaluate("3 - 4 / 2")); }
    @Test void testPrecedencePowerOverMul() { assertEquals(24.0, MathEvaluator.evaluate("3 * 2 ^ 3")); }

    // --- Power ---
    @Test void testSquare()                 { assertEquals(25.0, MathEvaluator.evaluate("5 ^ 2")); }
    @Test void testCube()                   { assertEquals(8.0, MathEvaluator.evaluate("2 ^ 3")); }
    @Test void testPowerRightAssociative()  { assertEquals(512.0, MathEvaluator.evaluate("2 ^ 3 ^ 2")); }

    // --- Sqrt (~) ---
    @Test void testSqrt()          { assertEquals(3.0, MathEvaluator.evaluate("~9")); }
    @Test void testSqrtChained()   { assertEquals(3.0, MathEvaluator.evaluate("~~81")); }
    @Test void testSqrtInExpr()    { assertEquals(7.0, MathEvaluator.evaluate("4 + ~9")); }
    @Test void testSqrtOfPower()   { assertEquals(5.0, MathEvaluator.evaluate("~25")); }

    // --- Unary minus ---
    @Test void testUnaryMinus()     { assertEquals(-5.0, MathEvaluator.evaluate("-5")); }
    @Test void testUnaryMinusExpr() { assertEquals(-1.0, MathEvaluator.evaluate("-3 + 2")); }
    @Test void testDoubleNeg()      { assertEquals(5.0, MathEvaluator.evaluate("--5")); }

    // --- No whitespace (JSON/JSON5 reader behavior) ---
    @Test void testNoSpaces()      { assertEquals(5.0, MathEvaluator.evaluate("2+3")); }
    @Test void testNoSpacesMixed() { assertEquals(14.0, MathEvaluator.evaluate("2+3*4")); }
    @Test void testNoSpacesPower() { assertEquals(25.0, MathEvaluator.evaluate("5^2")); }

    // --- Decimal numbers ---
    @Test void testDecimals()      { assertEquals(3.5, MathEvaluator.evaluate("1.5 + 2.0")); }
    @Test void testDecimalMul()    { assertEquals(4.5, MathEvaluator.evaluate("1.5 * 3")); }

    // --- Complex expressions ---
    @Test void testMultiOp()       { assertEquals(9.0, MathEvaluator.evaluate("2 + 3 * 4 - 5 / 1")); }
    @Test void testSqrtAndPower()  { assertEquals(10.0, MathEvaluator.evaluate("~64 + ~4")); }

    // --- Plain number passthrough (tryEvaluate) ---
    @Test void testPlainInt()       { assertEquals("42", MathEvaluator.tryEvaluate("42", false)); }
    @Test void testPlainDecimal()   { assertEquals("3.14", MathEvaluator.tryEvaluate("3.14", false)); }
    @Test void testPlainNegative()  { assertEquals("-5", MathEvaluator.tryEvaluate("-5", false)); }

    // --- Result formatting (tryEvaluate) ---
    @Test void testWholeResult()    { assertEquals("5", MathEvaluator.tryEvaluate("2 + 3", false)); }
    @Test void testFractionResult() { assertEquals("2.5", MathEvaluator.tryEvaluate("5 / 2", false)); }
    @Test void testPowerResult()    { assertEquals("25", MathEvaluator.tryEvaluate("5 ^ 2", false)); }

    // --- Error handling ---
    @Test void testStrictInvalid()     { assertThrows(IllegalArgumentException.class, () -> MathEvaluator.tryEvaluate("abc", true)); }
    @Test void testNonStrictInvalid()  { assertNull(MathEvaluator.tryEvaluate("abc", false)); }
    @Test void testStrictBadSyntax()   { assertThrows(IllegalArgumentException.class, () -> MathEvaluator.tryEvaluate("2 + + 3", true)); }
    @Test void testNonStrictBadSyntax(){ assertNull(MathEvaluator.tryEvaluate("2 + + 3", false)); }
    @Test void testEmptyStrict()       { assertThrows(IllegalArgumentException.class, () -> MathEvaluator.tryEvaluate("", true)); }
    @Test void testEmptyNonStrict()    { assertNull(MathEvaluator.tryEvaluate("", false)); }
    @Test void testNullNonStrict()     { assertNull(MathEvaluator.tryEvaluate(null, false)); }
}
