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
package org.ops4j.pax.cdi.sample6.impl;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Dynamic;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.sample6.MessageConsumer;
import org.ops4j.pax.cdi.sample6.MessageProducer;

@Service @Component
public class MessageConsumerImpl implements MessageConsumer {

    @Inject
    @Service @Dynamic
    private MessageProducer producer;

    @Override
    public String consume() {
        return producer.getMessage();
    }
}
