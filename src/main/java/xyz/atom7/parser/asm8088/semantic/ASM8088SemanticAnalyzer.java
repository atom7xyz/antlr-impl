package xyz.atom7.parser.asm8088.semantic;

import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
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
     * Flag for the label scanning pass
     */
    private boolean isLabelScanPass;

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
     * Map to store all labels for forward reference checking
     */
    private final Set<String> allLabels = new HashSet<>();

    /**
     * Constructor that initializes the analyzer
     */
    public ASM8088SemanticAnalyzer()
    {
        this.symbolTable = new ASM8088SymbolTable();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.isDeclarationPass = true;
        this.isLabelScanPass = false;
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
        instructions.add("JZ");
        instructions.add("JNE");
        instructions.add("JNZ");
        instructions.add("JG");
        instructions.add("JNLE");
        instructions.add("JGE");
        instructions.add("JNL");
        instructions.add("JL");
        instructions.add("JNGE");
        instructions.add("JLE");
        instructions.add("JNG");
        instructions.add("JB");
        instructions.add("JNAE");
        instructions.add("JBE");
        instructions.add("JNA");
        instructions.add("JA");
        instructions.add("JNBE");
        instructions.add("JAE");
        instructions.add("JNB");
        instructions.add("JS");
        instructions.add("JNS");
        instructions.add("JO");
        instructions.add("JNO");
        instructions.add("JP");
        instructions.add("JPE");
        instructions.add("JNP");
        instructions.add("JPO");
        instructions.add("JC");
        instructions.add("JNC");
        instructions.add("JCXZ");
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
        allLabels.clear();

        // First pass: Process declarations and constants
        isDeclarationPass = true;
        isLabelScanPass = false;
        visit(tree);

        // Second pass: Collect all label declarations
        isDeclarationPass = false;
        isLabelScanPass = true;
        visit(tree);

        // Third pass: Verify references and check operand types
        isDeclarationPass = false;
        isLabelScanPass = false;
        visit(tree);

        // Final check for undefined symbols after all passes
        checkForUndefinedSymbols();
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
        var undefinedSymbols = symbolTable.getUndefinedSymbols();
        for (var symbol : undefinedSymbols)
        {
            addError("Symbol '" + symbol.getName() + "' is referenced but never defined", null);
        }

        // Check for unreferenced labels (warning)
        var unreferencedSymbols = symbolTable.getUnreferencedSymbols();
        for (var symbol : unreferencedSymbols)
        {
            if (symbol.getType() == ASM8088SymbolTable.SymbolType.LABEL) {
                // Skip warning for 'main' label - it's allowed to be defined but never referenced
                if (!"main".equalsIgnoreCase(symbol.getName())) {
                    addWarning("Label '" + symbol.getName() + "' is defined but never referenced", null);
                }
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
        if (ctx.line() == null) {
            return null;
        }

        for (asm8088Parser.LineContext lineCtx : ctx.line()) {
            visit(lineCtx);
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
        }
        
        if (ctx.section() != null) {
            visit(ctx.section());
        }
        
        if (ctx.labelDecl() != null) {
            visit(ctx.labelDecl());
        }
        
        if (ctx.statement() != null) {
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
        String section = getSectionName(ctx);
        
        if (section != null) {
            if (isDeclarationPass) {
                symbolTable.setCurrentSection(section);
            }
            this.currentSection = section;
        }
        
        return null;
    }

    /**
     * Get the section name from the context
     *
     * @param ctx The section context
     * @return The section name as a string
     */
    private String getSectionName(asm8088Parser.SectionContext ctx)
    {
        if (ctx.DATA() != null) {
            return ".DATA";
        }
        if (ctx.TEXT() != null) {
            return ".TEXT";
        }
        if (ctx.BSS() != null) {
            return ".BSS";
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
        if (!isDeclarationPass) {
            return null;
        }

        String id = ctx.ID().getText();
        
        // Check if the symbol already exists
        if (symbolTable.symbolExists(id)) {
            addError("Symbol '" + id + "' is already defined", ctx.ID().getSymbol());
            return null;
        }
        
        // Parse the value
        try {
            int value = parseAssignmentValue(ctx);
            symbolTable.addConstant(id, value);
        } catch (NumberFormatException e) {
            addError("Invalid number format for constant '" + id + "'", 
                      ctx.HEX() != null ? ctx.HEX().getSymbol() : ctx.NUM().getSymbol());
        }
        
        return null;
    }

    /**
     * Parse the assignment value from the context
     *
     * @param ctx The assignment context
     * @return The parsed integer value
     * @throws NumberFormatException If the number format is invalid
     */
    private int parseAssignmentValue(asm8088Parser.AssignmentContext ctx) throws NumberFormatException
    {
        if (ctx.HEX() != null) {
            String hexValue = ctx.HEX().getText();
            if ((!hexValue.startsWith("0x") && !hexValue.startsWith("0X")) || hexValue.length() <= 2) {
                throw new NumberFormatException("Invalid hex format");
            }
            hexValue = hexValue.substring(2);
            return Integer.parseInt(hexValue, 16);
        }
        
        String numValue = ctx.NUM().getText();
        return Integer.parseInt(numValue);
    }

    /**
     * Visit the label declaration context
     * 
     * @param ctx The label declaration context
     */
    @Override
    public Void visitLabelDecl(asm8088Parser.LabelDeclContext ctx)
    {
        String label = ctx.ID().getText();
        
        if (isLabelScanPass) {
            // Collect all labels for forward reference checking
            allLabels.add(label);
            return null;
        }
        
        if (!isDeclarationPass) {
            return null;
        }

        // Check if the label is already defined
        ASM8088SymbolTable.SymbolEntry entry = symbolTable.lookupSymbol(label);
        if (entry != null && entry.isDefined() && entry.getType() == ASM8088SymbolTable.SymbolType.LABEL) {
            addError("Label '" + label + "' is already defined", ctx.ID().getSymbol());
            return null;
        }
        
        // Add to symbol table
        symbolTable.addLabel(label);
        
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
        }
        else if (ctx.directive() != null) {
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
        if (isDeclarationPass || isLabelScanPass || ctx.mnemonic() == null) {
            return null;
        }

        String mnemonic = ctx.mnemonic().getText().toUpperCase();
        boolean isByteInstruction = byteInstructions.contains(mnemonic);
        boolean isJumpInstruction = jumpInstructions.contains(mnemonic);
        
        // Check jump instructions in the correct section
        if (isJumpInstruction && currentSection != null && !currentSection.equals(".TEXT")) {
            addWarning("Jump/call instruction used outside .TEXT section", ctx.mnemonic().getStart());
        }
        
        // For instructions with operands, verify operand compatibility
        if (ctx.operandList() != null) {
            visitOperandList(ctx.operandList(), isByteInstruction, isJumpInstruction);
        }
        
        return null;
    }

    /**
     * Visit the operand list context with instruction type awareness
     *
     * @param ctx The operand list context
     * @param isByteInstruction Whether the instruction is a byte instruction
     * @param isJumpInstruction Whether the instruction is a jump instruction
     */
    private Void visitOperandList(asm8088Parser.OperandListContext ctx,
                                  boolean isByteInstruction,
                                  boolean isJumpInstruction)
    {
        if (ctx.operand() == null) {
            return null;
        }

        for (asm8088Parser.OperandContext opCtx : ctx.operand())
        {
            // Check register size compatibility
            if (opCtx.register() != null) {
                checkRegisterCompatibility(opCtx, isByteInstruction);
                continue;
            }

            // Check for undefined symbols in expressions
            if (opCtx.expr() != null) {
                visitExpr(opCtx.expr());
                continue;
            }

            // Check memory operands
            if (opCtx.memory() != null) {
                visitMemory(opCtx.memory());
                continue;
            }
        }

        return null;
    }

    /**
     * Check register compatibility based on instruction type
     *
     * @param opCtx The operand context containing the register
     * @param isByteInstruction Whether the instruction is a byte instruction
     */
    private void checkRegisterCompatibility(asm8088Parser.OperandContext opCtx, boolean isByteInstruction)
    {
        String register = opCtx.register().getText().toUpperCase();
        boolean isByteRegister = byteRegisters.contains(register);

        if (isByteInstruction && !isByteRegister) {
            addError("Byte instruction requires byte register, found '" + register + "'",
                    opCtx.register().getStart());
            return;
        }
        
        if (!isByteInstruction && isByteRegister && opCtx.immediate() != null) {
            addError("Word instruction requires word register, found '" + register + "'",
                    opCtx.register().getStart());
        }
    }

    /**
     * Visit the expression context
     * 
     * @param ctx The expression context
     */
    @Override
    public Void visitExpr(asm8088Parser.ExprContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        // In expressions, there can be multiple IDs
        var idNodes = ctx.ID();

        if (idNodes == null || idNodes.isEmpty()) {
            return null;
        }

        for (org.antlr.v4.runtime.tree.TerminalNode idNode : idNodes)
        {
            if (idNode == null) {
                continue;
            }
            processExpressionId(idNode);
        }
        
        return null;
    }

    /**
     * Process an ID node in an expression
     *
     * @param idNode The ID node to process
     */
    private void processExpressionId(org.antlr.v4.runtime.tree.TerminalNode idNode)
    {
        String id = idNode.getText();
        symbolTable.referenceSymbol(id);

        if (!symbolTable.isSymbolDefined(id) && !allLabels.contains(id)) {
            addError("Symbol '" + id + "' is not defined", idNode.getSymbol());
        }
    }

    /**
     * Visit the memory context
     * 
     * @param ctx The memory context
     */
    @Override
    public Void visitMemory(asm8088Parser.MemoryContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        // Visit the memory expression to check for symbol references
        if (ctx.memoryExpression() != null) {
            visit(ctx.memoryExpression());
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
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        // Check section compatibility
        if (currentSection != null) {
            checkDirectiveSectionCompatibility(ctx);
        } else if (hasDirective(ctx)) {
            addWarning("Directive used without a section defined", ctx.getStart());
        }
        
        return null;
    }

    /**
     * Check if the directive is compatible with the current section
     *
     * @param ctx The directive context
     */
    private void checkDirectiveSectionCompatibility(asm8088Parser.DirectiveContext ctx)
    {
        if (ctx.BYTE() != null && !currentSection.equals(".DATA")) {
            addWarning(".BYTE directive should be in .DATA section", ctx.BYTE().getSymbol());
        }
        else if (ctx.ASCII() != null && !currentSection.equals(".DATA")) {
            addWarning(".ASCII directive should be in .DATA section", ctx.ASCII().getSymbol());
        }
        else if (ctx.SPACE() != null && !currentSection.equals(".DATA") && !currentSection.equals(".BSS")) {
            addWarning(".SPACE directive should be in .DATA or .BSS section", ctx.SPACE().getSymbol());
        }
    }

    /**
     * Check if the directive context has any of the known directives
     *
     * @param ctx The directive context
     * @return true if it has a directive, false otherwise
     */
    private boolean hasDirective(asm8088Parser.DirectiveContext ctx)
    {
        return ctx.BYTE() != null || ctx.ASCII() != null || ctx.SPACE() != null;
    }
} 