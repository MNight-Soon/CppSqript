grammar CPPRecipe;

// --- PARSER RULES ---
script : topLevelElement* EOF ;

topLevelElement
    : functionDef
    | statement
    ;

// รองรับโครงสร้างฟังก์ชัน เช่น void register_recipes(ServerEvents::Recipes& event) { ... }
functionDef
    : 'void' ID '(' paramList? ')' '{' statement* '}'
    ;

paramList
    : param (',' param)*
    ;

param
    : typeName '&'? ID
    ;

typeName
    : ('const')? ID ('::' ID)* ('*')?
    ;

statement
    : varDecl ';'
    | removeStatement ';'
    | shapelessStatement ';'
    | shapedStatement ';'
    ;

// รองรับการประกาศตัวแปร เช่น auto sword = "minecraft:diamond_sword"; หรือ int amt = 4;
varDecl
    : typeName ID '=' expr ';' ?
    ;

// รองรับการเรียกแบบ event.remove(...) หรือ remove(...)
removeStatement
    : (ID '.')? 'remove' '(' expr ')'
    ;

shapelessStatement
    : (ID '.')? 'shapeless' '(' expr ',' expr ',' exprList ')'
    ;

shapedStatement
    : (ID '.')? 'shaped' '(' expr ',' expr ',' exprList ',' charMap ')'
    ;

exprList
    : '{' expr (',' expr)* '}'
    ;

charMap
    : '{' charPair (',' charPair)* '}'
    ;

charPair
    : '{' CHAR ',' expr '}'
    ;

// Expression สามารถเป็นค่าตรงๆ หรือชื่อตัวแปร (ID) ก็ได้
expr
    : STRING
    | INT
    | ID
    ;

// --- LEXER RULES ---
PREPROCESSOR : '#' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

CHAR : '\'' ~['\r\n] '\'' ;
STRING : '"' ( '\\"' | ~["\r\n] )* '"' ;
INT : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
WS : [ \t\r\n]+ -> skip ;