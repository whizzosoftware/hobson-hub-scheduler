/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTask;

/**
 * An interface for classes that can schedule the execution of tasks.
 *
 * @author Dan Noguerol
 */
public interface ScheduledTaskExecutor {
    /**
     * Starts the task executor.
     */
    public void start();

    /**
     * Stops the task executor.
     */
    public void stop();

    /**
     * Schedules a new task for execution. Note that this also sets the executorId for the task.
     *
     * @param task the task to schedule
     * @param delayInSeconds how many seconds to wait before executing the task
     */
    public void schedule(ICalTask task, long delayInSeconds);

    /**
     * Indicates whether a task is scheduled for execution.
     *
     * @param task the task
     *
     * @return a boolean
     */
    public boolean isTaskScheduled(ICalTask task);

    /**
     * Cancels a task scheduled for execution.
     *
     * @param task the task to cancel
     *
     * @throws TaskNotFoundException on failure
     */
    public void cancel(ICalTask task) throws TaskNotFoundException;

    /**
     * Cancels all tasks scheduled for execution.
     */
    public void cancelAll();
}
