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
package org.ops4j.pax.cdi.sample1.web.session;

import java.io.Serializable;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;

@SuppressWarnings("serial")
@SessionScoped
class SimpleSessionBean implements Serializable {

    private static boolean beanDestroyed;
    private final long timestamp = System.currentTimeMillis();

    public long getTimestamp() {
        return timestamp;
    }

    @PreDestroy
    public void destroyBean() {
        beanDestroyed = true;
    }

    public static boolean isBeanDestroyed() {
        return beanDestroyed;
    }

    public static void setBeanDestroyed(boolean beanDestroyed) {
        SimpleSessionBean.beanDestroyed = beanDestroyed;
    }
}
