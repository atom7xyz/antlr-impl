package xyz.atom7.interpreter.ijvm;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.Utils;
import xyz.atom7.api.interpreter.Scope;
import xyz.atom7.parser.IJVMParser;

import java.util.LinkedHashMap;

@Getter
@Setter
public class IJVMScope extends Scope<IJVMParser.StatementContext, IJVMInstruction>
{
    private int leftAtInstruction = -1, pc = -1; // inner program counter of the scope
    private int returnPc = -1;

    private final LinkedHashMap<String, Integer> locals;
    private final LinkedHashMap<String, Integer> arguments;

    /**
     * Constructor for IJVMScope
     * 
     * @param name The name of the scope
     */
    public IJVMScope(String name)
    {
        super(name);
        locals = new LinkedHashMap<>();
        arguments = new LinkedHashMap<>();
    }

    /**
     * Copy constructor for creating new instances during calls.
     * 
     * @param toCopy The blueprint scope to copy.
     */
    public IJVMScope(IJVMScope toCopy)
    {
        super(toCopy.name);
        this.locals = new LinkedHashMap<>(toCopy.locals);
        this.arguments = new LinkedHashMap<>(toCopy.arguments);
        this.stack.addAll(toCopy.stack);
        this.instructions.addAll(toCopy.instructions);
    }

    /**
     * Add an instruction to the scope
     * 
     * @param statementCtx The statement context
     */
    @Override
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
    public int getLabelInstructionIndex(String label)
    {
        for (int i = 0; i < instructions.size(); i++)
        {
            var instruction = instructions.get(i);

            if ("label".equals(instruction.getOpCode()) &&
                label.equals(instruction.getArgument())) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public String toString()
    {
        return "Scope{" +
                "name='" + name + '\'' +
                ", leftAtInstruction=" + leftAtInstruction +
                ", pc=" + pc +
                ", locals=" + locals +
                ", stack=" + stack +
                '}';
    }
}
