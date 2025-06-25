package com.pink.family.assignment.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Hold common error messages for convenience.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorMessages {
        public static final String NO_RECORD = "No record found";
        public static final String NO_PARTNER = "Does not have a partner";
        public static final String NO_SHARED_CHILDREN = "No shared children";
        public static final String NO_UNDERAGE_CHILD = "Does not have a child under 18";
        public static final String NOT_EXACTLY_3_CHILDREN = "Does not have exactly 3 children";
        public static final String NO_DISTINCT_RECORD = "Could not find a single matching record";
    }
