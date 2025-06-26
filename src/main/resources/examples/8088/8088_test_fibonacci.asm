// 8088 Assembly Program to compute Fibonacci sequence
// Input: n (number of terms to compute)
// Output: First n terms of Fibonacci sequence

_PRINTF = 127
_GETCHAR = 117
_EXIT = 1

.SECT .TEXT
main:
    // Read input number n
    PUSH _GETCHAR
    SYS
    SUBB AL, 0x30      // Convert ASCII to number
    MOVB [n], AL

    // Check if n is 0
    MOVB AL, [n]
    CMPB AL, 0
    JE exit
    
    // Initialize Fibonacci sequence
    MOVB AL, 0         // F(0) = 0
    MOVB [fib_prev], AL
    MOVB AL, 1         // F(1) = 1
    MOVB [fib_curr], AL
    
    // Print first term (0)
    MOVB AL, [fib_prev]
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS
    
    // Check if n is 1
    MOVB AL, [n]
    CMPB AL, 1
    JE exit
    
    // Print second term (1)
    MOVB AL, [fib_curr]
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS
    
    // Check if n is 2
    MOVB AL, [n]
    CMPB AL, 2
    JE exit
    
    // Compute remaining terms
    MOVB BL, 2         // Counter starts at 2 (already printed 2 terms)
    
fib_loop:
    // Calculate next Fibonacci number: next = prev + curr
    MOVB AL, [fib_prev]
    ADDB AL, [fib_curr]
    MOVB [fib_next], AL
    
    // Print the next term
    XORB AH, AH
    PUSH AX
    PUSH format
    PUSH _PRINTF
    SYS
    
    // Update for next iteration: prev = curr, curr = next
    MOVB AL, [fib_curr]
    MOVB [fib_prev], AL
    MOVB AL, [fib_next]
    MOVB [fib_curr], AL
    
    // Increment counter and check if done
    INC BL
    CMPB BL, [n]
    JL fib_loop
    
exit:
    // Exit program
    PUSH 0
    PUSH _EXIT
    SYS

.SECT .DATA
    format: .ASCII "%d \0"

.SECT .BSS
    n: .SPACE 1          // Number of terms to compute
    fib_prev: .SPACE 1   // Previous Fibonacci number
    fib_curr: .SPACE 1   // Current Fibonacci number
    fib_next: .SPACE 1   // Next Fibonacci number
