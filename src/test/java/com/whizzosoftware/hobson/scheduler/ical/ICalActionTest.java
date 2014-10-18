/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ICalActionTest {
    @Test
    public void testJSONConstructorWithMethod() {
        JSONObject json = new JSONObject("{\"method\":\"foo\"}");
        ICalAction action = new ICalAction(null, json);
        assertEquals("foo", action.getMethod());
    }

    @Test
    public void testJSONConstructorWithMethodAndArguments() {
        JSONObject json = new JSONObject("{\"method\":\"foo\",\"arg1\":\"bar\"}");
        ICalAction action = new ICalAction(null, json);
        assertEquals("foo", action.getMethod());
        assertNotNull(action.getValues());
        assertEquals(1, action.getValues().size());
        assertEquals("bar", action.getValues().get(0));
    }
}
