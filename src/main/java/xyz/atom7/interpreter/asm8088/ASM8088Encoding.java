package xyz.atom7.interpreter.asm8088;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Computes the byte addresses of the instructions in the text segment.
 * The size of each instruction follows the 8086 encoding: opcode, ModR/M byte, displacement and immediate.
 * Jumps use the short form when the target is in range and the near form otherwise.
 */
public final class ASM8088Encoding
{
    private static final Set<String> REGISTERS_16 = Set.of("AX", "BX", "CX", "DX", "SI", "DI", "BP", "SP");
    private static final Set<String> REGISTERS_8 = Set.of("AL", "AH", "BL", "BH", "CL", "CH", "DL", "DH");
    private static final Set<String> SEGMENT_REGISTERS = Set.of("CS", "DS", "ES", "SS");

    private static final Set<String> ALU = Set.of("ADD", "SUB", "CMP", "AND", "OR", "XOR", "ADC", "SBB");
    private static final Set<String> UNARY = Set.of("MUL", "DIV", "NOT", "NEG", "IMUL", "IDIV");
    private static final Set<String> BYTE_OPERATIONS = Set.of(
            "MOVB", "ADDB", "SUBB", "CMPB", "ANDB", "ORB", "XORB", "ADCB", "SBBB",
            "MULB", "DIVB", "NOTB", "NEGB", "INCB", "DECB");
    private static final Set<String> SHORT_ONLY_JUMPS = Set.of("LOOP", "JCXZ");

    private static final Pattern NUMBER = Pattern.compile("-?(0[xX][0-9a-fA-F]+|\\d+)");

    private static final int MAX_PASSES = 16;

    private ASM8088Encoding()
    {
    }

