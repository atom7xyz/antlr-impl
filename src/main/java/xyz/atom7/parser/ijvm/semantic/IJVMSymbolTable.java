package xyz.atom7.parser.ijvm.semantic;

import lombok.Getter;
import lombok.Setter;
import xyz.atom7.api.parser.semantic.SymbolTable;

import java.util.*;

/**
 * A symbol table implementation for IJVM semantic analysis.
 * Tracks variables, methods, and constants and their scopes.
 */
public class IJVMSymbolTable extends SymbolTable
{
    
    // Symbol types in IJVM
    public enum SymbolType {
        CONSTANT,
        VARIABLE,
        METHOD,
        LABEL
    }
    
    // Symbol entry to track information about each symbol
    @Getter
    public static class SymbolEntry
    {
        private final String name;
        private final SymbolType type;
        private final String scope;
        private final int value; // For constants
        private final List<String> parameters; // For methods
        @Setter
        private boolean initialized; // For variables
        
        public SymbolEntry(String name, SymbolType type, String scope)
        {
            this(name, type, scope, 0, Collections.emptyList());
        }
        
        public SymbolEntry(String name, SymbolType type, String scope, int value, List<String> parameters)
        {
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.value = value;
            this.parameters = parameters;
            this.initialized = type != SymbolType.VARIABLE; // Only variables need initialization
        }
    }
    
    // Map to store symbols by scope and name (using case-insensitive map for lookup)
    private final Map<String, Map<String, SymbolEntry>> scopedSymbols = new HashMap<>();
    
    // Map to store normalized names (lowercase) to original names for case-insensitive lookup
    private final Map<String, Map<String, String>> normalizedNames = new HashMap<>();
    
    // Global scope for constants
    private static final String GLOBAL_SCOPE = "GLOBAL";
    
    // Current scope for analysis
    private String currentScope = GLOBAL_SCOPE;
    
    // Initialize symbol table with global scope
    public IJVMSymbolTable()
    {
        scopedSymbols.put(GLOBAL_SCOPE, new HashMap<>());
        normalizedNames.put(GLOBAL_SCOPE, new HashMap<>());
    }
    
    /**
     * Set the current scope for symbol operations
     */
    public void enterScope(String scope)
    {
        currentScope = scope;
        scopedSymbols.putIfAbsent(scope, new HashMap<>());
        normalizedNames.putIfAbsent(scope, new HashMap<>());
    }
    
    /**
     * Reset to global scope
     */
    public void exitScope()
    {
        currentScope = GLOBAL_SCOPE;
    }
    
    /**
     * Add a constant to the symbol table
     */
    public void addConstant(String name, int value)
    {
        SymbolEntry entry = new SymbolEntry(name, SymbolType.CONSTANT, GLOBAL_SCOPE, value, Collections.emptyList());
        scopedSymbols.get(GLOBAL_SCOPE).put(name, entry);
        normalizedNames.get(GLOBAL_SCOPE).put(name.toLowerCase(), name);
    }
    
    /**
     * Add a variable to the current scope
     */
    public void addVariable(String name)
    {
        SymbolEntry entry = new SymbolEntry(name, SymbolType.VARIABLE, currentScope);
        scopedSymbols.get(currentScope).put(name, entry);
        normalizedNames.get(currentScope).put(name.toLowerCase(), name);
    }
    
    /**
     * Add a method to the symbol table
     */
    public void addMethod(String name, List<String> parameters)
    {
        SymbolEntry entry = new SymbolEntry(name, SymbolType.METHOD, GLOBAL_SCOPE, 0, parameters);
        scopedSymbols.get(GLOBAL_SCOPE).put(name, entry);
        normalizedNames.get(GLOBAL_SCOPE).put(name.toLowerCase(), name);
    }
    
    /**
     * Add a label to the current scope
     */
    public void addLabel(String name)
    {
        SymbolEntry entry = new SymbolEntry(name, SymbolType.LABEL, currentScope);
        scopedSymbols.get(currentScope).put(name, entry);
        normalizedNames.get(currentScope).put(name.toLowerCase(), name);
    }
    
    /**
     * Mark a variable as initialized in the current scope
     */
    public void markVariableInitialized(String name)
    {
        SymbolEntry entry = lookupSymbol(name);
        if (entry != null && entry.getType() == SymbolType.VARIABLE) {
            entry.setInitialized(true);
        }
    }
    
    /**
     * Look up a symbol by name, checking the current scope first, then global.
     * This method performs case-insensitive lookups.
     */
    public SymbolEntry lookupSymbol(String name)
    {
        String normalizedName = name.toLowerCase();
        
        // Try to find symbol using normalized name
        SymbolEntry entry = lookupInScope(currentScope, normalizedName);
        if (entry != null) {
            return entry;
        }
        
        // If not in current scope and not in global scope, try global scope
        if (!currentScope.equals(GLOBAL_SCOPE)) {
            entry = lookupInScope(GLOBAL_SCOPE, normalizedName);
            if (entry != null) {
                return entry;
            }
        }
        
        // Direct lookup in case normalized map is out of sync
        entry = directLookupInScope(currentScope, name);
        if (entry != null) {
            return entry;
        }

        if (currentScope.equals(GLOBAL_SCOPE)) {
            return null;
        }

        entry = directLookupInScope(GLOBAL_SCOPE, name);
        return entry;
    }
    
    /**
     * Helper method to lookup in a specific scope
     */
    private SymbolEntry lookupInScope(String scope, String normalizedName)
    {
        Map<String, SymbolEntry> scopeSymbols = scopedSymbols.get(scope);
        Map<String, String> scopeNormalized = normalizedNames.get(scope);
        
        if (scopeSymbols != null && scopeNormalized != null) {
            String originalName = scopeNormalized.get(normalizedName);
            if (originalName != null && scopeSymbols.containsKey(originalName)) {
                return scopeSymbols.get(originalName);
            }
        }
        return null;
    }

    /**
     * Helper method to directly lookup in a specific scope
     */
    private SymbolEntry directLookupInScope(String scope, String name)
    {
        Map<String, SymbolEntry> scopeSymbols = scopedSymbols.get(scope);
        if (scopeSymbols != null && scopeSymbols.containsKey(name)) {
            return scopeSymbols.get(name);
        }
        return null;
    }
    
    /**
     * Check if a symbol exists in any scope
     */
    public boolean symbolExists(String name)
    {
        return lookupSymbol(name) != null;
    }
    
    /**
     * Check if a method exists with the given name
     */
    public boolean methodExists(String name)
    {
        SymbolEntry entry = lookupSymbol(name);
        return entry != null && entry.getType() == SymbolType.METHOD;
    }
    
    /**
     * Get method parameter count for checking method calls
     */
    public int getMethodParameterCount(String name)
    {
        SymbolEntry entry = lookupSymbol(name);
        if (entry != null && entry.getType() == SymbolType.METHOD) {
            return entry.getParameters().size();
        }
        return -1;
    }
}