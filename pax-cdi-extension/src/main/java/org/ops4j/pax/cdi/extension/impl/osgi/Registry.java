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
package org.ops4j.pax.cdi.extension.impl.osgi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.ComponentManager;
import org.apache.felix.scr.impl.manager.ReferenceManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.ops4j.pax.cdi.extension.api.runtime.CdiOsgiRuntime;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentConfigurationDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentDescriptionDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ReferenceDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.SatisfiedReferenceDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.UnsatisfiedReferenceDTO;
import org.ops4j.pax.cdi.extension.impl.component2.ComponentRegistry;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class Registry implements CdiOsgiRuntime {

    private static final String[] EMPTY = {};
    private static final Registry INSTANCE = new Registry();

    private final List<ComponentRegistry> registries = new CopyOnWriteArrayList<>();

    private Registry() {
    }

    public static Registry getInstance() {
        return INSTANCE;
    }

    public void register(ComponentRegistry componentRegistry) {
        if (!registries.contains(componentRegistry)) {
            registries.add(componentRegistry);
        }
    }

    public void unregister(ComponentRegistry componentRegistry) {
        registries.remove(componentRegistry);
    }

    @Override
    public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles) {
        Collection<ComponentDescriptionDTO> dtos = new ArrayList<>();
        for (ComponentRegistry registry : registries) {
            Bundle bundle = registry.getBundleContext().getBundle();
            if (bundles.length == 0 || Arrays.asList(bundles).contains(bundle)) {
                for (ComponentHolder<?> holder : registry.getComponentHolders()) {
                    dtos.add(holderToDescription(holder));
                }
            }
        }
        return dtos;
    }

    @Override
    public ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name) {
        return getComponentDescriptionDTO(bundle.getBundleId(), name);
    }

    @Override
    public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description) {
        if (description == null) {
            return Collections.emptyList();
        }
        ComponentHolder<?> holder = getHolderFromDescription(description);
        if (holder == null) {
            return Collections.emptyList();
        }
        description = holderToDescription(holder);
        List<? extends ComponentManager<?>> managers = holder.getComponents();
        List<ComponentConfigurationDTO> result = new ArrayList<>(managers.size());
        for (ComponentManager<?> manager : managers) {
            result.add(managerToConfiguration(manager, description));
        }
        return result;
    }

    private ComponentHolder<?> getHolderFromDescription(ComponentDescriptionDTO description) {
        if (description.bundle == null) {
            throw new IllegalArgumentException("No bundle supplied in ComponentDescriptionDTO named " + description.name);
        }
        long bundleId = description.bundle.id;
        for (ComponentRegistry registry : registries) {
            if (registry.getBundleContext().getBundle().getBundleId() == bundleId) {
                return registry.getComponentHolder(description.name);
            }
        }
        return null;
    }

    private ComponentDescriptionDTO getComponentDescriptionDTO(long bundleId, String name) {
        for (ComponentRegistry registry : registries) {
            if (registry.getBundleContext().getBundle().getBundleId() == bundleId) {
                ComponentHolder<?> holder = registry.getComponentHolder(name);
                if (holder != null) {
                    return holderToDescription(holder);
                }
            }
        }
        return null;
    }

    private ComponentDescriptionDTO holderToDescription(ComponentHolder<?> holder) {
        ComponentDescriptionDTO dto = new ComponentDescriptionDTO();
        ComponentMetadata m = holder.getComponentMetadata();
        dto.activate = m.getActivate();
        dto.bundle = bundleToDTO(holder.getActivator().getBundleContext());
        dto.configurationPid = m.getConfigurationPid().toArray(new String[m.getConfigurationPid().size()]);
        dto.configurationPolicy = m.getConfigurationPolicy();
        dto.deactivate = m.getDeactivate();
        dto.defaultEnabled = m.isEnabled();
        dto.factory = m.getFactoryIdentifier();
        dto.immediate = m.isImmediate();
        dto.implementationClass = m.getImplementationClassName();
        dto.modified = m.getModified();
        dto.name = m.getName();
        dto.properties = deepCopy(m.getProperties());
        dto.references = refsToDTO(m.getDependencies());
        dto.scope = m.getServiceMetadata() == null ? null : m.getServiceMetadata().getScope().name();
        dto.serviceInterfaces = m.getServiceMetadata() == null ? EMPTY : m.getServiceMetadata().getProvides();
        return dto;
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        HashMap<String, Object> result = new HashMap<>(source.size());
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), convert(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> deepCopy(ServiceReference<?> source) {
        String[] keys = source.getPropertyKeys();
        HashMap<String, Object> result = new HashMap<>(keys.length);
        for (String key : keys) {
            result.put(key, convert(source.getProperty(key)));
        }
        return result;
    }

    Object convert(Object source) {
        if (source.getClass().isArray()) {
            Class<?> type = source.getClass().getComponentType();
            if (checkType(type)) {
                return source;
            }
            return String.valueOf(source);
            /* array copy code in case it turns out to be needed
            int length = Array.getLength(source);
            Object copy = Array.newInstance(type, length);
            for (int i = 0; i<length; i++)
            {
                Array.set(copy, i, Array.get(source, i));
            }
            return copy;
            */
        }
        if (checkType(source.getClass())) {
            return source;
        }
        return String.valueOf(source);
    }

    boolean checkType(Class<?> type) {
        if (type == String.class) {
            return true;
        }
        if (type == Boolean.class) {
            return true;
        }
        if (Number.class.isAssignableFrom(type)) {
            return true;
        }
        return DTO.class.isAssignableFrom(type);
    }

    private ReferenceDTO[] refsToDTO(List<ReferenceMetadata> dependencies) {
        ReferenceDTO[] dtos = new ReferenceDTO[dependencies.size()];
        int i = 0;
        for (ReferenceMetadata r : dependencies) {
            ReferenceDTO dto = new ReferenceDTO();
            dto.bind = r.getBind();
            dto.cardinality = r.getCardinality();
            dto.field = r.getField();
            dto.fieldOption = r.getFieldOption();
            dto.interfaceName = r.getInterface();
            dto.name = r.getName();
            dto.policy = r.getPolicy();
            dto.policyOption = r.getPolicyOption();
            dto.scope = r.getScope().name();
            dto.target = r.getTarget();
            dto.unbind = r.getUnbind();
            dto.updated = r.getUpdated();
            dtos[i++] = dto;
        }
        return dtos;
    }

    private BundleDTO bundleToDTO(BundleContext bundleContext) {
        if (bundleContext == null) {
            return null;
        }
        Bundle bundle = bundleContext.getBundle();
        if (bundle == null) {
            return null;
        }
        BundleDTO b = new BundleDTO();
        b.id = bundle.getBundleId();
        b.lastModified = bundle.getLastModified();
        b.state = bundle.getState();
        b.symbolicName = bundle.getSymbolicName();
        b.version = bundle.getVersion().toString();
        return b;
    }

    private ComponentConfigurationDTO managerToConfiguration(ComponentManager<?> manager, ComponentDescriptionDTO description) {
        ComponentConfigurationDTO dto = new ComponentConfigurationDTO();
        dto.satisfiedReferences = satisfiedRefManagersToDTO(manager.getReferenceManagers());
        dto.unsatisfiedReferences = unsatisfiedRefManagersToDTO(manager.getReferenceManagers());
        dto.description = description;
        dto.id = manager.getId();
        dto.properties = new HashMap<>(manager.getProperties()); // TODO deep copy?
        dto.state = manager.getSpecState();
        return dto;
    }

    private SatisfiedReferenceDTO[] satisfiedRefManagersToDTO(List<? extends ReferenceManager<?, ?>> referenceManagers) {
        List<SatisfiedReferenceDTO> dtos = new ArrayList<>();
        for (ReferenceManager<?, ?> ref : referenceManagers) {
            if (ref.isSatisfied()) {
                SatisfiedReferenceDTO dto = new SatisfiedReferenceDTO();
                dto.name = ref.getName();
                dto.target = ref.getTarget();
                List<ServiceReference<?>> serviceRefs = ref.getServiceReferences();
                ServiceReferenceDTO[] srDTOs = new ServiceReferenceDTO[serviceRefs.size()];
                int j = 0;
                for (ServiceReference<?> serviceRef : serviceRefs) {
                    ServiceReferenceDTO srefDTO = serviceReferenceToDTO(serviceRef);
                    if (srefDTO != null) {
                        srDTOs[j++] = srefDTO;
                    }
                }
                dto.boundServices = srDTOs;
                dtos.add(dto);
            }
        }
        return dtos.toArray(new SatisfiedReferenceDTO[dtos.size()]);
    }

    private UnsatisfiedReferenceDTO[] unsatisfiedRefManagersToDTO(List<? extends ReferenceManager<?, ?>> referenceManagers) {
        List<UnsatisfiedReferenceDTO> dtos = new ArrayList<>();
        for (ReferenceManager<?, ?> ref : referenceManagers) {
            if (!ref.isSatisfied()) {
                UnsatisfiedReferenceDTO dto = new UnsatisfiedReferenceDTO();
                dto.name = ref.getName();
                dto.target = ref.getTarget();
                List<ServiceReference<?>> serviceRefs = ref.getServiceReferences();
                ServiceReferenceDTO[] srDTOs = new ServiceReferenceDTO[serviceRefs.size()];
                int j = 0;
                for (ServiceReference<?> serviceRef : serviceRefs) {
                    ServiceReferenceDTO srefDTO = serviceReferenceToDTO(serviceRef);
                    if (srefDTO != null) {
                        srDTOs[j++] = srefDTO;
                    }
                }
                dto.targetServices = srDTOs;
                dtos.add(dto);
            }
        }
        return dtos.toArray(new UnsatisfiedReferenceDTO[dtos.size()]);
    }

    private ServiceReferenceDTO serviceReferenceToDTO(ServiceReference<?> serviceRef) {
        if (serviceRef == null) {
            return null;
        }

        ServiceReferenceDTO dto = new ServiceReferenceDTO();
        Bundle bundle = serviceRef.getBundle();
        // No bundle ever has -1 as ID, so this indicates no bundle.
        dto.bundle = bundle != null ? bundle.getBundleId() : -1;
        dto.id = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        dto.properties = deepCopy(serviceRef);
        Bundle[] usingBundles = serviceRef.getUsingBundles();
        if (usingBundles != null) {
            long[] usingBundleIds = new long[usingBundles.length];
            for (int i = 0; i < usingBundles.length; i++) {
                usingBundleIds[i] = usingBundles[i].getBundleId();
            }
            dto.usingBundles = usingBundleIds;
        }
        return dto;
    }

}
