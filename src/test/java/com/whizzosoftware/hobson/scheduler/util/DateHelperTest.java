/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.util;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.*;

public class DateHelperTest {
    @Test
    public void testGetMillisecondsUntilMidnightGMT() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        long now = DateHelper.getTime(tz, 2014, 7, 1, 9, 41, 0);
        assertEquals(51540000, DateHelper.getMillisecondsUntilMidnight(now, tz));
    }

    @Test
    public void testGetMillisecondsUntilMidnightDefaultTZ() {
        TimeZone tz = TimeZone.getDefault();
        long now = DateHelper.getTime(tz, 2014, 7, 1, 9, 41, 0);
        assertEquals(51540000, DateHelper.getMillisecondsUntilMidnight(now, tz));
    }
}
