// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

(RESET_I)
    // Set variable i to 8192, the LAST register of SCREEN map
    @8192
    D=A
    @i
    M=D

(LISTEN)
    // Listen to the keyboard, and if 0, no key pressed, jump to blank
    @KBD
    D=M
    @BLANK
    D;JEQ

    // Key is pressed, get current memory register (i), then get location
    // by adding it to SCREEN, then color it black by setting M=-1 (1111111111111111)
    @i
    D=M
    @SCREEN
    A=D+A
    M=-1

    // Skip the BLANK
    @CONTINUE
    0;JMP

(BLANK)
    // Key was not pressed so color the 16 pixels white (M=0)
    @i
    D=M
    @SCREEN
    A=D+A
    M=0

(CONTINUE)
    // Reduce value of i by 1
    @i
    MD=M-1

    // If i < 0, the entire screen has been painted, so reset i, and begin again
    @RESET_I
    D;JLT

    // Listen for key presses again
    @LISTEN
    0;JMP
    
(END)
    @END
    0;JMP
    