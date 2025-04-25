package xyz.atom7.interpreter;

import lombok.Getter;
import org.antlr.v4.runtime.tree.TerminalNode;
import xyz.atom7.api.interpreter.Instruction;

@Getter
public class IJVMInstruction extends Instruction
{
    private final String argument, secondArgument;
 
    /**
     * Constructor for IJVMInstruction with no arguments
     * 
     * @param opCode The operation code for the instruction
     */
    public IJVMInstruction(String opCode)
    {
        super(opCode);
        this.argument = null;
        this.secondArgument = null;
    }

    /**
     * Constructor for IJVMInstruction with one argument
     * 
     * @param opCode The operation code for the instruction
     * @param argument The argument for the instruction
     */
    public IJVMInstruction(String opCode, String argument)
    {
        super(opCode);
        this.argument = argument;
        this.secondArgument = null;
    }

    /**
     * Constructor for IJVMInstruction with one argument
     * 
     * @param opCode The operation code for the instruction
     * @param argument The argument for the instruction
     */
    public IJVMInstruction(String opCode, TerminalNode argument)
    {
        super(opCode);
        this.argument = argument == null ? null : argument.getText();
        this.secondArgument = null;
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
        super(opCode);
        this.argument = argument == null ? null : argument.getText();
        this.secondArgument = secondArgument == null ? null : secondArgument.getText();
    }

    @Override
    public String toString()
    {
        String arg = argument == null ? "" : ", argument='" + argument + "'";

        if (secondArgument != null) {
            arg += ", secondArgument=" + secondArgument + "'";
        }

        return "IJVMInstruction{" +
                "opCode='" + getOpCode() + '\'' +
                arg +
                '}';
    }
}
