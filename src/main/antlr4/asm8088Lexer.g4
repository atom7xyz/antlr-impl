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

// Data Transfer Instructions
MOV: 'MOV' | 'mov';
PUSH: 'PUSH' | 'push';
POP: 'POP' | 'pop';

// Arithmetic Instructions
ADD: 'ADD' | 'add';
ADC: 'ADC' | 'adc';
SUB: 'SUB' | 'sub';
SBB: 'SBB' | 'sbb';
MUL: 'MUL' | 'mul';
DIV: 'DIV' | 'div';
INC: 'INC' | 'inc';
DEC: 'DEC' | 'dec';
CMP: 'CMP' | 'cmp';

// Logic Instructions
AND: 'AND' | 'and';
OR: 'OR' | 'or';
XOR: 'XOR' | 'xor';
NOT: 'NOT' | 'not';

// Control Transfer Instructions
JMP: 'JMP' | 'jmp';
JE: 'JE' | 'je';
JZ: 'JZ' | 'jz';
JNE: 'JNE' | 'jne';
JNZ: 'JNZ' | 'jnz';
JL: 'JL' | 'jl';
JNGE: 'JNGE' | 'jnge';
JLE: 'JLE' | 'jle';
JNG: 'JNG' | 'jng';
JG: 'JG' | 'jg';
JNLE: 'JNLE' | 'jnle';
JGE: 'JGE' | 'jge';
JNL: 'JNL' | 'jnl';
JB: 'JB' | 'jb';
JNAE: 'JNAE' | 'jnae';
JBE: 'JBE' | 'jbe';
JNA: 'JNA' | 'jna';
JA: 'JA' | 'ja';
JNBE: 'JNBE' | 'jnbe';
JAE: 'JAE' | 'jae';
JNB: 'JNB' | 'jnb';
JS: 'JS' | 'js';
JNS: 'JNS' | 'jns';
JO: 'JO' | 'jo';
JNO: 'JNO' | 'jno';
JP: 'JP' | 'jp';
JPE: 'JPE' | 'jpe';
JNP: 'JNP' | 'jnp';
JPO: 'JPO' | 'jpo';
JC: 'JC' | 'jc';
JNC: 'JNC' | 'jnc';
JCXZ: 'JCXZ' | 'jcxz';
LOOP: 'LOOP' | 'loop';
CALL: 'CALL' | 'call';
RET: 'RET' | 'ret';

// 8-bit instructions
MOVB: 'MOVB' | 'movb';
SUBB: 'SUBB' | 'subb';
DIVB: 'DIVB' | 'divb';
CMPB: 'CMPB' | 'cmpb';
ADDB: 'ADDB' | 'addb';
XORB: 'XORB' | 'xorb';
MULB: 'MULB' | 'mulb';

// System call
SYS: 'SYS' | 'sys';

// 16-bit General Purpose Registers
AX: 'AX' | 'ax';
BX: 'BX' | 'bx';
CX: 'CX' | 'cx';
DX: 'DX' | 'dx';
SI: 'SI' | 'si';
DI: 'DI' | 'di';
BP: 'BP' | 'bp';
SP: 'SP' | 'sp';

// 8-bit General Purpose Registers
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

// Symbols and operators
COLON: ':';
COMMA: ',';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
PLUS: '+';
MINUS: '-';

// Numbers and literals
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