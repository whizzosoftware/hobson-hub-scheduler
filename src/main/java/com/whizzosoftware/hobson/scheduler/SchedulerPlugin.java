/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.event.HobsonEvent;
import com.whizzosoftware.hobson.api.event.HubConfigurationUpdateEvent;
import com.whizzosoftware.hobson.api.hub.HubConfigurationClass;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.PluginType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.TaskProvider;
import com.whizzosoftware.hobson.api.variable.HobsonVariable;
import com.whizzosoftware.hobson.api.variable.VariableContext;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.scheduler.condition.ScheduleConditionClass;
import com.whizzosoftware.hobson.scheduler.queue.LocalTaskQueue;
import com.whizzosoftware.hobson.scheduler.ical.ICalTaskProvider;
import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hobson plugin that provides scheduling capabilities.
 *
 * @author Dan Noguerol
 */
public class SchedulerPlugin extends AbstractHobsonPlugin implements DayResetListener {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerPlugin.class);

    private static final String SUNRISE = "sunrise";
    private static final String SUNSET = "sunset";

    private ICalTaskProvider taskProvider;
    private Double latitude;
    private Double longitude;

    public SchedulerPlugin(String pluginId) {
        super(pluginId);
    }

    @Override
    public void onStartup(PropertyContainer config) {
        // get latitude and longitude
        latitude = getHubLatitude();
        longitude = getHubLongitude();

        // create an ical task provider
        taskProvider = new ICalTaskProvider(getContext(), latitude, longitude);
        taskProvider.setScheduleExecutor(new LocalTaskQueue(getTaskManager()));
        taskProvider.setTaskManager(getTaskManager());
        taskProvider.setDayResetListener(this);
        taskProvider.start();

        // publish conditions that this plugin can trigger
        publishConditionClass(new ScheduleConditionClass(getContext()));

        // set the initial sunrise and sunset
        String sunrise = null;
        String sunset = null;
        String ss[] = SolarHelper.getSunriseSunset(latitude, longitude, DateTimeZone.getDefault(), System.currentTimeMillis());
        if (ss != null) {
            sunrise = ss[0];
            sunset = ss[1];
        }

        // publish sunrise/sunset global variables
        publishGlobalVariable(SUNRISE, sunrise, HobsonVariable.Mask.READ_ONLY);
        publishGlobalVariable(SUNSET, sunset, HobsonVariable.Mask.READ_ONLY);

        // set the plugin to running status
        setStatus(new PluginStatus(PluginStatus.Code.RUNNING));
    }

    @Override
    public void onShutdown() {
        taskProvider.stop();
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return null;
    }

    public TaskProvider getTaskProvider() {
        return taskProvider;
    }

    @Override
    public PluginType getType() {
        return PluginType.CORE;
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {
    }

    @Override
    public String getName() {
        return "Hobson Scheduler";
    }

    @Override
    public void onHobsonEvent(HobsonEvent event) {
        super.onHobsonEvent(event);

        // update latitude and longitude if they've changed
        if (event instanceof HubConfigurationUpdateEvent) {
            updateLatitudeLongitude(getHubLatitude(), getHubLongitude());
        }
    }

    @Override
    public void onDayReset(long now) {
        logger.debug("Day was reset - calculating sunrise/sunset");

        // set global variables for sunrise and sunset at the start of each new day
        updateSunriseSunset(now);
    }

    private Double getHubLatitude() {
        return (Double)getHub().getConfiguration().getPropertyValue(HubConfigurationClass.LATITUDE);
    }

    private Double getHubLongitude() {
        return (Double)getHub().getConfiguration().getPropertyValue(HubConfigurationClass.LONGITUDE);
    }

    private void updateLatitudeLongitude(Double latitude, Double longitude) {
        if (this.latitude == null || !this.latitude.equals(latitude) || this.longitude == null || !this.longitude.equals(longitude)) {
            logger.debug("Latitude and longitude have changed");
            this.latitude = latitude;
            this.longitude = longitude;
            taskProvider.setLatitudeLongitude(latitude, longitude);
            updateSunriseSunset(System.currentTimeMillis());
        }
    }

    private void updateSunriseSunset(long now) {
        String sunrise = null;
        String sunset = null;
        if (latitude != null && longitude != null) {
            String[] ss = SolarHelper.getSunriseSunset(latitude, longitude, DateTimeZone.forID("America/Denver"), now);
            sunrise = ss[0];
            sunset = ss[1];
            logger.debug("Sunrise: {}, sunset: {}", sunrise, sunset);
        }
        List<VariableUpdate> updates = new ArrayList<>();
        updates.add(new VariableUpdate(VariableContext.createGlobal(getContext(), SUNRISE), sunrise));
        updates.add(new VariableUpdate(VariableContext.createGlobal(getContext(), SUNSET), sunset));
        fireVariableUpdateNotifications(updates);
    }
}
