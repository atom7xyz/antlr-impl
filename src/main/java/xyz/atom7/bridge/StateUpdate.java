package xyz.atom7.bridge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Payload sent by a DebugSession to its client.
 */
@Getter
@AllArgsConstructor
public final class StateUpdate
{
    private final String state;
    private final boolean terminal;
    private final int exitCode;

    @Nullable
    private final String message;

    @Nullable
    private final String warnings;

    private final int programCounter;

    /**
     * The source line of the current instruction, starting from 1, or -1 if unknown
     */
    private final int line;

    @Nullable
    private final String instruction;

    @Nullable
    private final String scope;

    private final String output;
    private final StateDelta delta;
}
