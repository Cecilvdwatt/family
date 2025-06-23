package com.pink.family.assignment.util;

import org.springframework.util.ObjectUtils;

public class CompareUtil {

    public static <T> T compareReturnNewIfDifferent(T newObj, T oldObj) {
        return (!ObjectUtils.isEmpty(newObj) && !ObjectUtils.nullSafeEquals(newObj, oldObj))
            ? newObj
            : oldObj;
    }
}
