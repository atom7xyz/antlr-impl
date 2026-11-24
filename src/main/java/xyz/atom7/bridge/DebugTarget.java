package xyz.atom7.bridge;

/**
 * Interface for interpreters that can be controlled by a DebugSession.
 */
interface DebugTarget
{
    /**
     * Initializes the interpreter with the given contents.
     *
     * @param contents The contents of the program
     */
    void init(String contents);

    /**
     * Executes the program until it ends.
     */
    void execute();

    /**
     * Returns the names of the memory contexts of the language.
     *
     * @return The context names, in display order
     */
    String[] getContexts();

    /**
     * Writes the visible state of the program into the view.
     *
     * @param view The view that receives the state
     */
    void collectState(StateView view);

    /**
     * Returns the name of the current scope.
     *
     * @return The scope name, or null if no scope is active
     */
    String getScopeName();
}
