/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.executor;

import com.whizzosoftware.hobson.scheduler.ical.ICalTrigger;

/**
 * An interface for classes that can schedule the execution of tasks.
 *
 * @author Dan Noguerol
 */
public interface ScheduledTriggerExecutor {
    /**
     * Starts the task executor.
     */
    public void start();

    /**
     * Stops the task executor.
     */
    public void stop();

    /**
     * Schedules a new trigger for execution. Note that this also sets the executorId for the trigger.
     *
     * @param trigger the trigger to schedule
     * @param delayInSeconds how many seconds to wait before executing the task
     */
    public void schedule(ICalTrigger trigger, long delayInSeconds);

    /**
     * Indicates whether a trigger is scheduled for execution.
     *
     * @param trigger the trigger
     *
     * @return a boolean
     */
    public boolean isTriggerScheduled(ICalTrigger trigger);

    /**
     * Cancels a task scheduled for execution.
     *
     * @param trigger the trigger to cancel
     *
     * @throws TriggerNotFoundException on failure
     */
    public void cancel(ICalTrigger trigger) throws TriggerNotFoundException;

    /**
     * Cancels all tasks scheduled for execution.
     */
    public void cancelAll();
}
