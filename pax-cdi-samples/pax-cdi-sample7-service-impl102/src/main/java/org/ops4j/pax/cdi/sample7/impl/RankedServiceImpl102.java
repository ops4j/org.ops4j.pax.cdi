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

import org.ops4j.pax.cdi.api.*;
import org.ops4j.pax.cdi.sample7.api.RankedService;

/**
 * Implementation of a RankedService with rank 102
 *
 * @author Martin Schäfer.
 *
 */
@Service @Component
@Properties({
        @Property(name = "service.ranking", value = "" + RankedServiceImpl102.RANKING, type = "Integer")
})
public class RankedServiceImpl102 implements RankedService {

    static final int RANKING = 102;

    @Override
    public int getRanking() {
        return RANKING;
    }
}
