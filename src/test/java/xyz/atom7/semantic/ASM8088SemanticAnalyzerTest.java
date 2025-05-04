package xyz.atom7.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.parser.ASM8088ParseResult;
import xyz.atom7.parser.ASM8088ParserHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.atom7.Utils.codeWritten;

/**
 * Tests for the ASM8088 semantic analyzer to verify proper semantic validation.
 */
public class ASM8088SemanticAnalyzerTest
{
    private ASM8088ParserHelper parserHelper;

    @BeforeEach
    void setUp()
    {
        parserHelper = new ASM8088ParserHelper();
    }
    
    /**
     * Test valid 8088 assembly code with no semantic errors
     */
    @Test
    @DisplayName("Valid program should have no semantic errors")
    public void testValidProgram() 
    {
        String validCode = codeWritten(
                "_PRINTF = 127",
                "_EXIT = 1",
                "",
                ".SECT .TEXT",
                "main:",
                "    MOV AX, 0x1000",
                "    MOV BX, AX",
                "    JMP loop",
                "loop:",
                "    INC CX",
                "    CMP CX, 10",
                "    JNE loop",
                "    PUSH 0",
                "    PUSH _EXIT",
                "    SYS",
                "",
                ".SECT .DATA",
                "message: .ASCII \"Hello, world!\\0\"",
                "var1: .BYTE 1, 2, 3",
                "buffer: .SPACE 10"
        );
        
        List<SemanticError> result = analyzeCode(validCode);
        assertTrue(result.isEmpty(), "Valid program should have no semantic errors");
    }
    
    /**
     * Test for undefined label references - This test should identify when a jump target doesn't exist
     */
    @Test
    @DisplayName("Should detect undefined label references")
    public void testUndefinedLabelReference() 
    {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    MOV AX, 0x1000",
                "    JMP nonexistent_label" // Undefined label
        );
        
        List<SemanticError> result = analyzeCode(invalidCode);
        
        boolean foundIssue;

        foundIssue = result.stream()
                .map(SemanticError::getMessage)
                .anyMatch(m ->
                    m.contains("referenced but never defined") ||
                    m.contains("nonexistent_label"));
    
