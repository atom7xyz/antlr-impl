package xyz.atom7.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import xyz.atom7.api.parser.ParserHelper;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.error.ParserErrorListener;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.parser.semantic.ASM8088SemanticAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for parsing 8088 assembly source files.
 */
public class ASM8088ParserHelper extends ParserHelper<ASM8088ParseResult>
{
    /**
     * Parse an 8088 assembly code from a CharStream.
     *
     * @param input CharStream containing 8088 assembly code
     * @return The parsed program result containing the tree and any errors
     */
    @Override
    protected ASM8088ParseResult parseStream(CharStream input)
    {
        // Create a lexer with our custom error listener
        ParserErrorListener lexerErrorListener = new ParserErrorListener();
        asm8088Lexer lexer = new asm8088Lexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrorListener);
        
        // Create a token stream from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        // Create a parser with our custom error listener
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        asm8088Parser parser = new asm8088Parser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrorListener);
        
        // Parse the program
        asm8088Parser.ProgramContext programContext = parser.program();
        
        // Collect parser errors
        List<ParserError> parserErrors = new ArrayList<>();
        parserErrors.addAll(lexerErrorListener.getErrors());
        parserErrors.addAll(parserErrorListener.getErrors());

        List<SemanticError> semanticErrors = new ArrayList<>();
        List<SemanticWarning> semanticWarnings = new ArrayList<>();
        
        // If there are parser errors, don't proceed to semantic analysis
        if (!parserErrors.isEmpty()) {
            return new ASM8088ParseResult(programContext, parserErrors, semanticErrors, semanticWarnings);
        }
        
        // Perform semantic analysis
        ASM8088SemanticAnalyzer semanticAnalyzer = new ASM8088SemanticAnalyzer();
        semanticAnalyzer.analyze(programContext);

        semanticErrors.addAll(semanticAnalyzer.getErrors());
        semanticWarnings.addAll(semanticAnalyzer.getWarnings());

        return new ASM8088ParseResult(programContext, parserErrors, semanticErrors, semanticWarnings);
    }
} 