/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.scheduler.MockActionManager;
import com.whizzosoftware.hobson.scheduler.executor.MockScheduledTriggerExecutor;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.TimeZone;

public class ICalTriggerProviderTest {
    @Test
    public void testloadCalendarWithNoExecutor() {
        ICalTriggerProvider scheduler = new ICalTriggerProvider();
        String s = "";
        try {
            scheduler.loadICSStream(new ByteArrayInputStream(s.getBytes()), System.currentTimeMillis());
            fail("Should have thrown an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testEmptyCalendarFile() throws Exception {
        File sfile = File.createTempFile("hobsonschedule", ".ics");
        sfile.delete();
        try {
            ICalTriggerProvider scheduler = new ICalTriggerProvider();
            scheduler.setScheduleExecutor(new MockScheduledTriggerExecutor());
            scheduler.setScheduleFile(sfile);
        } finally {
            sfile.delete();
        }
    }

    @Test
    public void testClearAllTasks() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();

        // make sure executor has no delays already set
        assertFalse(executor.hasDelays());

        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:uid1@example.com\n" +
                "DTSTART:20130714T170000Z\n" +
                "DTEND:20130714T170000Z\n" +
                "SUMMARY:My Task\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long startOfDay = DateHelper.getTime(tz, 2013, 7, 14, 0, 0, 0);

        ICalTriggerProvider s = new ICalTriggerProvider(tz);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), startOfDay);

        // confirm executor has a delay
        assertTrue(executor.hasDelays());

        s.clearAllTasks();

