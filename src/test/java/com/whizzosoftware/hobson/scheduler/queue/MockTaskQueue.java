/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.queue;

import com.whizzosoftware.hobson.api.task.TaskContext;
import com.whizzosoftware.hobson.scheduler.TaskNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class MockTaskQueue implements TaskQueue {
    private Map<TaskContext,Long> delayMap = new HashMap<>();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void schedule(TaskContext taskContext, long delayInMs) {
        delayMap.put(taskContext, delayInMs);
    }

    @Override
    public boolean isTaskScheduled(TaskContext context) {
        return delayMap.containsKey(context);
    }

    @Override
    public void cancel(TaskContext context) throws TaskNotFoundException {
        delayMap.remove(context);
    }

    @Override
    public void cancelAll() {
        delayMap.clear();
    }

    public Long getDelayForTask(TaskContext context) {
        return delayMap.get(context);
    }

    public boolean hasDelays() {
        return (delayMap.size() > 0);
    }

    public void clearDelays() {
        delayMap.clear();
    }
}
