// Link: https://web.unica.it/static/resources/cms/documents/80883_12_2_2021.pdf
//
// Esempio:
// v = 2, 7, 5, 6, 4, 9;
// n = 5;
// m = 5;
// output = 1 4 3 3 2 6;
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

	PUSH v
	PUSH s-v
	CALL fun

	PUSH v
	PUSH s-v
	CALL print

	PUSH 0
	PUSH _EXIT
	SYS


fun:
	PUSH BP
	MOV BP, SP

	MOV BX, 0
    MOV SI, 6(BP)
	MOV CX, 4(BP)

for:
    XOR AX, AX
    MOVB AH, (BX)(SI)

    PUSH BX
    MOVB BL, (n)
    MOVB AL, (BX)(SI)
    POP BX

    CMPB AH, AL
    JGE elseif

    XOR AX, AX
    PUSH BX
    MOVB BL, (n)
    MOVB AL, (BX)(SI)
    POP BX
    MOVB DL, (n)
    DIVB DL

    XORB AH, AH
    MOVB DL, (m)
    DIVB DL

    ADDB AH, (BX)(SI)

    MOVB (BX)(SI), AH

    JMP endif

elseif:
    XOR AX, AX

    MOVB AH, BL
    MOVB AL, 4(BP)
    SUBB AL, 3
    CMPB AH, AL
    JG realelseif

    MOVB AH, (BX)(SI)
    MOVB AL, 2
    CMPB AH, AL
    JE realelseif

    XOR AX, AX
    MOVB AL, (n)
    ADDB AL, (BX)(SI)
    MOVB DL, 2
    DIVB DL

    MOVB (BX)(SI), AH

    JMP endif
realelseif:
    XOR AX, AX
    MOVB AL, (BX)(SI)
    MOVB DL, 5
    DIVB DL
    ADDB AH, (BX)(SI)

    MOVB (BX)(SI), AH


endif:

    INC BX
    LOOP for

	POP BP
	RET

print:
	PUSH BP
	MOV BP, SP

    MOV SI, 6(BP)
	MOV CX, 4(BP)
	MOV AX, 0

ciclo_stampa:
    MOVB AL, (SI)
    MOVB DL, 2
    DIVB DL

    XORB AH, AH

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
	v: .BYTE 2, 7, 5, 6, 4, 9
	s: .ASCII "%d \0"

.SECT .BSS
	n: .SPACE 1
    m: .SPACE 1
