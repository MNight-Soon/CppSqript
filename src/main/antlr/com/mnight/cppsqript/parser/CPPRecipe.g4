grammar CPPRecipe;

// --- PARSER RULES ---
script: statement* EOF ;

statement
    : removeStatement ';'
    | shapelessStatement ';'
    | shapedStatement ';'
    | comment
    ;

removeStatement
    : 'remove' '(' STRING ')'
    ;

shapelessStatement
    : 'shapeless' '(' STRING ',' INT ',' stringList ')'
    ;

shapedStatement
    : 'shaped' '(' STRING ',' INT ',' stringList ',' charMap ')'
    ;

stringList
    : '{' STRING (',' STRING)* '}'
    ;

charMap
    : '{' charPair (',' charPair)* '}'
    ;

charPair
    : '{' CHAR ',' STRING '}'
    ;

comment
    : COMMENT
    ;

// --- LEXER RULES ---
CHAR : '\'' ~['\r\n] '\'';
STRING : '"' ('\\"' | ~["\r\n])* '"' ;
INT : [0-9]+ ;
COMMENT : '//' ~[\r\n]* ;
WS : [ \t\r\n]+ -> skip ;