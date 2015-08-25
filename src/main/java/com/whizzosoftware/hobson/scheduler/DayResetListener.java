/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

/**
 * An interface for classes that want to be notified when the current day changes.
 *
 * @author Dan Noguerol
 */
public interface DayResetListener {
    void onDayReset(long now);
}
