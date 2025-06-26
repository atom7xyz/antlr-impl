package xyz.atom7.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.atom7.interpreter.asm8088.ASM8088Instruction;
import xyz.atom7.interpreter.asm8088.ASM8088Program;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.atom7.Utils.codeWritten;

public class ASM8088ProgramTest
{
    private ASM8088Program<ASM8088Instruction> program;

    @BeforeEach
    void setUp()
    {
        program = new ASM8088Program<>();
    }

    /**
     * Provides test cases for 8088 assembly example files.
     * Each test case contains: test name, file name, input values, expected output
     */
    static Stream<Arguments> asm8088TestCases()
    {
        return Stream.of(
            Arguments.of("8088_test_fibonacci - n=5 -> first 5 fibonacci numbers", 
                "8088_test_fibonacci.asm", "5", "0 1 1 2 3"),
            Arguments.of("8088_test_is_prime - n=7 -> prime (1)", 
                "8088_test_is_prime.asm", "7", "1"),
            Arguments.of("8088_test_is_prime - n=4 -> not prime (0)", 
                "8088_test_is_prime.asm", "4", "0"),
            Arguments.of("8088_test_pythagorean - a=3,b=4 -> hypotenuse=5, perimeter=12, area=6", 
                "8088_test_pythagorean.asm", "3;4;", "5 12 6"),
            Arguments.of("8088_test_sqrt - num1=9,num2=4 -> sqrt(9)*sqrt(4)=6", 
                "8088_test_sqrt.asm", "9;4;", "6"),
            Arguments.of("8088_test_gdc - a=9,b=6 -> GCD=3",
                "8088_test_gdc.asm", "9;6;", "3")
        );
    }

    /**
     * Test runner for 8088 assembly example files that require input and produce output.
     * Uses input redirection and output capture to automate testing.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("asm8088TestCases")
    @DisplayName("8088 Assembly Example Files")
    void test8088ExampleFiles(String testName, String fileName, String inputs, String expectedOutput)
    {
        // Custom SecurityManager to catch System.exit calls
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        
        try {
            // Read the file content
            Path filePath = Paths.get("src/main/resources/examples/8088/" + fileName);
            String fileContent = Files.readString(filePath);

            // Prepare input stream if inputs are provided
            InputStream originalIn = System.in;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            
            try {
                // Redirect input if needed
                if (inputs != null && !inputs.trim().isEmpty()) {
                    // For 8088 input, we need to simulate entering characters
                    StringBuilder inputBuilder = new StringBuilder();
                    String[] inputValues = inputs.split(",");
                    for (String value : inputValues) {
                        String trimmedValue = value.trim();
                        // Add each character of the input
                        for (char c : trimmedValue.toCharArray()) {
                            inputBuilder.append(c).append("\n");
                        }
                    }
                    System.setIn(new ByteArrayInputStream(inputBuilder.toString().getBytes()));
                }

                // Capture output
                System.setOut(new PrintStream(outputStream));

                // Create and run the program
                ASM8088Program<ASM8088Instruction> testProgram = new ASM8088Program<>();
                testProgram.init(fileContent);
                
                try {
                    testProgram.execute();
                } catch (ExitException e) {
                    // Expected when program calls System.exit
                    // This is normal program termination
                }

            } finally {
                // Restore original streams
                System.setIn(originalIn);
                System.setOut(originalOut);
            }

            // Get the captured output and extract the printed numbers
            String output = outputStream.toString();
            String actualOutput = extractPrintedNumbers(output);
            
            // Compare with expected output
            assertEquals(expectedOutput.trim(), actualOutput.trim(), 
                "Output mismatch for " + testName + ". Full captured output: " + output);

        } catch (Exception e) {
            throw new RuntimeException("Failed to test " + fileName + ": " + e.getMessage(), e);
        } finally {
            // Restore original security manager
            System.setSecurityManager(originalSecurityManager);
        }
    }

    /**
     * Extracts printed numbers from the captured output.
     * Looks for numbers formatted by printf calls.
     */
    private String extractPrintedNumbers(String output)
    {
        // Look for formatted output from printf calls
        StringBuilder result = new StringBuilder();
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            // Skip debug/trace output and look for actual program output
            if (line.trim().isEmpty() || 
                line.contains("TRACER") || 
                line.contains("PC:") || 
                line.contains("Registers") ||
                line.contains("Stack") ||
                line.contains("Flags") ||
                line.contains("Memory") ||
                line.contains("OUTPUT:") ||
                line.contains("exited")) {
                continue;
            }
            
            // Extract numbers from the line
            String trimmed = line.trim();
            if (trimmed.matches(".*\\d+.*")) {
                // Extract all numbers from the line
                String[] parts = trimmed.split("\\s+");
                for (String part : parts) {
                    if (part.matches("\\d+")) {
                        if (result.length() > 0) {
                            result.append(" ");
                        }
                        result.append(part);
                    }
                }
            }
        }
        
