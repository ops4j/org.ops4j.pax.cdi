package org.osgi.service.cdi;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Qualifier;

/**
 */
public class ServiceTest {

    /** Demo event annotation */
    @EventAdmin
    public @interface Demo {
    }

    @Qualifier
    @Filter
    public @interface SomeKey {
        String value() default "somevalue";
    }

    @Qualifier
    @Filter("some-other-key")
    public @interface SomeOtherKey {
        String value() default "someothervalue";
    }

    public interface MyService {
    }

    @Inject @Service @SomeKey
    MyService service1;

    @Inject @Service @SomeKey("somevalue") @SomeOtherKey("someothervalue")
    MyService service2;

    @Inject @Service @Filter("(somekey=somevalue)")
    MyService service3;


    void serviceAdded1(@Observes @ServiceAdded MyService service) { }

    void serviceAdded2(@Observes @ServiceAdded @SomeKey MyService service) { }

    void serviceRemoved1(@Observes @ServiceRemoved MyService service) { }

    void serviceRemoved2(@Observes @ServiceRemoved @Filter("(somekey=somevalue)") MyService service) { }

}
