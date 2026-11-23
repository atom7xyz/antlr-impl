package xyz.atom7.bridge;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.api.interpreter.Instruction;
import xyz.atom7.api.parser.ParseResult;
import xyz.atom7.api.parser.ParserHelper;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.parser.asm8088.ASM8088ParserHelper;
import xyz.atom7.parser.ijvm.IJVMParserHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs one program on a dedicated thread and exposes it to an external client.
 * The client controls the program with step, feed and stop, and reads the changes with awaitUpdate.
 */
public final class DebugSession
{
    /**
     * Execution states of a session.
     */
    public enum State
    {
        CREATED, RUNNING, PAUSED, WAITING_INPUT, FINISHED, STOPPED, FAILED;

        boolean isTerminal()
        {
            return this == FINISHED || this == STOPPED || this == FAILED;
        }
    }

    private static final int MAX_OUTPUT = 8 * 1024;
    private static final int MAX_PENDING_INPUTS = 4096;
    private static final long FRAME_MILLIS = 16;

    private final String source;
    private final ParserHelper<?> parser;
    private final DebugTarget target;
    private final StateView view = new StateView();

    private final StringBuilder output = new StringBuilder();
    private final Deque<String> inputs = new ArrayDeque<>();

    private volatile boolean stopRequested;
    private boolean gated;
    private boolean initialized;
    private boolean dirty;
    private long stepBudget;

    private State state = State.CREATED;
    private int exitCode;

    @Nullable
    private String message;

    @Nullable
    private String warnings;

    @Nullable
    private Instruction currentInstruction;

    private DebugSession(String language, String contents)
    {
        source = contents.endsWith("\n") ? contents : contents + "\n";

        switch (language) {
            case "ijvm":
                parser = new IJVMParserHelper();
                target = new IJVMDebugProgram(this);
                break;
            case "8088":
                parser = new ASM8088ParserHelper();
                target = new ASM8088DebugProgram(this);
                break;
            default:
                throw new IllegalArgumentException("Unknown language: " + language);
        }
    }

    /**
     * Creates a session for a program.
     *
     * @param language The language of the program, ijvm or 8088
     * @param contents The contents of the program
     * @return The new session
     */
    public static DebugSession create(String language, String contents)
    {
        return new DebugSession(language, contents);
    }

    /**
     * Returns the names of the memory contexts of the language.
     *
     * @return The context names, in display order
     */
    public String[] getContexts()
    {
        return target.getContexts();
    }

