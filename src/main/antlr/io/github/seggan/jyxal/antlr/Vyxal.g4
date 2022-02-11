grammar Vyxal;

@header {
package io.github.seggan.jyxal.antlr;
}

file
    : program EOF
    ;

program
    : (literal | structure | element)+?
    ;

element
    : MODIFIER? PREFIX? element_type
    ;

element_type
    : ALPHA | '<' | ':' | '×' | 'Ṁ' | 'ṫ' | '₇' | '¾' | '₄' | '↵' | '¹' | 'Π' | 'æ' | 'ṡ' | '∑' | 'Ẏ'
    | '√' | 'ḋ' | '§' | '²' | '…' | 'ṅ' | 'Ż' | 'Ǎ' | '-' | '∵' | '↔' | '≠' | 'ɾ' | '¤' | '₴' | 'Ǐ'
    | '⇧' | 'ġ' | 'ẏ' | '⁼' | '⁋' | '∩' | '≈' | '∷' | '₈' | '÷' | 'ȧ' | 'ʀ' | '₀' | 'Ḃ' | '⊍' | '∴'
    | '∨' | 'ȯ' | '⁰' | 'Ẋ' | '⇩' | 'ẇ' | '‹' | 'ḭ' | '†' | '‟' | '⌈' | '₁' | '!' | '€' | 'ƈ' | 'ǒ'
    | 'ɽ' | 'ʁ' | ',' | 'Ȯ' | '⋎' | 'τ' | 'ǎ' | 'ṙ' | '%' | 'Ẇ' | '∧' | '↲' | 'ǐ' | '¢' | '„' | 'Ė'
    | '₂' | 'Ḟ' | '꘍' | '*' | 'ẋ' | '?' | '₅' | 'ŀ' | '⟇' | '℅' | '¥'| '₆' | 'Ġ' | 'ṗ'
    | '∞' | 'Ṗ' | 'ꜝ' | 'Ǔ' | '›' | 'ε' | '□' | 'Ṫ' | '¦' | 'ė' | '$' | 'Ṙ' | 'İ' | '=' | '↓' | 'ċ'
    | '₃' | 'Ḣ' | '_' | '⟑' | 'Ċ' | 'Ŀ' | '¬' | '¶' | 'ð' | 'ḟ' | '¡' | '¯' | '≥' | 'ǔ' | 'ż' | '↑'
    | 'Ḋ' | '¼' | '⋏' | 'Ǒ' | '>' | 'ṁ' | '£' | '⅛' | 'ḣ' | '+' | '±' | '/' | '↳' | '∪' | '∇' | '≤'
    | 'ḃ' | '⌐' | '^' | 'Ṡ' | 'Ȧ' | 'β' | '•' | '½' | 'Ṅ'
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
    : LAMBDA_TYPE (INTEGER '|')? program ';'?
    ;

function
    : '@' variable ((':' parameter (':' parameter)*)? '|' program)? ';'?
    ;

variable_assn
    : ASSN_SIGN variable
    ;

variable
    : (ALPHA)+
    ;

parameter
    : '*' | variable | INTEGER
    ;


// types
literal
    : number
    | string
    | list
    ;

string
    : normal_string
    | compressed_string
    | single_char_string
    | double_char_string
    ;

number
    : integer
    | complex
    | compressed_number
    ;

integer
    : ('+' | '-')? INTEGER ('.' INTEGER)?
    ;

complex
    : INTEGER '°' INTEGER
    ;

list
    : '⟨' program ('|' program)* '⟩'?
    ;

any_text
    : .+?
    ;

compressed_string
    : '\u00ab' any_text '\u00ab'?
    ;

normal_string
    : '`' any_text '`'?
    ;

single_char_string
    : '\\' .
    ;

double_char_string
    : '‛' . .
    ;

compressed_number
    : '\u00bb' any_text '\u00bb'?
    ;

fragment DIGIT
    : [0-9]
    ;

INTEGER
    : DIGIT+
    ;


// code
PREFIX
    : [¨Þkø∆]
    ;

MODIFIER
    : [ßvƒɖ⁽‡≬⁺₌₍~]
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