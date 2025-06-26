package xyz.atom7.interpreter.asm8088;

import lombok.Getter;
import lombok.Setter;
import xyz.atom7.Utils;
import xyz.atom7.api.interpreter.Scope;

import java.util.*;

import static xyz.atom7.Utils.debugln;

/**
 * ASM8088Scope represents the execution environment for 8088 assembly programs.
 * This class manages the processor state including registers, memory, flags, and symbol table.
 * It uses a byte array for efficient register storage and implements proper 8088 segmented addressing.
 */
@Getter
@Setter
public class ASM8088Scope extends Scope<Object, ASM8088Instruction>
{
    private int pc = -1;
    
    private final byte[] memory = new byte[0x100000];
    private final byte[] registers = new byte[16];
    
    private static final int AX_OFFSET = 0;
    private static final int BX_OFFSET = 2;
    private static final int CX_OFFSET = 4;
    private static final int DX_OFFSET = 6;
    private static final int SI_OFFSET = 8;
    private static final int DI_OFFSET = 10;
    private static final int BP_OFFSET = 12;
    private static final int SP_OFFSET = 14;
    
    private int DS = 0, ES = 0, SS = 0;
    
    private boolean zeroFlag = false;
    private boolean carryFlag = false;
    private boolean signFlag = false;
    private boolean overflowFlag = false;
    private boolean parityFlag = false;
    
    private int dataSegmentStart = 0x1000;
    private int bssSegmentStart = 0x2000;
    private int currentDataOffset = 0;
    private int currentBssOffset = 0;
    
    private final Map<String, Integer> labelAddresses = new HashMap<>();

    /**
     * Constructor for ASM8088Scope
     * Initializes memory, registers, and sets up the initial processor state
     * 
     * @param name The name of the scope
     */
    public ASM8088Scope(String name)
    {
        super(name);
        initializeRegisters();
    }

    /**
     * Abstract method implementation - not used in ASM8088 as instructions are handled separately
     * 
     * @param adder The instruction to add (unused in this implementation)
     */
    @Override
    public void addInstruction(Object adder)
    {
        // Instructions are handled in a separate list structure, not in this scope
    }
    
    /**
     * Initializes all 8088 registers to their default values
     * Sets up segment registers and stack pointer according to 8088 conventions
     */
    private void initializeRegisters() 
    {
        DS = dataSegmentStart >> 4;     // Data segment points to data area
        ES = 0;                         // Extra segment starts at 0
        SS = 0xF000 >> 4;               // Stack segment in high memory

        setRegister16("SP", 0xFFFF);    // Stack pointer starts at top of segment
    }
    
    /**
     * Retrieves the value of a 16-bit register
     * Uses little-endian byte ordering (low byte first, high byte second)
     * 
     * @param reg The register name (AX, BX, CX, DX, SI, DI, BP, SP)
     * @return The 16-bit register value
     * @throws IllegalArgumentException if the register name is invalid
     */
    public int getRegister16(String reg) 
    {
        int offset = getRegisterOffset(reg);
        return ((registers[offset + 1] & 0xFF) << 8) | (registers[offset] & 0xFF);
    }
    
