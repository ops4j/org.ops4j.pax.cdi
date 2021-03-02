/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.spi.scan;

import java.net.URL;
import java.util.List;



public interface BeanDescriptor {

    /**
     * @return the version string of the beans.xml file (if any), or <code>null</code> if not set
     */
    String getVersion();

    /**
     * @return the BeanDiscoveryMode used by this very BDA
     */
    BeanDiscoveryMode getBeanDiscoveryMode();

    /**
     * @return whether the given class is excluded from scanning or not
     */
    boolean isClassExcluded(String clazz);

    /**
     * @return whether the given package is excluded from scanning or not
     */
    boolean isPackageExcluded(String packageName);

    /**
     * @return the class name of the Interceptors defined in the beans.xml
     *          in a &lt;interceptors&gt;&lt;class&gt; section or an empty List.
     */
    List<String> getInterceptors();

    /**
     * @return the class name of Decorators defined in this beans.xml
     *          in a &lt;decorators&gt;&lt;class&gt; section or an empty List.
     */
    List<String> getDecorators();

    /**
     * @return the class name of the Alternatives defined in this beans.xml
     *          in a &lt;alternatives&gt;&lt;class&gt; section or an empty List.
     */
    List<String> getAlternativeClasses();

    /**
     * @return the class name of the Alternatives defined in this beans.xml
     *          in a &lt;alternatives&gt;&lt;stereotype&gt; section or an empty List.
     */
    List<String> getAlternativeStereotypes();


    List<String> getExcludedClasses();
    List<String> getExcludedPackages();

    URL getUrl();
}
