/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.*;

public class SolarHelperTest {
    @Test
    public void testSunset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        LocalDate c = LocalDate.parse("2014-10-18");
        assertEquals("20141018T181600Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SS"), 39.3722, -104.8561));
        assertEquals("20141018T184600Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SS+30"), 39.3722, -104.8561));
        assertEquals("20141018T191600Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SS+60"), 39.3722, -104.8561));
        assertEquals("20141018T171600Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SS-60"), 39.3722, -104.8561));
    }

    @Test
    public void testSunrise() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        LocalDate c = new LocalDate(2014, 10, 18);
        assertEquals("20141018T071200Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SR"), 39.3722, -104.8561));
        assertEquals("20141018T074200Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SR+30"), 39.3722, -104.8561));
        assertEquals("20141018T081200Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SR+60"), 39.3722, -104.8561));
        assertEquals("20141018T061200Z-0600", SolarHelper.createDateString(c, tz, new SolarOffset("SR-60"), 39.3722, -104.8561));
    }

    @Test
    public void testGetSunriseSunset() {
        String[] s = SolarHelper.getSunriseSunset(39.3722, -104.8561, DateTimeZone.forID("America/Denver"), 1422549445956l);
        assertEquals(2, s.length);
        assertEquals("07:10-0700", s[0]);
        assertEquals("17:16-0700", s[1]);
    }
}