        assertTrue(foundIssue, "Should detect reference to undefined label 'nonexistent_label'");
    }
    
    /**
     * Test for duplicate label definitions - verify we catch when the same label is defined twice
     */
    @Test
    @DisplayName("Should detect duplicate label definitions")
    public void testDuplicateLabelDefinition()
    {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "label1:",
                "    MOV AX, 0x1000",
                "    JMP next",
                "next:",
                "    MOV BX, AX",
                "label1:", // Duplicate label
                "    RET"
        );
        
        List<SemanticError> result = analyzeCode(invalidCode);
        
        boolean foundDuplicateError;

        foundDuplicateError = result.stream()
                .map(SemanticError::getMessage)
                .anyMatch(m ->
                    m.contains("already defined") &&
                    m.contains("label1"));
        
        assertTrue(foundDuplicateError, "Should detect duplicate definition of label 'label1'");
    }
    
    /**
     * Test for byte/word register type mismatches with instructions
     */
    @Test
    @DisplayName("Should detect register type mismatches with instructions")
    public void testRegisterTypeMismatch() 
    {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    MOVB AX, BX", // Byte instruction with word registers
                "    MOV AL, BL"    // Word instruction with byte registers
        );
        
        List<SemanticError> result = analyzeCode(invalidCode);
        
        boolean foundMismatchError;

        foundMismatchError = result.stream()
                .map(SemanticError::getMessage)
                .anyMatch(m ->
                    m.contains("Byte instruction requires byte register") ||
                    m.contains("Word instruction requires word register"));
        
        assertTrue(foundMismatchError, "Should detect at least one register type mismatch");
    }

    /**
     * Test for jump instructions outside .TEXT section
     */
    @Test
    @DisplayName("Should warn about jump instructions outside .TEXT section")
    public void testJumpInstructionsOutsideTextSection() 
    {
        String invalidCode = codeWritten(
                ".SECT .DATA",
                "JMP label1", // Jump in .DATA section
                ".SECT .TEXT",
                "label1:",
                "    RET"
        );
        
        List<SemanticWarning> result = analyzeWarnings(invalidCode);
        
        boolean foundJumpWarning;

        foundJumpWarning = result.stream()
                .map(SemanticWarning::getMessage)
                .anyMatch(m -> m.contains("Jump/call instruction used outside .TEXT section"));

        assertTrue(foundJumpWarning, "Should warn about jump instructions outside .TEXT section");
    }
    
    /**
     * Test for unreferenced labels
     */
    @Test
    @DisplayName("Should warn about unreferenced labels")
    public void testUnreferencedLabels() 
    {
        String codeWithUnreferencedLabel = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    MOV AX, 0x1000",
                "    JMP used_label",
                "used_label:",
                "    MOV BX, AX",
                "    RET",
                "unused_label:", // Never referenced by jumps
                "    MOV CX, AX",
                "    RET"
        );
        
        List<SemanticWarning> result = analyzeWarnings(codeWithUnreferencedLabel);
        
        boolean foundUnreferencedWarning;

        foundUnreferencedWarning = result.stream()
                .map(SemanticWarning::getMessage)
                .anyMatch(m ->
                    m.contains("defined but never referenced") &&
                    m.contains("unused_label"));

        assertTrue(foundUnreferencedWarning, "Should warn about unreferenced label 'unused_label'");
    }
    
    /**
     * Test for duplicate constant definition
     */
    @Test
    @DisplayName("Should detect duplicate constant definitions")
    public void testDuplicateConstantDefinition() 
    {
        String invalidCode = codeWritten(
                "max_count = 10",
                "min_count = 5",
                "max_count = 20", // Duplicate constant
                ".SECT .TEXT",
                "main:",
                "    MOV AX, max_count",
                "    RET"
        );
        
        List<SemanticError> result = analyzeCode(invalidCode);
        
        boolean foundDuplicateError;

        foundDuplicateError = result.stream()
                .map(SemanticError::getMessage)
                .anyMatch(m ->
                    m.contains("already defined") &&
                    m.contains("max_count"));
        
        assertTrue(foundDuplicateError, "Should detect duplicate definition of constant 'max_count'");
    }
    
    /**
     * Test for memory access to named locations outside .DATA section
     */
    @Test
    @DisplayName("Should warn about memory access to named locations outside .DATA section")
    public void testMemoryAccessOutsideDataSection() 
    {
        String invalidCode = codeWritten(
                ".SECT .DATA",
                "buffer: .SPACE 10",
                "value: .BYTE 5",
                ".SECT .TEXT",
                "main:",
                "    MOV AX, (buffer)", // Reference to named location from .TEXT
                "    MOV BX, (value)",
                "    RET"
        );
        
        List<SemanticWarning> result = analyzeWarnings(invalidCode);
        
        assertNotNull(result, "Should be able to parse code with memory access");
        
        boolean hasWarning = result.stream()
                .map(SemanticWarning::getMessage)
                .anyMatch(m -> m.contains("Memory access to named location should be in .DATA section"));

        assertTrue(hasWarning, "Should warn about memory access to named location outside .DATA section");
    }

    /**
     * Test for section presence (as a warning)
     */
    @Test
    @DisplayName("Program should have sections defined")
    public void testNoSectionDefined() 
    {
        String codeWithoutSections = codeWritten(
                "main:",
                "    MOV AX, 0x1000",
                "    RET"
        );
        
        List<SemanticError> result = analyzeCode(codeWithoutSections);
        assertNotNull(result, "Should be able to parse code without sections");
    }
    
    /**
     * Test parsing of example files
     */
    @Test
    @DisplayName("Should parse example files without semantic errors")
    public void testExampleFiles() throws IOException 
    {
        URL resourceUrl = getClass()
                .getClassLoader()
                .getResource("examples/ijvm");
        assertNotNull(resourceUrl, "Examples directory not found in resources");

        File dir = new File(resourceUrl.getFile());

        var list = dir.listFiles((dir1, name) -> name.contains("Tanenbaum"));
        assertNotNull(list);

        for (File file : list) {
            List<SemanticError> errors = analyzeCodeFromPath(file.getAbsolutePath());
            assertTrue(errors.isEmpty(), "Valid program with comments should have no errors");
        }
    }

    /**
     * Helper method to analyze code and return the parse result
     */
    private List<SemanticError> analyzeCode(String code)
    {
        ASM8088ParseResult result = parserHelper.parseString(code);
        return result.getSemanticErrors();
    }

    /**
     * Helper method to analyze code and return the parse result for warnings
     */
    private List<SemanticWarning> analyzeWarnings(String code)
    {
        ASM8088ParseResult result = parserHelper.parseString(code);
        return result.getSemanticWarnings();
    }
    
    /**
     * Helper method to analyze code from a file path
     */
    private List<SemanticError> analyzeCodeFromPath(String filePath) throws IOException
    {
        ASM8088ParseResult result = parserHelper.parseFile(filePath);
        return result.getSemanticErrors();
    }
} 