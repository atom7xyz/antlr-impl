// Generated from C:/Users/conso/Desktop/projects/idea/antlr-impl/src/main/antlr4/IJVMParser.g4 by ANTLR 4.13.2

package xyz.atom7.parser;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link IJVMParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface IJVMParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link IJVMParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(IJVMParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#constantBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantBlock(IJVMParser.ConstantBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#mainBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMainBlock(IJVMParser.MainBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#methodBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBlock(IJVMParser.MethodBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#varBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarBlock(IJVMParser.VarBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#constantDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDecl(IJVMParser.ConstantDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(IJVMParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#methodDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDecl(IJVMParser.MethodDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#paramList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamList(IJVMParser.ParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(IJVMParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#labelDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelDecl(IJVMParser.LabelDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#instruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstruction(IJVMParser.InstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#zeroArgInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZeroArgInstr(IJVMParser.ZeroArgInstrContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#byteArgInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitByteArgInstr(IJVMParser.ByteArgInstrContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#varArgInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarArgInstr(IJVMParser.VarArgInstrContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#methodArgInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodArgInstr(IJVMParser.MethodArgInstrContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#constantArgInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantArgInstr(IJVMParser.ConstantArgInstrContext ctx);
	/**
	 * Visit a parse tree produced by {@link IJVMParser#jumpInstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJumpInstr(IJVMParser.JumpInstrContext ctx);
}