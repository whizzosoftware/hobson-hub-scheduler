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

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.task.*;
import com.whizzosoftware.hobson.scheduler.DayResetListener;
import com.whizzosoftware.hobson.scheduler.SchedulingException;
import com.whizzosoftware.hobson.scheduler.TaskNotFoundException;
import com.whizzosoftware.hobson.scheduler.condition.TriggerConditionListener;
import com.whizzosoftware.hobson.scheduler.queue.TaskQueue;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.joda.time.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A Scheduler implementation that uses the iCal (RFC 5445) format.
 *
 * @author Dan Noguerol
 */
public class ICalTaskProvider implements TaskProvider, TriggerConditionListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final long MS_24_HOURS = 86400000;

    private PluginContext pluginContext;
    private TaskManager taskManager;
    private DayResetListener dayResetListener;
    private Calendar calendar = new Calendar();
    private TaskQueue taskQueue;
    private ScheduledThreadPoolExecutor resetDayExecutor = new ScheduledThreadPoolExecutor(1);
    private Double latitude;
    private Double longitude;
    private DateTimeZone timeZone;
    private boolean running = false;

    public ICalTaskProvider(PluginContext pluginContext, Double latitude, Double longitude) {
        this(pluginContext, latitude, longitude, DateTimeZone.getDefault());
    }

    public ICalTaskProvider(PluginContext pluginContext, Double latitude, Double longitude, DateTimeZone timeZone) {
        this.pluginContext = pluginContext;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = timeZone;
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setDayResetListener(DayResetListener dayResetListener) {
        this.dayResetListener = dayResetListener;
    }

    public PluginContext getPluginContext() {
        return pluginContext;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setLatitudeLongitude(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        try {
            refreshLocalCalendarData(System.currentTimeMillis(), false);
        } catch (Exception e) {
            logger.error("Error refreshing calendar data", e);
        }
    }

    /**
     * Adds a new task.
     *
     * @param task the task to add
     * @param now the current time
     * @param wasDayReset indicates whether this is being done as part of a new day reset
     *
     * @return a boolean indicating whether the task should be run immediately
     *
     * @throws Exception on failure
     */
    protected boolean addTask(ICalTask task, long now, boolean wasDayReset) throws Exception {
        logger.trace("Adding task: {}", task.getContext());
        return scheduleNextRun(task, now, wasDayReset);
    }

    /**
     * Schedules the next run of a task.
     *
     * @param task the task to schedule
     * @param now the current time
     * @param wasDayReset indicates whether this is being done as part of a new day reset
     *
     * @return a boolean indicating whether the task should be run immediately
     *
     * @throws Exception on failure
     */
    protected boolean scheduleNextRun(ICalTask task, long now, boolean wasDayReset) throws Exception {
        logger.trace("Attempting to schedule next run of task: {}", task.getContext());

        if (taskQueue == null) {
            throw new HobsonRuntimeException("No task executor configured");
        }

        long startOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 0, 0, 0, 0).getMillis();
        long endOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getMillis();
        boolean shouldRunToday = false;
        Map<String,Object> properties = new HashMap<>();

        properties.put(ICalTask.PROP_SCHEDULED, false);

        try {
            // check if there is more than 1 run in the next two days
            List<Long> todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 86400000l, timeZone);
            // if not, check if there is more than 1 run in the next 6 weeks
            if (todaysRunTimes.size() < 2) {
                todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 3628800000l, timeZone);
                // it not, check if there is more than 1 run in the next 53 weeks
                if (todaysRunTimes.size() < 2) {
                    todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 32054400000l, timeZone);
                }
            }

            if (todaysRunTimes.size() > 0) {
                long nextRunTime = 0;
                for (Long l : todaysRunTimes) {
                    if (l - now < 0 && wasDayReset) {
                        shouldRunToday = true;
                        logger.trace("Task will run today");
                    } else if (l - now > 0) {
                        nextRunTime = l;
                        break;
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Next run time for task {}: {}", task.getContext(), nextRunTime > 0 ? new Date(nextRunTime) : "Unknown");
                }
                if (nextRunTime > 0) {
                    properties.put(ICalTask.PROP_NEXT_RUN_TIME, nextRunTime);
                    if (nextRunTime < endOfToday) {
                        properties.put(ICalTask.PROP_SCHEDULED, true);
                        taskQueue.schedule(task.getContext(), nextRunTime - now);
                    }
                }
            }
        } catch (SchedulingException e) {
            properties.put(ICalTask.PROP_ERROR, e.getLocalizedMessage());
        }

        taskManager.updateTaskProperties(task.getContext(), properties);

        return shouldRunToday;
    }

    public void setScheduleExecutor(TaskQueue executor) {
        this.taskQueue = executor;
    }

    public void start() {
        if (!running) {
            taskQueue.start();
            scheduleNextWakeup();
            running = true;
        }
    }

    public void stop() {
        running = false;

        resetDayExecutor.shutdownNow();
        resetDayExecutor = null;

        taskQueue.stop();
        taskQueue = null;
    }

    public void resetForNewDay(long now) {
        logger.debug("Resetting for new day at {}", new DateTime(now));

        // alert listener
        if (dayResetListener != null) {
            dayResetListener.onDayReset(now);
        }

        // refresh the internal calendar data to identify new tasks that should be scheduled
        try {
            refreshLocalCalendarData(now, true);
        } catch (Exception e) {
            logger.error("Error reloading calendar file on day reset", e);
        }

        // schedule the next run
        scheduleNextWakeup();
    }

    private void scheduleNextWakeup() {
        long now = System.currentTimeMillis();
        long delay = DateHelper.getMillisecondsUntilMidnight(now, timeZone);
        logger.debug("New day will start at {} ({} seconds)", new DateTime(now + delay), (delay / 1000));
        resetDayExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                resetForNewDay(System.currentTimeMillis());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onTriggerCondition(ICalTask task, long now) {
        onTaskExecuted(task, now, false);
    }

    /**
     * Callback when a task is executed.
     *
     * @param task the task that executed
     * @param now the current time
     * @param forceCheck post-process regardless of running state?
     */
    protected void onTaskExecuted(ICalTask task, long now, boolean forceCheck) {
        // notify task manager that the trigger condition has fired
        if (taskManager != null) {
            taskManager.fireTaskTrigger(task.getContext());
        } else {
            logger.error("Task trigger condition fired but no task manager to notify");
        }

        if (running || forceCheck) {
            // check if the task needs to execute again today
            try {
                long endOfDay = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getMillis();
                logger.debug("Task is done executing; checking for any more runs between {} and {}", now, endOfDay);
                scheduleNextRun(task, now, false);
            } catch (Exception e) {
                logger.error("Unable to determine if task needs to run again today", e);
            }
        }
    }

    synchronized protected void clearAllTasks() {
        logger.debug("Clearing all tasks");
        taskQueue.cancelAll();
    }

    synchronized protected void refreshLocalCalendarData(long now, boolean wasDayReset) throws Exception {
        if (taskQueue == null) {
            throw new Exception("Can't load a schedule without a configured executor");
        }

        // clear all existing scheduled tasks
        clearAllTasks();

        // iterate through all events, add them to the internal store and schedule them if they are supposed to run today
        ComponentList eventList = calendar.getComponents(Component.VEVENT);
        for (Object anEventList : eventList) {
            VEvent event = (VEvent)anEventList;
            ICalTask task = new ICalTask(pluginContext, event, this);
            task.setLocation(latitude, longitude);
            if (addTask(task, now, wasDayReset)) {
                task.run(now);
            }
        }
    }

    @Override
    public void onRegisterTasks(Collection<TaskContext> tasks) {
        onCreateTasks(tasks, System.currentTimeMillis());
    }

    @Override
    public void onCreateTask(TaskContext ctx) {
        HobsonTask task = taskManager.getTask(ctx);
        if (task != null && task.isEnabled()) {
            onCreateTask(task, System.currentTimeMillis());
        }
    }

    protected ICalTask onCreateTask(HobsonTask task, long startOfDay) {
        try {
            ICalTask ict = new ICalTask(task.getContext(), TaskHelper.getTriggerCondition(taskManager, task.getConditions()));
            ict.setLocation(latitude, longitude);
            calendar.getComponents().add(ict.getVEvent());
            addTask(ict, startOfDay, false);
            return ict;
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error creating task", e);
        }
    }

    protected List<ICalTask> onCreateTasks(Collection<TaskContext> tasks, long startOfDay) {
        List<ICalTask> results = new ArrayList<>();

        for (TaskContext ctx : tasks) {
            HobsonTask task = taskManager.getTask(ctx);
            if (task.isEnabled()) {
                results.add(onCreateTask(task, startOfDay));
            }
        }

        return results;
    }

    @Override
    public void onUpdateTask(TaskContext ctx) {
        logger.trace("Detected update for task {}", ctx);
        HobsonTask task = taskManager.getTask(ctx);
        if (task != null) {
            onDeleteTask(ctx, false);
            if (task.isEnabled()) {
                logger.trace("Task is enabled so re-adding");
                long now = System.currentTimeMillis();
                onCreateTask(task, now);
            } else {
                fireNotScheduled(ctx);
            }
        }
    }

    @Override
    public void onDeleteTask(TaskContext ctx) {
        onDeleteTask(ctx, true);
    }

    private void onDeleteTask(TaskContext ctx, boolean fireUpdate) {
        // first cancel the task if it is queued to run
        try {
            taskQueue.cancel(ctx);
            logger.debug("Removed task {} from task queue", ctx);
        } catch (TaskNotFoundException e) {
            logger.debug("Unable to find task {} to cancel; ignoring", ctx);
        }

        // then remove it from the calendar
        Component c = null;
        for (Object e : calendar.getComponents()) {
            if (e instanceof VEvent) {
                if (((VEvent)e).getUid().getValue().equals(ctx.getTaskId())) {
                    c = (Component)e;
                }
            }
        }
        if (c != null) {
            logger.debug("Removing task from calendar: {}", ctx);
            calendar.getComponents().remove(c);
        }

        if (fireUpdate) {
            fireNotScheduled(ctx);
        }
    }

    private void fireNotScheduled(TaskContext ctx) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ICalTask.PROP_SCHEDULED, false);
        properties.put(ICalTask.PROP_NEXT_RUN_TIME, 0);
        taskManager.updateTaskProperties(ctx, properties);
    }
}
