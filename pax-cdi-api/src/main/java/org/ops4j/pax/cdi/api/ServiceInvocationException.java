package org.ops4j.pax.cdi.api;

/**
 * Exception which is thrown when an OSGi service invocation failed for some reason.
 *
 * @author msc
 *
 */
public class ServiceInvocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceInvocationException() {
    }

    public ServiceInvocationException(String message) {
        super(message);
    }

    public ServiceInvocationException(Throwable cause) {
        super(cause);
    }

    public ServiceInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
