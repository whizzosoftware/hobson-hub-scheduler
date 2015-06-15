/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.util;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.*;

public class DateHelperTest {
    @Test
    public void testGetMillisecondsUntilMidnightGMT() {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        long now = DateHelper.getTime(2014, 7, 1, 9, 41, 0, tz);
        assertEquals(51540000, DateHelper.getMillisecondsUntilMidnight(now, tz));
    }

    @Test
    public void testGetMillisecondsUntilMidnightDefaultTZ() {
        DateTimeZone tz = DateTimeZone.getDefault();
        long now = DateHelper.getTime(2014, 7, 1, 9, 41, 0, tz);
        assertEquals(51540000, DateHelper.getMillisecondsUntilMidnight(now, tz));
    }
}
