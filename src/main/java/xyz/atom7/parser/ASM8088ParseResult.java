package xyz.atom7.parser;

import xyz.atom7.api.parser.ParseResult;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;

import java.util.List;

/**
 * Represents the result of parsing an 8088 assembly program.
 */
public class ASM8088ParseResult extends ParseResult<asm8088Parser.ProgramContext>
{
    /**
     * Constructor for ASM8088ParseResult
     * 
     * @param programContext The program context
     * @param parserErrors The parser errors
     * @param semanticErrors The semantic errors
     * @param semanticWarnings The semantic warnings
     */
    public ASM8088ParseResult(asm8088Parser.ProgramContext programContext,
                             List<ParserError> parserErrors,
                             List<SemanticError> semanticErrors,
                             List<SemanticWarning> semanticWarnings)
    {
        super(programContext, parserErrors, semanticErrors, semanticWarnings);
    }
} 