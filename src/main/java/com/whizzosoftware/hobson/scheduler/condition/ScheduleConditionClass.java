/*******************************************************************************
 * Copyright (c) 2015 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler.condition;

import com.whizzosoftware.hobson.api.plugin.PluginContext;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.PropertyContainerClassContext;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.task.condition.ConditionClassType;
import com.whizzosoftware.hobson.api.task.condition.ConditionEvaluationContext;
import com.whizzosoftware.hobson.api.task.condition.TaskConditionClass;

import java.util.ArrayList;
import java.util.List;

/**
 * A condition class for creating tasks that are executed on a schedule.
 *
 * @author Dan Noguerol
 */
public class ScheduleConditionClass extends TaskConditionClass {
    public static final String SCHEDULE_CONDITION_CLASS_ID = "schedule";

    public ScheduleConditionClass(PluginContext context) {
        super(PropertyContainerClassContext.create(context, SCHEDULE_CONDITION_CLASS_ID), "A scheduled time occurs", "The time is {time} on {date} repeating {recurrence}");
    }

    @Override
    public ConditionClassType getConditionClassType() {
        return ConditionClassType.trigger;
    }

    @Override
    public boolean evaluate(ConditionEvaluationContext context, PropertyContainer values) {
        return true;
    }

    @Override
    protected List<TypedProperty> createProperties() {
        List<TypedProperty> props = new ArrayList<>();
        props.add(new TypedProperty("date", "Start date", "The date the task will first occur", TypedProperty.Type.DATE));
        props.add(new TypedProperty("time", "Start time", "The time of day the task will occur", TypedProperty.Type.TIME));
        props.add(new TypedProperty("recurrence", "Repeat", "How often the task should repeat", TypedProperty.Type.RECURRENCE));
        return props;
    }
}
