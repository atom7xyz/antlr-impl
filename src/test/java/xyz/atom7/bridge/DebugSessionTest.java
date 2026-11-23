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

        assertTrue(session.step(1));
        update = awaitState(session, "PAUSED", output);
        assertEquals(1, update.getProgramCounter());

        List<String> keys = Arrays.asList(update.getDelta().getChangedKeys().split("\n"));
        assertTrue(keys.contains("STACK:0_main.stack.[0]"));

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
