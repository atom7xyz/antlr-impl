package xyz.atom7.tracer.asm8088;

import lombok.Getter;
import xyz.atom7.api.tracer.Snapshot;
import xyz.atom7.interpreter.asm8088.ASM8088Instruction;
import xyz.atom7.interpreter.asm8088.ASM8088Program;
import xyz.atom7.interpreter.asm8088.ASM8088Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ASM8088Snapshot extends Snapshot<ASM8088Program<ASM8088Instruction>, ASM8088Instruction>
{
    private ASM8088Scope currentScope;
    
    private final Map<String, Integer> registers16;
    private final Map<String, Integer> registers8;
    private final Map<String, Integer> segmentRegisters;
    
    private final Map<String, Integer> memory;
    private final Map<String, Integer> labels;
    
    private final List<Integer> stack;
    
    private boolean zeroFlag;
    private boolean carryFlag;
    private boolean signFlag;
    private boolean overflowFlag;
    private boolean parityFlag;
    
    private int programCounter;
    
    private final List<Integer> outputBuffer;

    public ASM8088Snapshot(ASM8088Program<ASM8088Instruction> interpreter,
                           List<ASM8088Instruction> scopeInstructions,
                           int pc)
    {
        super(interpreter, scopeInstructions, pc);
        registers16 = new HashMap<>();
        registers8 = new HashMap<>();
        segmentRegisters = new HashMap<>();
        memory = new HashMap<>();
        labels = new HashMap<>();
        stack = new ArrayList<>();
        outputBuffer = new ArrayList<>();
    }

    public void snapshot()
    {
        currentScope = interpreter.getScope();

        registers16.clear();
        registers8.clear();
        segmentRegisters.clear();
        memory.clear();
        labels.clear();
        stack.clear();
        outputBuffer.clear();

        if (currentScope == null) {
            return;
        }

        programCounter = currentScope.getPc();

        registers16.put("AX", currentScope.getRegister16("AX"));
        registers16.put("BX", currentScope.getRegister16("BX"));
        registers16.put("CX", currentScope.getRegister16("CX"));
        registers16.put("DX", currentScope.getRegister16("DX"));
        registers16.put("SI", currentScope.getRegister16("SI"));
        registers16.put("DI", currentScope.getRegister16("DI"));
        registers16.put("BP", currentScope.getRegister16("BP"));
        registers16.put("SP", currentScope.getRegister16("SP"));

        registers8.put("AL", currentScope.getRegister8("AL"));
        registers8.put("AH", currentScope.getRegister8("AH"));
        registers8.put("BL", currentScope.getRegister8("BL"));
        registers8.put("BH", currentScope.getRegister8("BH"));
        registers8.put("CL", currentScope.getRegister8("CL"));
        registers8.put("CH", currentScope.getRegister8("CH"));
        registers8.put("DL", currentScope.getRegister8("DL"));
        registers8.put("DH", currentScope.getRegister8("DH"));

        segmentRegisters.put("DS", currentScope.getSegmentRegister("DS"));
        segmentRegisters.put("ES", currentScope.getSegmentRegister("ES"));
        segmentRegisters.put("SS", currentScope.getSegmentRegister("SS"));

        zeroFlag = currentScope.isZeroFlag();
        carryFlag = currentScope.isCarryFlag();
        signFlag = currentScope.isSignFlag();
        overflowFlag = currentScope.isOverflowFlag();
        parityFlag = currentScope.isParityFlag();

        captureMemoryState();

        labels.putAll(currentScope.getLabelAddresses());

        captureStackState();
    }
    
    /**
     * Captures non-zero memory locations with their addresses
     * Focuses on DATA and BSS segments to avoid showing the entire 1MB memory space
     */
    private void captureMemoryState()
    {
        byte[] memoryArray = currentScope.getMemory();
        
        // Data segment (0x1000 - 0x2000)
        for (int i = 0x1000; i < 0x2000; i++) {
            if (memoryArray[i] != 0) {
                memory.put("0x" + String.format("%04X", i), memoryArray[i] & 0xFF);
            }
        }
        
        // BSS segment (0x2000 - 0x3000)
        for (int i = 0x2000; i < 0x3000; i++) {
            if (memoryArray[i] != 0) {
                memory.put("0x" + String.format("%04X", i), memoryArray[i] & 0xFF);
            }
        }
        
        int stackSegment = currentScope.getSegmentRegister("SS");
        int stackBase = stackSegment << 4;
        
        int sp = currentScope.getRegister16("SP");
        int stackStart = Math.max(stackBase + sp - 100, stackBase);
        int stackEnd = Math.min(stackBase + sp + 100, stackBase + 0x10000);
        
        for (int i = stackStart; i < stackEnd; i++) {
            if (i >= 0 && i < memoryArray.length && memoryArray[i] != 0) {
                memory.put("0x" + String.format("%05X", i), memoryArray[i] & 0xFF);
            }
        }
    }
    
    /**
     * Captures the current stack state by reading stack memory
     */
    private void captureStackState()
    {
        int sp = currentScope.getRegister16("SP");

        // Read stack entries (words) from SP upwards until we hit empty space or invalid memory
        for (int i = 0; i < 1000; i++) 
        {
            int stackAddr = currentScope.getStackAddress(sp + i * 2);
            int word = currentScope.readWord(stackAddr);

            stack.add(word);

            // Stop if we've read a reasonable amount and hit several zeros in a row
            if (i > 20 && word == 0) {
                boolean allZeros = true;
                int lookAhead = Math.min(5, 1000 - i - 1);

                for (int j = 1; j <= lookAhead; j++)
                {
                    int nextAddr = currentScope.getStackAddress(sp + (i + j) * 2);
                    int nextWord = currentScope.readWord(nextAddr);
                    if (nextWord != 0) {
                        allZeros = false;
                        break;
                    }
                }

                if (allZeros) {
                    break;
                }
            }
        }
    }

} 