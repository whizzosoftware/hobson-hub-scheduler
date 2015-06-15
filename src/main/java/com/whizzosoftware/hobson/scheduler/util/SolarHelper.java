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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

/**
 * A start date/time relative to sunrise or sunset.
 *
 * @author Dan Noguerol
 */
public class SolarHelper {
    public static String createDateString(LocalDate c, DateTimeZone tz, SolarOffset offset, double latitude, double longitude) throws ParseException {
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'Z");
        return dateFormat.print(createCalendar(c, tz, offset, latitude, longitude));
    }

    /**
     * Returns the sunrise/sunset calendars for a particular day and geographic location.
     *
     * @param today a Calendar for the day
     * @param latitude the latitude
     * @param longitude the longitude
     *
     * @return a SunriseSunsetCalendar instance
     */
    public static SunriseSunsetCalendar getSunriseSunsetCalendar(LocalDate today, DateTimeZone tz, double latitude, double longitude) {
        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(new Location(latitude, longitude), tz.toTimeZone());
        Calendar c = today.toDateTimeAtStartOfDay(tz).toCalendar(Locale.ENGLISH);
        return new SunriseSunsetCalendar(new DateTime(calc.getOfficialSunriseCalendarForDate(c)), new DateTime(calc.getOfficialSunsetCalendarForDate(c)));
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
    public static DateTime createCalendar(LocalDate startDateTime, DateTimeZone tz, SolarOffset offset, double latitude, double longitude) throws ParseException {
        DateTime newCal;

        // perform the sunrise or sunset calculation
        SunriseSunsetCalendar ssc = getSunriseSunsetCalendar(startDateTime, tz, latitude, longitude);
        if (offset.getType() == SolarOffset.Type.SUNSET) {
            newCal = ssc.getSunset();
        } else {
            newCal = ssc.getSunrise();
        }

        // add the offset
        newCal = newCal.plusMinutes(offset.getOffset());

        // create and return a DtStart object with the offset time
        return newCal;
    }

    public static String[] getSunriseSunset(Double latitude, Double longitude, DateTimeZone tz, long now) {
        if (latitude != null && longitude != null) {
            DateTime c = new DateTime(now, tz);

            SolarHelper.SunriseSunsetCalendar ssc = SolarHelper.getSunriseSunsetCalendar(c.toLocalDate(), tz, latitude, longitude);
            DateTimeFormatter df = DateTimeFormat.forPattern("HH:mmZ");

            String sunrise = df.print(ssc.getSunrise());
            String sunset = df.print(ssc.getSunset());
            return new String[]{sunrise, sunset};
        } else {
            return null;
        }
    }

    public static class SunriseSunsetCalendar {
        private DateTime sunrise;
        private DateTime sunset;

        public SunriseSunsetCalendar(DateTime sunrise, DateTime sunset) {
            this.sunrise = sunrise;
            this.sunset = sunset;
        }

        public DateTime getSunrise() {
            return sunrise;
        }

        public DateTime getSunset() {
            return sunset;
        }
    }
}
