package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.action.HobsonAction;
import com.whizzosoftware.hobson.api.action.manager.ActionManager;

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

    public Collection<HobsonAction> getAllActions() {
        return null;
    }

    public HobsonAction getAction(String pluginId, String deviceId) {
        return null;
    }
}
