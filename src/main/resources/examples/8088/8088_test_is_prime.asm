// 8088 Assembly Program to check if a number is prime
// Input: n (single digit number to check)
// Output: 1 if prime, 0 if not prime

_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
    // Read input number
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [number], AL

    // Check if number is prime
    MOVB AL, [number]
    CALL is_prime
    MOVB [result], AL

    // Print result
    XORB AH, AH
    MOVB AL, [result]
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS

    // Exit program
    PUSH 0
    PUSH _EXIT
    SYS

// Prime checking function
// Input: AL contains the number to check
// Output: AL contains 1 if prime, 0 if not prime
is_prime:
    PUSH BP
    MOV BP, SP
    PUSH BX
    PUSH CX

    MOVB BL, AL        // Store the number

    // Handle special cases
    CMPB AL, 0
    JE not_prime       // 0 is not prime
    CMPB AL, 1
    JE not_prime       // 1 is not prime
    CMPB AL, 2
    JE is_prime_yes    // 2 is prime

    // Check if even (divisible by 2)
    MOVB AL, BL
    MOVB CL, 2
    XOR AX, AX
    MOVB AL, BL
    DIVB CL
    CMPB AH, 0         // Check remainder
    JE not_prime       // If remainder is 0, it's even and not prime

    // Check odd divisors from 3 up to sqrt(n)
    MOVB CL, 3         // Start checking from 3

check_loop:
    // Check if CL * CL > BL (if CL > sqrt(BL))
    MOVB AL, CL
    MULB AL            // AL = CL * CL
    CMPB AL, BL
    JG is_prime_yes    // If CL² > n, then n is prime

    // Check if BL is divisible by CL
    XOR AX, AX
    MOVB AL, BL
    DIVB CL
    CMPB AH, 0         // Check remainder
    JE not_prime       // If remainder is 0, not prime

    // Move to next odd number
    ADDB CL, 2
    CMPB CL, 9         // Don't check beyond 9 for single digits
    JLE check_loop

is_prime_yes:
    MOVB AL, 1         // Return 1 (prime)
    JMP prime_end

not_prime:
    MOVB AL, 0         // Return 0 (not prime)

prime_end:
    POP CX
    POP BX
    POP BP
    RET

.SECT .DATA
    format: .ASCII "%d \0"

.SECT .BSS
    number: .SPACE 1   // Input number
    result: .SPACE 1   // Result (1 if prime, 0 if not)
