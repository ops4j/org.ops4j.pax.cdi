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
package org.ops4j.pax.cdi.spi.scan;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive;

/**
 * Extends {@link AnnotationFinder} to provide access to {@link ClassInfo}.
 *
 * @author Harald Wellmann
 *
 */
public class BeanAnnotationFinder extends AnnotationFinder {

    public BeanAnnotationFinder(Archive archive) {
        super(archive);
    }

    public ClassInfo getClassInfo(String className) {
        return classInfos.get(className);
    }
}
