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
package org.ops4j.pax.cdi.sample1.client;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.*;
import org.ops4j.pax.cdi.api.event.ServiceAdded;
import org.ops4j.pax.cdi.api.event.ServiceCdiEvent;
import org.ops4j.pax.cdi.api.event.ServiceRemoved;
import org.ops4j.pax.cdi.sample1.IceCreamService;

@Service @Component @Immediate @SingletonScoped
public class IceCreamClient {

    @Inject
    @Service @Dynamic @Filter("(flavour=chocolate)")
    private IceCreamService iceCreamService;

    @Inject
    @Service @Dynamic
    private Instance<IceCreamService> iceCreamServices;

    private List<String> events = new ArrayList<>();

    // we may be in trouble when this even is sent to us before reaching pax-cdi extension...

//    public void onInit(@Observes @Initialized(ApplicationScoped.class) Object object) {
//        events.add("initialized application scope");
//    }
//
//    public void onShutdown(@Observes @Destroyed(ApplicationScoped.class) Object object) {
//        events.add("destroyed application scope");
//    }

    public void onInit(@Observes @ServiceAdded BeanManager manager) {
        events.add("registered BeanManager");
    }

    public void onIceCreamServiceAdded(@Observes @ServiceAdded ServiceCdiEvent<? extends IceCreamService> event) {
        String flavour = (String) event.getReference().getProperty("flavour");
        events.add("added IceCreamService with flavour " + flavour);
    }

    public void onIceCreamServiceAdded(@Observes @ServiceAdded IceCreamService service) {
        events.add("added IceCreamService with class " + service.getClass().getName());
    }

    public void onIceCreamServiceRemoved(@Observes @ServiceRemoved ServiceCdiEvent<? extends IceCreamService> event) {
        String flavour = (String) event.getReference().getProperty("flavour");
        events.add("removed IceCreamService with flavour " + flavour);
    }

    public void onIceCreamServiceRemoved(@Observes @ServiceRemoved IceCreamService service) {
        events.add("removed IceCreamService with class " + service.getClass().getName());
    }

    public String getFlavour() {
        return iceCreamService.getFlavour();
    }

    public List<String> getAllFlavours() {
        List<String> flavours = new ArrayList<String>();
        for (IceCreamService service : iceCreamServices) {
            String flavour = service.getFlavour();
            flavours.add(flavour);
        }
        return flavours;
    }

    public List<String> getEvents() {
        return events;
    }
}
