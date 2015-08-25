/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.queue;

import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.api.task.TaskManager;
import com.whizzosoftware.hobson.scheduler.TaskNotFoundException;
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
public class LocalTaskQueue implements TaskQueue {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TaskManager taskManager;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
    private Map<TaskContext,ScheduledFuture> futureMap = Collections.synchronizedMap(new HashMap<TaskContext,ScheduledFuture>());

    public LocalTaskQueue(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

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
    public void schedule(final TaskContext taskContext, long delayInMs) {
        logger.debug("Scheduling task {} to run in {} seconds", taskContext.getTaskId(), delayInMs / 1000);
        ScheduledFuture future = executor.schedule(new Runnable() {
            @Override
            public void run() {
                taskManager.fireTaskTrigger(taskContext);
            }
        }, delayInMs, TimeUnit.MILLISECONDS);
        futureMap.put(taskContext, future);
    }

    @Override
    public boolean isTaskScheduled(TaskContext context) {
        return futureMap.containsKey(context);
    }

    @Override
    public void cancel(TaskContext context) throws TaskNotFoundException {
        ScheduledFuture future = futureMap.get(context);
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
