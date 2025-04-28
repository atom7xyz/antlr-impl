package xyz.atom7.tracer;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.api.tracer.Snapshot;
import xyz.atom7.interpreter.IJVMInstruction;
import xyz.atom7.interpreter.IJVMProgram;
import xyz.atom7.interpreter.IJVMScope;

import java.util.*;

@Getter
public class IJVMSnapshot extends Snapshot<IJVMProgram<IJVMInstruction>, IJVMInstruction>
{
    private final Stack<IJVMScope> scopes;
    private final Stack<IJVMScope> callStack;
    private final Map<String, Integer> constantPool;
    private final LinkedList<Stack<Integer>> stacks;
    private final Stack<Integer> scopedStack;
    private final Map<String, Integer> scopedLocals;

    @Nullable
    private Integer pendingReturnValue;
    private IJVMScope currentScope;

    public IJVMSnapshot(IJVMProgram<IJVMInstruction> interpreter, List<IJVMInstruction> scopeInstructions, int pc)
    {
        super(interpreter, scopeInstructions, pc);
        scopes = new Stack<>();
        callStack = new Stack<>();
        constantPool = new HashMap<>();
        stacks = new LinkedList<>();
        scopedStack = new Stack<>();
        scopedLocals = new HashMap<>();
    }

    public void snapshot()
    {
        pendingReturnValue = interpreter.getPendingReturnValue();
        currentScope = interpreter.getCurrentScope();

        scopes.clear();
        callStack.clear();
        constantPool.clear();
        stacks.clear();
        scopedStack.clear();

        scopes.addAll(interpreter.getScopes());
        callStack.addAll(interpreter.getCallStack());
        constantPool.putAll(interpreter.getConstantPool());
        callStack.forEach(scope -> stacks.add(scope.getStack()));
        scopedStack.addAll(interpreter.getCurrentScope().getStack());
        scopedLocals.putAll(interpreter.getCurrentScope().getLocals());
    }

}
