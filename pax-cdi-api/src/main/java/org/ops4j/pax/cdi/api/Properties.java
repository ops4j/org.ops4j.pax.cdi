package org.ops4j.pax.cdi.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be used in combination with {@link OsgiServiceProvider} to define the properties
 * used for service registration.
 * <p>
 * Note: This annotation is not a qualifer, since array-valued qualifier members have to be
 * non-binding, which would be of little use.
 * 
 * @author Harald Wellmann
 *
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Properties {

    /** List of properties for service registration. */
    Property[] value() default {};
}
