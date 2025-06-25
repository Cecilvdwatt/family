package com.pink.family.assignment.constants;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Keys used by micrometer
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeterKeys {
    public static final String TIME_CHECK_PARTNER_CHILDREN_FALLBACK = "CHECK.PARTNER.CHILDREN.FALLBACK.TIME";
    public static final String COUNT_CHECK_PARTNER_CHILDREN_FALLBACK = "CHECK.PARTNER.CHILDREN.FALLBACK.COUNT";
    public static final String TIME_CHECK_PARTNER_CHILDREN_ID = "CHECK.PARTNER.CHILDREN.ID.TIME";
    public static final String COUNT_CHECK_PARTNER_CHILDREN_ID = "CHECK.PARTNER.CHILDREN.ID.COUNT";
    public static final String TIME_RETRIEVE_AND_UPDATE = "RETRIEVE.UPDATE.TIME";
    public static final String COUNT_RETRIEVE_AND_UPDATE = "RETRIEVE.UPDATE.COUNT";
}
