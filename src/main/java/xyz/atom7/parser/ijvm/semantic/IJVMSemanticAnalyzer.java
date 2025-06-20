package xyz.atom7.parser.ijvm.semantic;

import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import xyz.atom7.api.parser.semantic.SemanticError;
import xyz.atom7.api.parser.semantic.SemanticWarning;
import xyz.atom7.parser.IJVMParser;
import xyz.atom7.parser.IJVMParserBaseVisitor;

import java.util.*;

/**
 * Semantic analyzer for IJVM that traverses the parse tree and enforces semantic rules.
 */
public class IJVMSemanticAnalyzer extends IJVMParserBaseVisitor<Void>
{
    /**
     * Symbol table to track declarations
     */
    private final IJVMSymbolTable symbolTable;

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
     * Stack simulation for stack operations
     */
    private int stackSize;

    /**
     * Flag for the first declaration pass
     */
    private boolean isDeclarationPass;

    /**
     * Flag for collecting label declarations pass
     */
    private boolean isLabelScanPass;

    /**
     * Flag to track if instructions have started in current method/main
     */
    private boolean instructionsStarted;

    /**
     * Map to store methods and their labels (to handle forward references)
     */
    private final Map<String, Set<String>> methodLabels = new HashMap<>();

    /**
     * Current method being processed
     */
    private String currentMethod;

