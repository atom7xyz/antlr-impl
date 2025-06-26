package xyz.atom7.tracer.asm8088;

import lombok.Getter;
import xyz.atom7.api.tracer.Tracer;
import xyz.atom7.interpreter.asm8088.ASM8088Instruction;
import xyz.atom7.interpreter.asm8088.ASM8088Program;

import java.util.Map;
import java.util.Objects;

public class ASM8088Tracer extends Tracer<ASM8088Program<ASM8088Instruction>, ASM8088Instruction, ASM8088Snapshot>
{
    public ASM8088Tracer(ASM8088Program<ASM8088Instruction> interpreter)
    {
        super(interpreter);
    }

    @Override
    public void takeSnapshot()
    {
        interpreter.halt();

        var snapshot = new ASM8088Snapshot(interpreter, interpreter.getInstructions(), interpreter.getScope().getPc());
        snapshot.snapshot();

        previousSnapshot = currentSnapshot;
        currentSnapshot = snapshot;

        interpreter.resume();
    }

    @Override
    protected boolean compareSnapshots()
    {
        return Objects.deepEquals(previousSnapshot, currentSnapshot);
    }

    @Override
    public void displayTrace()
    {
        takeSnapshot();
        var snapshot = currentSnapshot;

        if (compareSnapshots() || snapshot == null) {
            return;
        }

        setStep(step + 1);

        var currentInstructionTemp = getCurrentInstruction();
        var currentInstructionDisplay = currentInstructionTemp == null ?
                "none" : currentInstructionTemp.toString();

        String stackDisplay = formatStackDisplay(snapshot.getStack());
        String memoryDisplay = formatMapWithHex(snapshot.getMemory());
        String labelsDisplay = formatMapWithHex(snapshot.getLabels());

        printColoredNoValue("\n\n\n+++ ASM8088 TRACER (step: " + step + ") +++", ColorCode.GREEN);

        printColored("\nStatic", "", ColorCode.YELLOW);
        printColored("\tLabels", labelsDisplay, ColorCode.YELLOW);
        printColored("\tMemory (non-zero)", memoryDisplay, ColorCode.YELLOW);

        printColored("\nCurrent", "", ColorCode.RED);
        printColored("\tInstruction", currentInstructionDisplay, ColorCode.RED);
        if (snapshot.getCurrentScope() != null) {
            printColored("\tPC", snapshot.getProgramCounter(), ColorCode.RED);
            printColored("\tScope Name", snapshot.getCurrentScope().getName(), ColorCode.RED);
        } else {
            printColored("\tPC", "n/a", ColorCode.RED);
            printColored("\tScope", "none", ColorCode.RED);
        }

        var registers16 = snapshot.getRegisters16();
        var registers8 = snapshot.getRegisters8();
        var segmentRegisters = snapshot.getSegmentRegisters();

        printColored("\n16-bit Registers", "", ColorCode.BLUE);
        printColored("\tAX", formatHex16(registers16.getOrDefault("AX", 0)), ColorCode.BLUE);
        printColored("\tBX", formatHex16(registers16.getOrDefault("BX", 0)), ColorCode.BLUE);
        printColored("\tCX", formatHex16(registers16.getOrDefault("CX", 0)), ColorCode.BLUE);
        printColored("\tDX", formatHex16(registers16.getOrDefault("DX", 0)), ColorCode.BLUE);
        printColored("\tSI", formatHex16(registers16.getOrDefault("SI", 0)), ColorCode.BLUE);
        printColored("\tDI", formatHex16(registers16.getOrDefault("DI", 0)), ColorCode.BLUE);
        printColored("\tBP", formatHex16(registers16.getOrDefault("BP", 0)), ColorCode.BLUE);
        printColored("\tSP", formatHex16(registers16.getOrDefault("SP", 0)), ColorCode.BLUE);

        printColored("\n8-bit Registers", "", ColorCode.CYAN);
        printColored("\tAL", formatHex8(registers8.getOrDefault("AL", 0)), ColorCode.CYAN);
        printColored("\tAH", formatHex8(registers8.getOrDefault("AH", 0)), ColorCode.CYAN);
        printColored("\tBL", formatHex8(registers8.getOrDefault("BL", 0)), ColorCode.CYAN);
        printColored("\tBH", formatHex8(registers8.getOrDefault("BH", 0)), ColorCode.CYAN);
        printColored("\tCL", formatHex8(registers8.getOrDefault("CL", 0)), ColorCode.CYAN);
        printColored("\tCH", formatHex8(registers8.getOrDefault("CH", 0)), ColorCode.CYAN);
        printColored("\tDL", formatHex8(registers8.getOrDefault("DL", 0)), ColorCode.CYAN);
        printColored("\tDH", formatHex8(registers8.getOrDefault("DH", 0)), ColorCode.CYAN);

        printColored("\nSegment Registers", "", ColorCode.MAGENTA);
        printColored("\tDS", formatHex16(segmentRegisters.getOrDefault("DS", 0)), ColorCode.MAGENTA);
        printColored("\tES", formatHex16(segmentRegisters.getOrDefault("ES", 0)), ColorCode.MAGENTA);
        printColored("\tSS", formatHex16(segmentRegisters.getOrDefault("SS", 0)), ColorCode.MAGENTA);

        printColored("\nFlags", "", ColorCode.PURPLE);
        printColored("\tZero Flag (ZF)", snapshot.isZeroFlag() ? "1" : "0", ColorCode.PURPLE);
        printColored("\tCarry Flag (CF)", snapshot.isCarryFlag() ? "1" : "0", ColorCode.PURPLE);
        printColored("\tSign Flag (SF)", snapshot.isSignFlag() ? "1" : "0", ColorCode.PURPLE);

        printColored("\nStack", "", ColorCode.BOLD_BLUE);
        printColored("\tStack", stackDisplay, ColorCode.BOLD_BLUE);

        if (!snapshot.getOutputBuffer().isEmpty()) {
            StringBuilder outputStr = new StringBuilder();
            for (int value : snapshot.getOutputBuffer()) {
                outputStr.append((char) value);
            }
            printColored("\nOutput", outputStr.toString(), ColorCode.BOLD_GREEN);
        }
    }

