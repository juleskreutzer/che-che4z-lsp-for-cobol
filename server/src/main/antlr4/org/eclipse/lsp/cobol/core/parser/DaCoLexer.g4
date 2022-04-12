/*
 * Copyright (c) 2022 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */

lexer grammar DaCoLexer;

channels{COMMENTS}
import TechnicalLexer;

ADD : A D D;
ADDRESS : A D D R E S S;
AFTER : A F T E R;
ALL : A L L;
ANA : A N A;
ANALIST : A N A L I S T;
ANY : A N Y;
ASC : A S C;
AUTO : A U T O;
AVG : A V G;
BUFFER : B U F F E R;
CR : C R;
D_B : D MINUSCHAR B;
D_C : D MINUSCHAR C;
DATE : D A T E;
DAY : D A Y;
DAY_OF_WEEK : D A Y MINUSCHAR O F MINUSCHAR W E E K;
DB : D B;
DEBUG_CONTENTS : D E B U G MINUSCHAR C O N T E N T S;
DEBUG_ITEM : D E B U G MINUSCHAR I T E M;
DEBUG_LINE : D E B U G MINUSCHAR L I N E;
DEBUG_NAME : D E B U G MINUSCHAR N A M E;
DEBUG_SUB_1 : D E B U G MINUSCHAR S U B MINUSCHAR '1';
DEBUG_SUB_2 : D E B U G MINUSCHAR S U B MINUSCHAR '2';
DEBUG_SUB_3 : D E B U G MINUSCHAR S U B MINUSCHAR '3';
DELETE : D E L E T E;
DES : D E S;
DESCRIPTION : D E S C R I P T I O N;
DESIGNER : D E S I G N E R;
DOM : D O M;
DUPLICATE : D U P L I C A T E;
END : E N D;
ENDRPT : E N D R P T;
ENTITY : E N T I T Y;
ERROR : E R R O R;
FALSE : F A L S E;
FILE : F I L E;
FOR : F O R;
FROM : F R O M;
FUNCTION : F U N C T I O N;
GET : G E T;
GRS : G R S;
HIGH_VALUE : H I G H MINUSCHAR V A L U E;
HIGH_VALUES : H I G H MINUSCHAR V A L U E S;
IN : I N;
INFO : I N F O;
INSERT : I N S E R T;
INTEGER : I N T E G E R;
INTO : I N T O;
INVERT : I N V E R T;
IS : I S;
ITEM : I T E M;
JNIENVPTR : J N I E N V P T R;
JOB : J O B;
LENGTH : L E N G T H;
LINAGE_COUNTER : L I N A G E MINUSCHAR C O U N T E R;
LINES : L I N E S;
LINE_COUNTER : L I N E MINUSCHAR C O U N T E R;
LOW_VALUE : L O W MINUSCHAR V A L U E;
LOW_VALUES : L O W MINUSCHAR V A L U E S;
MATCH : M A T C H;
NETWORK : N E T W O R K;
NEXT : N E X T;
NULL : N U L L;
NULLS : N U L L S;
MESSAGE : M E S S A G E;
MODIFY : M O D I F Y;
ODETTE : O D E T T E;
OF : O F;
ON : O N;
OPEN : O P E N;
OWN : O W N;
OWNER : O W N E R;
PACKET : P A C K E T;
PAGE : P A G E;
PAGE_COUNTER : P A G E MINUSCHAR C O U N T E R;
PRIOR : P R I O R;
QUOTE : Q U O T E;
QUOTES : Q U O T E S;
RANDOM: R A N D O M;
READ : R E A D;
REPORT : R E P O R T;
RESTORE : R E S T O R E;
RESULT : R E S U L T;
RETURN : R E T U R N;
RETURN_CODE : R E T U R N MINUSCHAR C O D E;
ROW : R O W;
SEQ : S E Q;
SHIFT_IN : S H I F T MINUSCHAR I N;
SHIFT_OUT : S H I F T MINUSCHAR O U T;
SHOW : S H O W;
SINGLE : S I N G L E;
SORT : S O R T;
SORT_CONTROL : S O R T MINUSCHAR C O N T R O L;
SORT_CORE_SIZE : S O R T MINUSCHAR C O R E MINUSCHAR S I Z E;
SORT_FILE_SIZE : S O R T MINUSCHAR F I L E MINUSCHAR S I Z E;
SORT_MESSAGE : S O R T MINUSCHAR M E S S A G E;
SORT_MODE_SIZE : S O R T MINUSCHAR M O D E MINUSCHAR S I Z E;
SORT_RETURN : S O R T MINUSCHAR R E T U R N;
SPACE : S P A C E;
SPACES : S P A C E S;
START : S T A R T;
SAVE : S A V E;
STD : S T D;
STRING : S T R I N G;
SUM : S U M;
TABLE : T A B L E;
TALLY : T A L L Y;
TASK : T A S K;
TIME : T I M E;
TO : T O;
TRANSACTION : T R A N S A C T I O N;
TRUE : T R U E;
USER : U S E R;
USING : U S I N G;
VERSION : V E R S I O N;
VOLSER : V O L S E R;
WARNING : W A R N I N G;
WHEN_COMPILED : W H E N MINUSCHAR C O M P I L E D;
WITH : W I T H;
WRITE : W R I T E;
YES : Y E S;
ZERO : Z E R O;
ZEROES : Z E R O E S;
ZEROS : Z E R O S;


mode PICTURECLAUSE;
FINALCHARSTRING: CHARSTRING+ ->popMode;
CHARSTRING: PICTURECHARSGROUP1+ PICTURECHARSGROUP2? LParIntegralRPar? '.'? (PICTURECHARSGROUP1|PICTURECHARSGROUP2)
			PICTURECHARSGROUP1+ PICTURECHARSGROUP2? LParIntegralRPar?|
			PICTURECHARSGROUP1* '.' PICTUREPeriodAcceptables+ LParIntegralRPar?|
			PICTURECHARSGROUP1* PICTURECHARSGROUP2? PICTURECHARSGROUP1+ LParIntegralRPar? '.'? (PICTURECHARSGROUP1|PICTURECHARSGROUP2)|
			PICTURECHARSGROUP1* PICTURECHARSGROUP2? PICTURECHARSGROUP1+ LParIntegralRPar?|
			PICTURECHARSGROUP2 PICTURECHARSGROUP1* LParIntegralRPar? '.'? (PICTURECHARSGROUP1|PICTURECHARSGROUP2)|
			PICTURECHARSGROUP2 PICTURECHARSGROUP1* LParIntegralRPar?
;

PICTURECHARSGROUP1: PICTURECharAcceptedMultipleTime+;
PICTURECHARSGROUP2: PICTURECharAcceptedOneTime+;
WS2 : [ \t\f]+ -> channel(HIDDEN);
LParIntegralRPar: LPARENCHAR INTEGERLITERAL RPARENCHAR;
fragment PICTUREPeriodAcceptables: ('0'|'9'|B|Z|CR|DB|ASTERISKCHAR|COMMACHAR|MINUSCHAR|PLUSCHAR|SLASHCHAR);
fragment PICTURECharAcceptedMultipleTime: (A|G|N|P|X|DOLLARCHAR|PICTUREPeriodAcceptables);
fragment PICTURECharAcceptedOneTime: (V|E|S|CR|DB);