    public IJVMSemanticAnalyzer()
    {
        this.symbolTable = new IJVMSymbolTable();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.stackSize = 0;
        this.isDeclarationPass = true;
        this.isLabelScanPass = false;
        this.instructionsStarted = false;
        this.currentMethod = null;
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

        // First pass: Process declarations
        isDeclarationPass = true;
        isLabelScanPass = false;
        visit(tree);

        // Label scan pass: Collect all label declarations for forward references
        isDeclarationPass = false;
        isLabelScanPass = true;
        visit(tree);

        // Analysis pass: Check semantics
        isDeclarationPass = false;
        isLabelScanPass = false;
        visit(tree);
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
     * Visit the program context
     * 
     * @param ctx The program context
     */
    @Override
    public Void visitProgram(IJVMParser.ProgramContext ctx)
    {
        if (isDeclarationPass) {
            processDeclarationPass(ctx);
            return null;
        }
        
        if (isLabelScanPass) {
            processLabelScanPass(ctx);
            return null;
        }
        
        // Main analysis pass
        processAnalysisPass(ctx);
        return null;
    }

    private void processDeclarationPass(IJVMParser.ProgramContext ctx)
    {
        // First pass: Process declarations
        if (ctx.constantBlock() != null) {
            visit(ctx.constantBlock());
        }

        // Register all method declarations first
        if (ctx.methodBlock() == null) {
            return;
        }
        
        for (IJVMParser.MethodBlockContext methodCtx : ctx.methodBlock()) {
            processMethodDeclaration(methodCtx);
        }
    }

    private void processLabelScanPass(IJVMParser.ProgramContext ctx)
    {
        // Scan for labels in main block
        if (ctx.mainBlock() != null) {
            currentMethod = "main";
            methodLabels.put(currentMethod, new HashSet<>());
            symbolTable.enterScope("main");
            scanLabelsMain(ctx.mainBlock());
            symbolTable.exitScope();
        }

        // Scan for labels in all methods
        if (ctx.methodBlock() == null) {
            return;
        }
        
        for (IJVMParser.MethodBlockContext methodCtx : ctx.methodBlock()) {
            scanLabelsMethod(methodCtx);
        }
    }

    private void processAnalysisPass(IJVMParser.ProgramContext ctx)
    {
        if (ctx.mainBlock() != null) {
            currentMethod = "main";
            symbolTable.enterScope("main");
            // Reset instruction tracking for main block
            instructionsStarted = false;
            stackSize = 0;
            visit(ctx.mainBlock());
            symbolTable.exitScope();
        }

        if (ctx.methodBlock() == null) {
            return;
        }
        
        for (IJVMParser.MethodBlockContext methodCtx : ctx.methodBlock())
        {
            // Reset instruction tracking for each method
            instructionsStarted = false;
            stackSize = 0;
            processMethodImplementation(methodCtx);
        }
    }

    /**
     * Process method declaration (first pass)
     * 
     * @param ctx The method block context
     */
    private void processMethodDeclaration(IJVMParser.MethodBlockContext ctx)
    {
        IJVMParser.MethodDeclContext methodDeclCtx = ctx.methodDecl();
        if (methodDeclCtx == null) {
            return;
        }

        String methodName = methodDeclCtx.ID().getText();

        // Check for duplicate method declarations
        if (symbolTable.methodExists(methodName)) {
            addError("Method '" + methodName + "' is already defined", methodDeclCtx.ID().getSymbol());
            return;
        }

        // Process method parameters
        List<String> parameters = new ArrayList<>();
        if (methodDeclCtx.paramList() == null) {
            symbolTable.addMethod(methodName, parameters);
            return;
        }
        
        IJVMParser.ParamListContext paramListCtx = methodDeclCtx.paramList();

        // Check for duplicate parameter names
        Set<String> paramSet = new HashSet<>();
        for (int i = 0; i < paramListCtx.ID().size(); i++)
        {
            String paramName = paramListCtx.ID(i).getText();
            if (paramSet.contains(paramName)) {
                addError("Duplicate parameter name '" + paramName + "' in method '" + methodName + "'",
                        paramListCtx.ID(i).getSymbol());
            }
            else {
                paramSet.add(paramName);
                parameters.add(paramName);
            }
        }

        // Add method to symbol table
        symbolTable.addMethod(methodName, parameters);
    }

    /**
     * Scan for all labels in a method (label scan pass)
     * 
     * @param ctx The method block context
     */
    private void scanLabelsMethod(IJVMParser.MethodBlockContext ctx)
    {
        IJVMParser.MethodDeclContext methodDeclCtx = ctx.methodDecl();
        if (methodDeclCtx == null) {
            return;
        }

        String methodName = methodDeclCtx.ID().getText();
        currentMethod = methodName;
        methodLabels.put(methodName, new HashSet<>());

        // Enter method scope for scanning labels
        symbolTable.enterScope(methodName);

        scanLabels(ctx);

        // Exit method scope
        symbolTable.exitScope();
    }

    /**
     * Scan for all labels in main block (label scan pass)
     * 
     * @param ctx The main block context
     */
    private void scanLabelsMain(IJVMParser.MainBlockContext ctx) {
        scanLabels(ctx);
    }

    /**
     * Scan for all labels in a block
     * 
     * @param ctx The block context
     */
    private <T extends ParserRuleContext> void scanLabels(T ctx)
    {
        List<IJVMParser.StatementContext> statements = null;

        if (ctx instanceof IJVMParser.MainBlockContext) {
            statements = ((IJVMParser.MainBlockContext) ctx).statement();
        }

        if (ctx instanceof IJVMParser.MethodBlockContext) {
            statements = ((IJVMParser.MethodBlockContext) ctx).statement();
        }

        if (statements == null) {
            return;
        }

        for (IJVMParser.StatementContext stmt : statements)
        {
            if (stmt.labelDecl() == null) {
                continue;
            }

            String labelName = stmt.labelDecl().ID().getText();
            // Add to main's labels
            methodLabels.get(currentMethod).add(labelName);
            // Also add to symbol table
            symbolTable.addLabel(labelName);
        }
    }

    /**
     * Process method implementation (analysis pass)
     * 
     * @param ctx The method block context
     */
    private void processMethodImplementation(IJVMParser.MethodBlockContext ctx)
    {
        IJVMParser.MethodDeclContext methodDeclCtx = ctx.methodDecl();
        if (methodDeclCtx == null) {
            return;
        }

        String methodName = methodDeclCtx.ID().getText();
        currentMethod = methodName;

        // Enter method scope for analyzing method body
        symbolTable.enterScope(methodName);

        // Add parameters as initialized variables in the method scope
        addParametersAsVariables(methodDeclCtx);

        // First check if there are any instructions before var blocks
        checkVarBlocksAtTopMethod(ctx);

        // Process var blocks
        processVarBlocks(ctx.varBlock());

        // Now we'll consider instructions have started for later var blocks
        instructionsStarted = true;

        // Check for control flow paths that don't end with IRETURN
        checkMethodFlowsPaths(ctx, methodDeclCtx);

        // Process all statements normally
        processStatements(ctx.statement());

        // Exit method scope
        symbolTable.exitScope();
    }

    private void addParametersAsVariables(IJVMParser.MethodDeclContext methodDeclCtx)
    {
        if (methodDeclCtx.paramList() == null) {
            return;
        }
        
        IJVMParser.ParamListContext paramListCtx = methodDeclCtx.paramList();
        for (int i = 0; i < paramListCtx.ID().size(); i++)
        {
            String paramName = paramListCtx.ID(i).getText();
            symbolTable.addVariable(paramName);
            symbolTable.markVariableInitialized(paramName);
        }
    }

    private void processStatements(List<IJVMParser.StatementContext> statements)
    {
        if (statements == null) {
            return;
        }
        
        for (IJVMParser.StatementContext stmt : statements) {
            visit(stmt);
        }
    }

    /**
     * Check all control flow paths in a method for proper IRETURN placement
     * 
     * @param ctx The method block context
     * @param methodDeclCtx The method declaration context
     */
    private void checkMethodFlowsPaths(IJVMParser.MethodBlockContext ctx, IJVMParser.MethodDeclContext methodDeclCtx)
    {
        List<IJVMParser.StatementContext> statements = ctx.statement();
        if (statements == null || statements.isEmpty()) {
            addError("Method must end with IRETURN", methodDeclCtx.getStop());
            return;
        }

        // Get only statements with instructions
        List<IJVMParser.StatementContext> instructionStatements = getInstructionStatements(statements);

        if (instructionStatements.isEmpty()) {
            addError("Method must end with IRETURN", methodDeclCtx.getStop());
            return;
        }

        checkIReturnPlacement(instructionStatements, statements, methodDeclCtx);
        checkUnreachableCodeAfterIReturn(instructionStatements);
    }

    private List<IJVMParser.StatementContext> getInstructionStatements(List<IJVMParser.StatementContext> statements)
    {
        List<IJVMParser.StatementContext> instructionStatements = new ArrayList<>();
        for (IJVMParser.StatementContext stmt : statements)
        {
            if (stmt.instruction() != null) {
                instructionStatements.add(stmt);
            }
        }
        return instructionStatements;
    }

    private void checkIReturnPlacement(List<IJVMParser.StatementContext> instructionStatements, 
                                     List<IJVMParser.StatementContext> allStatements,
                                     IJVMParser.MethodDeclContext methodDeclCtx)
    {
        IJVMParser.StatementContext lastInstrStmt = instructionStatements.get(instructionStatements.size() - 1);
        boolean lastIsIReturn = hasIReturn(lastInstrStmt);

        if (lastIsIReturn) {
            return;
        }

        // Check if there are labels that end with IRETURN
        boolean foundReturnInLabel = false;
        for (IJVMParser.StatementContext stmt : allStatements)
        {
            if (hasIReturn(stmt) && !stmt.equals(lastInstrStmt)) {
                foundReturnInLabel = true;
                // Change to warning if IRETURN exists in other code paths
                addWarning("Method does not end with IRETURN in main path, but has IRETURN in other paths",
                          methodDeclCtx.getStop());
                break;
            }
        }

        // If no IRETURN found anywhere, it's an error
        if (!foundReturnInLabel) {
            addError("Method must end with IRETURN", methodDeclCtx.getStop());
        }
    }

    private void checkUnreachableCodeAfterIReturn(List<IJVMParser.StatementContext> instructionStatements)
    {
        for (int i = 0; i < instructionStatements.size() - 1; i++)
        {
            IJVMParser.StatementContext stmt = instructionStatements.get(i);
            if (hasIReturn(stmt)) {
                // Only warn about code after IRETURN if there's no jump instruction before it
                // because it might be in a conditional branch
                IJVMParser.StatementContext nextStmt = instructionStatements.get(i + 1);
                addWarning("Potentially unreachable code after IRETURN", nextStmt.getStart());
            }
        }
    }

    /**
     * Check if var blocks are at the top
     * 
     * @param ctx The block context
     */
    private <T extends ParserRuleContext> void checkVarBlocksAtTop(T ctx)
    {
        List<IJVMParser.StatementContext> statements;
        List<IJVMParser.VarBlockContext> varBlocks;

        if (ctx instanceof IJVMParser.MethodBlockContext) {
            var context = (IJVMParser.MethodBlockContext) ctx;
            statements = context.statement();
            varBlocks = context.varBlock();
        }
        else if (ctx instanceof IJVMParser.MainBlockContext) {
            var context = (IJVMParser.MainBlockContext) ctx;
            statements = context.statement();
            varBlocks = context.varBlock();
        }
        else {
            return; // not supported
        }

        if (statements == null || statements.isEmpty() || varBlocks == null || varBlocks.isEmpty()) {
            return; // No statements or var blocks, nothing to check
        }

        // Get positions of all var blocks and instructions
        Map<Integer, Token> varBlockPositions = new HashMap<>();
        Map<Integer, Token> instructionPositions = new HashMap<>();

        // Map var block positions
        for (IJVMParser.VarBlockContext varBlock : varBlocks) {
            varBlockPositions.put(varBlock.getStart().getLine(), varBlock.getStart());
        }

        // Map instruction positions
        for (IJVMParser.StatementContext stmt : statements)
        {
            if (stmt.instruction() != null) {
                instructionPositions.put(stmt.getStart().getLine(), stmt.getStart());
            }
        }

        // Find min instruction position and min var block position
        int minInstructionLine = getMinLine(instructionPositions);
        int minVarBlockLine = getMinLine(varBlockPositions);

        if (minInstructionLine >= minVarBlockLine) {
            return;
        }

        // If any instruction comes before any var block, report error
        checkVarBlocksAfterInstructions(varBlocks, minInstructionLine);
    }

    private int getMinLine(Map<Integer, Token> positions)
    {
        int minLine = Integer.MAX_VALUE;
        for (int line : positions.keySet()) {
            if (line < minLine) {
                minLine = line;
            }
        }
        return minLine;
    }

    private void checkVarBlocksAfterInstructions(List<IJVMParser.VarBlockContext> varBlocks, int minInstructionLine)
    {
        for (IJVMParser.VarBlockContext varBlock : varBlocks)
        {
            Token start = varBlock.getStart();
            if (start.getLine() > minInstructionLine) {
                addError(".var blocks must be defined at the top of methods/main before any instructions", start);
            }
        }
    }

    /**
     * Check if var blocks are at the top, before any instructions
     * 
     * @param ctx The method block context
     */
    private void checkVarBlocksAtTopMethod(IJVMParser.MethodBlockContext ctx)
    {
        checkVarBlocksAtTop(ctx);
    }

    /**
     * Process var blocks
     * 
     * @param varBlocks The var blocks
     */
    private void processVarBlocks(List<IJVMParser.VarBlockContext> varBlocks)
    {
        if (varBlocks == null) {
            return;
        }

        for (IJVMParser.VarBlockContext varCtx : varBlocks) {
            visit(varCtx);
        }
    }

    /**
     * Check if a statement contains an IRETURN instruction
     * 
     * @param ctx The statement context
     * @return True if the statement contains an IRETURN instruction, false otherwise
     */
    private boolean hasIReturn(IJVMParser.StatementContext ctx)
    {
        return  ctx != null &&
                ctx.instruction() != null &&
                ctx.instruction().zeroArgInstr() != null &&
                ctx.instruction().zeroArgInstr().IRETURN() != null;
    }

    /**
     * Check if a label exists in the current method
     * 
     * @param labelName The label name
     * @return True if the label exists in the current method, false otherwise
     */
    private boolean labelExistsInMethod(String labelName)
    {
        if (currentMethod == null || !methodLabels.containsKey(currentMethod)) {
            return false;
        }
        return methodLabels.get(currentMethod).contains(labelName);
    }

    /**
     * Visit the method block context
     * 
     * @param ctx The method block context
     */
    @Override
    public Void visitMethodBlock(IJVMParser.MethodBlockContext ctx)
    {
        // Handled in the multi-pass processing
        return null;
    }

    /**
     * Visit the constant block context
     * 
     * @param ctx The constant block context
     */
    @Override
    public Void visitConstantBlock(IJVMParser.ConstantBlockContext ctx)
    {
        if (!isDeclarationPass) {
            return null;
        }

        // Process all constant declarations
        for (int i = 0; i < ctx.constantDecl().size(); i++)
        {
            IJVMParser.ConstantDeclContext constCtx = ctx.constantDecl(i);
            if (constCtx == null) {
                continue;
            }

            processConstantDeclaration(constCtx);
        }

        return null;
    }

    private void processConstantDeclaration(IJVMParser.ConstantDeclContext constCtx)
    {
        String constName = constCtx.ID().getText();

        // Check for duplicate constant declarations
        if (symbolTable.symbolExists(constName)) {
            addError("Constant '" + constName + "' is already defined", constCtx.ID().getSymbol());
            return;
        }

        // Parse the numeric value
        try {
            int value = parseConstantValue(constCtx);
            symbolTable.addConstant(constName, value);
        }
        catch (NumberFormatException e) {
            addError("Invalid number format for constant '" + constName + "'", constCtx.NUM().getSymbol());
        }
    }

    private int parseConstantValue(IJVMParser.ConstantDeclContext constCtx) throws NumberFormatException
    {
        String numText = constCtx.NUM().getText();

        if (numText.startsWith("0x") || numText.startsWith("0X")) {
            return Integer.parseInt(numText.substring(2), 16);
        }
        if (numText.startsWith("o") || numText.startsWith("O")) {
            return Integer.parseInt(numText.substring(1), 8);
        }
        return Integer.parseInt(numText);
    }

    /**
     * Visit the main block context
     * 
     * @param ctx The main block context
     */
    @Override
    public Void visitMainBlock(IJVMParser.MainBlockContext ctx)
    {
        if (isLabelScanPass || isDeclarationPass) {
            return null; // Label scanning is handled separately
        }

        // Set current method to main for proper label lookups
        currentMethod = "main";
        
        // Check if var blocks are at the top
        checkMainVarBlocksAtTopMain(ctx);

        // Process var blocks first
        processVarBlocks(ctx.varBlock());

        // Mark that instructions have started
        instructionsStarted = true;

        // Process statements
        processStatements(ctx.statement());

        return null;
    }

    /**
     * Check if var blocks in main are at the top
     * 
     * @param ctx The main block context
     */
    private void checkMainVarBlocksAtTopMain(IJVMParser.MainBlockContext ctx)
    {
        checkVarBlocksAtTop(ctx);
    }

    /**
     * Visit the var block context
     * 
     * @param ctx The var block context
     */
    @Override
    public Void visitVarBlock(IJVMParser.VarBlockContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        // Check if instructions have already started
        if (instructionsStarted) {
            addError(".var blocks must be defined at the top of methods/main before any instructions", ctx.getStart());
            return null;
        }

        // Process all variable declarations
        for (int i = 0; i < ctx.varDecl().size(); i++)
        {
            IJVMParser.VarDeclContext varCtx = ctx.varDecl(i);
            if (varCtx == null) {
                continue;
            }

            processVariableDeclaration(varCtx);
        }

        return null;
    }

    private void processVariableDeclaration(IJVMParser.VarDeclContext varCtx)
    {
        var id = varCtx.ID();
        String varName = id.getText();

        // Check for duplicate variable declarations in the current scope
        IJVMSymbolTable.SymbolEntry existingSymbol = symbolTable.lookupSymbol(varName);
        if (existingSymbol == null) {
            symbolTable.addVariable(varName);
            return;
        }

        // Check if the conflicting symbol is a method parameter
        if (existingSymbol.getType() == IJVMSymbolTable.SymbolType.VARIABLE && 
            existingSymbol.isInitialized() && 
            currentMethod != null && 
            !currentMethod.equals("main")) {
            // Method parameters are added as initialized variables, so we can use this to detect conflicts
            addError("Variable '" + varName + "' in .var block conflicts with a method parameter", id.getSymbol());
        } else {
            addError("Variable '" + varName + "' is already defined in this scope", id.getSymbol());
        }
    }

    /**
     * Visit the label declaration context
     * 
     * @param ctx The label declaration context
     */
    @Override
    public Void visitLabelDecl(IJVMParser.LabelDeclContext ctx)
    {
        return null; // unused
    }

    /**
     * Visit the var argument instruction context
     * 
     * @param ctx The var argument instruction context
     */
    @Override
    public Void visitVarArgInstr(IJVMParser.VarArgInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        String varName = ctx.ID().getText();
        IJVMSymbolTable.SymbolEntry entry = symbolTable.lookupSymbol(varName);

        if (entry == null) {
            addError("Variable '" + varName + "' is not defined", ctx.ID().getSymbol());
            return null;
        }

        if (entry.getType() != IJVMSymbolTable.SymbolType.VARIABLE) {
            addError("Symbol '" + varName + "' is not a variable", ctx.ID().getSymbol());
            return null;
        }

        // Handle ILOAD/ISTORE semantics
        if (ctx.ILOAD() != null) {
            handleILoad(entry, varName, ctx);
        } else if (ctx.ISTORE() != null) {
            handleIStore(varName, ctx);
        }

        return null;
    }

    private void handleILoad(IJVMSymbolTable.SymbolEntry entry, String varName, IJVMParser.VarArgInstrContext ctx)
    {
        // Check if variable is initialized before loading
        if (!entry.isInitialized()) {
            addError("Variable '" + varName + "' may not have been initialized before use", ctx.ID().getSymbol());
        }
        // ILOAD pushes a value onto the stack
        stackSize++;
    }

    private void handleIStore(String varName, IJVMParser.VarArgInstrContext ctx)
    {
        // ISTORE requires a value on the stack
        if (stackSize <= 0) {
            addWarning("Stack underflow: ISTORE requires a value on the stack", ctx.ISTORE().getSymbol());
        } else {
            stackSize--;
            // Mark the variable as initialized
            symbolTable.markVariableInitialized(varName);
        }
    }

    /**
     * Visit the method argument instruction context
     * 
     * @param ctx The method argument instruction context
     */
    @Override
    public Void visitMethodArgInstr(IJVMParser.MethodArgInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        String methodName = ctx.ID().getText();

        // Check if method exists
        if (!symbolTable.methodExists(methodName)) {
            addError("Method '" + methodName + "' is not defined", ctx.ID().getSymbol());
            return null;
        }

        int paramCount = symbolTable.getMethodParameterCount(methodName);

        // Check if there are enough values on the stack for method parameters and object reference
        if (stackSize < paramCount + 1) { // +1 for object reference
            addWarning("Stack underflow: Not enough values on stack for method call '" + methodName +
                    "'. Expected " + (paramCount + 1) + " but have " + stackSize, ctx.INVOKEVIRTUAL().getSymbol());
            return null;
        }

        // Check if there's an object reference on the stack
        // This is a simplified check - in a real implementation you might track the types of values on the stack
        boolean hasObjectRef = stackSize >= paramCount + 1;
        if (!hasObjectRef) {
            addWarning("Object reference is required for method call '" + methodName + "'",
                    ctx.INVOKEVIRTUAL().getSymbol());
        }

        // Adjust stack: remove params and objref, push result
        stackSize = stackSize - paramCount - 1 + 1; // -params, -objref, +result

        return null;
    }

    /**
     * Visit the constant argument instruction context
     * 
     * @param ctx The constant argument instruction context
     */
    @Override
    public Void visitConstantArgInstr(IJVMParser.ConstantArgInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        String constName = ctx.ID().getText();
        IJVMSymbolTable.SymbolEntry entry = symbolTable.lookupSymbol(constName);

        if (entry == null) {
            addError("Constant '" + constName + "' is not defined", ctx.ID().getSymbol());
            return null;
        }

        if (entry.getType() != IJVMSymbolTable.SymbolType.CONSTANT) {
            addError("Symbol '" + constName + "' is not a constant", ctx.ID().getSymbol());
            return null;
        }

        // LDC_W pushes a value onto the stack
        stackSize++;

        return null;
    }

    /**
     * Visit the jump instruction context
     * 
     * @param ctx The jump instruction context
     */
    @Override
    public Void visitJumpInstr(IJVMParser.JumpInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        String labelName = ctx.ID().getText();

        // Check if the label exists in the current method
        boolean labelExists = labelExistsInMethod(labelName);
        if (!labelExists) {
            addError("Label '" + labelName + "' is not defined in this scope", ctx.ID().getSymbol());
        }

        // Check stack requirements for conditional jumps
        if (ctx.IFLT() != null || ctx.IFEQ() != null) {
            handleSingleValueJump(ctx);
        } else if (ctx.IF_ICMPEQ() != null) {
            handleTwoValueJump(ctx);
        }

        return null;
    }

    private void handleSingleValueJump(IJVMParser.JumpInstrContext ctx)
    {
        if (stackSize < 1) {
            addWarning("Stack underflow: Conditional jump requires a value on the stack",
                    ctx.IFLT() != null ? ctx.IFLT().getSymbol() : ctx.IFEQ().getSymbol());
        } else {
            stackSize--; // Pop value for comparison
        }
    }

    private void handleTwoValueJump(IJVMParser.JumpInstrContext ctx)
    {
        if (stackSize < 2) {
            addWarning("Stack underflow: IF_ICMPEQ requires more (two?) values on the stack", ctx.IF_ICMPEQ().getSymbol());
        } else {
            stackSize -= 2; // Pop two values for comparison
        }
    }

    /**
     * Visit the zero-argument instruction context
     * 
     * @param ctx The zero-argument instruction context
     */
    @Override
    public Void visitZeroArgInstr(IJVMParser.ZeroArgInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        // Handle stack effects of zero-argument instructions
        if (ctx.IADD() != null || ctx.ISUB() != null || ctx.IAND() != null || ctx.IOR() != null) {
            handleBinaryOperation(ctx);
        }
        else if (ctx.POP() != null) {
            handlePop(ctx);
        }
        else if (ctx.SWAP() != null) {
            handleSwap(ctx);
        }
        else if (ctx.DUP() != null) {
            handleDup(ctx);
        }
        else if (ctx.IN() != null) {
            handleIn();
        }
        else if (ctx.OUT() != null) {
            handleOut(ctx);
        }
        else if (ctx.IRETURN() != null) {
            handleIReturn(ctx);
        }

        return null;
    }

    private void handleBinaryOperation(IJVMParser.ZeroArgInstrContext ctx)
    {
        // Binary operations require two values on the stack
        if (stackSize < 2) {
            addWarning("Stack underflow: Binary operation requires more (two?) values on the stack",
                    ctx.getStart());
        } else {
            stackSize--; // Pop two, push one result
        }
    }

    private void handlePop(IJVMParser.ZeroArgInstrContext ctx)
    {
        // POP removes one value from the stack
        if (stackSize < 1) {
            addWarning("Stack underflow: POP requires a value on the stack", ctx.POP().getSymbol());
        } else {
            stackSize--;
        }
    }

    private void handleSwap(IJVMParser.ZeroArgInstrContext ctx)
    {
        // SWAP requires two values on the stack
        if (stackSize < 2) {
            addWarning("Stack underflow: SWAP requires more (two?) values on the stack", ctx.SWAP().getSymbol());
        }
        // Stack size remains the same
    }

    private void handleDup(IJVMParser.ZeroArgInstrContext ctx)
    {
        // DUP requires one value and adds one
        if (stackSize < 1) {
            addWarning("Stack underflow: DUP requires a value on the stack", ctx.DUP().getSymbol());
        } else {
            stackSize++;
        }
    }

    private void handleIn()
    {
        // IN adds a value to the stack
        stackSize++;
    }

    private void handleOut(IJVMParser.ZeroArgInstrContext ctx)
    {
        // OUT pops a value from the stack
        if (stackSize < 1) {
            addWarning("Stack underflow: OUT requires a value on the stack", ctx.OUT().getSymbol());
        } else {
            stackSize--;
        }
    }

    private void handleIReturn(IJVMParser.ZeroArgInstrContext ctx)
    {
        // IRETURN requires a value on the stack
        if (stackSize < 1) {
            addWarning("Stack underflow: IRETURN requires a value on the stack", ctx.IRETURN().getSymbol());
        }
        // Reset stack after return
        stackSize = 0;
    }

    /**
     * Visit the byte argument instruction context
     * 
     * @param ctx The byte argument instruction context
     */
    @Override
    public Void visitByteArgInstr(IJVMParser.ByteArgInstrContext ctx)
    {
        if (isDeclarationPass || isLabelScanPass) {
            return null;
        }

        if (ctx.BIPUSH() != null) {
            // BIPUSH pushes a byte value onto the stack
            stackSize++;
        }
        else if (ctx.IINC() != null) {
            handleIInc(ctx);
        }

        return null;
    }

    private void handleIInc(IJVMParser.ByteArgInstrContext ctx)
    {
        // IINC needs a valid variable
        String varName = ctx.ID().getText();
        IJVMSymbolTable.SymbolEntry entry = symbolTable.lookupSymbol(varName);

        if (entry == null) {
            addError("Variable '" + varName + "' is not defined", ctx.ID().getSymbol());
            return;
        }

        if (entry.getType() != IJVMSymbolTable.SymbolType.VARIABLE) {
            addError("Symbol '" + varName + "' is not a variable", ctx.ID().getSymbol());
            return;
        }

        if (!entry.isInitialized()) {
            addError("Variable '" + varName + "' may not have been initialized before increment",
                    ctx.ID().getSymbol());
        }

        // IINC doesn't change the stack
    }
}