    /**
     * Get the current instruction being executed
     * 
     * @return The current instruction
     */
    private ASM8088Instruction getCurrentInstruction()
    {
        if (interpreter.getScope() == null) {
            return null;
        }

        int pc = interpreter.getScope().getPc();
        var instructions = interpreter.getInstructions();

        if (pc < 0 || pc >= instructions.size()) {
            return null;
        }

        return instructions.get(pc);
    }

    /**
     * Format the stack for display
     * 
     * @param stack The stack to format
     * @return The formatted stack
     */
    private String formatStackDisplay(java.util.List<Integer> stack)
    {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            int value = stack.get(i);
            sb.append("0x").append(String.format("%04X", value & 0xFFFF));
        }
        sb.append("]");
        
        // Add a note about stack size if it's large
        if (stack.size() > 20) {
            sb.append(" (").append(stack.size()).append(" entries)");
        }
        
        return sb.toString();
    }

    /**
     * Format a map with hex values for display
     * 
     * @param map The map to format
     * @return The formatted map
     */
    private String formatMapWithHex(Map<String, Integer> map)
    {
        if (map == null || map.isEmpty()) {
            return "empty";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=");
            
            // Format all values consistently as hex
            int value = entry.getValue();
            if (entry.getKey().startsWith("0x") || value > 255) {
                sb.append("0x").append(String.format("%04X", value & 0xFFFF));
            } else {
                sb.append("0x").append(String.format("%02X", value & 0xFF));
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Format a 16-bit value as hex
     * 
     * @param value The value to format
     * @return The formatted value
     */
    private String formatHex16(int value)
    {
        return "0x" + String.format("%04X", value & 0xFFFF) + " (" + (value & 0xFFFF) + ")";
    }

    /**
     * Format an 8-bit value as hex
     * 
     * @param value The value to format
     * @return The formatted value
     */
    private String formatHex8(int value)
    {
        return "0x" + String.format("%02X", value & 0xFF) + " (" + (value & 0xFF) + ")";
    }
} 