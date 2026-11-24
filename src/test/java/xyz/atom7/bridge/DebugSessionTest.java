package xyz.atom7.bridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.atom7.interpreter.asm8088.ASM8088Instruction;
import xyz.atom7.interpreter.asm8088.ASM8088Program;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.atom7.Utils.codeWritten;

public class DebugSessionTest
{
    private static final long TIMEOUT_MILLIS = 5000;

    /**
     * Reads updates until the session reaches the given state.
     *
     * @param session The session to read
     * @param state The expected state
     * @param output The buffer that receives the program output
     * @return The update with the expected state
     */
    private StateUpdate awaitState(DebugSession session, String state, StringBuilder output) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline)
        {
            StateUpdate update = session.awaitUpdate(100);

            if (update == null) {
                continue;
            }

            output.append(update.getOutput());

            if (state.equals(update.getState())) {
                return update;
            }

            if (update.isTerminal()) {
                return fail("Session ended with " + update.getState() + ": " + update.getMessage());
            }
        }

        return fail("Session did not reach state " + state);
    }

    @Test
    @DisplayName("IRETURN leaves the method immediately")
    void testIReturnLeavesMethod() throws InterruptedException
    {
        String code = codeWritten(
                ".constant",
                "objref 0",
                ".end-constant",
                ".main",
                "LDC_W objref",
                "BIPUSH 1",
                "INVOKEVIRTUAL f",
                "OUT",
                "HALT",
                ".end-main",
                ".method f(x)",
                "BIPUSH 65",
                "IRETURN",
                "BIPUSH 66",
                "OUT",
                ".end-method"
        );

        DebugSession session = DebugSession.create("ijvm", code);
        StringBuilder output = new StringBuilder();

        session.start(false);
        awaitState(session, "FINISHED", output);

        assertEquals("A\nPROGRAM OUTPUT IS:\nA \n", output.toString());
    }

    @Test
    @DisplayName("Debug mode executes one instruction per step")
    void testStep() throws InterruptedException
    {
        String code = codeWritten(".main", "BIPUSH 2", "BIPUSH 3", "IADD", "HALT", ".end-main");

        DebugSession session = DebugSession.create("ijvm", code);
        StringBuilder output = new StringBuilder();

        session.start(true);
        StateUpdate update = awaitState(session, "PAUSED", output);
        assertEquals(0, update.getProgramCounter());
        assertEquals(2, update.getLine());

        assertTrue(session.step(1));
        update = awaitState(session, "PAUSED", output);
        assertEquals(2, update.getProgramCounter());
        assertEquals(3, update.getLine());

        List<String> keys = Arrays.asList(update.getDelta().getChangedKeys().split("\n"));
        assertTrue(keys.contains("STACK:main().0x8000"));

        assertTrue(session.step(10));
        awaitState(session, "FINISHED", output);
    }

    @Test
    @DisplayName("Input resumes a waiting program")
    void testInput() throws InterruptedException
    {
        String code = codeWritten(".main", "IN", "OUT", "HALT", ".end-main");

        DebugSession session = DebugSession.create("ijvm", code);
        StringBuilder output = new StringBuilder();

        session.start(false);
        awaitState(session, "WAITING_INPUT", output);
        assertTrue(session.feed("xyz"));
        awaitState(session, "FINISHED", output);

        assertEquals("x\nPROGRAM OUTPUT IS:\nx \n", output.toString());
    }

    @Test
    @DisplayName("Input is read one character per instruction without a newline")
    void testInputCharacters() throws InterruptedException
    {
        String code = codeWritten(".main", "IN", "OUT", "IN", "OUT", "IN", "OUT", "HALT", ".end-main");

        DebugSession session = DebugSession.create("ijvm", code);
        StringBuilder output = new StringBuilder();

        session.start(false);
        awaitState(session, "WAITING_INPUT", output);
        assertFalse(session.feed(""));
        assertTrue(session.feed("ab"));
        awaitState(session, "WAITING_INPUT", output);
        assertTrue(session.feed(";"));
        awaitState(session, "FINISHED", output);

        assertEquals("ab;\nPROGRAM OUTPUT IS:\nab; \n", output.toString());
    }

    @Test
    @DisplayName("Input rejects characters above the largest byte value")
    void testInputLimit() throws InterruptedException
    {
        String code = codeWritten(".main", "IN", "OUT", "HALT", ".end-main");

        DebugSession session = DebugSession.create("ijvm", code);

        session.start(false);
        awaitState(session, "WAITING_INPUT", new StringBuilder());

        assertFalse(session.feed("\u20AC"));
        assertTrue(session.feed("\u00FF"));
        session.stop();
    }

    @Test
    @DisplayName("A called IJVM method is shown inside its caller with stack addresses")
    void testNestedFrames() throws InterruptedException
    {
        String code = codeWritten(
                ".constant",
                "objref 0",
                ".end-constant",
                ".main",
                ".var",
                "a",
                ".end-var",
                "LDC_W objref",
                "BIPUSH 7",
                "INVOKEVIRTUAL f",
                "ISTORE a",
                "HALT",
                ".end-main",
                ".method f(x)",
                "LDC_W objref",
                "ILOAD x",
                "INVOKEVIRTUAL g",
                "IRETURN",
                ".end-method",
                ".method g(y)",
                ".var",
                "z",
                ".end-var",
                "ILOAD y",
                "IRETURN",
                ".end-method"
        );

        DebugSession session = DebugSession.create("ijvm", code);
        StringBuilder output = new StringBuilder();

        session.start(true);
        awaitState(session, "PAUSED", output);

        // LDC_W, BIPUSH, INVOKEVIRTUAL f, LDC_W, ILOAD x, INVOKEVIRTUAL g
        assertTrue(session.step(6));
        StateUpdate update = awaitState(session, "PAUSED", output);
        List<String> keys = Arrays.asList(update.getDelta().getChangedKeys().split("\n"));

        // main: a at 0x8000, stack from 0x8001. f: LV 0x8001, x, then 3 link words, stack from 0x8005.
        // g: LV 0x8005, y, z, then 3 link words, stack from 0x800A.
        assertTrue(keys.contains("STACK:main().f()"));
        assertTrue(keys.contains("STACK:main().f().x"));
        assertTrue(keys.contains("STACK:main().f().g()"));
        assertTrue(keys.contains("STACK:main().f().g().y"));
        assertTrue(keys.contains("STACK:main().f().g().z"));
        assertFalse(keys.stream().anyMatch(key -> key.contains("locals") || key.contains(".stack")));

        // The first instruction of g follows main (11 bytes), the header of f, f (9 bytes) and the header of g.
        assertEquals(11 + 4 + 9 + 4, update.getProgramCounter());

        assertTrue(session.step(1));
        update = awaitState(session, "PAUSED", output);
        keys = Arrays.asList(update.getDelta().getChangedKeys().split("\n"));
        assertTrue(keys.contains("STACK:main().f().g().0x800A"));

        session.stop();
    }

    @Test
    @DisplayName("8088 CALL pushes the byte address of the next instruction")
    void test8088CallAddress() throws InterruptedException
    {
        String code = codeWritten(
                ".SECT .TEXT",
                "MOV AX, 1",
                "CALL work",
                "HLT",
                "work:",
                "PUSH AX",
                "POP AX",
                "RET"
        );

        DebugSession session = DebugSession.create("8088", code);
        StringBuilder output = new StringBuilder();

        session.start(true);
        awaitState(session, "PAUSED", output);

        // MOV AX, 1 is 3 bytes and CALL is 3 bytes, so the return address is 6.
        assertTrue(session.step(2));
        StateUpdate update = awaitState(session, "PAUSED", output);
        assertEquals(7, update.getProgramCounter());
        assertEquals(6, update.getLine());

        List<String> keys = Arrays.asList(update.getDelta().getChangedKeys().split("\n"));
        int index = keys.indexOf("STACK:work().0xFFFD");
        assertTrue(index >= 0);
        assertEquals(6, update.getDelta().getChangedValues()[index]);

        assertTrue(session.step(10));
        awaitState(session, "FINISHED", output);
    }

    @Test
    @DisplayName("Stop ends an infinite loop")
    void testStop() throws InterruptedException
    {
        String code = codeWritten(".main", "L1:", "NOP", "GOTO L1", ".end-main");

        DebugSession session = DebugSession.create("ijvm", code);

        session.start(false);
        session.stop();

        assertTrue(awaitState(session, "STOPPED", new StringBuilder()).isTerminal());
    }

    @Test
    @DisplayName("Syntax errors fail the session")
    void testSyntaxError() throws InterruptedException
    {
        DebugSession session = DebugSession.create("ijvm", ".main\nNOTANOPCODE 1 2 3\n");

        session.start(false);
        StateUpdate update = awaitState(session, "FAILED", new StringBuilder());

        assertNotNull(update.getMessage());
    }

    @Test
    @DisplayName("HLT ends the program with the same output in CLI and session")
    void testHltParity() throws InterruptedException
    {
        String code = codeWritten(
                ".SECT .TEXT",
                "MOV AX, 7",
                "HLT",
                "MOV AX, 9"
        );

        CliProgram cli = new CliProgram();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(captured));
            cli.init(code);
            assertThrows(CliExit.class, cli::execute);
        }
        finally {
            System.setOut(originalOut);
        }

        DebugSession session = DebugSession.create("8088", code);
        StringBuilder output = new StringBuilder();

        session.start(false);
        StateUpdate update = awaitState(session, "FINISHED", output);

        assertEquals(7, cli.getScope().getRegister16("AX"));
        assertEquals(captured.toString(), output.toString());
        assertEquals("\nProgram halted.\n", output.toString());
        assertEquals(0, update.getExitCode());
    }

    private static final class CliExit extends RuntimeException
    {
    }

    private static final class CliProgram extends ASM8088Program<ASM8088Instruction>
    {
        @Override
        protected void exitProgram(int exitCode)
        {
            throw new CliExit();
        }
    }

    @Test
    @DisplayName("8088 exit does not terminate the JVM")
    void test8088Exit() throws InterruptedException
    {
        String code = codeWritten(
                "_EXIT = 1",
                ".SECT .TEXT",
                "MOV AX, 7",
                "PUSH AX",
                "PUSH _EXIT",
                "SYS"
        );

        DebugSession session = DebugSession.create("8088", code);

        session.start(false);
        StateUpdate update = awaitState(session, "FINISHED", new StringBuilder());

        assertEquals(7, update.getExitCode());
    }
}
