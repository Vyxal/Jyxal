lexer grammar VyxalLexer;

@header {
package io.github.seggan.jyxal.antlr;
}

ALPHA
    : [A-Za-z]
    ;

NON_ALPHA_ELEMENT
    : '<' | ':' | '×' | 'Ṁ' | 'ṫ' | '₇' | '¾' | '₄' | '↵' | '¹' | 'Π' | 'æ' | 'ṡ' | '∑' | 'Ẏ' | 'Ṅ'
    | '√' | 'ḋ' | '§' | '²' | '…' | 'ṅ' | 'Ż' | 'Ǎ' | '-' | '∵' | '↔' | '≠' | 'ɾ' | '¤' | '₴' | 'Ǐ'
    | '⇧' | 'ġ' | 'ẏ' | '⁼' | '⁋' | '∩' | '≈' | '∷' | '₈' | '÷' | 'ȧ' | 'ʀ' | '₀' | 'Ḃ' | '⊍' | '∴'
    | '∨' | 'ȯ' | '⁰' | 'Ẋ' | '⇩' | 'ẇ' | '‹' | 'ḭ' | '†' | '‟' | '⌈' | '₁' | '!' | '€' | 'ƈ' | 'ǒ'
    | 'ɽ' | 'ʁ' | ',' | 'Ȯ' | '⋎' | 'τ' | 'ǎ' | 'ṙ' | '%' | 'Ẇ' | '∧' | '↲' | 'ǐ' | '¢' | '„' | 'Ė'
    | '₂' | 'Ḟ' | '꘍' | '*' | 'ẋ' | '?' | '₅' | 'ŀ' | '⟇' | '℅' | '¥'| '₆' | 'Ġ' | 'ṗ' | '•' | '½'
    | '∞' | 'Ṗ' | 'ꜝ' | 'Ǔ' | '›' | 'ε' | '□' | 'Ṫ' | '¦' | 'ė' | '$' | 'Ṙ' | 'İ' | '=' | '↓' | 'ċ'
    | '₃' | 'Ḣ' | '_' | '⟑' | 'Ċ' | 'Ŀ' | '¬' | '¶' | 'ð' | 'ḟ' | '¡' | '¯' | '≥' | 'ǔ' | 'ż' | '↑'
    | 'Ḋ' | '¼' | '⋏' | 'Ǒ' | '>' | 'ṁ' | '£' | '⅛' | 'ḣ' | '+' | '±' | '/' | '↳' | '∪' | '∇' | '≤'
    | 'ḃ' | '⌐' | '^' | 'Ṡ' | 'Ȧ' | 'β'
    ;

PREFIX
    : [¨Þkø∆]
    ;

MODIFIER
    : [ßvƒɖ⁽‡≬⁺₌₍~]
    ;

DIGIT
    : [0-9]
    ;

WHITESPACE
    : [ \t\r\n]
    ;

ASSN_SIGN
    : '→' | '←'
    ;

LAMBDA_TYPE
    : [λƛ'µ]
    ;

// syntax elements
PIPE
    : '|'
    ;

WHILE_OPEN
    : '{'
    ;

WHILE_CLOSE
    : '}'
    ;

IF_OPEN
    : '['
    ;

IF_CLOSE
    : ']'
    ;

FOR_OPEN
    : '('
    ;

FOR_CLOSE
    : ')'
    ;

LIST_OPEN
    : '⟨'
    ;

LIST_CLOSE
    : '⟩'
    ;

BACKTICK
    : '`'
    ;

PERIOD
    : '.'
    ;

SEMICOLON
    : ';'
    ;

AT_SIGN
    : '@'
    ;

STAR
    : '*'
    ;

COLON
    : ':'
    ;

COMPRESSED_STRING
    : '«'
    ;

COMPRESSED_NUMBER
    : '»'
    ;

COMPLEX_SEPARATOR
    : '°'
    ;

SINGLE_CHAR_STRING
    : '\\'
    ;

DOUBLE_CHAR_STRING
    : '‛'
    ;

LITERALLY_ANY_TEXT
    : [\u0010-\uFFFF]
    ;
