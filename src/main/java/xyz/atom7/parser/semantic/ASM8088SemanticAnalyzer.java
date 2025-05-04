package xyz.atom7.parser.semantic;

import lombok.Getter;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.parser.asm8088Parser;
import xyz.atom7.parser.asm8088ParserBaseVisitor;

import java.util.*;

/**
 * Semantic analyzer for 8088 assembly language that traverses the parse tree and enforces semantic rules.
 */
public class ASM8088SemanticAnalyzer extends asm8088ParserBaseVisitor<Void>
{
    /**
     * Symbol table to track declarations
     */
    private final ASM8088SymbolTable symbolTable;

    /**
     * List of semantic errors found during analysis
     */
    @Getter
    private final List<SemanticError> errors;

    /**
     * List of semantic warnings found during analysis
     */
    @Getter
    private final List<SemanticWarning> warnings;

    /**
     * Flag for the first declaration pass
     */
    private boolean isDeclarationPass;

    /**
     * Flag for the verification pass
     */
    private boolean isVerificationPass;

    /**
     * Set to track instructions that require byte operands
     */
    private final Set<String> byteInstructions;

    /**
     * Set to track 8-bit registers
     */
    private final Set<String> byteRegisters;

    /**
     * Set to track jump instructions
     */
    private final Set<String> jumpInstructions;

    /**
     * Current section (.DATA, .TEXT, .BSS)
     */
    private String currentSection;

    /**
     * Constructor that initializes the analyzer
     */
    public ASM8088SemanticAnalyzer()
    {
        this.symbolTable = new ASM8088SymbolTable();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.isDeclarationPass = true;
        this.isVerificationPass = false;
        this.currentSection = null;
        this.byteInstructions = initializeByteInstructions();
        this.byteRegisters = initializeByteRegisters();
        this.jumpInstructions = initializeJumpInstructions();
    }

    /**
     * Initialize the set of byte instructions
     */
    private Set<String> initializeByteInstructions()
    {
        Set<String> instructions = new HashSet<>();
        instructions.add("MOVB");
        instructions.add("SUBB");
        instructions.add("DIVB");
        instructions.add("CMPB");
        instructions.add("ADDB");
        instructions.add("XORB");
        instructions.add("MULB");
        return instructions;
    }

    /**
     * Initialize the set of byte registers
     */
    private Set<String> initializeByteRegisters()
    {
        Set<String> registers = new HashSet<>();
        registers.add("AL");
        registers.add("AH");
        registers.add("BL");
        registers.add("BH");
        registers.add("CL");
        registers.add("CH");
        registers.add("DL");
        registers.add("DH");
        return registers;
    }

    /**
     * Initialize the set of jump and call instructions
     */
    private Set<String> initializeJumpInstructions()
    {
        Set<String> instructions = new HashSet<>();
        instructions.add("JMP");
        instructions.add("JE");
        instructions.add("JNE");
        instructions.add("JG");
        instructions.add("JGE");
        instructions.add("JL");
        instructions.add("JLE");
        instructions.add("JNGE");
        instructions.add("JNG");
        instructions.add("JZ");
        instructions.add("JNZ");
        instructions.add("LOOP");
        instructions.add("CALL");
        return instructions;
    }

    /**
     * Run semantic analysis on a parse tree
     *
     * @param tree The parse tree to analyze
     */
    public void analyze(ParseTree tree)
    {
        // Clear any existing errors and warnings
        errors.clear();
        warnings.clear();

        // First pass: Process declarations and sections
        isDeclarationPass = true;
        isVerificationPass = false;
        visit(tree);

        // Second pass: Verify references and check operand types
        isDeclarationPass = false;
        isVerificationPass = true;
        visit(tree);

        checkForUndefinedSymbols();
    }

    /**
     * Check if semantic analysis found any errors
     */
    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    /**
     * Check if semantic analysis found any warnings
     */
    public boolean hasWarnings()
    {
        return !warnings.isEmpty();
    }

    /**
     * Add a semantic error
     */
    private void addError(String message, Token token)
    {
        errors.add(new SemanticError(message, token));
    }

    /**
     * Add a semantic warning
     */
    private void addWarning(String message, Token token)
    {
        warnings.add(new SemanticWarning(message, token));
    }


    /**
     * Check for undefined symbols after all passes
     */
    private void checkForUndefinedSymbols()
    {
        List<ASM8088SymbolTable.SymbolEntry> undefinedSymbols = symbolTable.getUndefinedSymbols();
        for (ASM8088SymbolTable.SymbolEntry symbol : undefinedSymbols) {
            // We don't have token information here, so we'll use null for the token
            addError("Symbol '" + symbol.getName() + "' is referenced but never defined", null);
        }

        // Check for unreferenced labels (warning)
        List<ASM8088SymbolTable.SymbolEntry> unreferencedSymbols = symbolTable.getUnreferencedSymbols();
        for (ASM8088SymbolTable.SymbolEntry symbol : unreferencedSymbols) {
            if (symbol.getType() == ASM8088SymbolTable.SymbolType.LABEL) {
                addWarning("Label '" + symbol.getName() + "' is defined but never referenced", null);
            }
        }
    }

