package xyz.atom7.api.interpreter;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Abstract class for interpreters that handle a specific type of instruction.
 * 
 * @param <T> The type of instruction handled by the interpreter
 */
@Getter
public abstract class Interpreter<T extends Instruction> implements Program
{
    protected final Map<String, Consumer<T>> instructionHandlers;

    @Setter
    protected int pc;

    protected final Scanner scanner;
    protected Semaphore running;

    public Interpreter()
    {
        scanner = new Scanner(System.in);
        instructionHandlers = new HashMap<>();
        running = new Semaphore(0);
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
     * Initializes the interpreter with the given contents.
     * 
     * @param contents The contents of the program
     */
    public void init(String contents)
    {
        initInstructions();
        initProgram(contents);
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
     * Halts the program.
     */
    @SneakyThrows
    @Override
    public void halt()
    {
        running.acquire();
    }

    /**
     * Resumes the program.
     */
    @Override
    public void resume()
    {
        running.release();
    }

    /**
     * Returns whether the program is running.
     * 
     * @return True if the program is running, false otherwise
     */
    @Override
    public boolean isRunning()
    {
        return running.availablePermits() > 0;
    }

    @Override
    public String toString()
    {
        return "Interpreter{" +
                "instructionHandlers=" + instructionHandlers +
                ", pc=" + pc +
                ", running=" + running +
                '}';
    }
}
