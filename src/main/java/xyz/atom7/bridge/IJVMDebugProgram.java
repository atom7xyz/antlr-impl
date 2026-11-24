package xyz.atom7.bridge;

import xyz.atom7.interpreter.ijvm.IJVMInstruction;
import xyz.atom7.interpreter.ijvm.IJVMProgram;
import xyz.atom7.interpreter.ijvm.IJVMScope;

/**
 * IJVM interpreter controlled by a DebugSession.
 * The instructions are the same as the CLI. Only input, output and program exit go through the session.
 */
final class IJVMDebugProgram extends IJVMProgram<IJVMInstruction> implements DebugTarget
{
    private static final String CONSTANTS = "CONSTANTS";
    private static final String STACK = "STACK";

    private final DebugSession session;

    IJVMDebugProgram(DebugSession session)
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
    public void interpret(IJVMInstruction instruction)
    {
        if (!"label".equals(instruction.getOpCode())) {
            session.beforeInstruction(instruction);
        }

        super.interpret(instruction);
    }

    @Override
    public String[] getContexts()
    {
        return new String[] { CONSTANTS, STACK };
    }

    /**
     * Writes the constants and the call frames into the view.
     * Each frame contains its variables, then its operand stack by word address, then the frame of the method it called.
     *
     * @param view The view that receives the state
     */
    @Override
    public void collectState(StateView view)
    {
        getConstantPool().forEach((name, value) -> view.value(CONSTANTS, name, value));

        var frames = getCallStack();
        int firstFrame = Math.max(0, frames.size() - StateView.MAX_FRAMES);
        String framePath = null;

        for (int i = firstFrame; i < frames.size(); i++)
        {
            IJVMScope frame = frames.get(i);
            String parentPath = framePath;
            framePath = parentPath == null ? frameKey(frame) : parentPath + "." + frameKey(frame);

            String path = framePath;
            view.group(STACK, path);
            frame.getLocals().forEach((name, value) -> view.value(STACK, path + "." + name, value));

            var stack = frame.getStack();
            int firstEntry = Math.max(0, stack.size() - StateView.MAX_ENTRIES);

            for (int j = firstEntry; j < stack.size(); j++) {
                view.value(STACK, path + "." + StateView.address(frame.getStackAddress(j)), stack.get(j));
            }
        }
    }

    /**
     * Returns the key of a call frame. The parentheses keep it apart from a variable with the same name.
     *
     * @param frame The call frame
     * @return The key of the frame
     */
    private static String frameKey(IJVMScope frame)
    {
        return frame.getName() + "()";
    }

    @Override
    public String getScopeName()
    {
        return getScope() == null ? null : getScope().getName();
    }
}
