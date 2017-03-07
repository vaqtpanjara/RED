/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.services.IDisposable;
import org.rf.ide.core.execution.LogLevel;
import org.rf.ide.core.execution.RobotDefaultAgentEventListener;
import org.rf.ide.core.execution.Status;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotTestExecutionService.RobotTestsLaunch;

public class MessagesTrackerForLogView extends RobotDefaultAgentEventListener {

    private final RobotTestsLaunch testsLaunchContext;

    public MessagesTrackerForLogView(final RobotTestsLaunch testsLaunchContext) {
        this.testsLaunchContext = testsLaunchContext;
    }

    @Override
    public void handleAgentInitializing() {
        testsLaunchContext.getExecutionData(MessageStore.class, () -> new MessageStore());
    }

    @Override
    public void handleLogMessage(final String msg, final LogLevel level, final String timestamp) {
        testsLaunchContext.getExecutionData(MessageStore.class)
                .ifPresent(store -> store.append(timestamp + " : " + level.name() + " : " + msg + "\n"));
    }

    @Override
    public void handleTestStarted(final String testCaseName, final String testCaseLongName) {
        testsLaunchContext.getExecutionData(MessageStore.class)
                .ifPresent(store -> store.append("Starting test: " + testCaseLongName + '\n'));
    }

    @Override
    public void handleTestEnded(final String testCaseName, final String testCaseLongName, final int elapsedTime,
            final Status status, final String errorMessage) {
        testsLaunchContext.getExecutionData(MessageStore.class)
                .ifPresent(store -> store.append("Ending test: " + testCaseLongName + "\n\n"));
    }

    public static class MessageStore implements IDisposable {

        private final StringBuilder message = new StringBuilder();

        private final List<MessageStoreListener> listeners = new ArrayList<>();

        public synchronized void addStoreListener(final MessageStoreListener listener) {
            listeners.add(listener);
        }

        synchronized void append(final String msg) {
            message.append(msg);
            listeners.stream().forEach(listener -> listener.storeAppended(this, msg));
        }

        synchronized public String getMessage() {
            return message.toString();
        }

        @Override
        public synchronized void dispose() {
            listeners.clear();
        }
    }

    @FunctionalInterface
    public static interface MessageStoreListener {

        void storeAppended(MessageStore store, String appendedMsg);
    }
}
