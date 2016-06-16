/*
 * Copyright 2016 Guillaume Nodet
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
package org.ops4j.pax.cdi.extension.impl.component2;

import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleContextHolder {

    private static final ThreadLocal<BundleContext> STORAGE = new ThreadLocal<>();

    public static BundleContext getBundleContext() {
        BundleContext bundleContext =  STORAGE.get();
        if (bundleContext == null) {
            Bundle bundle = BeanBundles.getBundle(Thread.currentThread().getContextClassLoader());
            if (bundle != null) {
                bundleContext = bundle.getBundleContext();
            }
        }
        return bundleContext;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        STORAGE.set(bundleContext);
    }

}
