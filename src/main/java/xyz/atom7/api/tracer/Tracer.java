package xyz.atom7.api.tracer;

import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import xyz.atom7.api.interpreter.Instruction;
import xyz.atom7.api.interpreter.Interpreter;

public abstract class Tracer<I extends Interpreter<T>, T extends Instruction, S extends Snapshot<I, T>>
{
    protected final I interpreter;
    @Setter
    protected int step;

    @Nullable
    protected S previousSnapshot, currentSnapshot;

    public Tracer(I interpreter)
    {
        this.interpreter = interpreter;
    }

    protected abstract boolean compareSnapshots();
    public abstract void takeSnapshot();
    public abstract void displayTrace();
}
