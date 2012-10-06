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
package org.ops4j.pax.cdi.spi;

/**
 * Type of a CDI container. Used as a marker for modifying startup behaviour.
 * @author Harald Wellmann
 */
public enum CdiContainerType {

    /** 
     * The owning bundle is an ordinary bean bundle. The CDI container can be started 
     * directly after creation.
     */
    STANDALONE,
    
    /**
     * The owning bundle is a web bundle. The CDI container cannot be started until the
     * servlet context is available.
     */
    WEB
}
