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
/*
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
package org.ops4j.pax.cdi.sample7.impl;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.Dynamic;
import org.ops4j.pax.cdi.api.Global;
import org.ops4j.pax.cdi.api.Greedy;
import org.ops4j.pax.cdi.api.Optional;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.sample7.api.RankedService;
import org.ops4j.pax.cdi.sample7.api.RankedServiceClient;

/**
 * Implementation of a dynamic client for ranked services
 *
 * @author Martin Schäfer.
 *
 */
@Service @Global
public class ClientDynamic implements RankedServiceClient {

    @Inject
    @Service @Optional @Dynamic @Greedy
    private RankedService rankedService;

    @Override
    public int getServiceRanking() {
        return this.rankedService.getRanking();
    }

}
