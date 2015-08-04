/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.PropertyContainerSet;
import com.whizzosoftware.hobson.api.task.HobsonTask;
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.TaskManager;
import com.whizzosoftware.hobson.scheduler.SchedulerPlugin;
import com.whizzosoftware.hobson.scheduler.TaskExecutionListener;
import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;

/**
 * An implementation of HobsonTask for iCal scheduled events.
 *
 * @author Dan Noguerol
 */
public class ICalTask extends HobsonTask implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final String PROP_SUN_OFFSET = "X-SUN-OFFSET";
    protected static final String PROP_ACTION_SET = "X-ACTION-SET";
    protected static final String PROP_NEXT_RUN_TIME = "nextRunTime";
    protected static final String PROP_SCHEDULED = "scheduled";
    protected static final String PROP_ERROR = "error";

    private TaskManager taskManager;
    private VEvent event;
    private TaskExecutionListener listener;
    private Double latitude;
    private Double longitude;
    private SolarOffset solarOffset;

    public ICalTask(TaskManager taskManager, PluginContext pluginContext, VEvent event, TaskExecutionListener listener) throws InvalidVEventException {
        this.taskManager = taskManager;
        this.event = event;
        this.listener = listener;

        if (event != null) {
            // set the task context
            setContext(TaskContext.create(pluginContext, event.getUid().getValue()));

            // set the task name
            if (event.getSummary() != null) {
                setName(event.getSummary().getValue());
            }

            // adjust start time if sun offset is set
            Property sunOffset = event.getProperty(PROP_SUN_OFFSET);
            if (sunOffset != null) {
                try {
                    solarOffset = new SolarOffset(sunOffset.getValue());
                } catch (ParseException e) {
                    throw new InvalidVEventException("Invalid X-SUN-OFFSET", e);
                }
            }

            // set the condition set
            setConditionSet(createConditionSet(event));

            // set the action set
            Property actionSetId = event.getProperty(PROP_ACTION_SET);
            if (actionSetId != null) {
                setActionSet(new PropertyContainerSet(actionSetId.getValue(), null));
            } else {
                throw new InvalidVEventException("ICalEventTask event must have a X-ACTION-SET property");
            }
        } else {
            throw new InvalidVEventException("ICalEventTask must have a non-null event");
        }
    }

    public ICalTask(TaskManager taskManager, TaskContext ctx, String name, String description, PropertyContainerSet conditionSet, PropertyContainerSet actionSet) {
        this.taskManager = taskManager;
        setContext(ctx);
        update(ctx.getTaskId(), name, description, conditionSet, actionSet);
    }

    protected PropertyContainerSet createConditionSet(VEvent event) {
        String date = null;
        String time = null;
        String recurrence = null;

        if (event.getStartDate() != null) {
            DateTime dt = new DateTime(event.getStartDate().getDate()).toDateTime(DateTimeZone.forID("GMT"));
            date = DateTimeFormat.forPattern("YYYYMMdd").print(dt);
            time = DateTimeFormat.forPattern("HHmmss'Z'").print(dt);
            Property p = event.getProperty(PROP_SUN_OFFSET);
            if (p != null) {
                time = p.getValue();
            }
        }

        RRule rrule = (RRule)event.getProperty("RRULE");
        if (rrule != null && rrule.getRecur() != null) {
            recurrence = rrule.getRecur().toString();
        }

        Map<String,Object> values = new HashMap<>();
        values.put("date", date);
        values.put("time", time);
        values.put("recurrence", recurrence);
        return new PropertyContainerSet(
            new PropertyContainer(
                PropertyContainerClassContext.create(getContext().getPluginContext(), SchedulerPlugin.SCHEDULE_CONDITION_CLASS_ID),
                values
            )
        );
    }

    public void update(String id, String name, String description, PropertyContainerSet conditionSet, PropertyContainerSet actionSet) {
        // at this point, we require an action set ID
        if (!actionSet.hasId()) {
            throw new HobsonRuntimeException("Action set has no ID");
        }

        setName(name);
        setDescription(description);
        setConditionSet(conditionSet);
        setActionSet(actionSet);

        this.event = new VEvent();
        event.getProperties().add(new Uid(id));
        event.getProperties().add(new Summary(name));

        try {
            if (conditionSet.hasPrimaryProperty()) {
                PropertyContainer triggerCondition = conditionSet.getPrimaryProperty();
                if (triggerCondition.hasPropertyValue("date") && triggerCondition.hasPropertyValue("time")) {
                    String date = ((String)triggerCondition.getPropertyValue("date")).replace("-", "");
                    String time = ((String)triggerCondition.getPropertyValue("time")).replace(":", "");

                    // if the time is relative to sunset (e.g. SR or SS), set the time to 000000 and set the sun offset property
                    if (time.startsWith("S")) {
                        event.getProperties().add(new DtStart(date + "T000000"));
                        event.getProperties().add(new XProperty(PROP_SUN_OFFSET, time));

                    // otherwise, set the time as-is
                    } else {
                        event.getProperties().add(new DtStart(date + "T" + time));
                    }
                }
                if (triggerCondition.hasPropertyValue("recurrence")) {
                    String r = (String)triggerCondition.getPropertyValue("recurrence");
                    if (r.length() > 0 && !"never".equalsIgnoreCase(r)) {
                        event.getProperties().add(new RRule(r));
                    }
                }
                if (actionSet.hasId()) {
                    event.getProperties().add(new XProperty(PROP_ACTION_SET, actionSet.getId()));
                }
            }
        } catch (ParseException e) {
            throw new HobsonRuntimeException("Error parsing recurrence rule", e);
        }
    }

    @Override
    public void run() {
        run(System.currentTimeMillis());
    }

    protected void run(long now) {
        logger.info("Task \"{}\" is executing", getName());

        Throwable error = null;

        // execute the actions but catch any exception or the task will never execute again
        try {
            executeActions(getActionSet());
        } catch (Throwable e) {
            error = e;
            logger.error("Error executing task " + getName(), e);
        }

        // notify the listener that the task has executed (whether successfully or unsuccessfully)
        if (listener != null) {
            listener.onTaskExecuted(this, now, error);
        }
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public VEvent getVEvent() {
        return event;
    }

    public List<Long> getRunsDuringInterval(long startTime, long endTime, DateTimeZone tz) throws SchedulingException {
        List<Long> results = new ArrayList<>();
        if (event != null) {
            // if there's a solar offset, reset the start time to the beginning of the day so that
            // we can see if the event should run at any point during the first to subsequent days
            if (solarOffset != null) {
                if (latitude == null || longitude == null) {
                    throw new SchedulingException("Unable to calculate sunrise/sunset; please set Hub latitude/longitude");
                }
                DateTime c = new DateTime(startTime, tz);
                startTime = c.withTimeAtStartOfDay().getMillis();
            }

            PeriodList periods = event.calculateRecurrenceSet(new Period(new net.fortuna.ical4j.model.DateTime(startTime), new net.fortuna.ical4j.model.DateTime(endTime)));
            for (Object period : periods) {
                // get the recurrence time
                long time = ((Period)period).getStart().getTime();

                // adjust time if there's an solar offset defined
                if (solarOffset != null) {
                    DateTime c = new DateTime(time, tz);
                    try {
                        c = SolarHelper.createCalendar(c.toLocalDate(), tz, solarOffset, latitude, longitude);
                        time = c.getMillis();
                    } catch (ParseException e) {
                        throw new SchedulingException("Error parsing solar offset", e);
                    }
                }

                results.add(time);
            }
        }
        return results;
    }

    private void executeActions(PropertyContainerSet actionSet) {
        if (taskManager != null) {
            if (actionSet.hasId()) {
                taskManager.executeActionSet(getContext().getHubContext(), actionSet.getId());
            } else {
                logger.error("Unable to execute task: action set has no ID");
            }
        } else {
            logger.error("No action manager is available to execute actions");
        }
    }
}
