/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.util;

import java.util.Calendar;
import java.util.TimeZone;

public class DateHelper {
    static public long getTime(TimeZone tz, int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance(tz);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static public java.util.Calendar getTimeInCurrentDay(long now, TimeZone tz, int hour, int minute, int second, int millisecond) {
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        cal.set(java.util.Calendar.SECOND, second);
        cal.set(java.util.Calendar.MILLISECOND, millisecond);
        return cal;
    }

    static public long getMillisecondsUntilMidnight(long now, TimeZone tz) {
        java.util.Calendar c = DateHelper.getTimeInCurrentDay(now, tz, 0, 0, 0, 0);
        c.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis() - now;
    }

    static public void resetToBeginningOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
}
