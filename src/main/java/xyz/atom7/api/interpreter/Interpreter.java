package xyz.atom7.api.interpreter;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Abstract class for interpreters that handle a specific type of instruction.
 * 
 * @param <T> The type of instruction handled by the interpreter
 */
@Getter
public abstract class Interpreter<T extends Instruction> implements Program<T>
{
    protected final Map<String, Consumer<T>> instructionHandlers;

    protected AtomicBoolean running;
    protected List<T> byteCode;

    @Setter
    protected int pc;

    public Interpreter()
    {
        instructionHandlers = new HashMap<>();
        running = new AtomicBoolean(false);
        byteCode = new ArrayList<>();
        pc = 0;
    }

    /**
     * Adds an instruction handler for a specific operation code.
     * 
     * @param opCode The operation code to handle
     * @param handler The handler to execute when the operation code is encountered
     */
    public void addInstruction(String opCode, Consumer<T> handler)
    {
        instructionHandlers.put(opCode, handler);
    }

    /**
     * Initializes the program with the given contents.
     * 
     * @param contents The contents of the program
     */
    protected abstract void initProgram(String contents);

    /**
     * Initializes the instructions for the program.
     */
    protected abstract void initInstructions();

    /**
     * Interprets an instruction.
     * 
     * @param instruction The instruction to interpret
     */
    public abstract void interpret(T instruction);

    /**
     * Interprets an instruction.
     */
    @Override
    public T fetch()
    {
        return byteCode.get(pc++);
    }

    /**
     * Halts the program.
     */
    @Override
    public void halt()
    {
        running.set(false);
    }

    /**
     * Resumes the program.
     */
    @Override
    public void resume()
    {
        running.set(true);
    }

    /**
     * Returns whether the program is running.
     * 
     * @return True if the program is running, false otherwise
     */
    public boolean isRunning()
    {
        return running.get();
    }

    /**
     * Returns the size of the byte code.
     * 
     * @return The size of the byte code
     */
    public int getByteCodeSize()
    {
        return byteCode.size();
    }

    @Override
    public String toString()
    {
        return "Interpreter{" +
                "instructionHandlers=" + instructionHandlers +
                ", byteCode=" + byteCode +
                ", pc=" + pc +
                ", running=" + running +
                '}';
    }
}
