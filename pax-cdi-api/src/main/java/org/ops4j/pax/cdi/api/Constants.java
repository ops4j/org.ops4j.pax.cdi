package org.ops4j.pax.cdi.api;


public class Constants {

    
    private Constants() {
    }
    
    /**
     * Opt-in manifest header, listing beans descriptors.
     * At the moment, only single bundle resource is supported.
     */
    public static final String MANAGED_BEANS_KEY = "Pax-ManagedBeans";
}
