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

/**
 * An interface for classes that can queue the execution of tasks.
 *
 * @author Dan Noguerol
 */
public interface TaskQueue {
    /**
     * Starts the task queue.
     */
    void start();

    /**
     * Stops the task queue.
     */
    void stop();

    /**
     * Queue a new task for execution. Note that this also sets the executorId for the task.
     *
     * @param taskContext the context of the task to be executed
     * @param delayInMs how many milliseconds to wait before executing the task
     */
    void schedule(TaskContext taskContext, long delayInMs);

    /**
     * Indicates whether a task is queued for execution.
     *
     * @param context the context of the task
     *
     * @return a boolean
     */
    boolean isTaskScheduled(TaskContext context);

    /**
     * Cancels a task queued for execution.
     *
     * @param context the context of the task
     *
     * @throws TaskNotFoundException on failure
     */
    void cancel(TaskContext context) throws TaskNotFoundException;

    /**
     * Cancels all tasks scheduled for execution.
     */
    void cancelAll();
}
