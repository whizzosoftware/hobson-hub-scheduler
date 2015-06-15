/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateHelper {
    static public long getTime(int year, int month, int day, int hour, int minute, int second, DateTimeZone tz) {
        return new DateTime(year, month, day, hour, minute, second, 0, tz).getMillis();
    }

    static public DateTime getTimeInCurrentDay(long now, DateTimeZone tz, int hour, int minute, int second, int millisecond) {
        return new DateTime(now, tz).withTime(hour, minute, second, millisecond);
    }

    static public long getMillisecondsUntilMidnight(long now, DateTimeZone tz) {
        return new DateTime(now, tz).plusDays(1).withTimeAtStartOfDay().getMillis() - now;
    }
}
