// Link: https://web.unica.it/static/resources/cms/documents/80881_12_2_2021.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 1;
// m = 2;
// output = 13 24 16 17 15 11;
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
    MOVB AH, (BX)(SI)
    ADDB AH, (m)
    MOVB AL, (n)
    ADDB AL, (m)
    CMPB AH, AL
    JGE elseif

    XOR AX, AX
    MOVB AL, (BX)(DI)
    MOVB DL, 4
    DIVB DL

    ADDB AH, (m)
    SUBB AH, (n)
    MOVB (BX)(DI), AH

    JMP endif

elseif:
    XOR AX, AX
    MOVB AH, (BX)(DI)
    MOVB AL, BL
    CMPB AH, AL
    JL else

    MOVB AL, (BX)(DI)
    ADDB AL, (m)
    SUBB AL, (n)
    ADDB AL, (BX)(SI)
    MOVB (BX)(DI), AL

    JMP endif
else:
    XOR AX, AX
    MOVB AL, (BX)(DI)
    MOVB DL, (m)
    MULB DL

    XORB AH, AH
    MOVB DL, 5
    DIVB DL

    MOVB (BX)(DI), AH


endif:

    INC BX
    LOOP for

	POP BP
	RET

print:
	PUSH BP
	MOV BP, SP

    MOV SI, 8(BP)
	MOV DI, 6(BP)
	MOV CX, 4(BP)
	MOV AX, 0

ciclo_stampa:
    MOVB AL, (DI)
    ADDB AL, (SI)
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
