package xyz.atom7.interpreter.ijvm;

import lombok.Getter;
import org.antlr.v4.runtime.tree.TerminalNode;
import xyz.atom7.api.interpreter.Instruction;

@Getter
public class IJVMInstruction extends Instruction
{
    /**
     * Constructor for IJVMInstruction with no arguments
     * 
     * @param opCode The operation code for the instruction
     */
    public IJVMInstruction(String opCode)
    {
        super(opCode, null, null);
    }

    /**
     * Constructor for IJVMInstruction with one argument
     * 
     * @param opCode The operation code for the instruction
     * @param argument The argument for the instruction
     */
    public IJVMInstruction(String opCode, String argument)
    {
        super(opCode, argument, null);
    }

    /**
     * Constructor for IJVMInstruction with one argument
     * 
     * @param opCode The operation code for the instruction
     * @param argument The argument for the instruction
     */
    public IJVMInstruction(String opCode, TerminalNode argument)
    {
        super(opCode, argument == null ? null : argument.getText(), null);
    }

    /**
     * Constructor for IJVMInstruction with two arguments
     * 
     * @param opCode The operation code for the instruction
     * @param argument The argument for the instruction
     * @param secondArgument The second argument for the instruction
     */
    public IJVMInstruction(String opCode, TerminalNode argument, TerminalNode secondArgument)
    {
        super(opCode,
                argument == null ? null : argument.getText(),
                secondArgument == null ? null : secondArgument.getText()
        );
    }
}
