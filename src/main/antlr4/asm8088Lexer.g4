lexer grammar asm8088Lexer;

@header {
package xyz.atom7.parser;
}

// Directives
SECT: '.SECT' | '.sect';
BYTE: '.BYTE' | '.byte';
ASCII: '.ASCII' | '.ascii';
SPACE: '.SPACE' | '.space';
DATA: '.DATA' | '.data';
TEXT: '.TEXT' | '.text';
BSS: '.BSS' | '.bss';

// Assignment
EQUAL: '=';

// Instructions
INC: 'INC' | 'inc';
DEC: 'DEC' | 'dec';
JGE: 'JGE' | 'jge';
JG: 'JG' | 'jg';
JE: 'JE' | 'je';
JMP: 'JMP' | 'jmp';
JNE: 'JNE' | 'jne';
LOOP: 'LOOP' | 'loop';
CALL: 'CALL' | 'call';
RET: 'RET' | 'ret';
PUSH: 'PUSH' | 'push';
POP: 'POP' | 'pop';
SYS: 'SYS' | 'sys';
XOR: 'XOR' | 'xor';
MOV: 'MOV' | 'mov';
DIV: 'DIV' | 'div';
SUB: 'SUB' | 'sub';
CMP: 'CMP' | 'cmp';
ADD: 'ADD' | 'add';
JLE: 'JLE' | 'jle';
JNGE: 'JNGE' | 'jnge';
JNG: 'JNG' | 'jng';
JL: 'JL' | 'jl';
JNZ: 'JNZ' | 'jnz';
JZ: 'JZ' | 'jz';
MUL: 'MUL' | 'mul';

// Byte instructions
MOVB: 'MOVB' | 'movb';
SUBB: 'SUBB' | 'subb';
DIVB: 'DIVB' | 'divb';
CMPB: 'CMPB' | 'cmpb';
ADDB: 'ADDB' | 'addb';
XORB: 'XORB' | 'xorb';
MULB: 'MULB' | 'mulb';

// Registers
AX: 'AX' | 'ax';
BX: 'BX' | 'bx';
CX: 'CX' | 'cx';
DX: 'DX' | 'dx';
SI: 'SI' | 'si';
DI: 'DI' | 'di';
BP: 'BP' | 'bp';
SP: 'SP' | 'sp';
AL: 'AL' | 'al';
AH: 'AH' | 'ah';
BL: 'BL' | 'bl';
BH: 'BH' | 'bh';
CL: 'CL' | 'cl';
CH: 'CH' | 'ch';
DL: 'DL' | 'dl';
DH: 'DH' | 'dh';

// Segment Registers
CS: 'CS' | 'cs';
DS: 'DS' | 'ds';
ES: 'ES' | 'es';
SS: 'SS' | 'ss';

// Symbols
COLON: ':';
COMMA: ',';
LPAREN: '(';
RPAREN: ')';
PLUS: '+';
MINUS: '-';

// Numbers
HEX: '0' [xX] [0-9a-fA-F]+;
NUM: '-'? [0-9]+;

// Identifiers (labels, variables)
ID: [a-zA-Z_][a-zA-Z0-9_]*;

// String literals for .ASCII
STRING: '"' (~["\r\n])* '"';

// Whitespace and comments
NEWLINE: [\r\n]+;
WS: [ \t]+ -> skip;
COMMENT: '//' ~[\r\n]* -> skip;
SEMICOLON_COMMENT: ';' ~[\r\n]* -> skip;