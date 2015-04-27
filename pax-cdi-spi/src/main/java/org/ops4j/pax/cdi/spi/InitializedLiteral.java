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
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Annotation literal for {@link Initialized}.
 *
 * @author Jozef Hartinger
 *
 */
public class InitializedLiteral extends AnnotationLiteral<Initialized> implements Initialized {

    public static final InitializedLiteral REQUEST = new InitializedLiteral(RequestScoped.class);
    public static final InitializedLiteral CONVERSATION = new InitializedLiteral(ConversationScoped.class);
    public static final InitializedLiteral SESSION = new InitializedLiteral(SessionScoped.class);
    public static final InitializedLiteral APPLICATION = new InitializedLiteral(ApplicationScoped.class);

    private static final long serialVersionUID = -9014737143348998988L;

    private Class<? extends Annotation> value;

    private InitializedLiteral(Class<? extends Annotation> value) {
        this.value = value;
    }

    @Override
    public Class<? extends Annotation> value() {
        return value;
    }
}