        // confirm executor has no delays
        assertFalse(executor.hasDelays());
    }

    @Test
    public void testLoadScheduleWithSingleEvent() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:uid1@example.com\n" +
                "DTSTART:20130714T170000Z\n" +
                "DTEND:20130714T170000Z\n" +
                "SUMMARY:My Task\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // assert what happens the day OF the event
        long startOfDay = DateHelper.getTime(tz, 2013, 7, 14, 0, 0, 0);

        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();
        ICalTriggerProvider s = new ICalTriggerProvider(tz);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), startOfDay);

        // verify task was created
        assertEquals(1, s.getTriggers().size());
        ICalTrigger t = (ICalTrigger)s.getTriggers().iterator().next();
        assertEquals("My Task", t.getName());

        // verify task was scheduled -- should have been scheduled to execute in 61200 seconds (17 hours)
        assertEquals(61200000, (long)executor.getDelayForTask(t));

        // assert what happens the day AFTER the event
        startOfDay = DateHelper.getTime(tz, 2013, 7, 15, 0, 0, 0);
        executor.clearDelays();
        s.resetForNewDay(startOfDay);

        // verify the task was created but not scheduled
        assertEquals(1, s.getTriggers().size());

        // verify task was not scheduled
        assertFalse(executor.isTriggerScheduled((ICalTrigger)s.getTriggers().iterator().next()));
        assertFalse(executor.hasDelays());
        assertNull(executor.getDelayForTask(t));
    }

    @Test
    public void testLoadScheduleWithSingleEventWithSunsetOffset() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("America/Denver");
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:uid1@example.com\n" +
                "DTSTART:20141018T000000\n" +
                "SUMMARY:My Task\n" +
                "X-SUN-OFFSET: SS30\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long startOfDay = DateHelper.getTime(tz, 2014, 10, 18, 0, 0, 0);

        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();
        ICalTriggerProvider s = new ICalTriggerProvider(tz);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), startOfDay);

        // verify task was created
        assertEquals(1, s.getTriggers().size());
        ICalTrigger t = (ICalTrigger)s.getTriggers().iterator().next();
        assertEquals("My Task", t.getName());

        // verify task was scheduled -- should have been scheduled to execute in 61200 seconds (17 hours)
        assertEquals(67560000, (long)executor.getDelayForTask(t));
    }

    @Test
    public void testDayReset() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:uid1@example.com\n" +
                "DTSTART:20140701T090000Z\n" +
                "RRULE:FREQ=DAILY\n" +
                "SUMMARY:My Task\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'Test'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long schedulerStart = DateHelper.getTime(tz, 2014, 7, 1, 8, 0, 0);

        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();
        MockActionManager actionContext = new MockActionManager();
        ICalTriggerProvider s = new ICalTriggerProvider(tz);
        s.setScheduleExecutor(executor);
        s.setActionManager(actionContext);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), schedulerStart);

        // verify task was created but not run
        assertEquals(1, s.getTriggers().size());
        assertEquals(0, actionContext.getLogCalls());

        // reload the file at midnight
        s.resetForNewDay(DateHelper.getTime(tz, 2014, 7, 2, 0, 0, 0));

        // verify task was created but not executed
        assertEquals(1, s.getTriggers().size());
        assertEquals(0, actionContext.getLogCalls());
    }

    @Test
    public void testDelayedDayReset() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:uid1@example.com\n" +
                "DTSTART:20140701T000000Z\n" +
                "RRULE:FREQ=DAILY\n" +
                "SUMMARY:My Task\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'Test'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler after the task should have run
        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();
        MockActionManager actionManager = new MockActionManager();
        ICalTriggerProvider s = new ICalTriggerProvider(tz);
        s.setScheduleExecutor(executor);
        s.setActionManager(actionManager);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 17, 0, 0));

        // verify task was not scheduled
        assertEquals(1, s.getTriggers().size());
        assertFalse(executor.isTriggerScheduled((ICalTrigger)s.getTriggers().iterator().next()));
        assertEquals(0, actionManager.getLogCalls());

        // start a new day 30 seconds after midnight -- this covers the corner case where a delay causes the
        // resetForNewDay() method to get fired slightly after midnight and there are tasks that should have
        // already executed by then
        s.resetForNewDay(DateHelper.getTime(tz, 2014, 7, 2, 0, 0, 30));

        // verify task was not scheduled but task executed
        assertEquals(1, s.getTriggers().size());
        assertFalse(executor.isTriggerScheduled((ICalTrigger)s.getTriggers().iterator().next()));
        assertEquals(1, actionManager.getLogCalls());
    }

    @Test
    public void testTaskRescheduling() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // an event that runs every minute
        String ical = "BEGIN:VCALENDAR\n" +
                "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
                "VERSION:2.0\n" +
                "CALSCALE:GREGORIAN\n" +
                "BEGIN:VJOURNAL\n" +
                "DTSTAMP:20140701T044038Z\n" +
                "DTSTART;VALUE=DATE:20140701\n" +
                "SUMMARY:Created\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "END:VJOURNAL\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20140701T100000Z\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "RRULE:FREQ=MINUTELY;INTERVAL=1\n" +
                "SUMMARY:My Task\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'Testing!'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n";

        MockScheduledTriggerExecutor executor = new MockScheduledTriggerExecutor();
        MockActionManager actionContext = new MockActionManager();
        ICalTriggerProvider scheduler = new ICalTriggerProvider(tz);
        scheduler.setScheduleExecutor(executor);
        scheduler.setActionManager(actionContext);

        assertFalse(executor.hasDelays());
        scheduler.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 11, 0, 1));

        // verify task was created and scheduled
        assertEquals(1, scheduler.getTriggers().size());
        ICalTrigger trigger = (ICalTrigger)scheduler.getTriggers().iterator().next();
        assertEquals(0, actionContext.getLogCalls());
        assertTrue(executor.hasDelays());
        assertEquals(59000, (long)executor.getDelayForTask(trigger));

        // force task to fire
        executor.clearDelays();
        scheduler.onTriggerExecuted(trigger, DateHelper.getTime(tz, 2014, 7, 1, 11, 1, 0));

        // verify that task was scheduled again
        assertTrue(executor.hasDelays());
        assertEquals(60000, (long)executor.getDelayForTask(trigger));
    }
}
