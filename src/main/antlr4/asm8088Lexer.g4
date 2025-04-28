lexer grammar asm8088Lexer;

@header {
package xyz.atom7.parser;
}

// Directives
SECT: '.SECT';
BYTE: '.BYTE';
ASCII: '.ASCII';
SPACE: '.SPACE';
DATA: '.DATA';
TEXT: '.TEXT';
BSS: '.BSS';

// Assignment
EQUAL: '=';

// Instructions
INC: 'INC';
DEC: 'DEC';
JGE: 'JGE';
JG: 'JG';
JE: 'JE';
JMP: 'JMP';
JNE: 'JNE';
LOOP: 'LOOP';
CALL: 'CALL';
RET: 'RET';
PUSH: 'PUSH';
POP: 'POP';
SYS: 'SYS';
XOR: 'XOR';
MOV: 'MOV';
DIV: 'DIV';
SUB: 'SUB';
CMP: 'CMP';
ADD: 'ADD';

// Byte instructions
MOVB: 'MOVB';
SUBB: 'SUBB';
DIVB: 'DIVB';
CMPB: 'CMPB';
ADDB: 'ADDB';
XORB: 'XORB';

// Registers
AX: 'AX';
BX: 'BX';
CX: 'CX';
DX: 'DX';
SI: 'SI';
DI: 'DI';
BP: 'BP';
SP: 'SP';
AL: 'AL';
AH: 'AH';
BL: 'BL';
BH: 'BH';
CL: 'CL';
CH: 'CH';
DL: 'DL';
DH: 'DH';

// Symbols
COLON: ':';
COMMA: ',';
LPAREN: '(';
RPAREN: ')';
PLUS: '+';
MINUS: '-';

// Numbers
HEX: '0x' [0-9a-fA-F]+;
NUM: [0-9]+;

// Identifiers (labels, variables)
ID: [a-zA-Z_][a-zA-Z0-9_]*;

// String literals for .ASCII
STRING: '"' (~["\r\n])* '"';

// Whitespace and comments
NEWLINE: [\r\n]+;
WS: [ \t]+ -> skip;
COMMENT: ';' ~[\r\n]* -> skip;