/*
 * Copyright 2014 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.context;

import java.util.HashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;


public class BeanMap extends HashMap<Contextual<?>, SingletonScopeContextEntry<?>> {

    private static final long serialVersionUID = 1L;

    private CreationalContext<Object> creationalContext;


    /**
     * @return the creationalContext
     */
    public CreationalContext<Object> getCreationalContext() {
        return creationalContext;
    }


    /**
     * @param creationalContext the creationalContext to set
     */
    public void setCreationalContext(CreationalContext<Object> creationalContext) {
        this.creationalContext = creationalContext;
    }
}
