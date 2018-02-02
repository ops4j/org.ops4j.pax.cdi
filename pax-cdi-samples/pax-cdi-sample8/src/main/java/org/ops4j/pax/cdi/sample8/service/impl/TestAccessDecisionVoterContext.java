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
package org.ops4j.pax.cdi.sample8.service.impl;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.security.impl.authorization.DefaultAccessDecisionVoterContext;

/**
 * We don't have a request scope in a non-web setup, so we override the default bean by
 * specialization.
 *
 * @author Harald Wellmann
 *
 */
//@Specializes
@ApplicationScoped
public class TestAccessDecisionVoterContext extends DefaultAccessDecisionVoterContext {

    public TestAccessDecisionVoterContext() {
        System.out.println();
    }

}
