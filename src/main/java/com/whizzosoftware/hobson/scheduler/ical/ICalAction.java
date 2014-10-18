/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.action.manager.ActionManager;
import org.apache.commons.beanutils.MethodUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A Runnable implementation for ICalEventTriggers that uses reflection to execute actions via an ActionManager.
 *
 * @author Dan Noguerol
 */
public class ICalAction implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ActionManager actionManager;
    private String method;
    private final List<Object> values = new ArrayList<Object>();

    public ICalAction(ActionManager context, String method) {
        this.actionManager = context;
        this.method = method;
    }

    public ICalAction(ActionManager context, JSONObject json) throws JSONException {
        this.actionManager = context;
        this.method = json.getString("method");

        int k = 1;
        while (json.has("arg" + k)) {
            addValue(json.get("arg" + k));
            k++;
        }
    }

    public String getMethod() {
        return method;
    }

    public void addValue(Object val) {
        values.add(val);
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public void run() {
        try {
            MethodUtils.invokeMethod(actionManager, method, values.toArray());
        } catch (Throwable t) {
            logger.error("Unable to invoke method: " + method, t);
        }
    }
}
