/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTrigger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MockScheduledTriggerExecutor implements ScheduledTriggerExecutor {
    private Map<ICalTrigger,Long> delayMap = new HashMap<>();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void schedule(ICalTrigger trigger, long delayInMs) {
        delayMap.put(trigger, delayInMs);
    }

    @Override
    public boolean isTriggerScheduled(ICalTrigger trigger) {
        return delayMap.containsKey(trigger);
    }

    @Override
    public void cancel(ICalTrigger trigger) throws TriggerNotFoundException {
        delayMap.remove(trigger);
    }

    @Override
    public void cancelAll() {
        delayMap.clear();
    }

    public Long getDelayForTask(ICalTrigger trigger) {
        return delayMap.get(trigger);
    }

    public boolean hasDelays() {
        return (delayMap.size() > 0);
    }

    public void clearDelays() {
        delayMap.clear();
    }
}
