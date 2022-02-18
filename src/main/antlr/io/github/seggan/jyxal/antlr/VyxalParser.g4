parser grammar VyxalParser;

@header {
package io.github.seggan.jyxal.antlr;
}

options {
    tokenVocab=VyxalLexer;
}

file
    : program EOF?
    ;

program
    : (literal | statement | element | WHITESPACE)+
    ;

literal
    : normal_string
    | compressed_string
    | single_char_string
    | double_char_string
    | number
    | compressed_number
    | complex_number
    | list
    ;

number
    : integer (PERIOD integer)?
    ;

integer
    : DIGIT+
    ;

compressed_number
    : COMPRESSED_NUMBER any COMPRESSED_NUMBER
    ;

complex_number
    : number COMPLEX_SEPARATOR number
    ;

list
    : LIST_OPEN program (PIPE program)* LIST_CLOSE?
    ;

normal_string
    : BACKTICK any BACKTICK
    ;

compressed_string
    : COMPRESSED_STRING any COMPRESSED_STRING
    ;

single_char_string
    : SINGLE_CHAR_STRING .
    ;

double_char_string
    : DOUBLE_CHAR_STRING . .
    ;

any
    : .+?
    ;

statement
    : if_statement
    | for_loop
    | while_loop
    | lambda
    | function
    | variable_assn
    ;

if_statement
    : OPENING_BRACKET program (PIPE program)? CLOSING_BRACKET?
    ;

for_loop
    : OPENING_PARENTHESIS (variable PIPE)? program CLOSING_PARENTHESIS?
    ;

while_loop
    : OPENING_BRACE (program PIPE)? program CLOSING_BRACE?
    ;

lambda
    : LAMBDA_TYPE (integer PIPE)? program SEMICOLON?
    ;

function
    : AT_SIGN variable ((COLON parameter (COLON parameter)*)? PIPE program)? SEMICOLON?
    ;

parameter
    : STAR | variable | integer
    ;

variable_assn
    : ASSN_SIGN variable
    ;

variable
    : (ALPHA | DIGIT)+
    ;

element
    : MODIFIER? PREFIX? element_type
    ;

element_type
    : ALPHA | NON_ALPHA_ELEMENT
    ;

