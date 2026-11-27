package xyz.atom7.interpreter.ijvm;

import lombok.Getter;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import xyz.atom7.Utils;
import xyz.atom7.api.interpreter.Interpreter;
import xyz.atom7.parser.IJVMParser;
import xyz.atom7.parser.ijvm.IJVMParserHelper;

import java.util.*;
import java.util.function.Consumer;

import static xyz.atom7.Utils.debugln;

@Getter
public class IJVMProgram<T extends IJVMInstruction> extends Interpreter<T>
{
    private static final int MAX_OUTPUT_SUMMARY = 1_000_000;

    /**
     * Word address of the frame of main, the bottom of the stack
     */
    public static final int STACK_BASE = 0x8000;

    /**
     * Bytes before the code of a method: the parameter count and the local count, 2 bytes each
     */
    public static final int METHOD_HEADER_SIZE = 4;

    /**
     * Words of a method frame that are not parameters or locals: the link pointer, the saved PC and the saved LV
     */
    public static final int FRAME_LINK_WORDS = 3;

    private static final Set<String> TWO_BYTE_INSTRUCTIONS = Set.of("BIPUSH", "ILOAD", "ISTORE");
    private static final Set<String> THREE_BYTE_INSTRUCTIONS = Set.of(
            "GOTO", "IFEQ", "IFLT", "IF_ICMPEQ", "IINC", "LDC_W", "INVOKEVIRTUAL");

    private final Map<String, Integer> constantPool;
    
    private final List<IJVMScope> scopes;
    private final Stack<IJVMScope> callStack;

    private Integer pendingReturnValue;

    private IJVMScope scope;

    public IJVMProgram()
    {
        super();
        this.constantPool = new HashMap<>();
        this.scopes = new ArrayList<>();
        this.callStack = new Stack<>();
        this.pendingReturnValue = null;
    }

