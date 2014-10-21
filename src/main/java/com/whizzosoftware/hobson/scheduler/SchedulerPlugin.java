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
        addConfigurationMetaData(new ConfigurationMetaData("latitude", "Latitude", "The latitude of your location", ConfigurationMetaData.Type.STRING));
        addConfigurationMetaData(new ConfigurationMetaData("longitude", "Longitude", "The longitude of your location", ConfigurationMetaData.Type.STRING));

        // create an ical trigger provider
        ICalTriggerProvider provider = new ICalTriggerProvider(getId());
        applyProviderConfig(provider, config);
        provider.setScheduleExecutor(new ThreadPoolScheduledTriggerExecutor());
        provider.setScheduleFile(getDataFile("schedule.ics"));

        this.triggerProvider = provider;
        publishTriggerProvider(provider);

        setStatus(new PluginStatus(PluginStatus.Status.RUNNING));
    }

    @Override
    public void stop() {
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
        applyProviderConfig(triggerProvider, dictionary);
    }

    @Override
    public String getName() {
        return "Hobson Scheduler";
    }

    private void applyProviderConfig(ICalTriggerProvider provider, Dictionary config) {
        if (config != null && config.get("latitude") != null && config.get("longitude") != null) {
            provider.setLatitude((String)config.get("latitude"));
            provider.setLongitude((String) config.get("longitude"));
        }
    }
}
