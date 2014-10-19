/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class AstronomicalDtStartTest {
    @Test
    public void testSunset() throws Exception {
        AstronomicalDtStart dta = new AstronomicalDtStart();
        assertEquals("20141018T181600Z-0600", dta.getDate("20141018T000000", "SS", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T184600Z-0600", dta.getDate("20141018T000000", "SS+30", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T191600Z-0600", dta.getDate("20141018T000000", "SS+60", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T171600Z-0600", dta.getDate("20141018T000000", "SS-60", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
    }

    @Test
    public void testSunrise() throws Exception {
        AstronomicalDtStart dta = new AstronomicalDtStart();
        assertEquals("20141018T071200Z-0600", dta.getDate("20141018T000000", "SR", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T074200Z-0600", dta.getDate("20141018T000000", "SR+30", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T081200Z-0600", dta.getDate("20141018T000000", "SR+60", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
        assertEquals("20141018T061200Z-0600", dta.getDate("20141018T000000", "SR-60", 39.3722, -104.8561, TimeZone.getTimeZone("America/Denver")));
    }

    @Test
    public void testCreateCalendar() throws Exception {
        AstronomicalDtStart dta = new AstronomicalDtStart();

        // test with SS
        Calendar c = dta.createCalendar("20141018SS+30", TimeZone.getTimeZone("America/Denver"));
        assertNotNull(c);
        assertEquals(2014, c.get(Calendar.YEAR));
        assertEquals(9, c.get(Calendar.MONTH));
        assertEquals(18, c.get(Calendar.DAY_OF_MONTH));

        // test with SR
        c = dta.createCalendar("20141018SR+30", TimeZone.getTimeZone("America/Denver"));
        assertNotNull(c);
        assertEquals(2014, c.get(Calendar.YEAR));
        assertEquals(9, c.get(Calendar.MONTH));
        assertEquals(18, c.get(Calendar.DAY_OF_MONTH));
    }
}
