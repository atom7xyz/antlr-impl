// Link: https://web.unica.it/static/resources/cms/documents/80881_8_9_2020.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 3;
// output = 2 12 0 2 2 20
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
    MOVB AL, BL
    MOVB DL, 3
    MULB DL
    MOVB AH, 4(BP)
    CMPB AH, AL
    JNG elseif

    MOVB AL, (BX)(SI)

    PUSH BX
    MOV BX, (n)

    SUBB AL, (BX)(DI)
    ADDB AL, (n)

    POP BX

    MOVB (BX)(SI), AL

    JMP end_if
elseif:
    MOVB AH, (BX)(SI)
    MOVB AL, (n)
    ADDB AL, 5
    CMPB AH, AL
    JNG else

    MOVB AL, (BX)(SI)
    ADDB AL, (BX)(DI)
    MOVB (BX)(SI), AL

    JMP end_if
else:

    XOR AX, AX
    MOVB AL, (n)
    ADDB AL, (BX)(SI)
    MOVB DL, 2
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

	MOV SI, 6(BP)
    MOV CX, 4(BP)
	MOV AX, 0

ciclo_stampa:
    MOVB AL, (SI)
    ADDB AL, (SI)
	PUSH AX
	PUSH s
	PUSH _PRINTF
	SYS

	INC SI
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
