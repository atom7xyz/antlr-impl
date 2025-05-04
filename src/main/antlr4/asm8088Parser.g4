parser grammar asm8088Parser;

@header {
package xyz.atom7.parser;
}

options {
    tokenVocab = asm8088Lexer;
}

// Program structure
program: NEWLINE* line* EOF;

// Lines in the program
line
    : labelDecl NEWLINE
    | labelDecl statement? NEWLINE  // Label followed by optional statement
    | assignment NEWLINE
    | section NEWLINE
    | statement? NEWLINE
    ;

// Assignment of constants/variables
assignment: ID EQUAL (HEX | NUM);

// Section directives
section: SECT (DATA | TEXT | BSS);

// Label declaration
labelDecl: ID COLON;

// Statements
statement: instruction | directive;

// Instructions with optional operands
instruction: mnemonic operandList?;

// Group instructions by their operand requirements
mnemonic:
      // No operand instructions
      RET
      // Single operand instructions
    | (INC | DEC | PUSH | POP | CALL | JMP | JE | JNE | JZ | JNZ | JG | JGE | JL | JLE | JNGE | JNG | LOOP)
      // Two operand instructions (word)
    | (MOV | ADD | SUB | CMP | DIV | XOR | MUL)
      // Two operand instructions (byte)
    | (MOVB | ADDB | SUBB | CMPB | DIVB | XORB | MULB)
      // Special instructions
    | SYS
    ;

// List of operands (comma-separated) with count validation
operandList:
      operand                       // For single-operand instructions
    | operand COMMA operand         // For two-operand instructions
    | operand (COMMA operand)*      // For variable number of operands (like PUSH/SYS)
    ;

// Operand
operand
    : register
    | immediate
    | memory
    | expr
    ;

// Register names
register: AX | BX | CX | DX | SI | DI | BP | SP | AL | AH | BL | BH | CL | CH | DL | DH | CS | DS | ES | SS;

// Immediate value (number)
immediate: HEX | NUM;

// Arithmetic expressions (e.g., s-v, 5+BP, etc.)
expr
    : ID ((PLUS | MINUS) (ID | NUM))*  // Label arithmetic like v2-v1
    | NUM ((PLUS | MINUS) (ID | NUM))*
    ;

// Memory addressing modes
memory
    : LPAREN ID RPAREN
    | LPAREN register RPAREN
    | NUM LPAREN register RPAREN
    | LPAREN register RPAREN NUM
    | ID LPAREN register RPAREN
    | LPAREN register RPAREN ID
    | LPAREN register RPAREN LPAREN register RPAREN  // (BX)(DI) format
    | LPAREN register RPAREN LPAREN register RPAREN LPAREN immediate RPAREN
    | LPAREN ID RPAREN LPAREN register RPAREN
    | LPAREN register RPAREN LPAREN ID RPAREN
    ;

// Data directives (.BYTE, .ASCII, .SPACE)
directive
    : BYTE valueList
    | ASCII STRING
    | SPACE NUM
    ;

// List of values for .BYTE
valueList: (HEX | NUM) (COMMA (HEX | NUM))*;