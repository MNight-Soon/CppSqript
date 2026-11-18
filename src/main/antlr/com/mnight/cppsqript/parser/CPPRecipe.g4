grammar CPPRecipe;

// --- PARSER RULES ---
script : topLevelElement* EOF ;

topLevelElement
    : functionDef
    | statement
    ;

functionDef
    : 'void' ID '(' paramList? ')' '{' statement* '}'
    ;

paramList : param (',' param)* ;
param : typeName '&'? ID ;
typeName : ('const')? ID ('::' ID)* ('*')? ;

statement
    : varDecl ';'
    | methodCall ';'
    ;

varDecl
    : typeName ID '=' expr
    ;

// รองรับการเรียกคำสั่งทุกชื่อ เช่น event.remove(...), event.crushing(...), custom_cmd(...)
methodCall
    : (ID '.')? ID '(' argList? ')'
    ;

argList
    : expr (',' expr)*
    ;

// รองรับค่าหลากหลายรูปแบบเพื่อให้ Addon ดึงไปใช้ได้อิสระ
expr
    : RAW_STRING                            # RawStringExpr
    | STRING                                # StringExpr
    | INT                                   # IntExpr
    | FLOAT                                 # FloatExpr
    | CHAR                                  # CharExpr
    | ID                                    # VarExpr
    | '{' CHAR ',' expr '}'                 # PairExpr
    | '{' (expr (',' expr)*)? '}'           # ArrayExpr
    ;

// --- LEXER RULES ---
PREPROCESSOR : '#' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

RAW_STRING : 'R"(' .*? ')"' ;
CHAR : '\'' ~['\r\n] '\'' ;
STRING : '"' ( '\\"' | ~["\r\n] )* '"' ;
FLOAT : [0-9]+ '.' [0-9]+ 'f'? ;
INT : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
WS : [ \t\r\n]+ -> skip ;