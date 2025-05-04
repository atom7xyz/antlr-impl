// Link: https://web.unica.it/static/resources/cms/documents/80880_8_9_2020.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 1;
// m = 5;
// output = 17 23 17 17 17 13;
//
_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
	PUSH _GETCHAR
	SYS
	SUBB AL, 0x30
	MOVB (n), AL

	PUSH _GETCHAR
	SYS

    PUSH _GETCHAR
	SYS
	SUBB AL, 0x30
	MOVB (m), AL

    PUSH _GETCHAR
	SYS

	PUSH v1
    PUSH v2
	PUSH v2-v1
	CALL fun

	PUSH v1
    PUSH v2
	PUSH v2-v1
	CALL print

	PUSH 0
	PUSH _EXIT
	SYS


fun:
	PUSH BP
	MOV BP, SP

	MOV BX, 0
    MOV SI, 8(BP)
	MOV DI, 6(BP)
	MOV CX, 4(BP)

for:

    XOR AX, AX
    MOVB AL, (BX)(DI)
    DIVB (BX)(SI)
    CMPB AH, 1
    JNE elseif

    XOR AX, AX
    MOVB AL, (m)
    MOVB DL, 2
    DIVB DL
    ADDB AL, (BX)(SI)
    MOVB (BX)(SI), AL

    JMP end_if
elseif:
    XOR AX, AX
    MOVB AH, (BX)(SI)
    SUBB AH, (BX)(DI)
    MOVB AL, (n)
    ADDB AL, (m)
    CMPB AH, AL
    JNGE else

    XOR AX, AX
    PUSH BX
    MOV BX, (m)
    MOVB AL, (BX)(SI)
    MULB (n)
    POP BX
    MOVB (BX)(SI), AL

    JMP end_if
else:
    MOVB AL, (BX)(SI)
    ADDB AL, 4(BP)
    MOVB (BX)(SI), AL
end_if:

    INC BX

    LOOP for

	POP BP
	RET

print:
	PUSH BP
	MOV BP, SP

	MOV DI, 8(BP)
	MOV SI, 6(BP)
    MOV CX, 4(BP)
	MOV AX, 0

ciclo_stampa:
	MOVB AL, (SI)
    ADDB AL, (DI)
    ADDB AL, 1
	PUSH AX
	PUSH s
	PUSH _PRINTF
	SYS

	INC SI
	INC DI
	LOOP ciclo_stampa

	MOV SP, BP
	POP BP
	RET

.SECT .DATA
	v1: .BYTE 2, 7, 5, 6, 4, 9
    v2: .BYTE 8, 9, 5, 4, 6, 1
	s: .ASCII "%d \0"

.SECT .BSS
	n: .SPACE 1
    m: .SPACE 1
