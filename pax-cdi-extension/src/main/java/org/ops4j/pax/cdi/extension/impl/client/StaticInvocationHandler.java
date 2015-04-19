/*
 * Copyright 2012 Harald Wellmann.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.osgi.framework.ServiceException;

/**
 * A static proxy invocation handler which always uses the same service reference obtained on
 * construction.
 *
 * @author Harald Wellmann
 *
 */
public class StaticInvocationHandler<S> extends AbstractServiceInvocationHandler<S> {
	
	private S	service;
	
	public StaticInvocationHandler(InjectionPoint ip) {
		super(ip);
		this.service = this.serviceObjects.getService();
	}
	
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			// CHECKSTYLE:SKIP
			throws Throwable {
		
		if (this.serviceRef != null) {
			Object result = ContextClassLoaderUtils.doWithClassLoader(
					this.cdiContainer.getContextClassLoader(), new Callable<Object>() {
						
						@Override
						public Object call() throws Exception {
							if (StaticInvocationHandler.this.service != null) {
								return method.invoke(StaticInvocationHandler.this.service, args);
							}
							return null;
						}
					});
			return result;
		}
		throw new ServiceException("no service for injection point " + this.ip,
				ServiceException.UNREGISTERED);
	}
	
	@Override
	public void release() {
		this.serviceObjects.ungetService(this.service);
	}
}
