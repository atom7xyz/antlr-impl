package xyz.atom7.interpreter;

import lombok.Getter;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.tree.TerminalNode;
import xyz.atom7.Utils;
import xyz.atom7.api.interpreter.Interpreter;
import xyz.atom7.parser.IJVMParser;
import xyz.atom7.parser.IJVMParserHelper;

import java.util.*;
import java.util.function.Consumer;

import static xyz.atom7.Utils.debugln;

@Getter
public class IJVMProgram<T extends IJVMInstruction> extends Interpreter<T>
{
    private final Map<String, Integer> constantPool;
    
    private final List<IJVMScope> scopes;
    private final Stack<IJVMScope> callStack;
    
    private Integer pendingReturnValue = null;

    public IJVMProgram()
    {
        super();
        this.constantPool = new HashMap<>();
        this.scopes = new ArrayList<>();
        this.callStack = new Stack<>();
    }

    public void init(String contents)
    {
        initInstructions();
        initProgram(contents);
    }

    @Override
    protected void initInstructions()
    {
        addInstruction("BIPUSH", (instr) -> {
            String value = instr.getArgument();
            int conv = Utils.parseInt(value);

            getCurrentScope().pushStack(conv);
            debugln("BIPUSH " + conv);
        });
        
        addInstruction("ILOAD", (instr) -> {
            String varName = instr.getArgument();
            int localValue = getCurrentScope().getLocals().get(varName);
            
            getCurrentScope().pushStack(localValue);
            debugln("ILOAD " + varName + " = " + localValue);
        });
        
        addInstruction("ISTORE", (instr) -> {
            String varName = instr.getArgument();
            int popValue = getCurrentScope().popStack();

            getCurrentScope().getLocals().replace(varName, popValue);
            debugln("ISTORE " + varName + " = " + popValue);
        });
        
        addInstruction("IADD", (instr) -> {
            int b = getCurrentScope().popStack();
            int a = getCurrentScope().popStack();

            getCurrentScope().pushStack(a + b);
            debugln("IADD " + a + " + " + b + " = " + (a+b));
        });
        
        addInstruction("ISUB", (instr) -> {
            int b = getCurrentScope().popStack();
            int a = getCurrentScope().popStack();

            getCurrentScope().pushStack(a - b);
            debugln("ISUB " + a + " - " + b + " = " + (a-b));
        });
        
        addInstruction("GOTO", (instr) -> {
            String label = instr.getArgument();
            int targetIndex = getCurrentScope().getLabelInstructionIndex(label);

            debugln("GOTO " + label + " -> index: " + targetIndex);

            if (targetIndex == -1) {
                throw new IllegalStateException("Label not found: " + label);
            }

            getCurrentScope().setPc(targetIndex - 1);
        });

        addInstruction("POP", (instr) -> {
            getCurrentScope().popStack();
            debugln("POP");
        });
        
        addInstruction("INVOKEVIRTUAL", (instr) -> {
            String methodName = instr.getArgument();
            IJVMScope targetScope = scopes.stream()
                .filter(s -> methodName.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
            
            IJVMScope currentScope = getCurrentScope();
            currentScope.setReturnPc(currentScope.getPc());
            
            int argCount = targetScope.getArgumentCount();
            Integer[] args = new Integer[argCount];
            
            for (int i = argCount - 1; i >= 0; i--) {
                args[i] = currentScope.popStack();
            }
            
            Object objRef = currentScope.popStack(); // unused, just pop the objRef
            callStack.push(targetScope);
            targetScope.setPc(-1);

            Iterator<Map.Entry<String, Integer>> iterator = targetScope.getArguments().entrySet().iterator();
            for (int i = 0; i < argCount; i++) {
                targetScope.getLocals().replace(iterator.next().getKey(), args[i]);
            }

            debugln("INVOKEVIRTUAL " + methodName);
        });
        
        addInstruction("IRETURN", (instr) -> {
            pendingReturnValue = getCurrentScope().popStack();
            debugln("IRETURN " + pendingReturnValue);
        });

        addInstruction("LABEL", (instr) -> {
            debugln("LABEL " + instr.getArgument());
        });

        addInstruction("IFLT", (instr) -> {
            int a = getCurrentScope().popStack();
            debugln("IFLT " + a + " < 0");

            if (a < 0) {
                getCurrentScope().setPc(getCurrentScope().getLabelInstructionIndex(instr.getArgument()));
            }
        });
        
        addInstruction("IFEQ", (instr) -> {
            int a = getCurrentScope().popStack();
            debugln("IFEQ " + a + " == 0");

            if (a == 0) {
                getCurrentScope().setPc(getCurrentScope().getLabelInstructionIndex(instr.getArgument()));
            }
        });

        addInstruction("LDC_W", (instr) -> {
            String constantName = instr.getArgument().toUpperCase();
            int constantValue = constantPool.get(constantName);

            getCurrentScope().pushStack(constantValue);
            debugln("LDC_W " + constantName + " = " + constantValue);
        });

        Scanner scanner = new Scanner(System.in);

        addInstruction("IN", (instr) -> {
            char value;

            if (scanner.hasNext()) {
                String input = scanner.nextLine();
                value = input.charAt(0);

                getCurrentScope().pushStack((int) value);
                debugln("IN = " + input + " (input=" + input + ", value=" + value + ")");
            }
        });

        addInstruction("OUT", (instr) -> {
            int value = getCurrentScope().popStack();

            if (Utils.DEBUG) {
                System.out.println("OUT CHAR: `" + ((char) value) + "`" + " (int equivalent is: " + value + ")");
            }
            else {
                System.out.print((char) value);
            }
        });

        addInstruction("DUP", (instr) -> {
            int value = getCurrentScope().popStack();

            getCurrentScope().pushStack(value);
            getCurrentScope().pushStack(value);
            debugln("DUP " + value);
        });

        addInstruction("SWAP", (instr) -> {
            int value1 = getCurrentScope().popStack();
            int value2 = getCurrentScope().popStack();

            getCurrentScope().pushStack(value1);
            getCurrentScope().pushStack(value2);
            debugln("SWAP " + value1 + " <-> " + value2);
        });

        addInstruction("HALT", (instr) -> {
            debugln("HALT");
            System.exit(0);
        });

        addInstruction("NOP", (instr) -> {
            debugln("NOP");
        });

        addInstruction("IINC", (instr) -> {
            int value = Utils.parseInt(instr.getArgument());
            String varName = instr.getSecondArgument();

            Map<String, Integer> locals = getCurrentScope().getLocals();
            locals.replace(varName, locals.get(varName) + value);
            
            debugln("IINC " + varName + " += " + value);
        });
        
        addInstruction("IAND", (instr) -> {
            int b = getCurrentScope().popStack();
            int a = getCurrentScope().popStack();

            getCurrentScope().pushStack(a & b);
            debugln("IAND " + a + " & " + b + " = " + (a&b));
        });

        addInstruction("IOR", (instr) -> {
            int b = getCurrentScope().popStack();
            int a = getCurrentScope().popStack();

            getCurrentScope().pushStack(a | b);
            debugln("IOR " + a + " | " + b + " = " + (a|b));
        });
        
        addInstruction("IF_ICMPEQ", (instr) -> {
            int b = getCurrentScope().popStack();
            int a = getCurrentScope().popStack();

            if (a == b) {
                getCurrentScope().setPc(getCurrentScope().getLabelInstructionIndex(instr.getArgument()));
            }
        });
    }

    @SneakyThrows
    @Override
    protected void initProgram(String contents)
    {
        IJVMParserHelper helper = new IJVMParserHelper();
        var parseResult = helper.parseString(contents);

        if (parseResult.hasSyntacticErrors() || parseResult.hasSemanticErrors()) {
            System.err.println("Program has errors, cannot interpret!");
            System.err.println(parseResult.getParserErrors());
            System.err.println(parseResult.getSemanticErrors());
            return;
        }

        var ctx = parseResult.getProgramContext();

        IJVMParser.ConstantBlockContext constantBlock = ctx.constantBlock();
        IJVMParser.MainBlockContext mainBlock = ctx.mainBlock();
        List<IJVMParser.MethodBlockContext> methodBlocks = ctx.methodBlock();

        byteCode.clear();

        if (constantBlock != null && constantBlock.constantDecl() != null) {
            constantBlock
                    .constantDecl()
                    .forEach(decl -> addConstant(decl.ID(), decl.NUM()));
        }

        IJVMScope mainScope = new IJVMScope("main");
        mainScope.populateLocalsFromVarBlock(mainBlock.varBlock(0));

        for (var statement : mainBlock.statement())
        {
            mainScope.addInstruction(statement);
        }

        scopes.add(mainScope);

        for (var methodBlock : methodBlocks)
        {
            var methodDecl = methodBlock.methodDecl();

            IJVMScope scope = new IJVMScope(methodDecl.ID().getText());

            scope.populateLocalsFromMethodParamList(methodDecl);
            scope.populateLocalsFromVarBlock(methodBlock.varBlock(0));

            for (var statement : methodBlock.statement())
            {
                scope.addInstruction(statement);
            }

            scopes.add(scope);
        }

        for (var scope : scopes)
        {
            debugln(scope.toString());
            for (IJVMInstruction instruction : scope.getInstructions())
                debugln(instruction.toString());
            debugln(" ");
        }

        for (var elem : constantPool.entrySet()) {
            debugln("const: " + elem.getKey() + " >> " + elem.getValue());
        }
    }

    @Override
    public void interpret(T instruction)
    {
        String opCode = instruction.getOpCode().toUpperCase();
        Consumer<T> handler = instructionHandlers.get(opCode.toUpperCase());
        
        if (handler != null) {
            handler.accept(instruction);
        }
        else {
            throw new IllegalArgumentException("Unknown instruction: " + opCode);
        }
    }

    @SneakyThrows
    @Override
    public void execute()
    {
        callStack.push(scopes.get(0));
        
        while (!callStack.isEmpty())
        {
            IJVMScope currentScope = getCurrentScope();
            debugln("Executing in scope: " + currentScope.getName() + ", Stack: " + currentScope.getStack());
            int pc = currentScope.getPc() + 1;
            
            if (pc >= currentScope.getInstructions().size()) {
                debugln("No more instructions in scope " + currentScope.getName() + ", popping scope");
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
            debugln("PC=" + pc + ", Next instruction: " + currentScope.getInstructions().get(pc));

            IJVMInstruction instruction = currentScope.getInstructions().get(pc);
            interpret((T) instruction);
        }
    }

    private IJVMScope getCurrentScope()
    {
        return callStack.peek();
    }

    public void addConstant(TerminalNode node1, TerminalNode node2)
    {
        constantPool.put(node1.getText().toUpperCase(), Utils.parseInt(node2.getText()));
    }

}
