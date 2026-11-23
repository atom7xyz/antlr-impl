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

    @Override
    public void collectState(StateView view)
    {
        getConstantPool().forEach((name, value) -> view.value(CONSTANTS, name, value));

        var frames = getCallStack();
        int firstFrame = Math.max(0, frames.size() - StateView.MAX_FRAMES);

        for (int i = firstFrame; i < frames.size(); i++)
        {
            IJVMScope frame = frames.get(i);
            String framePath = i + "_" + frame.getName();

            view.group(STACK, framePath);
            view.group(STACK, framePath + ".locals");
            frame.getLocals().forEach((name, value) -> view.value(STACK, framePath + ".locals." + name, value));
            view.group(STACK, framePath + ".stack");

            var stack = frame.getStack();
            int firstEntry = Math.max(0, stack.size() - StateView.MAX_ENTRIES);

            for (int j = firstEntry; j < stack.size(); j++) {
                view.value(STACK, framePath + ".stack.[" + j + "]", stack.get(j));
            }
        }
    }

    @Override
    public int getProgramCounter()
    {
        return getScope() == null ? -1 : getScope().getPc();
    }

    @Override
    public String getScopeName()
    {
        return getScope() == null ? null : getScope().getName();
    }
}
