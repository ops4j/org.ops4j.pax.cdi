package org.ops4j.pax.cdi.api;

/**
 * Exception which is thrown when a OSGi-serice is unavailable for invocation
 *
 * @author msc
 *
 */
@SuppressWarnings("serial")
public class ServiceUnavailableException extends ServiceInvocationException {
	
	public ServiceUnavailableException() {}
	
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
