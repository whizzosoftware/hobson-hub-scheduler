/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.scheduler;

import com.whizzosoftware.hobson.api.action.ActionManager;
import com.whizzosoftware.hobson.api.task.TaskProvider;
import com.whizzosoftware.hobson.scheduler.executor.ThreadPoolScheduledTaskExecutor;
import com.whizzosoftware.hobson.scheduler.ical.ICalTaskProvider;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class Activator extends DependencyActivatorBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ServiceTracker serviceTracker;
    private Component service;

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        serviceTracker = new ServiceTracker(context, TaskProvider.class.getName(), new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference serviceReference) {
                logger.info("Adding service: {}", serviceReference.getBundle().getSymbolicName());
                try {
                    BundleContext context = getBundleContext();
                    TaskProvider provider = (TaskProvider)context.getService(serviceReference);
                    if (provider instanceof ICalTaskProvider) {
                        ICalTaskProvider scheduler = (ICalTaskProvider)provider;

                        // set schedule file
                        scheduler.setScheduleExecutor(new ThreadPoolScheduledTaskExecutor());
                        scheduler.setScheduleFile(context.getDataFile("schedule.ics"));
                    }
                    return provider;
                } catch (Exception e) {
                    logger.error("Error initializing scheduler", e);
                    return null;
                }
            }

            @Override
            public void addedService(ServiceReference serviceReference, Object o) {

            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
                logger.info("Modified service: {}", serviceReference.getBundle().getSymbolicName());
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                logger.info("Removed service: {}", serviceReference.getBundle().getSymbolicName());
            }
        });
        serviceTracker.open();

        // add scheduler service
        Hashtable props = new Hashtable();
        props.put("providerId", ICalTaskProvider.PROVIDER);
        service = createComponent().
            setInterface(TaskProvider.class.getName(), props).
            setImplementation(ICalTaskProvider.class).
            add(createServiceDependency().setService(EventAdmin.class).setRequired(true)).
            add(createServiceDependency().setService(ActionManager.class).setRequired(true));
        manager.add(service);
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        serviceTracker.close();
        manager.remove(service);
    }
}
