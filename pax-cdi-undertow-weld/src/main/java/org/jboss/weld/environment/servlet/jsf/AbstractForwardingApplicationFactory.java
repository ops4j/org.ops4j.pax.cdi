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
package org.jboss.weld.environment.servlet.jsf;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

/**
 * Base class for forwarding JSF ApplicationFactories. Extend to add or adapt functionality.
 *
 * @author pmuir
 * @author Harald Wellmann
 */
public abstract class AbstractForwardingApplicationFactory extends ApplicationFactory {

    protected abstract ApplicationFactory delegate();

    @Override
    public Application getApplication() {
        return delegate().getApplication();
    }

    @Override
    public void setApplication(Application application) {
        delegate().setApplication(application);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

}
