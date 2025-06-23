package com.pink.family.assignment.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MaskUtilTest {

    /**
     * method source for mask method.
     */
    private static Stream<Arguments> maskTestSource() {
        return Stream.of(
            Arguments.of("Null input", null, null, 1, 1, 1),
            Arguments.of("Empty input", "", "", 1, 1, 1),
            Arguments.of("Shorter than minLengthToReveal", "hello", "*****", 2, 2, 10),
            Arguments.of("Length equals minLengthToReveal but too short to reveal", "abc", "***", 2, 2, 3),
            Arguments.of("Length equals front + rear, should be fully masked", "abcdef", "******", 3, 3, 1),
            Arguments.of("Both sides revealed", "hellothereworld", "hel*********rld", 3, 3, 5),
            Arguments.of("Only front reveal", "helloworld12", "he**********", 2, 0, 5),
            Arguments.of("Only rear reveal", "helloworld12", "**********12", 0, 2, 5),
            Arguments.of("Edge case: front + rear = length - 1", "abcdefgh", "abc**fgh", 3, 3, 5),
            Arguments.of("Just long enough to reveal", "shopping", "sho**ing", 3, 3, 8),
            Arguments.of("Reveal more than string length – should fully mask", "short", "*****", 3, 3, 1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maskTestSource")
    void test_mask(String testName, String input, String expected, int front, int rear, int min) {
        assertEquals(MaskUtil.mask(input, front, rear, min), expected);
    }

    private static Stream<Arguments> maskBsnTestSource() {
        return Stream.of(
            Arguments.of("1234567890", "12******90"),
            Arguments.of("1234567890", "12******90"),
            Arguments.of("9876543210", "98******10"),
            Arguments.of("1122334455", "11******55"),
            Arguments.of("5566778899", "55******99"),
            Arguments.of("1029384756", "10******56"),
            Arguments.of("6677889900", "66******00"),
            Arguments.of("1231231231", "12******31"),
            Arguments.of("9081726354", "90******54"),
            Arguments.of("4455667788", "44******88"),
            Arguments.of("8765432109", "87******09")
        );
    }

    @ParameterizedTest(name = "maskBsn({0}) = {1}")
    @MethodSource("maskBsnTestSource")
    void test_maskBsn(String input, String expected) {
        assertEquals(expected, MaskUtil.maskBsn(input));
    }

}