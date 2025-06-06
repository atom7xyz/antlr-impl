// Link: https://web.unica.it/static/resources/cms/documents/80880_12_2_2021.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 1;
// m = 2;
// output = 11 12 8 9 21 6;
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
    MOVB AH, (m)
    ADDB AH, (n)
    MOVB AL, (n)
    ADDB AL, BL
    CMPB AH, AL
    JL elseif

    XOR AX, AX
    MOVB AL, (m)
    MOVB DL, (n)
    DIVB DL
    MOVB AL, AH
    XORB AH, AH
    MOVB DL, (BX)(DI)
    MULB DL
    MOVB (BX)(SI), AL

    JMP endif

elseif:
    XOR AX, AX
    MOVB AH, (BX)(DI)
    MOVB AL, (n)
    ADDB AL, 3
    CMPB AH, AL
    JLE else

    MOVB AL, (BX)(DI)
    ADDB AL, (BX)(DI)
    MOVB (BX)(SI), AL

    JMP endif
else:
    XOR AX, AX
    MOVB AL, (BX)(DI)
    MOVB DL, (n)
    ADDB DL, (m)
    ADDB DL, 2
    MULB DL

    XORB AH, AH
    MOVB DL, 3
    DIVB DL

    MOVB (BX)(SI), AH


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
    ADDB AL, 3
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