    /**
     * Assigns a byte address to every instruction, starting from address 0.
     *
     * @param instructions The instructions of the text segment
     * @param labels The instruction index of every label
     * @return The size of the text segment in bytes
     */
    public static int assignAddresses(List<? extends ASM8088Instruction> instructions, Map<String, Integer> labels)
    {
        int count = instructions.size();
        boolean[] longJumps = new boolean[count];
        int[] addresses = new int[count + 1];

        // A jump grows only from short to long, so the passes end after at most one change per jump.
        for (int pass = 0; pass < MAX_PASSES; pass++)
        {
            int address = 0;

            for (int i = 0; i < count; i++)
            {
                addresses[i] = address;
                address += sizeOf(instructions.get(i), longJumps[i]);
            }

            addresses[count] = address;
            boolean changed = false;

            for (int i = 0; i < count; i++)
            {
                ASM8088Instruction instruction = instructions.get(i);

                if (longJumps[i] || !isRelativeJump(instruction)) {
                    continue;
                }

                Integer target = labels.get(instruction.getArgument());

                if (target == null) {
                    continue;
                }

                int next = addresses[i] + sizeOf(instruction, false);
                int displacement = addresses[Math.min(target, count)] - next;

                if (displacement < Byte.MIN_VALUE || displacement > Byte.MAX_VALUE) {
                    longJumps[i] = true;
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        for (int i = 0; i < count; i++) {
            instructions.get(i).setAddress(addresses[i]);
        }

        return addresses[count];
    }

    /**
     * Returns the size in bytes of an instruction.
     *
     * @param instruction The instruction
     * @param longJump True if a jump needs the near form
     * @return The size in bytes
     */
    static int sizeOf(ASM8088Instruction instruction, boolean longJump)
    {
        String opCode = instruction.getOpCode().toUpperCase();
        boolean byteOperation = BYTE_OPERATIONS.contains(opCode);
        String base = byteOperation ? opCode.substring(0, opCode.length() - 1) : opCode;

        String first = normalize(instruction.getArgument());
        String second = normalize(instruction.getSecondArgument());

        switch (base) {
            case "LABEL":
                return 0;
            case "HLT":
            case "NOP":
            case "CBW":
            case "CWD":
            case "CLC":
            case "STC":
            case "PUSHF":
            case "POPF":
                return 1;
            case "RET":
                return first == null ? 1 : 3;
            case "SYS":
            case "INT":
                return 2;
            case "CALL":
                return isRegister16(first) ? 2 : isMemory(first) ? 2 + displacement(first) : 3;
            case "JMP":
                return longJump ? 3 : 2;
            case "MOV":
                return sizeOfMov(first, second, byteOperation);
            case "PUSH":
            case "POP":
                return sizeOfPushPop(base, first);
            case "INC":
            case "DEC":
                if (isRegister16(first)) {
                    return 1;
                }
                return 2 + displacement(first);
            default:
                break;
        }

        if (ALU.contains(base)) {
            return sizeOfAlu(first, second, byteOperation);
        }

        if (UNARY.contains(base)) {
            return 2 + displacement(first);
        }

        if (SHORT_ONLY_JUMPS.contains(base)) {
            return 2;
        }

        if (base.startsWith("J")) {
            // 8086 conditional jumps are short; a far target needs an inverted jump around a near JMP.
            return longJump ? 5 : 2;
        }

        return 2 + displacement(first) + displacement(second);
    }

    private static boolean isRelativeJump(ASM8088Instruction instruction)
    {
        String opCode = instruction.getOpCode().toUpperCase();
        return opCode.startsWith("J") && !SHORT_ONLY_JUMPS.contains(opCode);
    }

    private static int sizeOfMov(String destination, String source, boolean byteOperation)
    {
        boolean wide = !byteOperation && !isRegister8(destination) && !isRegister8(source);

        if (isSegmentRegister(destination) || isSegmentRegister(source)) {
            return 2 + displacement(destination) + displacement(source);
        }

        if (isRegister(destination) && isImmediate(source)) {
            return wide ? 3 : 2;
        }

        if (isMemory(destination) && isImmediate(source)) {
            return 2 + displacement(destination) + (wide ? 2 : 1);
        }

        if (isAccumulator(destination) && isDirect(source) || isAccumulator(source) && isDirect(destination)) {
            return 3;
        }

        return 2 + displacement(destination) + displacement(source);
    }

    private static int sizeOfAlu(String destination, String source, boolean byteOperation)
    {
        boolean wide = !byteOperation && !isRegister8(destination) && !isRegister8(source);

        if (!isImmediate(source)) {
            return 2 + displacement(destination) + displacement(source);
        }

        if (isAccumulator(destination)) {
            return wide ? 3 : 2;
        }

        int immediate = !wide || fitsInByte(source) ? 1 : 2;
        return 2 + displacement(destination) + immediate;
    }

    private static int sizeOfPushPop(String opCode, String operand)
    {
        if (isRegister16(operand) || isSegmentRegister(operand)) {
            return 1;
        }

        if (isMemory(operand)) {
            return 2 + displacement(operand);
        }

        // PUSH with an immediate operand uses the 80186 encoding.
        if ("PUSH".equals(opCode)) {
            return fitsInByte(operand) ? 2 : 3;
        }

        return 2;
    }

    /**
     * Returns the size of the displacement of a memory operand, or 0 for other operands.
     */
    private static int displacement(String operand)
    {
        if (!isMemory(operand)) {
            return 0;
        }

        String inner = operand.substring(1, operand.length() - 1);
        String[] parts = inner.split("(?=[+-])");
        boolean hasRegister = false;
        boolean hasLabel = false;
        long offset = 0;

        for (String rawPart : parts)
        {
            String part = rawPart.trim();
            boolean negative = part.startsWith("-");
            part = part.replaceFirst("^[+-]", "").trim();

            if (part.isEmpty()) {
                continue;
            }

            if (REGISTERS_16.contains(part)) {
                hasRegister = true;
            }
            else if (NUMBER.matcher(part).matches()) {
                long value = parseNumber(part);
                offset += negative ? -value : value;
            }
            else {
                hasLabel = true;
            }
        }

        if (!hasRegister || hasLabel) {
            return 2;
        }

        if (offset == 0) {
            // [BP] has no encoding without a displacement, so it uses a zero byte.
            return inner.trim().equals("BP") ? 1 : 0;
        }

        return offset >= Byte.MIN_VALUE && offset <= Byte.MAX_VALUE ? 1 : 2;
    }

    private static boolean fitsInByte(String operand)
    {
        if (operand == null || !NUMBER.matcher(operand).matches()) {
            return false;
        }

        long value = parseNumber(operand);
        return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
    }

    private static long parseNumber(String text)
    {
        boolean negative = text.startsWith("-");
        String digits = negative ? text.substring(1) : text;
        long value = digits.startsWith("0x") || digits.startsWith("0X")
                ? Long.parseLong(digits.substring(2), 16)
                : Long.parseLong(digits);
        return negative ? -value : value;
    }

    private static String normalize(String operand)
    {
        return operand == null ? null : operand.trim().toUpperCase().replace("0X", "0x");
    }

    private static boolean isMemory(String operand)
    {
        return operand != null && operand.startsWith("[") && operand.endsWith("]");
    }

    private static boolean isDirect(String operand)
    {
        if (!isMemory(operand)) {
            return false;
        }

        for (String part : operand.substring(1, operand.length() - 1).split("[+-]")) {
            if (REGISTERS_16.contains(part.trim())) {
                return false;
            }
        }

        return true;
    }

    private static boolean isRegister16(String operand)
    {
        return operand != null && REGISTERS_16.contains(operand);
    }

    private static boolean isRegister8(String operand)
    {
        return operand != null && REGISTERS_8.contains(operand);
    }

    private static boolean isSegmentRegister(String operand)
    {
        return operand != null && SEGMENT_REGISTERS.contains(operand);
    }

    private static boolean isRegister(String operand)
    {
        return isRegister16(operand) || isRegister8(operand);
    }

    private static boolean isAccumulator(String operand)
    {
        return "AX".equals(operand) || "AL".equals(operand);
    }

    private static boolean isImmediate(String operand)
    {
        return operand != null && !isRegister(operand) && !isSegmentRegister(operand) && !isMemory(operand);
    }
}
