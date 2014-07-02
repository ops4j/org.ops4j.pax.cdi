/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.undertow.weld.impl;

import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.jboss.weld.Container;
import org.jboss.weld.jsf.ConversationAwareViewHandler;

/**
 * Wraps {@link ConversationAwareViewHandler} to copy the CDI context ID from the
 * servlet context to the faces context.
 * 
 * @author Harald Wellmann
 *
 */
public class ConversationAndContextAwareViewHandler extends ConversationAwareViewHandler {
    
    private String contextId;

    public ConversationAndContextAwareViewHandler(ViewHandler delegate) {
        super(delegate);
    }

    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        if (contextId == null) {
            ServletContext sc = (ServletContext) facesContext.getExternalContext().getContext();
            contextId = sc.getInitParameter(Container.CONTEXT_ID_KEY);
            facesContext.getAttributes().put(Container.CONTEXT_ID_KEY, contextId);
        }
        return super.getActionURL(facesContext, viewId);
    }
}
