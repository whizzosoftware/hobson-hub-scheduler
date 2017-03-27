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
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.MockTaskManager;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;
import com.whizzosoftware.hobson.scheduler.condition.ScheduleConditionClass;
import com.whizzosoftware.hobson.scheduler.queue.MockTaskQueue;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
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
    public void testClearAllTasks() throws Exception {
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));

        DateTimeZone tz = DateTimeZone.forID("GMT");
        MockTaskManager mgr = createMockTaskManager(scc);
        MockTaskQueue executor = new MockTaskQueue();

        // make sure executor has no delays already set
        assertFalse(executor.hasDelays());

        long startOfDay = DateHelper.getTime(2013, 7, 14, 0, 0, 0, tz);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20130714", "170000Z", null);
        ICalTaskProvider s = new ICalTaskProvider(scc.getContext().getPluginContext(), null, null, tz);
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
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        DateTimeZone tz = DateTimeZone.forID("GMT");
        MockTaskManager manager = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(manager, scc.getContext(), "20130714", "170000Z", null);

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
    public void testLoadScheduleWithSingleEventWithSunsetOffset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20141018", "SS30", null);


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
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));

        MockTaskManager mgr = createMockTaskManager(scc);
        MockTaskQueue executor = new MockTaskQueue();

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "090000Z", "FREQ=DAILY");

        long schedulerStart = DateHelper.getTime(2014, 7, 1, 8, 0, 0, tz);

        ICalTaskProvider s = new ICalTaskProvider(scc.getContext().getPluginContext(), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), schedulerStart);

        // verify task was created but not run
        assertEquals(1, s.getCalendar().getComponents().size());

        // verify task properties were set correctly
        Collection<HobsonTask> tasks = mgr.getTasks(HubContext.createLocal());
        assertEquals(1, tasks.size());
        HobsonTask t = tasks.iterator().next();
        assertTrue((boolean)t.getProperties().get(ICalTask.PROP_SCHEDULED));
        assertEquals(1404205200000L, t.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));

        // reload the file at midnight
        s.resetForNewDay(DateHelper.getTime(2014, 7, 2, 0, 0, 0, tz));

        // verify task was created but not executed
        assertEquals(1, s.getCalendar().getComponents().size());

        // verify task properties were set correctly
        tasks = mgr.getTasks(HubContext.createLocal());
        t = tasks.iterator().next();
        assertTrue((boolean)t.getProperties().get(ICalTask.PROP_SCHEDULED));
        assertEquals(1404291600000L, t.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        System.out.println(tasks);
    }

    @Test
    public void testDelayedDayReset() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "000000Z", "FREQ=DAILY");

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
        assertEquals(1404345600000L, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testDayResetWithSolarOffsetTask() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("America/Denver");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20170316", "SS+30", "FREQ=DAILY;INTERVAL=1");

        // start the scheduler after the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setLatitudeLongitude(39.3722, -104.8561);
        s.setScheduleExecutor(executor);

        assertEquals(0, s.getCalendar().getComponents().size());

        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2017, 3, 16, 22, 0, 0, tz));

        // verify task was not scheduled or executed
        assertEquals(1, s.getCalendar().getComponents().size());
        assertFalse(executor.isTaskScheduled(task.getContext()));
        assertNotNull(task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));

        // verify task properties were set correctly
        Collection<HobsonTask> tasks = mgr.getTasks(HubContext.createLocal());
        assertEquals(1, tasks.size());

        // start a new day at midnight
        s.resetForNewDay(DateHelper.getTime(2017, 3, 17, 0, 0, 0, tz));

        // verify task was scheduled at appropriate time and task did not execute
        assertEquals(1, s.getCalendar().getComponents().size());
        assertTrue(executor.isTaskScheduled(task.getContext()));
        assertEquals(70680000L, (long) executor.getDelayForTask(task.getContext()));
        assertEquals(1489801080000L, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        assertTrue((boolean) task.getProperties().get(ICalTask.PROP_SCHEDULED));

        // start next day at midnight
        s.resetForNewDay(DateHelper.getTime(2017, 3, 18, 0, 0, 0, tz));

        // verify task was scheduled at appropriate time and task did not execute
        assertEquals(1, s.getCalendar().getComponents().size());
        assertTrue(executor.isTaskScheduled(task.getContext()));
        assertEquals(70740000L, (long) executor.getDelayForTask(task.getContext()));
        assertEquals(1489887540000L, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
        assertTrue((boolean) task.getProperties().get(ICalTask.PROP_SCHEDULED));
    }

    @Test
    public void testMonthlyNextRun() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "220000Z", "FREQ=MONTHLY");

        // start the scheduler when the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 23, 0, 0, tz));

        // verify task was created and its next run time
        assertEquals(1, s.getCalendar().getComponents().size());
        assertEquals(1406930400000L, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testYearlyNextRun() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "220000Z", "FREQ=YEARLY");

        // start the scheduler when the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 8, 1, 21, 0, 0, tz));

        // verify task was created and its next run time
        assertEquals(1, s.getCalendar().getComponents().size());
        assertEquals(1435788000000L, task.getProperties().get(ICalTask.PROP_NEXT_RUN_TIME));
    }

    @Test
    public void testTaskRescheduling() throws Exception {
        DateTimeZone tz = DateTimeZone.forID("GMT");
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "100000Z", "FREQ=MINUTELY;INTERVAL=1");

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
        ScheduleConditionClass scc = new ScheduleConditionClass(PluginContext.createLocal("plugin1"));
        MockTaskManager mgr = createMockTaskManager(scc);

        HobsonTask task = createScheduleTask(mgr, scc.getContext(), "20140701", "SS30", "FREQ=DAILY");

        // start the scheduler after the task should have run
        MockTaskQueue executor = new MockTaskQueue();
        ICalTaskProvider s = new ICalTaskProvider(PluginContext.createLocal("pluginId"), null, null, tz);
        s.setTaskManager(mgr);
        s.setScheduleExecutor(executor);
        s.onCreateTasks(Collections.singletonList(task.getContext()), DateHelper.getTime(2014, 7, 1, 22, 0, 0, tz));

        assertEquals(1, s.getCalendar().getComponents().size());
        assertTrue(task.getProperties().containsKey(ICalTask.PROP_ERROR));
    }

    private MockTaskManager createMockTaskManager(TaskConditionClass pcc) {
        MockTaskManager mgr = new MockTaskManager();
        mgr.publishConditionClass(pcc);
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
