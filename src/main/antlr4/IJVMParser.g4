parser grammar IJVMParser;

@header {
package xyz.atom7.parser;
}

options {
    tokenVocab = IJVMLexer;
}

// Program structure
program: NEWLINE* constantBlock? mainBlock methodBlock* EOF;

// Blocks
constantBlock: CONSTANT (constantDecl? NEWLINE)* END_CONSTANT NEWLINE+;
mainBlock: MAIN (varBlock | statement? NEWLINE)* END_MAIN NEWLINE+;
methodBlock: methodDecl (varBlock | statement? NEWLINE)* END_METHOD NEWLINE*;
varBlock: VAR (varDecl? NEWLINE)* END_VAR;

// Declarations
constantDecl: ID NUM;
varDecl: ID;
methodDecl: METHOD ID LPAREN paramList? RPAREN;
paramList: ID (COMMA ID)*;

// Statements
statement: instruction | labelDecl;
labelDecl: ID COLON;

// Instructions
instruction: zeroArgInstr
           | byteArgInstr
           | varArgInstr
           | methodArgInstr
           | constantArgInstr
           | jumpInstr;

// Zero-argument instructions
zeroArgInstr: HALT | NOP | IADD | IAND | IOR | ISUB | POP | SWAP | DUP | ERR | IN | OUT | IRETURN;

// Instructions with byte arguments
byteArgInstr: BIPUSH NUM | IINC ID NUM;

// Instructions with variable arguments
varArgInstr: ILOAD ID | ISTORE ID;

// Instructions with method arguments
methodArgInstr: INVOKEVIRTUAL ID;

// Instructions with constant arguments
constantArgInstr: LDC_W ID;

// Jump instructions
jumpInstr: IFLT ID | IFEQ ID | IF_ICMPEQ ID | GOTO ID;