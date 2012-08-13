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
package org.ops4j.pax.cdi.api;


public class Constants {

    
    private Constants() {
    }
    
    /**
     * Opt-in manifest header, listing beans descriptors.
     * At the moment, only a single bundle resource is supported.
     */
    public static final String MANAGED_BEANS_KEY = "Pax-ManagedBeans";
}