    /**
     * Visit the program context
     * 
     * @param ctx The program context
     */
    @Override
    public Void visitProgram(asm8088Parser.ProgramContext ctx)
    {
        if (ctx.line() != null) {
            for (asm8088Parser.LineContext lineCtx : ctx.line()) {
                visit(lineCtx);
            }
        }
        return null;
    }

    /**
     * Visit the line context
     * 
     * @param ctx The line context
     */
    @Override
    public Void visitLine(asm8088Parser.LineContext ctx)
    {
        // Process each type of line
        if (ctx.assignment() != null) {
            visit(ctx.assignment());
        } else if (ctx.section() != null) {
            visit(ctx.section());
        } else if (ctx.labelDecl() != null) {
            visit(ctx.labelDecl());
        } else if (ctx.statement() != null) {
            visit(ctx.statement());
        }
        return null;
    }

    /**
     * Visit the section context
     * 
     * @param ctx The section context
     */
    @Override
    public Void visitSection(asm8088Parser.SectionContext ctx)
    {
        String section = null;
        
        if (ctx.DATA() != null) {
            section = ".DATA";
        } else if (ctx.TEXT() != null) {
            section = ".TEXT";
        } else if (ctx.BSS() != null) {
            section = ".BSS";
        }
        
        if (section != null) {
            if (isDeclarationPass) {
                symbolTable.setCurrentSection(section);
                this.currentSection = section;
            }
        }
        
        return null;
    }

    /**
     * Visit the assignment context
     * 
     * @param ctx The assignment context
     */
    @Override
    public Void visitAssignment(asm8088Parser.AssignmentContext ctx)
    {
        if (isDeclarationPass) {
            String id = ctx.ID().getText();
            
            // Check if the symbol already exists
            if (symbolTable.symbolExists(id)) {
                addError("Symbol '" + id + "' is already defined", ctx.ID().getSymbol());
                return null;
            }
            
            // Parse the value
            int value;
            try {
                if (ctx.HEX() != null) {
                    String hexValue = ctx.HEX().getText();
                    if ((!hexValue.startsWith("0x") && !hexValue.startsWith("0X")) || hexValue.length() <= 2) {
                        throw new NumberFormatException("Invalid hex format");
                    }

                    hexValue = hexValue.substring(2);
                    value = Integer.parseInt(hexValue, 16);
                } else {
                    String numValue = ctx.NUM().getText();
                    value = Integer.parseInt(numValue);
                }
                
                // Add to symbol table
                symbolTable.addConstant(id, value);
            } catch (NumberFormatException e) {
                addError("Invalid number format for constant '" + id + "'", 
                          ctx.HEX() != null ? ctx.HEX().getSymbol() : ctx.NUM().getSymbol());
            }
        }
        
        return null;
    }

    /**
     * Visit the label declaration context
     * 
     * @param ctx The label declaration context
     */
    @Override
    public Void visitLabelDecl(asm8088Parser.LabelDeclContext ctx)
    {
        if (isDeclarationPass) {
            String label = ctx.ID().getText();
            
            // Check if the label is already defined
            ASM8088SymbolTable.SymbolEntry entry = symbolTable.lookupSymbol(label);
            if (entry != null && entry.isDefined() && entry.getType() == ASM8088SymbolTable.SymbolType.LABEL) {
                addError("Label '" + label + "' is already defined", ctx.ID().getSymbol());
                return null;
            }
            
            // Add to symbol table
            symbolTable.addLabel(label);
        }
        
        return null;
    }

    /**
     * Visit the statement context
     * 
     * @param ctx The statement context
     */
    @Override
    public Void visitStatement(asm8088Parser.StatementContext ctx)
    {
        if (ctx.instruction() != null) {
            visit(ctx.instruction());
        } else if (ctx.directive() != null) {
            visit(ctx.directive());
        }
        
        return null;
    }

    /**
     * Visit the instruction context
     * 
     * @param ctx The instruction context
     */
    @Override
    public Void visitInstruction(asm8088Parser.InstructionContext ctx)
    {
        if (isVerificationPass && ctx.mnemonic() != null) {
            String mnemonic = ctx.mnemonic().getText();
            boolean isByteInstruction = byteInstructions.contains(mnemonic.toUpperCase());
            boolean isJumpInstruction = jumpInstructions.contains(mnemonic.toUpperCase());
            
            // For instructions with operands, verify operand compatibility
            if (ctx.operandList() != null) {
                visitOperandList(ctx.operandList(), isByteInstruction);
            }
            
            // Check jump instructions in the correct section
            if (isJumpInstruction && currentSection != null && !currentSection.equals(".TEXT")) {
                addWarning("Jump/call instruction used outside .TEXT section", ctx.mnemonic().getStart());
            }
        }
        
        return null;
    }

