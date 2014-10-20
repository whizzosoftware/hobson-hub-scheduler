/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.util;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.whizzosoftware.hobson.scheduler.ical.SolarOffset;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A start date/time relative to sunrise or sunset.
 *
 * @author Dan Noguerol
 */
public class SolarHelper {
    public static String createDateString(Calendar c, SolarOffset offset, double latitude, double longitude) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'Z");
        return dateFormat.format(createCalendar(c, offset, latitude, longitude).getTime());
    }

    /**
     * Creates a new Calendar based on a start date/time and a solar offset. The new Calendar will be on the same
     * date as the startDateTime argument but its time will be determined by the solar offset (a certain number of
     * minutes before or after sunrise or sunset).
     *
     * @param startDateTime the calendar representing the start date/time
     * @param offset the solar offset to apply to the startDateTime
     * @param latitude the location's latitude
     * @param longitude the location's longitude
     *
     * @return a Calendar object
     *
     * @throws ParseException on failure
     */
    public static Calendar createCalendar(Calendar startDateTime, SolarOffset offset, double latitude, double longitude) throws ParseException {
        Calendar newCal;

        // perform the sunrise or sunset calculation
        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(new Location(latitude, longitude), startDateTime.getTimeZone());
        if (offset.getType() == SolarOffset.Type.SUNSET) {
            newCal = calc.getOfficialSunsetCalendarForDate(startDateTime);
        } else {
            newCal = calc.getOfficialSunriseCalendarForDate(startDateTime);
        }

        // add the offset
        newCal.add(Calendar.MINUTE, offset.getOffset());

        // create and return a DtStart object with the offset time
        return newCal;
    }
}
