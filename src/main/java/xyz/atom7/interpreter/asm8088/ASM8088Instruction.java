package xyz.atom7.interpreter.asm8088;

import lombok.Getter;
import xyz.atom7.api.interpreter.Instruction;

@Getter
public class ASM8088Instruction extends Instruction
{
    /**
     * Constructor for ASM8088Instruction with no operands
     * 
     * @param opCode The mnemonic for the instruction
     */
    public ASM8088Instruction(String opCode)
    {
        super(opCode, null, null);
    }

    /**
     * Constructor for ASM8088Instruction with one operand
     * 
     * @param opCode The mnemonic for the instruction
     * @param argument The first operand for the instruction
     */
    public ASM8088Instruction(String opCode, String argument)
    {
        super(opCode, argument, null);
    }

    /**
     * Constructor for ASM8088Instruction with two operands
     * 
     * @param opCode The mnemonic for the instruction
     * @param argument The first operand for the instruction
     * @param secondArgument The second operand for the instruction
     */
    public ASM8088Instruction(String opCode, String argument, String secondArgument)
    {
        super(opCode, argument, secondArgument);
    }
}