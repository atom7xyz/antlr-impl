package xyz.atom7.api.tracer;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.api.interpreter.Instruction;
import xyz.atom7.api.interpreter.Interpreter;

import java.util.Map;

public abstract class Tracer<I extends Interpreter<T>, T extends Instruction, S extends Snapshot<I, T>>
{
    protected final I interpreter;
    @Setter
    protected int step;

    @Nullable
    protected S previousSnapshot, currentSnapshot;

    public Tracer(I interpreter)
    {
        this.interpreter = interpreter;
    }

    protected abstract boolean compareSnapshots();
    public abstract void takeSnapshot();
    public abstract void displayTrace();

    @Getter
    protected enum ColorCode
    {
        RED("31"),
        GREEN("32"),
        YELLOW("33"),
        BLUE("34"),
        PURPLE("35"),
        CYAN("36"),
        MAGENTA("95"),
        BOLD_BLUE("1;34"),
        BOLD_GREEN("1;32");

        private final String code;

        ColorCode(String code)
        {
            this.code = code;
        }
    }

    /**
     * Print a colored label without a value
     * 
     * @param label The label to print
     * @param color The color to use
     */
    protected void printColoredNoValue(String label, ColorCode color)
    {
        System.out.println("\u001B[" + color.getCode() + "m" + label + " \u001B[0m");
    }

    /**
     * Print a colored label with a value
     * 
     * @param label The label to print
     * @param value The value to print
     * @param color The color to use
     */
    protected void printColored(String label, Object value, ColorCode color)
    {
        var valueDisplay = value == null ? "null" : value.toString();
        System.out.println("\u001B[" + color.getCode() + "m" + label + ": \u001B[0m" + valueDisplay);
    }

    /**
     * Format a map for display
     * 
     * @param map The map to format
     * @return The formatted map
     */
    protected String formatMap(Map<String, Integer> map)
    {
        if (map == null || map.isEmpty()) {
            return "empty";
        }

        return map.toString()
                .replace("{", "[")
                .replace("}", "]");
    }
}
