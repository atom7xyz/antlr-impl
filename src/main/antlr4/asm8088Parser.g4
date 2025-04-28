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
    : assignment NEWLINE         // Constant/variable assignment
    | section NEWLINE            // Section directive
    | labelDecl                  // Label declaration
    | statement? NEWLINE         // Instruction or directive (or empty line)
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

mnemonic:
      MOV | MOVB | SUB | SUBB | DIV | DIVB | CMP | CMPB | ADD | ADDB | XOR | XORB
    | INC | DEC | JGE | JG | JE | JMP | JNE | LOOP | CALL | RET | PUSH | POP | SYS
    ;

// List of operands (comma-separated)
operandList: operand (COMMA operand)*;

// Operand
operand
    : register
    | immediate
    | memory
    | expr
    ;

// Register names
register: AX | BX | CX | DX | SI | DI | BP | SP | AL | AH | BL | BH | CL | CH | DL | DH;

// Immediate value (number)
immediate: HEX | NUM;

// Arithmetic expressions (e.g., s-v, 5+BP, etc.)
expr
    : ID ( (PLUS | MINUS) ID | (PLUS | MINUS) NUM )*
    | NUM ( (PLUS | MINUS) ID | (PLUS | MINUS) NUM )*
    ;

// Memory addressing modes
memory
    : LPAREN ID RPAREN
    | LPAREN register RPAREN
    | NUM LPAREN register RPAREN
    | LPAREN register RPAREN NUM
    | ID LPAREN register RPAREN
    | LPAREN register RPAREN ID
    | LPAREN register RPAREN LPAREN register RPAREN
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