package org.osgi.service.cdi;

/**
 */
public class ComponentTest {

    public interface A {}

    public interface B {}

    @Component(interfaces={A.class})
    public class MyComponent implements A,B {}

    @Component(properties = {@ComponentProperty(key = "key", value = "value"), @ComponentProperty(key = "key2", value = "value2")})
    public class ExampleComponent {}

}
