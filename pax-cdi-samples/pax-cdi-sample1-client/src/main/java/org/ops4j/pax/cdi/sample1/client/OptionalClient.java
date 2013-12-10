package org.ops4j.pax.cdi.sample1.client;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.Timeout;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.osgi.service.cdi.Component;
import org.osgi.service.cdi.Filter;
import org.osgi.service.cdi.Service;

@Component
public class OptionalClient {

    @Inject
    @Service(required = false)
    private IceCreamService iceCreamService;

    public String getFlavour() {
        return iceCreamService.getFlavour();
    }

}
