/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A TaskExecutor implementation that uses a ScheduledThreadPoolExecutor.
 *
 * @author Dan Noguerol
 */
public class ThreadPoolScheduledTriggerExecutor implements ScheduledTriggerExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
    private Map<ICalTrigger,ScheduledFuture> futureMap = Collections.synchronizedMap(new HashMap<ICalTrigger,ScheduledFuture>());

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        executor = null;
        futureMap.clear();
    }

    @Override
    public void schedule(ICalTrigger trigger, long delayInMs) {
        logger.debug("Scheduling task {} to run in {} seconds", trigger.getId(), delayInMs / 1000);
        ScheduledFuture future = executor.schedule(trigger, delayInMs, TimeUnit.MILLISECONDS);
        futureMap.put(trigger, future);
    }

    @Override
    public boolean isTriggerScheduled(ICalTrigger trigger) {
        return futureMap.containsKey(trigger);
    }

    @Override
    public void cancel(ICalTrigger trigger) throws TriggerNotFoundException {
        ScheduledFuture future = futureMap.get(trigger);
        if (future != null) {
            future.cancel(true);
        } else {
            throw new TriggerNotFoundException();
        }
    }

    @Override
    public void cancelAll() {
        for (ScheduledFuture future : futureMap.values()) {
            future.cancel(true);
        }
    }
}
