package xyz.atom7.bridge;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visible state of a program.
 * The view keeps the last published state and computes only the changes.
 */
final class StateView
{
    static final int MAX_ENTRIES = 64;
    static final int MAX_FRAMES = 16;
    static final int MAX_MEMORY = 128;

    private static final long GROUP = Long.MIN_VALUE;
    private static final long UNSET = Long.MAX_VALUE;

    private Map<String, Long> published = new LinkedHashMap<>();
    private Map<String, Long> current = new LinkedHashMap<>();

    /**
     * Adds a node that contains other nodes.
     *
     * @param context The name of the context
     * @param path The path of the node
     */
    void group(String context, String path)
    {
        current.put(key(context, path), GROUP);
    }

    /**
     * Adds a node with a value.
     *
     * @param context The name of the context
     * @param path The path of the node
     * @param value The value of the node, or null if the value is not set
     */
    void value(String context, String path, @Nullable Integer value)
    {
        current.put(key(context, path), value == null ? UNSET : value.longValue());
    }

    /**
     * Computes the changes since the last call and publishes the current state.
     *
     * @return The changes of the state
     */
    StateDelta publish()
    {
        List<String> changed = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        Map<String, Integer> childCounts = new HashMap<>();

        for (Map.Entry<String, Long> entry : current.entrySet())
        {
            int order = childCounts.merge(parentOf(entry.getKey()), 1, Integer::sum) - 1;

            if (!entry.getValue().equals(published.get(entry.getKey()))) {
                changed.add(entry.getKey());
                values.add(entry.getValue());
                orders.add(order);
            }
        }

        List<String> removed = new ArrayList<>();

        for (String key : published.keySet())
        {
            if (!current.containsKey(key) && !isParentRemoved(key)) {
                removed.add(key);
            }
        }

        Map<String, Long> recycled = published;
        published = current;
        current = recycled;
        current.clear();

        if (changed.isEmpty() && removed.isEmpty()) {
            return StateDelta.EMPTY;
        }

        byte[] kinds = new byte[values.size()];
        int[] numbers = new int[values.size()];
        int[] positions = new int[values.size()];

        for (int i = 0; i < kinds.length; i++)
        {
            long value = values.get(i);

            kinds[i] = value == GROUP ? StateDelta.KIND_GROUP :
                    value == UNSET ? StateDelta.KIND_UNSET : StateDelta.KIND_VALUE;
            numbers[i] = kinds[i] == StateDelta.KIND_VALUE ? (int) value : 0;
            positions[i] = orders.get(i);
        }

        return new StateDelta(String.join("\n", changed), kinds, numbers, positions, String.join("\n", removed));
    }

    /**
     * Checks if the parent of a removed node is also removed.
     *
     * @param key The key of the node
     * @return True if the parent is removed, false otherwise
     */
    private boolean isParentRemoved(String key)
    {
        int separator = key.lastIndexOf('.');

        if (separator < 0) {
            return false;
        }

        String parent = key.substring(0, separator);
        return published.containsKey(parent) && !current.containsKey(parent);
    }

    /**
     * Returns the key of the parent of a node: the parent path, or the context for a top-level node.
     *
     * @param key The key of the node
     * @return The key of the parent
     */
    private static String parentOf(String key)
    {
        int separator = key.lastIndexOf('.');
        return separator < 0 ? key.substring(0, key.indexOf(':')) : key.substring(0, separator);
    }

    /**
     * Formats a memory address as a node key.
     *
     * @param address The address
     * @return The address in hexadecimal, for example 0x8000
     */
    static String address(int address)
    {
        return String.format("0x%04X", address);
    }

    private static String key(String context, String path)
    {
        return context + ":" + path;
    }
}
