/*
 * Copyright (c) 2023 by Montant Limited. All rights reserved.
 *
 * This software is the confidential and proprietary property of
 * Montant Limited and may not be disclosed, copied or distributed
 * in any form without the express written permission of Montant Limited.
 */

package com.flash.sanitization.sanitizer.metrics.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SanitizerMeasure {

}
