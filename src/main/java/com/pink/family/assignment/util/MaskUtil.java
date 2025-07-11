package com.pink.family.assignment.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

/**
 * Utility class used to mask values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskUtil {

    public static String fullyMask(String input) {
        if (input == null) {
            return null;
        }
        return "*".repeat(input.length());
    }

    /**
     * Mask te External Id value revealing only the first and last two numbers
     */
    public static String maskExternalId(Long input) {
        return ObjectUtils.isEmpty(input) ?
            "N/A" :
            mask(String.valueOf(input), 2, 2, 4);
    }

    /**
     * Mask a given value, specifying how many character should be revealed at the start and end.
     *
     * @param input
     * value to mask.
     * @param frontReveal
     * number of characters to reveal at the start.
     * @param endReveal
     * number of characters to reveal at the end.
     * @param minLengthToReveal
     * The minimum length that values can be revealed for.
     * @return
     * The masked value. Null if input is null.
     */
    public static String mask(String input, int frontReveal, int endReveal, int minLengthToReveal) {
        if (input == null) return null;
        int length = input.length();
        int revealLength = frontReveal + endReveal;

        if (length < minLengthToReveal || length <= revealLength) {
            return fullyMask(input);
        }

        return
            input.substring(0, frontReveal) +
            "*".repeat(length - revealLength) +
            input.substring(length - endReveal);
    }
}
