/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import net.fortuna.ical4j.model.property.DtStart;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * A start date/time relative to sunrise or sunset.
 *
 * @author Dan Noguerol
 */
public class AstronomicalDtStart {
    private DateFormat format = new SimpleDateFormat("yyyyMMdd");
    private DateFormat format2 = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'Z");

    /**
     * Creates a DtStart object for the sunset/sunrise offset time
     *
     * @param aValue the date string
     * @param latitude the location's latitude
     * @param longitude the location's longitude
     * @param timezone the timezone to use for the calculation
     *
     * @return a DtStart object
     *
     * @throws ParseException on failure
     */
    public String getDate(String aValue, String sunOffset, double latitude, double longitude, TimeZone timezone) throws ParseException {
        Calendar c2;

        // parse the astronomical offset
        AstronomicalOffset offset = new AstronomicalOffset(sunOffset);

        // get the date from the string argument
        Calendar c = createCalendar(aValue, timezone);

        // perform the sunrise or sunset calculation
        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(new Location(latitude, longitude), timezone);
        if (offset.getType() == AstronomicalOffset.Type.SUNSET) {
            c2 = calc.getOfficialSunsetCalendarForDate(c);
        } else {
            c2 = calc.getOfficialSunriseCalendarForDate(c);
        }

        // add the offset
        c2.add(Calendar.MINUTE, offset.getOffset());

        // create and return a DtStart object with the offset time
        return format2.format(c2.getTime());
    }

    protected Calendar createCalendar(String dateString, TimeZone timezone) throws ParseException {
        Calendar c = Calendar.getInstance(timezone);
        c.setTime(format.parse(dateString.substring(0, 8)));
        return c;
    }
}
