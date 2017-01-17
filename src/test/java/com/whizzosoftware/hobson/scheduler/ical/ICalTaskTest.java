/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.scheduler.SchedulingException;
import com.whizzosoftware.hobson.scheduler.condition.ScheduleConditionClass;
import com.whizzosoftware.hobson.scheduler.queue.MockTaskQueue;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.UidGenerator;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ICalTaskTest {
    @Test
    public void testConstructorWithActions() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        VEvent event = new VEvent(new DateTime(), "task2");
        event.getProperties().add(new Uid("uid2"));
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        ICalTask task = new ICalTask(ctx, event, null);
        assertEquals("uid2", task.getContext().getTaskId());
    }

    @Test
    public void testNextFridayThe13th() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        DateTimeZone tz = DateTimeZone.forID("GMT");

        // event starts on 4/13/2001 and goes monthly every friday the 13th
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(2001, 4, 13, 0, 0, 0, tz)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new Comment("[{'pluginId':'com.whizzosoftware.hobson.server-api','actionId':'log','name':'My Action','properties':{'message':'foo'}}]"));
        Recur recur = new Recur("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");
        event.getProperties().add(new RRule(recur));

        // check from day before first occurrence
        ICalTask task = new ICalTask(ctx, event, null);
        List<Long> periods = task.getRunsDuringInterval(DateHelper.getTime(2001, 4, 12, 0, 0, 0, tz), DateHelper.getTime(2001, 4, 14, 0, 0, 0, tz), tz);
        assertEquals(1, periods.size());

        // check from day of first occurrence
        long startDate = DateHelper.getTime(2001, 4, 13, 0, 0, 0, tz);
        periods = task.getRunsDuringInterval(startDate, DateHelper.getTime(2001, 4, 14, 0, 0, 0, tz), tz);
        assertEquals(1, periods.size());

        // check from much later date
        startDate = DateHelper.getTime(2014, 6, 30, 0, 0, 0, tz);
        periods = task.getRunsDuringInterval(startDate, startDate + (1000 * 60 * 60 * 24), tz);
        assertEquals(0, periods.size());
    }

    @Test
    public void testEvery3DaysAt9AM() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        DateTimeZone tz = DateTimeZone.forID("GMT");

        // create event
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(2014, 6, 1, 9, 0, 0, tz)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=3");
        event.getProperties().add(new RRule(recur));

        // check from day before first occurrence
        ICalTask task = new ICalTask(ctx, event, null);
        List<Long> periods = task.getRunsDuringInterval(DateHelper.getTime(2014, 6, 1, 9, 0, 0, tz), DateHelper.getTime(2014, 8, 31, 23, 59, 59, tz), tz);
        assertEquals(31, periods.size());
        assertEquals(DateHelper.getTime(2014, 6, 1, 9, 0, 0, tz), (long)periods.get(0));
        assertEquals(DateHelper.getTime(2014, 6, 4, 9, 0, 0, tz), (long)periods.get(1));
        assertEquals(DateHelper.getTime(2014, 6, 7, 9, 0, 0, tz), (long)periods.get(2));
        assertEquals(DateHelper.getTime(2014, 6, 10, 9, 0, 0, tz), (long)periods.get(3));
        assertEquals(DateHelper.getTime(2014, 6, 13, 9, 0, 0, tz), (long)periods.get(4));
        assertEquals(DateHelper.getTime(2014, 6, 16, 9, 0, 0, tz), (long)periods.get(5));
        assertEquals(DateHelper.getTime(2014, 6, 19, 9, 0, 0, tz), (long)periods.get(6));
        assertEquals(DateHelper.getTime(2014, 6, 22, 9, 0, 0, tz), (long)periods.get(7));
        assertEquals(DateHelper.getTime(2014, 6, 25, 9, 0, 0, tz), (long)periods.get(8));
        assertEquals(DateHelper.getTime(2014, 6, 28, 9, 0, 0, tz), (long)periods.get(9));
        assertEquals(DateHelper.getTime(2014, 7, 1, 9, 0, 0, tz), (long)periods.get(10));
        assertEquals(DateHelper.getTime(2014, 7, 4, 9, 0, 0, tz), (long)periods.get(11));
        assertEquals(DateHelper.getTime(2014, 7, 7, 9, 0, 0, tz), (long)periods.get(12));
        assertEquals(DateHelper.getTime(2014, 7, 10, 9, 0, 0, tz), (long)periods.get(13));
        assertEquals(DateHelper.getTime(2014, 7, 13, 9, 0, 0, tz), (long)periods.get(14));
        assertEquals(DateHelper.getTime(2014, 7, 16, 9, 0, 0, tz), (long)periods.get(15));
        assertEquals(DateHelper.getTime(2014, 7, 19, 9, 0, 0, tz), (long)periods.get(16));
        assertEquals(DateHelper.getTime(2014, 7, 22, 9, 0, 0, tz), (long)periods.get(17));
        assertEquals(DateHelper.getTime(2014, 7, 25, 9, 0, 0, tz), (long)periods.get(18));
        assertEquals(DateHelper.getTime(2014, 7, 28, 9, 0, 0, tz), (long)periods.get(19));
        assertEquals(DateHelper.getTime(2014, 7, 31, 9, 0, 0, tz), (long)periods.get(20));
        assertEquals(DateHelper.getTime(2014, 8, 3, 9, 0, 0, tz), (long)periods.get(21));
        assertEquals(DateHelper.getTime(2014, 8, 6, 9, 0, 0, tz), (long)periods.get(22));
        assertEquals(DateHelper.getTime(2014, 8, 9, 9, 0, 0, tz), (long)periods.get(23));
        assertEquals(DateHelper.getTime(2014, 8, 12, 9, 0, 0, tz), (long)periods.get(24));
        assertEquals(DateHelper.getTime(2014, 8, 15, 9, 0, 0, tz), (long)periods.get(25));
        assertEquals(DateHelper.getTime(2014, 8, 18, 9, 0, 0, tz), (long)periods.get(26));
        assertEquals(DateHelper.getTime(2014, 8, 21, 9, 0, 0, tz), (long)periods.get(27));
        assertEquals(DateHelper.getTime(2014, 8, 24, 9, 0, 0, tz), (long)periods.get(28));
        assertEquals(DateHelper.getTime(2014, 8, 27, 9, 0, 0, tz), (long)periods.get(29));
        assertEquals(DateHelper.getTime(2014, 8, 30, 9, 0, 0, tz), (long)periods.get(30));
        assertEquals(DateHelper.getTime(2014, 8, 30, 9, 0, 0, tz), (long)periods.get(30));
    }

    @Test
    public void testJSONRuleConstruction() throws Exception {
        PluginContext pctx = PluginContext.createLocal("pluginId");
        MockTaskManager taskManager = new MockTaskManager();
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        taskManager.publishConditionClass(scc);
        ICalTaskProvider provider = new ICalTaskProvider(pctx, null, null);
        provider.setTaskManager(taskManager);
        provider.setScheduleExecutor(new MockTaskQueue());

        Map<String,Object> props = new HashMap<>();
        props.put("date", "2014-07-01");
        props.put("time", "10:00:00Z");
        props.put("recurrence", "FREQ=MINUTELY;INTERVAL=1");
        taskManager.createTask(HubContext.createLocal(), "My Task", null, Collections.singletonList(new PropertyContainer(scc.getContext(), props)), new PropertyContainerSet("foo", null));
        provider.onRegisterTasks(Collections.singletonList(taskManager.getTasks(HubContext.createLocal()).iterator().next().getContext()));

        // make sure the provider updated the rule file
        Calendar cal = provider.getCalendar();

        assertEquals(1, cal.getComponents().size());
        assertTrue(cal.getComponents().get(0) instanceof VEvent);

        VEvent event = (VEvent)cal.getComponents().get(0);
        assertNotNull(event.getUid().getValue());

        assertEquals("20140701T100000Z", event.getProperties().getProperty("DTSTART").getValue());
        assertEquals("FREQ=MINUTELY;INTERVAL=1", event.getProperties().getProperty("RRULE").getValue());
    }

    @Test
    public void testJSONRuleConstructionWithNeverRecurrence() throws Exception {
        PluginContext pctx = PluginContext.createLocal("pluginId");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));

        MockTaskManager taskManager = new MockTaskManager();
        taskManager.publishConditionClass(scc);

        ICalTaskProvider provider = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
        provider.setScheduleExecutor(new MockTaskQueue());
        provider.setTaskManager(taskManager);

        Map<String,Object> props = new HashMap<>();
        props.put("date", "2014-07-01");
        props.put("time", "10:00:00Z");
        props.put("recurrence", "never");
        taskManager.createTask(HubContext.createLocal(), "My Task", null, Collections.singletonList(new PropertyContainer(scc.getContext(), props)), new PropertyContainerSet("foo", null));

        provider.onRegisterTasks(Collections.singletonList(taskManager.getTasks(HubContext.createLocal()).iterator().next().getContext()));

        // make sure the provider updated the rule file
        Calendar cal = provider.getCalendar();

        assertEquals(1, cal.getComponents().size());
        assertTrue(cal.getComponents().get(0) instanceof VEvent);

        VEvent event = (VEvent)cal.getComponents().get(0);
        assertNotNull(event.getUid().getValue());

        assertEquals("20140701T100000Z", event.getProperties().getProperty("DTSTART").getValue());
        assertNull(event.getProperties().getProperty("RRULE"));
    }

    @Test
    public void testJSONRuleConstructionWithSunOffset() throws Exception {
        PluginContext pctx = PluginContext.createLocal("plugin1");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));

        MockTaskManager taskManager = new MockTaskManager();
        taskManager.publishConditionClass(scc);

        ICalTaskProvider provider = new ICalTaskProvider(pctx, null, null);
        provider.setScheduleExecutor(new MockTaskQueue());
        provider.setTaskManager(taskManager);

        Map<String,Object> props = new HashMap<>();
        props.put("date", "2014-07-01");
        props.put("time", "SR");
        props.put("recurrence", "FREQ=MINUTELY;INTERVAL=1");
        taskManager.createTask(HubContext.createLocal(), "My Task", null, Collections.singletonList(new PropertyContainer(scc.getContext(), props)), new PropertyContainerSet("foo", null));
        provider.onRegisterTasks(Collections.singletonList(taskManager.getTasks(HubContext.createLocal()).iterator().next().getContext()));

        // make sure the provider updated the rule file
        Calendar cal = provider.getCalendar();

        assertEquals(1, cal.getComponents().size());
        assertTrue(cal.getComponents().get(0) instanceof VEvent);

        VEvent event = (VEvent)cal.getComponents().get(0);
        assertNotNull(event.getUid().getValue());

        assertEquals("20140701T000000", event.getProperties().getProperty("DTSTART").getValue());
        assertEquals("FREQ=MINUTELY;INTERVAL=1", event.getProperties().getProperty("RRULE").getValue());
        assertEquals("SR", event.getProperties().getProperty(ICalTask.PROP_SUN_OFFSET).getValue());
    }

    @Test
    public void testSunOffset() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        MockTaskManager am = new MockTaskManager();
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(2014, 10, 19, 0, 0, 0, tz)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new XProperty(ICalTask.PROP_SUN_OFFSET, "SS+30"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=1");
        event.getProperties().add(new RRule(recur));

        ICalTask task = new ICalTask(ctx, event, null);
        task.setLocation(39.3722, -104.8561);

        List<Long> runs = task.getRunsDuringInterval(DateHelper.getTime(2014, 10, 19, 0, 0, 0, tz), DateHelper.getTime(2014, 10, 19, 23, 59, 59, tz), tz);
        assertEquals(1, runs.size());
        assertEquals(1413765900000l, (long)runs.get(0));

        runs = task.getRunsDuringInterval(DateHelper.getTime(2014, 12, 19, 0, 0, 0, tz), DateHelper.getTime(2014, 12, 19, 23, 59, 59, tz), tz);
        assertEquals(1, runs.size());
        assertEquals(1419034080000l, (long) runs.get(0));

        runs = task.getRunsDuringInterval(DateHelper.getTime(2015, 7, 19, 0, 0, 0, tz), DateHelper.getTime(2015, 7, 19, 23, 59, 59, tz), tz);
        assertEquals(1, runs.size());
        assertEquals(1437360780000l, (long)runs.get(0));

        // This one is interesting -- since we store the start time for events with a sun offset as midnight, this event
        // technically would have already run as it's strictly defined by the VEvent. However, with the sun offset the
        // start time is pushed until after the current time and it SHOULD run
        runs = task.getRunsDuringInterval(DateHelper.getTime(2014, 10, 20, 16, 46, 0, tz), DateHelper.getTime(2014, 10, 20, 23, 59, 59, tz), tz);
        assertEquals(1, runs.size());
    }

    @Test
    public void testGetRunsForIntervalWithNoLatLong() throws Exception {
        PluginContext ctx = PluginContext.createLocal("pluginId");
        MockTaskManager am = new MockTaskManager();
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        VEvent event = new VEvent(new DateTime(DateHelper.getTime(2014, 10, 19, 0, 0, 0, tz)), "task1");
        event.getProperties().add(new UidGenerator("1").generateUid());
        event.getProperties().add(new XProperty(ICalTask.PROP_ACTION_SET, "foo"));
        event.getProperties().add(new XProperty(ICalTask.PROP_SUN_OFFSET, "SS+30"));
        Recur recur = new Recur("FREQ=DAILY;INTERVAL=1");
        event.getProperties().add(new RRule(recur));

        ICalTask task = new ICalTask(ctx, event, null);

        try {
            task.getRunsDuringInterval(DateHelper.getTime(2014, 10, 19, 0, 0, 0, tz), DateHelper.getTime(2014, 10, 19, 23, 59, 59, tz), tz);
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
