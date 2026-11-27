package xyz.atom7.bridge;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Changes of the visible state since the previous update.
 * Keys use the format CONTEXT:path and are joined by a newline, so a client reads them with one call.
 * The arrays changedKinds, changedValues and changedOrders follow the order of changedKeys.
 * changedOrders holds the position of each node among the current children of its parent.
 */
@Getter
@AllArgsConstructor
public final class StateDelta
{
    public static final byte KIND_GROUP = 0;
    public static final byte KIND_VALUE = 1;
    public static final byte KIND_UNSET = 2;

    static final StateDelta EMPTY = new StateDelta("", new byte[0], new int[0], new int[0], "");

    private final String changedKeys;
    private final byte[] changedKinds;
    private final int[] changedValues;
    private final int[] changedOrders;
    private final String removedKeys;
}
