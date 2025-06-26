// 8088 Assembly Program to compute square roots and multiply them
// Input: num1, num2 (two single digit numbers)
// Output: sqrt(num1) * sqrt(num2)

_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
    // Read first number
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [num1], AL
    PUSH _GETCHAR      // Read semicolon
    SYS
    
    // Read second number
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [num2], AL
    PUSH _GETCHAR      // Read semicolon
    SYS
    
    // Calculate square root of num1
    MOVB AL, [num1]
    CALL sqrt_approx
    MOVB [sqrt1], AL
    
    // Calculate square root of num2
    MOVB AL, [num2]
    CALL sqrt_approx
    MOVB [sqrt2], AL
    
    // Multiply the square roots
    MOVB AL, [sqrt1]
    MOVB BL, [sqrt2]
    MULB BL
    
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

// Simple integer square root approximation function
// Input: AL contains the number
// Output: AL contains approximate square root
sqrt_approx:
    PUSH BP
    MOV BP, SP
    PUSH BX
    PUSH CX
    
    MOVB BL, AL        // Store original number
    CMPB AL, 0
    JE sqrt_zero
    CMPB AL, 1
    JE sqrt_one
    
    // For numbers 2-9, use lookup table approach
    MOVB CL, 1         // Start with guess = 1
    
sqrt_loop:
    MOVB AL, CL
    MULB AL            // AL = CL * CL
    CMPB AL, BL        // Compare CL^2 with original number
    JE sqrt_exact      // If equal, we found exact square root
    JG sqrt_approx_done // If greater, previous CL was the answer
    
    INC CL
    CMPB CL, 4         // Don't go beyond 3 (since 3^2 = 9)
    JLE sqrt_loop
    
sqrt_approx_done:
    DEC CL
    MOVB AL, CL
    JMP sqrt_end
    
sqrt_exact:
    MOVB AL, CL
    JMP sqrt_end
    
sqrt_zero:
    MOVB AL, 0
    JMP sqrt_end
    
sqrt_one:
    MOVB AL, 1
    
sqrt_end:
    POP CX
    POP BX
    POP BP
    RET

.SECT .DATA
    format: .ASCII "%d \0"

.SECT .BSS
    num1: .SPACE 1     // First input number
    num2: .SPACE 1     // Second input number
    sqrt1: .SPACE 1    // Square root of num1
    sqrt2: .SPACE 1    // Square root of num2
