package xyz.atom7.tracer;

import lombok.Getter;
import xyz.atom7.api.tracer.Tracer;
import xyz.atom7.interpreter.IJVMInstruction;
import xyz.atom7.interpreter.IJVMProgram;
import xyz.atom7.interpreter.IJVMScope;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class IJVMTracer extends Tracer<IJVMProgram<IJVMInstruction>, IJVMInstruction, IJVMSnapshot>
{
    public IJVMTracer(IJVMProgram<IJVMInstruction> interpreter)
    {
        super(interpreter);
    }

    @Override
    public void takeSnapshot()
    {
        interpreter.halt();

        var snapshot = new IJVMSnapshot(interpreter, interpreter.getCurrentScope().getInstructions(), interpreter.getPc());
        snapshot.snapshot();

        previousSnapshot = currentSnapshot;
        currentSnapshot = snapshot;

        interpreter.resume();
    }

    @Override
    protected boolean compareSnapshots()
    {
        return Objects.deepEquals(previousSnapshot, currentSnapshot);
    }

    @Override
    public void displayTrace()
    {
        takeSnapshot();
        var snapshot = currentSnapshot;

        if (compareSnapshots() || snapshot == null) {
            return;
        }

        setStep(step + 1);

        var scopedStackDisplay = snapshot.getScopedStack().isEmpty() ?
                "empty" : snapshot.getScopedStack().toString();

        var currentInstructionTemp = interpreter.getCurrentInstruction();
        var currentInstructionDisplay = currentInstructionTemp == null ?
                "none" : currentInstructionTemp.toString();

        var pendingReturnValueDisplay = snapshot.getPendingReturnValue() == null ?
                "none" : snapshot.getPendingReturnValue().toString();

        String callStackDisplay = snapshot.getCallStack().isEmpty() ? "empty" :
                snapshot.getCallStack().stream()
                        .map(IJVMScope::getName)
                        .collect(Collectors.joining(" -> ", "[", "]"));

        String allScopesDisplay = snapshot.getScopes().isEmpty() ? "empty" :
                snapshot.getScopes().stream()
                        .map(IJVMScope::getName)
                        .collect(Collectors.joining(", ", "[", "]"));

        String constantPoolDisplay = formatMap(snapshot.getConstantPool());
        String scopedLocalsDisplay = formatMap(snapshot.getScopedLocals());

        printColoredNoValue("\n\n\n+++ TRACER (step: " + step + ") +++", ColorCode.GREEN);

        printColored("\nStatic", "", ColorCode.YELLOW);
        printColored("\tConstant Pool", constantPoolDisplay, ColorCode.YELLOW);
        printColored("\tAll Methods", allScopesDisplay, ColorCode.YELLOW);

        printColored("\nCurrent", "", ColorCode.RED);
        printColored("\tInstruction", currentInstructionDisplay, ColorCode.RED);
        if (snapshot.getCurrentScope() != null) {
            printColored("\tPC", snapshot.getCurrentScope().getPc(), ColorCode.RED);
            printColored("\tScope Name", snapshot.getCurrentScope().getName(), ColorCode.RED);
            printColored("\tScope Stack", scopedStackDisplay, ColorCode.RED);
            printColored("\tScope Locals", scopedLocalsDisplay, ColorCode.RED);
        }
        else {
            printColored("\tPC", "n/a", ColorCode.RED);
            printColored("\tScope", "none", ColorCode.RED);
            printColored("\tScope Stack", "empty", ColorCode.RED);
            printColored("\tScope Locals", "empty", ColorCode.RED);
        }

        printColored("\nFlow", "", ColorCode.BLUE);
        printColored("\tCall Stack", callStackDisplay, ColorCode.BLUE);
        printColored("\tPending Return Value", pendingReturnValueDisplay, ColorCode.BLUE);

        printColored("\tScope Stacks (from Call Stack)", "", ColorCode.BLUE);
        if (snapshot.getCallStack().isEmpty()) {
            printColored("\t\t", "empty", ColorCode.BLUE);
        }
        else {
            for (IJVMScope scope : snapshot.getCallStack())
            {
                var stack = scope.getStack();

                String stackStr = stack.isEmpty() ? "[]" : stack.toString();
                printColored("\t\t" + scope.getName(), "\t" + stackStr, ColorCode.BLUE);
            }
        }
    }

    @Getter
    private enum ColorCode
    {
        RED("31"),
        GREEN("32"),
        YELLOW("33"),
        BLUE("34"),
        PURPLE("35"),
        CYAN("36"),
        BOLD_BLUE("1;34");

        private final String code;

        ColorCode(String code)
        {
            this.code = code;
        }
    }

    private void printColoredNoValue(String label, ColorCode color)
    {
        System.out.println("\u001B[" + color.getCode() + "m" + label + " \u001B[0m");
    }

    private void printColored(String label, Object value, ColorCode color)
    {
        var valueDisplay = value == null ? "null" : value.toString();
        System.out.println("\u001B[" + color.getCode() + "m" + label + ": \u001B[0m" + valueDisplay);
    }

    private String formatMap(Map<String, Integer> map)
    {
        if (map == null || map.isEmpty()) {
            return "empty";
        }

        return map.toString()
                .replace("{", "[")
                .replace("}", "]");
    }
}
