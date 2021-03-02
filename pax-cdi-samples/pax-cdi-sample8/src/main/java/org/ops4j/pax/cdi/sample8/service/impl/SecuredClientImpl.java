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
package org.ops4j.pax.cdi.sample8.service.impl;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.sample8.service.SecuredClient;
import org.ops4j.pax.cdi.sample8.service.SecuredService;

@Service
@Component
public class SecuredClientImpl implements SecuredClient {

    @Inject
    @Service
    private SecuredService service;

    @Override
    public String getBlockedResult() {
        return service.getBlockedResult();
    }

    @Override
    public String getResult() {
        return service.getResult();
    }
}
