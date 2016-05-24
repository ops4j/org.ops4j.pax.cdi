/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.api2;

/**
 * Constants used in the PAX CDI API.
 *
 * @author Harald Wellmann
 *
 */
public class Constants {

    /**
     * Value of the {@link #EXTENDER_CAPABILITY}, denoting the PAX CDI extender.
     */
    public static final String CDI_EXTENDER = "pax.cdi";

    /**
     * Namespace of OSGi extender capability. In OSGi 5.0.0 or higher, this is defined by
     * {@code org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE}.
     */
    public static final String EXTENDER_CAPABILITY = "osgi.extender";

    /**
     * Namespace of Pax CDI extension capability. Each portable CDI extension must provide a
     * capability in this namespace to be considered by Pax CDI.
     */
    public static final String CDI_EXTENSION_CAPABILITY = "org.ops4j.pax.cdi.extension";

    /**
     * Hidden constructor of utility class.
     */
    private Constants() {
    }
}
