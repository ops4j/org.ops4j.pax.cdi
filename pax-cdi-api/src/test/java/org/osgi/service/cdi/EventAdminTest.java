package org.osgi.service.cdi;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.event.EventAdmin;

/**
 */
public class EventAdminTest {

    /** Demo event annotation */
    @EventAdmin
    public @interface Demo {
    }

    @Filter
    public @interface Replay {
        String value() default "true";
    }

    /** Event definition */
    public static class MyEvent {
        private final String value;
        public MyEvent(String value) {
            this.value = value;
        }
    }

    /** Receive events with specific annotation */
    public void process1(@Observes @Demo MyEvent event) {}

    /** Receive events with basic annotation */
    public void process2(@Observes @EventAdmin("Demo") MyEvent event) {}

    /** Receive filtered events */
    public void process3(@Observes @Demo @Filter("replay=true") MyEvent event) {}

    /** Receive filtered events */
    public void process4(@Observes @Demo @Replay MyEvent event) {}

    /** Send event with specific annotation */
    @Inject
    @Demo
    Event<MyEvent> event1;
    public void send1() {
        event1.fire(new MyEvent("example"));
    }

    /** Send event with basic annotation */
    @Inject
    @EventAdmin("Demo")
    Event<MyEvent> event2;
    public void send2() {
        event2.fire(new MyEvent("example"));
    }

}
