package xyz.atom7.api.interpreter;

/**
 * Interface for programs that can be interpreted.
 */
public interface Program
{
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