package xyz.atom7.bridge;

import xyz.atom7.interpreter.asm8088.ASM8088Instruction;
import xyz.atom7.interpreter.asm8088.ASM8088Program;
import xyz.atom7.interpreter.asm8088.ASM8088Program.CallFrame;
import xyz.atom7.interpreter.asm8088.ASM8088Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 8088 interpreter controlled by a DebugSession.
 * The instructions are the same as the CLI. Only input, output and program exit go through the session.
 */
final class ASM8088DebugProgram extends ASM8088Program<ASM8088Instruction> implements DebugTarget
{
    private static final String REGISTERS = "REGISTERS";
    private static final String STACK = "STACK";
    private static final String MEMORY = "MEMORY";
    private static final String FLAGS = "FLAGS";

    private static final String[][] GENERAL_REGISTERS = {
            { "AX", "AH", "AL" },
            { "BX", "BH", "BL" },
            { "CX", "CH", "CL" },
            { "DX", "DH", "DL" }
    };
    private static final String[] POINTER_REGISTERS = { "SI", "DI", "BP", "SP" };
    private static final String[] SEGMENT_REGISTERS = { "DS", "ES", "SS" };

    private static final int STACK_TOP = 0xFFFF;
    private static final int DATA_START = 0x1000;
    private static final int BSS_START = 0x2000;
    private static final int BSS_END = 0x3000;

    private final DebugSession session;

    ASM8088DebugProgram(DebugSession session)
    {
        super();
        this.session = session;
    }

    @Override
    protected String readInput()
    {
        return session.awaitInput();
    }

    @Override
    protected void writeOutput(String text)
    {
        session.writeOutput(text);
    }

    @Override
    protected void exitProgram(int exitCode)
    {
        throw SessionExit.finished(exitCode);
    }

    @Override
    public void interpret(ASM8088Instruction instruction)
    {
        session.beforeInstruction(instruction);
        super.interpret(instruction);
    }

    @Override
    public String[] getContexts()
    {
        return new String[] { REGISTERS, STACK, MEMORY, FLAGS };
    }

    @Override
    public void collectState(StateView view)
    {
        ASM8088Scope scope = getScope();

        collectRegisters(view, scope);
        collectStack(view, scope);
        collectMemory(view, scope);

        view.value(FLAGS, "ZF", scope.isZeroFlag() ? 1 : 0);
        view.value(FLAGS, "SF", scope.isSignFlag() ? 1 : 0);
        view.value(FLAGS, "OF", scope.isOverflowFlag() ? 1 : 0);
        view.value(FLAGS, "CF", scope.isCarryFlag() ? 1 : 0);
        view.value(FLAGS, "PF", scope.isParityFlag() ? 1 : 0);
    }

    /**
     * Writes the general, pointer and segment registers into the view.
     *
     * @param view The view that receives the state
     * @param scope The scope of the program
     */
    private void collectRegisters(StateView view, ASM8088Scope scope)
    {
        for (String[] register : GENERAL_REGISTERS)
        {
            view.group(REGISTERS, register[0]);
            view.value(REGISTERS, register[0] + "." + register[1], scope.getRegister8(register[1]));
            view.value(REGISTERS, register[0] + "." + register[2], scope.getRegister8(register[2]));
        }

        for (String register : POINTER_REGISTERS) {
            view.value(REGISTERS, register, scope.getRegister16(register));
        }

        for (String register : SEGMENT_REGISTERS) {
            view.value(REGISTERS, register, scope.getSegmentRegister(register));
        }
    }

    /**
     * Writes the most recent stack words into the view, from the oldest to the newest.
     * The words of a subroutine, from its return address down, go into a group named after its label.
     * The group of a nested call is inside the group of its caller.
     *
     * @param view The view that receives the state
     * @param scope The scope of the program
     */
    private void collectStack(StateView view, ASM8088Scope scope)
    {
        int sp = scope.getRegister16("SP");
        int entries = Math.min((STACK_TOP - sp) / 2, StateView.MAX_ENTRIES);

        // A frame stays visible while its return address is on the stack.
        List<CallFrame> frames = new ArrayList<>();

        for (CallFrame frame : getCallFrames())
        {
            if (frame.getReturnSlot() >= sp) {
                frames.add(frame);
            }
        }

        int frameIndex = 0;
        String path = null;

        for (int i = entries - 1; i >= 0; i--)
        {
            int offset = sp + i * 2;

            while (frameIndex < frames.size() && frames.get(frameIndex).getReturnSlot() >= offset)
            {
                String key = frames.get(frameIndex).getName() + "()";
                path = path == null ? key : path + "." + key;
                view.group(STACK, path);
                frameIndex++;
            }

            String entry = StateView.address(offset);
            view.value(STACK, path == null ? entry : path + "." + entry, scope.readWord(scope.getStackAddress(offset)));
        }
    }

    /**
     * Writes the non-zero bytes of the DATA and BSS segments into the view.
     * A byte inside the space of a data label is in a group with the label and is named by its offset,
     * for example "msg (0x1000)" with "msg+0" and "msg+1". A byte outside every label is named by its address.
     *
     * @param view The view that receives the state
     * @param scope The scope of the program
     */
    private void collectMemory(StateView view, ASM8088Scope scope)
    {
        byte[] memory = scope.getMemory();
        List<Map.Entry<String, Integer>> labels = new ArrayList<>();

        for (Map.Entry<String, Integer> label : scope.getDataLabelAddresses().entrySet())
        {
            if (label.getValue() >= DATA_START && label.getValue() < BSS_END) {
                labels.add(label);
            }
        }

        labels.sort(Map.Entry.comparingByValue());

        int dataEnd = scope.getDataSegmentStart() + scope.getCurrentDataOffset();
        int bssEnd = scope.getBssSegmentStart() + scope.getCurrentBssOffset();

        int entries = 0;
        int next = 0;
        String group = null;
        String name = null;
        int start = 0;
        int end = 0;

        for (int i = DATA_START; i < BSS_END && entries < StateView.MAX_MEMORY; i++)
        {
            while (next < labels.size() && labels.get(next).getValue() <= i)
            {
                // The space of a label ends at the next label or at the end of the data declared in its segment.
                name = labels.get(next).getKey();
                start = labels.get(next).getValue();
                end = start < BSS_START ? dataEnd : bssEnd;

                if (next + 1 < labels.size()) {
                    end = Math.min(end, labels.get(next + 1).getValue());
                }

                group = name + " (" + StateView.address(start) + ")";
                view.group(MEMORY, group);
                next++;
            }

            if (group != null && i >= end) {
                group = null;
            }

            if (memory[i] == 0) {
                continue;
            }

            int value = memory[i] & 0xFF;

            if (group == null) {
                view.value(MEMORY, StateView.address(i), value);
            }
            else {
                view.value(MEMORY, group + "." + name + "+" + (i - start), value);
            }

            entries++;
        }
    }

    @Override
    public String getScopeName()
    {
        return getScope().getName();
    }
}
