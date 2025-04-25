package xyz.atom7.api.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.parser.semantic.SemanticError;
import xyz.atom7.parser.semantic.SemanticWarning;

import java.util.List;

@Getter
@AllArgsConstructor
public class ParseResult<T extends ParserRuleContext>
{
    private final T programContext;
    private final List<ParserError> parserErrors;
    private final List<SemanticError> semanticErrors;
    private final List<SemanticWarning> semanticWarnings;

    /**
     * Check if there are any errors (parser or semantic)
     */
    public boolean hasErrors()
    {
        return hasSyntacticErrors() || hasSemanticErrors();
    }

    /**
     * Check if there are any warnings (semantic)
     */
    public boolean hasWarnings()
    {
        return hasSemanticWarnings();
    }

    /**
     * Check if there are any semantic warnings
     */
    public boolean hasSemanticWarnings()
    {
        return !semanticWarnings.isEmpty();
    }

    /**
     * Check if there are any semantic errors
     */
    public boolean hasSemanticErrors()
    {
        return !semanticErrors.isEmpty();
    }

    /**
     * Check if there are any syntactic errors
     */
    public boolean hasSyntacticErrors()
    {
        return !parserErrors.isEmpty();
    }
}
