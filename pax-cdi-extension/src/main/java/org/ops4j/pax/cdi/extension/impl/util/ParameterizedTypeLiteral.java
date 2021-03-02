/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension.impl.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import javax.enterprise.util.TypeLiteral;

import org.ops4j.pax.cdi.spi.util.Exceptions;

/**
 * Extends {@link TypeLiteral} to dynamically set a parameterized type which is not known at compile
 * time.
 * <p>
 * FIXME since {@link TypeLiteral#getType()} is final, we have to use reflection to override the
 * type.
 *
 * @author Harald Wellmann
 * @see <a href="https://issues.jboss.org/browse/CDI-516">CDI-516</a>
 */
public class ParameterizedTypeLiteral extends TypeLiteral<Object> {

    private static final long serialVersionUID = 1L;

    private static Field actualTypeField;

    static {
        for (Field field : TypeLiteral.class.getDeclaredFields()) {
            if (field.getType().equals(Type.class)) {
                field.setAccessible(true);
                actualTypeField = field;
                break;
            }
        }
    }

    /**
     * Creates a type literal for a parameterized type with the given properties.
     *
     * @param rawType
     *            top-level raw type
     * @param typeArguments
     *            arguments of parameterized type
     */
    public ParameterizedTypeLiteral(Class<?> rawType, Type... typeArguments) {
        this(null, rawType, typeArguments);
    }

    /**
     * Creates a type literal for a parameterized type with the given properties.
     *
     * @param owner
     *            owner of parameterized type
     * @param rawType
     *            raw type
     * @param typeArguments
     *            arguments of parameterized type
     */
    public ParameterizedTypeLiteral(Type owner, Type rawType, Type... typeArguments) {
        ParameterizedTypeImpl paramType = new ParameterizedTypeImpl(owner, rawType, typeArguments);
        try {
            actualTypeField.set(this, paramType);
        }
        catch (IllegalArgumentException | IllegalAccessException exc) {
            throw Exceptions.unchecked(exc);
        }
    }
}
