package org.ops4j.pax.cdi.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation defining a single property for OSGi service registration. To be used as
 * a member of {@link Properties} in combination with {@link OsgiServiceProvider}.
 * 
 * @author Harald Wellmann
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Property {

    /** Property name. */
    String name();

    /** Property value. */
    String value();
}
