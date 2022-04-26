// Grammar for transpiled SymJy expressions

grammar SymJy;

file
    : expressions EOF
    ;

expressions
    : expression+
    ;

expression
    : multiplyingExpression (ADD_OPERATOR multiplyingExpression)*
    ;

multiplyingExpression
    : powExpression (MUL_OPERATOR? powExpression)*
    ;

powExpression
    : atom ('^' expression)?
    ;


functionCall
    : FUNCTION_NAME multiplyingExpression
    ;

atom
    : variable
    | number
    | functionCall
    | '(' expressions ')'
    | '@' expression expression
    | '$' expression expression expression
    | '#' expression expression expression expression
    ;

number
    : INTEGER
    | FLOAT
    ;

variable
    : VARIABLE
    ;

VARIABLE
    : 'x'
    | 'y'
    | 'z'
    | '?'
    ;

INTEGER
    : '0'..'9'+
    ;

FLOAT
    : INTEGER '.' INTEGER
    ;

FUNCTION_NAME
    : 's' // sin
    | 'as' // asin
    | 'c' // cos
    | 'ac' // acos
    | 't' // tan
    | 'at' // atan
    | '|' // abs
    | 'l' // log10
    | 'L' // ln
    | 'S' // square
    | 'C' // cube
    | 'h' // halve
    | 'D' // double
    | 'R' // square root
    | '\\' // 1 / x
    | '!' // factorial
    ;

MUL_OPERATOR
    : '*'
    | '/'
    ;

ADD_OPERATOR
    : '+'
    | '-'
    ;

WS : (' '|'\n') -> skip ;
