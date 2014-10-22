/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.action.manager.ActionManager;
import com.whizzosoftware.hobson.api.trigger.HobsonTrigger;
import com.whizzosoftware.hobson.api.trigger.TriggerProvider;
import com.whizzosoftware.hobson.api.util.filewatch.FileWatcherListener;
import com.whizzosoftware.hobson.api.util.filewatch.FileWatcherThread;
import com.whizzosoftware.hobson.bootstrap.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.scheduler.TriggerExecutionListener;
import com.whizzosoftware.hobson.scheduler.executor.ScheduledTriggerExecutor;
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
import org.json.JSONObject;
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
public class ICalTriggerProvider implements TriggerProvider, FileWatcherListener, TriggerExecutionListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PROVIDER = "com.whizzosoftware.hobson.hub.hobson-hub-scheduler";

    private static final long MS_24_HOURS = 86400000;

    private String pluginId;
    private ActionManager actionManager;
    private Calendar calendar;
    private ScheduledTriggerExecutor executor;
    private final Map<String,ICalTrigger> triggerMap = new HashMap<>();
    private File scheduleFile;
    private FileWatcherThread watcherThread;
    private ScheduledThreadPoolExecutor resetDayExecutor;
    private Double latitude;
    private Double longitude;
    private TimeZone timeZone;
    private boolean running = true;

    public ICalTriggerProvider(String pluginId) {
        this(pluginId, TimeZone.getDefault());
    }

    public ICalTriggerProvider(String pluginId, TimeZone timeZone) {
        this.pluginId = pluginId;
        this.timeZone = timeZone;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

    public void setLatitude(String latitude) {
        this.latitude = Double.parseDouble(latitude);
    }

    public void setLongitude(String longitude) {
        this.longitude = Double.parseDouble(longitude);
    }

    @Override
    synchronized public Collection<HobsonTrigger> getTriggers() {
        return new ArrayList<HobsonTrigger>(triggerMap.values());
    }

    @Override
    synchronized public HobsonTrigger getTrigger(String triggerId) {
        return triggerMap.get(triggerId);
    }

    @Override
    synchronized public void addTrigger(Object trigger) {
        try {
            JSONObject json = (JSONObject)trigger;
            ICalTrigger ict = new ICalTrigger(actionManager, PROVIDER, json);
            ict.setLatitude(latitude);
            ict.setLongitude(longitude);
            calendar.getComponents().add(ict.getVEvent());
            writeFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean addTrigger(ICalTrigger trigger, long now) throws Exception {
        return addTrigger(trigger, now, DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getTimeInMillis());
    }

    protected boolean addTrigger(ICalTrigger trigger, long now, long endOfToday) throws Exception {
        logger.debug("Adding task {} with ID: {}", trigger.getName(), trigger.getId());
        triggerMap.put(trigger.getId(), trigger);
        return scheduleNextRun(trigger, now, endOfToday);
    }

    protected boolean scheduleNextRun(ICalTrigger trigger, long now, long endOfToday) throws Exception {
        // check if there are any runs in the next two days
        List<Long> todaysRunTimes = trigger.getRunsDuringInterval(now, now + 86400000l);
        // if not, check if there are any runs in the next 6 weeks
        if (todaysRunTimes.size() == 0 || (todaysRunTimes.size() == 1 && todaysRunTimes.get(0) == now)) {
            todaysRunTimes = trigger.getRunsDuringInterval(now, now + 3628800000l);
            // it not, check if there are any runs in the next 53 weeks
            if (todaysRunTimes.size() == 0 || (todaysRunTimes.size() == 1 && todaysRunTimes.get(0) == now)) {
                todaysRunTimes = trigger.getRunsDuringInterval(now, now + 32054400000l);
            }
        }
        if (todaysRunTimes != null && todaysRunTimes.size() > 0) {
            long nextRunTime = 0;
            for (Long l : todaysRunTimes) {
                if (l - now > 0) {
                    nextRunTime = l;
                    break;
                }
            }
            if (nextRunTime > 0) {
                trigger.getProperties().put(ICalTrigger.PROP_NEXT_RUN_TIME, nextRunTime);
                if (nextRunTime < endOfToday) {
                    trigger.getProperties().put(ICalTrigger.PROP_SCHEDULED, true);
                    executor.schedule(trigger, nextRunTime - now);
                    return true;
                }
            }
        }
        trigger.getProperties().put(ICalTrigger.PROP_SCHEDULED, false);
        return false;
    }

    @Override
    synchronized public void updateTrigger(String triggerId, String name, Object data) {
        // TODO
    }

    @Override
    synchronized public void deleteTrigger(String triggerId) {
        ICalTrigger trigger = triggerMap.get(triggerId);
        if (trigger != null) {
            // remove the trigger from the calendar and write out a new calendar file
            calendar.getComponents().remove(trigger.getVEvent());
            writeFile();
        } else {
            logger.error("Unable to find trigger for removal: {}", triggerId);
        }
    }

    public void setScheduleFile(File scheduleFile) {
        this.scheduleFile = scheduleFile;
        restartFileWatcher();
        reloadScheduleFile();
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
                logger.info("Scheduler loading file: {}", scheduleFile.getAbsolutePath());
                loadICSStream(new FileInputStream(scheduleFile), System.currentTimeMillis());
            } catch (Exception e) {
                throw new HobsonRuntimeException("Error setting schedule file", e);
            }
        }
    }

    public void setScheduleExecutor(ScheduledTriggerExecutor executor) {
        this.executor = executor;
        executor.start();
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    public void start() {
        long initialDelay = DateHelper.getMillisecondsUntilMidnight(System.currentTimeMillis(), timeZone);

        logger.info("New day will start in {} seconds", (initialDelay / 1000));
        resetDayExecutor = new ScheduledThreadPoolExecutor(1);
        resetDayExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                resetForNewDay(System.currentTimeMillis());
            }
        }, initialDelay, MS_24_HOURS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;

        stopFileWatcher();

        resetDayExecutor.shutdownNow();
        resetDayExecutor = null;

        executor.stop();
        executor = null;

        triggerMap.clear();
    }

    public void resetForNewDay(long now) {
        try {
            refreshLocalCalendarData(now, true);
        } catch (Exception e) {
            logger.error("Error reloading calendar file on day reset", e);
        }
    }

    @Override
    public void onFileChanged(File ruleFile) {
        try {
            logger.debug("Detected calendar file change");
            // load and parse the new calendar file
            loadICSStream(new FileInputStream(ruleFile), System.currentTimeMillis());
        } catch (FileNotFoundException e) {
            logger.error("Unable to find schedule file at " + scheduleFile.getAbsolutePath(), e);
        } catch (Exception e) {
            logger.error("Error loading calendar file; will continue to use previous one", e);
        }
    }

    @Override
    public void onTriggerExecuted(ICalTrigger trigger, long now) {
        if (running) {
            // check if the task needs to execute again today
            try {
                long endOfDay = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getTimeInMillis();
                logger.debug("Task is done executing; checking for any more runs between {} and {}", now, endOfDay);
                scheduleNextRun(trigger, now, endOfDay);
            } catch (Exception e) {
                logger.error("Unable to determine if task needs to run again today", e);
            }
        }
    }

    synchronized protected void clearAllTasks() {
        logger.debug("Clearing all tasks");
        executor.cancelAll();
        triggerMap.clear();
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

        // determine first & last milliseconds of today
        long startOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 0, 0, 0, 0).getTimeInMillis();
        long endOfToday = DateHelper.getTimeInCurrentDay(now, timeZone, 23, 59, 59, 999).getTimeInMillis();

        logger.debug("Checking task applicability between {} and {}", new Date(now), new Date(endOfToday));

        // iterate through all events and schedule them if they are supposed to run today
        ComponentList eventList = calendar.getComponents(Component.VEVENT);
        for (Object anEventList : eventList) {
            VEvent event = (VEvent)anEventList;
            ICalTrigger trigger = new ICalTrigger(actionManager, PROVIDER, event, this);
            trigger.setLatitude(latitude);
            trigger.setLongitude(longitude);
            // if task wasn't added and it's a day reset, check if the task should have already run today
            if (!addTrigger(trigger, now, endOfToday) && wasDayReset) {
                List<Long> runTimes = trigger.getRunsDuringInterval(startOfToday, endOfToday);
                // if it should have run already, manually run it
                if (runTimes != null && runTimes.size() > 0 && runTimes.get(0) < now) {
                    trigger.run(now);
                }
            }
        }
    }

    protected void stopFileWatcher() {
        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }
    }

    protected void startFileWatcher() {
        // start a new file watcher thread
        try {
            watcherThread = new FileWatcherThread(scheduleFile, this);
            watcherThread.start();
        } catch (IOException e) {
            logger.error("Error watching schedule file; changes will not be automatically detected", e);
        }
    }

    protected void restartFileWatcher() {
        stopFileWatcher();
        startFileWatcher();
    }

    protected void writeFile() {
        try {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, new FileOutputStream(scheduleFile));
        } catch (Exception e) {
            logger.error("Error writing schedule file", e);
        }
    }
}
