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

public class AstronomicalOffsetTest {
    @Test
    public void testValidConstructor() throws ParseException {
        // positive offset after sunset
        AstronomicalOffset ao = new AstronomicalOffset("SS30");
        assertEquals(30, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNSET, ao.getType());

        // negative offset after sunset
        ao = new AstronomicalOffset("SS-30");
        assertEquals(-30, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNSET, ao.getType());

        // no offset after sunset
        ao = new AstronomicalOffset("SS");
        assertEquals(0, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNSET, ao.getType());

        // no offset after sunset (with leading space)
        ao = new AstronomicalOffset(" SS");
        assertEquals(0, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNSET, ao.getType());

        // positive offset after sunset
        ao = new AstronomicalOffset("SR30");
        assertEquals(30, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNRISE, ao.getType());

        // negative offset after sunset
        ao = new AstronomicalOffset("SR-30");
        assertEquals(-30, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNRISE, ao.getType());

        // no offset after sunrise
        ao = new AstronomicalOffset("SR");
        assertEquals(0, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNRISE, ao.getType());

        // no offset after sunrise (with leading space)
        ao = new AstronomicalOffset(" SR");
        assertEquals(0, ao.getOffset());
        assertEquals(AstronomicalOffset.Type.SUNRISE, ao.getType());
    }
}
