package xyz.atom7.api.parser.semantic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.Token;

import static xyz.atom7.Utils.escapeSpecialChars;

/**
 * Class for reporting semantic errors during semantic analysis.
 */
@Getter
@AllArgsConstructor
public class SemanticError
{
    private final String message;
    private final int line;
    private final int column;

    /**
     * Create a semantic error from an ANTLR token
     * 
     * @param message Error message
     * @param token ANTLR token where the error occurred
     */
    public SemanticError(String message, Token token)
    {
        this(escapeSpecialChars(message),
                token != null ? token.getLine() : -1,
                token != null ? token.getCharPositionInLine() : -1);
    }
    
    /**
     * Get a formatted error message with line and column information
     */
    public String getFormattedMessage()
    {
        if (line == -1 || column == -1) {
            return String.format("Semantic error - %s", message);
        }

        return String.format("Semantic error at line %d:%d - %s", line, column, message);
    }
    
    @Override
    public String toString()
    {
        return getFormattedMessage();
    }
} 