/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import org.junit.Test;
import static org.junit.Assert.*;

import java.text.ParseException;

public class SolarOffsetTest {
    @Test
    public void testValidConstructor() throws ParseException {
        // positive offset after sunset
        SolarOffset ao = new SolarOffset("SS30");
        assertEquals(30, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNSET, ao.getType());

        // negative offset after sunset
        ao = new SolarOffset("SS-30");
        assertEquals(-30, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNSET, ao.getType());

        // no offset after sunset
        ao = new SolarOffset("SS");
        assertEquals(0, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNSET, ao.getType());

        // no offset after sunset (with leading space)
        ao = new SolarOffset(" SS");
        assertEquals(0, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNSET, ao.getType());

        // positive offset after sunset
        ao = new SolarOffset("SR30");
        assertEquals(30, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNRISE, ao.getType());

        // negative offset after sunset
        ao = new SolarOffset("SR-30");
        assertEquals(-30, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNRISE, ao.getType());

        // no offset after sunrise
        ao = new SolarOffset("SR");
        assertEquals(0, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNRISE, ao.getType());

        // no offset after sunrise (with leading space)
        ao = new SolarOffset(" SR");
        assertEquals(0, ao.getOffset());
        assertEquals(SolarOffset.Type.SUNRISE, ao.getType());
    }
}
