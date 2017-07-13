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
package org.ops4j.pax.cdi.extension.impl.component2;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.ops4j.pax.cdi.api.*;
import org.ops4j.pax.cdi.extension.impl.support.Filters;
import org.ops4j.pax.cdi.extension.impl.support.IterableInstance;
import org.ops4j.pax.cdi.extension.impl.support.PrivateRegistryWrapper;
import org.ops4j.pax.cdi.extension.impl.support.SimpleBean;
import org.ops4j.pax.cdi.extension.impl.support.Types;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

public class GlobalDescriptor extends AbstractDescriptor {

    private final ComponentRegistry registry;
    private final Map<InjectionPoint, Supplier<Object>> instanceSuppliers = new HashMap<>();
    private final List<Bean<?>> producers = new ArrayList<>();
    private CdiContainer container;

    private ComponentContext context;

    public GlobalDescriptor(ComponentRegistry registry) {
        super(registry);
        this.registry = registry;

        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.addProvide(Object.class.getName());
        serviceMetadata.setScope("singleton");

        setName(UUID.randomUUID().toString());
        setImmediate(true);
        setImplementationClassName(Object.class.getName());
        setConfigurationPolicy(CONFIGURATION_POLICY_IGNORE);
        getProperties().put(GlobalDescriptor.class.getName(), this);
        getProperties().put(ComponentRegistry.class.getName(), registry);
        setService(serviceMetadata);
    }

    public void pauseIfNeeded() {
        if (!getProducers().isEmpty()) {
            BundleContext bundleContext = BundleContextHolder.getBundleContext();
            ServiceReference<CdiContainerFactory> factoryRef = bundleContext.getServiceReference(CdiContainerFactory.class);
            CdiContainerFactory factory = bundleContext.getService(factoryRef);
            container = factory.getContainer(bundleContext.getBundle());
            container.pause();
        }
    }

    static class DummyInjectionPoint implements InjectionPoint, Annotated {

        private final Class<?> type;
        private final Map<Class<? extends Annotation>, Annotation> annotations;

        public DummyInjectionPoint(Class<?> type, Set<Annotation> annotations) {
            this.type = type;
            this.annotations = new HashMap<>();
            for (Annotation annotation : annotations) {
                this.annotations.put(annotation.annotationType(), annotation);
            }
        }

        @Override
        public String toString() {
            return type.getName() + new ArrayList<>(annotations.values());
        }

        @Override
        public Type getBaseType() {
            return type;
        }

        @Override
        public Set<Type> getTypeClosure() {
            return Collections.singleton(type);
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            return (T) annotations.get(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return Collections.unmodifiableSet(new HashSet<>(annotations.values()));
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return annotations.containsKey(annotationType);
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return getAnnotations();
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return null;
        }

        @Override
        public Annotated getAnnotated() {
            return this;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Bean<T> addGlobalInjectionPoint(Class<T> clazz, Set<Annotation> qualifiers) {
        return (Bean<T>) addGlobalInjectionPoint(new DummyInjectionPoint(clazz, qualifiers));
    }

    public Bean<?> addGlobalInjectionPoint(final InjectionPoint injectionPoint) {
        Service   ref = injectionPoint.getAnnotated().getAnnotation(Service.class);
        Component cmp = injectionPoint.getAnnotated().getAnnotation(Component.class);
        Config    cfg = injectionPoint.getAnnotated().getAnnotation(Config.class);

        Type type = injectionPoint.getType();
        Class clazz;
        final boolean multiple;
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw == Instance.class) {
                multiple = true;
                clazz = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            } else {
                multiple = false;
                clazz = (Class) ((ParameterizedType) type).getRawType();
            }
        } else {
            if (type == Instance.class) {
                throw new IllegalArgumentException();
            }
            multiple = false;
            clazz = (Class) type;
        }
        if (multiple) {
            throw new IllegalArgumentException("@Global Instance<?> not supported: " + injectionPoint);
        }
        if (cfg != null) {
            throw new IllegalArgumentException("@Config @Global not supported: " + injectionPoint);
        }
        else {
            List<String> subFilters = Filters.getSubFilters(injectionPoint.getAnnotated().getAnnotations());
            if (ref == null) {
                subFilters.add("(" + PrivateRegistryWrapper.PRIVATE + "=true)");
            }
            String filter = Filters.and(subFilters);

            boolean optional = injectionPoint.getAnnotated().isAnnotationPresent(Optional.class);
            boolean greedy = injectionPoint.getAnnotated().isAnnotationPresent(Greedy.class);
            final boolean dynamic = injectionPoint.getAnnotated().isAnnotationPresent(Dynamic.class);

            ReferenceMetadata reference = new ReferenceMetadata();
            reference.setName(injectionPoint.getAnnotated().toString());
            reference.setInterface(clazz.getName());
            reference.setTarget(filter);
            reference.setCardinality(optional ? "0..1" : "1..1");
            reference.setPolicy(dynamic ? "dynamic" : "static");
            reference.setPolicyOption(greedy ? "greedy" : "reluctant");
            addDependency(reference);

            Supplier<Object> supplier = () -> GlobalDescriptor.this.getService(injectionPoint, multiple, dynamic);
            Bean<?> bean = new SimpleBean<>(clazz, Dependent.class, injectionPoint, supplier);
            producers.add(bean);
            instanceSuppliers.put(injectionPoint, supplier);
            return bean;
        }
    }

    public ComponentContext getComponentContext() {
        return context;
    }

    protected Object getService(final InjectionPoint injectionPoint, boolean isInstance, boolean dynamic) {
        final ComponentContext cc = context;
        if (cc == null) {
            throw new IllegalStateException("Can not obtain @Component instance");
        }
        if (dynamic && isInstance) {
            Iterable<Object> iterable = () -> new Iterator<Object>() {
                final Object[] services = cc.locateServices(injectionPoint.toString());
                int idx;
                public boolean hasNext() {
                    return services != null && idx < services.length;
                }
                public Object next() {
                    return services[idx++];
                }
            };
            return new IterableInstance<>(iterable);
        }
        else if (isInstance) {
            final Object[] services = cc.locateServices(injectionPoint.toString());
            Iterable<Object> iterable = () -> new Iterator<Object>() {
                int idx;
                public boolean hasNext() {
                    return services != null && idx < services.length;
                }
                public Object next() {
                    return services[idx++];
                }
            };
            return new IterableInstance<>(iterable);
        }
        else if (dynamic) {
            Class<Object> clazz = Types.getRawType(injectionPoint.getType());
            ClassLoader cl = registry.getBundleContext().getBundle().adapt(BundleWiring.class).getClassLoader();
            return Proxy.newProxyInstance(cl, new Class[]{ clazz },
                    (p, method, args) -> {
                        Object t = cc.locateService(injectionPoint.toString());
                        return t != null ? method.invoke(t, args) : null;
                    });
        }
        else {
            return cc.locateService(injectionPoint.toString());
        }
    }

    public List<Bean<?>> getProducers() {
        return producers;
    }

    public Object activate(ComponentContext cc) {
        if (container != null) {
            container.resume();
            context = cc;
        }
        return new Object();
    }

    public void deactivate(ComponentContext cc) {
        new Thread(() -> {
            if (container != null) {
                context = null;
                container.stop();
                container.start(new Object());
            }
        }).start();
    }

}
