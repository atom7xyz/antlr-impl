package xyz.atom7.parser.asm8088.semantic;

import lombok.Getter;
import lombok.Setter;
import xyz.atom7.api.parser.semantic.SymbolTable;

import java.util.*;

/**
 * A symbol table implementation for 8088 assembly semantic analysis.
 * Tracks labels, constants, variables, and sections.
 */
public class ASM8088SymbolTable extends SymbolTable
{
    // Symbol types in 8088 assembly
    public enum SymbolType {
        LABEL,        // Jump targets
        CONSTANT,     // Assigned constants (ID = NUM)
        SECTION,      // Sections (.SECT .DATA, .TEXT, .BSS)
        REGISTER      // Hardware registers
    }
    
    // Symbol entry to track information about each symbol
    @Getter
    public static class SymbolEntry
    {
        private final String name;
        private final SymbolType type;
        private final String section; // Which section the symbol belongs to
        @Setter
        private Integer value;        // For constants
        @Setter
        private boolean defined;      // If label/constant has been defined
        @Setter
        private boolean referenced;   // If symbol has been referenced
        
        public SymbolEntry(String name, SymbolType type, String section)
        {
            this(name, type, section, null);
        }
        
        public SymbolEntry(String name, SymbolType type, String section, Integer value)
        {
            this.name = name;
            this.type = type;
            this.section = section;
            this.value = value;
            this.defined = type == SymbolType.REGISTER; // Registers are predefined
            this.referenced = false;
        }
    }
    
    // Map to store symbols by name (using case-insensitive map)
    private final Map<String, SymbolEntry> symbols = new HashMap<>();
    
    // Map to store normalized names (lowercase) to original names for case-insensitive lookup
    private final Map<String, String> normalizedNames = new HashMap<>();

    // Current section (.DATA, .TEXT, .BSS)
    @Getter
    private String currentSection = null;
    
    // Initialize symbol table with predefined registers
    public ASM8088SymbolTable()
    {
        initializeRegisters();
    }
    
    /**
     * Initialize all CPU registers as predefined symbols
     */
    private void initializeRegisters()
    {
        // General-purpose registers
        addRegister("AX");
        addRegister("BX");
        addRegister("CX");
        addRegister("DX");
        
        // Index registers
        addRegister("SI");
        addRegister("DI");
        
        // Pointer registers
        addRegister("BP");
        addRegister("SP");
        
        // 8-bit registers
        addRegister("AL");
        addRegister("AH");
        addRegister("BL");
        addRegister("BH");
        addRegister("CL");
        addRegister("CH");
        addRegister("DL");
        addRegister("DH");
    }
    
    /**
     * Add a register to the symbol table
     */
    private void addRegister(String name)
    {
        SymbolEntry entry = new SymbolEntry(name, SymbolType.REGISTER, null);
        symbols.put(name, entry);
        normalizedNames.put(name.toLowerCase(), name);
    }
    
    /**
     * Set the current section for symbol operations
     */
    public void setCurrentSection(String section)
    {
        currentSection = section;
        
        // Add the section as a symbol if it doesn't exist
        String normalizedSection = section.toLowerCase();
        if (!normalizedNames.containsKey(normalizedSection)) {
            SymbolEntry entry = new SymbolEntry(section, SymbolType.SECTION, null);
            symbols.put(section, entry);
            normalizedNames.put(normalizedSection, section);
        }
    }

    /**
     * Add a label to the symbol table
     */
    public void addLabel(String name)
    {
        String normalizedName = name.toLowerCase();
        
        // If the symbol already exists, update it
        if (normalizedNames.containsKey(normalizedName)) {
            String originalName = normalizedNames.get(normalizedName);
            SymbolEntry entry = symbols.get(originalName);
            
            if (entry.getType() == SymbolType.LABEL && !entry.isDefined()) {
                // Forward reference is now defined
                entry.setDefined(true);
            }
        } else {
            // New label
            SymbolEntry entry = new SymbolEntry(name, SymbolType.LABEL, currentSection);
            entry.setDefined(true);
            symbols.put(name, entry);
            normalizedNames.put(normalizedName, name);
        }
    }
    
    /**
     * Add a constant to the symbol table
     */
    public void addConstant(String name, int value)
    {
        String normalizedName = name.toLowerCase();
        
        SymbolEntry entry = new SymbolEntry(name, SymbolType.CONSTANT, currentSection, value);
        entry.setDefined(true);
        symbols.put(name, entry);
        normalizedNames.put(normalizedName, name);
    }
    
    /**
     * Reference a symbol without defining it (forward reference)
     */
    public void referenceSymbol(String name)
    {
        String normalizedName = name.toLowerCase();
        
        // If the symbol already exists, mark it as referenced
        if (normalizedNames.containsKey(normalizedName)) {
            String originalName = normalizedNames.get(normalizedName);
            SymbolEntry entry = symbols.get(originalName);
            entry.setReferenced(true);
        } else {
            // Create a forward reference
            SymbolEntry entry = new SymbolEntry(name, SymbolType.LABEL, currentSection);
            entry.setReferenced(true);
            symbols.put(name, entry);
            normalizedNames.put(normalizedName, name);
        }
    }
    
    /**
     * Look up a symbol by name.
     * This method performs case-insensitive lookups.
     */
    public SymbolEntry lookupSymbol(String name)
    {
        String normalizedName = name.toLowerCase();
        
        if (normalizedNames.containsKey(normalizedName)) {
            String originalName = normalizedNames.get(normalizedName);
            return symbols.get(originalName);
        }
        
        return null;
    }
    
    /**
     * Check if a symbol exists
     */
    public boolean symbolExists(String name)
    {
        return lookupSymbol(name) != null;
    }
    
    /**
     * Check if a symbol is defined (not just a forward reference)
     */
    public boolean isSymbolDefined(String name)
    {
        SymbolEntry entry = lookupSymbol(name);
        return entry != null && entry.isDefined();
    }

    /**
     * Get all unreferenced symbols
     */
    public List<SymbolEntry> getUnreferencedSymbols()
    {
        List<SymbolEntry> unreferenced = new ArrayList<>();
        
        for (SymbolEntry entry : symbols.values())
        {
            // Skip predefined registers and sections
            if (entry.getType() == SymbolType.REGISTER || entry.getType() == SymbolType.SECTION) {
                continue;
            }
            
            if (entry.isDefined() && !entry.isReferenced()) {
                unreferenced.add(entry);
            }
        }
        
        return unreferenced;
    }
    
    /**
     * Get all undefined symbols (referenced but not defined)
     */
    public List<SymbolEntry> getUndefinedSymbols()
    {
        List<SymbolEntry> undefined = new ArrayList<>();
        
        for (SymbolEntry entry : symbols.values())
        {
            if (entry.isReferenced() && !entry.isDefined()) {
                undefined.add(entry);
            }
        }
        
        return undefined;
    }
} 