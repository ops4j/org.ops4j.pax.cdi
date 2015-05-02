/*
 * Copyright 2015 Harald Wellmann.
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
package org.ops4j.pax.cdi.extension.impl.client;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionTarget;

import org.ops4j.pax.cdi.spi.InjectionTargetWrapper;

/**
 * Wraps an injection target to add OSGi-specific resolution of {@code Instance<T>} injection
 * points.
 *
 * @param <T>
 *            type of injection target
 *
 * @author Harald Wellmann
 *
 */
@Dependent
public class OsgiInjectionTargetWrapper<T> implements InjectionTargetWrapper<T> {

    @Override
    public InjectionTarget<T> wrap(InjectionTarget<T> delegate) {
        return new OsgiInjectionTarget<T>(delegate);
    }
}
