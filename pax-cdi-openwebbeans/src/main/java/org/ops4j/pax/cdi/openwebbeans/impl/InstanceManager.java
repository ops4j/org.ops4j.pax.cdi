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
package org.ops4j.pax.cdi.openwebbeans.impl;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * An unmanaged injection target to access the {@code Instance} and {@code Event} objects of the
 * current CDI container which are hard to obtain directly from the {@code WebBeansContext}.
 * 
 * @author Harald Wellmann
 * 
 */
public class InstanceManager {

    @Inject
    @Any
    private Instance<Object> instance;

    @Inject
    @Any
    private Event<Object> event;

    public Instance<Object> getInstance() {
        return instance;
    }

    public Event<Object> getEvent() {
        return event;
    }
}
