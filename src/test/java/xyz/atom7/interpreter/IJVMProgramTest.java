package xyz.atom7.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.atom7.interpreter.ijvm.IJVMInstruction;
import xyz.atom7.interpreter.ijvm.IJVMProgram;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Stack;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.atom7.Utils.codeWritten;

public class IJVMProgramTest
{
    private IJVMProgram<IJVMInstruction> program;

    @BeforeEach
    void setUp()
    {
        program = new IJVMProgram<>();
    }

    /**
     * Provides test cases for IJVM example files.
     * Each test case contains: test name, file name, input values, expected output
     */
    static Stream<Arguments> ijvmTestCases()
    {
        return Stream.of(
            Arguments.of("ijvm_test_0_(13-06-2023) - x=1,y=2,z=3 -> 0",
                "ijvm_test_0_(13-06-2023).jas", "1,2,3", "0"),
                
            Arguments.of("ijvm_test_0_(13-06-2022) - x=0,y=3,z=2 -> 6",
                "ijvm_test_0_(13-06-2022).jas", "0,3,2", "6"),
                
            Arguments.of("ijvm_test_0_(28-06-2024) - x=15,y=5,z=6 -> 93",
                "ijvm_test_0_(28-06-2024).jas", "15,5,6", "93"),
                
            Arguments.of("ijvm_test_1_(10-02-2023) - x=9,y=1 -> 8", 
                "ijvm_test_1_(10-02-2023).jas", "9,1", "8"),
                
            Arguments.of("ijvm_test_1_(13-06-2023) - x=1,y=2,z=3 -> 19", 
                "ijvm_test_1_(13-06-2023).jas", "1,2,3", "19"),
                
            Arguments.of("ijvm_test_1_(30-04-2021) - x=1,y=2,z=3 -> 2", 
                "ijvm_test_1_(30-04-2021).jas", "1,2,3", "2"),
                
            Arguments.of("ijvm_test_2_(13-06-2023) - x=1,y=2,z=3 -> 33", 
                "ijvm_test_2_(13-06-2023).jas", "1,2,3", "33"),
                
            Arguments.of("ijvm_test_2_(27-06-2022) - a=5,b=3 -> 6", 
                "ijvm_test_2_(27-06-2022).jas", "5,3", "6"),
                
            Arguments.of("ijvm_test_2_(30-04-2021) - x=1,y=3 -> 2", 
                "ijvm_test_2_(30-04-2021).jas", "1,3", "2"),
                
            Arguments.of("ijvm_test_3_(30-04-2021) - x=1,y=2,z=3 -> 4", 
                "ijvm_test_3_(30-04-2021).jas", "1,2,3", "4"),
                
            Arguments.of("ijvm_test_3_(8-05-2024) - x=1,y=2,z=3 -> 11", 
                "ijvm_test_3_(8-05-2024).jas", "1,2,3", "11")
        );
    }

