package xyz.atom7.interpreter.ijvm;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.Utils;
import xyz.atom7.parser.IJVMParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

@Getter
public class IJVMScope
{
    private final String name;
    @Setter
    private int leftAtInstruction = -1, pc = -1;

    private final LinkedHashMap<String, Integer> locals;
    private final LinkedHashMap<String, Integer> arguments;

    private final Stack<Integer> stack;
    private final List<IJVMInstruction> instructions;

    @Setter
    private int returnPc = -1;

    /**
     * Constructor for IJVMScope
     * 
     * @param name The name of the scope
     */
    public IJVMScope(String name)
    {
        this.name = name;
        locals = new LinkedHashMap<>();
        arguments = new LinkedHashMap<>();
        stack = new Stack<>();
        instructions = new ArrayList<>();
    }

    /**
     * Copy constructor for creating new instances during calls.
     * 
     * @param toCopy The blueprint scope to copy.
     */
    public IJVMScope(IJVMScope toCopy)
    {
        this.name = toCopy.name;

        this.locals = new LinkedHashMap<>(toCopy.locals); // New map, copies entries (refs okay for String/Integer)
        this.arguments = new LinkedHashMap<>(toCopy.arguments); // New map for arg definitions
        this.stack = new Stack<>(); // New, empty stack is crucial

        this.instructions = new ArrayList<>(toCopy.instructions); // New list shares instruction objects
    }

    /**
     * Add an instruction to the scope
     * 
     * @param statementCtx The statement context
     */
    public void addInstruction(IJVMParser.StatementContext statementCtx)
    {
        IJVMInstruction instruction = null;

        String opCode = statementCtx.getStart().getText();

        if (statementCtx.labelDecl() != null) {
            instruction = new IJVMInstruction("label", opCode);
            instructions.add(instruction);
            return;
        }

        IJVMParser.InstructionContext instructionCtx = statementCtx.instruction();

        if (instructionCtx.zeroArgInstr() != null) {
            instruction = new IJVMInstruction(opCode);
        }
        else if (instructionCtx.byteArgInstr() != null) {
            instruction = new IJVMInstruction(
                    opCode,
                    instructionCtx.byteArgInstr().NUM(),
                    instructionCtx.byteArgInstr().ID()
            );
        }
        else if (instructionCtx.jumpInstr() != null) {
            instruction = new IJVMInstruction(opCode, instructionCtx.jumpInstr().ID());
        }
        else if (instructionCtx.varArgInstr() != null) {
            instruction = new IJVMInstruction(opCode, instructionCtx.varArgInstr().ID());
        }
        else if (instructionCtx.methodArgInstr() != null) {
            instruction = new IJVMInstruction(opCode, instructionCtx.methodArgInstr().ID());
        }
        else if (instructionCtx.constantArgInstr() != null) {
            instruction = new IJVMInstruction(opCode, instructionCtx.constantArgInstr().ID());
        }

        instructions.add(instruction);
    }

    /**
     * Populate locals from a variable block
     * 
     * @param ctx The variable block context
     */
    public void populateLocalsFromVarBlock(@Nullable IJVMParser.VarBlockContext ctx)
    {
        if (ctx == null || ctx.varDecl() == null) {
            return;
        }

        ctx.varDecl().forEach(decl -> addLocal(decl.ID(), null));
    }

    /**
     * Populate locals from a method parameter list
     * 
     * @param ctx The method declaration context
     */
    public void populateLocalsFromMethodParamList(@Nullable IJVMParser.MethodDeclContext ctx)
    {
        if (ctx == null || ctx.paramList() == null) {
            return;
        }

        ctx.paramList().ID().forEach(decl -> {
            addArgument(decl);
            addLocal(decl, null);
        });
    }

    /**
     * Add a local variable to the scope
     * 
     * @param node The terminal node representing the local variable
     * @param value The value of the local variable
     */
    public void addLocal(TerminalNode node, Object value)
    {
        locals.put(node.getText(), Utils.parseInt(value));
    }

    /**
     * Add an argument to the scope
     * 
     * @param node The terminal node representing the argument
     */
    private void addArgument(TerminalNode node)
    {
        arguments.put(node.getText(), null);
    }

    /**
     * Push a value onto the stack
     * 
     * @param value The value to push onto the stack
     */
    public void pushStack(Integer value)
    {
        stack.push(value);
    }

    /**
     * Pop a value from the stack
     * 
     * @return The value popped from the stack
     */
    public Integer popStack()
    {
        return stack.pop();
    }

    /**
     * Save the leave instruction
     * 
     * @param pc The program counter
     */
    public void saveLeave(int pc)
    {
        leftAtInstruction = pc;
    }

    /**
     * Clear the locals
     */
    public void clearLocals()
    {
        locals.clear();
    }

    /**
     * Clear the stack
     */
    public void clearStack()
    {
        stack.clear();
    }

    /**
     * Get the number of arguments
     * 
     * @return The number of arguments
     */
    public int getArgumentCount()
    {
        return arguments.size();
    }

    /**
     * Get the index of a label instruction
     * 
     * @param label The label to search for
     * @return The index of the label instruction
     */
    public int getLabelInstructionIndex(String label) {
        for (int i = 0; i < instructions.size(); i++) {
            if ("label".equals(instructions.get(i).getOpCode()) && 
                label.equals(instructions.get(i).getArgument())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "Scope{" +
                "name='" + name + '\'' +
                ", leftAtInstruction=" + leftAtInstruction +
                ", pc=" + pc +
                ", locals=" + locals +
                ", stack=" + stack +
                '}';
    }
}
