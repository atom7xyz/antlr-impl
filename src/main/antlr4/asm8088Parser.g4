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

// Group instructions by their operand requirements and types
mnemonic:
      // No operand instructions
      RET

      // Single operand instructions
    | INC | DEC | PUSH | POP | CALL | JMP | NOT | MUL | DIV
    | JE | JZ | JNE | JNZ | JG | JNLE | JGE | JNL | JL | JNGE | JLE | JNG
    | JB | JNAE | JBE | JNA | JA | JNBE | JAE | JNB | JS | JNS | JO | JNO
    | JP | JPE | JNP | JPO | JC | JNC | JCXZ | LOOP

      // Two operand instructions (most common)
    | MOV | ADD | ADC | SUB | SBB | CMP | AND | OR | XOR

      // Legacy byte instructions
    | MOVB | ADDB | SUBB | CMPB | DIVB | XORB | MULB

      // System call
    | SYS
    ;

// List of operands with validation based on instruction type
operandList:
      operand                               // Single operand
    | operand COMMA operand                 // Two operands
    | operand COMMA operand COMMA operand   // Three operands (rare, for some variants)
    ;

// Operand types
operand
    : register
    | immediate
    | memory
    | expr
    ;

// Register names
register:
      // 16-bit general purpose registers
      AX | BX | CX | DX | SI | DI | BP | SP
      // 8-bit general purpose registers
    | AL | AH | BL | BH | CL | CH | DL | DH
      // Segment registers
    | CS | DS | ES | SS;

// Immediate value (number or hex)
immediate: HEX | NUM;

// Arithmetic expressions (e.g., s-v, 5+BP, etc.)
expr
    : ID ((PLUS | MINUS) (ID | NUM | HEX))*  // Label arithmetic like v2-v1
    | NUM ((PLUS | MINUS) (ID | NUM | HEX))*
    | HEX ((PLUS | MINUS) (ID | NUM | HEX))*
    ;

// Memory addressing modes for 8088
memory
    : LBRACKET memoryExpression RBRACKET    // Standard [expression] format
    | LPAREN memoryExpression RPAREN        // Alternative (expression) format for compatibility
    ;

// Memory expression inside brackets/parentheses
memoryExpression
    : immediate                                          // Direct addressing [1234h]
    | register                                           // Register indirect [BX]
    | register PLUS immediate                            // Based with displacement [BX+8]
    | register MINUS immediate                           // Based with negative displacement [BX-8]
    | immediate PLUS register                            // Displacement + base [8+BX]
    | register PLUS register                             // Based indexed [BX+SI]
    | register PLUS register PLUS immediate              // Based indexed with displacement [BX+SI+8]
    | register PLUS register MINUS immediate             // Based indexed with negative displacement [BX+SI-8]
    | immediate PLUS register PLUS register              // Displacement + based indexed [8+BX+SI]
    | ID                                                 // Label reference [label]
    | ID PLUS immediate                                  // Label + offset [label+4]
    | ID MINUS immediate                                 // Label - offset [label-4]
    | ID PLUS register                                   // Label + register [label+BX]
    | ID PLUS register PLUS immediate                    // Label + register + offset [label+BX+4]
    | register PLUS ID                                   // Register + label [BX+label]
    | register PLUS ID PLUS immediate                    // Register + label + offset [BX+label+4]
    | register PLUS register PLUS ID                     // Based indexed + label [BX+SI+label]
    ;

// Data directives (.BYTE, .ASCII, .SPACE)
directive
    : BYTE valueList
    | ASCII STRING
    | SPACE NUM
    ;

// List of values for .BYTE directive
valueList: (HEX | NUM) (COMMA (HEX | NUM))*;