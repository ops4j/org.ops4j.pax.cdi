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
package org.ops4j.pax.cdi.api2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides version information about this release of Pax CDI.
 *
 * Utility class with static methods only.
 *
 * @author Harald Wellmann
 */
public class Info {

    /**
     * Snapshot constant to avoid typos.
     */
    private static final String SNAPSHOT = "SNAPSHOT";

    /**
     * Pax Web version.
     */
    private static final String PAX_WEB_VERSION;

    /**
     * Jetty version.
     */
    private static final String JETTY_VERSION;

    /**
     * Pax CDI version.
     */
    private static final String PAX_CDI_VERSION;

    /**
     * True if Pax Web is a snapshot version.
     */
    private static boolean paxWebSnapshotVersion;

    /**
     * True if Pax CDI is a snapshot version.
     */
    private static boolean paxCdiSnapshotVersion;

    static {
        String paxCdiVersion = "";
        String paxWebVersion = "";
        String jettyVersion = "";
        try {
            InputStream is = Info.class.getResourceAsStream("/META-INF/pax-cdi-version.properties");
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                paxCdiVersion = readProperty(properties, "pax.cdi.version");
                paxWebVersion = readProperty(properties, "pax.web.version");
                jettyVersion = readProperty(properties, "jetty.version");
            }
        }
        catch (IOException exc) {
            throw new IllegalStateException("cannot read META-INF/pax-cdi-version.properties", exc);
        }
        PAX_CDI_VERSION = paxCdiVersion;
        paxCdiSnapshotVersion = paxCdiVersion.endsWith(SNAPSHOT);
        PAX_WEB_VERSION = paxWebVersion;
        paxWebSnapshotVersion = paxWebVersion.endsWith(SNAPSHOT);
        JETTY_VERSION = jettyVersion;
    }

    /**
     * No instances should be made (does not make sense).
     */
    private Info() {

    }

    private static String readProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(
                key + " missing in META-INF/pax-cdi-version.properties");
        }
        return value;

    }

    /**
     * Discovers the Pax CDI version. If version cannot be determined returns an empty string.
     *
     * @return Pax CDI version
     */
    public static String getPaxCdiVersion() {
        return PAX_CDI_VERSION;
    }

    /**
     * Getter.
     *
     * @return true if Pax CDI is a snapshot version, false otherwise
     */
    public static boolean isPaxCdiSnapshotVersion() {
        return paxCdiSnapshotVersion;
    }

    /**
     * Discovers the Pax Web version. If version cannot be determined returns an empty string.
     *
     * @return Pax Web version
     */
    public static String getPaxWebVersion() {
        return PAX_WEB_VERSION;
    }

    /**
     * Discovers the Jetty version. If version cannot be determined returns an empty string.
     *
     * @return Jetty version
     */
    public static String getJettyVersion() {
        return JETTY_VERSION;
    }

    /**
     * Getter.
     *
     * @return true if Pax Web is a snapshot version, false otherwise
     */
    public static boolean isPaxWebSnapshotVersion() {
        return paxWebSnapshotVersion;
    }
}
