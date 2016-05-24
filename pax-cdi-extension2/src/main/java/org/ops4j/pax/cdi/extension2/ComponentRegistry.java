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

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.felix.scr.impl.helper.ComponentMethod;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.ConfigAdminTracker;
import org.apache.felix.scr.impl.helper.InitReferenceMethod;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ReferenceMethod;
import org.apache.felix.scr.impl.helper.ReferenceMethods;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.ComponentContainer;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.ConfigurableComponentHolder;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.manager.ExtendedServiceEvent;
import org.apache.felix.scr.impl.manager.ExtendedServiceListener;
import org.apache.felix.scr.impl.manager.PrototypeServiceFactoryComponentManager;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.manager.RegionConfigurationSupport;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.impl.manager.ServiceFactoryComponentManager;
import org.apache.felix.scr.impl.manager.SingleComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;


public class ComponentRegistry implements ComponentActivator, SimpleLogger {

    private final BeanManager beanManager;
    private final BundleContext bundleContext;
    private final Map<Bean<?>, ComponentDescriptor> descriptors = new HashMap<>();

    private final List<ComponentHolder<?>> holders = new ArrayList<>();
    private final Map<String, ComponentHolder<?>> holdersByName = new HashMap<>();
    private final Map<String, Set<ComponentHolder<?>>> holdersByPid = new HashMap<>();
    ConfigAdminTracker configAdminTracker;

    private final AtomicBoolean m_active = new AtomicBoolean(false);
    private final ScrConfiguration m_configuration = new ScrConfigurationImpl();
    private final Map<String, ListenerInfo> listenerMap = new HashMap<>();
    private final Map<ExtendedServiceListener, ServiceListener> privateListeners = new HashMap<>();
    private final AtomicInteger componentId = new AtomicInteger();
    private final Map<ServiceReference<?>, List<Entry>> m_missingDependencies = new HashMap<>();
    private final ConcurrentMap<Long, RegionConfigurationSupport> bundleToRcsMap = new ConcurrentHashMap<>();
    private final Executor m_componentActor = Executors.newSingleThreadExecutor();


