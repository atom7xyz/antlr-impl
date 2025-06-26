// 8088 Assembly Program to calculate GCD using Euclidean Algorithm
// Input: a, b (two numbers)
// Output: GCD(a, b)

_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
    // Read first number (a)
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [num_a], AL
    PUSH _GETCHAR      // Read separator
    SYS

    // Read second number (b)
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [num_b], AL
    PUSH _GETCHAR      // Read newline
    SYS

    // Calculate GCD using Euclidean algorithm
    MOVB AL, [num_a]
    MOVB BL, [num_b]
    CALL euclidean_gcd

    // Print result
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS

    // Exit program
    PUSH 0
    PUSH _EXIT
    SYS

// Euclidean Algorithm for GCD
// Input: AL = first number, BL = second number
// Output: AL = GCD(AL, BL)
euclidean_gcd:
    PUSH BP
    MOV BP, SP
    PUSH CX
    PUSH DX

    // Handle case where one number is 0
    CMPB AL, 0
    JE gcd_b_result    // If a = 0, GCD = b
    CMPB BL, 0
    JE gcd_a_result    // If b = 0, GCD = a

    // Make sure AL >= BL (swap if necessary)
    CMPB AL, BL
    JGE gcd_loop

    // Swap AL and BL
    MOVB CL, AL
    MOVB AL, BL
    MOVB BL, CL

gcd_loop:
    // Euclidean step: AL = AL mod BL
    XOR AH, AH         // Clear AH for division
    DIVB BL            // AX / BL, quotient in AL, remainder in AH

    // Check if remainder is 0
    CMPB AH, 0
    JE gcd_done        // If remainder = 0, GCD = BL

    // Prepare for next iteration: AL = BL, BL = remainder
    MOVB AL, BL        // AL = old BL
    MOVB BL, AH        // BL = remainder

    JMP gcd_loop

gcd_done:
    MOVB AL, BL        // GCD = BL (the last non-zero remainder)
    JMP gcd_end

gcd_b_result:
    MOVB AL, BL        // Return b if a = 0
    JMP gcd_end

gcd_a_result:
    // AL already contains a, so no change needed

gcd_end:
    POP DX
    POP CX
    POP BP
    RET

.SECT .DATA
    format: .ASCII "%d \0"

.SECT .BSS
    num_a: .SPACE 1    // First input number
    num_b: .SPACE 1    // Second input number
