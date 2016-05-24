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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.PropertyMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.ops4j.pax.cdi.api2.Attribute;
import org.ops4j.pax.cdi.api2.BundleScoped;
import org.ops4j.pax.cdi.api2.Component;
import org.ops4j.pax.cdi.api2.Config;
import org.ops4j.pax.cdi.api2.Contract;
import org.ops4j.pax.cdi.api2.Contracts;
import org.ops4j.pax.cdi.api2.Dynamic;
import org.ops4j.pax.cdi.api2.Greedy;
import org.ops4j.pax.cdi.api2.Immediate;
import org.ops4j.pax.cdi.api2.Optional;
import org.ops4j.pax.cdi.api2.Properties;
import org.ops4j.pax.cdi.api2.Property;
import org.ops4j.pax.cdi.api2.PrototypeScoped;
import org.ops4j.pax.cdi.api2.Service;
import org.ops4j.pax.cdi.extension2.support.Configurable;
import org.ops4j.pax.cdi.extension2.support.Filters;
import org.ops4j.pax.cdi.extension2.support.IterableInstance;
import org.ops4j.pax.cdi.extension2.support.SimpleBean;
import org.ops4j.pax.cdi.extension2.support.Types;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

public class ComponentDescriptor extends ComponentMetadata {

    private final Bean<Object> bean;
    private final ComponentRegistry registry;
    private final Map<InjectionPoint, Supplier<Object>> instanceSuppliers = new HashMap<>();
    private final ThreadLocal<ComponentContext> context = new ThreadLocal<>();
    private final List<Bean<?>> producers = new ArrayList<>();

    private boolean m_immediate;

