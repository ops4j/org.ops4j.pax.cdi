/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.component;

/**
 * Handles component dependency events.
 *
 * @author Harald Wellmann
 *
 */
public interface ComponentDependencyListener {

    /**
     * Invoked when component becomes satisfied. (I.e. all required dependencies are available.)
     *
     * @param descriptor
     *            component descriptor
     * @param <S>
     *            component type
     */
    <S> void onComponentSatisfied(ComponentDescriptor<S> descriptor);

    /**
     * Invoked when component becomes unsatisfied. (I.e. at least one require dependency is not
     * available.)
     *
     * @param descriptor
     *            component descriptor
     * @param <S>
     *            component type
     */
    <S> void onComponentUnsatisfied(ComponentDescriptor<S> descriptor);
}
