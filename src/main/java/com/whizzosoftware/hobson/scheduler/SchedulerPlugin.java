/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.PluginType;
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
    public void onStartup(Dictionary config) {
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
    public void onShutdown() {
        triggerProvider.stop();
    }

    @Override
    public PluginType getType() {
        return PluginType.CORE;
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
