package xyz.atom7.api.parser.semantic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.Token;

import static xyz.atom7.Utils.escapeSpecialChars;

/**
 * Class for reporting semantic warnings during semantic analysis.
 * These are potential issues that may not prevent execution but could cause problems.
 */
@Getter
@AllArgsConstructor
public class SemanticWarning
{
    private final String message;
    private final int line;
    private final int column;

    /**
     * Create a semantic warning from an ANTLR token
     * 
     * @param message Warning message
     * @param token ANTLR token where the warning occurred
     */
    public SemanticWarning(String message, Token token)
    {
        this(escapeSpecialChars(message),
                token != null ? token.getLine() : -1,
                token != null ? token.getCharPositionInLine() : -1);
    }
    
    /**
     * Convert a semantic error to a warning with the same message and location
     * 
     * @param error The semantic error to convert
     */
    public SemanticWarning(SemanticError error)
    {
        this(error.getMessage(), error.getLine(), error.getColumn());
    }
    
    /**
     * Get a formatted warning message with line and column information
     */
    public String getFormattedMessage()
    {
        if (line == -1 || column == -1) {
            return String.format("Semantic warning - %s", message);
        }

        return String.format("Semantic warning at line %d:%d - %s", line, column, message);
    }
    
    @Override
    public String toString()
    {
        return getFormattedMessage();
    }
} 