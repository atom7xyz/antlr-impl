lexer grammar IJVMLexer;

@header {
package xyz.atom7.parser;
}

// Keywords for blocks
CONSTANT: '.constant';
END_CONSTANT: '.end-constant';
MAIN: '.main';
END_MAIN: '.end-main';
METHOD: '.method';
END_METHOD: '.end-method';
VAR: '.var';
END_VAR: '.end-var';

// Instruction keywords
HALT: 'HALT' | 'halt';
NOP: 'NOP' | 'nop';
IADD: 'IADD' | 'iadd';
IAND: 'IAND' | 'iand';
IOR: 'IOR' | 'ior';
ISUB: 'ISUB' | 'isub';
POP: 'POP' | 'pop';
SWAP: 'SWAP' | 'swap';
DUP: 'DUP' | 'dup';
ERR: 'ERR' | 'err';
IN: 'IN' | 'in';
OUT: 'OUT' | 'out';
IRETURN: 'IRETURN' | 'ireturn';
BIPUSH: 'BIPUSH' | 'bipush';
IINC: 'IINC' | 'iinc';
ILOAD: 'ILOAD' | 'iload';
ISTORE: 'ISTORE' | 'istore';
INVOKEVIRTUAL: 'INVOKEVIRTUAL' | 'invokevirtual';
LDC_W: 'LDC_W' | 'ldc_w';
IFLT: 'IFLT' | 'iflt';
IFEQ: 'IFEQ' | 'ifeq';
IF_ICMPEQ: 'IF_ICMPEQ' | 'if_icmpeq';
GOTO: 'GOTO' | 'goto';

// Symbols
LPAREN: '(';
RPAREN: ')';
COMMA: ',';
COLON: ':';

// Number formats
fragment DECIMAL_DIGIT: '-'? [0-9];
fragment HEX_DIGIT: [0-9a-fA-F];
fragment OCTAL_DIGIT: [0-7];
fragment HEX_PREFIX: '0' [xX];
fragment OCTAL_PREFIX: [oO];
fragment NUM_HEX: HEX_PREFIX HEX_DIGIT+;
fragment NUM_OCTAL: OCTAL_PREFIX OCTAL_DIGIT+;
fragment NUM_DECIMAL: DECIMAL_DIGIT+;
NUM: (NUM_HEX | NUM_OCTAL | NUM_DECIMAL);

// Identifiers
ID: [a-zA-Z_][a-zA-Z0-9_\-]*;

// Whitespace and comments
NEWLINE: [\r\n]+;
WS: [ \t]+ -> skip;
COMMENT: '//' ~[\r\n]* -> skip;
SEMICOLON_COMMENT: ';' ~[\r\n]* -> skip;