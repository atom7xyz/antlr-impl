package xyz.atom7.interpreter.asm8088;

import lombok.Getter;
import lombok.SneakyThrows;
import xyz.atom7.Utils;
import xyz.atom7.api.interpreter.Interpreter;
import xyz.atom7.parser.asm8088.ASM8088ParserHelper;
import xyz.atom7.parser.asm8088Parser;

import java.util.*;
import java.util.function.Consumer;

import static xyz.atom7.Utils.debugln;

/**
 * ASM8088Program represents an 8088 assembly program interpreter
 * This class handles parsing, section processing, and instruction execution
 */
@Getter
public class ASM8088Program<T extends ASM8088Instruction> extends Interpreter<T>
{
    private final ASM8088Scope scope;
    
    /**
     * Map to store label names and their instruction indices
     * Used for jump instructions and CALL instructions
     */
    protected final Map<String, Integer> labels = new HashMap<>();
    
    /**
     * Lists to store section content for processing
     */
    private final List<String> textSectionLines = new ArrayList<>();
    private final List<String> dataSectionLines = new ArrayList<>();
    private final List<String> bssSectionLines = new ArrayList<>();

    private final List<ASM8088Instruction> instructions;

    /**
     * Constructor for ASM8088Program
     * Initializes the scope and instruction storage
     */
    public ASM8088Program()
    {
        super();
        this.scope = new ASM8088Scope("main");
        this.instructions = scope.getInstructions();
    }

    /**
     * Initializes all instruction handlers for the 8088 processor
     * Sets up lambda functions for each supported instruction
     */
    @Override
    protected void initInstructions()
    {
        addInstruction("MOV", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int oldValue = getValue(dest);
            int value = getValue(src);
            
            setValue(dest, value);
            
            debugln(formatDebugTwoOp("MOV", dest, src, oldValue, value, value, "="));
        });

        addInstruction("PUSH", (instr) -> {
            String operand = instr.getArgument();
            int value = getValue(operand);
            
            scope.pushStack(value);
            
            debugln("PUSH " + operand + " (value: 0x" + String.format("%04X", value & 0xFFFF) + ") ");
        });

        addInstruction("POP", (instr) -> {
            String dest = instr.getArgument();
            
            int oldValue = getValue(dest);
            int value = scope.popStack();

            setValue(dest, value);
            
            String extraInfo = "(from stack, SP: 0x" + String.format("%04X", scope.getRegister16("SP")) + ")";
            debugln(formatDebug("POP", dest, oldValue, value, extraInfo));
        });

