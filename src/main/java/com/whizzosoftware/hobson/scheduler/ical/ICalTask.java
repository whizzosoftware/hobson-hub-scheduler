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
import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.scheduler.SchedulingException;
import com.whizzosoftware.hobson.scheduler.SolarOffset;
import com.whizzosoftware.hobson.scheduler.condition.TriggerConditionListener;
import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.util.*;

/**
 * An implementation of HobsonTask for iCal scheduled events.
 *
 * @author Dan Noguerol
 */
public class ICalTask implements Runnable {
    protected static final String PROP_SUN_OFFSET = "X-SUN-OFFSET";
    protected static final String PROP_ACTION_SET = "X-ACTION-SET";
    protected static final String PROP_NEXT_RUN_TIME = "nextRunTime";
    protected static final String PROP_SCHEDULED = "scheduled";
    protected static final String PROP_ERROR = "error";

    private TaskContext taskContext;
    private VEvent event;
    private TriggerConditionListener listener;
    private Double latitude;
    private Double longitude;
    private SolarOffset solarOffset;

    public ICalTask(PluginContext pluginContext, VEvent event, TriggerConditionListener listener) throws InvalidVEventException {
        this.event = event;
        this.listener = listener;

        if (event != null) {
            // set the task context
            this.taskContext = TaskContext.create(pluginContext.getHubContext(), event.getUid().getValue());

            // adjust the time for any solar offset defined
            adjustForSolarOffset();
        } else {
            throw new InvalidVEventException("ICalEventTask must have a non-null event");
        }
    }

    public ICalTask(TaskContext taskContext, PropertyContainer triggerCondition) {
        this.taskContext = taskContext;
        update(taskContext, triggerCondition);
    }

    public TaskContext getContext() {
        return taskContext;
    }

    public void update(TaskContext taskContext, PropertyContainer triggerCondition) {
        this.event = new VEvent();
        event.getProperties().add(new Uid(taskContext.getTaskId()));

        try {
            if (triggerCondition != null) {
                if (triggerCondition.hasPropertyValue("date") && triggerCondition.hasPropertyValue("time")) {
                    String date = ((String)triggerCondition.getPropertyValue("date")).replace("-", "");
                    String time = ((String)triggerCondition.getPropertyValue("time")).replace(":", "");

                    // if the time is relative to sunset (e.g. SR or SS), set the time to 000000 and set the sun offset property
                    if (time.startsWith("S")) {
                        event.getProperties().add(new DtStart(date + "T000000"));
                        event.getProperties().add(new XProperty(PROP_SUN_OFFSET, time));
                        adjustForSolarOffset();
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
            }
        } catch (ParseException e) {
            throw new HobsonRuntimeException("Error parsing recurrence rule", e);
        }
    }

    @Override
    public void run() {
        run(System.currentTimeMillis());
    }

    protected void adjustForSolarOffset() {
        // adjust start time if sun offset is set
        Property sunOffset = event.getProperty(PROP_SUN_OFFSET);
        if (sunOffset != null) {
            try {
                solarOffset = new SolarOffset(sunOffset.getValue());
            } catch (ParseException e) {
                throw new HobsonRuntimeException("Invalid X-SUN-OFFSET", e);
            }
        }
    }

    protected void run(long now) {
        // notify the listener that the task has executed (whether successfully or unsuccessfully)
        if (listener != null) {
            listener.onTriggerCondition(this, now);
        }
    }

    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
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
}
