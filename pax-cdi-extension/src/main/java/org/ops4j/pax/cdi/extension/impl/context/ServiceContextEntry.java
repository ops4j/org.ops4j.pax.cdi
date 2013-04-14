package org.ops4j.pax.cdi.extension.impl.context;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * An entry in the {@link ServiceContext}, wrapping a managed bean, its contextual instance
 * and the creational context for this instance.
 * 
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class ServiceContextEntry<T> {

    private Contextual<T> bean;
    private T contextualInstance;
    private CreationalContext<T> creationalContext;

    public ServiceContextEntry(Contextual<T> bean, T contextualInstance,
        CreationalContext<T> creationalContext) {
        
        this.bean = bean;
        this.contextualInstance = contextualInstance;
        this.creationalContext = creationalContext;
    }

    public Contextual<T> getBean() {
        return bean;
    }

    public T getContextualInstance() {
        return contextualInstance;
    }

    public CreationalContext<T> getCreationalContext() {
        return creationalContext;
    }
}
