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

import java.lang.annotation.Annotation;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;

import org.osgi.framework.Bundle;

/**
 * A {@code CdiContainer} is a JSR-299 compliant CDI container for a given OSGi bundle, containing
 * all beans visible to this bundle according to its wiring. Any bundle with a {@code beans.xml}
 * descriptor visible to the current bundle and any CDI extension visible to the
 * {@link CdiContainerFactory} that created this container may contribute beans to this container,
 * turning the bundle into a <em>bean bundle</em>.
 * <p>
 * Any bean bundle has its own CDI container. All these containers are completely isolated from each
 * other, except for publishing or injecting OSGi services wrapped in CDI beans.
 * 
 * @author Harald Wellmann
 * 
 */
public interface CdiContainer {

    /**
     * Starts the CDI container for the current bundle.
     * @param environment implementation dependent environment for the container, maybe null.
     */
    void start(Object environment);

    /**
     * Stops the CDI container for the current bundle.
     */
    void stop();
    
    /**
     * Returns the type of this container.
     * @return
     */
    CdiContainerType getContainerType();

    /**
     * Gets the bundle hosting this CDI container.
     * 
     * @return current bundle
     */
    Bundle getBundle();

    /**
     * Returns the {@link BeanManager} of the CDI container.
     * 
     * @return
     */
    BeanManager getBeanManager();

    /**
     * Returns the {@link Event} bean of the CDI container, which may be further specialized to fire
     * specific events.
     * 
     * @return Event bean
     */
    <T> Event<T> getEvent();

    /**
     * Returns the overall @{link Instance} of the CDI container, providing access to all bean
     * instances.
     * 
     * @return Instance of container
     */
    Instance<Object> getInstance();

    /**
     * Returns the context class loader used by this container. All bean classes are loaded from
     * this class loader. The {@link CdiContainerFactoryClient} must set the thread context class
     * loader to this loader before creating this container.
     * 
     * @return context class loader associated to this container
     */
    ClassLoader getContextClassLoader();
    
    /**
     * Return an implementation object wrapped by this container.
     * @param wrappedClass
     * @return
     */
    <T> T unwrap(Class<T> wrappedClass);
    
    /**
     * Starts a context for the given scope.
     * @param scope CDI scope annotation
     */
    void startContext(Class<? extends Annotation> scope);
    
    /**
     * Stops the current context for the given scope.
     * @param scope CDI scope annotation
     */
    void stopContext(Class<? extends Annotation> scope);
}
