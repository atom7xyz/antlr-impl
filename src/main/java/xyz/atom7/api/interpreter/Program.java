package xyz.atom7.api.interpreter;

import xyz.atom7.api.parser.ParseResult;

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

    /**
     * Halts the program if there are errors.
     * 
     * @param result The parse result
     * @return True if the program has errors, false otherwise
     */
    default boolean haltIfErrors(ParseResult<?> result)
    {
        boolean errors = result.hasSyntacticErrors() || result.hasSemanticErrors();

        if (errors) {
            System.err.println("Program has errors, cannot interpret!");

            if (!result.getParserErrors().isEmpty()) {
                System.err.println(result.getParserErrors());
            }

            if (!result.getSemanticErrors().isEmpty()) {
                System.err.println(result.getSemanticErrors());
            }
        }

        return errors;
    }
}