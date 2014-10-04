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

import javax.enterprise.util.AnnotationLiteral;

import org.ops4j.pax.cdi.api.OsgiService;

/**
 * An {@link AnnotationLiteral} for the {@link OsgiService} qualifier.
 * 
 * @author Harald Wellmann
 */
@SuppressWarnings("serial")
public class OsgiServiceQualifierType extends AnnotationLiteral<OsgiService> implements OsgiService {

    private boolean dynamic;
    private boolean required;
    private String filter = "";
    private int timeout;

    public OsgiServiceQualifierType(OsgiService qualifier) {
        this.dynamic = qualifier.dynamic();
        this.required = qualifier.required();
        this.filter = qualifier.filter();
        this.timeout = qualifier.timeout();
    }

    @Override
    public String filter() {
        return filter;
    }

    @Override
    public boolean dynamic() {
        return dynamic;
    }

    @Override
    public boolean required() {
        return required;
    }

    @Override
    public int timeout() {
        return timeout;
    }
}
