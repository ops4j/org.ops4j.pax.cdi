package sample.osgiProviderProducerMethod;


import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.SingletonScoped;

import javax.enterprise.inject.Produces;


/**
 * @author mwinkels
 * @since Jul 25, 2016
 */
public class Beans {

    @Produces
    @OsgiServiceProvider
    @SingletonScoped
    public Greeter greeter() {
        return new EnglishGreeter();
    }
}
