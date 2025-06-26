package xyz.atom7.api.interpreter;

import lombok.Getter;
import xyz.atom7.interpreter.ijvm.IJVMInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Getter
public abstract class Scope<T, I extends Instruction>
{
    protected final String name;
    protected final Stack<Integer> stack;
    protected final List<I> instructions;

    public Scope(String name)
    {
        this.name = name;
        this.stack = new Stack<>();
        this.instructions = new ArrayList<>();
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
     * Clear the stack
     */
    public void clearStack()
    {
        stack.clear();
    }
 
    /**
     * Add an instruction to the scope instructions list via the `adder`
     *
     * @param adder The instruction to add
     */
    public abstract void addInstruction(T adder);
}
