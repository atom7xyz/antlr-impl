package xyz.atom7.api.interpreter;

/**
 * Interface for programs that can be interpreted.
 * 
 * @param <T> The type of instruction handled by the program
 */
public interface Program<T extends Instruction>
{
    /**
     * Fetches the next instruction.
     * 
     * @return The next instruction
     */
    T fetch();

    /**
     * Executes the next instruction.
     */
    void execute();

    /**
     * Halts the program.
     */
    void halt();

    /**
     * Resumes the program.
     */
    void resume();

    /**
     * Returns whether the program is running.
     * 
     * @return True if the program is running, false otherwise
     */
    boolean isRunning();
}