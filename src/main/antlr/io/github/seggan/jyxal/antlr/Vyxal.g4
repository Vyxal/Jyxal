grammar Vyxal;

@header {
package io.github.seggan.jyxal.antlr;
}

file
    : program EOF
    ;

program
    : (element | literal | structure)+
    ;

element
    : PREFIX? element_type
    ;

element_type
    : ALPHA | '<' | ':' | '×' | 'Ṁ' | 'ṫ' | '₇' | '¾' | '₄' | '↵' | '¹' | 'Π' | 'æ' | 'ṡ' | '∑' | 'Ẏ'
    | '√' | 'ḋ' | '§' | '²' | '…' | 'ṅ' | 'Ż' | 'Ǎ' | '-' | '∵' | '↔' | '≠' | 'ɾ' | '¤' | '₴' | 'Ǐ'
    | '⇧' | '\\' | 'ġ' | 'ẏ' | '⁼' | '⁋' | '∩' | '≈' | '∷' | '₈' | '÷' | 'ȧ' | 'ʀ' | '₀' | 'Ḃ' | '⊍'
    | '∨' | 'ȯ' | '⁰' | 'Ẋ' | '⇩' | 'ẇ' | '‹' | 'ḭ' | '†' | '‟' | '⌈' | '₁' | '!' | '€' | 'ƈ' | 'ǒ'
    | 'ɽ' | 'ʁ' | ',' | 'Ȯ' | '⋎' | 'τ' | 'ǎ' | 'ṙ' | '%' | 'Ẇ' | '∧' | '↲' | 'ǐ' | '¢' | '„' | 'Ė'
    | '₂' | 'Ḟ' | '꘍' | '}' | '*' | 'ẋ' | '?' | '₅' | 'ŀ' | 'ß' | '⟇' | '℅' | '¥'| '₆' | 'Ġ' | 'ṗ'
    | '∞' | 'Ṗ' | 'ꜝ' | 'Ǔ' | '›' | 'ε' | '□' | 'Ṫ' | '¦' | 'ė' | '$' | 'Ṙ' | 'İ' | '=' | '↓' | 'ċ'
    | '₃' | 'Ḣ' | '_' | '⟑' | 'Ċ' | 'Ŀ' | '¬' | '¶' | 'ð' | 'ḟ' | '¡' | '¯' | '≥' | 'ǔ' | 'ż' | '↑'
    | 'Ḋ' | '¼' | '⋏' | 'Ǒ' | '>' | 'ṁ' | '£' | '⅛' | 'ḣ' | '+' | '±' | '/' | '↳' | '∪' | '∇' | '≤'
    | 'ḃ' | '⌐' | '^' | 'Ṡ' | 'Ȧ' | 'β' | '•' | '½' | 'Ṅ' | '∴'
    ;

// structures
structure
    : if_statement
    | for_loop
    | while_loop
    | lambda
    | function
    | variable_assn
    ;

if_statement
    : '[' program ('|' program)? ']'?
    ;

for_loop
    : '(' (variable '|')? program ')'?
    ;

while_loop
    : '{' (program '|')? program '}'?
    ;

lambda
    : LAMBDA_TYPE (integer '|')? program ';'?
    ;

function
    : '@' variable (':' (parameter (':' parameter)* '|')? program)? ';'?
    ;

variable_assn
    : ASSN_SIGN variable
    ;

variable
    : (ALPHA | DIGIT)+
    ;

parameter
    : '*' | variable | integer
    ;


// types
literal
    : number
    | string
    | list
    ;

string
    : STRING
    | COMPRESSED_STRING
    | SINGLE_CHAR_STRING
    | DOUBLE_CHAR_STRING
    ;

number
    : integer
    | decimal
    | complex
    | COMPRESSED_NUMBER
    ;

integer
    : DIGIT+
    ;

decimal
    : integer '.' integer
    ;

complex
    : (integer | decimal) '°' (integer | decimal)
    ;

list
    : '⟨' program ('|' program)* '⟩'?
    ;

// literals
COMPRESSED_STRING
    : '«' .+? '«'?
    ;

STRING
    : '`' .+? '`'?
    ;

SINGLE_CHAR_STRING
    : '\\'.
    ;

DOUBLE_CHAR_STRING
    : '‛'. .
    ;

DIGIT
    : [0-9]
    ;

COMPRESSED_NUMBER
    : '»' .+? '»'?
    ;


// code
PREFIX
    : [¨Þkø∆]
    ;

ALPHA
    : [a-zA-Z]
    ;

ASSN_SIGN
    : '→' | '←'
    ;

// strucutres
LAMBDA_TYPE
    : [λƛ'µ]
    ;

WHT
    : [ \t\n\r] -> skip
    ;