package xyz.atom7.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import xyz.atom7.api.parser.ParserHelper;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.api.parser.error.ParserErrorListener;
import xyz.atom7.parser.semantic.SemanticAnalyzer;
import xyz.atom7.parser.semantic.SemanticError;
import xyz.atom7.parser.semantic.SemanticWarning;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for parsing IJVM source files.
 */
public class IJVMParserHelper extends ParserHelper<IJVMParseResult>
{
    /**
     * Parse an IJVM code from a CharStream.
     *
     * @param input CharStream containing IJVM code
     * @return The parsed program result containing the tree and any errors
     */
    @Override
    protected IJVMParseResult parseStream(CharStream input)
    {
        // Create a lexer with our custom error listener
        ParserErrorListener lexerErrorListener = new ParserErrorListener();
        IJVMLexer lexer = new IJVMLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrorListener);
        
        // Create a token stream from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        // Create a parser with our custom error listener
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        IJVMParser parser = new IJVMParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrorListener);
        
        // Parse the program
        IJVMParser.ProgramContext programContext = parser.program();
        
        // Collect parser errors
        List<ParserError> parserErrors = new ArrayList<>();
        parserErrors.addAll(lexerErrorListener.getErrors());
        parserErrors.addAll(parserErrorListener.getErrors());

        List<SemanticError> semanticErrors = new ArrayList<>();
        List<SemanticWarning> semanticWarnings = new ArrayList<>();
        
        // If there are parser errors, don't proceed to semantic analysis
        if (!parserErrors.isEmpty()) {
            return new IJVMParseResult(programContext, parserErrors, semanticErrors, semanticWarnings);
        }
        
        // Perform semantic analysis
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(programContext);

        semanticErrors.addAll(semanticAnalyzer.getErrors());
        semanticWarnings.addAll(semanticAnalyzer.getWarnings());

        return new IJVMParseResult(programContext, parserErrors, semanticErrors, semanticWarnings);
    }
}