/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import java.text.ParseException;

/**
 * Represents an offset from either sunrise or sunset.
 *
 * @author Dan Noguerol
 */
public class SolarOffset {
    private int offset;
    private Type type;

    public SolarOffset(String s) throws ParseException {
        s = s.trim();
        if (s.startsWith("SS")) {
            type = Type.SUNSET;
        } else if (s.startsWith("SR")) {
            type = Type.SUNRISE;
        } else {
            throw new ParseException("Date string requires SS or SR", 0);
        }

        if (s.length() > 2) {
            try {
                offset = Integer.parseInt(s.substring(2, s.length()));
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid offset value", 0);
            }
        }
    }

    public int getOffset() {
        return offset;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        SUNRISE,
        SUNSET
    }
}
