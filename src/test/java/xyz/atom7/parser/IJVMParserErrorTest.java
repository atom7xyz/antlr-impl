package xyz.atom7.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.parser.ijvm.IJVMParseResult;
import xyz.atom7.parser.ijvm.IJVMParserHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.atom7.Utils.codeWritten;

/**
 * Tests for the IJVM parser syntax error detection.
 */
public class IJVMParserErrorTest
{
    private IJVMParserHelper parserHelper;

    @BeforeEach
    void setUp()
    {
        parserHelper = new IJVMParserHelper();
    }
    
    /**
     * Test valid IJVM code with no parser errors
     */
    @Test
    @DisplayName("Valid program should have no parser errors")
    public void testValidProgram() {
        String validCode = codeWritten(
                ".constant",
                "OBJREF 10",
                ".end-constant",
                "",
                ".main",
                ".var",
                "x",
                "y",
                ".end-var",
                "BIPUSH 5",
                "ISTORE x",
                "BIPUSH 10",
                "ISTORE y",
                "ILOAD x",
                "ILOAD y",
                "IADD",
                "OUT",
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid program should have no parser errors");
    }
    
    /**
     * Test missing .end-constant block
     */
    @Test
    @DisplayName("Should detect missing .end-constant")
    public void testMissingEndConstant() {
        String invalidCode = codeWritten(
                ".constant",
                "OBJREF 10",
                // Missing .end-constant
                ".main",
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing .end-constant");
    }
    
    /**
     * Test missing .end-main block
     */
    @Test
    @DisplayName("Should detect missing .end-main")
    public void testMissingEndMain() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 5"
                // Missing .end-main
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing .end-main");
    }
    
    /**
     * Test invalid instruction
     */
    @Test
    @DisplayName("Should detect invalid instruction")
    public void testInvalidInstruction() {
        String invalidCode = codeWritten(
                ".main",
                "INVALID_INSTRUCTION", // Invalid instruction
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid instruction");
    }
    
    /**
     * Test missing argument for instruction that requires it
     */
    @Test
    @DisplayName("Should detect missing argument for BIPUSH")
    public void testMissingArgumentForInstruction() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH", // Missing argument for BIPUSH
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing argument for BIPUSH");
    }
    
    /**
     * Test invalid method declaration syntax
     */
    @Test
    @DisplayName("Should detect invalid method declaration syntax")
    public void testInvalidMethodDeclaration() {
        String invalidCode = codeWritten(
                ".method add a, b", // Missing parentheses
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid method declaration syntax");
    }
    
    /**
     * Test unclosed parentheses in method declaration
     */
    @Test
    @DisplayName("Should detect unclosed parentheses in method declaration")
    public void testUnclosedParentheses() {
        String invalidCode = codeWritten(
                ".method add(a, b", // Missing closing parenthesis
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect unclosed parentheses in method declaration");
    }
    
    /**
     * Test invalid parameter separator
     */
    @Test
    @DisplayName("Should detect invalid parameter separator")
    public void testInvalidParameterSeparator() {
        String invalidCode = codeWritten(
                ".method add(a; b)", // Using semicolon instead of comma
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid parameter separator");
    }
    
    /**
     * Test missing label for jump instruction
     */
    @Test
    @DisplayName("Should detect missing label for jump instruction")
    public void testMissingLabelForJump() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 5",
                "IFEQ", // Missing label
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing label for jump instruction");
    }
    
    /**
     * Test invalid constant number format
     */
    @Test
    @DisplayName("Should detect invalid constant number format")
    public void testInvalidConstantNumberFormat() {
        String invalidCode = codeWritten(
                ".constant",
                "OBJREF 0xZG", // Invalid hex format
                ".end-constant",
                ".main",
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid constant number format");
    }
    
    /**
     * Test missing colon in label declaration
     */
    @Test
    @DisplayName("Should detect missing colon in label declaration")
    public void testMissingColonInLabel() {
        String invalidCode = codeWritten(
                ".main",
                "start", // Missing colon after label
                "BIPUSH 5",
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing colon in label declaration");
    }
    
    /**
     * Test nested blocks (not allowed)
     */
    @Test
    @DisplayName("Should detect nested blocks")
    public void testNestedBlocks() {
        String invalidCode = codeWritten(
                ".main",
                ".method inner()", // Nesting .method inside .main
                "IRETURN",
                ".end-method",
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect nested blocks");
    }
    
    /**
     * Test multiple error detection
     */
    @Test
    @DisplayName("Should detect multiple errors")
    public void testMultipleErrors() {
        String invalidCode = codeWritten(
                ".main",
                "INVALID_INSTR", // First error
                "BIPUSH",        // Second error
                ".end-main"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertTrue(errors.size() >= 2, "Should detect multiple errors");
    }

    @Test
    @DisplayName("Valid programs should have no errors")
    public void testAllExampleFiles() throws IOException
    {
        URL resourceUrl = getClass()
                .getClassLoader()
                .getResource("examples/ijvm");
        assertNotNull(resourceUrl, "Examples directory not found in resources");

        File dir = new File(resourceUrl.getFile());

        var list = dir.listFiles((dir1, name) -> name.endsWith(".jas"));
        assertNotNull(list);

        for (File file : list)
        {
            List<ParserError> errors = analyzeCodeFromPath(file.getAbsolutePath());
            assertTrue(errors.isEmpty(), "Valid example program should have no parser errors");
        }
    }

    private List<ParserError> analyzeCode(String code)
    {
        IJVMParseResult result = parserHelper.parseString(code);
        return result.getParserErrors();
    }

    private List<ParserError> analyzeCodeFromPath(String filePath) throws IOException
    {
        IJVMParseResult result = parserHelper.parseFile(filePath);
        return result.getParserErrors();
    }
} 