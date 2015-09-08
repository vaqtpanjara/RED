package org.robotframework.ide.core.testData.model.table.variables.mapping;

import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.IRobotFileOutput;
import org.robotframework.ide.core.testData.model.table.VariableTable;
import org.robotframework.ide.core.testData.model.table.mapping.ElementsUtility;
import org.robotframework.ide.core.testData.model.table.mapping.IParsingMapper;
import org.robotframework.ide.core.testData.model.table.variables.UnknownVariable;
import org.robotframework.ide.core.testData.text.read.IRobotTokenType;
import org.robotframework.ide.core.testData.text.read.ParsingState;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;


public class UnknownVariableMapper implements IParsingMapper {

    private final ElementsUtility utility;


    public UnknownVariableMapper() {
        this.utility = new ElementsUtility();
    }


    @Override
    public RobotToken map(RobotLine currentLine,
            Stack<ParsingState> processingState,
            IRobotFileOutput robotFileOutput, RobotToken rt, FilePosition fp,
            String text) {
        List<IRobotTokenType> types = rt.getTypes();
        types.remove(RobotTokenType.UNKNOWN);
        types.add(0, RobotTokenType.VARIABLES_UNKNOWN_DECLARATION);
        rt.setStartColumn(fp.getColumn());
        rt.setText(new StringBuilder(text));
        rt.setRaw(new StringBuilder(text));

        VariableTable varTable = robotFileOutput.getFileModel()
                .getVariableTable();
        UnknownVariable varUnknown = robotFileOutput.getObjectCreator()
                .createUnknownVariable(rt.getRaw().toString(), rt);
        varTable.addVariable(varUnknown);

        processingState.push(ParsingState.VARIABLE_UNKNOWN);

        return rt;
    }


    @Override
    public boolean checkIfCanBeMapped(IRobotFileOutput robotFileOutput,
            RobotLine currentLine, RobotToken rt, String text,
            Stack<ParsingState> processingState) {
        ParsingState currentState = utility.getCurrentStatus(processingState);

        return (currentState == ParsingState.VARIABLE_TABLE_INSIDE);
    }
}
