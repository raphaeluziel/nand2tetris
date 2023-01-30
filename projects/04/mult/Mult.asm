// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

//  set a=0, b=0, product=0, ander=1    // ander checks if bit is 0 or 1
//  
//  ADD LOOP
//  if (b & ander == 0)  // the bit is a 0
//      do NOT add, go to SHIFT
//  else
//      add a to product (R2)
//
//  a = a + a       // double a to shift bits left
//  if (a == 0)     // shifted all bits beyond 16
//      END program
//
//  ander = ander + ander       // shift ander to next bit
//  go to ADD LOOP

    // Set a to R0 input
    @R0
    D=M
    @a
    M=D

    // Set b to R1 input
    @R1
    D=M
    @b
    M=D

    // Set product, R2 to 0
    @R2
    M=0

    // Set ander to 1
    @ander
    M=1

(ADD)
    // Check if current bit place in b is 0 or 1
    // If it's zero go to SHIFT, else add to product
    @b
    D=M
    @ander
    D=D&M
    @SHIFT
    D;JEQ

    // The bit is 1 so add to product
    @a
    D=M
    @R2
    M=D+M

(SHIFT)
    // Shift a's bits to left one place (multiply by 2)
    @a
    D=M
    MD=D+M

    // If shifted all 1's out of the 16 bits, END program
    @END
    D;JEQ

    // Shift ander's bits 1 place to left to check next bit
    @ander
    D=M
    M=D+M

    @ADD
    0;JMP

(END)
    @END
    0;JMP