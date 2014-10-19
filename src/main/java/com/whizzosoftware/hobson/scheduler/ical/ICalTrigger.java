/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.ical;

import com.whizzosoftware.hobson.api.action.HobsonActionRef;
import com.whizzosoftware.hobson.api.action.manager.ActionManager;
import com.whizzosoftware.hobson.api.trigger.HobsonTrigger;
import com.whizzosoftware.hobson.bootstrap.api.HobsonRuntimeException;
import com.whizzosoftware.hobson.scheduler.TriggerExecutionListener;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.TimeZone;

/**
 * An implementation of HobsonTrigger for iCal scheduled events.
 *
 * @author Dan Noguerol
 */
public class ICalTrigger implements HobsonTrigger, Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String providerId;
    private ActionManager actionManager;
    private VEvent event;
    private final List<HobsonActionRef> actions = new ArrayList<>();
    private TriggerExecutionListener listener;
    private final Properties properties = new Properties();
    private Double latitude = 39.3722;
    private Double longitude = -104.8561;
    private TimeZone timezone = TimeZone.getTimeZone("America/Denver");

    public ICalTrigger(ActionManager actionManager, String providerId, VEvent event, TriggerExecutionListener listener) throws InvalidVEventException {
        this.actionManager = actionManager;
        this.providerId = providerId;
        this.event = event;
        this.listener = listener;

        // adjust start time if sun offset is set
        Property sunOffset = event.getProperty("X-SUN-OFFSET");
        if (sunOffset != null) {
            try {
                event.getStartDate().setValue(calculateSunOffset(event.getStartDate(), sunOffset.getValue(), latitude, longitude, timezone));
            } catch (ParseException e) {
                throw new InvalidVEventException("Invalid X-SUN-OFFSET", e);
            }
        }

        // parse actions
        if (event != null) {
            Property commentProp = event.getProperty("COMMENT");
            if (commentProp != null) {
                JSONArray arr = new JSONArray(new JSONTokener(commentProp.getValue()));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject json = arr.getJSONObject(i);
                    if (json.has("pluginId") && json.has("actionId")) {
                        addActionRef(json);
                    } else {
                        throw new InvalidVEventException("Found scheduled event with no plugin and/or action");
                    }
                }
            } else {
                throw new InvalidVEventException("ICalEventTrigger event must have a COMMENT property");
            }
        } else {
            throw new InvalidVEventException("ICalEventTrigger must have a non-null event");
        }
    }

    public ICalTrigger(ActionManager actionManager, String providerId, JSONObject json) {
        this.actionManager = actionManager;
        this.providerId = providerId;

        this.event = new VEvent();
        String id;
        if (json.has("id")) {
            id = json.getString("id");
        } else {
            id = UUID.randomUUID().toString();
        }
        event.getProperties().add(new Uid(id));
        event.getProperties().add(new Summary(json.getString("name")));

        try {
            if (json.has("conditions")) {
                JSONArray conditions = json.getJSONArray("conditions");
                if (conditions.length() == 1) {
                    JSONObject jc = conditions.getJSONObject(0);
                    if (jc.has("start")) {
                        event.getProperties().add(new DtStart(jc.getString("start")));
                    }
                    if (jc.has("sunOffset")) {
                        event.getStartDate().setDate(new Date(calculateSunOffset(event.getStartDate(), jc.getString("sunOffset"), latitude, longitude, timezone)));
                    }
                    if (jc.has("recurrence")) {
                        event.getProperties().add(new RRule(jc.getString("recurrence")));
                    }
                } else {
                    throw new HobsonRuntimeException("ICalTriggers only support one condition");
                }
            }
        } catch (ParseException e) {
            throw new HobsonRuntimeException("Error parsing recurrence rule", e);
        }

        try {
            if (json.has("actions")) {
                JSONArray actions = json.getJSONArray("actions");
                event.getProperties().add(new Comment(actions.toString()));
                for (int i=0; i < actions.length(); i++) {
                    addActionRef(actions.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            throw new HobsonRuntimeException("Error parsing actions", e);
        }
    }

    @Override
    public String getId() {
        if (event != null && event.getUid() != null) {
            return event.getUid().getValue();
        } else {
            return null;
        }
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getName() {
        if (event.getSummary() != null) {
            return event.getSummary().getValue();
        } else {
            return null;
        }
    }

    @Override
    public Type getType() {
        return Type.SCHEDULE;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean hasConditions() {
        return (event != null);
    }

    @Override
    public Collection<Map<String, Object>> getConditions() {
        List<Map<String,Object>> conditions = new ArrayList<>();
        Map<String,Object> map = new HashMap<>();
        map.put("start", event.getStartDate().getValue());
        RRule rrule = (RRule)event.getProperty("RRULE");
        map.put("recurrence", rrule.getRecur().toString());
        conditions.add(map);
        return conditions;
    }

    @Override
    public boolean hasActions() {
        return (actions.size() > 0);
    }

    @Override
    public Collection<HobsonActionRef> getActions() {
        return actions;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void run() {
        try {
            logger.info("Trigger \"{}\" is executing", getName());
            executeActions();
            if (listener != null) {
                listener.onTriggerExecuted(this, System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.error("Error executing trigger actions", e);
        }
    }

    public VEvent getVEvent() {
        return event;
    }

    public List<Long> getRunsDuringInterval(long startTime, long endTime) throws Exception {
        List<Long> results = new ArrayList<>();
        if (event != null) {
            PeriodList periods = event.calculateRecurrenceSet(new Period(new DateTime(startTime), new DateTime(endTime)));
            for (Object period : periods) {
                results.add(((Period) period).getStart().getTime());
            }
        }
        return results;
    }

    protected String calculateSunOffset(DtStart startDate, String sunOffset, Double latitude, Double longitude, TimeZone timezone) throws ParseException {
        AstronomicalDtStart ads = new AstronomicalDtStart();
        return ads.getDate(startDate.getValue(), sunOffset, latitude, longitude, timezone);
    }

    private void executeActions() {
        for (HobsonActionRef ref : actions) {
            actionManager.executeAction(ref.getPluginId(), ref.getActionId(), ref.getProperties());
        }
    }

    private void addActionRef(JSONObject json) {
        HobsonActionRef ref = new HobsonActionRef(json.getString("pluginId"), json.getString("actionId"), json.getString("name"));
        if (json.has("properties")) {
            JSONObject propJson = json.getJSONObject("properties");
            Iterator it = propJson.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                ref.addProperty(key, propJson.get(key));
            }
        }
        actions.add(ref);
    }
}
