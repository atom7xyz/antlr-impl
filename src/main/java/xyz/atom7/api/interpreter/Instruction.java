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
    private final String opCode;
}
