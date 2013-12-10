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
package org.ops4j.pax.cdi.extension.impl;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.Service;
import org.osgi.service.cdi.ServiceType;

/**
 * An {@link AnnotationLiteral} for the {@link Service} qualifier.
 * 
 * @author Harald Wellmann
 */
@SuppressWarnings("serial")
public class OsgiServiceQualifierType extends AnnotationLiteral<Service> implements Service {

    private boolean required;
    private ServiceType type;

    public OsgiServiceQualifierType(Service qualifier) {
        this.required = qualifier.required();
        this.type = qualifier.type();
    }

    @Override
    public boolean required() {
        return required;
    }

    @Override
    public ServiceType type() {
        return type;
    }

}
