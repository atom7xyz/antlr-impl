package xyz.atom7.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.atom7.api.parser.error.ParserError;
import xyz.atom7.parser.asm8088.ASM8088ParseResult;
import xyz.atom7.parser.asm8088.ASM8088ParserHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.atom7.Utils.codeWritten;

/**
 * Tests for the 8088 assembly parser syntax error detection.
 */
public class ASM8088ParserErrorTest
{
    private ASM8088ParserHelper parserHelper;

    @BeforeEach
    void setUp()
    {
        parserHelper = new ASM8088ParserHelper();
    }
    
    /**
     * Test valid 8088 assembly code with no parser errors
     */
    @Test
    @DisplayName("Valid program should have no parser errors")
    public void testValidProgram() {
        String validCode = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    MOV AX, 5",
                "    MOV BX, 10",
                "    ADD AX, BX",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(validCode);
        assertTrue(errors.isEmpty(), "Valid program should have no parser errors");
    }
    
    /**
     * Test invalid mnemonic
     */
    @Test
    @DisplayName("Should detect invalid mnemonic")
    public void testInvalidMnemonic() {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    INVALID_MNEMONIC AX, BX", // Invalid mnemonic
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid mnemonic");
    }
    
    /**
     * Test invalid section directive
     */
    @Test
    @DisplayName("Should detect invalid section directive")
    public void testInvalidSectionDirective() {
        String invalidCode = codeWritten(
                ".SECT .INVALID", // Invalid section
                "main:",
                "    MOV AX, 5",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid section directive");
    }
    
    /**
     * Test missing colon in label declaration
     */
    @Test
    @DisplayName("Should detect missing colon in label declaration")
    public void testMissingColonInLabel() {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "main", // Missing colon after label
                "    MOV AX, 5",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect missing colon in label declaration");
    }

    /**
     * Test invalid memory addressing syntax
     */
    @Test
    @DisplayName("Should detect invalid memory addressing syntax")
    public void testInvalidMemoryAddressingSyntax() {
        String invalidCode = codeWritten(
                ".SECT .TEXT",
                "main:",
                "    MOV AX, [BX", // Missing closing bracket
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid memory addressing syntax");
    }
    
    /**
     * Test invalid constant format
     */
    @Test
    @DisplayName("Should detect invalid constant format")
    public void testInvalidConstantFormat() {
        String invalidCode = codeWritten(
                "count = 0xZG", // Invalid hex format
                ".SECT .TEXT",
                "main:",
                "    MOV AX, count",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid constant format");
    }
    
    /**
     * Test invalid byte directive format
     */
    @Test
    @DisplayName("Should detect invalid byte directive format")
    public void testInvalidByteDirectiveFormat() {
        String invalidCode = codeWritten(
                ".SECT .DATA",
                "data: .BYTE 1, 2, 3,", // Missing value after last comma
                ".SECT .TEXT",
                "main:",
                "    MOV AX, (data)",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid byte directive format");
    }
    
    /**
     * Test invalid string syntax in ASCII directive
     */
    @Test
    @DisplayName("Should detect invalid string syntax in ASCII directive")
    public void testInvalidStringSyntax() {
        String invalidCode = codeWritten(
                ".SECT .DATA",
                "message: .ASCII \"Hello World", // Missing closing quote
                ".SECT .TEXT",
                "main:",
                "    MOV AX, (message)",
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertFalse(errors.isEmpty(), "Should detect invalid string syntax in ASCII directive");
    }

    /**
     * Test multiple errors
     */
    @Test
    @DisplayName("Should detect multiple errors")
    public void testMultipleErrors() {
        String invalidCode = codeWritten(
                ".SECT .INVALID", // First error: invalid section
                "main", // Second error: missing colon
                "    INVALID_MNEMONIC AX", // Third error: invalid mnemonic
                "    MOV", // Fourth error: missing operands
                "    RET"
        );
        
        List<ParserError> errors = analyzeCode(invalidCode);
        assertTrue(errors.size() >= 2, "Should detect multiple errors");
    }

    /**
     * Test for invalid number format in constant assignment
     */
    @Test
    @DisplayName("Should detect invalid number format in constant assignment")
    public void testInvalidNumberFormat()
    {
        String invalidCode = codeWritten(
                "invalid_const = 0xG", // Invalid hex format
                ".SECT .TEXT",
                "main:",
                "    MOV AX, 0x1000",
                "    RET"
        );

        List<ParserError> result = analyzeCode(invalidCode);
        assertFalse(result.isEmpty(), "Should detect invalid number format in constant assignment");
    }

    /**
     * Test an example file from resources
     */
    @Test
    @DisplayName("Valid programs should have no errors")
    public void testAllExampleFiles() throws IOException
    {
        URL resourceUrl = getClass()
                .getClassLoader()
                .getResource("examples/8088");
        assertNotNull(resourceUrl, "Examples directory not found in resources");

        File dir = new File(resourceUrl.getFile());

        var list = dir.listFiles((dir1, name) -> name.endsWith(".asm"));
        assertNotNull(list);

        for (File file : list) {
            List<ParserError> errors = analyzeCodeFromPath(file.getAbsolutePath());
            assertTrue(errors.isEmpty(), "Valid example program should have no parser errors");
        }
    }

    private List<ParserError> analyzeCode(String code)
    {
        ASM8088ParseResult result = parserHelper.parseString(code);
        return result.getParserErrors();
    }

    private List<ParserError> analyzeCodeFromPath(String filePath) throws IOException
    {
        ASM8088ParseResult result = parserHelper.parseFile(filePath);
        return result.getParserErrors();
    }
}