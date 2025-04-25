package xyz.atom7.parser.semantic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.Token;

import static xyz.atom7.Utils.escapeSpecialChars;

/**
 * Class for reporting semantic errors during IJVM semantic analysis.
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
        this(escapeSpecialChars(message), token.getLine(), token.getCharPositionInLine());
    }
    
    /**
     * Get a formatted error message with line and column information
     */
    public String getFormattedMessage()
    {
        return String.format("Semantic error at line %d:%d - %s", line, column, message);
    }
    
    @Override
    public String toString()
    {
        return getFormattedMessage();
    }
} 