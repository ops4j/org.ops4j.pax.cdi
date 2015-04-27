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
package org.ops4j.pax.cdi.spi;


import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Annotation literal for {@link Destroyed}.
 *
 * @author Jozef Hartinger
 *
 */
public class DestroyedLiteral extends AnnotationLiteral<Destroyed> implements Destroyed {

    public static final DestroyedLiteral REQUEST = new DestroyedLiteral(RequestScoped.class);
    public static final DestroyedLiteral CONVERSATION = new DestroyedLiteral(ConversationScoped.class);
    public static final DestroyedLiteral SESSION = new DestroyedLiteral(SessionScoped.class);
    public static final DestroyedLiteral APPLICATION = new DestroyedLiteral(ApplicationScoped.class);

    private static final long serialVersionUID = -8560340388621930476L;

    private Class<? extends Annotation> value;

    private DestroyedLiteral(Class<? extends Annotation> value) {
        this.value = value;
    }

    @Override
    public Class<? extends Annotation> value() {
        return value;
    }
}
