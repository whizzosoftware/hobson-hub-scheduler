/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTask;

import java.util.HashMap;
import java.util.Map;

public class MockScheduledTaskExecutor implements ScheduledTaskExecutor {
    private Map<ICalTask,Long> delayMap = new HashMap<>();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void schedule(ICalTask task, long delayInMs) {
        delayMap.put(task, delayInMs);
    }

    @Override
    public boolean isTaskScheduled(ICalTask task) {
        return delayMap.containsKey(task);
    }

    @Override
    public void cancel(ICalTask task) throws TaskNotFoundException {
        delayMap.remove(task);
    }

    @Override
    public void cancelAll() {
        delayMap.clear();
    }

    public Long getDelayForTask(ICalTask task) {
        return delayMap.get(task);
    }

    public boolean hasDelays() {
        return (delayMap.size() > 0);
    }

    public void clearDelays() {
        delayMap.clear();
    }
}