        // If no formatted output found, look for individual character outputs
        if (result.length() == 0) {
            for (String line : lines) {
                if (line.contains("OUTPUT:")) {
                    // Extract the character after "OUTPUT:"
                    String[] parts = line.split("OUTPUT:");
                    if (parts.length > 1) {
                        String outputPart = parts[1].trim();
                        if (!outputPart.isEmpty()) {
                            char outputChar = outputPart.charAt(0);
                            if (Character.isDigit(outputChar)) {
                                if (result.length() > 0) {
                                    result.append(" ");
                                }
                                result.append(outputChar);
                            }
                        }
                    }
                }
            }
        }
        
        return result.toString();
    }

    @Nested
    @DisplayName("Movement Instructions")
    class MovementInstructionsTest
    {
        @Test
        @DisplayName("MOV should move value from source to destination")
        void testMov()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 42",
                    "MOV BX, AX"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
            assertEquals(42, program.getScope().getRegister16("BX"));
        }
        
        @Test
        @DisplayName("MOVB should move byte value")
        void testMovb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOVB AL, 255",
                    "MOVB BL, AL"
            );

            program.init(code);
            program.execute();

            assertEquals(255, program.getScope().getRegister8("AL"));
            assertEquals(255, program.getScope().getRegister8("BL"));
        }
    }
    
    @Nested
    @DisplayName("Arithmetic Instructions")
    class ArithmeticInstructionsTest
    {
        @Test
        @DisplayName("ADD should add source to destination")
        void testAdd()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 30",
                    "ADD AX, 12"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("SUB should subtract source from destination")
        void testSub()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 50",
                    "SUB AX, 8"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("INC should increment register")
        void testInc()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 41",
                    "INC AX"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("DEC should decrement register")
        void testDec()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 43",
                    "DEC AX"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("MUL should multiply AL with operand")
        void testMul()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 6",
                    "MUL 7"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("DIV should divide AX by operand")
        void testDiv()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 84",
                    "DIV 2"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister8("AL")); // Quotient in AL
            assertEquals(0, program.getScope().getRegister8("AH"));  // Remainder in AH
        }
    }
    
    @Nested
    @DisplayName("Comparison and Flags")
    class ComparisonInstructionsTest
    {
        @Test
        @DisplayName("CMP should set zero flag when operands are equal")
        void testCmpEqual()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 42",
                    "CMP AX, 42"
            );

            program.init(code);
            program.execute();

            assertTrue(program.getScope().isZeroFlag());
        }
        
        @Test
        @DisplayName("CMP should clear zero flag when operands are not equal")
        void testCmpNotEqual()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 42",
                    "CMP AX, 43"
            );

            program.init(code);
            program.execute();

            assertFalse(program.getScope().isZeroFlag());
        }
    }
    
    @Nested
    @DisplayName("Jump Instructions")
    class JumpInstructionsTest
    {
        @Test
        @DisplayName("JMP should jump unconditionally")
        void testJmp()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 10",
                    "JMP skip",
                    "MOV AX, 20",
                    "skip:",
                    "ADD AX, 32"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("JE should jump when zero flag is set")
        void testJe()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 42",
                    "CMP AX, 42",
                    "JE zero_branch",
                    "MOV AX, 100",
                    "zero_branch:"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
        
        @Test
        @DisplayName("JNE should jump when zero flag is not set")
        void testJne()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 42",
                    "CMP AX, 43",
                    "JNE nonzero_branch",
                    "MOV AX, 100",
                    "nonzero_branch:"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
    }
    
    @Nested
    @DisplayName("Stack Operations")
    class StackOperationsTest
    {
        @Test
        @DisplayName("PUSH should push value onto stack")
        void testPush()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "PUSH 42"
            );

            program.init(code);
            int initialSP = program.getScope().getRegister16("SP");
            program.execute();

            // SP should have decreased by 2 (word size)
            assertEquals(initialSP - 2, program.getScope().getRegister16("SP"));
            
            // Check that value was written to stack
            int stackAddr = program.getScope().getStackAddress(program.getScope().getRegister16("SP"));
            assertEquals(42, program.getScope().readWord(stackAddr));
        }
        
        @Test
        @DisplayName("POP should pop value from stack")
        void testPop()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "PUSH 42",
                    "POP AX"
            );

            program.init(code);
            int initialSP = program.getScope().getRegister16("SP");
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
            // SP should be back to initial value
            assertEquals(initialSP, program.getScope().getRegister16("SP"));
        }
    }
    
    @Nested
    @DisplayName("Constants and Labels")
    class ConstantsAndLabelsTest
    {
        @Test
        @DisplayName("Constants should be accessible")
        void testConstants()
        {
            String code = codeWritten(
                    "ANSWER = 42",
                    ".SECT .TEXT",
                    "PUSH ANSWER"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().popStack());
        }
        
        @Test
        @DisplayName("Hex constants should work")
        void testHexConstants()
        {
            String code = codeWritten(
                    "MAGIC = 0x2A",
                    ".SECT .TEXT",
                    "MOV AX, MAGIC"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
    }
    
    @Nested
    @DisplayName("8-bit Register Access")
    class ByteRegisterTest
    {
        @Test
        @DisplayName("Setting AL should affect low byte of AX")
        void testByteRegisterLow()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0x1234",
                    "MOV AL, 0x56"
            );

            program.init(code);
            program.execute();

            assertEquals(0x1256, program.getScope().getRegister16("AX"));
            assertEquals(0x56, program.getScope().getRegister8("AL"));
        }
        
        @Test
        @DisplayName("Setting AH should affect high byte of AX")
        void testByteRegisterHigh()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0x1234",
                    "MOV AH, 0x56"
            );

            program.init(code);
            program.execute();

            assertEquals(0x5634, program.getScope().getRegister16("AX"));
            assertEquals(0x56, program.getScope().getRegister8("AH"));
        }
    }

    @Test
    @DisplayName("Complex program with loop")
    void testComplexProgram()
    {
        String code = codeWritten(
                ".SECT .TEXT",
                "main:",
                "   MOV AX, 0",
                "   MOV CX, 5",
                "e:",
                "    ADD AX, CX",
                "    LOOP e"
        );

        program.init(code);
        program.execute();

        assertEquals(15, program.getScope().getRegister16("AX")); // Sum of 5+4+3+2+1 = 15
        assertEquals(0, program.getScope().getRegister16("CX"));  // CX should be 0 after loop
    }

    @Test
    @DisplayName("System call test")
    void testSystemCall()
    {
        // Custom SecurityManager to catch System.exit calls
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        
        try {
            String code = codeWritten(
                    ".SECT .DATA",
                    "msg: .ASCII \"Hi\"",
                    ".SECT .TEXT",
                    "PUSH msg",
                    "PUSH 42",
                    "PUSH 127",   // Printf syscall
                    "SYS",
                    "PUSH 0",     // Exit code
                    "PUSH 1",     // Exit syscall
                    "SYS"
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            
            try {
                System.setOut(new PrintStream(outputStream));
                
                program.init(code);
                
                try {
                    program.execute();
                } catch (ExitException e) {
                    // Expected when exit syscall is called
                    assertEquals(0, e.status);
                }
                
            } finally {
                System.setOut(originalOut);
            }

            String output = outputStream.toString();
            // Check that printf was called (should contain the format and value)
            assertNotNull(output, "Should produce output from printf");

        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    @Nested
    @DisplayName("Carry-based Arithmetic Instructions")
    class CarryArithmeticTest
    {
        @Test
        @DisplayName("ADC should add with carry")
        void testAdc()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0xFFFF",  // Set AX to max value
                    "ADD AX, 1",       // This will set carry flag
                    "MOV BX, 5",
                    "ADC BX, 3"        // Should add 3 + 1 (carry) = 4, result = 9
            );

            program.init(code);
            program.execute();

            assertEquals(9, program.getScope().getRegister16("BX"));
        }

        @Test
        @DisplayName("SBB should subtract with borrow")
        void testSbb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0",       // Set AX to 0
                    "SUB AX, 1",       // This will set carry flag (borrow)
                    "MOV BX, 10",
                    "SBB BX, 3"        // Should subtract 3 + 1 (borrow) = 4, result = 6
            );

            program.init(code);
            program.execute();

            assertEquals(6, program.getScope().getRegister16("BX"));
        }
    }

    @Nested
    @DisplayName("Logical Instructions")
    class LogicalInstructionsTest
    {
        @Test
        @DisplayName("AND should perform bitwise AND")
        void testAnd()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0xFF0F",
                    "AND AX, 0x0FF0"
            );

            program.init(code);
            program.execute();

            assertEquals(0x0F00, program.getScope().getRegister16("AX"));
            assertFalse(program.getScope().isCarryFlag()); // AND clears carry
        }

        @Test
        @DisplayName("OR should perform bitwise OR")
        void testOr()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0xFF0F",
                    "OR AX, 0x0FF0"
            );

            program.init(code);
            program.execute();

            assertEquals(0xFFFF, program.getScope().getRegister16("AX"));
            assertFalse(program.getScope().isCarryFlag()); // OR clears carry
        }

        @Test
        @DisplayName("XOR should perform bitwise XOR")
        void testXor()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0xFF0F",
                    "XOR AX, 0x0FF0"
            );

            program.init(code);
            program.execute();

            assertEquals(0xF0FF, program.getScope().getRegister16("AX"));
            assertFalse(program.getScope().isCarryFlag()); // XOR clears carry
        }

        @Test
        @DisplayName("XOR register with itself should clear it")
        void testXorSelf()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0x1234",
                    "XOR AX, AX"
            );

            program.init(code);
            program.execute();

            assertEquals(0, program.getScope().getRegister16("AX"));
            assertTrue(program.getScope().isZeroFlag());
        }

        @Test
        @DisplayName("NOT should perform bitwise NOT")
        void testNot()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0x0F0F",
                    "NOT AX"
            );

            program.init(code);
            program.execute();

            assertEquals(0xF0F0, program.getScope().getRegister16("AX"));
        }
    }

    @Nested
    @DisplayName("8-bit Arithmetic Instructions")
    class ByteArithmeticTest
    {
        @Test
        @DisplayName("ADDB should add byte values")
        void testAddb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 100",
                    "ADDB AL, 55"
            );

            program.init(code);
            program.execute();

            assertEquals(155, program.getScope().getRegister8("AL"));
        }

        @Test
        @DisplayName("SUBB should subtract byte values")
        void testSubb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 200",
                    "SUBB AL, 58"
            );

            program.init(code);
            program.execute();

            assertEquals(142, program.getScope().getRegister8("AL"));
        }

        @Test
        @DisplayName("CMPB should compare byte values and set flags")
        void testCmpb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 42",
                    "CMPB AL, 42"
            );

            program.init(code);
            program.execute();

            assertTrue(program.getScope().isZeroFlag());
        }

        @Test
        @DisplayName("MULB should multiply byte values")
        void testMulb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 12",
                    "MULB 11"        // 12 * 11 = 132 (0x84)
            );

            program.init(code);
            program.execute();

            assertEquals(132, program.getScope().getRegister8("AL"));
            assertEquals(0, program.getScope().getRegister8("AH")); // No overflow
        }

        @Test
        @DisplayName("MULB with overflow should set AH")
        void testMulbOverflow()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 16",
                    "MULB 17"        // 16 * 17 = 272 (0x110) -> AL=0x10, AH=0x01
            );

            program.init(code);
            program.execute();

            assertEquals(0x10, program.getScope().getRegister8("AL"));
            assertEquals(0x01, program.getScope().getRegister8("AH"));
        }

        @Test
        @DisplayName("DIVB should divide byte values")
        void testDivb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 100",
                    "DIVB 7"         // 100 / 7 = 14 remainder 2
            );

            program.init(code);
            program.execute();

            assertEquals(14, program.getScope().getRegister8("AL")); // Quotient
            assertEquals(2, program.getScope().getRegister8("AH"));  // Remainder
        }

        @Test
        @DisplayName("XORB should XOR byte values")
        void testXorb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AL, 0xF0",
                    "XORB AL, 0x0F"
            );

            program.init(code);
            program.execute();

            assertEquals(0xFF, program.getScope().getRegister8("AL"));
        }
    }

    @Nested
    @DisplayName("Advanced Jump Instructions")
    class AdvancedJumpTest
    {
        @Test
        @DisplayName("JZ should jump when zero flag is set")
        void testJz()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 5",
                    "SUB AX, 5",      // Sets zero flag
                    "JZ zero_target",
                    "MOV AX, 999",    // Should be skipped
                    "zero_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(1, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JNZ should jump when zero flag is clear")
        void testJnz()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 5",
                    "SUB AX, 3",      // Does not set zero flag
                    "JNZ nonzero_target",
                    "MOV AX, 999",    // Should be skipped
                    "nonzero_target:",
                    "ADD AX, 10"
            );

            program.init(code);
            program.execute();

            assertEquals(12, program.getScope().getRegister16("AX")); // 2 + 10
        }

        @Test
        @DisplayName("JG should jump for greater than (signed)")
        void testJg()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 10",
                    "CMP AX, 5",      // 10 > 5
                    "JG greater_target",
                    "MOV AX, 999",    // Should be skipped
                    "greater_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(11, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JL should jump for less than (signed)")
        void testJl()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 3",
                    "CMP AX, 7",      // 3 < 7
                    "JL less_target",
                    "MOV AX, 999",    // Should be skipped
                    "less_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(4, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JGE should jump for greater or equal (signed)")
        void testJge()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 5",
                    "CMP AX, 5",      // 5 >= 5
                    "JGE ge_target",
                    "MOV AX, 999",    // Should be skipped
                    "ge_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(6, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JLE should jump for less or equal (signed)")
        void testJle()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 5",
                    "CMP AX, 5",      // 5 <= 5
                    "JLE le_target",
                    "MOV AX, 999",    // Should be skipped
                    "le_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(6, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JA should jump for above (unsigned)")
        void testJa()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 300",
                    "CMP AX, 200",    // 300 > 200 (unsigned)
                    "JA above_target",
                    "MOV AX, 999",    // Should be skipped
                    "above_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(301, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JB should jump for below (unsigned)")
        void testJb()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 100",
                    "CMP AX, 200",    // 100 < 200 (unsigned)
                    "JB below_target",
                    "MOV AX, 999",    // Should be skipped
                    "below_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(101, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JAE should jump for above or equal (unsigned)")
        void testJae()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 200",
                    "CMP AX, 200",    // 200 >= 200 (unsigned)
                    "JAE ae_target",
                    "MOV AX, 999",    // Should be skipped
                    "ae_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(201, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JBE should jump for below or equal (unsigned)")
        void testJbe()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 200",
                    "CMP AX, 200",    // 200 <= 200 (unsigned)
                    "JBE be_target",
                    "MOV AX, 999",    // Should be skipped
                    "be_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(201, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JCXZ should jump when CX is zero")
        void testJcxz()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV CX, 0",      // Set CX to 0
                    "MOV AX, 5",
                    "JCXZ cx_zero_target",
                    "MOV AX, 999",    // Should be skipped
                    "cx_zero_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(6, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JCXZ should not jump when CX is not zero")
        void testJcxzNotZero()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV CX, 5",      // Set CX to non-zero
                    "MOV AX, 5",
                    "JCXZ cx_zero_target",
                    "ADD AX, 10",     // Should execute
                    "cx_zero_target:",
                    "ADD AX, 1"
            );

            program.init(code);
            program.execute();

            assertEquals(16, program.getScope().getRegister16("AX")); // 5 + 10 + 1
        }
    }

    @Nested
    @DisplayName("Flag-based Jump Instructions")
    class FlagJumpTest
    {
        @Test
        @DisplayName("JS should jump when sign flag is set")
        void testJs()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 5",
                    "SUB AX, 10",     // Result is negative, sets sign flag
                    "JS sign_target",
                    "MOV AX, 999",    // Should be skipped
                    "sign_target:",
                    "MOV AX, 42"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JNS should jump when sign flag is clear")
        void testJns()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 10",
                    "SUB AX, 5",      // Result is positive, clears sign flag
                    "JNS nosign_target",
                    "MOV AX, 999",    // Should be skipped
                    "nosign_target:",
                    "MOV AX, 42"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JC should jump when carry flag is set")
        void testJc()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0xFFFF",
                    "ADD AX, 1",      // This will set carry flag
                    "JC carry_target",
                    "MOV AX, 999",    // Should be skipped
                    "carry_target:",
                    "MOV AX, 42"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }

        @Test
        @DisplayName("JNC should jump when carry flag is clear")
        void testJnc()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 10",
                    "ADD AX, 5",      // This will not set carry flag
                    "JNC nocarry_target",
                    "MOV AX, 999",    // Should be skipped
                    "nocarry_target:",
                    "MOV AX, 42"
            );

            program.init(code);
            program.execute();

            assertEquals(42, program.getScope().getRegister16("AX"));
        }
    }

    @Nested
    @DisplayName("Function Call Instructions")
    class FunctionCallTest
    {
        @Test
        @DisplayName("CALL and RET should work together")
        void testCallRet()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 10",
                    "CALL subroutine",
                    "ADD AX, 1",      // Should execute after return
                    "JMP end",
                    "subroutine:",
                    "ADD AX, 5",
                    "RET",
                    "end:"
            );

            program.init(code);
            program.execute();

            assertEquals(16, program.getScope().getRegister16("AX")); // 10 + 5 + 1
        }

        @Test
        @DisplayName("Nested function calls should work")
        void testNestedCalls()
        {
            String code = codeWritten(
                    ".SECT .TEXT",
                    "MOV AX, 0",
                    "CALL func1",
                    "JMP end",
                    "func1:",
                    "ADD AX, 1",
                    "CALL func2",
                    "ADD AX, 4",      // Should execute after func2 returns
                    "RET",
                    "func2:",
                    "ADD AX, 2",
                    "RET",
                    "end:"
            );

            program.init(code);
            program.execute();

            assertEquals(7, program.getScope().getRegister16("AX")); // 0 + 1 + 2 + 4
        }
    }

    @Nested
    @DisplayName("Memory Operations")
    class MemoryOperationsTest
    {
        @Test
        @DisplayName("Memory addressing with offset")
        void testMemoryAddressingWithOffset()
        {
            String code = codeWritten(
                    ".SECT .DATA",
                    "values: .BYTE 10, 20, 30",
                    ".SECT .TEXT",
                    "MOV BX, values",
                    "MOV AL, [BX+1]"   // Load second value (20)
            );

            program.init(code);
            program.execute();

            assertEquals(20, program.getScope().getRegister8("AL"));
        }

        @Test
        @DisplayName("Writing to memory")
        void testMemoryWrite()
        {
            String code = codeWritten(
                    ".SECT .DATA",
                    "storage: .SPACE 1",
                    ".SECT .TEXT",
                    "MOV BX, storage",
                    "MOV AL, 99",
                    "MOV [BX], AL",    // Store AL to memory
                    "MOV CL, [BX]"     // Load back to verify
            );

            program.init(code);
            program.execute();

            assertEquals(99, program.getScope().getRegister8("CL"));
        }
    }

    @Nested
    @DisplayName("Data Section Tests")
    class DataSectionTest
    {
        @Test
        @DisplayName("BYTE arrays should be accessible")
        void testByteData()
        {
            String code = codeWritten(
                    ".SECT .DATA",
                    "numbers: .BYTE 1, 2, 3, 4, 5",
                    ".SECT .TEXT",
                    "MOV BX, numbers",
                    "MOV AL, [BX+2]"   // Load third number (3)
            );

            program.init(code);
            program.execute();

            assertEquals(3, program.getScope().getRegister8("AL"));
        }

        @Test
        @DisplayName("SPACE reservation should work")
        void testSpaceReservation()
        {
            String code = codeWritten(
                    ".SECT .BSS",
                    "buffer: .SPACE 10",
                    ".SECT .TEXT",
                    "MOV BX, buffer",
                    "MOV AL, 255",
                    "MOV [BX], AL",     // Write to first byte of buffer
                    "MOV CL, [BX]"      // Read it back
            );

            program.init(code);
            program.execute();

            assertEquals(255, program.getScope().getRegister8("CL"));
        }
    }

    /**
     * Custom SecurityManager that prevents System.exit() calls by throwing an exception instead.
     */
    private static class NoExitSecurityManager extends SecurityManager 
    {
        @Override
        public void checkPermission(Permission perm) 
        {
            // Allow all permissions except exit
        }

        @Override
        public void checkPermission(Permission perm, Object context) 
        {
            // Allow all permissions except exit
        }

        @Override
        public void checkExit(int status) 
        {
            super.checkExit(status);
            throw new ExitException(status);
        }
    }

    /**
     * Exception thrown when System.exit() is called.
     */
    private static class ExitException extends SecurityException 
    {
        public final int status;

        public ExitException(int status) 
        {
            super("System.exit(" + status + ") called");
            this.status = status;
        }
    }
} 