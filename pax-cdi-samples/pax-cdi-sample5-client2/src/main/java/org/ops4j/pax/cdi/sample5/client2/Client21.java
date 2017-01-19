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
package org.ops4j.pax.cdi.sample5.client2;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.*;
import org.ops4j.pax.cdi.sample5.BundleScopedService;
import org.ops4j.pax.cdi.sample5.Client;
import org.ops4j.pax.cdi.sample5.PrototypeScopedService;
import org.ops4j.pax.cdi.sample5.SingletonScopedService;

@Service
@Properties(@Property(name = "name", value = "client21"))
public class Client21 implements Client {

    @Inject
    @Service @Dynamic
    private SingletonScopedService singletonScopedService1;

    @Inject
    @Service @Dynamic
    private SingletonScopedService singletonScopedService2;

    @Inject
    @Service @Dynamic
    private BundleScopedService bundleScopedService1;

    @Inject
    @Service @Dynamic
    private BundleScopedService bundleScopedService2;

    @Inject
    @Service @Dynamic
    private PrototypeScopedService prototypeScopedService1;

    @Inject
    @Service @Dynamic
    private PrototypeScopedService prototypeScopedService2;

    @Override
    public SingletonScopedService getSingletonScoped1() {
        return singletonScopedService1;
    }

    @Override
    public SingletonScopedService getSingletonScoped2() {
        return singletonScopedService2;
    }

    @Override
    public BundleScopedService getBundleScoped1() {
        return bundleScopedService1;
    }

    @Override
    public BundleScopedService getBundleScoped2() {
        return bundleScopedService2;
    }

    @Override
    public PrototypeScopedService getPrototypeScoped1() {
        return prototypeScopedService1;
    }

    @Override
    public PrototypeScopedService getPrototypeScoped2() {
        return prototypeScopedService2;
    }
}
