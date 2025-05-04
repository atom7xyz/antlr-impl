// Link: https://web.unica.it/static/resources/cms/documents/80882_8_9_2020.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 3;
// output = 11 11 5 6 9 15;
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
    MOVB AL, (n)
    MOVB DL, 2
    DIVB DL
    MOVB AL, AH
    MOVB AH, (BX)(DI)
    CMPB AH, AL
    JG elseif

    PUSH BX
    MOV BX, (n)
    MOVB AL, (BX)(SI)
    POP BX
    ADDB AL, (BX)(SI)
    ADDB AL, (n)
    MOVB (BX)(SI), AL

    JMP end_if
elseif:
    MOVB AH, (BX)(DI)
    MOVB AL, (BX)(SI)
    ADDB AL, (n)
    CMPB AH, AL
    JL else

    ADDB (BX)(SI), 1

    JMP end_if
else:
    XOR AX, AX
    MOVB AL, (BX)(DI)
    MULB (n)

    XORB AH, AH
    MOVB DL, 5
    DIVB DL
    MOVB (BX)(SI), AH


end_if:

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
    MOVB AL, (SI)
    ADDB AL, (DI)
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