    public ComponentDescriptor(Bean<Object> bean, ComponentRegistry registry) {
        super(DSVersion.DS13);
        this.bean = bean;
        this.registry = registry;

        boolean immediate = false;
        boolean hasService = false;
        Set<String> names = new HashSet<>();
        for (Annotation annotation : bean.getQualifiers()) {
            if (annotation instanceof Immediate) {
                immediate = true;
            } else if (annotation instanceof Service) {
                hasService = true;
            } else if (annotation instanceof Contract) {
                names.add(((Contract) annotation).value().getName());
            } else if (annotation instanceof Contracts) {
                for (Contract ctr : ((Contracts) annotation).value()) {
                    names.add(ctr.value().getName());
                }
            } else if (annotation instanceof Property) {
                Property prop = (Property) annotation;
                PropertyMetadata propMeta = new PropertyMetadata();
                propMeta.setName(prop.name());
                propMeta.setValue(prop.value());
                propMeta.setType(prop.type());
                addProperty(propMeta);
            } else if (annotation instanceof Properties) {
                for (Property prop : ((Properties) annotation).value()) {
                    PropertyMetadata propMeta = new PropertyMetadata();
                    propMeta.setName(prop.name());
                    propMeta.setValue(prop.value());
                    propMeta.setType(prop.type());
                    addProperty(propMeta);
                }
            } else {
                Class<? extends Annotation> annClass = annotation.annotationType();
                Attribute attr = annClass.getAnnotation(Attribute.class);
                if (attr != null) {
                    String name = attr.value();
                    Object value;
                    try {
                        Method[] methods = annClass.getDeclaredMethods();
                        if (methods != null && methods.length == 1) {
                            value = methods[0].invoke(annotation);
                        } else {
                            throw new IllegalArgumentException("Bad attribute " + annClass);
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    getProperties().put(name, value);
                }
            }
        }

        ServiceMetadata serviceMetadata = new ServiceMetadata();
        if (hasService) {
            if (names.isEmpty()) {
                for (Class cl : bean.getBeanClass().getInterfaces()) {
                    names.add(cl.getName());
                }
            }
            if (names.isEmpty()) {
                names.add(bean.getBeanClass().getName());
            }
            for (String name : names) {
                serviceMetadata.addProvide(name);
            }
        } else {
            addAllClasses(serviceMetadata, bean.getBeanClass());
            getProperties().put(PrivateRegistryWrapper.PRIVATE, true);
        }
        if (bean.getScope() == PrototypeScoped.class) {
            serviceMetadata.setScope("prototype");
        } else if (bean.getScope() == BundleScoped.class) {
            serviceMetadata.setScope("bundle");
        } else {
            serviceMetadata.setScope("singleton");
        }

        String name = bean.getName();
        if (name == null) {
            name = bean.getBeanClass().getName();
        }
        setName(name);
        setImmediate(immediate);
        setImplementationClassName(Object.class.getName());
        setConfigurationPolicy(ComponentMetadata.CONFIGURATION_POLICY_IGNORE);
        getProperties().put(ComponentDescriptor.class.getName(), this);
        getProperties().put(ComponentRegistry.class.getName(), registry);
        setService(serviceMetadata);
    }

    private void addAllClasses(ServiceMetadata serviceMetadata, Class<?> beanClass) {
        serviceMetadata.addProvide(beanClass.getName());
        for (Class<?> itf : beanClass.getInterfaces()) {
            addAllClasses(serviceMetadata, itf);
        }
        if (beanClass.getSuperclass() != null) {
            addAllClasses(serviceMetadata, beanClass.getSuperclass());
        }
    }

    public void addInjectionPoint(InjectionPoint injectionPoint) {
        Service   ref = injectionPoint.getAnnotated().getAnnotation(Service.class);
        Component cmp = injectionPoint.getAnnotated().getAnnotation(Component.class);
        Config    cfg = injectionPoint.getAnnotated().getAnnotation(Config.class);

        Type type = injectionPoint.getType();
        Class clazz;
        boolean multiple;
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

        if (cfg != null) {
            if (ref != null) {
                throw new IllegalArgumentException("Only one of @Service or @Config can be set on injection point");
            }
            if (multiple) {
                throw new IllegalArgumentException("Illegal use of Instance<?> on configuration: " + clazz.getName());
            }
            if (!clazz.isAnnotation()) {
                throw new IllegalArgumentException("Configuration class should be an annotation: " + clazz.getName());
            }
            Config config = injectionPoint.getAnnotated().getAnnotation(Config.class);
            String pid = config.pid().isEmpty() ? clazz.getName() : config.pid();
            boolean optional = injectionPoint.getAnnotated().isAnnotationPresent(Optional.class);

            setConfigurationPolicy(optional ? ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL : ComponentMetadata.CONFIGURATION_POLICY_REQUIRE);
            setConfigurationPid(new String[]{ pid });

            producers.add(new SimpleBean<>(clazz, Dependent.class, injectionPoint, () -> createConfig(clazz)));
        }
        else {
            List<String> subFilters = Filters.getSubFilters(injectionPoint.getAnnotated().getAnnotations());
            if (ref == null) {
                subFilters.add("(" + PrivateRegistryWrapper.PRIVATE + "=true)");
            }
            String filter = Filters.and(subFilters);

            boolean optional = injectionPoint.getAnnotated().isAnnotationPresent(Optional.class);
            boolean greedy = injectionPoint.getAnnotated().isAnnotationPresent(Greedy.class);
            boolean dynamic = injectionPoint.getAnnotated().isAnnotationPresent(Dynamic.class);

            ReferenceMetadata reference = new ReferenceMetadata();
            reference.setName(injectionPoint.toString());
            reference.setInterface(clazz.getName());
            reference.setTarget(filter);
            reference.setCardinality(optional ? multiple ? "0..n" : "0..1" : multiple ? "1..n" : "1..1");
            reference.setPolicy(dynamic ? "dynamic" : "static");
            reference.setPolicyOption(greedy ? "greedy" : "reluctant");
            addDependency(reference);

            Supplier<Object> supplier = () -> getService(injectionPoint, multiple, dynamic);
            producers.add(new SimpleBean<>(clazz, Dependent.class, injectionPoint, supplier));
            instanceSuppliers.put(injectionPoint, supplier);
        }
    }

    public ComponentContext getComponentContext() {
        return context.get();
    }

    @SuppressWarnings("unchecked")
    protected Object createConfig(Class<?> clazz) {
        ComponentContext cc = context.get();
        Map<String, Object> cfg = (Map) cc.getProperties();
        return Configurable.create(clazz, cfg != null ? cfg : new Hashtable<>());
    }

    protected Object getService(InjectionPoint injectionPoint, boolean isInstance, boolean dynamic) {
        ComponentContext cc = context.get();
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
        this.context.set(cc);
        try {
            BeanManager beanManager = registry.getBeanManager();
            Context context = beanManager.getContext(bean.getScope());
            return context.get(bean, beanManager.createCreationalContext(bean));
        } finally {
            this.context.set(null);
        }
    }

    public void deactivate(ComponentContext cc) {
        this.context.set(cc);
        try {
            BeanManager beanManager = registry.getBeanManager();
            AlterableContext context = (AlterableContext) beanManager.getContext(bean.getScope());
            context.destroy(bean);
        } finally {
            this.context.set(null);
        }
    }

    public void inject(Object instance, InjectionPoint injectionPoint) {
        Supplier<Object> supplier = instanceSuppliers.get(injectionPoint);
        if (supplier != null) {
            Field field = ((AnnotatedField) injectionPoint.getAnnotated()).getJavaMember();
            field.setAccessible(true);
            try {
                field.set(instance, supplier.get());
            }
            catch (IllegalAccessException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    @Override
    public void setImmediate(boolean immediate) {
        m_immediate = immediate;
    }

    @Override
    public boolean isImmediate() {
        return m_immediate;
    }

    @Override
    public String toString() {
        return "Component[" + "bean=" + bean + ']';
    }

}
