/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.bootstrap.api.config.ConfigurationMetaData;
import com.whizzosoftware.hobson.bootstrap.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.scheduler.executor.ThreadPoolScheduledTriggerExecutor;
import com.whizzosoftware.hobson.scheduler.ical.ICalTriggerProvider;

import java.util.Dictionary;

/**
 * Hobson plugin that provides scheduling capabilities.
 *
 * @author Dan Noguerol
 */
public class SchedulerPlugin extends AbstractHobsonPlugin {
    private ICalTriggerProvider triggerProvider;

    public SchedulerPlugin(String pluginId) {
        super(pluginId);
    }

    @Override
    public void init(Dictionary config) {
        // latitude and longitude are two configurable properties of this plugin
        // define latitude and longitude as two configurable properties of this plugin
        addConfigurationMetaData(new ConfigurationMetaData("latitude", "Latitude", "The latitude of your location", ConfigurationMetaData.Type.STRING));
        addConfigurationMetaData(new ConfigurationMetaData("longitude", "Longitude", "The longitude of your location", ConfigurationMetaData.Type.STRING));

        // create an ical trigger provider
        triggerProvider = new ICalTriggerProvider(getId());
        applyProviderConfig(triggerProvider, config, false);
        triggerProvider.setScheduleExecutor(new ThreadPoolScheduledTriggerExecutor());
        triggerProvider.setScheduleFile(getDataFile("schedule.ics"));
        publishTriggerProvider(triggerProvider);
        triggerProvider.start();

        // set the plugin to running status
        setStatus(new PluginStatus(PluginStatus.Status.RUNNING));
    }

    @Override
    public void stop() {
        triggerProvider.stop();
    }

    @Override
    public long getRefreshInterval() {
        return 0;
    }

    @Override
    public void onRefresh() {
    }

    @Override
    public void onPluginConfigurationUpdate(Dictionary dictionary) {
        applyProviderConfig(triggerProvider, dictionary, true);
    }

    @Override
    public String getName() {
        return "Hobson Scheduler";
    }

    protected void applyProviderConfig(ICalTriggerProvider provider, Dictionary config, boolean reload) {
        if (config != null) {
            String newLatitudeS = (String)config.get("latitude");
            String newLongitudeS = (String)config.get("longitude");
            if (newLatitudeS != null && newLongitudeS != null) {
                Double newLatitude = Double.parseDouble(newLatitudeS);
                Double newLongitude = Double.parseDouble(newLongitudeS);
                if (!newLatitude.equals(provider.getLatitude()) && !newLongitude.equals(provider.getLongitude())) {
                    provider.setLatitude(newLatitude);
                    provider.setLongitude(newLongitude);
                    if (reload) {
                        triggerProvider.reloadScheduleFile();
                    }
                }
            }
        }
    }
}
