// 8088 Assembly Program to calculate hypotenuse using Pythagorean theorem
// Input: a, b (two sides of a right triangle)
// Output: c = sqrt(a² + b²)

_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
    // Read first side (a)
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [side_a], AL
    PUSH _GETCHAR      // Read separator
    SYS

    // Read second side (b)
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [side_b], AL
    PUSH _GETCHAR      // Read newline
    SYS

    // Calculate a² using 16-bit arithmetic
    XOR AX, AX
    MOVB AL, [side_a]
    MUL AL             // AX = a²
    MOV [a_squared], AX

    // Calculate b² using 16-bit arithmetic
    XOR BX, BX
    MOVB BL, [side_b]
    MOV AX, BX
    MUL BL             // AX = b²
    MOV [b_squared], AX

    // Calculate a² + b²
    MOV AX, [a_squared]
    ADD AX, [b_squared]
    MOV [sum_squares], AX

    // Calculate square root of (a² + b²)
    MOV AX, [sum_squares]
    CALL sqrt_approx
    MOVB [hypotenuse], AL

    // Print hypotenuse
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS

    // Calculate perimeter (a + b + c)
    MOVB AL, [side_a]
    ADDB AL, [side_b]
    ADDB AL, [hypotenuse]

    // Print perimeter
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS

    // Calculate area (a * b / 2)
    MOVB AL, [side_a]
    MOVB BL, [side_b]
    MULB BL            // AL = a * b

    // Divide by 2 (shift right or divide)
    MOVB BL, 2
    DIVB BL            // AL = (a * b) / 2

    // Print area
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS

    // Exit program
    PUSH 0
    PUSH _EXIT
    SYS

// Integer square root approximation function
// Input: AX contains the number
// Output: AL contains approximate square root
sqrt_approx:
    PUSH BP
    MOV BP, SP
    PUSH BX
    PUSH CX
    PUSH DX

    MOV BX, AX         // Store original number in BX
    CMP AX, 0
    JE sqrt_zero
    CMP AX, 1
    JE sqrt_one

    // Simple iterative approach for 16-bit input
    MOV CX, 1          // Start with guess = 1

sqrt_loop:
    MOV AX, CX         // AX = current guess
    MUL CX             // AX = CX * CX
    CMP AX, BX         // Compare CX^2 with original number
    JE sqrt_exact      // If equal, we found exact square root
    JG sqrt_approx_done // If greater, previous CX was the answer

    INC CX
    CMP CX, 256        // Don't go beyond reasonable limit
    JL sqrt_loop

sqrt_approx_done:
    DEC CX             // Use previous value
    MOV AX, CX
    JMP sqrt_end

sqrt_exact:
    MOV AX, CX
    JMP sqrt_end
    JMP sqrt_end

sqrt_zero:
    MOV AX, 0
    JMP sqrt_end

sqrt_one:
    MOV AX, 1

sqrt_end:
    POP DX
    POP CX
    POP BX
    POP BP
    RET

.SECT .DATA
    format: .ASCII "%d \0"

.SECT .BSS
    side_a: .SPACE 1      // First side of triangle
    side_b: .SPACE 1      // Second side of triangle
    a_squared: .SPACE 2   // a² (16-bit)
    b_squared: .SPACE 2   // b² (16-bit)
    sum_squares: .SPACE 2 // a² + b² (16-bit)
    hypotenuse: .SPACE 1  // Calculated hypotenuse
