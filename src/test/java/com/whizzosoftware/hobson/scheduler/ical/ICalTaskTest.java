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
import com.whizzosoftware.hobson.scheduler.executor.MockScheduledTaskExecutor;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.UidGenerator;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.*;

import static org.junit.Assert.*;

public class ICalTaskTest {
    @Test
    public void testConstructorWithNoActionSetId() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        VEvent event = new VEvent(new DateTime(), "task1");
        event.getProperties().add(new Uid("uid"));
        try {
            new ICalTask(null, ctx, event, null);
            fail("Should have thrown exception");
        } catch (InvalidVEventException ignored) {
        }
    }

    @Test
    public void testConstructorWithActions() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        VEvent event = new VEvent(new DateTime(), "task2");
        event.getProperties().add(new Uid("uid2"));
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        ICalTask task = new ICalTask(null, ctx, event, null);
        assertEquals("uid2", task.getContext().getTaskId());
        assertEquals("task2", task.getName());
        assertEquals("foo", task.getActionSet().getId());
    }

    @Test
    public void testGetConditions() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        long time = DateHelper.getTime(tz, 2001, 4, 13, 0, 0, 0);
        VEvent event = new VEvent(new DateTime(time), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        Recur recur = new Recur("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");
        event.getProperties().add(new RRule(recur));

        ICalTask task = new ICalTask(null, ctx, event, null);

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        Map<String,Object> map = task.getConditionSet().getPrimaryProperty().getPropertyValues();
        assertEquals(3, map.size());
        assertEquals("20010412", map.get("date"));
        assertEquals("180000", map.get("time"));
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR", map.get("recurrence"));
    }

    @Test
    public void testGetConditionsWithSunOffset() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(tz, 2001, 4, 13, 0, 0, 0)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        Recur recur = new Recur("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");
        event.getProperties().add(new RRule(recur));
        event.getProperties().add(new XProperty(ICalTask.PROP_SUN_OFFSET, "SS"));

        ICalTask task = new ICalTask(null, ctx, event, null);

        assertTrue(task.getConditionSet().hasPrimaryProperty());
        Map<String,Object> map = task.getConditionSet().getPrimaryProperty().getPropertyValues();
        assertEquals(3, map.size());
        assertEquals("20010412", map.get("date"));
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=13;BYDAY=FR", map.get("recurrence"));
        assertEquals("SS", map.get("time"));
    }

    @Test
    public void testNextFridayThe13th() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // event starts on 4/13/2001 and goes monthly every friday the 13th
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(tz, 2001, 4, 13, 0, 0, 0)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new Comment("[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]"));
        Recur recur = new Recur("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");
        event.getProperties().add(new RRule(recur));

        // check from day before first occurrence
        ICalTask task = new ICalTask(null, ctx, event, null);
        List<Long> periods = task.getRunsDuringInterval(DateHelper.getTime(tz, 2001, 4, 12, 0, 0, 0), DateHelper.getTime(tz, 2001, 4, 14, 0, 0, 0));
        assertEquals(1, periods.size());

        // check from day of first occurrence
        long startDate = DateHelper.getTime(tz, 2001, 4, 13, 0, 0, 0);
        periods = task.getRunsDuringInterval(startDate, DateHelper.getTime(tz, 2001, 4, 14, 0, 0, 0));
        assertEquals(1, periods.size());

        // check from much later date
        startDate = DateHelper.getTime(tz, 2014, 6, 30, 0, 0, 0);
        periods = task.getRunsDuringInterval(startDate, startDate + (1000 * 60 * 60 * 24));
        assertEquals(0, periods.size());
    }

    @Test
    public void testEvery3DaysAt9AM() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // create event
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(tz, 2014, 6, 1, 9, 0, 0)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=3");
        event.getProperties().add(new RRule(recur));

        // check from day before first occurrence
        ICalTask task = new ICalTask(null, ctx, event, null);
        List<Long> periods = task.getRunsDuringInterval(DateHelper.getTime(tz, 2014, 6, 1, 9, 0, 0), DateHelper.getTime(tz, 2014, 8, 31, 23, 59, 59));
        assertEquals(31, periods.size());
        assertEquals(DateHelper.getTime(tz, 2014, 6, 1, 9, 0, 0), (long)periods.get(0));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 4, 9, 0, 0), (long)periods.get(1));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 7, 9, 0, 0), (long)periods.get(2));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 10, 9, 0, 0), (long)periods.get(3));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 13, 9, 0, 0), (long)periods.get(4));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 16, 9, 0, 0), (long)periods.get(5));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 19, 9, 0, 0), (long)periods.get(6));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 22, 9, 0, 0), (long)periods.get(7));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 25, 9, 0, 0), (long)periods.get(8));
        assertEquals(DateHelper.getTime(tz, 2014, 6, 28, 9, 0, 0), (long)periods.get(9));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 1, 9, 0, 0), (long)periods.get(10));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 4, 9, 0, 0), (long)periods.get(11));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 7, 9, 0, 0), (long)periods.get(12));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 10, 9, 0, 0), (long)periods.get(13));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 13, 9, 0, 0), (long)periods.get(14));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 16, 9, 0, 0), (long)periods.get(15));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 19, 9, 0, 0), (long)periods.get(16));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 22, 9, 0, 0), (long)periods.get(17));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 25, 9, 0, 0), (long)periods.get(18));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 28, 9, 0, 0), (long)periods.get(19));
        assertEquals(DateHelper.getTime(tz, 2014, 7, 31, 9, 0, 0), (long)periods.get(20));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 3, 9, 0, 0), (long)periods.get(21));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 6, 9, 0, 0), (long)periods.get(22));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 9, 9, 0, 0), (long)periods.get(23));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 12, 9, 0, 0), (long)periods.get(24));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 15, 9, 0, 0), (long)periods.get(25));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 18, 9, 0, 0), (long)periods.get(26));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 21, 9, 0, 0), (long)periods.get(27));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 24, 9, 0, 0), (long)periods.get(28));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 27, 9, 0, 0), (long)periods.get(29));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 30, 9, 0, 0), (long)periods.get(30));
        assertEquals(DateHelper.getTime(tz, 2014, 8, 30, 9, 0, 0), (long)periods.get(30));
    }

    @Test
    public void testJSONRuleConstruction() throws Exception {
        ICalTaskProvider provider = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
        provider.setScheduleExecutor(new MockScheduledTaskExecutor());

        // validate we start with a non-existent temp file
        File calendarFile = File.createTempFile("schedule", ".ics");
        assertTrue(calendarFile.delete());
        assertFalse(calendarFile.exists());

        try {
            provider.setScheduleFile(calendarFile);
            provider.reloadScheduleFile();

            Map<String,Object> props = new HashMap<>();
            props.put("date", "2014-07-01");
            props.put("time", "10:00:00Z");
            props.put("recurrence", "FREQ=MINUTELY;INTERVAL=1");
            provider.onCreateTask(
                "My Task",
                    null, new PropertyContainerSet(
                    new PropertyContainer(
                        PropertyContainerClassContext.create(PluginContext.createLocal("pluginId"), "foo"),
                        props
                    )
                ),
                new PropertyContainerSet(
                    "foo",
                    null
                )
            );

            // make sure the provider updated the rule file
            assertTrue(calendarFile.exists());
            Calendar cal = new CalendarBuilder().build(new FileReader(calendarFile));

            assertEquals(2, cal.getComponents().size());
            assertTrue(cal.getComponents().get(0) instanceof VJournal);
            assertTrue(cal.getComponents().get(1) instanceof VEvent);

            VEvent event = (VEvent)cal.getComponents().get(1);
            assertNotNull(event.getUid().getValue());
            assertEquals("My Task", event.getSummary().getValue());

            assertEquals("20140701T100000Z", event.getProperties().getProperty("DTSTART").getValue());
            assertEquals("FREQ=MINUTELY;INTERVAL=1", event.getProperties().getProperty("RRULE").getValue());

            assertNotNull(event.getProperties().getProperty(ICalTask.PROP_ACTION_SET));
            assertEquals("foo", event.getProperties().getProperty(ICalTask.PROP_ACTION_SET).getValue());
        } finally {
            assertTrue(calendarFile.delete());
        }
    }

    @Test
    public void testJSONRuleConstructionWithSunOffset() throws Exception {
        ICalTaskProvider provider = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
        provider.setScheduleExecutor(new MockScheduledTaskExecutor());

        // validate we start with a non-existent temp file
        File calendarFile = File.createTempFile("schedule", ".ics");
        assertTrue(calendarFile.delete());
        assertFalse(calendarFile.exists());

        try {
            provider.setScheduleFile(calendarFile);
            provider.reloadScheduleFile();

            Map<String,Object> props = new HashMap<>();
            props.put("date", "2014-07-01");
            props.put("time", "SR");
            props.put("recurrence", "FREQ=MINUTELY;INTERVAL=1");
            provider.onCreateTask(
                "My Task",
                    null, new PropertyContainerSet(
                    new PropertyContainer(
                        PropertyContainerClassContext.create(PluginContext.createLocal("pluginId"), "foo"),
                        props
                    )
                ),
                new PropertyContainerSet(
                    "foo",
                    null
                )
            );

            // make sure the provider updated the rule file
            assertTrue(calendarFile.exists());
            Calendar cal = new CalendarBuilder().build(new FileReader(calendarFile));

            assertEquals(2, cal.getComponents().size());
            assertTrue(cal.getComponents().get(0) instanceof VJournal);
            assertTrue(cal.getComponents().get(1) instanceof VEvent);

            VEvent event = (VEvent)cal.getComponents().get(1);
            assertNotNull(event.getUid().getValue());
            assertEquals("My Task", event.getSummary().getValue());

            assertEquals("20140701T000000", event.getProperties().getProperty("DTSTART").getValue());
            assertEquals("FREQ=MINUTELY;INTERVAL=1", event.getProperties().getProperty("RRULE").getValue());
            assertEquals("SR", event.getProperties().getProperty(ICalTask.PROP_SUN_OFFSET).getValue());

            assertNotNull(event.getProperties().getProperty("X-ACTION-SET"));
            assertEquals("foo", event.getProperties().getProperty("X-ACTION-SET").getValue());
        } finally {
            assertTrue(calendarFile.delete());
        }
    }

    @Test
    public void testSunOffset() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        MockTaskManager am = new MockTaskManager();
        TimeZone tz = TimeZone.getTimeZone("America/Denver");
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(tz, 2014, 10, 19, 0, 0, 0)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new XProperty(ICalTask.PROP_SUN_OFFSET, "SS+30"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=1");
        event.getProperties().add(new RRule(recur));

        ICalTask task = new ICalTask(am, ctx, event, null);
        task.setLatitude(39.3722);
        task.setLongitude(-104.8561);

        List<Long> runs = task.getRunsDuringInterval(DateHelper.getTime(tz, 2014, 10, 19, 0, 0, 0), DateHelper.getTime(tz, 2014, 10, 19, 23, 59, 59));
        assertEquals(1, runs.size());
        assertEquals(1413765900000l, (long)runs.get(0));

        runs = task.getRunsDuringInterval(DateHelper.getTime(tz, 2014, 12, 19, 0, 0, 0), DateHelper.getTime(tz, 2014, 12, 19, 23, 59, 59));
        assertEquals(1, runs.size());
        assertEquals(1419034080000l, (long) runs.get(0));

        runs = task.getRunsDuringInterval(DateHelper.getTime(tz, 2015, 7, 19, 0, 0, 0), DateHelper.getTime(tz, 2015, 7, 19, 23, 59, 59));
        assertEquals(1, runs.size());
        assertEquals(1437360780000l, (long)runs.get(0));

        // This one is interesting -- since we store the start time for events with a sun offset as midnight, this event
        // technically would have already run as it's strictly defined by the VEvent. However, with the sun offset the
        // start time is pushed until after the current time and it SHOULD run
        runs = task.getRunsDuringInterval(DateHelper.getTime(tz, 2014, 10, 20, 16, 46, 0), DateHelper.getTime(tz, 2014, 10, 20, 23, 59, 59));
        assertEquals(1, runs.size());
    }

    @Test
    public void testGetRunsForIntervalWithNoLatLong() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        MockTaskManager am = new MockTaskManager();
        TimeZone tz = TimeZone.getTimeZone("America/Denver");
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(tz, 2014, 10, 19, 0, 0, 0)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new XProperty(ICalTask.PROP_SUN_OFFSET, "SS+30"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=1");
        event.getProperties().add(new RRule(recur));

        ICalTask task = new ICalTask(am, ctx, event, null);

        List<Long> runs = null;
        try {
            runs = task.getRunsDuringInterval(DateHelper.getTime(tz, 2014, 10, 19, 0, 0, 0), DateHelper.getTime(tz, 2014, 10, 19, 23, 59, 59));
            fail("Should have thrown exception");
        } catch (SchedulingException ignored) {}
    }

    @Test
    public void testCreateDescription() throws Exception {
//        MockTaskManager tm = new MockTaskManager();
//
//        HubContext hctx = HubContext.createLocal();
//        PluginContext pctx = PluginContext.create(hctx, "plugin");
//
//        // publish an action set to reference
//        List<TaskActionMetadata> actions = new ArrayList<>();
//        actions.add(new TaskActionMetadata(TaskActionClassContext.create(pctx, "turnOff"), null));
//        TaskActionSet as = tm.publishActionSet(hctx, "as", actions);
//
//        // publish a condition class to reference
//        tm.publishConditionClass(new ScheduleConditionClass(pctx));
//
//        // create task properties
//        Map<String,Object> props = new HashMap<>();
//        props.put("date", "20150518");
//        props.put("time", "100000Z");
//
//        assertEquals("", new ICalTask(tm, TaskContext.create(pctx, "tid"), "Test", new TaskConditionMetadata(TaskConditionClassContext.create(pctx, "schedule"), props), null, as.getId()).createDescription());
    }
}
