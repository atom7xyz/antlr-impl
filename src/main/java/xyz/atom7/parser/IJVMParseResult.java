package xyz.atom7.parser;

import xyz.atom7.api.parser.ParseResult;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;

import java.util.List;

/**
 * Represents the result of parsing an IJVM program.
 */
public class IJVMParseResult extends ParseResult<IJVMParser.ProgramContext>
{
    /**
     * Constructor for IJVMParseResult
     * 
     * @param programContext The program context
     * @param parserErrors The parser errors
     * @param semanticErrors The semantic errors
     * @param semanticWarnings The semantic warnings
     */
    public IJVMParseResult(IJVMParser.ProgramContext programContext,
                           List<ParserError> parserErrors,
                           List<SemanticError> semanticErrors,
                           List<SemanticWarning> semanticWarnings)
    {
        super(programContext, parserErrors, semanticErrors, semanticWarnings);
    }
}