        addInstruction("ADD", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int result = destVal + srcVal;
            
            setValue(dest, result);
            scope.updateArithmeticFlags(destVal, srcVal, result, is16BitOperand(dest), false);
            
            debugln(formatDebugTwoOp("ADD", dest, src, destVal, result, srcVal, "+"));
        });

        addInstruction("SUB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int result = destVal - srcVal;
            
            setValue(dest, result);
            scope.updateArithmeticFlags(destVal, srcVal, result, is16BitOperand(dest), true);
            
            debugln(formatDebugTwoOp("SUB", dest, src, destVal, result, srcVal, "-"));
        });

        addInstruction("MUL", (instr) -> {
            String operand = instr.getArgument();
            int multiplicand = getValue(operand);
            int multiplier = scope.getRegister8("AL");
            int oldAX = scope.getRegister16("AX");
            int result = multiplicand * multiplier;
            
            scope.setRegister16("AX", result);
            scope.updateFlags(result, false);
            
            String extraInfo = "(AL:0x" + String.format("%02X", multiplier) + " * " + 
                              operand + ":0x" + String.format("%02X", multiplicand) + 
                              " = 0x" + String.format("%04X", result) + ")";
            debugln(formatDebug("MUL", "AX", oldAX, result, extraInfo));
        });

        addInstruction("DIV", (instr) -> {
            String operand = instr.getArgument();
            int divisor = getValue(operand);
            int dividend = scope.getRegister16("AX");
            
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            
            int quotient = dividend / divisor;
            int remainder = dividend % divisor;
            
            scope.setRegister8("AL", quotient);
            scope.setRegister8("AH", remainder);
            
            String extraInfo = "(AX:0x" + String.format("%04X", dividend) + " / " + 
                              operand + ":0x" + String.format("%02X", divisor) + 
                              " = AL:0x" + String.format("%02X", quotient) + 
                              " R AH:0x" + String.format("%02X", remainder) + ")";
            debugln("DIV " + operand + " " + extraInfo);
        });

        addInstruction("INC", (instr) -> {
            String operand = instr.getArgument();
            int value = getValue(operand);
            int result = value + 1;
            
            setValue(operand, result);
            scope.updateFlags(result, is16BitOperand(operand));
            
            debugln(formatDebug("INC", operand, value, result, null));
        });

        addInstruction("DEC", (instr) -> {
            String operand = instr.getArgument();
            int value = getValue(operand);
            int result = value - 1;
            
            setValue(operand, result);
            scope.updateFlags(result, is16BitOperand(operand));
            
            debugln(formatDebug("DEC", operand, value, result, null));
        });

        addInstruction("CMP", (instr) -> {
            String op1 = instr.getArgument();
            String op2 = instr.getSecondArgument();
            
            int val1 = getValue(op1);
            int val2 = getValue(op2);
            int result = val1 - val2;
            
            scope.updateArithmeticFlags(val1, val2, result, is16BitOperand(op1), true);
            
            String flagInfo = "(Z:" + (scope.isZeroFlag() ? "1" : "0") + 
                             " S:" + (scope.isSignFlag() ? "1" : "0") + 
                             " C:" + (scope.isCarryFlag() ? "1" : "0") + 
                             " O:" + (scope.isOverflowFlag() ? "1" : "0") + 
                             " P:" + (scope.isParityFlag() ? "1" : "0") + ")";
            debugln(formatDebugTwoOp("CMP", op1, op2, val1, result, val2, "-") + " " + flagInfo);
        });

        addInstruction("AND", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int result = destVal & srcVal;
            
            setValue(dest, result);
            scope.updateFlags(result, is16BitOperand(dest));
            scope.setCarryFlag(false); // AND clears carry flag
            
            debugln(formatDebugTwoOp("AND", dest, src, destVal, result, srcVal, "&"));
        });

        addInstruction("OR", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int result = destVal | srcVal;
            
            setValue(dest, result);
            scope.updateFlags(result, is16BitOperand(dest));
            scope.setCarryFlag(false); // OR clears carry flag
            
            debugln(formatDebugTwoOp("OR", dest, src, destVal, result, srcVal, "|"));
        });

        addInstruction("XOR", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int result = destVal ^ srcVal;
            
            setValue(dest, result);
            scope.updateFlags(result, is16BitOperand(dest));
            scope.setCarryFlag(false); // XOR clears carry flag
            
            debugln(formatDebugTwoOp("XOR", dest, src, destVal, result, srcVal, "^"));
        });

        addInstruction("NOT", (instr) -> {
            String operand = instr.getArgument();
            int value = getValue(operand);
            int result = ~value;
            
            setValue(operand, result);
            
            debugln(formatDebug("NOT", operand, value, result, "(bitwise NOT)"));
        });

        addInstruction("JMP", (instr) -> {
            String label = instr.getArgument();
            Integer targetIndex = labels.get(label);
            if (targetIndex != null) {
                scope.setPc(targetIndex - 1);
                debugln("JMP " + label + " -> index: " + targetIndex);
            } else {
                throw new IllegalStateException("Label not found: " + label);
            }
        });

        addInstruction("JE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JE " + label + " (not taken)");
            }
        });

        addInstruction("JNE", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNE " + label + " (not taken)");
            }
        });

        addInstruction("JG", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isZeroFlag() && !scope.isSignFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JG " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JG " + label + " (not taken)");
            }
        });

        addInstruction("JL", (instr) -> {
            String label = instr.getArgument();
            if (scope.isSignFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JL " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JL " + label + " (not taken)");
            }
        });

        addInstruction("JLE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isZeroFlag() || scope.isSignFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JLE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JLE " + label + " (not taken)");
            }
        });

        addInstruction("CALL", (instr) -> {
            String label = instr.getArgument();
            Integer targetIndex = labels.get(label);
            if (targetIndex != null) {
                // Push return address (current PC + 1)
                int returnAddr = scope.getPc() + 1;
                int sp = scope.getRegister16("SP");
                sp = (sp - 2) & 0xFFFF;
                scope.setRegister16("SP", sp);
                
                int stackAddr = scope.getStackAddress(sp);
                scope.writeWord(stackAddr, returnAddr);
                
                // Jump to target
                scope.setPc(targetIndex - 1);
                debugln("CALL " + label + " (return addr: " + returnAddr + ", target: " + targetIndex + ")");
            } else {
                throw new IllegalStateException("Label not found: " + label);
            }
        });

        addInstruction("RET", (instr) -> {
            int returnAddr = scope.popStack();
            scope.setPc(returnAddr - 1);
            

            
            debugln("RET (return to: " + returnAddr + ")");
        });

        addInstruction("SYS", (instr) -> {
            int syscallNum = scope.popStack();
            
            switch (syscallNum) {
                case 1: // EXIT
                    int exitCode = scope.popStack();

                    debugln("SYS EXIT (code: " + exitCode + ")");
                    System.out.println("\nProgram exited with code: " + exitCode);

                    System.exit(exitCode);
                    halt();

                    break;
                    
                case 117: // GETCHAR
                    debugln("SYS GETCHAR");

                    char read;
                    if (scanner.hasNext()) {
                        String input = scanner.nextLine();
                        read = input.charAt(0);

                        scope.setRegister8("AL", read);
                        debugln("SYS GETCHAR (input: '" + read + "')");
                    }
                    break;
                case 127: // PRINTF
                    String format = getStringFromStack();
                    int value = scope.popStack();
                    debugln("SYS PRINTF (format: \"" + format + "\", value: " + value + ")");
                    System.out.printf(format, value);
                    break;

                default:
                    debugln("SYS (unknown syscall: " + syscallNum + ")");
                    break;
            }
        });

        addInstruction("HLT", (instr) -> {
            debugln("HLT");
            System.out.println("\nProgram halted.");
            halt();
        });

        addInstruction("NOP", (instr) -> {
            debugln("NOP");
        });

        addInstruction("MOVB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int oldValue = getValue(dest) & 0xFF;
            int value = getValue(src) & 0xFF;
                
            setValue(dest, value);
              
            debugln(formatDebugTwoOp("MOVB", dest, src, oldValue, value, value, "=") + " (8-bit)");
        });

        addInstruction("ADDB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest) & 0xFF;
            int srcVal = getValue(src) & 0xFF;
            int result = destVal + srcVal;
              
            setValue(dest, result);
            scope.updateFlags(result, false);
            
            debugln(formatDebugTwoOp("ADDB", dest, src, destVal, result, srcVal, "+") + " (8-bit)");
        });

        addInstruction("SUBB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest) & 0xFF;
            int srcVal = getValue(src) & 0xFF;
            int result = destVal - srcVal;
            
            setValue(dest, result);
            scope.updateFlags(result, false);
            
            debugln(formatDebugTwoOp("SUBB", dest, src, destVal, result, srcVal, "-") + " (8-bit)");
        });

        addInstruction("CMPB", (instr) -> {
            String op1 = instr.getArgument();
            String op2 = instr.getSecondArgument();
            
            int val1 = getValue(op1) & 0xFF;
            int val2 = getValue(op2) & 0xFF;
            int result = val1 - val2;
            
            scope.updateFlags(result, false);
            
            String flagInfo = "(Z:" + (scope.isZeroFlag() ? "1" : "0") + 
                             " S:" + (scope.isSignFlag() ? "1" : "0") + 
                             " C:" + (scope.isCarryFlag() ? "1" : "0") + ")";
            debugln(formatDebugTwoOp("CMPB", op1, op2, val1, result, val2, "-") + " (8-bit) " + flagInfo);
        });

        addInstruction("DIVB", (instr) -> {
            String operand = instr.getArgument();
            int divisor = getValue(operand) & 0xFF;
            int dividend = scope.getRegister8("AL");
            
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            
            int quotient = dividend / divisor;
            int remainder = dividend % divisor;
            
            scope.setRegister8("AL", quotient);
            scope.setRegister8("AH", remainder);
            
            String extraInfo = "(AL:0x" + String.format("%02X", dividend) + " / " + 
                              operand + ":0x" + String.format("%02X", divisor) + 
                              " = AL:0x" + String.format("%02X", quotient) + 
                              " R AH:0x" + String.format("%02X", remainder) + ") (8-bit)";
            debugln("DIVB " + operand + " " + extraInfo);
        });

        addInstruction("XORB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest) & 0xFF;
            int srcVal = getValue(src) & 0xFF;
            int result = destVal ^ srcVal;
            
            setValue(dest, result);
            scope.updateFlags(result, false);
            scope.setCarryFlag(false);
            
            debugln(formatDebugTwoOp("XORB", dest, src, destVal, result, srcVal, "^") + " (8-bit)");
        });

        addInstruction("MULB", (instr) -> {
            String operand = instr.getArgument();
            int multiplicand = getValue(operand) & 0xFF;
            int multiplier = scope.getRegister8("AL");
            int oldAL = scope.getRegister8("AL");
            int result = multiplicand * multiplier;
            
            scope.setRegister8("AL", result & 0xFF);
            scope.setRegister8("AH", (result >> 8) & 0xFF);
            scope.updateFlags(result, false);
            
            String extraInfo = "(AL:0x" + String.format("%02X", multiplier) + " * " + 
                              operand + ":0x" + String.format("%02X", multiplicand) + 
                              " = AL:0x" + String.format("%02X", result & 0xFF) + 
                              " AH:0x" + String.format("%02X", (result >> 8) & 0xFF) + ") (8-bit)";
            debugln(formatDebug("MULB", "AL", oldAL, result & 0xFF, extraInfo));
        });

        addInstruction("LOOP", (instr) -> {
            String label = instr.getArgument();
            int oldCX = scope.getRegister16("CX");
            int cx = (oldCX - 1) & 0xFFFF;
            scope.setRegister16("CX", cx);
            
            if (cx != 0) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    String extraInfo = "(CX: 0x" + String.format("%04X", oldCX) + " -> 0x" + String.format("%04X", cx) + 
                                     ", taken -> index: " + targetIndex + ")";
                    debugln("LOOP " + label + " " + extraInfo);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                String extraInfo = "(CX: 0x" + String.format("%04X", oldCX) + " -> 0x" + String.format("%04X", cx) + 
                                 ", not taken)";
                debugln("LOOP " + label + " " + extraInfo);
            }
        });

        addInstruction("LABEL", (instr) -> {
            debugln("LABEL " + instr.getArgument());
        });

        // ADC - Add with Carry
        addInstruction("ADC", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int carryVal = scope.isCarryFlag() ? 1 : 0;
            int result = destVal + srcVal + carryVal;
            
            setValue(dest, result);
            scope.updateArithmeticFlags(destVal, srcVal + carryVal, result, is16BitOperand(dest), false);
            
            debugln(formatDebugTwoOp("ADC", dest, src, destVal, result, srcVal, "+") + " (with carry)");
        });

        // SBB - Subtract with Borrow
        addInstruction("SBB", (instr) -> {
            String dest = instr.getArgument();
            String src = instr.getSecondArgument();
            
            int destVal = getValue(dest);
            int srcVal = getValue(src);
            int borrowVal = scope.isCarryFlag() ? 1 : 0;
            int result = destVal - srcVal - borrowVal;
            
            setValue(dest, result);
            scope.updateArithmeticFlags(destVal, srcVal + borrowVal, result, is16BitOperand(dest), true);
            
            debugln(formatDebugTwoOp("SBB", dest, src, destVal, result, srcVal, "-") + " (with borrow)");
        });



        // Jump instructions - aliases for existing jumps
        addInstruction("JZ", (instr) -> {
            String label = instr.getArgument();
            if (scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JZ " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JZ " + label + " (not taken)");
            }
        });

        addInstruction("JNZ", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNZ " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNZ " + label + " (not taken)");
            }
        });

        // JGE - Jump if Greater or Equal (ZF=0 AND SF=OF)
        addInstruction("JGE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isSignFlag() == scope.isOverflowFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JGE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JGE " + label + " (not taken)");
            }
        });

        // JNL - Jump if Not Less (same as JGE)
        addInstruction("JNL", (instr) -> {
            String label = instr.getArgument();
            if (scope.isSignFlag() == scope.isOverflowFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNL " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNL " + label + " (not taken)");
            }
        });

        // JNGE - Jump if Not Greater or Equal (same as JL)
        addInstruction("JNGE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isSignFlag() != scope.isOverflowFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNGE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNGE " + label + " (not taken)");
            }
        });

        // JNG - Jump if Not Greater (same as JLE)
        addInstruction("JNG", (instr) -> {
            String label = instr.getArgument();
            if (scope.isZeroFlag() || (scope.isSignFlag() != scope.isOverflowFlag())) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNG " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNG " + label + " (not taken)");
            }
        });

        // JNLE - Jump if Not Less or Equal (same as JG)
        addInstruction("JNLE", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isZeroFlag() && (scope.isSignFlag() == scope.isOverflowFlag())) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNLE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNLE " + label + " (not taken)");
            }
        });

        // Unsigned comparison jumps
        // JB - Jump if Below (CF=1)
        addInstruction("JB", (instr) -> {
            String label = instr.getArgument();
            if (scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JB " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JB " + label + " (not taken)");
            }
        });

        // JNAE - Jump if Not Above or Equal (same as JB)
        addInstruction("JNAE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNAE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNAE " + label + " (not taken)");
            }
        });

        // JBE - Jump if Below or Equal (CF=1 OR ZF=1)
        addInstruction("JBE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isCarryFlag() || scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JBE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JBE " + label + " (not taken)");
            }
        });

        // JNA - Jump if Not Above (same as JBE)
        addInstruction("JNA", (instr) -> {
            String label = instr.getArgument();
            if (scope.isCarryFlag() || scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNA " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNA " + label + " (not taken)");
            }
        });

        // JA - Jump if Above (CF=0 AND ZF=0)
        addInstruction("JA", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isCarryFlag() && !scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JA " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JA " + label + " (not taken)");
            }
        });

        // JNBE - Jump if Not Below or Equal (same as JA)
        addInstruction("JNBE", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isCarryFlag() && !scope.isZeroFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNBE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNBE " + label + " (not taken)");
            }
        });

        // JAE - Jump if Above or Equal (CF=0)
        addInstruction("JAE", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JAE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JAE " + label + " (not taken)");
            }
        });

        // JNB - Jump if Not Below (same as JAE)
        addInstruction("JNB", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNB " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNB " + label + " (not taken)");
            }
        });

        // Flag-based jumps
        // JS - Jump if Sign (SF=1)
        addInstruction("JS", (instr) -> {
            String label = instr.getArgument();
            if (scope.isSignFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JS " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JS " + label + " (not taken)");
            }
        });

        // JNS - Jump if Not Sign (SF=0)
        addInstruction("JNS", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isSignFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNS " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNS " + label + " (not taken)");
            }
        });

        // JO - Jump if Overflow (OF=1)
        addInstruction("JO", (instr) -> {
            String label = instr.getArgument();
            if (scope.isOverflowFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JO " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JO " + label + " (not taken)");
            }
        });

        // JNO - Jump if Not Overflow (OF=0)
        addInstruction("JNO", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isOverflowFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNO " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNO " + label + " (not taken)");
            }
        });

        // JP - Jump if Parity (PF=1)
        addInstruction("JP", (instr) -> {
            String label = instr.getArgument();
            if (scope.isParityFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JP " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JP " + label + " (not taken)");
            }
        });

        // JPE - Jump if Parity Even (same as JP)
        addInstruction("JPE", (instr) -> {
            String label = instr.getArgument();
            if (scope.isParityFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JPE " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JPE " + label + " (not taken)");
            }
        });

        // JNP - Jump if Not Parity (PF=0)
        addInstruction("JNP", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isParityFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNP " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNP " + label + " (not taken)");
            }
        });

        // JPO - Jump if Parity Odd (same as JNP)
        addInstruction("JPO", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isParityFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JPO " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JPO " + label + " (not taken)");
            }
        });

        // JC - Jump if Carry (CF=1)
        addInstruction("JC", (instr) -> {
            String label = instr.getArgument();
            if (scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JC " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JC " + label + " (not taken)");
            }
        });

        // JNC - Jump if Not Carry (CF=0)
        addInstruction("JNC", (instr) -> {
            String label = instr.getArgument();
            if (!scope.isCarryFlag()) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JNC " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JNC " + label + " (not taken)");
            }
        });

        // JCXZ - Jump if CX is Zero
        addInstruction("JCXZ", (instr) -> {
            String label = instr.getArgument();
            if (scope.getRegister16("CX") == 0) {
                Integer targetIndex = labels.get(label);
                if (targetIndex != null) {
                    scope.setPc(targetIndex - 1);
                    debugln("JCXZ " + label + " (taken) -> index: " + targetIndex);
                } else {
                    throw new IllegalStateException("Label not found: " + label);
                }
            } else {
                debugln("JCXZ " + label + " (not taken)");
            }
        });
    }

    /**
     * Initializes the program by parsing the assembly source code
     * Processes sections, labels, and instructions in multiple passes
     * 
     * @param contents The assembly source code as a string
     */
    @SneakyThrows
    @Override
    protected void initProgram(String contents)
    {
        ASM8088ParserHelper helper = new ASM8088ParserHelper();
        var parseResult = helper.parseString(contents);

        if (haltIfErrors(parseResult)) {
            return;
        }

        var ctx = parseResult.getProgramContext();
        List<asm8088Parser.LineContext> lines = ctx.line();

        String currentSection = null;
        for (var line : lines)
        {
            if (line.section() != null) {
                currentSection = handleSections(line);
            }
            else if (line.assignment() != null) {
                handleAssignment(line);
            }

            if (currentSection == null) {
                continue;
            }

            String lineText = getLineText(line);

            if (lineText.trim().isEmpty()) {
                continue;
            }

            switch (currentSection) {
                case ".TEXT":
                    textSectionLines.add(lineText);
                    break;
                case ".DATA":
                    dataSectionLines.add(lineText);
                    break;
                case ".BSS":
                    bssSectionLines.add(lineText);
                    break;
            }
        }

        if (!dataSectionLines.isEmpty()) {
            scope.processSectData(dataSectionLines);
        }

        if (!bssSectionLines.isEmpty()) {
            scope.processSectBss(bssSectionLines);
        }

        processTextSection();
        
        debugln("Program initialization completed:");
        debugln("- Instructions: " + instructions.size());
        debugln("- Labels: " + labels.size());
        debugln("- Data entries: " + dataSectionLines.size());
        debugln("- BSS entries: " + bssSectionLines.size());
    }

    /**
     * Handles section directives (.SECT .TEXT, .SECT .DATA, .SECT .BSS)
     * 
     * @param line The line context containing the section directive
     * @return The section name (.TEXT, .DATA, or .BSS)
     */
    private String handleSections(asm8088Parser.LineContext line)
    {
        var section = line.section();

        if (section == null) {
            return null;
        }

        if (section.TEXT() != null) {
            return ".TEXT";
        }
        else if (section.DATA() != null) {
            return ".DATA";
        }
        else if (section.BSS() != null) {
            return ".BSS";
        }

        return null;
    }

    /**
     * Handles assignment statements (constants like _PRINTF = 127)
     * 
     * @param line The line context containing the assignment
     */
    private void handleAssignment(asm8088Parser.LineContext line)
    {
        var assignment = line.assignment();
        if (assignment == null) return;

        String id = assignment.ID().getText();
        int value;

        if (assignment.HEX() != null) {
            String hexStr = assignment.HEX().getText();
            value = Integer.parseInt(hexStr.substring(2), 16); // Remove 0x prefix
        }
        else if (assignment.NUM() != null) {
            value = Integer.parseInt(assignment.NUM().getText());
        }
        else {
            return;
        }

        scope.getLabelAddresses().put(id, value);
        debugln("Constant: " + id + " = " + value);
    }

    /**
     * Processes the .TEXT section to extract labels and instructions
     * Builds the instruction list and label table for execution
     */
    private void processTextSection()
    {
        for (String line : textSectionLines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith(";")) {
                continue;
            }

            // Check if line contains a label
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String label = parts[0].trim();
                
                // Store label with current instruction index
                labels.put(label, instructions.size());
                debugln("Label: " + label + " at instruction " + instructions.size());

                // Process instruction part if present
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    processInstructionLine(parts[1].trim());
                }
            } else {
                // Regular instruction line
                processInstructionLine(line);
            }
        }
    }

    /**
     * Processes a single instruction line and adds it to the instruction list
     * 
     * @param line The instruction line to process
     */
    private void processInstructionLine(String line)
    {
        line = line.trim();
        if (line.isEmpty()) return;

        // Split instruction into opcode and operands
        String[] parts = line.split("\\s+", 2);
        String opCode = parts[0].toUpperCase();

        ASM8088Instruction instruction;
        if (parts.length > 1) {
            // Instruction has operands
            String operands = parts[1].trim();
            if (operands.contains(",")) {
                // Two operands
                String[] operandParts = operands.split(",", 2);
                instruction = new ASM8088Instruction(opCode, 
                    operandParts[0].trim(), 
                    operandParts[1].trim());
            } else {
                // Single operand
                instruction = new ASM8088Instruction(opCode, operands);
            }
        } else {
            // No operands
            instruction = new ASM8088Instruction(opCode);
        }

        instructions.add((T) instruction);
        debugln("Instruction " + instructions.size() + ": " + instruction);
    }

    /**
     * Extracts the text content from a line context
     * 
     * @param line The line context
     * @return The text representation of the line
     */
    private String getLineText(asm8088Parser.LineContext line)
    {
        if (line.labelDecl() != null && line.statement() != null) {
            return line.labelDecl().getText() + " " + getStatementText(line.statement());
        } else if (line.labelDecl() != null) {
            return line.labelDecl().getText();
        } else if (line.statement() != null) {
            return getStatementText(line.statement());
        }
        return "";
    }

    /**
     * Extracts the text content from a statement context
     * 
     * @param statement The statement context
     * @return The text representation of the statement
     */
    private String getStatementText(asm8088Parser.StatementContext statement)
    {
        if (statement.instruction() != null) {
            return getInstructionText(statement.instruction());
        } else if (statement.directive() != null) {
            return getDirectiveText(statement.directive());
        }
        return "";
    }

    /**
     * Extracts the text content from an instruction context
     * 
     * @param instruction The instruction context
     * @return The text representation of the instruction
     */
    private String getInstructionText(asm8088Parser.InstructionContext instruction)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(instruction.mnemonic().getText());
        
        if (instruction.operandList() != null) {
            sb.append(" ").append(instruction.operandList().getText());
        }
        
        return sb.toString();
    }

    /**
     * Extracts the text content from a directive context
     * 
     * @param directive The directive context
     * @return The text representation of the directive
     */
    private String getDirectiveText(asm8088Parser.DirectiveContext directive)
    {
        if (directive.BYTE() != null) {
            return ".BYTE " + directive.valueList().getText();
        } else if (directive.ASCII() != null) {
            return ".ASCII " + directive.STRING().getText();
        } else if (directive.SPACE() != null) {
            return ".SPACE " + directive.NUM().getText();
        }
        return "";
    }

    /**
     * Gets the value of an operand (register, immediate, memory, or label)
     * 
     * @param operand The operand string to evaluate
     * @return The integer value of the operand
     */
    private int getValue(String operand)
    {
        if (operand == null) return 0;
        
        operand = operand.trim();
        
        // Handle memory references [operand]
        if (operand.startsWith("[") && operand.endsWith("]")) {
            String memRef = operand.substring(1, operand.length() - 1).trim();
            int address = getMemoryAddress(memRef);
            
            if (is16BitMemoryAccess(memRef)) {
                return scope.readWord(address);
            } else {
                return scope.readByte(address) & 0xFF;
            }
        }
        
        // Handle 16-bit registers
        if (isRegister16(operand)) {
            return scope.getRegister16(operand);
        }
        
        // Handle 8-bit registers
        if (isRegister8(operand)) {
            return scope.getRegister8(operand);
        }
        
        // Handle segment registers
        if (isSegmentRegister(operand)) {
            return scope.getSegmentRegister(operand);
        }
        
        // Handle immediate values (hex or decimal)
        if (operand.startsWith("0x") || operand.startsWith("0X")) {
            return Integer.parseInt(operand.substring(2), 16);
        }
        
        // Handle arithmetic expressions like v2-v1
        if (operand.contains("-")) {
            String[] parts = operand.split("-");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                
                // Try to resolve both parts as labels
                int leftAddr = scope.getLabelAddress(left);
                int rightAddr = scope.getLabelAddress(right);
                
                if (leftAddr != -1 && rightAddr != -1) {
                    return leftAddr - rightAddr;
                }
            }
        }
        
        if (operand.contains("+")) {
            String[] parts = operand.split("\\+");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                
                // Try to resolve both parts as labels
                int leftAddr = scope.getLabelAddress(left);
                int rightAddr = scope.getLabelAddress(right);
                
                if (leftAddr != -1 && rightAddr != -1) {
                    return leftAddr + rightAddr;
                }
            }
        }
        
        // Handle decimal immediate
        try {
            return Integer.parseInt(operand);
        } catch (NumberFormatException e) {
            // Handle labels and constants
            Integer labelAddr = scope.getLabelAddress(operand);
            if (labelAddr != -1) {
                return labelAddr;
            }
            
            // Default to 0 for unknown operands
            return 0;
        }
    }

    /**
     * Sets the value of an operand (register or memory)
     * 
     * @param operand The operand string to set
     * @param value The value to set
     */
    private void setValue(String operand, int value)
    {
        if (operand == null) return;
        
        operand = operand.trim();
        
        // Handle memory references [operand]
        if (operand.startsWith("[") && operand.endsWith("]")) {
            String memRef = operand.substring(1, operand.length() - 1).trim();
            int address = getMemoryAddress(memRef);
            
            if (is16BitMemoryAccess(memRef)) {
                scope.writeWord(address, value & 0xFFFF);
            } else {
                scope.writeByte(address, (byte) (value & 0xFF));
            }
            return;
        }
        
        // Handle 16-bit registers
        if (isRegister16(operand)) {
            scope.setRegister16(operand, value & 0xFFFF);
            return;
        }
        
        // Handle 8-bit registers
        if (isRegister8(operand)) {
            scope.setRegister8(operand, value & 0xFF);
            return;
        }
        
        // Handle segment registers
        if (isSegmentRegister(operand)) {
            scope.setSegmentRegister(operand, value & 0xFFFF);
            return;
        }
    }

    /**
     * Calculates memory address from a memory reference string
     * 
     * @param memRef The memory reference (without brackets)
     * @return The calculated memory address
     */
    private int getMemoryAddress(String memRef)
    {
        memRef = memRef.trim();
        
        // Handle simple label reference
        if (memRef.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            Integer labelAddr = scope.getLabelAddress(memRef);
            return labelAddr != -1 ? labelAddr : 0;
        }
        
        // Handle register + offset patterns like "BX+SI", "BP+8", etc.
        if (memRef.contains("+")) {
            String[] parts = memRef.split("\\+");
            int offset = 0;
            boolean usesStackSegment = false;
            

            
            for (String part : parts) {
                part = part.trim();
                if (isRegister16(part)) {
                    if (part.equals("BP")) {
                        // BP-relative addressing uses stack segment
                        usesStackSegment = true;
                        offset += scope.getRegister16(part);
                    } else {
                        offset += scope.getRegister16(part);
                    }
                } else if (part.matches("\\d+")) {
                    offset += Integer.parseInt(part);
                } else if (part.startsWith("0x")) {
                    offset += Integer.parseInt(part.substring(2), 16);
                } else {
                    // Handle label
                    int labelAddr = scope.getLabelAddress(part);
                    if (labelAddr != -1) {
                        offset += labelAddr;
                    }
                }
            }
            
            // For BP-relative addressing, use stack addressing
            if (usesStackSegment) {
                return scope.getStackAddress(offset);
            }
            
            return offset & 0xFFFFF; // 20-bit address space
        }
        
        // Handle register - offset patterns
        if (memRef.contains("-")) {
            String[] parts = memRef.split("-");
            int address = 0;
            
            // First part is added
            String part = parts[0].trim();
            if (isRegister16(part)) {
                address += scope.getRegister16(part);
            } else if (part.matches("\\d+")) {
                address += Integer.parseInt(part);
            }
            
            // Remaining parts are subtracted
            for (int i = 1; i < parts.length; i++) {
                part = parts[i].trim();
                if (isRegister16(part)) {
                    address -= scope.getRegister16(part);
                } else if (part.matches("\\d+")) {
                    address -= Integer.parseInt(part);
                }
            }
            return address & 0xFFFFF;
        }
        
        // Handle simple register
        if (isRegister16(memRef)) {
            return scope.getRegister16(memRef);
        }
        
        // Handle immediate address
        try {
            if (memRef.startsWith("0x")) {
                return Integer.parseInt(memRef.substring(2), 16);
            } else {
                return Integer.parseInt(memRef);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Determines if an operand represents a 16-bit operation
     * 
     * @param operand The operand to check
     * @return true if 16-bit, false if 8-bit
     */
    private boolean is16BitOperand(String operand)
    {
        if (operand == null) return false;
        
        operand = operand.trim();
        
        // Memory references default to 16-bit unless explicitly 8-bit
        if (operand.startsWith("[") && operand.endsWith("]")) {
            return is16BitMemoryAccess(operand.substring(1, operand.length() - 1));
        }
        
        return isRegister16(operand) || isSegmentRegister(operand);
    }

    /**
     * Determines if a memory access is 16-bit based on context
     * 
     * @param memRef The memory reference string
     * @return true if 16-bit access, false if 8-bit
     */
    private boolean is16BitMemoryAccess(String memRef)
    {
        memRef = memRef.trim().toUpperCase();

        // If the memory reference uses 8-bit registers, treat as 8-bit
        if (memRef.matches(".*\\b(AL|AH|BL|BH|CL|CH|DL|DH)\\b.*")) {
            return false;
        }

        // If the memory reference uses 16-bit registers, treat as 16-bit
        if (memRef.matches(".*\\b(AX|BX|CX|DX|SI|DI|BP|SP)\\b.*")) {
            return true;
        }

        // Default: treat as 16-bit
        return true;
    }

    /**
     * Checks if an operand is a 16-bit register
     * 
     * @param operand The operand to check
     * @return true if it's a 16-bit register
     */
    private boolean isRegister16(String operand)
    {
        return operand.matches("(AX|BX|CX|DX|SI|DI|BP|SP)");
    }

    /**
     * Checks if an operand is an 8-bit register
     * 
     * @param operand The operand to check
     * @return true if it's an 8-bit register
     */
    private boolean isRegister8(String operand)
    {
        return operand.matches("(AL|AH|BL|BH|CL|CH|DL|DH)");
    }

    /**
     * Checks if an operand is a segment register
     * 
     * @param operand The operand to check
     * @return true if it's a segment register
     */
    private boolean isSegmentRegister(String operand)
    {
        return operand.matches("(DS|ES|SS)");
    }

    /**
     * Formats debug output for instruction execution with detailed operand information
     * 
     * @param instruction The instruction mnemonic
     * @param operand The operand being modified
     * @param oldValue The value before the operation
     * @param newValue The value after the operation
     * @param extraInfo Additional information about the operation
     * @return Formatted debug string
     */
    private String formatDebug(String instruction, String operand, int oldValue, int newValue, String extraInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(instruction).append(" ").append(operand);
        
        // Add address information for memory references
        if (operand.startsWith("[") && operand.endsWith("]")) {
            String memRef = operand.substring(1, operand.length() - 1).trim();
            int address = getMemoryAddress(memRef);
            sb.append(" (addr: 0x").append(String.format("%04X", address));
            
            // Show both 16-bit and 8-bit values for memory
            byte byteVal = scope.readByte(address);
            int wordVal = scope.readWord(address);
            sb.append(", mem - 16bit: 0x").append(String.format("%04X", wordVal & 0xFFFF));
            sb.append(" | 8bit: 0x").append(String.format("%02X", byteVal & 0xFF)).append(")");
        } 
        // Add register information
        else if (isRegister16(operand) || isRegister8(operand) || isSegmentRegister(operand)) {
            sb.append(" (reg");
            if (is16BitOperand(operand)) {
                sb.append(" - 16bit: 0x").append(String.format("%04X", newValue & 0xFFFF));
            } else {
                sb.append(" - 8bit: 0x").append(String.format("%02X", newValue & 0xFF));
            }
            sb.append(")");
        }
        
        // Add the value change arrow
        sb.append(" => ");
        if (is16BitOperand(operand)) {
            sb.append("0x").append(String.format("%04X", oldValue & 0xFFFF));
            sb.append(" -> 0x").append(String.format("%04X", newValue & 0xFFFF));
        } else {
            sb.append("0x").append(String.format("%02X", oldValue & 0xFF));
            sb.append(" -> 0x").append(String.format("%02X", newValue & 0xFF));
        }
        
        // Add extra information if provided
        if (extraInfo != null && !extraInfo.isEmpty()) {
            sb.append(" ").append(extraInfo);
        }
        
        return sb.toString();
    }

    /**
     * Formats debug output for two-operand instructions (like ADD, SUB, etc.)
     * 
     * @param instruction The instruction mnemonic
     * @param dest The destination operand
     * @param src The source operand
     * @param destOldValue The old value of destination
     * @param destNewValue The new value of destination
     * @param srcValue The value of source
     * @param operation The operation symbol (e.g., "+", "-", "&")
     * @return Formatted debug string
     */
    private String formatDebugTwoOp(String instruction, String dest, String src, int destOldValue, int destNewValue, int srcValue, String operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(instruction).append(" ").append(dest).append(", ").append(src);
        
        // Add destination address/register info
        if (dest.startsWith("[") && dest.endsWith("]")) {
            String memRef = dest.substring(1, dest.length() - 1).trim();
            int address = getMemoryAddress(memRef);
            sb.append(" (dest addr: 0x").append(String.format("%04X", address)).append(")");
        } else if (isRegister16(dest) || isRegister8(dest) || isSegmentRegister(dest)) {
            sb.append(" (dest reg)");
        }
        
        // Add the operation details
        sb.append(" => ");
        if (is16BitOperand(dest)) {
            sb.append("0x").append(String.format("%04X", destOldValue & 0xFFFF));
            sb.append(" ").append(operation).append(" 0x").append(String.format("%04X", srcValue & 0xFFFF));
            sb.append(" = 0x").append(String.format("%04X", destNewValue & 0xFFFF));
        } else {
            sb.append("0x").append(String.format("%02X", destOldValue & 0xFF));
            sb.append(" ").append(operation).append(" 0x").append(String.format("%02X", srcValue & 0xFF));
            sb.append(" = 0x").append(String.format("%02X", destNewValue & 0xFF));
        }
        
        return sb.toString();
    }

    /**
     * Gets the list of instructions for this program
     * Used by the tracer to access the instruction list
     * 
     * @return The list of ASM8088 instructions
     */
    public List<ASM8088Instruction> getInstructions() {
        return instructions;
    }

    /**
     * Retrieves a string from the stack for printf system call
     * 
     * @return The format string from memory
     */
    private String getStringFromStack()
    {
        int stringAddr = scope.popStack();
        StringBuilder sb = new StringBuilder();
        
        int addr = stringAddr;
        byte b;
        while ((b = scope.readByte(addr)) != 0 && addr < stringAddr + 256) { // Limit to prevent infinite loop
            sb.append((char) b);
            addr++;
        }
        
        return sb.toString();
    }

    /**
     * Interprets a single instruction by delegating to the appropriate handler
     * 
     * @param instruction The instruction to interpret
     */
    @Override
    public void interpret(T instruction)
    {
        String opCode = instruction.getOpCode().toUpperCase();
        Consumer<T> handler = instructionHandlers.get(opCode);
        
        if (handler != null) {
            handler.accept(instruction);

            if (Utils.TRACER != null) {
                Utils.TRACER.displayTrace();
            }
        }
        else {
            throw new IllegalArgumentException("Unknown instruction: " + opCode);
        }
    }

    /**
     * Executes the program by running through all instructions
     * Uses the program counter to track execution progress
     */
    @SneakyThrows
    @Override
    public void execute()
    {
        resume();
        
        scope.setPc(-1);

        while (scope.getPc() + 1 < instructions.size())
        {
            int pc = scope.getPc() + 1;
            scope.setPc(pc);

            ASM8088Instruction instruction = instructions.get(pc);

            debugln("PC=" + pc + ", Next instruction: " + instruction);
            interpret((T) instruction);
        }
        
        debugln("Program execution completed.");
        debugln("Final state - PC: " + scope.getPc());
    }
}

