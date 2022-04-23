lexer grammar VyxalLexer;

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
    : [¨Þø∆]
    ;

CONSTANT_PREFIX
    : 'k'
    ;

CONTEXT_VAR
    : 'n'
    ;

MODIFIER
    : [ßvƒɖ⁺₌₍~&]
    ;

COMMENT
    : '#' (~'\n' .)* -> skip
    ;

DIGIT
    : [0-9]
    ;

ALPHA
    : [A-Za-z]
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

NORMAL_STRING
    : '"' .*? '"'
    ;

COMPRESSED_STRING
    : '«' .*? '«'
    ;

SINGLE_CHAR_STRING
    : '\\' .
    ;

DOUBLE_CHAR_STRING
    : '‛' . .
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

COMPRESSED_NUMBER
    : '»'
    ;

COMPLEX_SEPARATOR
    : '°'
    ;

ONE_ELEMENT_LAMBDA
    : '⁽'
    ;

TWO_ELEMENT_LAMBDA
    : '‡'
    ;

THREE_ELEMENT_LAMBDA
    : '≬'
    ;

LITERALLY_ANY_TEXT
    : [\u0010-\uFFFF]
    ;
