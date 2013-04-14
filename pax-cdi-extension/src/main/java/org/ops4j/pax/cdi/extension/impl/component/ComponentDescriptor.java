/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.component;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes an OSGi service component and its dependencies.
 * 
 * @author Harald Wellmann
 * 
 */
public class ComponentDescriptor {

    private static Logger log = LoggerFactory.getLogger(ComponentDescriptor.class);

    /**
     * Dependencies of this component. There is one entry for each {@code @OsgiService} injection
     * point.
     */
    private List<ComponentDependency> dependencies = new ArrayList<ComponentDependency>();

    /**
     * Number of unsatisfied dependencies. The component is satisfied when there are no unsatisfied
     * dependencies.
     */
    private int numUnsatisfiedDependencies;

    /**
     * Checks if the component is satisfied.
     * 
     * @return
     */
    public boolean isSatisfied() {
        return numUnsatisfiedDependencies == 0;
    }

    /**
     * Checks if the given service reference satisfies an unsatisfied dependency.
     * 
     * @param ref
     *            service reference
     * @param service
     *            referenced service object
     * @return true if the component is now satisfied.
     */
    public <S> boolean checkDependencies(ServiceReference<S> ref, S service) {
        for (ComponentDependency dependency : dependencies) {
            if (dependency.checkDependency(ref, service)) {
                numUnsatisfiedDependencies--;
                log.debug("found required dependency {}, unsatisfied = {}", ref,
                    numUnsatisfiedDependencies);
            }
        }
        return isSatisfied();
    }

    /**
     * Adds a service dependency to the given component.
     * 
     * @param ip
     *            OSGi service injection point of the corresponding bean
     */
    public void addDependency(InjectionPoint ip) {
        String filterString = InjectionPointOsgiUtils.getFilter(ip);
        try {
            Filter filter = FrameworkUtil.createFilter(filterString);
            ComponentDependency componentDependency = new ComponentDependency(ip, filter);
            dependencies.add(componentDependency);
            numUnsatisfiedDependencies++;
        }
        catch (InvalidSyntaxException exc) {
            throw new Ops4jException(exc);
        }
    }
}
