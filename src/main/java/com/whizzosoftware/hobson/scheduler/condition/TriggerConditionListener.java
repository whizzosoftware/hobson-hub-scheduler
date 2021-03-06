/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.condition;

import com.whizzosoftware.hobson.scheduler.ical.ICalTask;

/**
 * Interface for listeners to be alerted when a trigger condition occurs.
 *
 * @author Dan Noguerol
 */
public interface TriggerConditionListener {
    void onTriggerCondition(ICalTask task, long now);
}
