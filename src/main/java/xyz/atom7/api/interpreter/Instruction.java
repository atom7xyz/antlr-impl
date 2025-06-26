package xyz.atom7.api.interpreter;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an instruction in the program.
 */
@Getter
@AllArgsConstructor
public class Instruction
{
    /**
     * The operation code of the instruction.
     */
    protected final String opCode, argument, secondArgument;

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
