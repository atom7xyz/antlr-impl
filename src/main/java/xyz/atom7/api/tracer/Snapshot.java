package xyz.atom7.api.tracer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import xyz.atom7.api.interpreter.Instruction;
import xyz.atom7.api.interpreter.Interpreter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Snapshot<T extends Interpreter<I>, I extends Instruction>
{
    protected T interpreter;
    protected List<I> scopeInstructions;
    protected int pc;
}
