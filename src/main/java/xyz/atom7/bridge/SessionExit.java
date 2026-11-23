package xyz.atom7.bridge;

import lombok.Getter;

/**
 * Signals the end of a program without terminating the JVM.
 * The exception has no stack trace because it controls the execution flow.
 */
@Getter
final class SessionExit extends RuntimeException
{
    private final boolean stopped;
    private final int exitCode;

    private SessionExit(boolean stopped, int exitCode)
    {
        super(null, null, false, false);
        this.stopped = stopped;
        this.exitCode = exitCode;
    }

    /**
     * Creates the signal for a program that ended by itself.
     *
     * @param exitCode The exit code of the program
     * @return The exit signal
     */
    static SessionExit finished(int exitCode)
    {
        return new SessionExit(false, exitCode);
    }

    /**
     * Creates the signal for a program stopped by the user.
     *
     * @return The exit signal
     */
    static SessionExit stopped()
    {
        return new SessionExit(true, 0);
    }
}
