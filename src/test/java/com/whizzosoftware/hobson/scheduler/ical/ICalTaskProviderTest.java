/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.scheduler.SchedulerPlugin;
import com.whizzosoftware.hobson.scheduler.executor.MockScheduledTaskExecutor;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.*;

public class ICalTaskProviderTest {
    @Test
    public void testLoadCalendarWithNoExecutor() {
        ICalTaskProvider scheduler = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
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
            ICalTaskProvider scheduler = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
            scheduler.setScheduleExecutor(new MockScheduledTaskExecutor());
            scheduler.setScheduleFile(sfile);
        } finally {
            sfile.delete();
        }
    }

    @Test
    public void testClearAllTasks() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();

        // make sure executor has no delays already set
        assertFalse(executor.hasDelays());

        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20130714T170000Z\n" +
                "DTEND:20130714T170000Z\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "COMMENT:[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long startOfDay = DateHelper.getTime(tz, 2013, 7, 14, 0, 0, 0);

        MockTaskManager mgr = new MockTaskManager();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
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
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20130714T170000Z\n" +
                "DTEND:20130714T170000Z\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // assert what happens the day OF the event
        long startOfDay = DateHelper.getTime(tz, 2013, 7, 14, 0, 0, 0);

        MockTaskManager manager = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(manager);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), startOfDay);

        // verify task was created
        assertEquals(1, manager.getPublishedTasks().size());
        ICalTask t = (ICalTask)manager.getPublishedTasks().iterator().next();
        assertEquals("My Task", t.getName());

        // verify task was scheduled -- should have been scheduled to execute in 61200 seconds (17 hours)
        assertEquals(61200000, (long)executor.getDelayForTask(t));

        // assert what happens the day AFTER the event
        startOfDay = DateHelper.getTime(tz, 2013, 7, 15, 0, 0, 0);
        executor.clearDelays();
        s.resetForNewDay(startOfDay);

        // verify the task was created but not scheduled
        assertEquals(1, manager.getPublishedTasks().size());

        // verify task was not scheduled
        assertFalse(executor.isTaskScheduled((ICalTask) manager.getPublishedTasks().iterator().next()));
        assertFalse(executor.hasDelays());
        assertNull(executor.getDelayForTask(t));
    }

    @Test
    public void testEditEvent() throws Exception {
        File file = File.createTempFile("hob", ".ics");
        try {
            String ical = "BEGIN:VCALENDAR\n" +
                    "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
                    "VERSION:2.0\n" +
                    "BEGIN:VEVENT\n" +
                    "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                    "DTSTART:20130714T170000Z\n" +
                    "DTEND:20130714T170000Z\n" +
                    "SUMMARY:My Task\n" +
                    "X-ACTION-SET:foo\n" +
                    "END:VEVENT\n" +
                    "END:VCALENDAR";

            String ical2 = "BEGIN:VCALENDAR\n" +
                    "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
                    "VERSION:2.0\n" +
                    "BEGIN:VEVENT\n" +
                    "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                    "DTSTART:20130714T170000Z\n" +
                    "DTEND:20130714T170000Z\n" +
                    "SUMMARY:My Edited Task\n" +
                    "END:VEVENT\n" +
                    "END:VCALENDAR";

            // write out ICS to temp file
            FileWriter fw = new FileWriter(file);
            fw.append(ical);
            fw.close();

            MockTaskManager mgr = new MockTaskManager();
            ICalTaskProvider p = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, TimeZone.getTimeZone("America/Denver"));
            p.setTaskManager(mgr);
            p.setScheduleExecutor(new MockScheduledTaskExecutor());
            p.setScheduleFile(file);
            p.start();

            // make sure the task was created
            assertEquals(1, mgr.getPublishedTasks().size());

            // create task JSON
            JSONObject json = new JSONObject();
            json.put("name", "My Edited Task");
            JSONArray conds = new JSONArray();
            json.put("conditions", conds);
            JSONObject cond = new JSONObject();
            conds.put(cond);
            cond.put("start", "20130714T170000Z");
            JSONArray actions = new JSONArray();
            json.put("actions", actions);
            JSONObject action = new JSONObject();
            actions.put(action);
            action.put("pluginId", "com.whizzosoftware.hobson.server-api");
            action.put("actionId", "log");
            action.put("name", "My Edited Action");
            JSONObject props = new JSONObject();
            action.put("properties", props);
            props.put("message", "foobar");

            // update the task
            TaskContext ctx = TaskContext.createLocal("pluginId", "15dee4fe-a841-4cf6-8d7f-76c3ad5492b1");
            Map<String,Object> propValues = new HashMap<>();
            propValues.put("date", "20130714");
            propValues.put("time", "170000Z");
            p.onUpdateTask(
                ctx,
                "My Edited Task",
                new PropertyContainerSet(
                    new PropertyContainer(
                        PropertyContainerClassContext.create(PluginContext.createLocal("pluginId"), "foo"),
                        propValues
                    )
                ),
                new PropertyContainerSet(
                    "log",
                    null
                )
            );
            assertTrue(file.exists());

            // read back file
            Calendar cal = new CalendarBuilder().build(new FileInputStream(file));
            assertEquals(1, cal.getComponents().size());
            VEvent c = (VEvent)cal.getComponents().get(0);
            assertEquals("My Edited Task", c.getProperty("SUMMARY").getValue());
            assertEquals("15dee4fe-a841-4cf6-8d7f-76c3ad5492b1", c.getProperty("UID").getValue());
            assertEquals("20130714T170000Z", c.getProperty("DTSTART").getValue());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testLoadScheduleWithSingleEventWithSunsetOffset() throws Exception {
        TimeZone tz = TimeZone.getTimeZone("America/Denver");
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20141018T000000\n" +
                "SUMMARY:My Task\n" +
                "X-SUN-OFFSET: SS30\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long startOfDay = DateHelper.getTime(tz, 2014, 10, 18, 0, 0, 0);

        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setLatitudeLongitude(39.3722, -104.8561);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), startOfDay);

        // verify task was created
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask t = (ICalTask)mgr.getPublishedTasks().iterator().next();
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
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T090000Z\n" +
                "RRULE:FREQ=DAILY\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        long schedulerStart = DateHelper.getTime(tz, 2014, 7, 1, 8, 0, 0);

        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), schedulerStart);

        // verify task was created but not run
        assertEquals(1, mgr.getPublishedTasks().size());

        // reload the file at midnight
        s.resetForNewDay(DateHelper.getTime(tz, 2014, 7, 2, 0, 0, 0));

        // verify task was created but not executed
        assertEquals(1, mgr.getPublishedTasks().size());
    }

    @Test
    public void testDelayedDayReset() throws Exception {
        TimeZone tz = TimeZone.getDefault();

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T000000\n" +
                "RRULE:FREQ=DAILY\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler after the task should have run
        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 17, 0, 0));

        // verify task was not scheduled
        assertEquals(1, mgr.getPublishedTasks().size());
        assertFalse(executor.isTaskScheduled((ICalTask)mgr.getPublishedTasks().iterator().next()));

        // start a new day 30 seconds after midnight -- this covers the corner case where a delay causes the
        // resetForNewDay() method to get fired slightly after midnight and there are tasks that should have
        // already executed by then
        s.resetForNewDay(DateHelper.getTime(tz, 2014, 7, 2, 0, 0, 30));

        // verify task was not scheduled but task executed
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask task = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertFalse(executor.isTaskScheduled(task));
        assertEquals(1404367200000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        assertFalse((boolean) task.getProperties().get(ICalTask.PROP_SCHEDULED));
    }

    @Test
    public void testDayResetWithSolarOffsetTask() throws Exception {
        TimeZone tz = TimeZone.getDefault();

        // an event that runs every day 30 minutes after sunset
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T000000\n" +
                "RRULE:FREQ=DAILY\n" +
                "X-SUN-OFFSET:SS30\n" +
                "X-ACTION-SET:foo\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler after the task should have run
        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setLatitudeLongitude(39.3722, -104.8561);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 22, 0, 0));

        // verify task was not scheduled or executed
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask t = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertFalse(executor.isTaskScheduled(t));
        assertNotNull(t.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));

        // start a new day at midnight
        s.resetForNewDay(DateHelper.getTime(tz, 2014, 7, 2, 0, 0, 0));

        // verify task was scheduled at appropriate time and task did not execute
        assertEquals(1, mgr.getPublishedTasks().size());
        t = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertTrue(executor.isTaskScheduled(t));
        assertEquals(75600000, (long) executor.getDelayForTask(t));
        assertEquals(1404356400000l, t.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        assertTrue((boolean) t.getProperties().get(ICalTask.PROP_SCHEDULED));
    }

    @Test
    public void testMonthlyNextRun() throws Exception {
        TimeZone tz = TimeZone.getDefault();

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T220000\n" +
                "RRULE:FREQ=MONTHLY\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler when the task should have run
        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 23, 0, 0));

        // verify task was created and its next run time
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask task = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertEquals(1406952000000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testYearlyNextRun() throws Exception {
        TimeZone tz = TimeZone.getDefault();

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T220000\n" +
                "RRULE:FREQ=YEARLY\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler when the task should have run
        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 8, 1, 21, 0, 0));

        // verify task was created and its next run time
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask task = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertEquals(1435809600000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
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
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n";

        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider scheduler = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        scheduler.setTaskManager(mgr);
        scheduler.setScheduleExecutor(executor);

        assertFalse(executor.hasDelays());
        scheduler.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 11, 0, 1));

        // verify task was created and scheduled
        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask task = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertTrue(executor.hasDelays());
        assertEquals(59000, (long) executor.getDelayForTask(task));

        // force task to fire
        executor.clearDelays();
        scheduler.onTaskExecuted(task, DateHelper.getTime(tz, 2014, 7, 1, 11, 1, 0), true);

        // verify that task was scheduled again
        assertTrue(executor.hasDelays());
        assertEquals(60000, (long)executor.getDelayForTask(task));
    }

    @Test
    public void testSunOffsetWithNoLatLong() throws Exception {
        TimeZone tz = TimeZone.getDefault();

        // an event that runs every day at midnight
        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
                "DTSTART:20140701T000000\n" +
                "RRULE:FREQ=DAILY\n" +
                "X-SUN-OFFSET:SS30\n" +
                "SUMMARY:My Task\n" +
                "X-ACTION-SET:foo\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // start the scheduler after the task should have run
        MockTaskManager mgr = new MockTaskManager();
        MockScheduledTaskExecutor executor = new MockScheduledTaskExecutor();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2014, 7, 1, 22, 0, 0));

        assertEquals(1, mgr.getPublishedTasks().size());
        ICalTask t = (ICalTask)mgr.getPublishedTasks().iterator().next();
        assertTrue(t.getProperties().containsKey(ICalTask.PROP_ERROR));
    }

    @Test
    public void testOnCreateTask() throws Exception {
        File scheduleFile = File.createTempFile("hobson", "ics");
        scheduleFile.deleteOnExit();

        PluginContext pctx = PluginContext.createLocal("pluginId");
        MockTaskManager tm = new MockTaskManager();
        MockScheduledTaskExecutor ste = new MockScheduledTaskExecutor();

        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
                "END:VCALENDAR";

        TimeZone tz = TimeZone.getTimeZone("GMT");
        ICalTaskProvider p = new ICalTaskProvider(pctx, null, null, tz);
        p.setTaskManager(tm);
        p.setScheduleExecutor(ste);
        p.setScheduleFile(scheduleFile);
        p.loadICSStream(new ByteArrayInputStream(ical.getBytes()), DateHelper.getTime(tz, 2015, 5, 20, 12, 0, 0));

        Map<String,Object> values = new HashMap<>();
        values.put("date", "20150520");
        values.put("time", "100000Z");

        p.onCreateTask(
            "New Task",
            new PropertyContainerSet(
                new PropertyContainer(
                    PropertyContainerClassContext.create(pctx, SchedulerPlugin.SCHEDULE_CONDITION_CLASS_ID),
                    values
                )
            ),
            new PropertyContainerSet(
                "actionset1",
                null
            )
        );

        int checkCount = 0;

        BufferedReader br = new BufferedReader(new FileReader(scheduleFile));
        String s;
        while ((s = br.readLine()) != null) {
            if ("X-ACTION-SET:actionset1".equals(s.trim())) {
                checkCount++;
            }
            if ("SUMMARY:New Task".equals(s.trim())) {
                checkCount++;
            }
            if ("DTSTART:20150520T100000Z".equals(s.trim())) {
                checkCount++;
            }
        }
        br.close();

        assertEquals(3, checkCount);
    }
}