    /**
     * Sets the value of a 16-bit register
     * Uses little-endian byte ordering for storage
     * 
     * @param reg The register name (AX, BX, CX, DX, SI, DI, BP, SP)
     * @param value The 16-bit value to set (will be masked to 16 bits)
     * @throws IllegalArgumentException if the register name is invalid
     */
    public void setRegister16(String reg, int value)
    {
        int offset = getRegisterOffset(reg);
        registers[offset] = (byte) (value & 0xFF);
        registers[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private final Map<String, Integer> registerMap = Map.of(
        "AL", AX_OFFSET,
        "AH", AX_OFFSET + 1,
        "BL", BX_OFFSET,
        "BH", BX_OFFSET + 1,
        "CL", CX_OFFSET,
        "CH", CX_OFFSET + 1,
        "DL", DX_OFFSET,
        "DH", DX_OFFSET + 1
    );
    
    /**
     * Retrieves the value of an 8-bit register
     * Handles both low (L) and high (H) byte access for 16-bit registers
     * 
     * @param reg The 8-bit register name (AL, AH, BL, BH, CL, CH, DL, DH)
     * @return The 8-bit register value (0-255)
     * @throws IllegalArgumentException if the register name is invalid
     */
    public int getRegister8(String reg) 
    {
        Integer offset = registerMap.get(reg);

        if (offset == null) {
            throw new IllegalArgumentException("Invalid 8-bit register: " + reg);
        }

        return registers[offset] & 0xFF;
    }
    
    /**
     * Sets the value of an 8-bit register
     * Only affects the specified byte, leaving the other byte of the 16-bit register unchanged
     * 
     * @param reg The 8-bit register name (AL, AH, BL, BH, CL, CH, DL, DH)
     * @param value The 8-bit value to set (will be masked to 8 bits)
     * @throws IllegalArgumentException if the register name is invalid
     */
    public void setRegister8(String reg, int value)
    {
        Integer offset = registerMap.get(reg);

        if (offset == null) {
            throw new IllegalArgumentException("Invalid 8-bit register: " + reg);
        }

        registers[offset] = (byte) (value & 0xFF);
    }
    
    /**
     * Retrieves the value of a segment register
     * Segment registers are used for memory addressing in the 8088's segmented memory model
     * 
     * @param reg The segment register name (DS, ES, SS)
     * @return The 16-bit segment register value
     * @throws IllegalArgumentException if the register name is invalid
     */
    public int getSegmentRegister(String reg) 
    {
        switch (reg) {
            case "DS": return DS;
            case "ES": return ES;
            case "SS": return SS;
            default: throw new IllegalArgumentException("Invalid segment register: " + reg);
        }
    }
    
    /**
     * Sets the value of a segment register
     * Segment registers are masked to 16 bits as per 8088 architecture
     * 
     * @param reg The segment register name (DS, ES, SS)
     * @param value The 16-bit segment value (will be masked to 16 bits)
     * @throws IllegalArgumentException if the register name is invalid
     */
    public void setSegmentRegister(String reg, int value) 
    {
        value &= 0xFFFF;
        switch (reg) {
            case "DS": DS = value; break;
            case "ES": ES = value; break;
            case "SS": SS = value; break;
            default: throw new IllegalArgumentException("Invalid segment register: " + reg);
        }
    }
    
    private final Map<String, Integer> registers16OffsetMap = Map.of(
        "AX", AX_OFFSET,
        "BX", BX_OFFSET,
        "CX", CX_OFFSET,
        "DX", DX_OFFSET,
        "SI", SI_OFFSET,
        "DI", DI_OFFSET,
        "BP", BP_OFFSET,
        "SP", SP_OFFSET
    );

    /**
     * Maps register names to their byte array offsets
     * This provides efficient access to register storage locations
     * 
     * @param reg The 16-bit register name
     * @return The byte offset in the registers array
     * @throws IllegalArgumentException if the register name is invalid
     */
    private int getRegisterOffset(String reg) 
    {
        Integer offset = registers16OffsetMap.get(reg);

        if (offset == null) {
            throw new IllegalArgumentException("Invalid 16-bit register: " + reg);
        }

        return offset;
    }

    /**
     * Reads a single byte from memory at the specified address
     * Includes bounds checking to prevent memory access violations
     * 
     * @param address The memory address to read from (0 to 0xFFFFF)
     * @return The byte value at the specified address
     * @throws IndexOutOfBoundsException if the address is outside valid memory range
     */
    public byte readByte(int address)
    {
        if (address < 0 || address >= memory.length) {
            throw new IndexOutOfBoundsException("Address: " + Integer.toHexString(address));
        }

        return memory[address];
    }

    /**
     * Writes a single byte to memory at the specified address
     * Includes bounds checking to prevent memory access violations
     * 
     * @param address The memory address to write to (0 to 0xFFFFF)
     * @param value The byte value to write
     * @throws IndexOutOfBoundsException if the address is outside valid memory range
     */
    public void writeByte(int address, byte value) 
    {
        if (address < 0 || address >= memory.length) {
            throw new IndexOutOfBoundsException("Address: " + Integer.toHexString(address));
        }

        memory[address] = value;
    }

    /**
     * Reads a 16-bit word from memory using little-endian byte ordering
     * The 8088 uses little-endian format: low byte at address, high byte at address+1
     * 
     * @param address The memory address to read from
     * @return The 16-bit word value
     * @throws IndexOutOfBoundsException if the address range is outside valid memory
     */
    public int readWord(int address) 
    {
        return (readByte(address) & 0xFF) | ((readByte(address + 1) & 0xFF) << 8);
    }

    /**
     * Writes a 16-bit word to memory using little-endian byte ordering
     * Stores low byte first, then high byte (8088 convention)
     * 
     * @param address The memory address to write to
     * @param value The 16-bit word value to write
     * @throws IndexOutOfBoundsException if the address range is outside valid memory
     */
    public void writeWord(int address, int value) 
    {
        writeByte(address, (byte) (value & 0xFF));
        writeByte(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    /**
     * Calculates a 20-bit physical address from segment:offset pair
     * Uses the 8088 segmented addressing formula: (segment << 4) + offset
     * This allows addressing 1MB of memory with 16-bit registers
     * 
     * @param segment The 16-bit segment value
     * @param offset The 16-bit offset value
     * @return The 20-bit physical address
     */
    public int calculatePhysicalAddress(int segment, int offset) 
    {
        return ((segment & 0xFFFF) << 4) + (offset & 0xFFFF);
    }
    
    /**
     * Alias for calculatePhysicalAddress for consistency with naming conventions
     * 
     * @param segment The 16-bit segment value
     * @param offset The 16-bit offset value
     * @return The 20-bit physical address
     */
    public int getPhysicalAddress(int segment, int offset) 
    {
        return calculatePhysicalAddress(segment, offset);
    }
    
    /**
     * Calculates a stack address using the stack segment (SS)
     * Used for stack operations and BP-relative addressing
     * 
     * @param offset The 16-bit offset within the stack segment
     * @return The 20-bit physical address
     */
    public int getStackAddress(int offset) 
    {
        return getPhysicalAddress(SS, offset);
    }

    /**
     * Processes the .DATA section of an assembly program
     * Handles .BYTE and .ASCII directives to initialize data in memory
     * Updates the symbol table with label addresses for program references
     * 
     * @param dataLines List of strings containing data section declarations
     */
    public void processSectData(List<String> dataLines)
    {
        currentDataOffset = 0;
        
        debugln("=== DATA SECTION PROCESSING ===");

        for (String line : dataLines)
        {
            debugln("Processing data line: '" + line + "'");
            line = line.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("//")) {
                debugln("  -> Skipping empty/comment line");
                continue;
            }
            
            if (line.contains(".BYTE")) {
                processDataByte(line);
            } 
            else if (line.contains(".ASCII")) {
                processDataAscii(line);
            } 
            else {
                debugln("  -> Unknown data directive: " + line);
            }
        }
        
        debugln("=== END DATA SECTION PROCESSING ===\n");
    }
    
    /**
     * Processes the .BSS section of an assembly program
     * Handles .SPACE directive to reserve uninitialized memory space
     * Updates the symbol table with label addresses for program references
     * 
     * @param bssLines List of strings containing BSS section declarations
     */
    public void processSectBss(List<String> bssLines)
    {
        currentBssOffset = 0;
        
        for (String line : bssLines) 
        {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("//")) {
                continue;
            }
            
            if (line.contains(".SPACE")) {
                processReserveSpace(line);
            }
        }
    }
    
    /**
     * Processes a .BYTE directive from the data section
     * Parses byte values and stores them in memory starting at the data segment
     * Supports decimal, hexadecimal, and character literal formats
     * 
     * @param line The assembly line containing the .BYTE directive
     */
    private void processDataByte(String line)
    {
        String[] parts = line.split(":");
        if (parts.length < 2) {
            debugln("\tERROR: No ':' found in line, expecting '<label>: .BYTE <values>'");
            return;
        }

        String label = parts[0].trim();
        String remaining = parts[1].trim();

        if (!remaining.contains(".BYTE")) {
            debugln("\tERROR: No .BYTE directive found in: " + remaining);
            return;
        }

        String values = remaining.substring(remaining.indexOf(".BYTE") + 5).trim();

        debugln("\tLabel: '" + label + "'");
        debugln("\tValues: '" + values + "'");
        debugln("\tAddress (head): "
                + (dataSegmentStart + currentDataOffset)
                + " (0x" + Integer.toHexString(dataSegmentStart + currentDataOffset)
                + ")");

        storeLabelAddress(label, dataSegmentStart + currentDataOffset);

        String[] byteValues = values.split(",");

        for (String value : byteValues)
        {
            value = value.trim();
            byte byteVal = Utils.parseByte(value);

            debugln("\tStoring byte: " + value + " -> "
                    + (byteVal & 0xFF) + " at address "
                    + (dataSegmentStart + currentDataOffset));
            memory[dataSegmentStart + currentDataOffset] = byteVal;
            currentDataOffset++;
        }
    }
    
    /**
     * Processes a .ASCII directive from the data section
     * Parses ASCII string and stores each character as a byte in memory
     * Handles escape sequences and null termination
     * 
     * @param line The assembly line containing the .ASCII directive
     */
    private void processDataAscii(String line)
    {
        String[] parts = line.split(":");
        String label = parts[0].trim();
        String asciiString = line.substring(line.indexOf(".ASCII") + 6).trim();
        
        // Remove quotes and handle escape sequences
        if (asciiString.startsWith("\"") && asciiString.endsWith("\"")) {
            asciiString = asciiString.substring(1, asciiString.length() - 1);
        }
        
        storeLabelAddress(label, dataSegmentStart + currentDataOffset);
        
        // Process string with escape sequences
        for (int i = 0; i < asciiString.length(); i++) 
        {
            char c = asciiString.charAt(i);
            if (c == '\\' && i + 1 < asciiString.length()) {
                char next = asciiString.charAt(i + 1);
                switch (next) {
                    case '0': c = '\0'; i++; break;  // Null terminator
                    case 'n': c = '\n'; i++; break;  // Newline
                    case 't': c = '\t'; i++; break;  // Tab
                    case 'r': c = '\r'; i++; break;  // Carriage return
                    case '\\': c = '\\'; i++; break; // Backslash
                    case '"': c = '"'; i++; break;   // Quote
                }
            }
            memory[dataSegmentStart + currentDataOffset] = (byte) c;
            currentDataOffset++;
        }
    }
    
    /**
     * Processes a .SPACE directive from the BSS section
     * Reserves the specified number of bytes in the BSS segment
     * Memory is not initialized (remains zero from initial memory setup)
     * 
     * @param line The assembly line containing the .SPACE directive
     */
    private void processReserveSpace(String line) 
    {
        String[] parts = line.split(":");
        String label = parts[0].trim();
        String spaceStr = line.substring(line.indexOf(".SPACE") + 6).trim();
        int count = Integer.parseInt(spaceStr);
        
        storeLabelAddress(label, bssSegmentStart + currentBssOffset);
        currentBssOffset += count;
    }

    /**
     * Stores a label address in the symbol table
     * Used by assembler directives to track memory locations of labels
     * 
     * @param label The label name
     * @param address The memory address associated with the label
     */
    private void storeLabelAddress(String label, int address) 
    {
        labelAddresses.put(label, address);
    }
    
    /**
     * Retrieves the memory address associated with a label
     * Used during instruction execution to resolve label references
     * 
     * @param label The label name to look up
     * @return The memory address of the label, or -1 if not found
     */
    public int getLabelAddress(String label)
    {
        return labelAddresses.getOrDefault(label, -1);
    }
    
    /**
     * Updates processor flags based on an arithmetic or logical operation result
     * Sets zero, sign, carry, overflow, and parity flags according to 8088 processor behavior
     * 
     * @param result The result of the operation
     * @param is16Bit True if the operation was 16-bit, false if 8-bit
     */
    public void updateFlags(int result, boolean is16Bit) 
    {
        if (is16Bit) {
            zeroFlag = (result & 0xFFFF) == 0;
            signFlag = (result & 0x8000) != 0;
            carryFlag = (result & 0x10000) != 0;
            // Parity flag is based on the low byte only
            parityFlag = calculateParity(result & 0xFF);
        } else {
            zeroFlag = (result & 0xFF) == 0;
            signFlag = (result & 0x80) != 0;
            carryFlag = (result & 0x100) != 0;
            parityFlag = calculateParity(result & 0xFF);
        }
    }

    /**
     * Updates flags specifically for arithmetic operations that can generate overflow
     * 
     * @param operand1 First operand
     * @param operand2 Second operand  
     * @param result The result of the operation
     * @param is16Bit True if the operation was 16-bit, false if 8-bit
     * @param isSubtraction True if this was a subtraction operation
     */
    public void updateArithmeticFlags(int operand1, int operand2, int result, boolean is16Bit, boolean isSubtraction)
    {
        updateFlags(result, is16Bit);
        
        if (is16Bit) {
            // Check for signed overflow in 16-bit operations
            if (isSubtraction) {
                overflowFlag = ((operand1 & 0x8000) != (operand2 & 0x8000)) && 
                              ((operand1 & 0x8000) != (result & 0x8000));
            } else {
                overflowFlag = ((operand1 & 0x8000) == (operand2 & 0x8000)) && 
                              ((operand1 & 0x8000) != (result & 0x8000));
            }
        } else {
            // Check for signed overflow in 8-bit operations
            if (isSubtraction) {
                overflowFlag = ((operand1 & 0x80) != (operand2 & 0x80)) && 
                              ((operand1 & 0x80) != (result & 0x80));
            } else {
                overflowFlag = ((operand1 & 0x80) == (operand2 & 0x80)) && 
                              ((operand1 & 0x80) != (result & 0x80));
            }
        }
    }

    /**
     * Calculates the parity flag for a byte value
     * Parity flag is set if the number of 1 bits in the low byte is even
     * 
     * @param value The byte value to check
     * @return True if parity is even (parity flag set)
     */
    private boolean calculateParity(int value)
    {
        int count = 0;
        value &= 0xFF;
        
        for (int i = 0; i < 8; i++) {
            if ((value & (1 << i)) != 0) {
                count++;
            }
        }
        
        return (count % 2) == 0;
    }

    @Override
    public Integer popStack()
    {
        int sp = getRegister16("SP");
        int stackAddr = getStackAddress(sp);
        int word = readWord(stackAddr);

        sp = (sp + 2) & 0xFFFF;
        setRegister16("SP", sp);

        return word;
    }

    public void pushStack(int value)
    {
        int sp = getRegister16("SP");
        int stackAddr = getStackAddress(sp - 2);

        writeWord(stackAddr, value);

        sp = (sp - 2) & 0xFFFF;
        setRegister16("SP", sp);
    }

    /**
     * Sets the overflow flag
     * 
     * @param overflowFlag The overflow flag value
     */
    public void setOverflowFlag(boolean overflowFlag) {
        this.overflowFlag = overflowFlag;
    }

    /**
     * Sets the parity flag
     * 
     * @param parityFlag The parity flag value
     */
    public void setParityFlag(boolean parityFlag) {
        this.parityFlag = parityFlag;
    }

    /**
     * Provides a string representation of the scope's current state
     * Includes program counter, flags, and segment register values in hexadecimal
     * 
     * @return A formatted string showing the scope's state
     */
    @Override
    public String toString()
    {
        return "ASM8088Scope{" +
                "name='" + name + '\'' +
                ", pc=" + pc +
                ", zeroFlag=" + zeroFlag +
                ", carryFlag=" + carryFlag +
                ", signFlag=" + signFlag +
                ", overflowFlag=" + overflowFlag +
                ", parityFlag=" + parityFlag +
                ", DS=" + Integer.toHexString(DS) +
                ", ES=" + Integer.toHexString(ES) +
                ", SS=" + Integer.toHexString(SS) +
                '}';
    }
} 