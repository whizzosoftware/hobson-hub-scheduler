/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTask;
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
public class ThreadPoolScheduledTaskExecutor implements ScheduledTaskExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
    private Map<ICalTask,ScheduledFuture> futureMap = Collections.synchronizedMap(new HashMap<ICalTask,ScheduledFuture>());

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
    public void schedule(ICalTask task, long delayInMs) {
        logger.debug("Scheduling task {} to run in {} seconds", task.getId(), delayInMs / 1000);
        ScheduledFuture future = executor.schedule(task, delayInMs, TimeUnit.MILLISECONDS);
        futureMap.put(task, future);
    }

    @Override
    public boolean isTaskScheduled(ICalTask task) {
        return futureMap.containsKey(task);
    }

    @Override
    public void cancel(ICalTask task) throws TaskNotFoundException {
        ScheduledFuture future = futureMap.get(task);
        if (future != null) {
            future.cancel(true);
        } else {
            throw new TaskNotFoundException();
        }
    }

    @Override
    public void cancelAll() {
        for (ScheduledFuture future : futureMap.values()) {
            future.cancel(true);
        }
    }
}
