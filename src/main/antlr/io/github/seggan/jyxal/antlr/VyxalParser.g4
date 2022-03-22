parser grammar VyxalParser;

options {
    tokenVocab=VyxalLexer;
}

file
    : alias* program EOF
    ;

alias
    : PREFIX element_type PIPE element_type WHITESPACE*
    ;

program
    : (program_node | WHITESPACE)+
    ;

program_node
    : statement | literal | element
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
    | constant
    ;

normal_string
    : BACKTICK .*? BACKTICK
    ;

compressed_string
    : COMPRESSED_STRING .*? COMPRESSED_STRING
    ;

single_char_string
    : SINGLE_CHAR_STRING .
    ;

double_char_string
    : DOUBLE_CHAR_STRING . .
    ;

number
    : integer (PERIOD integer)?
    ;

integer
    : DIGIT+
    ;

compressed_number
    : COMPRESSED_NUMBER .*? COMPRESSED_NUMBER
    ;

complex_number
    : number COMPLEX_SEPARATOR number
    ;

list
    : LIST_OPEN program (PIPE program)* LIST_CLOSE?
    ;

constant
    : CONSTANT_PREFIX (element_type | DIGIT)
    ;

statement
    : if_statement
    | fori_loop
    | for_loop
    | while_loop
    | lambda
    | one_element_lambda
    | two_element_lambda
    | three_element_lambda
    | function
    | variable_assn
    ;

if_statement
    : IF_OPEN program (PIPE program)? IF_CLOSE?
    ;

fori_loop
    : DIGIT DIGIT? DIGIT? DIGIT? DIGIT? DIGIT? DIGIT? DIGIT? DIGIT? FOR_OPEN program FOR_CLOSE?
    ;

for_loop
    : FOR_OPEN (variable PIPE)? program FOR_CLOSE?
    ;

while_loop
    : WHILE_OPEN (program PIPE)? program WHILE_CLOSE?
    ;

lambda
    : LAMBDA_TYPE (integer PIPE)? program SEMICOLON?
    ;

one_element_lambda
    : ONE_ELEMENT_LAMBDA program_node
    ;

two_element_lambda
    : TWO_ELEMENT_LAMBDA program_node program_node
    ;

three_element_lambda
    : THREE_ELEMENT_LAMBDA program_node program_node program_node
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
    : (ALPHA | DIGIT | CONSTANT_PREFIX | CONTEXT_VAR)+
    ;

element
    : MODIFIER? PREFIX? element_type
    ;

element_type
    : ALPHA | NON_ALPHA_ELEMENT | CONTEXT_VAR
    ;

