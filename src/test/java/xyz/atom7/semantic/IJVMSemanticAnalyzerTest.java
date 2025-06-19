package xyz.atom7.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.parser.ijvm.IJVMParseResult;
import xyz.atom7.parser.ijvm.IJVMParserHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.atom7.Utils.codeWritten;

/**
 * Tests for the IJVM semantic analyzer.
 */
public class IJVMSemanticAnalyzerTest
{
    private IJVMParserHelper parserHelper;

    @BeforeEach
    void setUp()
    {
        parserHelper = new IJVMParserHelper();
    }

    /**
     * Test valid IJVM code with no semantic errors
     */
    @Test
    @DisplayName("Valid program should have no semantic errors")
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
                ".end-main",
                "",
                ".method add(a, b)",
                "ILOAD a",
                "ILOAD b",
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<SemanticError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid program should have no semantic errors");
    }
    
    /**
     * Test duplicate variable declarations
     */
    @Test
    @DisplayName("Should detect duplicate variable declaration")
    public void testDuplicateVariableError() {
        String invalidCode = codeWritten(
                ".main",
                ".var",
                "x",
                "x", // Duplicate variable
                ".end-var",
                "BIPUSH 5",
                "ISTORE x",
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect duplicate variable declaration");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("already defined")),
                "Error should mention variable is already defined");
    }
    
    /**
     * Test undeclared variable usage
     */
    @Test
    @DisplayName("Should detect undeclared variable")
    public void testUndeclaredVariableError() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 5",
                "ISTORE x", // x is not declared
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect undeclared variable");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("not defined")),
                "Error should mention variable is not defined");
    }
    
    /**
     * Test uninitialized variable usage
     */
    @Test
    @DisplayName("Should detect uninitialized variable usage")
    public void testUninitializedVariableError() {
        String invalidCode = codeWritten(
                ".main",
                ".var",
                "x",
                ".end-var",
                "ILOAD x", // x is not initialized
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect uninitialized variable usage");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("not have been initialized")),
                "Error should mention variable may not be initialized");
    }
    
    /**
     * Test undeclared method invocation
     */
    @Test
    @DisplayName("Should detect undeclared method")
    public void testUndeclaredMethodError() {
        String invalidCode = codeWritten(
                ".constant",
                "OBJREF 10",
                ".end-constant",
                "",
                ".main",
                "LDC_W OBJREF",
                "INVOKEVIRTUAL undeclaredMethod", // Method not declared
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect undeclared method");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("not defined")),
                "Error should mention method is not defined");
    }

    /**
     * Test method call with correct number of parameters
     */
    @Test
    @DisplayName("Valid method call should have no errors")
    public void testMethodCallCorrectParams() {
        String validCode = codeWritten(
                ".constant",
                "OBJREF 10",
                ".end-constant",
                "",
                ".main",
                "BIPUSH 5",
                "BIPUSH 10",
                "LDC_W OBJREF",
                "INVOKEVIRTUAL add",
                ".end-main",
                "",
                ".method add(a, b)",
                "ILOAD a",
                "ILOAD b",
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<SemanticError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid method call should have no errors");
    }
    
    /**
     * Test undefined label in jump instructions
     */
    @Test
    @DisplayName("Should detect undefined label")
    public void testUndefinedLabelError() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 5",
                "IFEQ nonexistentLabel", // Label doesn't exist
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect undefined label");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("not defined")),
                "Error should mention label is not defined");
    }
    
    /**
     * Test .var blocks must be at the top of methods
     */
    @Test
    @DisplayName("Should detect var block not at top")
    public void testVarBlockAtTop() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 5",  // Instruction before var block
                ".var",
                "x",
                ".end-var",
                "ISTORE x",
                ".end-main"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect var block not at top");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("var blocks must be defined at the top")),
                "Error should mention var blocks must be at the top");
    }
    
    /**
     * Test object reference required for method calls
     */
    @Test
    @DisplayName("Valid method call with object reference should have no errors")
    public void testObjectRefForMethodCall() {
        String validCode = codeWritten(
                ".constant",
                "OBJREF 10",
                ".end-constant",
                "",
                ".main",
                "BIPUSH 5",
                "BIPUSH 10",
                "LDC_W OBJREF",   // Object reference loaded
                "INVOKEVIRTUAL add",
                ".end-main",
                "",
                ".method add(a, b)",
                "ILOAD a",
                "ILOAD b",
                "IADD",
                "IRETURN",
                ".end-method"
        );
        
        List<SemanticError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid method call with object reference should have no errors");
    }

    /**
     * Test method must end with IRETURN
     */
    @Test
    @DisplayName("Should detect missing IRETURN")
    public void testMethodEndWithIReturn() {
        String invalidCode = codeWritten(
                ".main",
                "BIPUSH 1", // Only one value on stack
                ".end-main",
                ".method add(a, b)",
                "ILOAD a",
                "ILOAD b",
                "IADD",      // No IRETURN at the end
                ".end-method"
        );
        
        List<SemanticError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing IRETURN");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Method must end with IRETURN")),
                "Error should mention method must end with IRETURN");
    }
    
    /**
     * Test comments are allowed
     */
    @Test
    @DisplayName("Valid program with comments should have no errors")
    public void testCommentsAllowed() {
        String validCode = codeWritten(
                "// My program",
                "",
                ".constant ; This is a comment",
                "OBJREF 10 // Another comment",
                ".end-constant",
                "",
                ".main ; Main block",
                ".var ; Variables",
                "x ; Variable x",
                ".end-var",
                "BIPUSH 5 ; Push 5",
                "ISTORE x ; Store to x",
                ".end-main",
                "",
                ".method add(a, b) ; Method with two params",
                "ILOAD a ; Load a",
                "ILOAD b ; Load b",
                "IADD // Add them",
                "IRETURN // Return result",
                ".end-method ; End of method"
        );
        
        List<SemanticError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid program with comments should have no errors");
    }

    @Test
    @DisplayName("Tests the Tanenbaum 1999 file")
    public void testTanenbaum1999() throws IOException
    {
        URL resourceUrl = getClass()
                .getClassLoader()
                .getResource("examples/ijvm");
        assertNotNull(resourceUrl, "Examples directory not found in resources");

        File dir = new File(resourceUrl.getFile());

        var list = dir.listFiles((dir1, name) -> name.contains("Tanenbaum"));
        assertNotNull(list);

        for (File file : list)
        {
            List<SemanticError> errors = analyzeCodeFromPath(file.getAbsolutePath());
            assertTrue(errors.isEmpty(), "Valid program with comments should have no errors");
        }
    }

    /**
     * Helper method to analyze code and return the parse result
     */
    private List<SemanticError> analyzeCode(String code)
    {
        IJVMParseResult result = parserHelper.parseString(code);
        return result.getSemanticErrors();
    }

    /**
     * Helper method to analyze code from a file path and return errors
     */
    private List<SemanticError> analyzeCodeFromPath(String filePath) throws IOException
    {
        IJVMParseResult result = parserHelper.parseFile(filePath);
        return result.getSemanticErrors();
    }
} 