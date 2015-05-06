/*
 * Copyright 2015 Harald Wellmann.
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
package org.ops4j.pax.cdi.extension.impl.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Implements {@link ParameterizedType}, since there is no public implementation in the JRE.
 *
 * @author Harald Wellmann
 *
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private Type owner;
    private Type rawType;
    private Type[] actualTypes;

    /**
     * Creates a parameterized type with given owner, raw type and actual type arguments.
     *
     * @param owner
     *            enclosing class, or null
     * @param rawType
     *            raw type (without any arguments)
     * @param actualTypes
     *            actual type arguments
     */
    public ParameterizedTypeImpl(Type owner, Type rawType, Type... actualTypes) {
        this.owner = owner;
        this.rawType = rawType;
        this.actualTypes = actualTypes;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypes;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return owner;
    }
}
