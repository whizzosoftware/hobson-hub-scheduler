/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class SolarHelperTest {
    @Test
    public void testSunset() throws Exception {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 9);
        c.set(Calendar.DAY_OF_MONTH, 18);
        assertEquals("20141018T181600Z-0600", SolarHelper.createDateString(c, new SolarOffset("SS"), 39.3722, -104.8561));
        assertEquals("20141018T184600Z-0600", SolarHelper.createDateString(c, new SolarOffset("SS+30"), 39.3722, -104.8561));
        assertEquals("20141018T191600Z-0600", SolarHelper.createDateString(c, new SolarOffset("SS+60"), 39.3722, -104.8561));
        assertEquals("20141018T171600Z-0600", SolarHelper.createDateString(c, new SolarOffset("SS-60"), 39.3722, -104.8561));
    }

    @Test
    public void testSunrise() throws Exception {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 9);
        c.set(Calendar.DAY_OF_MONTH, 18);
        assertEquals("20141018T071200Z-0600", SolarHelper.createDateString(c, new SolarOffset("SR"), 39.3722, -104.8561));
        assertEquals("20141018T074200Z-0600", SolarHelper.createDateString(c, new SolarOffset("SR+30"), 39.3722, -104.8561));
        assertEquals("20141018T081200Z-0600", SolarHelper.createDateString(c, new SolarOffset("SR+60"), 39.3722, -104.8561));
        assertEquals("20141018T061200Z-0600", SolarHelper.createDateString(c, new SolarOffset("SR-60"), 39.3722, -104.8561));
    }

    @Test
    public void testGetSunriseSunset() {
        String[] s = SolarHelper.getSunriseSunset(39.3722, -104.8561, 1422549445956l);
        assertEquals(2, s.length);
        assertEquals("07:10-07", s[0]);
        assertEquals("17:16-07", s[1]);
    }
}
