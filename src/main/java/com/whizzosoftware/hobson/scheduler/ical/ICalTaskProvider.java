/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.HobsonNotFoundException;
import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.TaskManager;
import com.whizzosoftware.hobson.api.task.TaskProvider;
import com.whizzosoftware.hobson.scheduler.DayResetListener;
import com.whizzosoftware.hobson.scheduler.TaskExecutionListener;
import com.whizzosoftware.hobson.scheduler.executor.ScheduledTaskExecutor;
import com.whizzosoftware.hobson.scheduler.util.DateHelper;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.TimeZone;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A Scheduler implementation that uses the iCal (RFC 5445) format.
 *
 * @author Dan Noguerol
 */
public class ICalTaskProvider implements TaskProvider, TaskExecutionListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long MS_24_HOURS = 86400000;

    private PluginContext ctx;
    private TaskManager taskManager;
    private DayResetListener dayResetListener;
    private Calendar calendar;
    private ScheduledTaskExecutor executor;
    private File scheduleFile;
    private ScheduledThreadPoolExecutor resetDayExecutor;
    private Double latitude;
    private Double longitude;
    private TimeZone timeZone;
    private boolean running = false;

    public ICalTaskProvider(PluginContext ctx, Double latitude, Double longitude) {
        this(ctx, latitude, longitude, TimeZone.getDefault());
    }

    public ICalTaskProvider(PluginContext ctx, Double latitude, Double longitude, TimeZone timeZone) {
        this.ctx = ctx;
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
        return ctx;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLatitudeLongitude(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        reloadScheduleFile();
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
        logger.debug("Adding task {} with ID: {}", task.getName(), task.getContext().getTaskId());
        if (taskManager != null) {
            taskManager.publishTask(task);
        } else {
            logger.error("No task manager set; unable to publish task");
        }
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
        long startOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 0, 0, 0, 0).getTimeInMillis();
        long endOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getTimeInMillis();
        boolean shouldRunToday = false;

        task.setProperty(ICalTask.PROP_SCHEDULED, false);

        try {
            // check if there is more than 1 run in the next two days
            List<Long> todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 86400000l);
            // if not, check if there is more than 1 run in the next 6 weeks
            if (todaysRunTimes.size() < 2) {
                todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 3628800000l);
                // it not, check if there is more than 1 run in the next 53 weeks
                if (todaysRunTimes.size() < 2) {
                    todaysRunTimes = task.getRunsDuringInterval(startOfToday, startOfToday + 32054400000l);
                }
            }

            if (todaysRunTimes.size() > 0) {
                long nextRunTime = 0;
                for (Long l : todaysRunTimes) {
                    if (l - now < 0 && wasDayReset) {
                        shouldRunToday = true;
                    } else if (l - now > 0) {
                        nextRunTime = l;
                        break;
                    }
                }
                if (nextRunTime > 0) {
                    task.setProperty(ICalTask.PROP_NEXT_RUN_TIME, nextRunTime);
                    if (nextRunTime < endOfToday) {
                        task.setProperty(ICalTask.PROP_SCHEDULED, true);
                        executor.schedule(task, nextRunTime - now);
                    }
                }
            }
        } catch (SchedulingException e) {
            task.setProperty(ICalTask.PROP_ERROR, e.getLocalizedMessage());
        }

        return shouldRunToday;
    }

    public void setScheduleFile(File scheduleFile) {
        this.scheduleFile = scheduleFile;

        if (running) {
            reloadScheduleFile();
        }
    }

    public void reloadScheduleFile() {
        if (scheduleFile != null) {
            try {
                // create empty calendar file if it doesn't exist
                if (!scheduleFile.exists()) {
                    Calendar calendar = new Calendar();
                    calendar.getProperties().add(new ProdId("-//Whizzo Software//Hobson 1.0//EN"));
                    calendar.getProperties().add(Version.VERSION_2_0);
                    calendar.getProperties().add(CalScale.GREGORIAN);
                    VJournal entry = new VJournal(new Date(), "Created");
                    entry.getProperties().add(new Uid(UUID.randomUUID().toString()));
                    calendar.getComponents().add(entry);
                    CalendarOutputter outputter = new CalendarOutputter();
                    outputter.output(calendar, new FileOutputStream(scheduleFile));
                }

                // load the calendar file
                logger.debug("Scheduler loading file: {}", scheduleFile.getAbsolutePath());
                loadICSStream(new FileInputStream(scheduleFile), System.currentTimeMillis());
            } catch (Exception e) {
                throw new HobsonRuntimeException("Error setting schedule file", e);
            }
        }
    }

    public void setScheduleExecutor(ScheduledTaskExecutor executor) {
        this.executor = executor;
    }

    public void start() {
        if (!running) {
            long initialDelay = DateHelper.getMillisecondsUntilMidnight(System.currentTimeMillis(), timeZone);

            executor.start();

            reloadScheduleFile();

            logger.debug("New day will start in {} seconds", (initialDelay / 1000));
            resetDayExecutor = new ScheduledThreadPoolExecutor(1);
            resetDayExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    resetForNewDay(System.currentTimeMillis());
                }
            }, initialDelay, MS_24_HOURS, TimeUnit.MILLISECONDS);

            running = true;
        }
    }

    public void stop() {
        running = false;

        resetDayExecutor.shutdownNow();
        resetDayExecutor = null;

        executor.stop();
        executor = null;
    }

    public void resetForNewDay(long now) {
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
    }

    @Override
    public void onTaskExecuted(ICalTask task, long now) {
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
        if (running || forceCheck) {
            // check if the task needs to execute again today
            try {
                long endOfDay = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getTimeInMillis();
                logger.debug("Task is done executing; checking for any more runs between {} and {}", now, endOfDay);
                scheduleNextRun(task, now, false);
            } catch (Exception e) {
                logger.error("Unable to determine if task needs to run again today", e);
            }
        }
    }

    synchronized protected void clearAllTasks() {
        logger.debug("Clearing all tasks");
        executor.cancelAll();
        if (taskManager != null) {
            taskManager.unpublishAllTasks(ctx);
        } else {
            logger.error("No task manager set; unable to clear tasks");
        }
    }

    synchronized protected void loadICSStream(InputStream is, long now) throws Exception {
        calendar = new CalendarBuilder().build(is);
        refreshLocalCalendarData(now, false);
    }

    synchronized protected void refreshLocalCalendarData(long now, boolean wasDayReset) throws Exception {
        if (executor == null) {
            throw new Exception("Can't load a schedule without a configured executor");
        }

        // clear all existing scheduled tasks
        clearAllTasks();

        // iterate through all events, add them to the internal store and schedule them if they are supposed to run today
        ComponentList eventList = calendar.getComponents(Component.VEVENT);
        for (Object anEventList : eventList) {
            VEvent event = (VEvent)anEventList;
            ICalTask task = new ICalTask(taskManager, ctx, event, this);
            task.setLatitude(latitude);
            task.setLongitude(longitude);
            if (addTask(task, now, wasDayReset)) {
                task.run(now);
            }
        }
    }

    protected void writeFile() {
        try {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, new FileOutputStream(scheduleFile));
        } catch (Exception e) {
            logger.error("Error writing schedule file", e);
        }
    }

    @Override
    public void onCreateTask(String name, String description, PropertyContainerSet conditionSet, PropertyContainerSet actionSet) {
        try {
            ICalTask ict = new ICalTask(taskManager, TaskContext.create(getPluginContext(), UUID.randomUUID().toString()), name, description, conditionSet, actionSet);
            ict.setLatitude(latitude);
            ict.setLongitude(longitude);

            calendar.getComponents().add(ict.getVEvent());
            writeFile();

            reloadScheduleFile();
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error creating task", e);
        }
    }

    @Override
    public void onUpdateTask(TaskContext ctx, String name, String description, PropertyContainerSet conditionSet, PropertyContainerSet actionSet) {
        try {
            HobsonTask task = taskManager.getTask(ctx);
            if (task instanceof ICalTask) {
                ICalTask icTask = (ICalTask)task;

                // remove scheduled event from calendar
                calendar.getComponents().remove(icTask.getVEvent());
                // update task
                icTask.update(task.getContext().getTaskId(), name, description, conditionSet, actionSet);
                // add new scheduled event to calendar
                calendar.getComponents().add(icTask.getVEvent());
                writeFile();

                reloadScheduleFile();
            } else {
                throw new HobsonNotFoundException("The specified task was not found or invalid");
            }
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error updating task", e);
        }
    }

    @Override
    public void onDeleteTask(TaskContext ctx) {
        try {
            HobsonTask task = taskManager.getTask(ctx);

            if (task != null && task instanceof ICalTask) {
                ICalTask icTask = (ICalTask)task;

                // remove the task from the calendar and write out a new calendar file
                calendar.getComponents().remove(icTask.getVEvent());
                writeFile();

                reloadScheduleFile();
            } else {
                logger.error("Unable to find task for removal: {}", task.getContext());
            }
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error deleting task", e);
        }
    }
}
