package org.robotframework.ide.core.testData.model.table;

import java.util.Stack;

import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.TxtRobotFileParser.ParsingState;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken.RobotTokenType;


public class GarbageBeforeFirstTableMapper implements IParsingMapper {

    @Override
    public RobotToken map(RobotLine currentLine,
            Stack<ParsingState> processingState,
            RobotFileOutput robotFileOutput, RobotToken rt, FilePosition fp,
            String text) {
        // nothing to do
        rt.setText(new StringBuilder(text));
        rt.setType(RobotTokenType.UNKNOWN);
        return rt;
    }


    @Override
    public boolean checkIfCanBeMapped(final RobotFileOutput robotFileOutput,
            RobotToken rt, Stack<ParsingState> processingState) {
        boolean result = false;
        if (rt.getType() != RobotTokenType.START_HASH_COMMENT) {
            if (processingState.isEmpty()) {
                result = true;
            } else {
                ParsingState state = processingState.peek();
                result = (state == ParsingState.UNKNOWN || state == ParsingState.TRASH);
            }
        }

        return result;
    }
}
