// Generated from C:/Users/conso/Desktop/projects/idea/antlr-impl/src/main/antlr4/IJVMParser.g4 by ANTLR 4.13.2

package xyz.atom7.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link IJVMParser}.
 */
public interface IJVMParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link IJVMParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(IJVMParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(IJVMParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#constantBlock}.
	 * @param ctx the parse tree
	 */
	void enterConstantBlock(IJVMParser.ConstantBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#constantBlock}.
	 * @param ctx the parse tree
	 */
	void exitConstantBlock(IJVMParser.ConstantBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#mainBlock}.
	 * @param ctx the parse tree
	 */
	void enterMainBlock(IJVMParser.MainBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#mainBlock}.
	 * @param ctx the parse tree
	 */
	void exitMainBlock(IJVMParser.MainBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#methodBlock}.
	 * @param ctx the parse tree
	 */
	void enterMethodBlock(IJVMParser.MethodBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#methodBlock}.
	 * @param ctx the parse tree
	 */
	void exitMethodBlock(IJVMParser.MethodBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#varBlock}.
	 * @param ctx the parse tree
	 */
	void enterVarBlock(IJVMParser.VarBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#varBlock}.
	 * @param ctx the parse tree
	 */
	void exitVarBlock(IJVMParser.VarBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#constantDecl}.
	 * @param ctx the parse tree
	 */
	void enterConstantDecl(IJVMParser.ConstantDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#constantDecl}.
	 * @param ctx the parse tree
	 */
	void exitConstantDecl(IJVMParser.ConstantDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void enterVarDecl(IJVMParser.VarDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void exitVarDecl(IJVMParser.VarDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#methodDecl}.
	 * @param ctx the parse tree
	 */
	void enterMethodDecl(IJVMParser.MethodDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#methodDecl}.
	 * @param ctx the parse tree
	 */
	void exitMethodDecl(IJVMParser.MethodDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#paramList}.
	 * @param ctx the parse tree
	 */
	void enterParamList(IJVMParser.ParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#paramList}.
	 * @param ctx the parse tree
	 */
	void exitParamList(IJVMParser.ParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(IJVMParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(IJVMParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#labelDecl}.
	 * @param ctx the parse tree
	 */
	void enterLabelDecl(IJVMParser.LabelDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#labelDecl}.
	 * @param ctx the parse tree
	 */
	void exitLabelDecl(IJVMParser.LabelDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#instruction}.
	 * @param ctx the parse tree
	 */
	void enterInstruction(IJVMParser.InstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#instruction}.
	 * @param ctx the parse tree
	 */
	void exitInstruction(IJVMParser.InstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#zeroArgInstr}.
	 * @param ctx the parse tree
	 */
	void enterZeroArgInstr(IJVMParser.ZeroArgInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#zeroArgInstr}.
	 * @param ctx the parse tree
	 */
	void exitZeroArgInstr(IJVMParser.ZeroArgInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#byteArgInstr}.
	 * @param ctx the parse tree
	 */
	void enterByteArgInstr(IJVMParser.ByteArgInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#byteArgInstr}.
	 * @param ctx the parse tree
	 */
	void exitByteArgInstr(IJVMParser.ByteArgInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#varArgInstr}.
	 * @param ctx the parse tree
	 */
	void enterVarArgInstr(IJVMParser.VarArgInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#varArgInstr}.
	 * @param ctx the parse tree
	 */
	void exitVarArgInstr(IJVMParser.VarArgInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#methodArgInstr}.
	 * @param ctx the parse tree
	 */
	void enterMethodArgInstr(IJVMParser.MethodArgInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#methodArgInstr}.
	 * @param ctx the parse tree
	 */
	void exitMethodArgInstr(IJVMParser.MethodArgInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#constantArgInstr}.
	 * @param ctx the parse tree
	 */
	void enterConstantArgInstr(IJVMParser.ConstantArgInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#constantArgInstr}.
	 * @param ctx the parse tree
	 */
	void exitConstantArgInstr(IJVMParser.ConstantArgInstrContext ctx);
	/**
	 * Enter a parse tree produced by {@link IJVMParser#jumpInstr}.
	 * @param ctx the parse tree
	 */
	void enterJumpInstr(IJVMParser.JumpInstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link IJVMParser#jumpInstr}.
	 * @param ctx the parse tree
	 */
	void exitJumpInstr(IJVMParser.JumpInstrContext ctx);
}