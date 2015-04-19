package org.ops4j.pax.cdi.api;

/**
 * Exception which is thrown when a OSGi-serice invocation failed for some reason
 *
 * @author msc
 *
 */
@SuppressWarnings("serial")
public class ServiceInvocationException extends RuntimeException {

	public ServiceInvocationException() {}

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
