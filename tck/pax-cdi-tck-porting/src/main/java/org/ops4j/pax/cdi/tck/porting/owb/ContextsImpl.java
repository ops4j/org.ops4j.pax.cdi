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
package org.ops4j.pax.cdi.tck.porting.owb;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.context.RequestContext;
import org.jboss.cdi.tck.spi.Contexts;

public class ContextsImpl implements Contexts<AbstractContext> {

    @Override
    public AbstractContext getRequestContext() {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();

        BeanManager beanManager = webBeansContext.getBeanManagerImpl();
        RequestContext ctx = (RequestContext) beanManager.getContext(RequestScoped.class);

        if (ctx == null) {
            webBeansContext.getContextsService().startContext(RequestScoped.class, null);
            ctx = (RequestContext) beanManager.getContext(RequestScoped.class);
        }

        return ctx;
    }

    @Override
    public void setActive(AbstractContext context) {
        context.setActive(true);
    }

    @Override
    public void setInactive(AbstractContext context) {
        context.setActive(false);
    }

    @Override
    public AbstractContext getDependentContext() {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();

        return (AbstractContext) webBeansContext.getBeanManagerImpl().getContext(Dependent.class);
    }

    @Override
    public void destroyContext(AbstractContext context) {
        context.destroy();
    }
}
