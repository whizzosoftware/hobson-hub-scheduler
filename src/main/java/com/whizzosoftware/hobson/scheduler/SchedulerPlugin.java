/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.config.Configuration;
import com.whizzosoftware.hobson.api.event.EventTopics;
import com.whizzosoftware.hobson.api.event.HobsonEvent;
import com.whizzosoftware.hobson.api.event.HubConfigurationUpdateEvent;
import com.whizzosoftware.hobson.api.hub.HubLocation;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.PluginType;
import com.whizzosoftware.hobson.api.util.UserUtil;
import com.whizzosoftware.hobson.api.variable.HobsonVariable;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.scheduler.executor.ThreadPoolScheduledTaskExecutor;
import com.whizzosoftware.hobson.scheduler.ical.ICalTaskProvider;
import com.whizzosoftware.hobson.scheduler.util.SolarHelper;
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
    public void onStartup(Configuration config) {
        // get latitude and longitude
        HubLocation hl = getHubLocation();
        latitude = hl.getLatitude();
        longitude = hl.getLongitude();

        // create an ical task provider
        taskProvider = new ICalTaskProvider(getId(), latitude, longitude);
        taskProvider.setScheduleExecutor(new ThreadPoolScheduledTaskExecutor());
        taskProvider.setScheduleFile(getDataFile("schedule.ics"));
        publishTaskProvider(taskProvider);
        taskProvider.start();

        // set the initial sunrise and sunset
        String sunrise = null;
        String sunset = null;
        String ss[] = SolarHelper.getSunriseSunset(latitude, longitude, System.currentTimeMillis());
        if (ss != null) {
            sunrise = ss[0];
            sunset = ss[1];
        }

        // publish sunrise/sunset global variables
        publishGlobalVariable(SUNRISE, sunrise, HobsonVariable.Mask.READ_ONLY);
        publishGlobalVariable(SUNSET, sunset, HobsonVariable.Mask.READ_ONLY);

        // set the plugin to running status
        setStatus(new PluginStatus(PluginStatus.Status.RUNNING));
    }

    @Override
    public void onShutdown() {
        taskProvider.stop();
        super.onShutdown();
    }

    @Override
    public PluginType getType() {
        return PluginType.CORE;
    }

    @Override
    public String[] getEventTopics() {
        return new String[] {EventTopics.CONFIG_TOPIC};
    }

    @Override
    public void onPluginConfigurationUpdate(Configuration config) {
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
            HubLocation hl = getHubLocation();
            updateLatitudeLongitude(hl.getLatitude(), hl.getLongitude());
        }
    }

    @Override
    public void onDayReset(long now) {
        logger.debug("Day was reset - calculating sunrise/sunset");

        // set global variables for sunrise and sunset at the start of each new day
        updateSunriseSunset(now);
    }

    private HubLocation getHubLocation() {
        return getHubManager().getHubLocation(UserUtil.DEFAULT_USER, UserUtil.DEFAULT_HUB);
    }

    private void updateLatitudeLongitude(Double latitude, Double longitude) {
        if (this.latitude == null || !this.latitude.equals(latitude) || this.longitude == null || !this.longitude.equals(longitude)) {
            logger.debug("Latitude and logitude have changed");
            this.latitude = latitude;
            this.longitude = longitude;
            taskProvider.setLatitudeLongitude(latitude, longitude);
            updateSunriseSunset(System.currentTimeMillis());
        }
    }

    private void updateSunriseSunset(long now) {
        if (latitude != null && longitude != null) {
            String[] ss = SolarHelper.getSunriseSunset(latitude, longitude, now);
            String sunrise = ss[0];
            String sunset = ss[1];
            logger.debug("Sunrise: {}, sunset: {}", sunrise, sunset);

            List<VariableUpdate> updates = new ArrayList<>();
            updates.add(new VariableUpdate(getId(), SUNRISE, sunrise));
            updates.add(new VariableUpdate(getId(), SUNSET, sunset));
            fireVariableUpdateNotifications(updates);
        }
    }
}
