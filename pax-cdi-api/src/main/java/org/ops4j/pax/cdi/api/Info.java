/*
 * Copyright 2012 Harald Wellmann
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides version information about this release of Pax CDI.
 * 
 * Fully static
 * 
 * @author Harald Wellmann
 */
public class Info {

    /**
     * Snapshot constant to avoid typos.
     */
    private static final String SNAPSHOT = "SNAPSHOT";

    /**
     * Pax CDI version.
     */
    private static final String PAX_CDI_VERSION;

    /**
     * True if Pax CDI is a snapshot version.
     */
    private static boolean paxCdiSnapshotVersion;

    static {
        String paxCdiVersion = "";
        try {
            InputStream is = Info.class.getResourceAsStream("/META-INF/pax-cdi-version.properties");
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                paxCdiVersion = properties.getProperty("pax.cdi.version");
                if (paxCdiVersion == null) {
                    throw new IllegalStateException(
                        "pax.cdi.version missing in META-INF/pax-cdi-version.properties");
                }
            }
        }
        catch (IOException ignore) {
            throw new IllegalStateException("cannot read META-INF/pax-cdi-version.properties");
        }
        PAX_CDI_VERSION = paxCdiVersion;
        paxCdiSnapshotVersion = paxCdiVersion.endsWith(SNAPSHOT);
    }

    /**
     * No instances should be made (does not make sense).
     */
    private Info() {

    }

    /**
     * Discovers the Pax Exam version. If version cannot be determined returns an empty string.
     * 
     * @return pax exam version
     */
    public static String getPaxCdiVersion() {
        return PAX_CDI_VERSION;
    }

    /**
     * Getter.
     * 
     * @return true if pax exam is a snapshot version, false otherwise
     */
    public static boolean isPaxCdiSnapshotVersion() {
        return paxCdiSnapshotVersion;
    }
}
