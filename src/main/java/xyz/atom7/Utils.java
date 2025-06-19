package xyz.atom7;

import org.jetbrains.annotations.Nullable;
import xyz.atom7.api.tracer.Tracer;

public class Utils
{
    public static boolean DEBUG = false;
    public static Tracer<?, ?, ?> TRACER;

    /**
     * Escapes special characters in strings to display them as literals in error messages
     *
     * @param text The text to escape
     * @return The escaped text
     */
    public static String escapeSpecialChars(String text)
    {
        if (text == null) {
            return "<null>";
        }

        return text.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Joins an array of strings with a newline character
     * 
     * @param code The array of strings to join
     * @return The joined string
     */
    public static String codeWritten(String ...code)
    {
        StringBuilder result = new StringBuilder();

        for (String string : code) {
            result.append(string).append("\n");
        }

        return result.toString();
    }

    /**
     * Checks if a string is a numeric value
     * 
     * @param input The string to check
     * @return True if the string is a numeric value, false otherwise
     */
    public static boolean isNumeric(String input)
    {
        try {
            Integer.parseInt(input);
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Parses an object to an integer
     * 
     * @param input The object to parse
     * @return The parsed integer, or null if the input is null
     */
    public static Integer parseInt(@Nullable Object input)
    {
        if (input == null) {
            return null;
        }

        if (input instanceof String) {
            String str = (String) input;

            int result;

            if (str.length() == 1 && !isNumeric(str)) {
                result = str.charAt(0);
            }
            else if (str.startsWith("0x") || str.startsWith("0X")) {
                result = Integer.parseInt(str.substring(2), 16);
            }
            else if (str.startsWith("o") || str.startsWith("O")) {
                result = Integer.parseInt(str.substring(1), 8);
            }
            else {
                result = Integer.parseInt(str);
            }

            return result;
        }

        if (input instanceof Character) {
            return (int) (char) input;
        }

        throw new IllegalArgumentException();
    }

    /**
     * Prints a message to the console if debugging is enabled
     * 
     * @param message The message to print
     */
    public static void debugln(String message)
    {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    /**
     * Prints a message to the console if debugging is enabled
     * 
     * @param message The message to print
     */
    public static void debug(String message)
    {
        if (DEBUG) {
            System.out.print(message);
        }
    }
}