    /**
     * Test runner for IJVM example files that require input and produce output.
     * Uses input redirection and output capture to automate testing.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("ijvmTestCases")
    @DisplayName("IJVM Example Files")
    void testIJVMExampleFiles(String testName, String fileName, String inputs, String expectedOutput)
    {
        // Custom SecurityManager to catch System.exit calls
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        
        try {
            // Read the file content
            Path filePath = Paths.get("src/main/resources/examples/ijvm/" + fileName);
            String fileContent = Files.readString(filePath);

            // Prepare input stream if inputs are provided
            InputStream originalIn = System.in;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            
            try {
                // Redirect input if needed
                if (inputs != null && !inputs.trim().isEmpty()) {
                    // For IJVM input, we need to simulate entering numbers followed by semicolons (0x3b)
                    StringBuilder inputBuilder = new StringBuilder();
                    String[] inputValues = inputs.split(",");
                    for (String value : inputValues) {
                        // Each number is entered digit by digit, followed by semicolon
                        String trimmedValue = value.trim();
                        for (char digit : trimmedValue.toCharArray()) {
                            inputBuilder.append(digit).append("\n");
                        }
                        inputBuilder.append(";").append("\n"); // semicolon acts as enter (0x3b)
                    }
                    System.setIn(new ByteArrayInputStream(inputBuilder.toString().getBytes()));
                }

                // Capture output
                System.setOut(new PrintStream(outputStream));

                // Create and run the program
                IJVMProgram<IJVMInstruction> testProgram = new IJVMProgram<>();
                testProgram.init(fileContent);
                
                try {
                    testProgram.execute();
                } catch (ExitException e) {
                    // Expected when HALT instruction calls System.exit
                    // This is normal program termination
                }

            } finally {
                // Restore original streams
                System.setIn(originalIn);
                System.setOut(originalOut);
            }

            // Get the captured output and extract just the printed numbers
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

    @Test
    @DisplayName("Tanenbaum Test (no input required)")
    void testTanenbaumExample()
    {
        // Custom SecurityManager to catch System.exit calls
        SecurityManager originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        
        try {
            // Read the Tanenbaum test file
            Path filePath = Paths.get("src/main/resources/examples/ijvm/ijvm_test_Tanenbaum_(16-02-1999).jas");
            String fileContent = Files.readString(filePath);

            // Capture output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            
            try {
                System.setOut(new PrintStream(outputStream));

                // Create and run the program
                IJVMProgram<IJVMInstruction> testProgram = new IJVMProgram<>();
                testProgram.init(fileContent);
                
                try {
                    testProgram.execute();
                } catch (ExitException e) {
                    // Expected when HALT instruction calls System.exit
                    // This is normal program termination
                }

            } finally {
                System.setOut(originalOut);
            }

            // Get the captured output
            String output = outputStream.toString();
            
            // The Tanenbaum test should output "OK" (characters 79, 75)
            assertTrue(output.contains("OK"), "Tanenbaum test should output 'OK'. Actual output: " + output);

        } catch (Exception e) {
            throw new RuntimeException("Failed to test Tanenbaum example: " + e.getMessage(), e);
        } finally {
            // Restore original security manager
            System.setSecurityManager(originalSecurityManager);
        }
    }

    /**
     * Extracts printed numbers from the captured output.
     * Looks for patterns like "PROGRAM OUTPUT IS:" followed by numbers.
     */
    private String extractPrintedNumbers(String output)
    {
        // Look for the final program output
        if (output.contains("PROGRAM OUTPUT IS:")) {
            String[] lines = output.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("PROGRAM OUTPUT IS:")) {
                    // The next line should contain the output
                    if (i + 1 < lines.length) {
                        return lines[i + 1].trim();
                    }
                }
            }
        }
        
        // If no "PROGRAM OUTPUT IS:" found, look for OUTPUT: patterns
        StringBuilder result = new StringBuilder();
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("OUTPUT:")) {
                // Extract the character after "OUTPUT:"
                String[] parts = line.split("OUTPUT:");
                if (parts.length > 1) {
                    String outputPart = parts[1].trim();
                    if (!outputPart.isEmpty()) {
                        char outputChar = outputPart.charAt(0);
                        result.append(outputChar);
                    }
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Custom SecurityManager that prevents System.exit() calls by throwing an exception instead.
     * This allows us to test programs that call System.exit() without terminating the test JVM.
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
     * Exception thrown when System.exit() is called, allowing us to catch it
     * instead of terminating the JVM.
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

    @Nested
    @DisplayName("Stack Operation Instructions")
    class StackOperationInstructionsTest
    {
        @Test
        @DisplayName("BIPUSH should push a byte value onto the stack")
        void testBipush()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    "a",
                    "b",
                    "c",
                    ".end-var",
                    "BIPUSH 1000",
                    "GOTO test",
                    "BIPUSH 1000",
                    "test:",
                    "BIPUSH 1000",
                    "IADD",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(2000, stack.get(0));
        }
        
        @Test
        @DisplayName("DUP should duplicate the top value on the stack")
        void testDup()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 42",
                    "DUP",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(2, stack.size());
            assertEquals(42, stack.get(0));
            assertEquals(42, stack.get(1));
        }
        
        @Test
        @DisplayName("SWAP should exchange the top two values on the stack")
        void testSwap()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 10",
                    "BIPUSH 20",
                    "SWAP",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(2, stack.size());
            assertEquals(20, stack.get(0));
            assertEquals(10, stack.get(1));
        }
        
        @Test
        @DisplayName("POP should remove the top value from the stack")
        void testPop()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 10",
                    "BIPUSH 20",
                    "POP",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(10, stack.get(0));
        }
    }
    
    @Nested
    @DisplayName("Arithmetic Instructions")
    class ArithmeticInstructionsTest
    {
        @Test
        @DisplayName("IADD should add the top two values on the stack")
        void testIadd()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 30",
                    "BIPUSH 12",
                    "IADD",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("ISUB should subtract the top value from the second value on the stack")
        void testIsub()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 50",
                    "BIPUSH 8",
                    "ISUB",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("IAND should perform bitwise AND on the top two values")
        void testIand()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 15",  // 1111 in binary
                    "BIPUSH 10",  // 1010 in binary
                    "IAND",       // Result: 1010 = 10
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(10, stack.get(0));
        }
        
        @Test
        @DisplayName("IOR should perform bitwise OR on the top two values")
        void testIor()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 12",  // 1100 in binary
                    "BIPUSH 5",   // 0101 in binary
                    "IOR",        // Result: 1101 = 13
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(13, stack.get(0));
        }
    }
    
    @Nested
    @DisplayName("Local Variable Instructions")
    class LocalVariableInstructionsTest
    {
        @Test
        @DisplayName("ISTORE should store a value in a local variable")
        void testIstore()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    "x",
                    ".end-var",
                    "BIPUSH 42",
                    "ISTORE x",
                    "ILOAD x",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("IINC should increment a local variable by a constant")
        void testIinc()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    "counter",
                    ".end-var",
                    "BIPUSH 40",
                    "ISTORE counter",
                    "IINC counter 2",
                    "ILOAD counter",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
    }
    
    @Nested
    @DisplayName("Control Flow Instructions")
    class ControlFlowInstructionsTest
    {
        @Test
        @DisplayName("GOTO should jump to the specified label")
        void testGoto()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 10",
                    "GOTO skip",
                    "BIPUSH 20",
                    "IADD",
                    "skip:",
                    "BIPUSH 32",
                    "IADD",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("IFEQ should jump if the top value is zero")
        void testIfeq()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 42",
                    "BIPUSH 0",
                    "IFEQ zero_branch",
                    "BIPUSH 100",  // This should be skipped
                    "zero_branch:",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("IFLT should jump if the top value is less than zero")
        void testIflt()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 42",
                    "BIPUSH -5",
                    "IFLT negative_branch",
                    "BIPUSH 100",  // This should be skipped
                    "negative_branch:",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
        
        @Test
        @DisplayName("IF_ICMPEQ should jump if the top two values are equal")
        void testIfIcmpeq()
        {
            String code = codeWritten(
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 42",
                    "BIPUSH 10",
                    "BIPUSH 10",
                    "IF_ICMPEQ equal_branch",
                    "BIPUSH 100",  // This should be skipped
                    "equal_branch:",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
    }
    
    @Nested
    @DisplayName("Method Invocation")
    class MethodInvocationTest
    {
        @Test
        @DisplayName("INVOKEVIRTUAL should call a method and return a value")
        void testInvokeVirtual()
        {
            String code = codeWritten(
                    ".constant",
                    "SQUARE 1",
                    "OBJREF 0x0",
                    ".end-constant",
                    
                    ".main",
                    ".var",
                    ".end-var",
                    "BIPUSH 6",
                    "LDC_W OBJREF",
                    "SWAP",
                    "INVOKEVIRTUAL square",
                    ".end-main",
                    
                    ".method square(n)",
                    ".var",
                    ".end-var",
                    "ILOAD n",
                    "ILOAD n",
                    "IADD",
                    "IRETURN",
                    ".end-method"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(12, stack.get(0));
        }
    }
    
    @Nested
    @DisplayName("Constant Pool")
    class ConstantPoolTest
    {
        @Test
        @DisplayName("LDC_W should load a constant from the constant pool")
        void testLdcW()
        {
            String code = codeWritten(
                    ".constant",
                    "ANSWER 42",
                    ".end-constant",
                    
                    ".main",
                    ".var",
                    ".end-var",
                    "LDC_W ANSWER",
                    ".end-main"
            );

            program.init(code);
            program.execute();

            var stack = getStackFromMain(program);
            assertEquals(1, stack.size());
            assertEquals(42, stack.get(0));
        }
    }

    @Test
    @DisplayName("Should iterate until zero")
    void testIter()
    {
        String code = codeWritten(
                ".main",
                ".var",
                "a",
                "b",
                "c",
                ".end-var",
                "BIPUSH 1000",
                "ISTORE a",
                "ILOAD a",
                "lab:",
                "    bipush 50",
                "    isub",
                "    dup",
                "    ifeq end",
                "    GOTO lab",
                "end:",
                "   ILOAD a",
                "   ISUB",
                ".end-main"
        );

        program.init(code);
        program.execute();

        var stack = getStackFromMain(program);
        assertEquals(-1000, stack.get(0));
    }
    
    @Test
    @DisplayName("Complex program with multiple instructions")
    void testComplexProgram()
    {
        String code = codeWritten(
                ".main",
                ".var",
                "sum",
                "i",
                ".end-var",
                "BIPUSH 0",
                "ISTORE sum",
                "BIPUSH 1",
                "ISTORE i",
                "loop:",
                "    ILOAD i",
                "    BIPUSH 5",
                "    IF_ICMPEQ end_loop",
                "    ILOAD sum",
                "    ILOAD i",
                "    IADD",
                "    ISTORE sum",
                "    ILOAD i",
                "    BIPUSH 1",
                "    IADD",
                "    ISTORE i",
                "    GOTO loop",
                "end_loop:",
                "    ILOAD sum",
                ".end-main"
        );

        program.init(code);
        program.execute();

        var stack = getStackFromMain(program);
        assertEquals(1, stack.size());
        assertEquals(10, stack.get(0));  // Sum of 1+2+3+4 = 10
    }

    private Stack<Integer> getStackFromMain(IJVMProgram<?> program)
    {
        return program.getScopes().get(0).getStack();
    }
}