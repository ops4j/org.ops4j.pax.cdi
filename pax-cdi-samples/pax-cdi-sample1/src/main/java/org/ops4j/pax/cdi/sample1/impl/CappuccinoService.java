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
package org.ops4j.pax.cdi.sample1.impl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ops4j.pax.cdi.api.BundleScoped;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.sample1.IceCreamService;

@BundleScoped
@Service
@Component
@Properties(@Property(name = "flavour", value = "cappuccino"))
public class CappuccinoService implements IceCreamService {


    public CappuccinoService() {
        System.out.println("constructing CappuccinoService");
    }

    @PostConstruct
    private void init() {
        System.out.println("CappuccinoService @PostConstruct");
    }

    @PreDestroy
    private void destroy() {
        System.out.println("CappuccinoService @PreDestroy");
    }

    @Override
    public String getFlavour() {
        return "Cappuccino";
    }
}
