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
 * Wraps the given JSF application factory.
 *
 * @author pmuir
 * @author alesj
 * @author Harald Wellmann
 */
public class WeldApplicationFactory extends AbstractForwardingApplicationFactory {

    private final ApplicationFactory applicationFactory;

    private volatile Application application;

    /**
     * Wraps the given application factory.
     * @param applicationFactory application factory
     */
    public WeldApplicationFactory(ApplicationFactory applicationFactory) {
        this.applicationFactory = applicationFactory;
    }

    @Override
    protected ApplicationFactory delegate() {
        return applicationFactory;
    }

    @Override
    public Application getApplication() {
        if (application == null) {
            synchronized (this) {
                if (application == null) {
                    application = new WeldApplication(delegate().getApplication());
                }
            }
        }
        return application;
    }

    @Override
    public void setApplication(Application application) {
        synchronized (this) {
            // invalidate the instance, so it picks up new application
            this.application = null;
            super.setApplication(application);
        }
    }
}
