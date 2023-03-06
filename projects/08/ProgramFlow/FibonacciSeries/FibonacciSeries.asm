// push argument 1
@1
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// pop pointer 1           
@SP
AM=M-1
D=M
@THAT
M=D

// push constant 0
@0
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop that 0              
@0
D=A
@THAT
A=M
D=A+D
@SP
AM=M-1
A=M
D=A+D
A=D-A
D=D-A
M=D

// push constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1

// pop that 1              
@1
D=A
@THAT
A=M
D=A+D
@SP
AM=M-1
A=M
D=A+D
A=D-A
D=D-A
M=D

// push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// push constant 2
@2
D=A
@SP
A=M
M=D
@SP
M=M+1

// sub
@SP
A=M
A=A-1
D=M
A=A-1
M=M-D
@SP
M=M-1

// pop argument 0          
@0
D=A
@ARG
A=M
D=A+D
@SP
AM=M-1
A=M
D=A+D
A=D-A
D=D-A
M=D

(MAIN_LOOP_START)
// push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// if-goto COMPUTE_ELEMENT 
@SP
A=M-1
D=M
@SP
M=M-1
@COMPUTE_ELEMENT
D;JNE

// goto END_PROGRAM        
@END_PROGRAM
0;JEQ

(COMPUTE_ELEMENT)
// push that 0
@0
D=A
@THAT
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// push that 1
@1
D=A
@THAT
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
A=M
A=A-1
D=M
A=A-1
M=D+M
@SP
M=M-1

// pop that 2              
@2
D=A
@THAT
A=M
D=A+D
@SP
AM=M-1
A=M
D=A+D
A=D-A
D=D-A
M=D

// push pointer 1
@THAT
D=M
@SP
A=M
M=D
@SP
M=M+1

// push constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
A=M
A=A-1
D=M
A=A-1
M=D+M
@SP
M=M-1

// pop pointer 1           
@SP
AM=M-1
D=M
@THAT
M=D

// push argument 0
@0
D=A
@ARG
A=M+D
D=M
@SP
A=M
M=D
@SP
M=M+1

// push constant 1
@1
D=A
@SP
A=M
M=D
@SP
M=M+1

// sub
@SP
A=M
A=A-1
D=M
A=A-1
M=M-D
@SP
M=M-1

// pop argument 0          
@0
D=A
@ARG
A=M
D=A+D
@SP
AM=M-1
A=M
D=A+D
A=D-A
D=D-A
M=D

// goto MAIN_LOOP_START
@MAIN_LOOP_START
0;JEQ

(END_PROGRAM)
