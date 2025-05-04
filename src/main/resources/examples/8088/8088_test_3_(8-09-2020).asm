// Link: https://web.unica.it/static/resources/cms/documents/80883_8_9_2020.pdf
//
// Esempio:
// v1 = 2, 7, 5, 6, 4, 9;
// v2 = 8, 9, 5, 4, 6, 1;
// n = 1;
// output = 9 29 21 25 17 37;
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

    MOVB AL, (BX)(SI)
    ADDB AL, (BX)(DI)
    CMPB AL, (n)
    JL elseif

    XOR AX, AX
    MOVB AL, (n)
    MOVB DL, 2
    DIVB DL
    MOVB AH, (BX)(SI)
    SUBB AH, AL

    MOVB AL, AH
    XORB AH, AH
    MULB DL
    MOVB (BX)(SI), AL
    JMP endif

elseif:
    XOR AX, AX
    MOVB AL, (BX)(DI)
    MOVB DL, 3
    DIVB DL
    CMPB AH, 1
    JNE else

    XOR AX, AX
    MOVB AL, (BX)(DI)
    MULB (n)
    MOVB (BX)(SI), AL

    JMP endif
else:
    MOVB AL, (BX)(DI)
    ADDB AL, 3
    MOVB (BX)(SI), AL


endif:

    INC BX
    LOOP for

	POP BP
	RET

print:
	PUSH BP
	MOV BP, SP

	MOV DI, 6(BP)
	MOV CX, 4(BP)
	MOV AX, 0

ciclo_stampa:
	MOVB AL, (DI)
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
