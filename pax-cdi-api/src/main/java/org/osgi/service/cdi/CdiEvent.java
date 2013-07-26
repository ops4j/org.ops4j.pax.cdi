/*
 * Copyright (c) OSGi Alliance (2011, 2013). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.cdi;

import org.osgi.framework.Bundle;

/**
 * CdiEvent are sent by the cdi extender and received by
 * registered CdiListener services.
 *
 * @see CdiListener
 */
public class CdiEvent {

    public static final int	CREATING		= 1;
    public static final int	CREATED			= 2;
    public static final int	DESTROYING		= 3;
    public static final int	DESTROYED		= 4;
    public static final int	FAILURE			= 5;


    private final int		type;
    /**
     * The time when the event occurred.
     *
     * @see #getTimestamp()
     */
    private final long		timestamp;
    /**
     * The Switchyard bundle.
     *
     * @see #getBundle()
     */
    private final Bundle bundle;
    /**
     * The Switchyard extender bundle.
     *
     * @see #getExtenderBundle()
     */
    private final Bundle	extenderBundle;
    /**
     * Cause of the failure.
     *
     * @see #getCause()
     */
    private final Throwable	cause;
    /**
     * Indicate if this event is a replay event or not.
     *
     * @see #isReplay()
     */
    private final boolean	replay;

    public CdiEvent(int type, Bundle bundle, Bundle extenderBundle) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.bundle = bundle;
        this.extenderBundle = extenderBundle;
        this.cause = null;
        this.replay = false;
    }

    public CdiEvent(int type, Bundle bundle, Bundle extenderBundle, Throwable cause) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.bundle = bundle;
        this.extenderBundle = extenderBundle;
        this.cause = cause;
        this.replay = false;
    }

    public CdiEvent(CdiEvent event, boolean replay) {
        this.type = event.type;
        this.timestamp = event.timestamp;
        this.bundle = event.bundle;
        this.extenderBundle = event.extenderBundle;
        this.cause = event.cause;
        this.replay = replay;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Bundle getExtenderBundle() {
        return extenderBundle;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isReplay() {
        return replay;
    }
}
