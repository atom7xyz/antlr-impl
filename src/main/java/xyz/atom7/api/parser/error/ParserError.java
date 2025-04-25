package xyz.atom7.api.parser.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import xyz.atom7.parser.semantic.SemanticError;

import static xyz.atom7.Utils.escapeSpecialChars;

/**
 * Class for representing syntax/parser errors.
 * Similar to SemanticError but for parser-level errors.
 */
@Getter
@AllArgsConstructor
public class ParserError
{
    private final String message;
    private final int line;
    private final int column;
    private final String offendingToken;

    /**
     * Create a parser error from an ANTLR token
     * 
     * @param message Error message
     * @param token ANTLR token where the error occurred
     */
    public ParserError(String message, Token token)
    {
        this(message, token.getLine(), token.getCharPositionInLine(), escapeSpecialChars(token.getText()));
    }
    
    /**
     * Create a parser error from a RecognitionException
     * 
     * @param message Error message
     * @param e RecognitionException from ANTLR
     */
    public ParserError(String message, RecognitionException e)
    {
        this(message, e.getOffendingToken());
    }
    
    /**
     * Get a formatted error message with line and column information
     */
    public String getFormattedMessage()
    {
        String escapedMessage = escapeSpecialChars(message);
        return String.format("Syntax error at line %d:%d - %s (near token '%s')", 
                line, column, escapedMessage, offendingToken);
    }
    
    /**
     * Convert this parser error to a semantic error for uniform error handling
     */
    public SemanticError toSemanticError()
    {
        return new SemanticError(escapeSpecialChars(message), line, column);
    }
    
    @Override
    public String toString()
    {
        return getFormattedMessage();
    }
} 