    /**
     * Visit the operand list context with instruction type awareness
     * 
     * @param ctx The operand list context
     * @param isByteInstruction Whether the instruction is a byte instruction
     */
    private Void visitOperandList(asm8088Parser.OperandListContext ctx, boolean isByteInstruction)
    {
        if (isVerificationPass && ctx.operand() != null) {
            for (asm8088Parser.OperandContext opCtx : ctx.operand()) {
                // Check register size compatibility
                if (opCtx.register() != null) {
                    String register = opCtx.register().getText();
                    boolean isByteRegister = byteRegisters.contains(register.toUpperCase());
                    
                    if (isByteInstruction && !isByteRegister) {
                        addError("Byte instruction requires byte register, found '" + register + "'", 
                                 opCtx.register().getStart());
                    } else if (!isByteInstruction && isByteRegister) {
                        addError("Word instruction requires word register, found '" + register + "'", 
                                 opCtx.register().getStart());
                    }
                }
                
                // Check for undefined symbols in expressions
                if (opCtx.expr() != null) {
                    visitExpr(opCtx.expr());
                }
                
                // Check memory operands
                if (opCtx.memory() != null) {
                    visitMemory(opCtx.memory());
                }
            }
        }
        
        return null;
    }

    /**
     * Visit the expression context
     * 
     * @param ctx The expression context
     */
    @Override
    public Void visitExpr(asm8088Parser.ExprContext ctx)
    {
        if (isVerificationPass) {
            // In expressions, there can be multiple IDs
            // The parser grammar shows expressions like: ID (PLUS|MINUS) ID
            var idNodes = ctx.ID();

            if (idNodes == null || idNodes.isEmpty()) {
                return null;
            }

            for (org.antlr.v4.runtime.tree.TerminalNode idNode : idNodes) {
                if (idNode == null) {
                    continue;
                }

                String id = idNode.getText();

                // Reference the symbol
                symbolTable.referenceSymbol(id);

                // Check if the symbol is defined
                if (!symbolTable.isSymbolDefined(id)) {
                    addWarning("Symbol '" + id + "' used in expression may not be defined", idNode.getSymbol());
                }
            }
        }
        
        return null;
    }

    /**
     * Visit the memory context
     * 
     * @param ctx The memory context
     */
    @Override
    public Void visitMemory(asm8088Parser.MemoryContext ctx)
    {
        if (isVerificationPass) {
            // Check for a named location (ID token) in memory reference
            boolean hasNamedLocation = false;

            // In the memory context, there can be ID tokens in different positions
            // Handle each possible position individually based on the grammar
            org.antlr.v4.runtime.tree.TerminalNode idNode = ctx.ID();
            if (idNode != null) {
                String id = idNode.getText();
                hasNamedLocation = true;
                
                // Reference the symbol
                symbolTable.referenceSymbol(id);
                
                // Check if the symbol is defined
                if (!symbolTable.isSymbolDefined(id)) {
                    addWarning("Symbol '" + id + "' used in memory reference may not be defined", idNode.getSymbol());
                }
            }
            
            // Check if memory access is in the correct section for data
            if (hasNamedLocation && currentSection != null && !currentSection.equals(".DATA")) {
                addWarning("Memory access to named location should be in .DATA section", ctx.getStart());
            }
        }
        
        return null;
    }

    /**
     * Visit the directive context
     * 
     * @param ctx The directive context
     */
    @Override
    public Void visitDirective(asm8088Parser.DirectiveContext ctx)
    {
        if (isVerificationPass) {
            // Check section compatibility
            if (currentSection != null) {
                if (ctx.BYTE() != null && !currentSection.equals(".DATA")) {
                    addWarning(".BYTE directive should be in .DATA section", ctx.BYTE().getSymbol());
                } else if (ctx.ASCII() != null && !currentSection.equals(".DATA")) {
                    addWarning(".ASCII directive should be in .DATA section", ctx.ASCII().getSymbol());
                } else if (ctx.SPACE() != null && !currentSection.equals(".DATA") && !currentSection.equals(".BSS")) {
                    addWarning(".SPACE directive should be in .DATA or .BSS section", ctx.SPACE().getSymbol());
                }
            } else {
                // No section defined but directive being used
                if (ctx.BYTE() != null || ctx.ASCII() != null || ctx.SPACE() != null) {
                    addWarning("Directive used without a section defined", ctx.getStart());
                }
            }
        }
        
        return null;
    }
} 