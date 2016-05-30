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
package org.ops4j.pax.cdi.extension2;

import javax.enterprise.inject.spi.Bean;
import java.util.List;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.osgi.service.component.ComponentContext;

public abstract class AbstractDescriptor extends ComponentMetadata {

    private final ThreadLocal<ComponentContext> context = new ThreadLocal<>();
    protected final ComponentRegistry registry;
    private boolean m_immediate;

    public AbstractDescriptor(ComponentRegistry registry) {
        super(DSVersion.DS13);
        this.registry = registry;
    }

    public ComponentContext getComponentContext() {
        return context.get();
    }

    public abstract List<Bean<?>> getProducers();

    public abstract Object activate(ComponentContext cc);

    public abstract void deactivate(ComponentContext cc);

    @Override
    public void setImmediate(boolean immediate) {
        m_immediate = immediate;
    }

    @Override
    public boolean isImmediate() {
        return m_immediate;
    }

}
