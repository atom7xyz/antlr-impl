package xyz.atom7.api.interpreter;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an instruction in the program.
 */
@Getter
public class Instruction
{
    /**
     * The operation code of the instruction.
     */
    protected final String opCode, argument, secondArgument;

    /**
     * The line of the instruction in the source file, starting from 1, or -1 if unknown.
     */
    @Setter
    protected int line = -1;

    /**
     * The byte address of the instruction in the code area, or -1 if unknown.
     */
    @Setter
    protected int address = -1;

    public Instruction(String opCode, String argument, String secondArgument)
    {
        this.opCode = opCode;
        this.argument = argument;
        this.secondArgument = secondArgument;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(opCode);

        if (argument != null) {
            sb.append(" ").append(argument);
        }

        if (secondArgument != null) {
            sb.append(", ").append(secondArgument);
        }

        return sb.toString();
    }
}
