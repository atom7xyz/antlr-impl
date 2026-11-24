package xyz.atom7.api.interpreter;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.ArrayDeque;
import java.util.Deque;
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

    /**
     * The largest character code that a program can read: IN and GETCHAR read one byte
     */
    public static final int MAX_INPUT_VALUE = 0xFF;

    /**
     * Characters of the last input line that the program did not read yet
     */
    private final Deque<Character> pendingInput = new ArrayDeque<>();
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
     * Reads the next character of input. Each line gives its characters in order, without the newline.
     * A line with a character code greater than MAX_INPUT_VALUE is skipped.
     *
     * @return The character as a string of length 1, or null if no input is available
     */
    protected String readInput()
    {
        while (pendingInput.isEmpty() && scanner.hasNextLine())
        {
            String line = scanner.nextLine();

            if (line.chars().anyMatch(c -> c > MAX_INPUT_VALUE)) {
                System.err.println("Input not accepted: every character must have a code from 0 to " + MAX_INPUT_VALUE);
                continue;
            }

            for (int i = 0; i < line.length(); i++) {
                pendingInput.add(line.charAt(i));
            }
        }

        return pendingInput.isEmpty() ? null : String.valueOf(pendingInput.poll());
    }

    /**
     * Writes program output.
     *
     * @param text The text to write
     */
    protected void writeOutput(String text)
    {
        System.out.print(text);
    }

    /**
     * Ends the program.
     *
     * @param exitCode The exit code of the program
     */
    protected void exitProgram(int exitCode)
    {
        System.exit(exitCode);
    }

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
