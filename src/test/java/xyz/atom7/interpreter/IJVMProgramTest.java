package xyz.atom7.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.atom7.interpreter.ijvm.IJVMInstruction;
import xyz.atom7.interpreter.ijvm.IJVMProgram;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static xyz.atom7.Utils.codeWritten;

public class IJVMProgramTest
{
    private IJVMProgram<IJVMInstruction> program;

    @BeforeEach
    void setUp()
    {
        program = new IJVMProgram<>();
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