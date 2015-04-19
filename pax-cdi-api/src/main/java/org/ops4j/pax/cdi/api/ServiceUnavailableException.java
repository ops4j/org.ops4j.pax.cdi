package org.ops4j.pax.cdi.api;

/**
 * Exception which is thrown when an OSGi service is unavailable for invocation.
 *
 * @author msc
 *
 */
public class ServiceUnavailableException extends ServiceInvocationException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException() {
    }

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
