/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.hub.HubContext;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.scheduler.queue.MockTaskQueue;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

public class ICalTaskProviderTest {
    @Test
    public void testLoadTaskWithNoTaskManager() {
        ICalTaskProvider provider = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
        try {
            provider.onRegisterTasks(Collections.singletonList(TaskContext.createLocal("task1")));
            fail("Should have thrown an exception");
        } catch (Exception ignored) {}
    }

    @Test
    public void testLoadTaskWithNoExecutor() {
        MockTaskManager taskManager = new MockTaskManager();
        ICalTaskProvider provider = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null);
        provider.setTaskManager(taskManager);
        try {
            provider.onRegisterTasks(Collections.singletonList(TaskContext.createLocal("task1")));
            fail("Should have thrown an exception");
        } catch (Exception ignored) {}
    }

    @Test
    public void testClearAllTasks() throws Exception {
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");

        DateTimeZone tz = DateTimeZone.forID("GMT");
        MockTaskManager mgr = createMockTaskManager(pccc);
        MockTaskQueue executor = new MockTaskQueue();

        // make sure executor has no delays already set
        assertFalse(executor.hasDelays());

        long startOfDay = DateHelper.getTime(2013, 7, 14, 0, 0, 0, tz);

        HobsonTask task = createScheduleTask(mgr, pccc, "20130714", "170000Z", null);
        ICalTaskProvider s = new ICalTaskProvider(pccc.getPluginContext(), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), startOfDay);

        // confirm executor has a delay
        assertTrue(executor.hasDelays());

        s.clearAllTasks();

        // confirm executor has no delays
        assertFalse(executor.hasDelays());
    }

    @Test
    public void testLoadScheduleWithSingleEvent() throws Exception {
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        DateTimeZone tz = DateTimeZone.forID("GMT");
        MockTaskManager manager = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(manager, pccc, "20130714", "170000Z", null);

        // assert what happens the day OF the event
        long startOfDay = DateHelper.getTime(2013, 7, 14, 0, 0, 0, tz);

        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(manager);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), startOfDay);

        // verify task was scheduled -- should have been scheduled to execute in 61200 seconds (17 hours)
        assertEquals(61200000, (long) executor.getDelayForTask(task.getContext()));

        // assert what happens the day AFTER the event
        startOfDay = DateHelper.getTime(2013, 7, 15, 0, 0, 0, tz);
        executor.clearDelays();
        s.resetForNewDay(startOfDay);

        // verify the task was created but not scheduled
        assertEquals(1, s.getCalendar().getComponents().size());

        // verify task was not scheduled
        assertFalse(executor.isTaskScheduled(task.getContext()));
        assertFalse(executor.hasDelays());
        assertNull(executor.getDelayForTask(task.getContext()));
    }

    @Test
    public void testEditEvent() throws Exception {
//        File file = File.createTempFile("hob", ".ics");
//        try {
//            String ical = "BEGIN:VCALENDAR\n" +
//                    "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
//                    "VERSION:2.0\n" +
//                    "BEGIN:VEVENT\n" +
//                    "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
//                    "DTSTART:20130714T170000Z\n" +
//                    "DTEND:20130714T170000Z\n" +
//                    "SUMMARY:My Task\n" +
//                    "X-ACTION-SET:foo\n" +
//                    "END:VEVENT\n" +
//                    "END:VCALENDAR";
//
//            String ical2 = "BEGIN:VCALENDAR\n" +
//                    "PRODID:-//Whizzo Software//Hobson 1.0//EN\n" +
//                    "VERSION:2.0\n" +
//                    "BEGIN:VEVENT\n" +
//                    "UID:15dee4fe-a841-4cf6-8d7f-76c3ad5492b1\n" +
//                    "DTSTART:20130714T170000Z\n" +
//                    "DTEND:20130714T170000Z\n" +
//                    "SUMMARY:My Edited Task\n" +
//                    "END:VEVENT\n" +
//                    "END:VCALENDAR";
//
//            // write out ICS to temp file
//            FileWriter fw = new FileWriter(file);
//            fw.append(ical);
//            fw.close();
//
//            MockTaskManager mgr = new MockTaskManager();
//            ICalTaskProvider p = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, DateTimeZone.forID("America/Denver"));
//            p.setTaskManager(mgr);
//            p.setScheduleExecutor(new MockScheduledTaskExecutor());
//            p.setScheduleFile(file);
//            p.start();
//
//            // make sure the task was created
//            assertEquals(1, mgr.getPublishedTasks().size());
//
//            // create task JSON
//            JSONObject json = new JSONObject();
//            json.put("name", "My Edited Task");
//            JSONArray conds = new JSONArray();
//            json.put("conditions", conds);
//            JSONObject cond = new JSONObject();
//            conds.put(cond);
//            cond.put("start", "20130714T170000Z");
//            JSONArray actions = new JSONArray();
//            json.put("actions", actions);
//            JSONObject action = new JSONObject();
//            actions.put(action);
//            action.put("pluginId", "com.whizzosoftware.hobson.server-api");
//            action.put("actionId", "log");
//            action.put("name", "My Edited Action");
//            JSONObject props = new JSONObject();
//            action.put("properties", props);
//            props.put("message", "foobar");
//
//            // update the task
//            TaskContext ctx = TaskContext.createLocal("pluginId", "15dee4fe-a841-4cf6-8d7f-76c3ad5492b1");
//            Map<String,Object> propValues = new HashMap<>();
//            propValues.put("date", "20130714");
//            propValues.put("time", "170000Z");
//            p.onUpdateTask(
//                ctx,
//                "My Edited Task",
//                "My Task Description",
//                new PropertyContainer(
//                    PropertyContainerClassContext.create(PluginContext.createLocal("pluginId"), "foo"),
//                    propValues
//                ),
//                new PropertyContainerSet(
//                    "log",
//                    null
//                ));
//            assertTrue(file.exists());
//
//            // read back file
//            Calendar cal = new CalendarBuilder().build(new FileInputStream(file));
//            assertEquals(1, cal.getComponents().size());
//            VEvent c = (VEvent)cal.getComponents().get(0);
//            assertEquals("My Edited Task", c.getProperty("SUMMARY").getValue());
//            assertEquals("15dee4fe-a841-4cf6-8d7f-76c3ad5492b1", c.getProperty("UID").getValue());
//            assertEquals("20130714T170000Z", c.getProperty("DTSTART").getValue());
//        } finally {
//            file.delete();
//        }
    }

    @Test
    public void testLoadScheduleWithSingleEventWithSunsetOffset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20141018", "SS30", null);


        long startOfDay = DateHelper.getTime(2014, 10, 18, 0, 0, 0, tz);

        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setLatitudeLongitude(39.3722, -104.8561);
        s.setScheduleExecutor(executor);

        s.onCreateTasks(Collections.singletonList(task.getContext()), startOfDay);

        // verify task was created
        assertEquals(1, s.getCalendar().getComponents().size());

        // verify task was scheduled -- should have been scheduled to execute in 61200 seconds (17 hours)
        assertEquals(67560000, (long)executor.getDelayForTask(task.getContext()));
    }

    @Test
    public void testDayReset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);
        MockTaskQueue executor = new MockTaskQueue();

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "090000Z", "FREQ=DAILY");

        long schedulerStart = DateHelper.getTime(2014, 7, 1, 8, 0, 0, tz);

        ICalTaskProvider s = new ICalTaskProvider(pccc.getPluginContext(), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), schedulerStart);

        // verify task was created but not run
        assertEquals(1, s.getCalendar().getComponents().size());

        // reload the file at midnight
        s.resetForNewDay(DateHelper.getTime(2014, 7, 2, 0, 0, 0, tz));

        // verify task was created but not executed
        assertEquals(1, s.getCalendar().getComponents().size());
    }

    @Test
    public void testDelayedDayReset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "000000Z", "FREQ=DAILY");

        // start the scheduler after the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        assertEquals(0, s.getCalendar().getComponents().size());

        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 17, 0, 0, tz));

        // verify task was not scheduled
        assertEquals(1, s.getCalendar().getComponents().size());
        assertFalse(executor.isTaskScheduled(task.getContext()));

        // start a new day 30 seconds after midnight -- this covers the corner case where a delay causes the
        // resetForNewDay() method to get fired slightly after midnight and there are tasks that should have
        // already executed by then
        s.resetForNewDay(DateHelper.getTime(2014, 7, 2, 0, 0, 30, tz));

        // verify task was not scheduled but task executed
        assertEquals(1, s.getCalendar().getComponents().size());
        assertFalse(executor.isTaskScheduled(task.getContext()));
        assertFalse((boolean) task.getProperties().get(ICalTask.PROP_SCHEDULED));
        assertEquals(1404345600000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testDayResetWithSolarOffsetTask() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "SS30", "FREQ=DAILY");

        // start the scheduler after the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setLatitudeLongitude(39.3722, -104.8561);
        s.setScheduleExecutor(executor);

        assertEquals(0, s.getCalendar().getComponents().size());

        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 22, 0, 0, tz));

        // verify task was not scheduled or executed
        assertEquals(1, s.getCalendar().getComponents().size());
        assertFalse(executor.isTaskScheduled(task.getContext()));
        assertNotNull(task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));

        // start a new day at midnight
        s.resetForNewDay(DateHelper.getTime(2014, 7, 2, 0, 0, 0, tz));

        // verify task was scheduled at appropriate time and task did not execute
        assertEquals(1, s.getCalendar().getComponents().size());
        assertTrue(executor.isTaskScheduled(task.getContext()));
        assertEquals(75600000, (long) executor.getDelayForTask(task.getContext()));
        assertEquals(1404356400000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        assertTrue((boolean) task.getProperties().get(ICalTask.PROP_SCHEDULED));
    }

    @Test
    public void testMonthlyNextRun() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "220000Z", "FREQ=MONTHLY");

        // start the scheduler when the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 23, 0, 0, tz));

        // verify task was created and its next run time
        assertEquals(1, s.getCalendar().getComponents().size());
        assertEquals(1406930400000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testYearlyNextRun() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "220000Z", "FREQ=YEARLY");

        // start the scheduler when the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 8, 1, 21, 0, 0, tz));

        // verify task was created and its next run time
        assertEquals(1, s.getCalendar().getComponents().size());
        assertEquals(1435788000000l, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testTaskRescheduling() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "100000Z", "FREQ=MINUTELY;INTERVAL=1");

        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider scheduler = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        scheduler.setTaskManager(mgr);
        scheduler.setScheduleExecutor(executor);

        assertFalse(executor.hasDelays());
        List<ICalTask> icts = scheduler.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 11, 0, 1, tz));

        // verify task was created and scheduled
        assertEquals(1, icts.size());
        assertEquals(1, scheduler.getCalendar().getComponents().size());
        assertTrue(executor.hasDelays());
        assertEquals(59000, (long) executor.getDelayForTask(task.getContext()));

        // force task to fire
        executor.clearDelays();
        scheduler.onTaskExecuted(icts.get(0), DateHelper.getTime(2014, 7, 1, 11, 1, 0, tz), true);

        // verify that task was scheduled again
        assertTrue(executor.hasDelays());
        assertEquals(60000, (long)executor.getDelayForTask(task.getContext()));
    }

    @Test
    public void testSunOffsetWithNoLatLong() throws Exception {
        DateTimeZone tz = DateTimeZone.getDefault();
        PropertyContainerClassContext pccc = PropertyContainerClassContext.create(PluginContext.createLocal("plugin1"), "schedule");
        MockTaskManager mgr = createMockTaskManager(pccc);

        HobsonTask task = createScheduleTask(mgr, pccc, "20140701", "SS30", "FREQ=DAILY");

        // start the scheduler after the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 22, 0, 0, tz));

        assertEquals(1, s.getCalendar().getComponents().size());
        assertTrue(task.getProperties().containsKey(ICalTask.PROP_ERROR));
    }

    private MockTaskManager createMockTaskManager(PropertyContainerClassContext pccc) {
        MockTaskManager mgr = new MockTaskManager();
        mgr.publishConditionClass(new TaskConditionClass(pccc, "schedule", "") {
            @Override
            public ConditionClassType getConditionClassType() {
                return ConditionClassType.trigger;
            }

            @Override
            public List<TypedProperty> createProperties() {
                return null;
            }

            @Override
            public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
                return true;
            }
        });
        return mgr;
    }

    private HobsonTask createScheduleTask(MockTaskManager mgr, PropertyContainerClassContext pccc, String date, String time, String recurrence) {
        mgr.createTask(HubContext.createLocal(), "My Task", null, createScheduleCondition(pccc, date, time, recurrence), null);
        return mgr.getCreatedTasks().iterator().next();
    }

    private List<PropertyContainer> createScheduleCondition(PropertyContainerClassContext pccc, String date, String time, String recurrence) {
        List<PropertyContainer> conditions = new ArrayList<>();
        Map<String,Object> values = new HashMap<>();
        values.put("date", date);
        values.put("time", time);
        if (recurrence != null) {
            values.put("recurrence", recurrence);
        }
        conditions.add(new PropertyContainer(pccc, values));
        return conditions;
    }
}
