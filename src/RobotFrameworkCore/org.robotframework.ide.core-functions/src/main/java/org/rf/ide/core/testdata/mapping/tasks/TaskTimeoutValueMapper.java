/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.tasks;

import java.util.List;
import java.util.Stack;

import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.mapping.table.IParsingMapper;
import org.rf.ide.core.testdata.mapping.table.ParsingStateHelper;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.tasks.Task;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class TaskTimeoutValueMapper implements IParsingMapper {

    private final ParsingStateHelper utility = new ParsingStateHelper();

    @Override
    public final boolean isApplicableFor(final RobotVersion robotVersion) {
        return robotVersion.isNewerOrEqualTo(new RobotVersion(3, 1));
    }

    @Override
    public boolean checkIfCanBeMapped(final RobotFileOutput robotFileOutput, final RobotLine currentLine,
            final RobotToken rt, final String text, final Stack<ParsingState> processingState) {

        if (utility.getCurrentState(processingState) == ParsingState.TASK_SETTING_TASK_TIMEOUT) {
            final List<Task> tasks = robotFileOutput.getFileModel().getTasksTable().getTasks();
            final List<LocalSetting<Task>> timeouts = tasks.get(tasks.size() - 1).getTimeouts();
            return !hasTimeoutAlready(timeouts);
        }
        return false;
    }

    static boolean hasTimeoutAlready(final List<LocalSetting<Task>> timeouts) {
        return !timeouts.isEmpty()
                && timeouts.get(timeouts.size() - 1).getToken(RobotTokenType.TASK_SETTING_TIMEOUT_VALUE) != null;
    }

    @Override
    public RobotToken map(final RobotLine currentLine, final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final RobotToken rt, final FilePosition fp, final String text) {

        rt.setText(text);

        final List<Task> tasks = robotFileOutput.getFileModel().getTasksTable().getTasks();
        final Task task = tasks.get(tasks.size() - 1);
        final List<LocalSetting<Task>> timeouts = task.getTimeouts();
        if (!timeouts.isEmpty()) {
            timeouts.get(timeouts.size() - 1).addToken(rt);
        }

        processingState.push(ParsingState.TASK_SETTING_TASK_TIMEOUT_VALUE);
        return rt;
    }
}