    @Override
    protected void initInstructions()
    {
        addInstruction("BIPUSH", (instr) -> {
            String value = instr.getArgument();
            int conv = Utils.parseInt(value);

            scope.pushStack(conv);
            debugln("BIPUSH " + conv);
        });
        
        addInstruction("ILOAD", (instr) -> {
            String varName = instr.getArgument();
            int localValue = scope.getLocals().get(varName);
            
            scope.pushStack(localValue);
            debugln("ILOAD " + varName + " = " + localValue);
        });
        
        addInstruction("ISTORE", (instr) -> {
            String varName = instr.getArgument();
            int popValue = scope.popStack();

            scope.getLocals().replace(varName, popValue);
            debugln("ISTORE " + varName + " = " + popValue);
        });
        
        addInstruction("IADD", (instr) -> {
            int b = scope.popStack();
            int a = scope.popStack();

            scope.pushStack(a + b);
            debugln("IADD " + a + " + " + b + " = " + (a+b));
        });
        
        addInstruction("ISUB", (instr) -> {
            int b = scope.popStack();
            int a = scope.popStack();

            scope.pushStack(a - b);
            debugln("ISUB " + a + " - " + b + " = " + (a-b));
        });
        
        addInstruction("GOTO", (instr) -> {
            String label = instr.getArgument();
            int targetIndex = scope.getLabelInstructionIndex(label);

            debugln("GOTO " + label + " -> index: " + targetIndex);

            if (targetIndex == -1) {
                throw new IllegalStateException("Label not found: " + label);
            }

            scope.setPc(targetIndex - 1);
        });

        addInstruction("POP", (instr) -> {
            scope.popStack();
            debugln("POP");
        });
        
        addInstruction("INVOKEVIRTUAL", (instr) -> {
            String methodName = instr.getArgument();
            IJVMScope targetScopeBlueprint = scopes.stream()
                .filter(s -> methodName.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method blueprint not found: " + methodName));

            scope.setReturnPc(scope.getPc());
            
            int argCount = targetScopeBlueprint.getArgumentCount();
            Integer[] args = new Integer[argCount];
            
            for (int i = argCount - 1; i >= 0; i--) {
                args[i] = scope.popStack();
            }
            
            scope.popStack(); // just pop the objRef

            IJVMScope newScopeInstance = new IJVMScope(targetScopeBlueprint);

            // The OBJREF slot becomes the link pointer, the base of the new frame.
            int lv = scope.getStackAddress(scope.getStack().size());
            newScopeInstance.setLv(lv);
            newScopeInstance.setStackBase(lv + FRAME_LINK_WORDS + newScopeInstance.getLocals().size());
            callStack.push(newScopeInstance);

            var iterator = newScopeInstance.getArguments().entrySet().iterator();
            for (int i = 0; i < argCount; i++) {
                newScopeInstance.getLocals().put(iterator.next().getKey(), args[i]);
            }

            debugln("INVOKEVIRTUAL " + methodName + " (new scope instance created)");
        });
        
        addInstruction("IRETURN", (instr) -> {
            pendingReturnValue = scope.popStack();
            scope.setPc(scope.getInstructions().size() - 1);
            debugln("IRETURN " + pendingReturnValue);
        });

        addInstruction("LABEL", (instr) -> debugln("LABEL " + instr.getArgument()));

        addInstruction("IFLT", (instr) -> {
            int a = scope.popStack();
            debugln("IFLT " + a + " < 0");

            if (a < 0) {
                scope.setPc(scope.getLabelInstructionIndex(instr.getArgument()));
            }
        });
        
        addInstruction("IFEQ", (instr) -> {
            int a = scope.popStack();
            debugln("IFEQ " + a + " == 0");

            if (a == 0) {
                scope.setPc(scope.getLabelInstructionIndex(instr.getArgument()));
            }
        });

        addInstruction("LDC_W", (instr) -> {
            String constantName = instr.getArgument().toUpperCase();
            int constantValue = constantPool.get(constantName);

            scope.pushStack(constantValue);
            debugln("LDC_W " + constantName + " = " + constantValue);
        });

        addInstruction("IN", (instr) -> {
            char value;
            String input = readInput();

            if (input != null) {
                value = input.charAt(0);

                scope.pushStack((int) value);
                debugln("IN = " + input + " (input=" + input + ", value=" + value + ")");
            }
        });

        List<Integer> outVideo = new ArrayList<>();

        addInstruction("OUT", (instr) -> {
            int value = scope.popStack();

            if (Utils.DEBUG) {
                System.out.println("OUT CHAR: `" + ((char) value) + "`" + " (int equiv. is: " + value + ")");
            }
            else {
                if (Utils.TRACER != null) {
                    System.out.println("OUTPUT: " + ((char) value) + " (int equiv. is: " + value + ")");
                }
                else {
                    writeOutput(String.valueOf((char) value));
                }
            }

            if (outVideo.size() < MAX_OUTPUT_SUMMARY) {
                outVideo.add(value);
            }
        });

        addInstruction("DUP", (instr) -> {
            int value = scope.popStack();

            scope.pushStack(value);
            scope.pushStack(value);
            debugln("DUP " + value);
        });

        addInstruction("SWAP", (instr) -> {
            int value1 = scope.popStack();
            int value2 = scope.popStack();

            scope.pushStack(value1);
            scope.pushStack(value2);
            debugln("SWAP " + value1 + " <-> " + value2);
        });

        addInstruction("HALT", (instr) -> {
            debugln("HALT");

            StringBuilder summary = new StringBuilder("\nPROGRAM OUTPUT IS:\n");
            for (int i : outVideo)
                summary.append((char) i);
            summary.append(" \n");

            writeOutput(summary.toString());
            exitProgram(0);
        });

        addInstruction("NOP", (instr) -> debugln("NOP"));

        // ERR prints an error message and halts, as in the Mic-1 simulator.
        addInstruction("ERR", (instr) -> {
            debugln("ERR");
            writeOutput("\nERROR: the program executed ERR.\n");
            exitProgram(1);
        });

        addInstruction("IINC", (instr) -> {
            int value = Utils.parseInt(instr.getArgument());
            String varName = instr.getSecondArgument();

            Map<String, Integer> locals = scope.getLocals();
            locals.replace(varName, locals.get(varName) + value);
            
            debugln("IINC " + varName + " += " + value);
        });
        
        addInstruction("IAND", (instr) -> {
            int b = scope.popStack();
            int a = scope.popStack();

            scope.pushStack(a & b);
            debugln("IAND " + a + " & " + b + " = " + (a&b));
        });

        addInstruction("IOR", (instr) -> {
            int b = scope.popStack();
            int a = scope.popStack();

            scope.pushStack(a | b);
            debugln("IOR " + a + " | " + b + " = " + (a|b));
        });
        
        addInstruction("IF_ICMPEQ", (instr) -> {
            int b = scope.popStack();
            int a = scope.popStack();

            if (a == b) {
                scope.setPc(scope.getLabelInstructionIndex(instr.getArgument()));
            }
        });
    }

    @SneakyThrows
    @Override
    protected void initProgram(String contents)
    {
        IJVMParserHelper helper = new IJVMParserHelper();
        var parseResult = helper.parseString(contents);

        if (haltIfErrors(parseResult)) {
            return;
        }

        var ctx = parseResult.getProgramContext();

        IJVMParser.ConstantBlockContext constantBlock = ctx.constantBlock();
        IJVMParser.MainBlockContext mainBlock = ctx.mainBlock();
        List<IJVMParser.MethodBlockContext> methodBlocks = ctx.methodBlock();

        if (constantBlock != null && constantBlock.constantDecl() != null) {
            constantBlock
                    .constantDecl()
                    .forEach(decl -> addConstant(decl.ID(), decl.NUM()));
        }

        IJVMScope mainScope = new IJVMScope("main");
        mainScope.populateLocalsFromVarBlock(mainBlock.varBlock(0));

        for (var statement : mainBlock.statement()) {
            mainScope.addInstruction(statement);
        }

        scopes.add(mainScope);

        for (var methodBlock : methodBlocks)
        {
            var methodDecl = methodBlock.methodDecl();

            IJVMScope scope = new IJVMScope(methodDecl.ID().getText());

            scope.populateLocalsFromMethodParamList(methodDecl);
            scope.populateLocalsFromVarBlock(methodBlock.varBlock(0));

            for (var statement : methodBlock.statement()) {
                scope.addInstruction(statement);
            }

            scopes.add(scope);
        }

        assignAddresses();

        for (var scope : scopes)
        {
            debugln(scope.toString());
            for (IJVMInstruction instruction : scope.getInstructions())
                debugln(instruction.toString());
            debugln(" ");
        }

        for (var elem : constantPool.entrySet())
            debugln("const: " + elem.getKey() + " >> " + elem.getValue());
    }

    /**
     * Lays out the methods in the method area in source order and gives each instruction its byte address.
     * main starts at address 0. Every other method starts with a header of METHOD_HEADER_SIZE bytes.
     */
    private void assignAddresses()
    {
        int address = 0;

        for (int i = 0; i < scopes.size(); i++)
        {
            if (i > 0) {
                address += METHOD_HEADER_SIZE;
            }

            for (IJVMInstruction instruction : scopes.get(i).getInstructions())
            {
                instruction.setAddress(address);
                address += instructionSize(instruction.getOpCode());
            }
        }
    }

    /**
     * Returns the size in bytes of an instruction: the opcode and its operands.
     *
     * @param opCode The operation code
     * @return The size in bytes, 0 for a label
     */
    public static int instructionSize(String opCode)
    {
        if ("label".equals(opCode)) {
            return 0;
        }

        String upper = opCode.toUpperCase();

        if (TWO_BYTE_INSTRUCTIONS.contains(upper)) {
            return 2;
        }

        return THREE_BYTE_INSTRUCTIONS.contains(upper) ? 3 : 1;
    }

    @Override
    public void interpret(T instruction)
    {
        String opCode = instruction.getOpCode().toUpperCase();
        Consumer<T> handler = instructionHandlers.get(opCode.toUpperCase());
        
        if (handler != null) {
            handler.accept(instruction);

            if (Utils.TRACER != null) {
                Utils.TRACER.displayTrace();
            }
        }
        else {
            throw new IllegalArgumentException("Unknown instruction: " + opCode);
        }
    }

    @SneakyThrows
    @Override
    public void execute()
    {
        resume();

        IJVMScope mainScope = scopes.get(0);
        mainScope.setLv(STACK_BASE);
        mainScope.setStackBase(STACK_BASE + mainScope.getLocals().size());
        callStack.push(mainScope);
        
        while (!callStack.isEmpty())
        {
            IJVMScope currentScope = getCurrentScope();
            var instructions = currentScope.getInstructions();
            String currentScopeName = currentScope.getName();

            debugln("Executing in scope: " + currentScopeName + ", Stack: " + currentScope.getStack());
            int pc = currentScope.getPc() + 1;
            
            if (pc >= instructions.size()) {
                debugln("No more instructions in scope " + currentScopeName + ", popping scope");
                callStack.pop();

                if (!callStack.isEmpty()) {
                    if (pendingReturnValue != null) {
                        getCurrentScope().pushStack(pendingReturnValue);
                        debugln("Pushed pending return value to previous scope: " + pendingReturnValue);
                        pendingReturnValue = null;
                    }

                    // Restore the PC of the calling scope to continue after INVOKEVIRTUAL
                    IJVMScope previousScope = getCurrentScope();
                    int returnPc = previousScope.getReturnPc();
                    debugln("Restoring PC for scope " + previousScope.getName() + " to: " + returnPc);
                    previousScope.setPc(returnPc);
                }

                continue;
            }
            
            currentScope.setPc(pc);

            IJVMInstruction instruction = instructions.get(pc);
            scope = currentScope;

            debugln("PC=" + pc + ", Next instruction: " + instruction);
            interpret((T) instruction);
        }
    }

    /**
     * Get the current scope of the program
     *
     * @return The current scope
     */
    public IJVMScope getCurrentScope()
    {
        return callStack.peek();
    }

    /**
     * Get the current scope of the program, or null if no scope is available
     *
     * @return The current scope, or null if not available
     */
    private IJVMInstruction getInstruction(int add)
    {
        var pc = getCurrentScope().getPc();
        var sum = pc + add;

        if (sum < 0) {
            return null;
        }

        var instructions = getCurrentScope().getInstructions();

        if (instructions.size() <= (pc + add)) {
            return null;
        }

        return instructions.get(pc + add);
    }

    /**
     * Get the current instruction being executed
     *
     * @return The current instruction
     */
    public IJVMInstruction getCurrentInstruction()
    {
        return getInstruction(0);
    }

    /**
     * Add a constant to the constant pool
     *
     * @param node1 The terminal node representing the constant name
     * @param node2 The terminal node representing the constant value
     */
    public void addConstant(@NotNull TerminalNode node1, @NotNull TerminalNode node2)
    {
        constantPool.put(node1.getText().toUpperCase(), Utils.parseInt(node2.getText()));
    }

}
