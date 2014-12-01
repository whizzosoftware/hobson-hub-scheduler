/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.action.ActionManager;
import com.whizzosoftware.hobson.api.action.HobsonAction;

import java.util.Map;
import java.util.Collection;

public class MockActionManager implements ActionManager {
    private int logCalls;

    public MockActionManager() {
        reset();
    }

    public int getLogCalls() {
        return logCalls;
    }

    public void reset() {
        logCalls = 0;
    }

    @Override
    public void publishAction(HobsonAction action) {

    }

    @Override
    public void executeAction(String pluginId, String actionId, Map<String, Object> properties) {
        if ("log".equals(actionId)) {
            logCalls++;
        }
    }

    @Override
    public Collection<HobsonAction> getAllActions(String userId, String hubId) {
        return null;
    }

    @Override
    public HobsonAction getAction(String userId, String hubId, String pluginId, String actionId) {
        return null;
    }
}