    /**
     * Starts the program on its own thread.
     *
     * @param debug True to pause before the first instruction, false to run without pauses
     */
    public synchronized void start(boolean debug)
    {
        if (state != State.CREATED) {
            throw new IllegalStateException("Session already started");
        }

        gated = debug;
        state = State.RUNNING;

        Thread thread = new Thread(this::run, "assembly-ui-interpreter");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Executes a number of instructions of a paused program.
     *
     * @param count The number of instructions to execute
     * @return True if the program accepted the request, false otherwise
     */
    public synchronized boolean step(int count)
    {
        if (state != State.PAUSED || count <= 0) {
            return false;
        }

        stepBudget += count;
        notifyAll();
        return true;
    }

    /**
     * Queues a line of input for the program.
     *
     * @param line The line of input
     * @return True if the session accepted the line, false otherwise
     */
    public synchronized boolean feed(String line)
    {
        if (state.isTerminal() || line.isEmpty() || inputs.size() >= MAX_PENDING_INPUTS) {
            return false;
        }

        inputs.add(line);
        notifyAll();
        return true;
    }

    /**
     * Requests the end of the program.
     */
    public synchronized void stop()
    {
        stopRequested = true;

        if (state == State.CREATED) {
            finish(State.STOPPED, 0, null);
        }

        notifyAll();
    }

    /**
     * Waits for a change of the program.
     * While the program runs, the method groups the output of one frame into one update.
     *
     * @param timeoutMillis The maximum wait in milliseconds
     * @return The update, or null if nothing changed
     * @throws InterruptedException If the thread is interrupted
     */
    public synchronized StateUpdate awaitUpdate(long timeoutMillis) throws InterruptedException
    {
        if (!dirty) {
            wait(timeoutMillis);
        }

        if (!dirty) {
            return null;
        }

        if (state == State.RUNNING) {
            wait(FRAME_MILLIS);
        }

        return buildUpdate();
    }

    /**
     * Checks the session before an instruction and pauses the program when required.
     *
     * @param instruction The instruction about to be executed
     */
    void beforeInstruction(Instruction instruction)
    {
        currentInstruction = instruction;

        if (stopRequested) {
            throw SessionExit.stopped();
        }

        if (gated) {
            awaitStep();
        }
    }

    /**
     * Waits for a line of input.
     *
     * @return The next line of input
     */
    @SneakyThrows
    synchronized String awaitInput()
    {
        while (inputs.isEmpty())
        {
            exitIfStopped();
            settle(State.WAITING_INPUT);
            wait();
        }

        exitIfStopped();
        state = State.RUNNING;
        return inputs.poll();
    }

    /**
     * Appends program output and waits while the output buffer is full.
     *
     * @param text The text to append
     */
    @SneakyThrows
    synchronized void writeOutput(String text)
    {
        int offset = 0;

        while (offset < text.length())
        {
            while (output.length() >= MAX_OUTPUT)
            {
                exitIfStopped();
                wait();
            }

            int end = Math.min(text.length(), offset + MAX_OUTPUT - output.length());
            output.append(text, offset, end);
            offset = end;

            if (!dirty) {
                dirty = true;
                notifyAll();
            }
        }
    }

    @SneakyThrows
    private synchronized void awaitStep()
    {
        while (stepBudget == 0)
        {
            exitIfStopped();
            settle(State.PAUSED);
            wait();
        }

        exitIfStopped();
        stepBudget--;
        state = State.RUNNING;
    }

    private void exitIfStopped()
    {
        if (stopRequested) {
            throw SessionExit.stopped();
        }
    }

    private void settle(State newState)
    {
        if (state != newState) {
            state = newState;
            dirty = true;
            notifyAll();
        }
    }

    /**
     * Parses and executes the program, then publishes its final state.
     */
    private void run()
    {
        try {
            if (!parse()) {
                return;
            }

            target.init(source);
            markInitialized();
            target.execute();
            finish(stopRequested ? State.STOPPED : State.FINISHED, 0, null);
        }
        catch (SessionExit exit) {
            finish(exit.isStopped() ? State.STOPPED : State.FINISHED, exit.getExitCode(), null);
        }
        catch (Throwable error) {
            finish(State.FAILED, 1, describe(error));
        }
    }

    private synchronized void markInitialized()
    {
        initialized = true;
    }

    /**
     * Parses the program and stores its diagnostics.
     *
     * @return True if the program has no errors, false otherwise
     */
    private boolean parse()
    {
        ParseResult<?> result = parser.parseString(source);

        String warningText = result.getSemanticWarnings().stream()
                .map(SemanticWarning::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        synchronized (this) {
            warnings = warningText.isEmpty() ? null : warningText;
        }

        if (!result.hasErrors()) {
            return true;
        }

        List<String> errors = new ArrayList<>();

        for (ParserError error : result.getParserErrors()) {
            errors.add(error.getFormattedMessage());
        }

        for (SemanticError error : result.getSemanticErrors()) {
            errors.add(error.getFormattedMessage());
        }

        finish(State.FAILED, 1, String.join("\n", errors));
        return false;
    }

    private synchronized void finish(State end, int code, @Nullable String text)
    {
        state = end;
        exitCode = code;
        message = text;
        dirty = true;
        notifyAll();
    }

    private String describe(Throwable error)
    {
        String instruction = currentInstruction == null ? "none" : currentInstruction.toString();
        return String.format("Runtime error at pc %d (%s): %s",
                target.getProgramCounter(), instruction, error);
    }

    /**
     * Builds the payload and clears the pending changes.
     * The state of the program is read only while its thread is parked or ended.
     *
     * @return The update for the client
     */
    private StateUpdate buildUpdate()
    {
        dirty = false;

        String text = output.toString();
        output.setLength(0);
        notifyAll();

        StateDelta delta = StateDelta.EMPTY;
        int pc = -1;
        String instruction = null;
        String scope = null;

        if (state != State.RUNNING && initialized) {
            target.collectState(view);
            delta = view.publish();
            pc = target.getProgramCounter();
            instruction = currentInstruction == null ? null : currentInstruction.toString();
            scope = target.getScopeName();
        }

        String pendingWarnings = warnings;
        warnings = null;

        return new StateUpdate(state.name(), state.isTerminal(), exitCode, message, pendingWarnings,
                pc, instruction, scope, text, delta);
    }
}
