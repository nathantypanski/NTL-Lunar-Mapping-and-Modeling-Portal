package com.topcoder.nasa.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Copy pasta from <a href=
 * "http://stackoverflow.com/questions/5722506/how-do-you-map-multiple-query-parameters-to-the-fields-of-a-bean-on-jersey-get-r"
 * >here</a>. Helps us bind a bunch of query params to a bean of or choosing without having to
 * manually call a bunch of getters and setters.
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchCriteriaParam {
}