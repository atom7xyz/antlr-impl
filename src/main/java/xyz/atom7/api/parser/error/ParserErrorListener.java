package xyz.atom7.api.parser.error;

import lombok.Getter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

import static xyz.atom7.Utils.escapeSpecialChars;

/**
 * Custom error listener that collects ANTLR parser errors as ParserError objects.
 */
@Getter
public class ParserErrorListener extends BaseErrorListener
{
    private final List<ParserError> errors = new ArrayList<>();
    
    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e)
    {
        String escapedMsg = escapeSpecialChars(msg);
        
        if (offendingSymbol instanceof Token) {
            Token token = (Token) offendingSymbol;
            errors.add(new ParserError(escapedMsg, token));
            return;
        }

        String offendingText = offendingSymbol != null ? escapeSpecialChars(offendingSymbol.toString()) : "<unknown>";
        errors.add(new ParserError(escapedMsg, line, charPositionInLine, offendingText));
    }
} 