    public ComponentRegistry(BeanManager beanManager, BundleContext bundleContext) {
        this.beanManager = beanManager;
        this.bundleContext = new PrivateRegistryWrapper(bundleContext);
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public void preStart(AfterBeanDiscovery event) {
        descriptors.values().stream()
                .map(ComponentDescriptor::getProducers)
                .flatMap(Collection::stream)
                .forEach(event::addBean);
    }

    public void start() {
        if (m_active.compareAndSet(false, true)) {

            for (ComponentDescriptor d : descriptors.values()) {
                d.validate(this);
                ComponentHolder<?> h = new CdiComponentHolder<>(this, d);
                holders.add(h);
            }

            for (ComponentHolder<?> h : holders) {
                if (holdersByName.put(h.getComponentMetadata().getName(), h) != null) {
                    throw new ComponentException("The component name '{0}" + h.getComponentMetadata().getName() + "' has already been registered.");
                }
            }
            for (ComponentHolder<?> h : holders) {
                for (String pid : h.getComponentMetadata().getConfigurationPid()) {
                    holdersByPid.computeIfAbsent(pid, s -> new HashSet()).add(h);
                }
            }

            ConfigAdminTracker tracker = null;
            for (ComponentHolder<?> holder : holders) {
                if (!holder.getComponentMetadata().isConfigurationIgnored()) {
                    tracker = new ConfigAdminTracker(this);
                    break;
                }
            }
            configAdminTracker = tracker;

            holders.forEach(h -> {
                try {
                    h.enableComponents(false);
                } catch (RuntimeException e) {
                    h.disableComponents(false);
                    throw e;
                }
            });
        }
    }

    public ComponentDescriptor addComponent(Bean<Object> component) {
        ComponentDescriptor descriptor = new ComponentDescriptor(component, this);
        descriptors.put(component, descriptor);
        return descriptor;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public Set<Bean<?>> getComponents() {
        return descriptors.keySet();
    }

    public ComponentDescriptor getDescriptor(Bean<?> component) {
        return descriptors.get(component);
    }

    @Override
    public boolean isActive() {
        return m_active.get();
    }

    @Override
    public ScrConfiguration getConfiguration() {
        return m_configuration;
    }

    @Override
    public void schedule(Runnable runnable) {
        if (isActive()) {
            m_componentActor.execute(runnable);
        }
    }

    @Override
    public long registerComponentId(AbstractComponentManager<?> sAbstractComponentManager) {
        return componentId.incrementAndGet();
    }

    @Override
    public void unregisterComponentId(AbstractComponentManager<?> sAbstractComponentManager) {
    }

    @Override
    public <S, T> void registerMissingDependency(org.apache.felix.scr.impl.manager.DependencyManager<S, T> dependencyManager, ServiceReference<T> serviceReference, int trackingCount) {
        //check that the service reference is from scr
        if (serviceReference.getProperty(ComponentConstants.COMPONENT_NAME) == null || serviceReference.getProperty(ComponentConstants.COMPONENT_ID) == null) {
            return;
        }
        List<Entry> dependencyManagers = m_missingDependencies.get(serviceReference);
        if (dependencyManagers == null) {
            dependencyManagers = new ArrayList<>();
            m_missingDependencies.put(serviceReference, dependencyManagers);
        }
        dependencyManagers.add(new Entry(dependencyManager, trackingCount));
    }

    @Override
    public <T> void missingServicePresent(ServiceReference<T> serviceReference) {
        final List<Entry> dependencyManagers = m_missingDependencies.remove(serviceReference);
        if (dependencyManagers != null) {
            m_componentActor.execute(new Runnable() {
                public void run() {
                    for (Entry entry : dependencyManagers) {
                        DependencyManager<?, T> dm = entry.getDm();
                        dm.invokeBindMethodLate(serviceReference, entry.getTrackingCount());
                    }
                }

                @Override
                public String toString() {
                    return "Late binding task of reference " + serviceReference + " for dependencyManagers " + dependencyManagers;
                }
            });
        }
    }

    @Override
    public void enableComponent(String name) {
        final Collection<ComponentHolder<?>> holders = getComponentHoldersByName(name);
        for (ComponentHolder<?> holder : holders) {
            try {
                log(LogService.LOG_DEBUG, "Enabling Component", holder.getComponentMetadata(), null, null);
                holder.enableComponents(true);
            } catch (Throwable t) {
                log(LogService.LOG_ERROR, "Cannot enable component", holder.getComponentMetadata(), null, t);
            }
        }
    }

    @Override
    public void disableComponent(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RegionConfigurationSupport setRegionConfigurationSupport(ServiceReference<ConfigurationAdmin> reference) {
        RegionConfigurationSupport trialRcs = new RegionConfigurationSupport(this, reference) {
            protected Collection<ComponentHolder<?>> getComponentHolders(TargetedPID pid) {
                return getComponentHoldersByPid(pid);
            }
        };
        RegionConfigurationSupport rcs = registerRegionConfigurationSupport(trialRcs);
        for (ComponentHolder<?> holder : holders) {
            rcs.configureComponentHolder(holder);
        }
        return rcs;
    }

    public RegionConfigurationSupport registerRegionConfigurationSupport(RegionConfigurationSupport trialRcs) {
        Long bundleId = trialRcs.getBundleId();
        RegionConfigurationSupport existing;
        RegionConfigurationSupport previous = null;
        while (true) {
            existing = bundleToRcsMap.putIfAbsent(bundleId, trialRcs);
            if (existing == null) {
                trialRcs.start();
                return trialRcs;
            }
            if (existing == previous) {
                //the rcs we referenced is still current
                return existing;
            }
            if (existing.reference()) {
                //existing can still be used
                previous = existing;
            } else {
                //existing was discarded in another thread, start over
                previous = null;
            }
        }
    }

    @Override
    public void unsetRegionConfigurationSupport(RegionConfigurationSupport rcs) {
        if (rcs.dereference()) {
            bundleToRcsMap.remove(rcs.getBundleId());
        }
    }

    public void addServiceListener(String classNameFilter, Filter eventFilter,
                                   ExtendedServiceListener<ExtendedServiceEvent> listener) {
        if (eventFilter != null && eventFilter.toString().contains(PrivateRegistryWrapper.PRIVATE)) {
            synchronized (privateListeners) {
                ServiceListener l = event -> listener.serviceChanged(new ExtendedServiceEvent(event));
                privateListeners.put(listener, l);
                try {
                    bundleContext.addServiceListener(l, "(&" + classNameFilter + eventFilter.toString() + ")");
                } catch (InvalidSyntaxException e) {
                    throw (IllegalArgumentException) new IllegalArgumentException(
                            "invalid class name filter").initCause(e);
                }
            }
            return;
        }
        ListenerInfo listenerInfo;
        synchronized (listenerMap) {
            log(LogService.LOG_DEBUG, "classNameFilter: " + classNameFilter
                    + " event filter: " + eventFilter, null, null, null);
            listenerInfo = listenerMap.get(classNameFilter);
            if (listenerInfo == null) {
                listenerInfo = new ListenerInfo();
                listenerMap.put(classNameFilter, listenerInfo);
                try {
                    bundleContext.addServiceListener(listenerInfo, classNameFilter);
                } catch (InvalidSyntaxException e) {
                    throw (IllegalArgumentException) new IllegalArgumentException(
                            "invalid class name filter").initCause(e);
                }
            }
        }
        listenerInfo.add(eventFilter, listener);
    }

    public void removeServiceListener(String className, Filter filter,
                                      ExtendedServiceListener<ExtendedServiceEvent> listener) {
        if (filter != null && filter.toString().contains(PrivateRegistryWrapper.PRIVATE)) {
            synchronized (privateListeners) {
                ServiceListener l = privateListeners.remove(listener);
                bundleContext.removeServiceListener(l);
            }
        }
        synchronized (listenerMap) {
            ListenerInfo listenerInfo = listenerMap.get(className);
            if (listenerInfo != null) {
                if (listenerInfo.remove(filter, listener)) {
                    listenerMap.remove(className);
                    bundleContext.removeServiceListener(listenerInfo);
                }
            }
        }
    }

    public Collection<ComponentHolder<?>> getComponentHoldersByPid(TargetedPID targetedPid) {
        String pid = targetedPid.getServicePid();
        Set<ComponentHolder<?>> componentHoldersUsingPid = new HashSet<>();
        synchronized (holdersByPid) {
            Set<ComponentHolder<?>> set = holdersByPid.get(pid);
            // only return the entry if non-null and not a reservation
            if (set != null) {
                for (ComponentHolder<?> holder : set) {
                    if (targetedPid.matchesTarget(holder.getActivator().getBundleContext().getBundle())) {
                        componentHoldersUsingPid.add(holder);
                    }
                }
            }
        }
        return componentHoldersUsingPid;
    }

    public Collection<ComponentHolder<?>> getComponentHoldersByName(String name) {
        if (name == null) {
            return holders;
        }
        ComponentHolder<?> componentHolder = holdersByName.get(name);
        if (componentHolder != null) {
            return Collections.<ComponentHolder<?>>singletonList(componentHolder);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isLogEnabled(int level) {
        // TODO
        return true;
    }

    @Override
    public void log(int level, String pattern, Object[] arguments, ComponentMetadata metadata, Long componentId, Throwable ex) {
        if (isLogEnabled(level)) {
            final String message = MessageFormat.format(pattern, arguments);
            log(level, message, metadata, componentId, ex);
        }
    }

    @Override
    public void log(int level, String message, ComponentMetadata metadata, Long componentId, Throwable ex) {
        // TODO
        System.err.println(message);
        if (ex != null) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void log(int level, String message, Throwable ex) {
        log(level, message, null, ex);
    }

    @Override
    public void log(int level, String message, Object[] arguments, Throwable ex) {
        log(level, message, arguments, null, 0L, ex);
    }

    private static class ListenerInfo implements ServiceListener {

        private Map<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> filterMap = new HashMap<>();

        public void serviceChanged(ServiceEvent event) {
            ServiceReference<?> ref = event.getServiceReference();
            ExtendedServiceEvent extEvent = null;
            ExtendedServiceEvent endMatchEvent = null;
            Map<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> filterMap;
            synchronized (this) {
                filterMap = this.filterMap;
            }
            for (Map.Entry<Filter, List<ExtendedServiceListener<ExtendedServiceEvent>>> entry : filterMap.entrySet()) {
                Filter filter = entry.getKey();
                if (filter == null || filter.match(ref)) {
                    if (extEvent == null) {
                        extEvent = new ExtendedServiceEvent(event);
                    }
                    for (ExtendedServiceListener<ExtendedServiceEvent> forwardTo : entry.getValue()) {
                        forwardTo.serviceChanged(extEvent);
                    }
                } else if (event.getType() == ServiceEvent.MODIFIED) {
                    if (endMatchEvent == null) {
                        endMatchEvent = new ExtendedServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, ref);
                    }
                    for (ExtendedServiceListener<ExtendedServiceEvent> forwardTo : entry.getValue()) {
                        forwardTo.serviceChanged(endMatchEvent);
                    }
                }
            }
            if (extEvent != null) {
                extEvent.activateManagers();
            }
            if (endMatchEvent != null) {
                endMatchEvent.activateManagers();
            }
        }

        public synchronized void add(Filter filter, ExtendedServiceListener<ExtendedServiceEvent> listener) {
            filterMap = new HashMap<>(filterMap);
            List<ExtendedServiceListener<ExtendedServiceEvent>> listeners = filterMap.get(filter);
            if (listeners == null) {
                listeners = Collections.singletonList(listener);
            } else {
                listeners = new ArrayList<>(listeners);
                listeners.add(listener);
            }
            filterMap.put(filter, listeners);
        }

        public synchronized boolean remove(Filter filter, ExtendedServiceListener<ExtendedServiceEvent> listener) {
            List<ExtendedServiceListener<ExtendedServiceEvent>> listeners = filterMap.get(filter);
            if (listeners != null) {
                filterMap = new HashMap<>(filterMap);
                listeners = new ArrayList<>(listeners);
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    filterMap.remove(filter);
                } else {
                    filterMap.put(filter, listeners);
                }
            }
            return filterMap.isEmpty();
        }
    }

    private static class Entry {
        private final DependencyManager<?, ?> dm;
        private final int trackingCount;

        private Entry(DependencyManager<?, ?> dm, int trackingCount) {
            this.dm = dm;
            this.trackingCount = trackingCount;
        }

        @SuppressWarnings("unchecked")
        public <S, T> DependencyManager<S, T> getDm() {
            return (DependencyManager<S, T>) dm;
        }

        public int getTrackingCount() {
            return trackingCount;
        }
    }

    private static class CdiComponentHolder<S> extends ConfigurableComponentHolder<S> {

        public CdiComponentHolder(ComponentActivator activator, ComponentMetadata metadata) {
            super(activator, metadata);
        }

        @Override
        protected ComponentMethods createComponentMethods() {
            return new EmptyMethods();
        }

        @Override
        protected AbstractComponentManager<S> createComponentManager(boolean factoryConfiguration) {
            ComponentMetadata metadata = getComponentMetadata();
            ComponentMethods componentMethods = getComponentMethods();
            switch (metadata.getServiceScope()) {
                case singleton:
                    return new CdiSingletonComponentManager<>(this, componentMethods);
                case bundle:
                    return new CdiBundleComponentManager<>(this, componentMethods);
                case prototype:
                    return new CdiPrototypeComponentManager<>(this, componentMethods);
                default:
                    throw new IllegalStateException();
            }
        }

    }

    private static class EmptyMethods implements ComponentMethods, ReferenceMethods, ReferenceMethod {
        @Override
        public void initComponentMethods(ComponentMetadata componentMetadata, Class<?> implementationObjectClass) {
        }

        @Override
        public ComponentMethod getActivateMethod() {
            return null;
        }

        @Override
        public ComponentMethod getDeactivateMethod() {
            return null;
        }

        @Override
        public ComponentMethod getModifiedMethod() {
            return null;
        }

        @Override
        public ReferenceMethods getBindMethods(String refName) {
            return this;
        }

        @Override
        public ReferenceMethod getBind() {
            return this;
        }

        @Override
        public ReferenceMethod getUnbind() {
            return null;
        }

        @Override
        public ReferenceMethod getUpdated() {
            return null;
        }

        @Override
        public InitReferenceMethod getInit() {
            return null;
        }

        @Override
        public MethodResult invoke(Object componentInstance, ComponentContextImpl<?> componentContext, RefPair<?, ?> refPair, MethodResult methodCallFailureResult, SimpleLogger logger) {
            return null;
        }

        @Override
        public <S, T> boolean getServiceObject(ComponentContextImpl<S> key, RefPair<S, T> refPair, BundleContext context, SimpleLogger logger) {
            return true;
        }
    }

    private static class CdiPrototypeComponentManager<S> extends PrototypeServiceFactoryComponentManager<S> {
        public CdiPrototypeComponentManager(ComponentContainer<S> container, ComponentMethods componentMethods) {
            super(container, componentMethods);
        }
        protected S createImplementationObject(Bundle usingBundle, SetImplementationObject<S> setter, ComponentContextImpl<S> componentContext) {
            return doCreate(this, componentContext, setter::presetComponentContext);
        }
        protected void disposeImplementationObject(ComponentContextImpl<S> componentContext, int reason) {
            doDestroy(this, componentContext);
        }
    }

    private static class CdiBundleComponentManager<S> extends ServiceFactoryComponentManager<S> {
        public CdiBundleComponentManager(ComponentContainer<S> container, ComponentMethods componentMethods) {
            super(container, componentMethods);
        }
        protected S createImplementationObject(Bundle usingBundle, SetImplementationObject<S> setter, ComponentContextImpl<S> componentContext) {
            return doCreate(this, componentContext, setter::presetComponentContext);
        }
        protected void disposeImplementationObject(ComponentContextImpl<S> componentContext, int reason) {
            doDestroy(this, componentContext);
        }
    }

    private static class CdiSingletonComponentManager<S> extends SingleComponentManager<S> {
        public CdiSingletonComponentManager(ComponentContainer<S> container, ComponentMethods componentMethods) {
            super(container, componentMethods);
        }
        protected S createImplementationObject(Bundle usingBundle, SetImplementationObject<S> setter, ComponentContextImpl<S> componentContext) {
            return doCreate(this, componentContext, setter::presetComponentContext);
        }
        protected void disposeImplementationObject(ComponentContextImpl<S> componentContext, int reason) {
            doDestroy(this, componentContext);
        }
    }

    private static <S> S doCreate(AbstractComponentManager<S> manager, ComponentContextImpl<S> componentContext, Consumer<ComponentContextImpl<S>> setter) {
        ComponentDescriptor descriptor = (ComponentDescriptor) manager.getComponentMetadata();
        S s = (S) descriptor.activate(componentContext);

        componentContext.setImplementationObject( s );
        setter.accept(componentContext);

        try {
            // componentContext.setImplementationAccessible( true );
            Method mth = ComponentContextImpl.class.getDeclaredMethod("setImplementationAccessible", boolean.class);
            mth.setAccessible(true);
            mth.invoke(componentContext, true);

            // m_circularReferences.remove();
            Field field = SingleComponentManager.class.getDeclaredField("m_circularReferences");
            field.setAccessible(true);
            ((ThreadLocal) field.get(manager)).remove();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return s;
    }

    private static <S> void doDestroy(AbstractComponentManager<S> manager, ComponentContextImpl<S> componentContext) {
        ComponentDescriptor descriptor = (ComponentDescriptor) manager.getComponentMetadata();
        descriptor.deactivate(componentContext);
    }


    static class ScrConfigurationImpl implements ScrConfiguration {
        @Override
        public int getLogLevel() {
            return 0;
        }

        @Override
        public boolean isFactoryEnabled() {
            return false;
        }

        @Override
        public boolean keepInstances() {
            return false;
        }

        @Override
        public boolean infoAsService() {
            return false;
        }

        @Override
        public long lockTimeout() {
            return DEFAULT_LOCK_TIMEOUT_MILLISECONDS;
        }

        @Override
        public long stopTimeout() {
            return DEFAULT_STOP_TIMEOUT_MILLISECONDS;
        }

    }

}
