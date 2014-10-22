/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.scheduler.ical.ICalTriggerProvider;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Hashtable;

public class SchedulerPluginTest {
    @Test
    public void testApplyProviderConfig() {
        SchedulerPlugin p = new SchedulerPlugin("id");
        Hashtable config = new Hashtable();
        config.put("latitude", "120");
        config.put("longitude", "130");
        ICalTriggerProvider provider = new ICalTriggerProvider("id");
        assertNull(provider.getLatitude());
        assertNull(provider.getLongitude());
        p.applyProviderConfig(provider, config, false);
        assertEquals(120, provider.getLatitude(), 0);
        assertEquals(130, provider.getLongitude(), 0);
    }

    @Test
    public void testApplyProviderConfigEmpty() {
        SchedulerPlugin p = new SchedulerPlugin("id");
        Hashtable config = new Hashtable();
        ICalTriggerProvider provider = new ICalTriggerProvider("id");
        assertNull(provider.getLatitude());
        assertNull(provider.getLongitude());
        p.applyProviderConfig(provider, config, false);
        assertNull(provider.getLatitude());
        assertNull(provider.getLongitude());
    }
}
