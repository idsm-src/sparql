// Generated from SparqlParser.g4 by ANTLR 4.5.1
 package cz.iocb.chemweb.server.sparql.grammar; 
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SparqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, BASE=2, PREFIX=3, SELECT=4, DISTINCT=5, REDUCED=6, CONSTRUCT=7, 
		DESCRIBE=8, ASK=9, FROM=10, NAMED=11, WHERE=12, ORDER=13, BY=14, ASC=15, 
		DESC=16, LIMIT=17, OFFSET=18, VALUES=19, OPTIONAL=20, GRAPH=21, UNION=22, 
		FILTER=23, A=24, STR=25, LANG=26, LANGMATCHES=27, DATATYPE=28, BOUND=29, 
		SAMETERM=30, ISIRI=31, ISURI=32, ISBLANK=33, ISLITERAL=34, REGEX=35, SUBSTR=36, 
		TRUE=37, FALSE=38, LOAD=39, CLEAR=40, DROP=41, ADD=42, MOVE=43, COPY=44, 
		CREATE=45, DELETE=46, INSERT=47, USING=48, SILENT=49, DEFAULT=50, ALL=51, 
		DATA=52, WITH=53, INTO=54, TO=55, AS=56, GROUP=57, HAVING=58, UNDEF=59, 
		BINDINGS=60, SERVICE=61, BIND=62, MINUS=63, IRI=64, URI=65, BNODE=66, 
		RAND=67, ABS=68, CEIL=69, FLOOR=70, ROUND=71, CONCAT=72, STRLEN=73, UCASE=74, 
		LCASE=75, ENCODE_FOR_URI=76, CONTAINS=77, STRSTARTS=78, STRENDS=79, STRBEFORE=80, 
		STRAFTER=81, REPLACE=82, YEAR=83, MONTH=84, DAY=85, HOURS=86, MINUTES=87, 
		SECONDS=88, TIMEZONE=89, TZ=90, NOW=91, UUID=92, STRUUID=93, MD5=94, SHA1=95, 
		SHA256=96, SHA384=97, SHA512=98, COALESCE=99, IF=100, STRLANG=101, STRDT=102, 
		ISNUMERIC=103, COUNT=104, SUM=105, MIN=106, MAX=107, AVG=108, SAMPLE=109, 
		GROUP_CONCAT=110, NOT=111, IN=112, EXISTS=113, SEPARATOR=114, IRIREF=115, 
		PNAME_NS=116, PNAME_LN=117, BLANK_NODE_LABEL=118, VAR1=119, VAR2=120, 
		LANGTAG=121, INTEGER=122, DECIMAL=123, DOUBLE=124, INTEGER_POSITIVE=125, 
		DECIMAL_POSITIVE=126, DOUBLE_POSITIVE=127, INTEGER_NEGATIVE=128, DECIMAL_NEGATIVE=129, 
		DOUBLE_NEGATIVE=130, STRING_LITERAL1=131, STRING_LITERAL2=132, STRING_LITERAL_LONG1=133, 
		STRING_LITERAL_LONG2=134, COMMENT=135, REFERENCE=136, LESS_EQUAL=137, 
		GREATER_EQUAL=138, NOT_EQUAL=139, AND=140, OR=141, INVERSE=142, OPEN_BRACE=143, 
		CLOSE_BRACE=144, OPEN_CURLY_BRACE=145, CLOSE_CURLY_BRACE=146, OPEN_SQUARE_BRACKET=147, 
		CLOSE_SQUARE_BRACKET=148, SEMICOLON=149, DOT=150, PLUS_SIGN=151, MINUS_SIGN=152, 
		ASTERISK=153, QUESTION_MARK=154, COMMA=155, NEGATION=156, DIVIDE=157, 
		EQUAL=158, LESS=159, GREATER=160, PIPE=161, INVALID=162, ANY=163;
	public static final int
		RULE_query = 0, RULE_prologue = 1, RULE_baseDecl = 2, RULE_prefixDecl = 3, 
		RULE_selectQuery = 4, RULE_subSelect = 5, RULE_selectClause = 6, RULE_selectVariable = 7, 
		RULE_constructQuery = 8, RULE_describeQuery = 9, RULE_askQuery = 10, RULE_datasetClause = 11, 
		RULE_whereClause = 12, RULE_solutionModifier = 13, RULE_groupClause = 14, 
		RULE_groupCondition = 15, RULE_havingClause = 16, RULE_havingCondition = 17, 
		RULE_orderClause = 18, RULE_orderCondition = 19, RULE_limitOffsetClauses = 20, 
		RULE_limitClause = 21, RULE_offsetClause = 22, RULE_valuesClause = 23, 
		RULE_updateCommand = 24, RULE_update = 25, RULE_load = 26, RULE_clear = 27, 
		RULE_drop = 28, RULE_create = 29, RULE_add = 30, RULE_move = 31, RULE_copy = 32, 
		RULE_insertData = 33, RULE_deleteData = 34, RULE_deleteWhere = 35, RULE_modify = 36, 
		RULE_deleteClause = 37, RULE_insertClause = 38, RULE_usingClause = 39, 
		RULE_graphOrDefault = 40, RULE_graphRef = 41, RULE_graphRefAll = 42, RULE_quadPattern = 43, 
		RULE_quadData = 44, RULE_quads = 45, RULE_quadsDetails = 46, RULE_quadsNotTriples = 47, 
		RULE_triplesTemplate = 48, RULE_groupGraphPattern = 49, RULE_groupGraphPatternSub = 50, 
		RULE_groupGraphPatternSubList = 51, RULE_triplesBlock = 52, RULE_graphPatternNotTriples = 53, 
		RULE_optionalGraphPattern = 54, RULE_graphGraphPattern = 55, RULE_serviceGraphPattern = 56, 
		RULE_bind = 57, RULE_inlineData = 58, RULE_dataBlock = 59, RULE_inlineDataOneVar = 60, 
		RULE_inlineDataFull = 61, RULE_dataBlockValues = 62, RULE_dataBlockValue = 63, 
		RULE_minusGraphPattern = 64, RULE_groupOrUnionGraphPattern = 65, RULE_filter = 66, 
		RULE_constraint = 67, RULE_functionCall = 68, RULE_argList = 69, RULE_expressionList = 70, 
		RULE_constructTemplate = 71, RULE_triplesSameSubject = 72, RULE_propertyList = 73, 
		RULE_propertyListNotEmpty = 74, RULE_verb = 75, RULE_objectList = 76, 
		RULE_object = 77, RULE_triplesSameSubjectPath = 78, RULE_propertyListPath = 79, 
		RULE_propertyListPathNotEmpty = 80, RULE_propertyListPathNotEmptyList = 81, 
		RULE_verbPath = 82, RULE_verbSimple = 83, RULE_objectListPath = 84, RULE_objectPath = 85, 
		RULE_path = 86, RULE_pathAlternative = 87, RULE_pathSequence = 88, RULE_pathElt = 89, 
		RULE_pathEltOrInverse = 90, RULE_pathMod = 91, RULE_pathPrimary = 92, 
		RULE_pathNegatedPropertySet = 93, RULE_pathOneInPropertySet = 94, RULE_integer = 95, 
		RULE_triplesNode = 96, RULE_blankNodePropertyList = 97, RULE_triplesNodePath = 98, 
		RULE_blankNodePropertyListPath = 99, RULE_collection = 100, RULE_collectionPath = 101, 
		RULE_graphNode = 102, RULE_graphNodePath = 103, RULE_varOrTerm = 104, 
		RULE_varOrIRI = 105, RULE_var = 106, RULE_graphTerm = 107, RULE_nil = 108, 
		RULE_expression = 109, RULE_unaryLiteralExpression = 110, RULE_unaryExpression = 111, 
		RULE_primaryExpression = 112, RULE_builtInCall = 113, RULE_regexExpression = 114, 
		RULE_subStringExpression = 115, RULE_strReplaceExpression = 116, RULE_existsFunction = 117, 
		RULE_notExistsFunction = 118, RULE_aggregate = 119, RULE_iriRefOrFunction = 120, 
		RULE_rdfLiteral = 121, RULE_numericLiteral = 122, RULE_numericLiteralUnsigned = 123, 
		RULE_numericLiteralPositive = 124, RULE_numericLiteralNegative = 125, 
		RULE_booleanLiteral = 126, RULE_string = 127, RULE_iri = 128, RULE_prefixedName = 129, 
		RULE_blankNode = 130, RULE_anon = 131;
	public static final String[] ruleNames = {
		"query", "prologue", "baseDecl", "prefixDecl", "selectQuery", "subSelect", 
		"selectClause", "selectVariable", "constructQuery", "describeQuery", "askQuery", 
		"datasetClause", "whereClause", "solutionModifier", "groupClause", "groupCondition", 
		"havingClause", "havingCondition", "orderClause", "orderCondition", "limitOffsetClauses", 
		"limitClause", "offsetClause", "valuesClause", "updateCommand", "update", 
		"load", "clear", "drop", "create", "add", "move", "copy", "insertData", 
		"deleteData", "deleteWhere", "modify", "deleteClause", "insertClause", 
		"usingClause", "graphOrDefault", "graphRef", "graphRefAll", "quadPattern", 
		"quadData", "quads", "quadsDetails", "quadsNotTriples", "triplesTemplate", 
		"groupGraphPattern", "groupGraphPatternSub", "groupGraphPatternSubList", 
		"triplesBlock", "graphPatternNotTriples", "optionalGraphPattern", "graphGraphPattern", 
		"serviceGraphPattern", "bind", "inlineData", "dataBlock", "inlineDataOneVar", 
		"inlineDataFull", "dataBlockValues", "dataBlockValue", "minusGraphPattern", 
		"groupOrUnionGraphPattern", "filter", "constraint", "functionCall", "argList", 
		"expressionList", "constructTemplate", "triplesSameSubject", "propertyList", 
		"propertyListNotEmpty", "verb", "objectList", "object", "triplesSameSubjectPath", 
		"propertyListPath", "propertyListPathNotEmpty", "propertyListPathNotEmptyList", 
		"verbPath", "verbSimple", "objectListPath", "objectPath", "path", "pathAlternative", 
		"pathSequence", "pathElt", "pathEltOrInverse", "pathMod", "pathPrimary", 
		"pathNegatedPropertySet", "pathOneInPropertySet", "integer", "triplesNode", 
		"blankNodePropertyList", "triplesNodePath", "blankNodePropertyListPath", 
		"collection", "collectionPath", "graphNode", "graphNodePath", "varOrTerm", 
		"varOrIRI", "var", "graphTerm", "nil", "expression", "unaryLiteralExpression", 
		"unaryExpression", "primaryExpression", "builtInCall", "regexExpression", 
		"subStringExpression", "strReplaceExpression", "existsFunction", "notExistsFunction", 
		"aggregate", "iriRefOrFunction", "rdfLiteral", "numericLiteral", "numericLiteralUnsigned", 
		"numericLiteralPositive", "numericLiteralNegative", "booleanLiteral", 
		"string", "iri", "prefixedName", "blankNode", "anon"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		"'a'", null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, "'^^'", "'<='", "'>='", "'!='", "'&&'", "'||'", 
		"'^'", "'('", "')'", "'{'", "'}'", "'['", "']'", "';'", "'.'", "'+'", 
		"'-'", "'*'", "'?'", "','", "'!'", "'/'", "'='", "'<'", "'>'", "'|'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "BASE", "PREFIX", "SELECT", "DISTINCT", "REDUCED", "CONSTRUCT", 
		"DESCRIBE", "ASK", "FROM", "NAMED", "WHERE", "ORDER", "BY", "ASC", "DESC", 
		"LIMIT", "OFFSET", "VALUES", "OPTIONAL", "GRAPH", "UNION", "FILTER", "A", 
		"STR", "LANG", "LANGMATCHES", "DATATYPE", "BOUND", "SAMETERM", "ISIRI", 
		"ISURI", "ISBLANK", "ISLITERAL", "REGEX", "SUBSTR", "TRUE", "FALSE", "LOAD", 
		"CLEAR", "DROP", "ADD", "MOVE", "COPY", "CREATE", "DELETE", "INSERT", 
		"USING", "SILENT", "DEFAULT", "ALL", "DATA", "WITH", "INTO", "TO", "AS", 
		"GROUP", "HAVING", "UNDEF", "BINDINGS", "SERVICE", "BIND", "MINUS", "IRI", 
		"URI", "BNODE", "RAND", "ABS", "CEIL", "FLOOR", "ROUND", "CONCAT", "STRLEN", 
		"UCASE", "LCASE", "ENCODE_FOR_URI", "CONTAINS", "STRSTARTS", "STRENDS", 
		"STRBEFORE", "STRAFTER", "REPLACE", "YEAR", "MONTH", "DAY", "HOURS", "MINUTES", 
		"SECONDS", "TIMEZONE", "TZ", "NOW", "UUID", "STRUUID", "MD5", "SHA1", 
		"SHA256", "SHA384", "SHA512", "COALESCE", "IF", "STRLANG", "STRDT", "ISNUMERIC", 
		"COUNT", "SUM", "MIN", "MAX", "AVG", "SAMPLE", "GROUP_CONCAT", "NOT", 
		"IN", "EXISTS", "SEPARATOR", "IRIREF", "PNAME_NS", "PNAME_LN", "BLANK_NODE_LABEL", 
		"VAR1", "VAR2", "LANGTAG", "INTEGER", "DECIMAL", "DOUBLE", "INTEGER_POSITIVE", 
		"DECIMAL_POSITIVE", "DOUBLE_POSITIVE", "INTEGER_NEGATIVE", "DECIMAL_NEGATIVE", 
		"DOUBLE_NEGATIVE", "STRING_LITERAL1", "STRING_LITERAL2", "STRING_LITERAL_LONG1", 
		"STRING_LITERAL_LONG2", "COMMENT", "REFERENCE", "LESS_EQUAL", "GREATER_EQUAL", 
		"NOT_EQUAL", "AND", "OR", "INVERSE", "OPEN_BRACE", "CLOSE_BRACE", "OPEN_CURLY_BRACE", 
		"CLOSE_CURLY_BRACE", "OPEN_SQUARE_BRACKET", "CLOSE_SQUARE_BRACKET", "SEMICOLON", 
		"DOT", "PLUS_SIGN", "MINUS_SIGN", "ASTERISK", "QUESTION_MARK", "COMMA", 
		"NEGATION", "DIVIDE", "EQUAL", "LESS", "GREATER", "PIPE", "INVALID", "ANY"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "SparqlParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SparqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class QueryContext extends ParserRuleContext {
		public PrologueContext prologue() {
			return getRuleContext(PrologueContext.class,0);
		}
		public ValuesClauseContext valuesClause() {
			return getRuleContext(ValuesClauseContext.class,0);
		}
		public TerminalNode EOF() { return getToken(SparqlParser.EOF, 0); }
		public SelectQueryContext selectQuery() {
			return getRuleContext(SelectQueryContext.class,0);
		}
		public ConstructQueryContext constructQuery() {
			return getRuleContext(ConstructQueryContext.class,0);
		}
		public DescribeQueryContext describeQuery() {
			return getRuleContext(DescribeQueryContext.class,0);
		}
		public AskQueryContext askQuery() {
			return getRuleContext(AskQueryContext.class,0);
		}
		public UpdateCommandContext updateCommand() {
			return getRuleContext(UpdateCommandContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_query);
		try {
			setState(278);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(264);
				prologue();
				setState(269);
				switch (_input.LA(1)) {
				case SELECT:
					{
					setState(265);
					selectQuery();
					}
					break;
				case CONSTRUCT:
					{
					setState(266);
					constructQuery();
					}
					break;
				case DESCRIBE:
					{
					setState(267);
					describeQuery();
					}
					break;
				case ASK:
					{
					setState(268);
					askQuery();
					}
					break;
				case EOF:
				case VALUES:
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(271);
				valuesClause();
				setState(272);
				match(EOF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(275);
				switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
				case 1:
					{
					setState(274);
					updateCommand();
					}
					break;
				}
				setState(277);
				match(EOF);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrologueContext extends ParserRuleContext {
		public List<BaseDeclContext> baseDecl() {
			return getRuleContexts(BaseDeclContext.class);
		}
		public BaseDeclContext baseDecl(int i) {
			return getRuleContext(BaseDeclContext.class,i);
		}
		public List<PrefixDeclContext> prefixDecl() {
			return getRuleContexts(PrefixDeclContext.class);
		}
		public PrefixDeclContext prefixDecl(int i) {
			return getRuleContext(PrefixDeclContext.class,i);
		}
		public PrologueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prologue; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrologue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrologueContext prologue() throws RecognitionException {
		PrologueContext _localctx = new PrologueContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_prologue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BASE || _la==PREFIX) {
				{
				setState(282);
				switch (_input.LA(1)) {
				case BASE:
					{
					setState(280);
					baseDecl();
					}
					break;
				case PREFIX:
					{
					setState(281);
					prefixDecl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(286);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BaseDeclContext extends ParserRuleContext {
		public TerminalNode BASE() { return getToken(SparqlParser.BASE, 0); }
		public TerminalNode IRIREF() { return getToken(SparqlParser.IRIREF, 0); }
		public BaseDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_baseDecl; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBaseDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BaseDeclContext baseDecl() throws RecognitionException {
		BaseDeclContext _localctx = new BaseDeclContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_baseDecl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(287);
			match(BASE);
			setState(288);
			match(IRIREF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrefixDeclContext extends ParserRuleContext {
		public TerminalNode PREFIX() { return getToken(SparqlParser.PREFIX, 0); }
		public TerminalNode PNAME_NS() { return getToken(SparqlParser.PNAME_NS, 0); }
		public TerminalNode IRIREF() { return getToken(SparqlParser.IRIREF, 0); }
		public PrefixDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prefixDecl; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrefixDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrefixDeclContext prefixDecl() throws RecognitionException {
		PrefixDeclContext _localctx = new PrefixDeclContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_prefixDecl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
			match(PREFIX);
			setState(291);
			match(PNAME_NS);
			setState(292);
			match(IRIREF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectQueryContext extends ParserRuleContext {
		public SelectClauseContext selectClause() {
			return getRuleContext(SelectClauseContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public SolutionModifierContext solutionModifier() {
			return getRuleContext(SolutionModifierContext.class,0);
		}
		public List<DatasetClauseContext> datasetClause() {
			return getRuleContexts(DatasetClauseContext.class);
		}
		public DatasetClauseContext datasetClause(int i) {
			return getRuleContext(DatasetClauseContext.class,i);
		}
		public SelectQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectQuery; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectQueryContext selectQuery() throws RecognitionException {
		SelectQueryContext _localctx = new SelectQueryContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_selectQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(294);
			selectClause();
			setState(298);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(295);
				datasetClause();
				}
				}
				setState(300);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(301);
			whereClause();
			setState(302);
			solutionModifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubSelectContext extends ParserRuleContext {
		public SelectClauseContext selectClause() {
			return getRuleContext(SelectClauseContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public SolutionModifierContext solutionModifier() {
			return getRuleContext(SolutionModifierContext.class,0);
		}
		public ValuesClauseContext valuesClause() {
			return getRuleContext(ValuesClauseContext.class,0);
		}
		public SubSelectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subSelect; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSubSelect(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubSelectContext subSelect() throws RecognitionException {
		SubSelectContext _localctx = new SubSelectContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_subSelect);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			selectClause();
			setState(305);
			whereClause();
			setState(306);
			solutionModifier();
			setState(307);
			valuesClause();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectClauseContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(SparqlParser.SELECT, 0); }
		public TerminalNode DISTINCT() { return getToken(SparqlParser.DISTINCT, 0); }
		public TerminalNode REDUCED() { return getToken(SparqlParser.REDUCED, 0); }
		public List<SelectVariableContext> selectVariable() {
			return getRuleContexts(SelectVariableContext.class);
		}
		public SelectVariableContext selectVariable(int i) {
			return getRuleContext(SelectVariableContext.class,i);
		}
		public SelectClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_selectClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(309);
			match(SELECT);
			setState(311);
			_la = _input.LA(1);
			if (_la==DISTINCT || _la==REDUCED) {
				{
				setState(310);
				_la = _input.LA(1);
				if ( !(_la==DISTINCT || _la==REDUCED) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(319);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
			case OPEN_BRACE:
				{
				setState(314); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(313);
					selectVariable();
					}
					}
					setState(316); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 119)) & ~0x3f) == 0 && ((1L << (_la - 119)) & ((1L << (VAR1 - 119)) | (1L << (VAR2 - 119)) | (1L << (OPEN_BRACE - 119)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(318);
				match(ASTERISK);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectVariableContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(SparqlParser.AS, 0); }
		public SelectVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectVariable; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectVariableContext selectVariable() throws RecognitionException {
		SelectVariableContext _localctx = new SelectVariableContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_selectVariable);
		try {
			setState(328);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(321);
				var();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(322);
				match(OPEN_BRACE);
				setState(323);
				expression(0);
				setState(324);
				match(AS);
				setState(325);
				var();
				setState(326);
				match(CLOSE_BRACE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstructQueryContext extends ParserRuleContext {
		public TerminalNode CONSTRUCT() { return getToken(SparqlParser.CONSTRUCT, 0); }
		public ConstructTemplateContext constructTemplate() {
			return getRuleContext(ConstructTemplateContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public SolutionModifierContext solutionModifier() {
			return getRuleContext(SolutionModifierContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(SparqlParser.WHERE, 0); }
		public List<DatasetClauseContext> datasetClause() {
			return getRuleContexts(DatasetClauseContext.class);
		}
		public DatasetClauseContext datasetClause(int i) {
			return getRuleContext(DatasetClauseContext.class,i);
		}
		public ConstructQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructQuery; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructQueryContext constructQuery() throws RecognitionException {
		ConstructQueryContext _localctx = new ConstructQueryContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_constructQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(330);
			match(CONSTRUCT);
			setState(351);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				{
				setState(331);
				constructTemplate();
				setState(335);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(332);
					datasetClause();
					}
					}
					setState(337);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(338);
				whereClause();
				setState(339);
				solutionModifier();
				}
				break;
			case FROM:
			case WHERE:
				{
				setState(344);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(341);
					datasetClause();
					}
					}
					setState(346);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(347);
				match(WHERE);
				setState(348);
				constructTemplate();
				setState(349);
				solutionModifier();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DescribeQueryContext extends ParserRuleContext {
		public TerminalNode DESCRIBE() { return getToken(SparqlParser.DESCRIBE, 0); }
		public SolutionModifierContext solutionModifier() {
			return getRuleContext(SolutionModifierContext.class,0);
		}
		public List<DatasetClauseContext> datasetClause() {
			return getRuleContexts(DatasetClauseContext.class);
		}
		public DatasetClauseContext datasetClause(int i) {
			return getRuleContext(DatasetClauseContext.class,i);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public List<VarOrIRIContext> varOrIRI() {
			return getRuleContexts(VarOrIRIContext.class);
		}
		public VarOrIRIContext varOrIRI(int i) {
			return getRuleContext(VarOrIRIContext.class,i);
		}
		public DescribeQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_describeQuery; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDescribeQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeQueryContext describeQuery() throws RecognitionException {
		DescribeQueryContext _localctx = new DescribeQueryContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_describeQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			match(DESCRIBE);
			setState(360);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				{
				setState(355); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(354);
					varOrIRI();
					}
					}
					setState(357); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(359);
				match(ASTERISK);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(365);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(362);
				datasetClause();
				}
				}
				setState(367);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(369);
			_la = _input.LA(1);
			if (_la==WHERE || _la==OPEN_CURLY_BRACE) {
				{
				setState(368);
				whereClause();
				}
			}

			setState(371);
			solutionModifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AskQueryContext extends ParserRuleContext {
		public TerminalNode ASK() { return getToken(SparqlParser.ASK, 0); }
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public List<DatasetClauseContext> datasetClause() {
			return getRuleContexts(DatasetClauseContext.class);
		}
		public DatasetClauseContext datasetClause(int i) {
			return getRuleContext(DatasetClauseContext.class,i);
		}
		public GroupClauseContext groupClause() {
			return getRuleContext(GroupClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public AskQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_askQuery; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAskQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AskQueryContext askQuery() throws RecognitionException {
		AskQueryContext _localctx = new AskQueryContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_askQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(373);
			match(ASK);
			setState(377);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(374);
				datasetClause();
				}
				}
				setState(379);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(380);
			whereClause();
			setState(382);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(381);
				groupClause();
				}
			}

			setState(385);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(384);
				havingClause();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DatasetClauseContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(SparqlParser.FROM, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode NAMED() { return getToken(SparqlParser.NAMED, 0); }
		public DatasetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_datasetClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDatasetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DatasetClauseContext datasetClause() throws RecognitionException {
		DatasetClauseContext _localctx = new DatasetClauseContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_datasetClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			match(FROM);
			setState(389);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(388);
				match(NAMED);
				}
			}

			setState(391);
			iri();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhereClauseContext extends ParserRuleContext {
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(SparqlParser.WHERE, 0); }
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitWhereClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_whereClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(394);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(393);
				match(WHERE);
				}
			}

			setState(396);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SolutionModifierContext extends ParserRuleContext {
		public GroupClauseContext groupClause() {
			return getRuleContext(GroupClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public OrderClauseContext orderClause() {
			return getRuleContext(OrderClauseContext.class,0);
		}
		public LimitOffsetClausesContext limitOffsetClauses() {
			return getRuleContext(LimitOffsetClausesContext.class,0);
		}
		public SolutionModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_solutionModifier; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSolutionModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SolutionModifierContext solutionModifier() throws RecognitionException {
		SolutionModifierContext _localctx = new SolutionModifierContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_solutionModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(399);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(398);
				groupClause();
				}
			}

			setState(402);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(401);
				havingClause();
				}
			}

			setState(405);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(404);
				orderClause();
				}
			}

			setState(408);
			_la = _input.LA(1);
			if (_la==LIMIT || _la==OFFSET) {
				{
				setState(407);
				limitOffsetClauses();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupClauseContext extends ParserRuleContext {
		public TerminalNode GROUP() { return getToken(SparqlParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(SparqlParser.BY, 0); }
		public List<GroupConditionContext> groupCondition() {
			return getRuleContexts(GroupConditionContext.class);
		}
		public GroupConditionContext groupCondition(int i) {
			return getRuleContext(GroupConditionContext.class,i);
		}
		public GroupClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupClauseContext groupClause() throws RecognitionException {
		GroupClauseContext _localctx = new GroupClauseContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_groupClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410);
			match(GROUP);
			setState(411);
			match(BY);
			setState(413); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(412);
				groupCondition();
				}
				}
				setState(415); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 25)) & ~0x3f) == 0 && ((1L << (_la - 25)) & ((1L << (STR - 25)) | (1L << (LANG - 25)) | (1L << (LANGMATCHES - 25)) | (1L << (DATATYPE - 25)) | (1L << (BOUND - 25)) | (1L << (SAMETERM - 25)) | (1L << (ISIRI - 25)) | (1L << (ISURI - 25)) | (1L << (ISBLANK - 25)) | (1L << (ISLITERAL - 25)) | (1L << (REGEX - 25)) | (1L << (SUBSTR - 25)) | (1L << (IRI - 25)) | (1L << (URI - 25)) | (1L << (BNODE - 25)) | (1L << (RAND - 25)) | (1L << (ABS - 25)) | (1L << (CEIL - 25)) | (1L << (FLOOR - 25)) | (1L << (ROUND - 25)) | (1L << (CONCAT - 25)) | (1L << (STRLEN - 25)) | (1L << (UCASE - 25)) | (1L << (LCASE - 25)) | (1L << (ENCODE_FOR_URI - 25)) | (1L << (CONTAINS - 25)) | (1L << (STRSTARTS - 25)) | (1L << (STRENDS - 25)) | (1L << (STRBEFORE - 25)) | (1L << (STRAFTER - 25)) | (1L << (REPLACE - 25)) | (1L << (YEAR - 25)) | (1L << (MONTH - 25)) | (1L << (DAY - 25)) | (1L << (HOURS - 25)) | (1L << (MINUTES - 25)) | (1L << (SECONDS - 25)))) != 0) || ((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & ((1L << (TIMEZONE - 89)) | (1L << (TZ - 89)) | (1L << (NOW - 89)) | (1L << (UUID - 89)) | (1L << (STRUUID - 89)) | (1L << (MD5 - 89)) | (1L << (SHA1 - 89)) | (1L << (SHA256 - 89)) | (1L << (SHA384 - 89)) | (1L << (SHA512 - 89)) | (1L << (COALESCE - 89)) | (1L << (IF - 89)) | (1L << (STRLANG - 89)) | (1L << (STRDT - 89)) | (1L << (ISNUMERIC - 89)) | (1L << (COUNT - 89)) | (1L << (SUM - 89)) | (1L << (MIN - 89)) | (1L << (MAX - 89)) | (1L << (AVG - 89)) | (1L << (SAMPLE - 89)) | (1L << (GROUP_CONCAT - 89)) | (1L << (NOT - 89)) | (1L << (EXISTS - 89)) | (1L << (IRIREF - 89)) | (1L << (PNAME_NS - 89)) | (1L << (PNAME_LN - 89)) | (1L << (VAR1 - 89)) | (1L << (VAR2 - 89)) | (1L << (OPEN_BRACE - 89)))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupConditionContext extends ParserRuleContext {
		public BuiltInCallContext builtInCall() {
			return getRuleContext(BuiltInCallContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(SparqlParser.AS, 0); }
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public GroupConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupCondition; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupConditionContext groupCondition() throws RecognitionException {
		GroupConditionContext _localctx = new GroupConditionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_groupCondition);
		int _la;
		try {
			setState(428);
			switch (_input.LA(1)) {
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
				enterOuterAlt(_localctx, 1);
				{
				setState(417);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(418);
				functionCall();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(419);
				match(OPEN_BRACE);
				setState(420);
				expression(0);
				setState(423);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(421);
					match(AS);
					setState(422);
					var();
					}
				}

				setState(425);
				match(CLOSE_BRACE);
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 4);
				{
				setState(427);
				var();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HavingClauseContext extends ParserRuleContext {
		public TerminalNode HAVING() { return getToken(SparqlParser.HAVING, 0); }
		public List<HavingConditionContext> havingCondition() {
			return getRuleContexts(HavingConditionContext.class);
		}
		public HavingConditionContext havingCondition(int i) {
			return getRuleContext(HavingConditionContext.class,i);
		}
		public HavingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitHavingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_havingClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(430);
			match(HAVING);
			setState(432); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(431);
				havingCondition();
				}
				}
				setState(434); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 25)) & ~0x3f) == 0 && ((1L << (_la - 25)) & ((1L << (STR - 25)) | (1L << (LANG - 25)) | (1L << (LANGMATCHES - 25)) | (1L << (DATATYPE - 25)) | (1L << (BOUND - 25)) | (1L << (SAMETERM - 25)) | (1L << (ISIRI - 25)) | (1L << (ISURI - 25)) | (1L << (ISBLANK - 25)) | (1L << (ISLITERAL - 25)) | (1L << (REGEX - 25)) | (1L << (SUBSTR - 25)) | (1L << (IRI - 25)) | (1L << (URI - 25)) | (1L << (BNODE - 25)) | (1L << (RAND - 25)) | (1L << (ABS - 25)) | (1L << (CEIL - 25)) | (1L << (FLOOR - 25)) | (1L << (ROUND - 25)) | (1L << (CONCAT - 25)) | (1L << (STRLEN - 25)) | (1L << (UCASE - 25)) | (1L << (LCASE - 25)) | (1L << (ENCODE_FOR_URI - 25)) | (1L << (CONTAINS - 25)) | (1L << (STRSTARTS - 25)) | (1L << (STRENDS - 25)) | (1L << (STRBEFORE - 25)) | (1L << (STRAFTER - 25)) | (1L << (REPLACE - 25)) | (1L << (YEAR - 25)) | (1L << (MONTH - 25)) | (1L << (DAY - 25)) | (1L << (HOURS - 25)) | (1L << (MINUTES - 25)) | (1L << (SECONDS - 25)))) != 0) || ((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & ((1L << (TIMEZONE - 89)) | (1L << (TZ - 89)) | (1L << (NOW - 89)) | (1L << (UUID - 89)) | (1L << (STRUUID - 89)) | (1L << (MD5 - 89)) | (1L << (SHA1 - 89)) | (1L << (SHA256 - 89)) | (1L << (SHA384 - 89)) | (1L << (SHA512 - 89)) | (1L << (COALESCE - 89)) | (1L << (IF - 89)) | (1L << (STRLANG - 89)) | (1L << (STRDT - 89)) | (1L << (ISNUMERIC - 89)) | (1L << (COUNT - 89)) | (1L << (SUM - 89)) | (1L << (MIN - 89)) | (1L << (MAX - 89)) | (1L << (AVG - 89)) | (1L << (SAMPLE - 89)) | (1L << (GROUP_CONCAT - 89)) | (1L << (NOT - 89)) | (1L << (EXISTS - 89)) | (1L << (IRIREF - 89)) | (1L << (PNAME_NS - 89)) | (1L << (PNAME_LN - 89)) | (1L << (OPEN_BRACE - 89)))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HavingConditionContext extends ParserRuleContext {
		public ConstraintContext constraint() {
			return getRuleContext(ConstraintContext.class,0);
		}
		public HavingConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingCondition; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitHavingCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingConditionContext havingCondition() throws RecognitionException {
		HavingConditionContext _localctx = new HavingConditionContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_havingCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(436);
			constraint();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderClauseContext extends ParserRuleContext {
		public TerminalNode ORDER() { return getToken(SparqlParser.ORDER, 0); }
		public TerminalNode BY() { return getToken(SparqlParser.BY, 0); }
		public List<OrderConditionContext> orderCondition() {
			return getRuleContexts(OrderConditionContext.class);
		}
		public OrderConditionContext orderCondition(int i) {
			return getRuleContext(OrderConditionContext.class,i);
		}
		public OrderClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOrderClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderClauseContext orderClause() throws RecognitionException {
		OrderClauseContext _localctx = new OrderClauseContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_orderClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(438);
			match(ORDER);
			setState(439);
			match(BY);
			setState(441); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(440);
				orderCondition();
				}
				}
				setState(443); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ASC) | (1L << DESC) | (1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)))) != 0) || _la==OPEN_BRACE );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderConditionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode ASC() { return getToken(SparqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(SparqlParser.DESC, 0); }
		public ConstraintContext constraint() {
			return getRuleContext(ConstraintContext.class,0);
		}
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public OrderConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderCondition; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOrderCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderConditionContext orderCondition() throws RecognitionException {
		OrderConditionContext _localctx = new OrderConditionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_orderCondition);
		int _la;
		try {
			setState(452);
			switch (_input.LA(1)) {
			case ASC:
			case DESC:
				enterOuterAlt(_localctx, 1);
				{
				setState(445);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(446);
				match(OPEN_BRACE);
				setState(447);
				expression(0);
				setState(448);
				match(CLOSE_BRACE);
				}
				break;
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(450);
				constraint();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 3);
				{
				setState(451);
				var();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LimitOffsetClausesContext extends ParserRuleContext {
		public LimitClauseContext limitClause() {
			return getRuleContext(LimitClauseContext.class,0);
		}
		public OffsetClauseContext offsetClause() {
			return getRuleContext(OffsetClauseContext.class,0);
		}
		public LimitOffsetClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limitOffsetClauses; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLimitOffsetClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitOffsetClausesContext limitOffsetClauses() throws RecognitionException {
		LimitOffsetClausesContext _localctx = new LimitOffsetClausesContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_limitOffsetClauses);
		int _la;
		try {
			setState(462);
			switch (_input.LA(1)) {
			case LIMIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(454);
				limitClause();
				setState(456);
				_la = _input.LA(1);
				if (_la==OFFSET) {
					{
					setState(455);
					offsetClause();
					}
				}

				}
				break;
			case OFFSET:
				enterOuterAlt(_localctx, 2);
				{
				setState(458);
				offsetClause();
				setState(460);
				_la = _input.LA(1);
				if (_la==LIMIT) {
					{
					setState(459);
					limitClause();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LimitClauseContext extends ParserRuleContext {
		public TerminalNode LIMIT() { return getToken(SparqlParser.LIMIT, 0); }
		public TerminalNode INTEGER() { return getToken(SparqlParser.INTEGER, 0); }
		public LimitClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limitClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLimitClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitClauseContext limitClause() throws RecognitionException {
		LimitClauseContext _localctx = new LimitClauseContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_limitClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			match(LIMIT);
			setState(465);
			match(INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OffsetClauseContext extends ParserRuleContext {
		public TerminalNode OFFSET() { return getToken(SparqlParser.OFFSET, 0); }
		public TerminalNode INTEGER() { return getToken(SparqlParser.INTEGER, 0); }
		public OffsetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_offsetClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOffsetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OffsetClauseContext offsetClause() throws RecognitionException {
		OffsetClauseContext _localctx = new OffsetClauseContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_offsetClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(467);
			match(OFFSET);
			setState(468);
			match(INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValuesClauseContext extends ParserRuleContext {
		public TerminalNode VALUES() { return getToken(SparqlParser.VALUES, 0); }
		public DataBlockContext dataBlock() {
			return getRuleContext(DataBlockContext.class,0);
		}
		public ValuesClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valuesClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitValuesClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValuesClauseContext valuesClause() throws RecognitionException {
		ValuesClauseContext _localctx = new ValuesClauseContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_valuesClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(472);
			_la = _input.LA(1);
			if (_la==VALUES) {
				{
				setState(470);
				match(VALUES);
				setState(471);
				dataBlock();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UpdateCommandContext extends ParserRuleContext {
		public List<PrologueContext> prologue() {
			return getRuleContexts(PrologueContext.class);
		}
		public PrologueContext prologue(int i) {
			return getRuleContext(PrologueContext.class,i);
		}
		public List<UpdateContext> update() {
			return getRuleContexts(UpdateContext.class);
		}
		public UpdateContext update(int i) {
			return getRuleContext(UpdateContext.class,i);
		}
		public UpdateCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_updateCommand; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUpdateCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateCommandContext updateCommand() throws RecognitionException {
		UpdateCommandContext _localctx = new UpdateCommandContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_updateCommand);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(474);
			prologue();
			setState(489);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LOAD) | (1L << CLEAR) | (1L << DROP) | (1L << ADD) | (1L << MOVE) | (1L << COPY) | (1L << CREATE) | (1L << DELETE) | (1L << INSERT) | (1L << WITH))) != 0)) {
				{
				setState(475);
				update();
				setState(482);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(476);
						match(SEMICOLON);
						setState(477);
						prologue();
						setState(478);
						update();
						}
						} 
					}
					setState(484);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
				}
				setState(487);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(485);
					match(SEMICOLON);
					setState(486);
					prologue();
					}
				}

				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UpdateContext extends ParserRuleContext {
		public LoadContext load() {
			return getRuleContext(LoadContext.class,0);
		}
		public ClearContext clear() {
			return getRuleContext(ClearContext.class,0);
		}
		public DropContext drop() {
			return getRuleContext(DropContext.class,0);
		}
		public AddContext add() {
			return getRuleContext(AddContext.class,0);
		}
		public MoveContext move() {
			return getRuleContext(MoveContext.class,0);
		}
		public CopyContext copy() {
			return getRuleContext(CopyContext.class,0);
		}
		public CreateContext create() {
			return getRuleContext(CreateContext.class,0);
		}
		public InsertDataContext insertData() {
			return getRuleContext(InsertDataContext.class,0);
		}
		public DeleteDataContext deleteData() {
			return getRuleContext(DeleteDataContext.class,0);
		}
		public DeleteWhereContext deleteWhere() {
			return getRuleContext(DeleteWhereContext.class,0);
		}
		public ModifyContext modify() {
			return getRuleContext(ModifyContext.class,0);
		}
		public UpdateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_update; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUpdate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateContext update() throws RecognitionException {
		UpdateContext _localctx = new UpdateContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_update);
		try {
			setState(502);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(491);
				load();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(492);
				clear();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(493);
				drop();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(494);
				add();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(495);
				move();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(496);
				copy();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(497);
				create();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(498);
				insertData();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(499);
				deleteData();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(500);
				deleteWhere();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(501);
				modify();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LoadContext extends ParserRuleContext {
		public TerminalNode LOAD() { return getToken(SparqlParser.LOAD, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public TerminalNode INTO() { return getToken(SparqlParser.INTO, 0); }
		public GraphRefContext graphRef() {
			return getRuleContext(GraphRefContext.class,0);
		}
		public LoadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_load; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLoad(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LoadContext load() throws RecognitionException {
		LoadContext _localctx = new LoadContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_load);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(504);
			match(LOAD);
			setState(506);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(505);
				match(SILENT);
				}
			}

			setState(508);
			iri();
			setState(511);
			_la = _input.LA(1);
			if (_la==INTO) {
				{
				setState(509);
				match(INTO);
				setState(510);
				graphRef();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClearContext extends ParserRuleContext {
		public TerminalNode CLEAR() { return getToken(SparqlParser.CLEAR, 0); }
		public GraphRefAllContext graphRefAll() {
			return getRuleContext(GraphRefAllContext.class,0);
		}
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public ClearContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clear; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitClear(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClearContext clear() throws RecognitionException {
		ClearContext _localctx = new ClearContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_clear);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(513);
			match(CLEAR);
			setState(515);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(514);
				match(SILENT);
				}
			}

			setState(517);
			graphRefAll();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DropContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(SparqlParser.DROP, 0); }
		public GraphRefAllContext graphRefAll() {
			return getRuleContext(GraphRefAllContext.class,0);
		}
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public DropContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDrop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DropContext drop() throws RecognitionException {
		DropContext _localctx = new DropContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_drop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(519);
			match(DROP);
			setState(521);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(520);
				match(SILENT);
				}
			}

			setState(523);
			graphRefAll();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreateContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(SparqlParser.CREATE, 0); }
		public GraphRefContext graphRef() {
			return getRuleContext(GraphRefContext.class,0);
		}
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public CreateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCreate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateContext create() throws RecognitionException {
		CreateContext _localctx = new CreateContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_create);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(525);
			match(CREATE);
			setState(527);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(526);
				match(SILENT);
				}
			}

			setState(529);
			graphRef();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AddContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(SparqlParser.ADD, 0); }
		public List<GraphOrDefaultContext> graphOrDefault() {
			return getRuleContexts(GraphOrDefaultContext.class);
		}
		public GraphOrDefaultContext graphOrDefault(int i) {
			return getRuleContext(GraphOrDefaultContext.class,i);
		}
		public TerminalNode TO() { return getToken(SparqlParser.TO, 0); }
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public AddContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAdd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AddContext add() throws RecognitionException {
		AddContext _localctx = new AddContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_add);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(531);
			match(ADD);
			setState(533);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(532);
				match(SILENT);
				}
			}

			setState(535);
			graphOrDefault();
			setState(536);
			match(TO);
			setState(537);
			graphOrDefault();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MoveContext extends ParserRuleContext {
		public TerminalNode MOVE() { return getToken(SparqlParser.MOVE, 0); }
		public List<GraphOrDefaultContext> graphOrDefault() {
			return getRuleContexts(GraphOrDefaultContext.class);
		}
		public GraphOrDefaultContext graphOrDefault(int i) {
			return getRuleContext(GraphOrDefaultContext.class,i);
		}
		public TerminalNode TO() { return getToken(SparqlParser.TO, 0); }
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public MoveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_move; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitMove(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MoveContext move() throws RecognitionException {
		MoveContext _localctx = new MoveContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_move);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(539);
			match(MOVE);
			setState(541);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(540);
				match(SILENT);
				}
			}

			setState(543);
			graphOrDefault();
			setState(544);
			match(TO);
			setState(545);
			graphOrDefault();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CopyContext extends ParserRuleContext {
		public TerminalNode COPY() { return getToken(SparqlParser.COPY, 0); }
		public List<GraphOrDefaultContext> graphOrDefault() {
			return getRuleContexts(GraphOrDefaultContext.class);
		}
		public GraphOrDefaultContext graphOrDefault(int i) {
			return getRuleContext(GraphOrDefaultContext.class,i);
		}
		public TerminalNode TO() { return getToken(SparqlParser.TO, 0); }
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public CopyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_copy; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCopy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopyContext copy() throws RecognitionException {
		CopyContext _localctx = new CopyContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_copy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(547);
			match(COPY);
			setState(549);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(548);
				match(SILENT);
				}
			}

			setState(551);
			graphOrDefault();
			setState(552);
			match(TO);
			setState(553);
			graphOrDefault();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InsertDataContext extends ParserRuleContext {
		public TerminalNode INSERT() { return getToken(SparqlParser.INSERT, 0); }
		public TerminalNode DATA() { return getToken(SparqlParser.DATA, 0); }
		public QuadDataContext quadData() {
			return getRuleContext(QuadDataContext.class,0);
		}
		public InsertDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertData; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInsertData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertDataContext insertData() throws RecognitionException {
		InsertDataContext _localctx = new InsertDataContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_insertData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(555);
			match(INSERT);
			setState(556);
			match(DATA);
			setState(557);
			quadData();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeleteDataContext extends ParserRuleContext {
		public TerminalNode DELETE() { return getToken(SparqlParser.DELETE, 0); }
		public TerminalNode DATA() { return getToken(SparqlParser.DATA, 0); }
		public QuadDataContext quadData() {
			return getRuleContext(QuadDataContext.class,0);
		}
		public DeleteDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteData; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteDataContext deleteData() throws RecognitionException {
		DeleteDataContext _localctx = new DeleteDataContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_deleteData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(559);
			match(DELETE);
			setState(560);
			match(DATA);
			setState(561);
			quadData();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeleteWhereContext extends ParserRuleContext {
		public TerminalNode DELETE() { return getToken(SparqlParser.DELETE, 0); }
		public TerminalNode WHERE() { return getToken(SparqlParser.WHERE, 0); }
		public QuadPatternContext quadPattern() {
			return getRuleContext(QuadPatternContext.class,0);
		}
		public DeleteWhereContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteWhere; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteWhere(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteWhereContext deleteWhere() throws RecognitionException {
		DeleteWhereContext _localctx = new DeleteWhereContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_deleteWhere);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(563);
			match(DELETE);
			setState(564);
			match(WHERE);
			setState(565);
			quadPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModifyContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(SparqlParser.WHERE, 0); }
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public DeleteClauseContext deleteClause() {
			return getRuleContext(DeleteClauseContext.class,0);
		}
		public InsertClauseContext insertClause() {
			return getRuleContext(InsertClauseContext.class,0);
		}
		public TerminalNode WITH() { return getToken(SparqlParser.WITH, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public List<UsingClauseContext> usingClause() {
			return getRuleContexts(UsingClauseContext.class);
		}
		public UsingClauseContext usingClause(int i) {
			return getRuleContext(UsingClauseContext.class,i);
		}
		public ModifyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modify; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitModify(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifyContext modify() throws RecognitionException {
		ModifyContext _localctx = new ModifyContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_modify);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(567);
				match(WITH);
				setState(568);
				iri();
				}
			}

			setState(576);
			switch (_input.LA(1)) {
			case DELETE:
				{
				setState(571);
				deleteClause();
				setState(573);
				_la = _input.LA(1);
				if (_la==INSERT) {
					{
					setState(572);
					insertClause();
					}
				}

				}
				break;
			case INSERT:
				{
				setState(575);
				insertClause();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(581);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==USING) {
				{
				{
				setState(578);
				usingClause();
				}
				}
				setState(583);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(584);
			match(WHERE);
			setState(585);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeleteClauseContext extends ParserRuleContext {
		public TerminalNode DELETE() { return getToken(SparqlParser.DELETE, 0); }
		public QuadPatternContext quadPattern() {
			return getRuleContext(QuadPatternContext.class,0);
		}
		public DeleteClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteClauseContext deleteClause() throws RecognitionException {
		DeleteClauseContext _localctx = new DeleteClauseContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_deleteClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(587);
			match(DELETE);
			setState(588);
			quadPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InsertClauseContext extends ParserRuleContext {
		public TerminalNode INSERT() { return getToken(SparqlParser.INSERT, 0); }
		public QuadPatternContext quadPattern() {
			return getRuleContext(QuadPatternContext.class,0);
		}
		public InsertClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInsertClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertClauseContext insertClause() throws RecognitionException {
		InsertClauseContext _localctx = new InsertClauseContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_insertClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(590);
			match(INSERT);
			setState(591);
			quadPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UsingClauseContext extends ParserRuleContext {
		public TerminalNode USING() { return getToken(SparqlParser.USING, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode NAMED() { return getToken(SparqlParser.NAMED, 0); }
		public UsingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_usingClause; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUsingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UsingClauseContext usingClause() throws RecognitionException {
		UsingClauseContext _localctx = new UsingClauseContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_usingClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(593);
			match(USING);
			setState(595);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(594);
				match(NAMED);
				}
			}

			setState(597);
			iri();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphOrDefaultContext extends ParserRuleContext {
		public TerminalNode DEFAULT() { return getToken(SparqlParser.DEFAULT, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode GRAPH() { return getToken(SparqlParser.GRAPH, 0); }
		public GraphOrDefaultContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphOrDefault; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphOrDefault(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphOrDefaultContext graphOrDefault() throws RecognitionException {
		GraphOrDefaultContext _localctx = new GraphOrDefaultContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_graphOrDefault);
		int _la;
		try {
			setState(604);
			switch (_input.LA(1)) {
			case DEFAULT:
				enterOuterAlt(_localctx, 1);
				{
				setState(599);
				match(DEFAULT);
				}
				break;
			case GRAPH:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(601);
				_la = _input.LA(1);
				if (_la==GRAPH) {
					{
					setState(600);
					match(GRAPH);
					}
				}

				setState(603);
				iri();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphRefContext extends ParserRuleContext {
		public TerminalNode GRAPH() { return getToken(SparqlParser.GRAPH, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public GraphRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphRef; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphRef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphRefContext graphRef() throws RecognitionException {
		GraphRefContext _localctx = new GraphRefContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_graphRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(606);
			match(GRAPH);
			setState(607);
			iri();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphRefAllContext extends ParserRuleContext {
		public GraphRefContext graphRef() {
			return getRuleContext(GraphRefContext.class,0);
		}
		public TerminalNode DEFAULT() { return getToken(SparqlParser.DEFAULT, 0); }
		public TerminalNode NAMED() { return getToken(SparqlParser.NAMED, 0); }
		public TerminalNode ALL() { return getToken(SparqlParser.ALL, 0); }
		public GraphRefAllContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphRefAll; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphRefAll(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphRefAllContext graphRefAll() throws RecognitionException {
		GraphRefAllContext _localctx = new GraphRefAllContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_graphRefAll);
		try {
			setState(613);
			switch (_input.LA(1)) {
			case GRAPH:
				enterOuterAlt(_localctx, 1);
				{
				setState(609);
				graphRef();
				}
				break;
			case DEFAULT:
				enterOuterAlt(_localctx, 2);
				{
				setState(610);
				match(DEFAULT);
				}
				break;
			case NAMED:
				enterOuterAlt(_localctx, 3);
				{
				setState(611);
				match(NAMED);
				}
				break;
			case ALL:
				enterOuterAlt(_localctx, 4);
				{
				setState(612);
				match(ALL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuadPatternContext extends ParserRuleContext {
		public QuadsContext quads() {
			return getRuleContext(QuadsContext.class,0);
		}
		public QuadPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quadPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadPatternContext quadPattern() throws RecognitionException {
		QuadPatternContext _localctx = new QuadPatternContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_quadPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(615);
			match(OPEN_CURLY_BRACE);
			setState(616);
			quads();
			setState(617);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuadDataContext extends ParserRuleContext {
		public QuadsContext quads() {
			return getRuleContext(QuadsContext.class,0);
		}
		public QuadDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quadData; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadDataContext quadData() throws RecognitionException {
		QuadDataContext _localctx = new QuadDataContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_quadData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(619);
			match(OPEN_CURLY_BRACE);
			setState(620);
			quads();
			setState(621);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuadsContext extends ParserRuleContext {
		public TriplesTemplateContext triplesTemplate() {
			return getRuleContext(TriplesTemplateContext.class,0);
		}
		public List<QuadsDetailsContext> quadsDetails() {
			return getRuleContexts(QuadsDetailsContext.class);
		}
		public QuadsDetailsContext quadsDetails(int i) {
			return getRuleContext(QuadsDetailsContext.class,i);
		}
		public QuadsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quads; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuads(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsContext quads() throws RecognitionException {
		QuadsContext _localctx = new QuadsContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_quads);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(624);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(623);
				triplesTemplate();
				}
			}

			setState(629);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==GRAPH) {
				{
				{
				setState(626);
				quadsDetails();
				}
				}
				setState(631);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuadsDetailsContext extends ParserRuleContext {
		public QuadsNotTriplesContext quadsNotTriples() {
			return getRuleContext(QuadsNotTriplesContext.class,0);
		}
		public TriplesTemplateContext triplesTemplate() {
			return getRuleContext(TriplesTemplateContext.class,0);
		}
		public QuadsDetailsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quadsDetails; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadsDetails(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsDetailsContext quadsDetails() throws RecognitionException {
		QuadsDetailsContext _localctx = new QuadsDetailsContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_quadsDetails);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(632);
			quadsNotTriples();
			setState(634);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(633);
				match(DOT);
				}
			}

			setState(637);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(636);
				triplesTemplate();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuadsNotTriplesContext extends ParserRuleContext {
		public TerminalNode GRAPH() { return getToken(SparqlParser.GRAPH, 0); }
		public VarOrIRIContext varOrIRI() {
			return getRuleContext(VarOrIRIContext.class,0);
		}
		public TriplesTemplateContext triplesTemplate() {
			return getRuleContext(TriplesTemplateContext.class,0);
		}
		public QuadsNotTriplesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quadsNotTriples; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadsNotTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsNotTriplesContext quadsNotTriples() throws RecognitionException {
		QuadsNotTriplesContext _localctx = new QuadsNotTriplesContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_quadsNotTriples);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(639);
			match(GRAPH);
			setState(640);
			varOrIRI();
			setState(641);
			match(OPEN_CURLY_BRACE);
			setState(643);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(642);
				triplesTemplate();
				}
			}

			setState(645);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesTemplateContext extends ParserRuleContext {
		public List<TriplesSameSubjectContext> triplesSameSubject() {
			return getRuleContexts(TriplesSameSubjectContext.class);
		}
		public TriplesSameSubjectContext triplesSameSubject(int i) {
			return getRuleContext(TriplesSameSubjectContext.class,i);
		}
		public TriplesTemplateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesTemplate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesTemplate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesTemplateContext triplesTemplate() throws RecognitionException {
		TriplesTemplateContext _localctx = new TriplesTemplateContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_triplesTemplate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(647);
			triplesSameSubject();
			setState(654);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(648);
				match(DOT);
				setState(650);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
					{
					setState(649);
					triplesSameSubject();
					}
				}

				}
				}
				setState(656);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupGraphPatternContext extends ParserRuleContext {
		public SubSelectContext subSelect() {
			return getRuleContext(SubSelectContext.class,0);
		}
		public GroupGraphPatternSubContext groupGraphPatternSub() {
			return getRuleContext(GroupGraphPatternSubContext.class,0);
		}
		public GroupGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternContext groupGraphPattern() throws RecognitionException {
		GroupGraphPatternContext _localctx = new GroupGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_groupGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(657);
			match(OPEN_CURLY_BRACE);
			setState(660);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(658);
				subSelect();
				}
				break;
			case VALUES:
			case OPTIONAL:
			case GRAPH:
			case FILTER:
			case TRUE:
			case FALSE:
			case SERVICE:
			case BIND:
			case MINUS:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case BLANK_NODE_LABEL:
			case VAR1:
			case VAR2:
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
			case OPEN_BRACE:
			case OPEN_CURLY_BRACE:
			case CLOSE_CURLY_BRACE:
			case OPEN_SQUARE_BRACKET:
				{
				setState(659);
				groupGraphPatternSub();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(662);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupGraphPatternSubContext extends ParserRuleContext {
		public TriplesBlockContext triplesBlock() {
			return getRuleContext(TriplesBlockContext.class,0);
		}
		public List<GroupGraphPatternSubListContext> groupGraphPatternSubList() {
			return getRuleContexts(GroupGraphPatternSubListContext.class);
		}
		public GroupGraphPatternSubListContext groupGraphPatternSubList(int i) {
			return getRuleContext(GroupGraphPatternSubListContext.class,i);
		}
		public GroupGraphPatternSubContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupGraphPatternSub; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPatternSub(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternSubContext groupGraphPatternSub() throws RecognitionException {
		GroupGraphPatternSubContext _localctx = new GroupGraphPatternSubContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_groupGraphPatternSub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(665);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(664);
				triplesBlock();
				}
			}

			setState(670);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALUES) | (1L << OPTIONAL) | (1L << GRAPH) | (1L << FILTER) | (1L << SERVICE) | (1L << BIND) | (1L << MINUS))) != 0) || _la==OPEN_CURLY_BRACE) {
				{
				{
				setState(667);
				groupGraphPatternSubList();
				}
				}
				setState(672);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupGraphPatternSubListContext extends ParserRuleContext {
		public GraphPatternNotTriplesContext graphPatternNotTriples() {
			return getRuleContext(GraphPatternNotTriplesContext.class,0);
		}
		public TriplesBlockContext triplesBlock() {
			return getRuleContext(TriplesBlockContext.class,0);
		}
		public GroupGraphPatternSubListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupGraphPatternSubList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPatternSubList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternSubListContext groupGraphPatternSubList() throws RecognitionException {
		GroupGraphPatternSubListContext _localctx = new GroupGraphPatternSubListContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_groupGraphPatternSubList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(673);
			graphPatternNotTriples();
			setState(675);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(674);
				match(DOT);
				}
			}

			setState(678);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(677);
				triplesBlock();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesBlockContext extends ParserRuleContext {
		public List<TriplesSameSubjectPathContext> triplesSameSubjectPath() {
			return getRuleContexts(TriplesSameSubjectPathContext.class);
		}
		public TriplesSameSubjectPathContext triplesSameSubjectPath(int i) {
			return getRuleContext(TriplesSameSubjectPathContext.class,i);
		}
		public TriplesBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesBlock; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesBlockContext triplesBlock() throws RecognitionException {
		TriplesBlockContext _localctx = new TriplesBlockContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_triplesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(680);
			triplesSameSubjectPath();
			setState(687);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(681);
				match(DOT);
				setState(683);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
					{
					setState(682);
					triplesSameSubjectPath();
					}
				}

				}
				}
				setState(689);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphPatternNotTriplesContext extends ParserRuleContext {
		public GroupOrUnionGraphPatternContext groupOrUnionGraphPattern() {
			return getRuleContext(GroupOrUnionGraphPatternContext.class,0);
		}
		public OptionalGraphPatternContext optionalGraphPattern() {
			return getRuleContext(OptionalGraphPatternContext.class,0);
		}
		public MinusGraphPatternContext minusGraphPattern() {
			return getRuleContext(MinusGraphPatternContext.class,0);
		}
		public GraphGraphPatternContext graphGraphPattern() {
			return getRuleContext(GraphGraphPatternContext.class,0);
		}
		public ServiceGraphPatternContext serviceGraphPattern() {
			return getRuleContext(ServiceGraphPatternContext.class,0);
		}
		public FilterContext filter() {
			return getRuleContext(FilterContext.class,0);
		}
		public BindContext bind() {
			return getRuleContext(BindContext.class,0);
		}
		public InlineDataContext inlineData() {
			return getRuleContext(InlineDataContext.class,0);
		}
		public GraphPatternNotTriplesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphPatternNotTriples; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphPatternNotTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphPatternNotTriplesContext graphPatternNotTriples() throws RecognitionException {
		GraphPatternNotTriplesContext _localctx = new GraphPatternNotTriplesContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_graphPatternNotTriples);
		try {
			setState(698);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(690);
				groupOrUnionGraphPattern();
				}
				break;
			case OPTIONAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(691);
				optionalGraphPattern();
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(692);
				minusGraphPattern();
				}
				break;
			case GRAPH:
				enterOuterAlt(_localctx, 4);
				{
				setState(693);
				graphGraphPattern();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 5);
				{
				setState(694);
				serviceGraphPattern();
				}
				break;
			case FILTER:
				enterOuterAlt(_localctx, 6);
				{
				setState(695);
				filter();
				}
				break;
			case BIND:
				enterOuterAlt(_localctx, 7);
				{
				setState(696);
				bind();
				}
				break;
			case VALUES:
				enterOuterAlt(_localctx, 8);
				{
				setState(697);
				inlineData();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OptionalGraphPatternContext extends ParserRuleContext {
		public TerminalNode OPTIONAL() { return getToken(SparqlParser.OPTIONAL, 0); }
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public OptionalGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOptionalGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalGraphPatternContext optionalGraphPattern() throws RecognitionException {
		OptionalGraphPatternContext _localctx = new OptionalGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_optionalGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(700);
			match(OPTIONAL);
			setState(701);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphGraphPatternContext extends ParserRuleContext {
		public TerminalNode GRAPH() { return getToken(SparqlParser.GRAPH, 0); }
		public VarOrIRIContext varOrIRI() {
			return getRuleContext(VarOrIRIContext.class,0);
		}
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public GraphGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphGraphPatternContext graphGraphPattern() throws RecognitionException {
		GraphGraphPatternContext _localctx = new GraphGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_graphGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(703);
			match(GRAPH);
			setState(704);
			varOrIRI();
			setState(705);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ServiceGraphPatternContext extends ParserRuleContext {
		public TerminalNode SERVICE() { return getToken(SparqlParser.SERVICE, 0); }
		public VarOrIRIContext varOrIRI() {
			return getRuleContext(VarOrIRIContext.class,0);
		}
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public TerminalNode SILENT() { return getToken(SparqlParser.SILENT, 0); }
		public ServiceGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_serviceGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitServiceGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ServiceGraphPatternContext serviceGraphPattern() throws RecognitionException {
		ServiceGraphPatternContext _localctx = new ServiceGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_serviceGraphPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(707);
			match(SERVICE);
			setState(709);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(708);
				match(SILENT);
				}
			}

			setState(711);
			varOrIRI();
			setState(712);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BindContext extends ParserRuleContext {
		public TerminalNode BIND() { return getToken(SparqlParser.BIND, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(SparqlParser.AS, 0); }
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public BindContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bind; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBind(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BindContext bind() throws RecognitionException {
		BindContext _localctx = new BindContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_bind);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(714);
			match(BIND);
			setState(715);
			match(OPEN_BRACE);
			setState(716);
			expression(0);
			setState(717);
			match(AS);
			setState(718);
			var();
			setState(719);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineDataContext extends ParserRuleContext {
		public TerminalNode VALUES() { return getToken(SparqlParser.VALUES, 0); }
		public DataBlockContext dataBlock() {
			return getRuleContext(DataBlockContext.class,0);
		}
		public InlineDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineData; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataContext inlineData() throws RecognitionException {
		InlineDataContext _localctx = new InlineDataContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_inlineData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(721);
			match(VALUES);
			setState(722);
			dataBlock();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataBlockContext extends ParserRuleContext {
		public InlineDataOneVarContext inlineDataOneVar() {
			return getRuleContext(InlineDataOneVarContext.class,0);
		}
		public InlineDataFullContext inlineDataFull() {
			return getRuleContext(InlineDataFullContext.class,0);
		}
		public DataBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataBlock; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockContext dataBlock() throws RecognitionException {
		DataBlockContext _localctx = new DataBlockContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_dataBlock);
		try {
			setState(726);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(724);
				inlineDataOneVar();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(725);
				inlineDataFull();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineDataOneVarContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public List<DataBlockValueContext> dataBlockValue() {
			return getRuleContexts(DataBlockValueContext.class);
		}
		public DataBlockValueContext dataBlockValue(int i) {
			return getRuleContext(DataBlockValueContext.class,i);
		}
		public InlineDataOneVarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineDataOneVar; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineDataOneVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataOneVarContext inlineDataOneVar() throws RecognitionException {
		InlineDataOneVarContext _localctx = new InlineDataOneVarContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_inlineDataOneVar);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(728);
			var();
			setState(729);
			match(OPEN_CURLY_BRACE);
			setState(733);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)))) != 0)) {
				{
				{
				setState(730);
				dataBlockValue();
				}
				}
				setState(735);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(736);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineDataFullContext extends ParserRuleContext {
		public List<VarContext> var() {
			return getRuleContexts(VarContext.class);
		}
		public VarContext var(int i) {
			return getRuleContext(VarContext.class,i);
		}
		public List<DataBlockValuesContext> dataBlockValues() {
			return getRuleContexts(DataBlockValuesContext.class);
		}
		public DataBlockValuesContext dataBlockValues(int i) {
			return getRuleContext(DataBlockValuesContext.class,i);
		}
		public InlineDataFullContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineDataFull; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineDataFull(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataFullContext inlineDataFull() throws RecognitionException {
		InlineDataFullContext _localctx = new InlineDataFullContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_inlineDataFull);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(738);
			match(OPEN_BRACE);
			setState(742);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==VAR1 || _la==VAR2) {
				{
				{
				setState(739);
				var();
				}
				}
				setState(744);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(745);
			match(CLOSE_BRACE);
			setState(746);
			match(OPEN_CURLY_BRACE);
			setState(750);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OPEN_BRACE) {
				{
				{
				setState(747);
				dataBlockValues();
				}
				}
				setState(752);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(753);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataBlockValuesContext extends ParserRuleContext {
		public List<DataBlockValueContext> dataBlockValue() {
			return getRuleContexts(DataBlockValueContext.class);
		}
		public DataBlockValueContext dataBlockValue(int i) {
			return getRuleContext(DataBlockValueContext.class,i);
		}
		public DataBlockValuesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataBlockValues; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlockValues(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockValuesContext dataBlockValues() throws RecognitionException {
		DataBlockValuesContext _localctx = new DataBlockValuesContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_dataBlockValues);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(755);
			match(OPEN_BRACE);
			setState(759);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)))) != 0)) {
				{
				{
				setState(756);
				dataBlockValue();
				}
				}
				setState(761);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(762);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataBlockValueContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public RdfLiteralContext rdfLiteral() {
			return getRuleContext(RdfLiteralContext.class,0);
		}
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
		public TerminalNode UNDEF() { return getToken(SparqlParser.UNDEF, 0); }
		public DataBlockValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataBlockValue; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlockValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockValueContext dataBlockValue() throws RecognitionException {
		DataBlockValueContext _localctx = new DataBlockValueContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_dataBlockValue);
		try {
			setState(769);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(764);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(765);
				rdfLiteral();
				}
				break;
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 3);
				{
				setState(766);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(767);
				booleanLiteral();
				}
				break;
			case UNDEF:
				enterOuterAlt(_localctx, 5);
				{
				setState(768);
				match(UNDEF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MinusGraphPatternContext extends ParserRuleContext {
		public TerminalNode MINUS() { return getToken(SparqlParser.MINUS, 0); }
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public MinusGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minusGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitMinusGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MinusGraphPatternContext minusGraphPattern() throws RecognitionException {
		MinusGraphPatternContext _localctx = new MinusGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_minusGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(771);
			match(MINUS);
			setState(772);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupOrUnionGraphPatternContext extends ParserRuleContext {
		public List<GroupGraphPatternContext> groupGraphPattern() {
			return getRuleContexts(GroupGraphPatternContext.class);
		}
		public GroupGraphPatternContext groupGraphPattern(int i) {
			return getRuleContext(GroupGraphPatternContext.class,i);
		}
		public List<TerminalNode> UNION() { return getTokens(SparqlParser.UNION); }
		public TerminalNode UNION(int i) {
			return getToken(SparqlParser.UNION, i);
		}
		public GroupOrUnionGraphPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupOrUnionGraphPattern; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupOrUnionGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupOrUnionGraphPatternContext groupOrUnionGraphPattern() throws RecognitionException {
		GroupOrUnionGraphPatternContext _localctx = new GroupOrUnionGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_groupOrUnionGraphPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			groupGraphPattern();
			setState(779);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION) {
				{
				{
				setState(775);
				match(UNION);
				setState(776);
				groupGraphPattern();
				}
				}
				setState(781);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterContext extends ParserRuleContext {
		public TerminalNode FILTER() { return getToken(SparqlParser.FILTER, 0); }
		public ConstraintContext constraint() {
			return getRuleContext(ConstraintContext.class,0);
		}
		public FilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filter; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitFilter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		FilterContext _localctx = new FilterContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_filter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(782);
			match(FILTER);
			setState(783);
			constraint();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstraintContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public BuiltInCallContext builtInCall() {
			return getRuleContext(BuiltInCallContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraint; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstraintContext constraint() throws RecognitionException {
		ConstraintContext _localctx = new ConstraintContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_constraint);
		try {
			setState(791);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(785);
				match(OPEN_BRACE);
				setState(786);
				expression(0);
				setState(787);
				match(CLOSE_BRACE);
				}
				break;
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
				enterOuterAlt(_localctx, 2);
				{
				setState(789);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(790);
				functionCall();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionCallContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public ArgListContext argList() {
			return getRuleContext(ArgListContext.class,0);
		}
		public FunctionCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionCall; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallContext functionCall() throws RecognitionException {
		FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_functionCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(793);
			iri();
			setState(794);
			argList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgListContext extends ParserRuleContext {
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public TerminalNode DISTINCT() { return getToken(SparqlParser.DISTINCT, 0); }
		public ArgListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitArgList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgListContext argList() throws RecognitionException {
		ArgListContext _localctx = new ArgListContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_argList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(796);
			match(OPEN_BRACE);
			setState(802);
			switch (_input.LA(1)) {
			case DISTINCT:
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case TRUE:
			case FALSE:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
			case OPEN_BRACE:
			case PLUS_SIGN:
			case MINUS_SIGN:
			case NEGATION:
				{
				setState(798);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(797);
					match(DISTINCT);
					}
				}

				setState(800);
				expressionList();
				}
				break;
			case CLOSE_BRACE:
				{
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(804);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionListContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ExpressionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitExpressionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionListContext expressionList() throws RecognitionException {
		ExpressionListContext _localctx = new ExpressionListContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_expressionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(806);
			expression(0);
			setState(811);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(807);
				match(COMMA);
				setState(808);
				expression(0);
				}
				}
				setState(813);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstructTemplateContext extends ParserRuleContext {
		public TriplesTemplateContext triplesTemplate() {
			return getRuleContext(TriplesTemplateContext.class,0);
		}
		public ConstructTemplateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructTemplate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructTemplate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructTemplateContext constructTemplate() throws RecognitionException {
		ConstructTemplateContext _localctx = new ConstructTemplateContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_constructTemplate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(814);
			match(OPEN_CURLY_BRACE);
			setState(816);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(815);
				triplesTemplate();
				}
			}

			setState(818);
			match(CLOSE_CURLY_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesSameSubjectContext extends ParserRuleContext {
		public VarOrTermContext varOrTerm() {
			return getRuleContext(VarOrTermContext.class,0);
		}
		public PropertyListNotEmptyContext propertyListNotEmpty() {
			return getRuleContext(PropertyListNotEmptyContext.class,0);
		}
		public TriplesNodeContext triplesNode() {
			return getRuleContext(TriplesNodeContext.class,0);
		}
		public PropertyListContext propertyList() {
			return getRuleContext(PropertyListContext.class,0);
		}
		public TriplesSameSubjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesSameSubject; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesSameSubject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesSameSubjectContext triplesSameSubject() throws RecognitionException {
		TriplesSameSubjectContext _localctx = new TriplesSameSubjectContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_triplesSameSubject);
		try {
			setState(826);
			switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(820);
				varOrTerm();
				setState(821);
				propertyListNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(823);
				triplesNode();
				setState(824);
				propertyList();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyListContext extends ParserRuleContext {
		public PropertyListNotEmptyContext propertyListNotEmpty() {
			return getRuleContext(PropertyListNotEmptyContext.class,0);
		}
		public PropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListContext propertyList() throws RecognitionException {
		PropertyListContext _localctx = new PropertyListContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_propertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(829);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0)) {
				{
				setState(828);
				propertyListNotEmpty();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyListNotEmptyContext extends ParserRuleContext {
		public List<VerbContext> verb() {
			return getRuleContexts(VerbContext.class);
		}
		public VerbContext verb(int i) {
			return getRuleContext(VerbContext.class,i);
		}
		public List<ObjectListContext> objectList() {
			return getRuleContexts(ObjectListContext.class);
		}
		public ObjectListContext objectList(int i) {
			return getRuleContext(ObjectListContext.class,i);
		}
		public PropertyListNotEmptyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyListNotEmpty; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListNotEmpty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListNotEmptyContext propertyListNotEmpty() throws RecognitionException {
		PropertyListNotEmptyContext _localctx = new PropertyListNotEmptyContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_propertyListNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(831);
			verb();
			setState(832);
			objectList();
			setState(841);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(833);
				match(SEMICOLON);
				setState(837);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0)) {
					{
					setState(834);
					verb();
					setState(835);
					objectList();
					}
				}

				}
				}
				setState(843);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VerbContext extends ParserRuleContext {
		public VarOrIRIContext varOrIRI() {
			return getRuleContext(VarOrIRIContext.class,0);
		}
		public TerminalNode A() { return getToken(SparqlParser.A, 0); }
		public VerbContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_verb; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerb(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbContext verb() throws RecognitionException {
		VerbContext _localctx = new VerbContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_verb);
		try {
			setState(846);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(844);
				varOrIRI();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(845);
				match(A);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectListContext extends ParserRuleContext {
		public List<ObjectContext> object() {
			return getRuleContexts(ObjectContext.class);
		}
		public ObjectContext object(int i) {
			return getRuleContext(ObjectContext.class,i);
		}
		public ObjectListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectListContext objectList() throws RecognitionException {
		ObjectListContext _localctx = new ObjectListContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_objectList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(848);
			object();
			setState(853);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(849);
				match(COMMA);
				setState(850);
				object();
				}
				}
				setState(855);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectContext extends ParserRuleContext {
		public GraphNodeContext graphNode() {
			return getRuleContext(GraphNodeContext.class,0);
		}
		public ObjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectContext object() throws RecognitionException {
		ObjectContext _localctx = new ObjectContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_object);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(856);
			graphNode();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesSameSubjectPathContext extends ParserRuleContext {
		public VarOrTermContext varOrTerm() {
			return getRuleContext(VarOrTermContext.class,0);
		}
		public PropertyListPathNotEmptyContext propertyListPathNotEmpty() {
			return getRuleContext(PropertyListPathNotEmptyContext.class,0);
		}
		public TriplesNodePathContext triplesNodePath() {
			return getRuleContext(TriplesNodePathContext.class,0);
		}
		public PropertyListPathContext propertyListPath() {
			return getRuleContext(PropertyListPathContext.class,0);
		}
		public TriplesSameSubjectPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesSameSubjectPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesSameSubjectPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesSameSubjectPathContext triplesSameSubjectPath() throws RecognitionException {
		TriplesSameSubjectPathContext _localctx = new TriplesSameSubjectPathContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_triplesSameSubjectPath);
		try {
			setState(864);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(858);
				varOrTerm();
				setState(859);
				propertyListPathNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(861);
				triplesNodePath();
				setState(862);
				propertyListPath();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyListPathContext extends ParserRuleContext {
		public PropertyListPathNotEmptyContext propertyListPathNotEmpty() {
			return getRuleContext(PropertyListPathNotEmptyContext.class,0);
		}
		public PropertyListPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyListPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathContext propertyListPath() throws RecognitionException {
		PropertyListPathContext _localctx = new PropertyListPathContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_propertyListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(867);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INVERSE - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (NEGATION - 115)))) != 0)) {
				{
				setState(866);
				propertyListPathNotEmpty();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyListPathNotEmptyContext extends ParserRuleContext {
		public ObjectListPathContext objectListPath() {
			return getRuleContext(ObjectListPathContext.class,0);
		}
		public VerbPathContext verbPath() {
			return getRuleContext(VerbPathContext.class,0);
		}
		public VerbSimpleContext verbSimple() {
			return getRuleContext(VerbSimpleContext.class,0);
		}
		public List<PropertyListPathNotEmptyListContext> propertyListPathNotEmptyList() {
			return getRuleContexts(PropertyListPathNotEmptyListContext.class);
		}
		public PropertyListPathNotEmptyListContext propertyListPathNotEmptyList(int i) {
			return getRuleContext(PropertyListPathNotEmptyListContext.class,i);
		}
		public PropertyListPathNotEmptyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyListPathNotEmpty; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPathNotEmpty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathNotEmptyContext propertyListPathNotEmpty() throws RecognitionException {
		PropertyListPathNotEmptyContext _localctx = new PropertyListPathNotEmptyContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_propertyListPathNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(869);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(870);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(873);
			objectListPath();
			setState(880);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(874);
				match(SEMICOLON);
				setState(876);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INVERSE - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (NEGATION - 115)))) != 0)) {
					{
					setState(875);
					propertyListPathNotEmptyList();
					}
				}

				}
				}
				setState(882);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyListPathNotEmptyListContext extends ParserRuleContext {
		public ObjectListPathContext objectListPath() {
			return getRuleContext(ObjectListPathContext.class,0);
		}
		public VerbPathContext verbPath() {
			return getRuleContext(VerbPathContext.class,0);
		}
		public VerbSimpleContext verbSimple() {
			return getRuleContext(VerbSimpleContext.class,0);
		}
		public PropertyListPathNotEmptyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyListPathNotEmptyList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPathNotEmptyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathNotEmptyListContext propertyListPathNotEmptyList() throws RecognitionException {
		PropertyListPathNotEmptyListContext _localctx = new PropertyListPathNotEmptyListContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_propertyListPathNotEmptyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(885);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(883);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(884);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(887);
			objectListPath();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VerbPathContext extends ParserRuleContext {
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public VerbPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_verbPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerbPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbPathContext verbPath() throws RecognitionException {
		VerbPathContext _localctx = new VerbPathContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_verbPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(889);
			path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VerbSimpleContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public VerbSimpleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_verbSimple; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerbSimple(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbSimpleContext verbSimple() throws RecognitionException {
		VerbSimpleContext _localctx = new VerbSimpleContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_verbSimple);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(891);
			var();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectListPathContext extends ParserRuleContext {
		public List<ObjectPathContext> objectPath() {
			return getRuleContexts(ObjectPathContext.class);
		}
		public ObjectPathContext objectPath(int i) {
			return getRuleContext(ObjectPathContext.class,i);
		}
		public ObjectListPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectListPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectListPathContext objectListPath() throws RecognitionException {
		ObjectListPathContext _localctx = new ObjectListPathContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_objectListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(893);
			objectPath();
			setState(898);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(894);
				match(COMMA);
				setState(895);
				objectPath();
				}
				}
				setState(900);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectPathContext extends ParserRuleContext {
		public GraphNodePathContext graphNodePath() {
			return getRuleContext(GraphNodePathContext.class,0);
		}
		public ObjectPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectPathContext objectPath() throws RecognitionException {
		ObjectPathContext _localctx = new ObjectPathContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_objectPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(901);
			graphNodePath();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathContext extends ParserRuleContext {
		public PathAlternativeContext pathAlternative() {
			return getRuleContext(PathAlternativeContext.class,0);
		}
		public PathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_path; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathContext path() throws RecognitionException {
		PathContext _localctx = new PathContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(903);
			pathAlternative();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathAlternativeContext extends ParserRuleContext {
		public List<PathSequenceContext> pathSequence() {
			return getRuleContexts(PathSequenceContext.class);
		}
		public PathSequenceContext pathSequence(int i) {
			return getRuleContext(PathSequenceContext.class,i);
		}
		public PathAlternativeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathAlternative; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathAlternative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathAlternativeContext pathAlternative() throws RecognitionException {
		PathAlternativeContext _localctx = new PathAlternativeContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_pathAlternative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(905);
			pathSequence();
			setState(910);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(906);
				match(PIPE);
				setState(907);
				pathSequence();
				}
				}
				setState(912);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathSequenceContext extends ParserRuleContext {
		public List<PathEltOrInverseContext> pathEltOrInverse() {
			return getRuleContexts(PathEltOrInverseContext.class);
		}
		public PathEltOrInverseContext pathEltOrInverse(int i) {
			return getRuleContext(PathEltOrInverseContext.class,i);
		}
		public PathSequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathSequence; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathSequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathSequenceContext pathSequence() throws RecognitionException {
		PathSequenceContext _localctx = new PathSequenceContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_pathSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(913);
			pathEltOrInverse();
			setState(918);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIVIDE) {
				{
				{
				setState(914);
				match(DIVIDE);
				setState(915);
				pathEltOrInverse();
				}
				}
				setState(920);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathEltContext extends ParserRuleContext {
		public PathPrimaryContext pathPrimary() {
			return getRuleContext(PathPrimaryContext.class,0);
		}
		public PathModContext pathMod() {
			return getRuleContext(PathModContext.class,0);
		}
		public PathEltContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathElt; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathElt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathEltContext pathElt() throws RecognitionException {
		PathEltContext _localctx = new PathEltContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_pathElt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(921);
			pathPrimary();
			setState(923);
			_la = _input.LA(1);
			if (((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (ASTERISK - 151)) | (1L << (QUESTION_MARK - 151)))) != 0)) {
				{
				setState(922);
				pathMod();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathEltOrInverseContext extends ParserRuleContext {
		public PathEltContext pathElt() {
			return getRuleContext(PathEltContext.class,0);
		}
		public TerminalNode INVERSE() { return getToken(SparqlParser.INVERSE, 0); }
		public PathEltOrInverseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathEltOrInverse; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathEltOrInverse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathEltOrInverseContext pathEltOrInverse() throws RecognitionException {
		PathEltOrInverseContext _localctx = new PathEltOrInverseContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_pathEltOrInverse);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(926);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(925);
				match(INVERSE);
				}
			}

			setState(928);
			pathElt();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathModContext extends ParserRuleContext {
		public Token op;
		public PathModContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathMod; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathMod(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathModContext pathMod() throws RecognitionException {
		PathModContext _localctx = new PathModContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_pathMod);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(930);
			((PathModContext)_localctx).op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (ASTERISK - 151)) | (1L << (QUESTION_MARK - 151)))) != 0)) ) {
				((PathModContext)_localctx).op = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathPrimaryContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode A() { return getToken(SparqlParser.A, 0); }
		public PathNegatedPropertySetContext pathNegatedPropertySet() {
			return getRuleContext(PathNegatedPropertySetContext.class,0);
		}
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public PathPrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathPrimary; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathPrimary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathPrimaryContext pathPrimary() throws RecognitionException {
		PathPrimaryContext _localctx = new PathPrimaryContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_pathPrimary);
		try {
			setState(940);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(932);
				iri();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(933);
				match(A);
				}
				break;
			case NEGATION:
				enterOuterAlt(_localctx, 3);
				{
				setState(934);
				match(NEGATION);
				setState(935);
				pathNegatedPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 4);
				{
				setState(936);
				match(OPEN_BRACE);
				setState(937);
				path();
				setState(938);
				match(CLOSE_BRACE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathNegatedPropertySetContext extends ParserRuleContext {
		public List<PathOneInPropertySetContext> pathOneInPropertySet() {
			return getRuleContexts(PathOneInPropertySetContext.class);
		}
		public PathOneInPropertySetContext pathOneInPropertySet(int i) {
			return getRuleContext(PathOneInPropertySetContext.class,i);
		}
		public PathNegatedPropertySetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathNegatedPropertySet; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathNegatedPropertySet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathNegatedPropertySetContext pathNegatedPropertySet() throws RecognitionException {
		PathNegatedPropertySetContext _localctx = new PathNegatedPropertySetContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_pathNegatedPropertySet);
		int _la;
		try {
			setState(955);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
				enterOuterAlt(_localctx, 1);
				{
				setState(942);
				pathOneInPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(943);
				match(OPEN_BRACE);
				setState(952);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INVERSE - 115)))) != 0)) {
					{
					setState(944);
					pathOneInPropertySet();
					setState(949);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==PIPE) {
						{
						{
						setState(945);
						match(PIPE);
						setState(946);
						pathOneInPropertySet();
						}
						}
						setState(951);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(954);
				match(CLOSE_BRACE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PathOneInPropertySetContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public TerminalNode A() { return getToken(SparqlParser.A, 0); }
		public TerminalNode INVERSE() { return getToken(SparqlParser.INVERSE, 0); }
		public PathOneInPropertySetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathOneInPropertySet; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathOneInPropertySet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathOneInPropertySetContext pathOneInPropertySet() throws RecognitionException {
		PathOneInPropertySetContext _localctx = new PathOneInPropertySetContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_pathOneInPropertySet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(958);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(957);
				match(INVERSE);
				}
			}

			setState(962);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				{
				setState(960);
				iri();
				}
				break;
			case A:
				{
				setState(961);
				match(A);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IntegerContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(SparqlParser.INTEGER, 0); }
		public IntegerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integer; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInteger(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntegerContext integer() throws RecognitionException {
		IntegerContext _localctx = new IntegerContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_integer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(964);
			match(INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesNodeContext extends ParserRuleContext {
		public CollectionContext collection() {
			return getRuleContext(CollectionContext.class,0);
		}
		public BlankNodePropertyListContext blankNodePropertyList() {
			return getRuleContext(BlankNodePropertyListContext.class,0);
		}
		public TriplesNodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesNode; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesNodeContext triplesNode() throws RecognitionException {
		TriplesNodeContext _localctx = new TriplesNodeContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_triplesNode);
		try {
			setState(968);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(966);
				collection();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(967);
				blankNodePropertyList();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlankNodePropertyListContext extends ParserRuleContext {
		public PropertyListNotEmptyContext propertyListNotEmpty() {
			return getRuleContext(PropertyListNotEmptyContext.class,0);
		}
		public BlankNodePropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blankNodePropertyList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNodePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodePropertyListContext blankNodePropertyList() throws RecognitionException {
		BlankNodePropertyListContext _localctx = new BlankNodePropertyListContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_blankNodePropertyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(970);
			match(OPEN_SQUARE_BRACKET);
			setState(971);
			propertyListNotEmpty();
			setState(972);
			match(CLOSE_SQUARE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TriplesNodePathContext extends ParserRuleContext {
		public CollectionPathContext collectionPath() {
			return getRuleContext(CollectionPathContext.class,0);
		}
		public BlankNodePropertyListPathContext blankNodePropertyListPath() {
			return getRuleContext(BlankNodePropertyListPathContext.class,0);
		}
		public TriplesNodePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_triplesNodePath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesNodePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesNodePathContext triplesNodePath() throws RecognitionException {
		TriplesNodePathContext _localctx = new TriplesNodePathContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_triplesNodePath);
		try {
			setState(976);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(974);
				collectionPath();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(975);
				blankNodePropertyListPath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlankNodePropertyListPathContext extends ParserRuleContext {
		public PropertyListPathNotEmptyContext propertyListPathNotEmpty() {
			return getRuleContext(PropertyListPathNotEmptyContext.class,0);
		}
		public BlankNodePropertyListPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blankNodePropertyListPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNodePropertyListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodePropertyListPathContext blankNodePropertyListPath() throws RecognitionException {
		BlankNodePropertyListPathContext _localctx = new BlankNodePropertyListPathContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_blankNodePropertyListPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(978);
			match(OPEN_SQUARE_BRACKET);
			setState(979);
			propertyListPathNotEmpty();
			setState(980);
			match(CLOSE_SQUARE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CollectionContext extends ParserRuleContext {
		public List<GraphNodeContext> graphNode() {
			return getRuleContexts(GraphNodeContext.class);
		}
		public GraphNodeContext graphNode(int i) {
			return getRuleContext(GraphNodeContext.class,i);
		}
		public CollectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collection; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCollection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CollectionContext collection() throws RecognitionException {
		CollectionContext _localctx = new CollectionContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_collection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(982);
			match(OPEN_BRACE);
			setState(984); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(983);
				graphNode();
				}
				}
				setState(986); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0) );
			setState(988);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CollectionPathContext extends ParserRuleContext {
		public List<GraphNodePathContext> graphNodePath() {
			return getRuleContexts(GraphNodePathContext.class);
		}
		public GraphNodePathContext graphNodePath(int i) {
			return getRuleContext(GraphNodePathContext.class,i);
		}
		public CollectionPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collectionPath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCollectionPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CollectionPathContext collectionPath() throws RecognitionException {
		CollectionPathContext _localctx = new CollectionPathContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_collectionPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(990);
			match(OPEN_BRACE);
			setState(992); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(991);
				graphNodePath();
				}
				}
				setState(994); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0) );
			setState(996);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphNodeContext extends ParserRuleContext {
		public VarOrTermContext varOrTerm() {
			return getRuleContext(VarOrTermContext.class,0);
		}
		public TriplesNodeContext triplesNode() {
			return getRuleContext(TriplesNodeContext.class,0);
		}
		public GraphNodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphNode; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphNodeContext graphNode() throws RecognitionException {
		GraphNodeContext _localctx = new GraphNodeContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_graphNode);
		try {
			setState(1000);
			switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(998);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(999);
				triplesNode();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphNodePathContext extends ParserRuleContext {
		public VarOrTermContext varOrTerm() {
			return getRuleContext(VarOrTermContext.class,0);
		}
		public TriplesNodePathContext triplesNodePath() {
			return getRuleContext(TriplesNodePathContext.class,0);
		}
		public GraphNodePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphNodePath; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphNodePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphNodePathContext graphNodePath() throws RecognitionException {
		GraphNodePathContext _localctx = new GraphNodePathContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_graphNodePath);
		try {
			setState(1004);
			switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1002);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1003);
				triplesNodePath();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarOrTermContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public GraphTermContext graphTerm() {
			return getRuleContext(GraphTermContext.class,0);
		}
		public VarOrTermContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varOrTerm; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVarOrTerm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarOrTermContext varOrTerm() throws RecognitionException {
		VarOrTermContext _localctx = new VarOrTermContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_varOrTerm);
		try {
			setState(1008);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1006);
				var();
				}
				break;
			case TRUE:
			case FALSE:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case BLANK_NODE_LABEL:
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
			case OPEN_BRACE:
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1007);
				graphTerm();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarOrIRIContext extends ParserRuleContext {
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public VarOrIRIContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varOrIRI; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVarOrIRI(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarOrIRIContext varOrIRI() throws RecognitionException {
		VarOrIRIContext _localctx = new VarOrIRIContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_varOrIRI);
		try {
			setState(1012);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1010);
				var();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1011);
				iri();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarContext extends ParserRuleContext {
		public TerminalNode VAR1() { return getToken(SparqlParser.VAR1, 0); }
		public TerminalNode VAR2() { return getToken(SparqlParser.VAR2, 0); }
		public VarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarContext var() throws RecognitionException {
		VarContext _localctx = new VarContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1014);
			_la = _input.LA(1);
			if ( !(_la==VAR1 || _la==VAR2) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GraphTermContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public RdfLiteralContext rdfLiteral() {
			return getRuleContext(RdfLiteralContext.class,0);
		}
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
		public BlankNodeContext blankNode() {
			return getRuleContext(BlankNodeContext.class,0);
		}
		public NilContext nil() {
			return getRuleContext(NilContext.class,0);
		}
		public GraphTermContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphTerm; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphTerm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphTermContext graphTerm() throws RecognitionException {
		GraphTermContext _localctx = new GraphTermContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_graphTerm);
		try {
			setState(1022);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1016);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1017);
				rdfLiteral();
				}
				break;
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1018);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(1019);
				booleanLiteral();
				}
				break;
			case BLANK_NODE_LABEL:
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 5);
				{
				setState(1020);
				blankNode();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1021);
				nil();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NilContext extends ParserRuleContext {
		public NilContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nil; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNil(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NilContext nil() throws RecognitionException {
		NilContext _localctx = new NilContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_nil);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1024);
			match(OPEN_BRACE);
			setState(1025);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	 
		public ExpressionContext() { }
		public void copyFrom(ExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class UnarySignedLiteralExpressionContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public UnaryLiteralExpressionContext unaryLiteralExpression() {
			return getRuleContext(UnaryLiteralExpressionContext.class,0);
		}
		public UnarySignedLiteralExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnarySignedLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ConditionalOrExpressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ConditionalOrExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConditionalOrExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AdditiveExpressionContext extends ExpressionContext {
		public Token op;
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public AdditiveExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAdditiveExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnaryAdditiveExpressionContext extends ExpressionContext {
		public Token op;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public UnaryAdditiveExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryAdditiveExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RelationalExpressionContext extends ExpressionContext {
		public Token op;
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public RelationalExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRelationalExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RelationalSetExpressionContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IN() { return getToken(SparqlParser.IN, 0); }
		public TerminalNode NOT() { return getToken(SparqlParser.NOT, 0); }
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public RelationalSetExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRelationalSetExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BaseExpressionContext extends ExpressionContext {
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public BaseExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBaseExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MultiplicativeExpressionContext extends ExpressionContext {
		public Token op;
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public MultiplicativeExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitMultiplicativeExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ConditionalAndExpressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ConditionalAndExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConditionalAndExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnaryNegationExpressionContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public UnaryNegationExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryNegationExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 218;
		enterRecursionRule(_localctx, 218, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1033);
			switch (_input.LA(1)) {
			case PLUS_SIGN:
			case MINUS_SIGN:
				{
				_localctx = new UnaryAdditiveExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1028);
				((UnaryAdditiveExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
					((UnaryAdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1029);
				expression(9);
				}
				break;
			case NEGATION:
				{
				_localctx = new UnaryNegationExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1030);
				match(NEGATION);
				setState(1031);
				expression(8);
				}
				break;
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case TRUE:
			case FALSE:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
			case OPEN_BRACE:
				{
				_localctx = new BaseExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1032);
				primaryExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(1064);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,120,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1062);
					switch ( getInterpreter().adaptivePredict(_input,119,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicativeExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1035);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1036);
						((MultiplicativeExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==ASTERISK || _la==DIVIDE) ) {
							((MultiplicativeExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1037);
						expression(8);
						}
						break;
					case 2:
						{
						_localctx = new AdditiveExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1038);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1039);
						((AdditiveExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
							((AdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1040);
						expression(7);
						}
						break;
					case 3:
						{
						_localctx = new RelationalExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1041);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1042);
						((RelationalExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (LESS_EQUAL - 137)) | (1L << (GREATER_EQUAL - 137)) | (1L << (NOT_EQUAL - 137)) | (1L << (EQUAL - 137)) | (1L << (LESS - 137)) | (1L << (GREATER - 137)))) != 0)) ) {
							((RelationalExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1043);
						expression(4);
						}
						break;
					case 4:
						{
						_localctx = new UnarySignedLiteralExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1044);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1045);
						unaryLiteralExpression();
						}
						break;
					case 5:
						{
						_localctx = new RelationalSetExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1046);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1048);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(1047);
							match(NOT);
							}
						}

						setState(1050);
						match(IN);
						setState(1051);
						match(OPEN_BRACE);
						setState(1053);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
							{
							setState(1052);
							expressionList();
							}
						}

						setState(1055);
						match(CLOSE_BRACE);
						}
						break;
					case 6:
						{
						_localctx = new ConditionalAndExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1056);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						{
						setState(1057);
						match(AND);
						setState(1058);
						expression(0);
						}
						}
						break;
					case 7:
						{
						_localctx = new ConditionalOrExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1059);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						{
						setState(1060);
						match(OR);
						setState(1061);
						expression(0);
						}
						}
						break;
					}
					} 
				}
				setState(1066);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,120,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class UnaryLiteralExpressionContext extends ParserRuleContext {
		public Token op;
		public NumericLiteralPositiveContext numericLiteralPositive() {
			return getRuleContext(NumericLiteralPositiveContext.class,0);
		}
		public NumericLiteralNegativeContext numericLiteralNegative() {
			return getRuleContext(NumericLiteralNegativeContext.class,0);
		}
		public UnaryExpressionContext unaryExpression() {
			return getRuleContext(UnaryExpressionContext.class,0);
		}
		public UnaryLiteralExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unaryLiteralExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryLiteralExpressionContext unaryLiteralExpression() throws RecognitionException {
		UnaryLiteralExpressionContext _localctx = new UnaryLiteralExpressionContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_unaryLiteralExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1069);
			switch (_input.LA(1)) {
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				{
				setState(1067);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				{
				setState(1068);
				numericLiteralNegative();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1073);
			switch ( getInterpreter().adaptivePredict(_input,122,_ctx) ) {
			case 1:
				{
				setState(1071);
				((UnaryLiteralExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASTERISK || _la==DIVIDE) ) {
					((UnaryLiteralExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1072);
				unaryExpression();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnaryExpressionContext extends ParserRuleContext {
		public Token op;
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public UnaryExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unaryExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryExpressionContext unaryExpression() throws RecognitionException {
		UnaryExpressionContext _localctx = new UnaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_unaryExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1076);
			_la = _input.LA(1);
			if (((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (MINUS_SIGN - 151)) | (1L << (NEGATION - 151)))) != 0)) {
				{
				setState(1075);
				((UnaryExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (MINUS_SIGN - 151)) | (1L << (NEGATION - 151)))) != 0)) ) {
					((UnaryExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(1078);
			primaryExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrimaryExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public BuiltInCallContext builtInCall() {
			return getRuleContext(BuiltInCallContext.class,0);
		}
		public IriRefOrFunctionContext iriRefOrFunction() {
			return getRuleContext(IriRefOrFunctionContext.class,0);
		}
		public RdfLiteralContext rdfLiteral() {
			return getRuleContext(RdfLiteralContext.class,0);
		}
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public PrimaryExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrimaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExpressionContext primaryExpression() throws RecognitionException {
		PrimaryExpressionContext _localctx = new PrimaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_primaryExpression);
		try {
			setState(1090);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1080);
				match(OPEN_BRACE);
				setState(1081);
				expression(0);
				setState(1082);
				match(CLOSE_BRACE);
				}
				break;
			case STR:
			case LANG:
			case LANGMATCHES:
			case DATATYPE:
			case BOUND:
			case SAMETERM:
			case ISIRI:
			case ISURI:
			case ISBLANK:
			case ISLITERAL:
			case REGEX:
			case SUBSTR:
			case IRI:
			case URI:
			case BNODE:
			case RAND:
			case ABS:
			case CEIL:
			case FLOOR:
			case ROUND:
			case CONCAT:
			case STRLEN:
			case UCASE:
			case LCASE:
			case ENCODE_FOR_URI:
			case CONTAINS:
			case STRSTARTS:
			case STRENDS:
			case STRBEFORE:
			case STRAFTER:
			case REPLACE:
			case YEAR:
			case MONTH:
			case DAY:
			case HOURS:
			case MINUTES:
			case SECONDS:
			case TIMEZONE:
			case TZ:
			case NOW:
			case UUID:
			case STRUUID:
			case MD5:
			case SHA1:
			case SHA256:
			case SHA384:
			case SHA512:
			case COALESCE:
			case IF:
			case STRLANG:
			case STRDT:
			case ISNUMERIC:
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
			case NOT:
			case EXISTS:
				enterOuterAlt(_localctx, 2);
				{
				setState(1084);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1085);
				iriRefOrFunction();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 4);
				{
				setState(1086);
				rdfLiteral();
				}
				break;
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 5);
				{
				setState(1087);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1088);
				booleanLiteral();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 7);
				{
				setState(1089);
				var();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BuiltInCallContext extends ParserRuleContext {
		public AggregateContext aggregate() {
			return getRuleContext(AggregateContext.class,0);
		}
		public TerminalNode STR() { return getToken(SparqlParser.STR, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode LANG() { return getToken(SparqlParser.LANG, 0); }
		public TerminalNode LANGMATCHES() { return getToken(SparqlParser.LANGMATCHES, 0); }
		public TerminalNode DATATYPE() { return getToken(SparqlParser.DATATYPE, 0); }
		public TerminalNode BOUND() { return getToken(SparqlParser.BOUND, 0); }
		public VarContext var() {
			return getRuleContext(VarContext.class,0);
		}
		public TerminalNode IRI() { return getToken(SparqlParser.IRI, 0); }
		public TerminalNode URI() { return getToken(SparqlParser.URI, 0); }
		public TerminalNode BNODE() { return getToken(SparqlParser.BNODE, 0); }
		public TerminalNode RAND() { return getToken(SparqlParser.RAND, 0); }
		public TerminalNode ABS() { return getToken(SparqlParser.ABS, 0); }
		public TerminalNode CEIL() { return getToken(SparqlParser.CEIL, 0); }
		public TerminalNode FLOOR() { return getToken(SparqlParser.FLOOR, 0); }
		public TerminalNode ROUND() { return getToken(SparqlParser.ROUND, 0); }
		public TerminalNode CONCAT() { return getToken(SparqlParser.CONCAT, 0); }
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public SubStringExpressionContext subStringExpression() {
			return getRuleContext(SubStringExpressionContext.class,0);
		}
		public TerminalNode STRLEN() { return getToken(SparqlParser.STRLEN, 0); }
		public StrReplaceExpressionContext strReplaceExpression() {
			return getRuleContext(StrReplaceExpressionContext.class,0);
		}
		public TerminalNode UCASE() { return getToken(SparqlParser.UCASE, 0); }
		public TerminalNode LCASE() { return getToken(SparqlParser.LCASE, 0); }
		public TerminalNode ENCODE_FOR_URI() { return getToken(SparqlParser.ENCODE_FOR_URI, 0); }
		public TerminalNode CONTAINS() { return getToken(SparqlParser.CONTAINS, 0); }
		public TerminalNode STRSTARTS() { return getToken(SparqlParser.STRSTARTS, 0); }
		public TerminalNode STRENDS() { return getToken(SparqlParser.STRENDS, 0); }
		public TerminalNode STRBEFORE() { return getToken(SparqlParser.STRBEFORE, 0); }
		public TerminalNode STRAFTER() { return getToken(SparqlParser.STRAFTER, 0); }
		public TerminalNode YEAR() { return getToken(SparqlParser.YEAR, 0); }
		public TerminalNode MONTH() { return getToken(SparqlParser.MONTH, 0); }
		public TerminalNode DAY() { return getToken(SparqlParser.DAY, 0); }
		public TerminalNode HOURS() { return getToken(SparqlParser.HOURS, 0); }
		public TerminalNode MINUTES() { return getToken(SparqlParser.MINUTES, 0); }
		public TerminalNode SECONDS() { return getToken(SparqlParser.SECONDS, 0); }
		public TerminalNode TIMEZONE() { return getToken(SparqlParser.TIMEZONE, 0); }
		public TerminalNode TZ() { return getToken(SparqlParser.TZ, 0); }
		public TerminalNode NOW() { return getToken(SparqlParser.NOW, 0); }
		public TerminalNode UUID() { return getToken(SparqlParser.UUID, 0); }
		public TerminalNode STRUUID() { return getToken(SparqlParser.STRUUID, 0); }
		public TerminalNode MD5() { return getToken(SparqlParser.MD5, 0); }
		public TerminalNode SHA1() { return getToken(SparqlParser.SHA1, 0); }
		public TerminalNode SHA256() { return getToken(SparqlParser.SHA256, 0); }
		public TerminalNode SHA384() { return getToken(SparqlParser.SHA384, 0); }
		public TerminalNode SHA512() { return getToken(SparqlParser.SHA512, 0); }
		public TerminalNode COALESCE() { return getToken(SparqlParser.COALESCE, 0); }
		public TerminalNode IF() { return getToken(SparqlParser.IF, 0); }
		public TerminalNode STRLANG() { return getToken(SparqlParser.STRLANG, 0); }
		public TerminalNode STRDT() { return getToken(SparqlParser.STRDT, 0); }
		public TerminalNode SAMETERM() { return getToken(SparqlParser.SAMETERM, 0); }
		public TerminalNode ISIRI() { return getToken(SparqlParser.ISIRI, 0); }
		public TerminalNode ISURI() { return getToken(SparqlParser.ISURI, 0); }
		public TerminalNode ISBLANK() { return getToken(SparqlParser.ISBLANK, 0); }
		public TerminalNode ISLITERAL() { return getToken(SparqlParser.ISLITERAL, 0); }
		public TerminalNode ISNUMERIC() { return getToken(SparqlParser.ISNUMERIC, 0); }
		public RegexExpressionContext regexExpression() {
			return getRuleContext(RegexExpressionContext.class,0);
		}
		public ExistsFunctionContext existsFunction() {
			return getRuleContext(ExistsFunctionContext.class,0);
		}
		public NotExistsFunctionContext notExistsFunction() {
			return getRuleContext(NotExistsFunctionContext.class,0);
		}
		public BuiltInCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_builtInCall; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBuiltInCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BuiltInCallContext builtInCall() throws RecognitionException {
		BuiltInCallContext _localctx = new BuiltInCallContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_builtInCall);
		int _la;
		try {
			setState(1360);
			switch (_input.LA(1)) {
			case COUNT:
			case SUM:
			case MIN:
			case MAX:
			case AVG:
			case SAMPLE:
			case GROUP_CONCAT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1092);
				aggregate();
				}
				break;
			case STR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1093);
				match(STR);
				setState(1094);
				match(OPEN_BRACE);
				setState(1095);
				expression(0);
				setState(1096);
				match(CLOSE_BRACE);
				}
				break;
			case LANG:
				enterOuterAlt(_localctx, 3);
				{
				setState(1098);
				match(LANG);
				setState(1099);
				match(OPEN_BRACE);
				setState(1100);
				expression(0);
				setState(1101);
				match(CLOSE_BRACE);
				}
				break;
			case LANGMATCHES:
				enterOuterAlt(_localctx, 4);
				{
				setState(1103);
				match(LANGMATCHES);
				setState(1104);
				match(OPEN_BRACE);
				setState(1105);
				expression(0);
				setState(1106);
				match(COMMA);
				setState(1107);
				expression(0);
				setState(1108);
				match(CLOSE_BRACE);
				}
				break;
			case DATATYPE:
				enterOuterAlt(_localctx, 5);
				{
				setState(1110);
				match(DATATYPE);
				setState(1111);
				match(OPEN_BRACE);
				setState(1112);
				expression(0);
				setState(1113);
				match(CLOSE_BRACE);
				}
				break;
			case BOUND:
				enterOuterAlt(_localctx, 6);
				{
				setState(1115);
				match(BOUND);
				setState(1116);
				match(OPEN_BRACE);
				setState(1117);
				var();
				setState(1118);
				match(CLOSE_BRACE);
				}
				break;
			case IRI:
				enterOuterAlt(_localctx, 7);
				{
				setState(1120);
				match(IRI);
				setState(1121);
				match(OPEN_BRACE);
				setState(1122);
				expression(0);
				setState(1123);
				match(CLOSE_BRACE);
				}
				break;
			case URI:
				enterOuterAlt(_localctx, 8);
				{
				setState(1125);
				match(URI);
				setState(1126);
				match(OPEN_BRACE);
				setState(1127);
				expression(0);
				setState(1128);
				match(CLOSE_BRACE);
				}
				break;
			case BNODE:
				enterOuterAlt(_localctx, 9);
				{
				setState(1130);
				match(BNODE);
				setState(1131);
				match(OPEN_BRACE);
				setState(1133);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1132);
					expression(0);
					}
				}

				setState(1135);
				match(CLOSE_BRACE);
				}
				break;
			case RAND:
				enterOuterAlt(_localctx, 10);
				{
				setState(1136);
				match(RAND);
				setState(1137);
				match(OPEN_BRACE);
				setState(1138);
				match(CLOSE_BRACE);
				}
				break;
			case ABS:
				enterOuterAlt(_localctx, 11);
				{
				setState(1139);
				match(ABS);
				setState(1140);
				match(OPEN_BRACE);
				setState(1141);
				expression(0);
				setState(1142);
				match(CLOSE_BRACE);
				}
				break;
			case CEIL:
				enterOuterAlt(_localctx, 12);
				{
				setState(1144);
				match(CEIL);
				setState(1145);
				match(OPEN_BRACE);
				setState(1146);
				expression(0);
				setState(1147);
				match(CLOSE_BRACE);
				}
				break;
			case FLOOR:
				enterOuterAlt(_localctx, 13);
				{
				setState(1149);
				match(FLOOR);
				setState(1150);
				match(OPEN_BRACE);
				setState(1151);
				expression(0);
				setState(1152);
				match(CLOSE_BRACE);
				}
				break;
			case ROUND:
				enterOuterAlt(_localctx, 14);
				{
				setState(1154);
				match(ROUND);
				setState(1155);
				match(OPEN_BRACE);
				setState(1156);
				expression(0);
				setState(1157);
				match(CLOSE_BRACE);
				}
				break;
			case CONCAT:
				enterOuterAlt(_localctx, 15);
				{
				setState(1159);
				match(CONCAT);
				setState(1160);
				match(OPEN_BRACE);
				setState(1162);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1161);
					expressionList();
					}
				}

				setState(1164);
				match(CLOSE_BRACE);
				}
				break;
			case SUBSTR:
				enterOuterAlt(_localctx, 16);
				{
				setState(1165);
				subStringExpression();
				}
				break;
			case STRLEN:
				enterOuterAlt(_localctx, 17);
				{
				setState(1166);
				match(STRLEN);
				setState(1167);
				match(OPEN_BRACE);
				setState(1168);
				expression(0);
				setState(1169);
				match(CLOSE_BRACE);
				}
				break;
			case REPLACE:
				enterOuterAlt(_localctx, 18);
				{
				setState(1171);
				strReplaceExpression();
				}
				break;
			case UCASE:
				enterOuterAlt(_localctx, 19);
				{
				setState(1172);
				match(UCASE);
				setState(1173);
				match(OPEN_BRACE);
				setState(1174);
				expression(0);
				setState(1175);
				match(CLOSE_BRACE);
				}
				break;
			case LCASE:
				enterOuterAlt(_localctx, 20);
				{
				setState(1177);
				match(LCASE);
				setState(1178);
				match(OPEN_BRACE);
				setState(1179);
				expression(0);
				setState(1180);
				match(CLOSE_BRACE);
				}
				break;
			case ENCODE_FOR_URI:
				enterOuterAlt(_localctx, 21);
				{
				setState(1182);
				match(ENCODE_FOR_URI);
				setState(1183);
				match(OPEN_BRACE);
				setState(1184);
				expression(0);
				setState(1185);
				match(CLOSE_BRACE);
				}
				break;
			case CONTAINS:
				enterOuterAlt(_localctx, 22);
				{
				setState(1187);
				match(CONTAINS);
				setState(1188);
				match(OPEN_BRACE);
				setState(1189);
				expression(0);
				setState(1190);
				match(COMMA);
				setState(1191);
				expression(0);
				setState(1192);
				match(CLOSE_BRACE);
				}
				break;
			case STRSTARTS:
				enterOuterAlt(_localctx, 23);
				{
				setState(1194);
				match(STRSTARTS);
				setState(1195);
				match(OPEN_BRACE);
				setState(1196);
				expression(0);
				setState(1197);
				match(COMMA);
				setState(1198);
				expression(0);
				setState(1199);
				match(CLOSE_BRACE);
				}
				break;
			case STRENDS:
				enterOuterAlt(_localctx, 24);
				{
				setState(1201);
				match(STRENDS);
				setState(1202);
				match(OPEN_BRACE);
				setState(1203);
				expression(0);
				setState(1204);
				match(COMMA);
				setState(1205);
				expression(0);
				setState(1206);
				match(CLOSE_BRACE);
				}
				break;
			case STRBEFORE:
				enterOuterAlt(_localctx, 25);
				{
				setState(1208);
				match(STRBEFORE);
				setState(1209);
				match(OPEN_BRACE);
				setState(1210);
				expression(0);
				setState(1211);
				match(COMMA);
				setState(1212);
				expression(0);
				setState(1213);
				match(CLOSE_BRACE);
				}
				break;
			case STRAFTER:
				enterOuterAlt(_localctx, 26);
				{
				setState(1215);
				match(STRAFTER);
				setState(1216);
				match(OPEN_BRACE);
				setState(1217);
				expression(0);
				setState(1218);
				match(COMMA);
				setState(1219);
				expression(0);
				setState(1220);
				match(CLOSE_BRACE);
				}
				break;
			case YEAR:
				enterOuterAlt(_localctx, 27);
				{
				setState(1222);
				match(YEAR);
				setState(1223);
				match(OPEN_BRACE);
				setState(1224);
				expression(0);
				setState(1225);
				match(CLOSE_BRACE);
				}
				break;
			case MONTH:
				enterOuterAlt(_localctx, 28);
				{
				setState(1227);
				match(MONTH);
				setState(1228);
				match(OPEN_BRACE);
				setState(1229);
				expression(0);
				setState(1230);
				match(CLOSE_BRACE);
				}
				break;
			case DAY:
				enterOuterAlt(_localctx, 29);
				{
				setState(1232);
				match(DAY);
				setState(1233);
				match(OPEN_BRACE);
				setState(1234);
				expression(0);
				setState(1235);
				match(CLOSE_BRACE);
				}
				break;
			case HOURS:
				enterOuterAlt(_localctx, 30);
				{
				setState(1237);
				match(HOURS);
				setState(1238);
				match(OPEN_BRACE);
				setState(1239);
				expression(0);
				setState(1240);
				match(CLOSE_BRACE);
				}
				break;
			case MINUTES:
				enterOuterAlt(_localctx, 31);
				{
				setState(1242);
				match(MINUTES);
				setState(1243);
				match(OPEN_BRACE);
				setState(1244);
				expression(0);
				setState(1245);
				match(CLOSE_BRACE);
				}
				break;
			case SECONDS:
				enterOuterAlt(_localctx, 32);
				{
				setState(1247);
				match(SECONDS);
				setState(1248);
				match(OPEN_BRACE);
				setState(1249);
				expression(0);
				setState(1250);
				match(CLOSE_BRACE);
				}
				break;
			case TIMEZONE:
				enterOuterAlt(_localctx, 33);
				{
				setState(1252);
				match(TIMEZONE);
				setState(1253);
				match(OPEN_BRACE);
				setState(1254);
				expression(0);
				setState(1255);
				match(CLOSE_BRACE);
				}
				break;
			case TZ:
				enterOuterAlt(_localctx, 34);
				{
				setState(1257);
				match(TZ);
				setState(1258);
				match(OPEN_BRACE);
				setState(1259);
				expression(0);
				setState(1260);
				match(CLOSE_BRACE);
				}
				break;
			case NOW:
				enterOuterAlt(_localctx, 35);
				{
				setState(1262);
				match(NOW);
				setState(1263);
				match(OPEN_BRACE);
				setState(1264);
				match(CLOSE_BRACE);
				}
				break;
			case UUID:
				enterOuterAlt(_localctx, 36);
				{
				setState(1265);
				match(UUID);
				setState(1266);
				match(OPEN_BRACE);
				setState(1267);
				match(CLOSE_BRACE);
				}
				break;
			case STRUUID:
				enterOuterAlt(_localctx, 37);
				{
				setState(1268);
				match(STRUUID);
				setState(1269);
				match(OPEN_BRACE);
				setState(1270);
				match(CLOSE_BRACE);
				}
				break;
			case MD5:
				enterOuterAlt(_localctx, 38);
				{
				setState(1271);
				match(MD5);
				setState(1272);
				match(OPEN_BRACE);
				setState(1273);
				expression(0);
				setState(1274);
				match(CLOSE_BRACE);
				}
				break;
			case SHA1:
				enterOuterAlt(_localctx, 39);
				{
				setState(1276);
				match(SHA1);
				setState(1277);
				match(OPEN_BRACE);
				setState(1278);
				expression(0);
				setState(1279);
				match(CLOSE_BRACE);
				}
				break;
			case SHA256:
				enterOuterAlt(_localctx, 40);
				{
				setState(1281);
				match(SHA256);
				setState(1282);
				match(OPEN_BRACE);
				setState(1283);
				expression(0);
				setState(1284);
				match(CLOSE_BRACE);
				}
				break;
			case SHA384:
				enterOuterAlt(_localctx, 41);
				{
				setState(1286);
				match(SHA384);
				setState(1287);
				match(OPEN_BRACE);
				setState(1288);
				expression(0);
				setState(1289);
				match(CLOSE_BRACE);
				}
				break;
			case SHA512:
				enterOuterAlt(_localctx, 42);
				{
				setState(1291);
				match(SHA512);
				setState(1292);
				match(OPEN_BRACE);
				setState(1293);
				expression(0);
				setState(1294);
				match(CLOSE_BRACE);
				}
				break;
			case COALESCE:
				enterOuterAlt(_localctx, 43);
				{
				setState(1296);
				match(COALESCE);
				setState(1297);
				match(OPEN_BRACE);
				setState(1299);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1298);
					expressionList();
					}
				}

				setState(1301);
				match(CLOSE_BRACE);
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 44);
				{
				setState(1302);
				match(IF);
				setState(1303);
				match(OPEN_BRACE);
				setState(1304);
				expression(0);
				setState(1305);
				match(COMMA);
				setState(1306);
				expression(0);
				setState(1307);
				match(COMMA);
				setState(1308);
				expression(0);
				setState(1309);
				match(CLOSE_BRACE);
				}
				break;
			case STRLANG:
				enterOuterAlt(_localctx, 45);
				{
				setState(1311);
				match(STRLANG);
				setState(1312);
				match(OPEN_BRACE);
				setState(1313);
				expression(0);
				setState(1314);
				match(COMMA);
				setState(1315);
				expression(0);
				setState(1316);
				match(CLOSE_BRACE);
				}
				break;
			case STRDT:
				enterOuterAlt(_localctx, 46);
				{
				setState(1318);
				match(STRDT);
				setState(1319);
				match(OPEN_BRACE);
				setState(1320);
				expression(0);
				setState(1321);
				match(COMMA);
				setState(1322);
				expression(0);
				setState(1323);
				match(CLOSE_BRACE);
				}
				break;
			case SAMETERM:
				enterOuterAlt(_localctx, 47);
				{
				setState(1325);
				match(SAMETERM);
				setState(1326);
				match(OPEN_BRACE);
				setState(1327);
				expression(0);
				setState(1328);
				match(COMMA);
				setState(1329);
				expression(0);
				setState(1330);
				match(CLOSE_BRACE);
				}
				break;
			case ISIRI:
				enterOuterAlt(_localctx, 48);
				{
				setState(1332);
				match(ISIRI);
				setState(1333);
				match(OPEN_BRACE);
				setState(1334);
				expression(0);
				setState(1335);
				match(CLOSE_BRACE);
				}
				break;
			case ISURI:
				enterOuterAlt(_localctx, 49);
				{
				setState(1337);
				match(ISURI);
				setState(1338);
				match(OPEN_BRACE);
				setState(1339);
				expression(0);
				setState(1340);
				match(CLOSE_BRACE);
				}
				break;
			case ISBLANK:
				enterOuterAlt(_localctx, 50);
				{
				setState(1342);
				match(ISBLANK);
				setState(1343);
				match(OPEN_BRACE);
				setState(1344);
				expression(0);
				setState(1345);
				match(CLOSE_BRACE);
				}
				break;
			case ISLITERAL:
				enterOuterAlt(_localctx, 51);
				{
				setState(1347);
				match(ISLITERAL);
				setState(1348);
				match(OPEN_BRACE);
				setState(1349);
				expression(0);
				setState(1350);
				match(CLOSE_BRACE);
				}
				break;
			case ISNUMERIC:
				enterOuterAlt(_localctx, 52);
				{
				setState(1352);
				match(ISNUMERIC);
				setState(1353);
				match(OPEN_BRACE);
				setState(1354);
				expression(0);
				setState(1355);
				match(CLOSE_BRACE);
				}
				break;
			case REGEX:
				enterOuterAlt(_localctx, 53);
				{
				setState(1357);
				regexExpression();
				}
				break;
			case EXISTS:
				enterOuterAlt(_localctx, 54);
				{
				setState(1358);
				existsFunction();
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 55);
				{
				setState(1359);
				notExistsFunction();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RegexExpressionContext extends ParserRuleContext {
		public TerminalNode REGEX() { return getToken(SparqlParser.REGEX, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public RegexExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regexExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRegexExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegexExpressionContext regexExpression() throws RecognitionException {
		RegexExpressionContext _localctx = new RegexExpressionContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_regexExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1362);
			match(REGEX);
			setState(1363);
			match(OPEN_BRACE);
			setState(1364);
			expression(0);
			setState(1365);
			match(COMMA);
			setState(1366);
			expression(0);
			setState(1369);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1367);
				match(COMMA);
				setState(1368);
				expression(0);
				}
			}

			setState(1371);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubStringExpressionContext extends ParserRuleContext {
		public TerminalNode SUBSTR() { return getToken(SparqlParser.SUBSTR, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public SubStringExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subStringExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSubStringExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubStringExpressionContext subStringExpression() throws RecognitionException {
		SubStringExpressionContext _localctx = new SubStringExpressionContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_subStringExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1373);
			match(SUBSTR);
			setState(1374);
			match(OPEN_BRACE);
			setState(1375);
			expression(0);
			setState(1376);
			match(COMMA);
			setState(1377);
			expression(0);
			setState(1380);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1378);
				match(COMMA);
				setState(1379);
				expression(0);
				}
			}

			setState(1382);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StrReplaceExpressionContext extends ParserRuleContext {
		public TerminalNode REPLACE() { return getToken(SparqlParser.REPLACE, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public StrReplaceExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_strReplaceExpression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitStrReplaceExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrReplaceExpressionContext strReplaceExpression() throws RecognitionException {
		StrReplaceExpressionContext _localctx = new StrReplaceExpressionContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_strReplaceExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1384);
			match(REPLACE);
			setState(1385);
			match(OPEN_BRACE);
			setState(1386);
			expression(0);
			setState(1387);
			match(COMMA);
			setState(1388);
			expression(0);
			setState(1389);
			match(COMMA);
			setState(1390);
			expression(0);
			setState(1393);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1391);
				match(COMMA);
				setState(1392);
				expression(0);
				}
			}

			setState(1395);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExistsFunctionContext extends ParserRuleContext {
		public TerminalNode EXISTS() { return getToken(SparqlParser.EXISTS, 0); }
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public ExistsFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_existsFunction; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitExistsFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExistsFunctionContext existsFunction() throws RecognitionException {
		ExistsFunctionContext _localctx = new ExistsFunctionContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_existsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1397);
			match(EXISTS);
			setState(1398);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotExistsFunctionContext extends ParserRuleContext {
		public TerminalNode NOT() { return getToken(SparqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(SparqlParser.EXISTS, 0); }
		public GroupGraphPatternContext groupGraphPattern() {
			return getRuleContext(GroupGraphPatternContext.class,0);
		}
		public NotExistsFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notExistsFunction; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNotExistsFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotExistsFunctionContext notExistsFunction() throws RecognitionException {
		NotExistsFunctionContext _localctx = new NotExistsFunctionContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_notExistsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1400);
			match(NOT);
			setState(1401);
			match(EXISTS);
			setState(1402);
			groupGraphPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AggregateContext extends ParserRuleContext {
		public TerminalNode COUNT() { return getToken(SparqlParser.COUNT, 0); }
		public TerminalNode ASTERISK() { return getToken(SparqlParser.ASTERISK, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode DISTINCT() { return getToken(SparqlParser.DISTINCT, 0); }
		public TerminalNode SUM() { return getToken(SparqlParser.SUM, 0); }
		public TerminalNode MIN() { return getToken(SparqlParser.MIN, 0); }
		public TerminalNode MAX() { return getToken(SparqlParser.MAX, 0); }
		public TerminalNode AVG() { return getToken(SparqlParser.AVG, 0); }
		public TerminalNode SAMPLE() { return getToken(SparqlParser.SAMPLE, 0); }
		public TerminalNode GROUP_CONCAT() { return getToken(SparqlParser.GROUP_CONCAT, 0); }
		public TerminalNode SEPARATOR() { return getToken(SparqlParser.SEPARATOR, 0); }
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public AggregateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAggregate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregateContext aggregate() throws RecognitionException {
		AggregateContext _localctx = new AggregateContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_aggregate);
		int _la;
		try {
			setState(1468);
			switch (_input.LA(1)) {
			case COUNT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1404);
				match(COUNT);
				setState(1405);
				match(OPEN_BRACE);
				setState(1407);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1406);
					match(DISTINCT);
					}
				}

				setState(1411);
				switch (_input.LA(1)) {
				case ASTERISK:
					{
					setState(1409);
					match(ASTERISK);
					}
					break;
				case STR:
				case LANG:
				case LANGMATCHES:
				case DATATYPE:
				case BOUND:
				case SAMETERM:
				case ISIRI:
				case ISURI:
				case ISBLANK:
				case ISLITERAL:
				case REGEX:
				case SUBSTR:
				case TRUE:
				case FALSE:
				case IRI:
				case URI:
				case BNODE:
				case RAND:
				case ABS:
				case CEIL:
				case FLOOR:
				case ROUND:
				case CONCAT:
				case STRLEN:
				case UCASE:
				case LCASE:
				case ENCODE_FOR_URI:
				case CONTAINS:
				case STRSTARTS:
				case STRENDS:
				case STRBEFORE:
				case STRAFTER:
				case REPLACE:
				case YEAR:
				case MONTH:
				case DAY:
				case HOURS:
				case MINUTES:
				case SECONDS:
				case TIMEZONE:
				case TZ:
				case NOW:
				case UUID:
				case STRUUID:
				case MD5:
				case SHA1:
				case SHA256:
				case SHA384:
				case SHA512:
				case COALESCE:
				case IF:
				case STRLANG:
				case STRDT:
				case ISNUMERIC:
				case COUNT:
				case SUM:
				case MIN:
				case MAX:
				case AVG:
				case SAMPLE:
				case GROUP_CONCAT:
				case NOT:
				case EXISTS:
				case IRIREF:
				case PNAME_NS:
				case PNAME_LN:
				case VAR1:
				case VAR2:
				case INTEGER:
				case DECIMAL:
				case DOUBLE:
				case INTEGER_POSITIVE:
				case DECIMAL_POSITIVE:
				case DOUBLE_POSITIVE:
				case INTEGER_NEGATIVE:
				case DECIMAL_NEGATIVE:
				case DOUBLE_NEGATIVE:
				case STRING_LITERAL1:
				case STRING_LITERAL2:
				case STRING_LITERAL_LONG1:
				case STRING_LITERAL_LONG2:
				case OPEN_BRACE:
				case PLUS_SIGN:
				case MINUS_SIGN:
				case NEGATION:
					{
					setState(1410);
					expression(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1413);
				match(CLOSE_BRACE);
				}
				break;
			case SUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(1414);
				match(SUM);
				setState(1415);
				match(OPEN_BRACE);
				setState(1417);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1416);
					match(DISTINCT);
					}
				}

				setState(1419);
				expression(0);
				setState(1420);
				match(CLOSE_BRACE);
				}
				break;
			case MIN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1422);
				match(MIN);
				setState(1423);
				match(OPEN_BRACE);
				setState(1425);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1424);
					match(DISTINCT);
					}
				}

				setState(1427);
				expression(0);
				setState(1428);
				match(CLOSE_BRACE);
				}
				break;
			case MAX:
				enterOuterAlt(_localctx, 4);
				{
				setState(1430);
				match(MAX);
				setState(1431);
				match(OPEN_BRACE);
				setState(1433);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1432);
					match(DISTINCT);
					}
				}

				setState(1435);
				expression(0);
				setState(1436);
				match(CLOSE_BRACE);
				}
				break;
			case AVG:
				enterOuterAlt(_localctx, 5);
				{
				setState(1438);
				match(AVG);
				setState(1439);
				match(OPEN_BRACE);
				setState(1441);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1440);
					match(DISTINCT);
					}
				}

				setState(1443);
				expression(0);
				setState(1444);
				match(CLOSE_BRACE);
				}
				break;
			case SAMPLE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1446);
				match(SAMPLE);
				setState(1447);
				match(OPEN_BRACE);
				setState(1449);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1448);
					match(DISTINCT);
					}
				}

				setState(1451);
				expression(0);
				setState(1452);
				match(CLOSE_BRACE);
				}
				break;
			case GROUP_CONCAT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1454);
				match(GROUP_CONCAT);
				setState(1455);
				match(OPEN_BRACE);
				setState(1457);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1456);
					match(DISTINCT);
					}
				}

				setState(1459);
				expression(0);
				setState(1464);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(1460);
					match(SEMICOLON);
					setState(1461);
					match(SEPARATOR);
					setState(1462);
					match(EQUAL);
					setState(1463);
					string();
					}
				}

				setState(1466);
				match(CLOSE_BRACE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IriRefOrFunctionContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public ArgListContext argList() {
			return getRuleContext(ArgListContext.class,0);
		}
		public IriRefOrFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_iriRefOrFunction; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitIriRefOrFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IriRefOrFunctionContext iriRefOrFunction() throws RecognitionException {
		IriRefOrFunctionContext _localctx = new IriRefOrFunctionContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_iriRefOrFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1470);
			iri();
			setState(1472);
			switch ( getInterpreter().adaptivePredict(_input,142,_ctx) ) {
			case 1:
				{
				setState(1471);
				argList();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RdfLiteralContext extends ParserRuleContext {
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TerminalNode LANGTAG() { return getToken(SparqlParser.LANGTAG, 0); }
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public RdfLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rdfLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRdfLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RdfLiteralContext rdfLiteral() throws RecognitionException {
		RdfLiteralContext _localctx = new RdfLiteralContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_rdfLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1474);
			string();
			setState(1478);
			switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
			case 1:
				{
				setState(1475);
				match(LANGTAG);
				}
				break;
			case 2:
				{
				{
				setState(1476);
				match(REFERENCE);
				setState(1477);
				iri();
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumericLiteralContext extends ParserRuleContext {
		public NumericLiteralUnsignedContext numericLiteralUnsigned() {
			return getRuleContext(NumericLiteralUnsignedContext.class,0);
		}
		public NumericLiteralPositiveContext numericLiteralPositive() {
			return getRuleContext(NumericLiteralPositiveContext.class,0);
		}
		public NumericLiteralNegativeContext numericLiteralNegative() {
			return getRuleContext(NumericLiteralNegativeContext.class,0);
		}
		public NumericLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralContext numericLiteral() throws RecognitionException {
		NumericLiteralContext _localctx = new NumericLiteralContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_numericLiteral);
		try {
			setState(1483);
			switch (_input.LA(1)) {
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1480);
				numericLiteralUnsigned();
				}
				break;
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1481);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1482);
				numericLiteralNegative();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumericLiteralUnsignedContext extends ParserRuleContext {
		public TerminalNode INTEGER() { return getToken(SparqlParser.INTEGER, 0); }
		public TerminalNode DECIMAL() { return getToken(SparqlParser.DECIMAL, 0); }
		public TerminalNode DOUBLE() { return getToken(SparqlParser.DOUBLE, 0); }
		public NumericLiteralUnsignedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteralUnsigned; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralUnsigned(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralUnsignedContext numericLiteralUnsigned() throws RecognitionException {
		NumericLiteralUnsignedContext _localctx = new NumericLiteralUnsignedContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_numericLiteralUnsigned);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1485);
			_la = _input.LA(1);
			if ( !(((((_la - 122)) & ~0x3f) == 0 && ((1L << (_la - 122)) & ((1L << (INTEGER - 122)) | (1L << (DECIMAL - 122)) | (1L << (DOUBLE - 122)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumericLiteralPositiveContext extends ParserRuleContext {
		public TerminalNode INTEGER_POSITIVE() { return getToken(SparqlParser.INTEGER_POSITIVE, 0); }
		public TerminalNode DECIMAL_POSITIVE() { return getToken(SparqlParser.DECIMAL_POSITIVE, 0); }
		public TerminalNode DOUBLE_POSITIVE() { return getToken(SparqlParser.DOUBLE_POSITIVE, 0); }
		public NumericLiteralPositiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteralPositive; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralPositive(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralPositiveContext numericLiteralPositive() throws RecognitionException {
		NumericLiteralPositiveContext _localctx = new NumericLiteralPositiveContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_numericLiteralPositive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1487);
			_la = _input.LA(1);
			if ( !(((((_la - 125)) & ~0x3f) == 0 && ((1L << (_la - 125)) & ((1L << (INTEGER_POSITIVE - 125)) | (1L << (DECIMAL_POSITIVE - 125)) | (1L << (DOUBLE_POSITIVE - 125)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumericLiteralNegativeContext extends ParserRuleContext {
		public TerminalNode INTEGER_NEGATIVE() { return getToken(SparqlParser.INTEGER_NEGATIVE, 0); }
		public TerminalNode DECIMAL_NEGATIVE() { return getToken(SparqlParser.DECIMAL_NEGATIVE, 0); }
		public TerminalNode DOUBLE_NEGATIVE() { return getToken(SparqlParser.DOUBLE_NEGATIVE, 0); }
		public NumericLiteralNegativeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteralNegative; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralNegative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralNegativeContext numericLiteralNegative() throws RecognitionException {
		NumericLiteralNegativeContext _localctx = new NumericLiteralNegativeContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_numericLiteralNegative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1489);
			_la = _input.LA(1);
			if ( !(((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BooleanLiteralContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(SparqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(SparqlParser.FALSE, 0); }
		public BooleanLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanLiteralContext booleanLiteral() throws RecognitionException {
		BooleanLiteralContext _localctx = new BooleanLiteralContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_booleanLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1491);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL1() { return getToken(SparqlParser.STRING_LITERAL1, 0); }
		public TerminalNode STRING_LITERAL2() { return getToken(SparqlParser.STRING_LITERAL2, 0); }
		public TerminalNode STRING_LITERAL_LONG1() { return getToken(SparqlParser.STRING_LITERAL_LONG1, 0); }
		public TerminalNode STRING_LITERAL_LONG2() { return getToken(SparqlParser.STRING_LITERAL_LONG2, 0); }
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1493);
			_la = _input.LA(1);
			if ( !(((((_la - 131)) & ~0x3f) == 0 && ((1L << (_la - 131)) & ((1L << (STRING_LITERAL1 - 131)) | (1L << (STRING_LITERAL2 - 131)) | (1L << (STRING_LITERAL_LONG1 - 131)) | (1L << (STRING_LITERAL_LONG2 - 131)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IriContext extends ParserRuleContext {
		public TerminalNode IRIREF() { return getToken(SparqlParser.IRIREF, 0); }
		public PrefixedNameContext prefixedName() {
			return getRuleContext(PrefixedNameContext.class,0);
		}
		public IriContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_iri; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitIri(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IriContext iri() throws RecognitionException {
		IriContext _localctx = new IriContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_iri);
		try {
			setState(1497);
			switch (_input.LA(1)) {
			case IRIREF:
				enterOuterAlt(_localctx, 1);
				{
				setState(1495);
				match(IRIREF);
				}
				break;
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1496);
				prefixedName();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrefixedNameContext extends ParserRuleContext {
		public TerminalNode PNAME_LN() { return getToken(SparqlParser.PNAME_LN, 0); }
		public TerminalNode PNAME_NS() { return getToken(SparqlParser.PNAME_NS, 0); }
		public PrefixedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prefixedName; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrefixedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrefixedNameContext prefixedName() throws RecognitionException {
		PrefixedNameContext _localctx = new PrefixedNameContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_prefixedName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1499);
			_la = _input.LA(1);
			if ( !(_la==PNAME_NS || _la==PNAME_LN) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlankNodeContext extends ParserRuleContext {
		public TerminalNode BLANK_NODE_LABEL() { return getToken(SparqlParser.BLANK_NODE_LABEL, 0); }
		public AnonContext anon() {
			return getRuleContext(AnonContext.class,0);
		}
		public BlankNodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blankNode; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodeContext blankNode() throws RecognitionException {
		BlankNodeContext _localctx = new BlankNodeContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_blankNode);
		try {
			setState(1503);
			switch (_input.LA(1)) {
			case BLANK_NODE_LABEL:
				enterOuterAlt(_localctx, 1);
				{
				setState(1501);
				match(BLANK_NODE_LABEL);
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1502);
				anon();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnonContext extends ParserRuleContext {
		public AnonContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anon; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAnon(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnonContext anon() throws RecognitionException {
		AnonContext _localctx = new AnonContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_anon);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1505);
			match(OPEN_SQUARE_BRACKET);
			setState(1506);
			match(CLOSE_SQUARE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 109:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 7);
		case 1:
			return precpred(_ctx, 6);
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 5);
		case 4:
			return precpred(_ctx, 4);
		case 5:
			return precpred(_ctx, 2);
		case 6:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00a5\u05e7\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv\4"+
		"w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t\u0080"+
		"\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084\4\u0085"+
		"\t\u0085\3\2\3\2\3\2\3\2\3\2\5\2\u0110\n\2\3\2\3\2\3\2\3\2\5\2\u0116\n"+
		"\2\3\2\5\2\u0119\n\2\3\3\3\3\7\3\u011d\n\3\f\3\16\3\u0120\13\3\3\4\3\4"+
		"\3\4\3\5\3\5\3\5\3\5\3\6\3\6\7\6\u012b\n\6\f\6\16\6\u012e\13\6\3\6\3\6"+
		"\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b\5\b\u013a\n\b\3\b\6\b\u013d\n\b\r\b\16"+
		"\b\u013e\3\b\5\b\u0142\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u014b\n\t\3"+
		"\n\3\n\3\n\7\n\u0150\n\n\f\n\16\n\u0153\13\n\3\n\3\n\3\n\3\n\7\n\u0159"+
		"\n\n\f\n\16\n\u015c\13\n\3\n\3\n\3\n\3\n\5\n\u0162\n\n\3\13\3\13\6\13"+
		"\u0166\n\13\r\13\16\13\u0167\3\13\5\13\u016b\n\13\3\13\7\13\u016e\n\13"+
		"\f\13\16\13\u0171\13\13\3\13\5\13\u0174\n\13\3\13\3\13\3\f\3\f\7\f\u017a"+
		"\n\f\f\f\16\f\u017d\13\f\3\f\3\f\5\f\u0181\n\f\3\f\5\f\u0184\n\f\3\r\3"+
		"\r\5\r\u0188\n\r\3\r\3\r\3\16\5\16\u018d\n\16\3\16\3\16\3\17\5\17\u0192"+
		"\n\17\3\17\5\17\u0195\n\17\3\17\5\17\u0198\n\17\3\17\5\17\u019b\n\17\3"+
		"\20\3\20\3\20\6\20\u01a0\n\20\r\20\16\20\u01a1\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\5\21\u01aa\n\21\3\21\3\21\3\21\5\21\u01af\n\21\3\22\3\22\6\22\u01b3"+
		"\n\22\r\22\16\22\u01b4\3\23\3\23\3\24\3\24\3\24\6\24\u01bc\n\24\r\24\16"+
		"\24\u01bd\3\25\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u01c7\n\25\3\26\3\26"+
		"\5\26\u01cb\n\26\3\26\3\26\5\26\u01cf\n\26\5\26\u01d1\n\26\3\27\3\27\3"+
		"\27\3\30\3\30\3\30\3\31\3\31\5\31\u01db\n\31\3\32\3\32\3\32\3\32\3\32"+
		"\3\32\7\32\u01e3\n\32\f\32\16\32\u01e6\13\32\3\32\3\32\5\32\u01ea\n\32"+
		"\5\32\u01ec\n\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\5\33\u01f9\n\33\3\34\3\34\5\34\u01fd\n\34\3\34\3\34\3\34\5\34\u0202\n"+
		"\34\3\35\3\35\5\35\u0206\n\35\3\35\3\35\3\36\3\36\5\36\u020c\n\36\3\36"+
		"\3\36\3\37\3\37\5\37\u0212\n\37\3\37\3\37\3 \3 \5 \u0218\n \3 \3 \3 \3"+
		" \3!\3!\5!\u0220\n!\3!\3!\3!\3!\3\"\3\"\5\"\u0228\n\"\3\"\3\"\3\"\3\""+
		"\3#\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3&\3&\5&\u023c\n&\3&\3&\5&\u0240"+
		"\n&\3&\5&\u0243\n&\3&\7&\u0246\n&\f&\16&\u0249\13&\3&\3&\3&\3\'\3\'\3"+
		"\'\3(\3(\3(\3)\3)\5)\u0256\n)\3)\3)\3*\3*\5*\u025c\n*\3*\5*\u025f\n*\3"+
		"+\3+\3+\3,\3,\3,\3,\5,\u0268\n,\3-\3-\3-\3-\3.\3.\3.\3.\3/\5/\u0273\n"+
		"/\3/\7/\u0276\n/\f/\16/\u0279\13/\3\60\3\60\5\60\u027d\n\60\3\60\5\60"+
		"\u0280\n\60\3\61\3\61\3\61\3\61\5\61\u0286\n\61\3\61\3\61\3\62\3\62\3"+
		"\62\5\62\u028d\n\62\7\62\u028f\n\62\f\62\16\62\u0292\13\62\3\63\3\63\3"+
		"\63\5\63\u0297\n\63\3\63\3\63\3\64\5\64\u029c\n\64\3\64\7\64\u029f\n\64"+
		"\f\64\16\64\u02a2\13\64\3\65\3\65\5\65\u02a6\n\65\3\65\5\65\u02a9\n\65"+
		"\3\66\3\66\3\66\5\66\u02ae\n\66\7\66\u02b0\n\66\f\66\16\66\u02b3\13\66"+
		"\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\5\67\u02bd\n\67\38\38\38\39\3"+
		"9\39\39\3:\3:\5:\u02c8\n:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3<\3<\3<\3=\3"+
		"=\5=\u02d9\n=\3>\3>\3>\7>\u02de\n>\f>\16>\u02e1\13>\3>\3>\3?\3?\7?\u02e7"+
		"\n?\f?\16?\u02ea\13?\3?\3?\3?\7?\u02ef\n?\f?\16?\u02f2\13?\3?\3?\3@\3"+
		"@\7@\u02f8\n@\f@\16@\u02fb\13@\3@\3@\3A\3A\3A\3A\3A\5A\u0304\nA\3B\3B"+
		"\3B\3C\3C\3C\7C\u030c\nC\fC\16C\u030f\13C\3D\3D\3D\3E\3E\3E\3E\3E\3E\5"+
		"E\u031a\nE\3F\3F\3F\3G\3G\5G\u0321\nG\3G\3G\5G\u0325\nG\3G\3G\3H\3H\3"+
		"H\7H\u032c\nH\fH\16H\u032f\13H\3I\3I\5I\u0333\nI\3I\3I\3J\3J\3J\3J\3J"+
		"\3J\5J\u033d\nJ\3K\5K\u0340\nK\3L\3L\3L\3L\3L\3L\5L\u0348\nL\7L\u034a"+
		"\nL\fL\16L\u034d\13L\3M\3M\5M\u0351\nM\3N\3N\3N\7N\u0356\nN\fN\16N\u0359"+
		"\13N\3O\3O\3P\3P\3P\3P\3P\3P\5P\u0363\nP\3Q\5Q\u0366\nQ\3R\3R\5R\u036a"+
		"\nR\3R\3R\3R\5R\u036f\nR\7R\u0371\nR\fR\16R\u0374\13R\3S\3S\5S\u0378\n"+
		"S\3S\3S\3T\3T\3U\3U\3V\3V\3V\7V\u0383\nV\fV\16V\u0386\13V\3W\3W\3X\3X"+
		"\3Y\3Y\3Y\7Y\u038f\nY\fY\16Y\u0392\13Y\3Z\3Z\3Z\7Z\u0397\nZ\fZ\16Z\u039a"+
		"\13Z\3[\3[\5[\u039e\n[\3\\\5\\\u03a1\n\\\3\\\3\\\3]\3]\3^\3^\3^\3^\3^"+
		"\3^\3^\3^\5^\u03af\n^\3_\3_\3_\3_\3_\7_\u03b6\n_\f_\16_\u03b9\13_\5_\u03bb"+
		"\n_\3_\5_\u03be\n_\3`\5`\u03c1\n`\3`\3`\5`\u03c5\n`\3a\3a\3b\3b\5b\u03cb"+
		"\nb\3c\3c\3c\3c\3d\3d\5d\u03d3\nd\3e\3e\3e\3e\3f\3f\6f\u03db\nf\rf\16"+
		"f\u03dc\3f\3f\3g\3g\6g\u03e3\ng\rg\16g\u03e4\3g\3g\3h\3h\5h\u03eb\nh\3"+
		"i\3i\5i\u03ef\ni\3j\3j\5j\u03f3\nj\3k\3k\5k\u03f7\nk\3l\3l\3m\3m\3m\3"+
		"m\3m\3m\5m\u0401\nm\3n\3n\3n\3o\3o\3o\3o\3o\3o\5o\u040c\no\3o\3o\3o\3"+
		"o\3o\3o\3o\3o\3o\3o\3o\3o\3o\5o\u041b\no\3o\3o\3o\5o\u0420\no\3o\3o\3"+
		"o\3o\3o\3o\3o\7o\u0429\no\fo\16o\u042c\13o\3p\3p\5p\u0430\np\3p\3p\5p"+
		"\u0434\np\3q\5q\u0437\nq\3q\3q\3r\3r\3r\3r\3r\3r\3r\3r\3r\3r\5r\u0445"+
		"\nr\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\5s\u0470\ns"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\5s\u048d\ns\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\5s\u0516\ns\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s"+
		"\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\5s"+
		"\u0553\ns\3t\3t\3t\3t\3t\3t\3t\5t\u055c\nt\3t\3t\3u\3u\3u\3u\3u\3u\3u"+
		"\5u\u0567\nu\3u\3u\3v\3v\3v\3v\3v\3v\3v\3v\3v\5v\u0574\nv\3v\3v\3w\3w"+
		"\3w\3x\3x\3x\3x\3y\3y\3y\5y\u0582\ny\3y\3y\5y\u0586\ny\3y\3y\3y\3y\5y"+
		"\u058c\ny\3y\3y\3y\3y\3y\3y\5y\u0594\ny\3y\3y\3y\3y\3y\3y\5y\u059c\ny"+
		"\3y\3y\3y\3y\3y\3y\5y\u05a4\ny\3y\3y\3y\3y\3y\3y\5y\u05ac\ny\3y\3y\3y"+
		"\3y\3y\3y\5y\u05b4\ny\3y\3y\3y\3y\3y\5y\u05bb\ny\3y\3y\5y\u05bf\ny\3z"+
		"\3z\5z\u05c3\nz\3{\3{\3{\3{\5{\u05c9\n{\3|\3|\3|\5|\u05ce\n|\3}\3}\3~"+
		"\3~\3\177\3\177\3\u0080\3\u0080\3\u0081\3\u0081\3\u0082\3\u0082\5\u0082"+
		"\u05dc\n\u0082\3\u0083\3\u0083\3\u0084\3\u0084\5\u0084\u05e2\n\u0084\3"+
		"\u0085\3\u0085\3\u0085\3\u0085\2\3\u00dc\u0086\2\4\6\b\n\f\16\20\22\24"+
		"\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtv"+
		"xz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094"+
		"\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac"+
		"\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4"+
		"\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc"+
		"\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0\u00f2\u00f4"+
		"\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106\u0108\2\20\3\2"+
		"\7\b\3\2\21\22\4\2\u0099\u0099\u009b\u009c\3\2yz\3\2\u0099\u009a\4\2\u009b"+
		"\u009b\u009f\u009f\4\2\u008b\u008d\u00a0\u00a2\4\2\u0099\u009a\u009e\u009e"+
		"\3\2|~\3\2\177\u0081\3\2\u0082\u0084\3\2\'(\3\2\u0085\u0088\3\2vw\u065d"+
		"\2\u0118\3\2\2\2\4\u011e\3\2\2\2\6\u0121\3\2\2\2\b\u0124\3\2\2\2\n\u0128"+
		"\3\2\2\2\f\u0132\3\2\2\2\16\u0137\3\2\2\2\20\u014a\3\2\2\2\22\u014c\3"+
		"\2\2\2\24\u0163\3\2\2\2\26\u0177\3\2\2\2\30\u0185\3\2\2\2\32\u018c\3\2"+
		"\2\2\34\u0191\3\2\2\2\36\u019c\3\2\2\2 \u01ae\3\2\2\2\"\u01b0\3\2\2\2"+
		"$\u01b6\3\2\2\2&\u01b8\3\2\2\2(\u01c6\3\2\2\2*\u01d0\3\2\2\2,\u01d2\3"+
		"\2\2\2.\u01d5\3\2\2\2\60\u01da\3\2\2\2\62\u01dc\3\2\2\2\64\u01f8\3\2\2"+
		"\2\66\u01fa\3\2\2\28\u0203\3\2\2\2:\u0209\3\2\2\2<\u020f\3\2\2\2>\u0215"+
		"\3\2\2\2@\u021d\3\2\2\2B\u0225\3\2\2\2D\u022d\3\2\2\2F\u0231\3\2\2\2H"+
		"\u0235\3\2\2\2J\u023b\3\2\2\2L\u024d\3\2\2\2N\u0250\3\2\2\2P\u0253\3\2"+
		"\2\2R\u025e\3\2\2\2T\u0260\3\2\2\2V\u0267\3\2\2\2X\u0269\3\2\2\2Z\u026d"+
		"\3\2\2\2\\\u0272\3\2\2\2^\u027a\3\2\2\2`\u0281\3\2\2\2b\u0289\3\2\2\2"+
		"d\u0293\3\2\2\2f\u029b\3\2\2\2h\u02a3\3\2\2\2j\u02aa\3\2\2\2l\u02bc\3"+
		"\2\2\2n\u02be\3\2\2\2p\u02c1\3\2\2\2r\u02c5\3\2\2\2t\u02cc\3\2\2\2v\u02d3"+
		"\3\2\2\2x\u02d8\3\2\2\2z\u02da\3\2\2\2|\u02e4\3\2\2\2~\u02f5\3\2\2\2\u0080"+
		"\u0303\3\2\2\2\u0082\u0305\3\2\2\2\u0084\u0308\3\2\2\2\u0086\u0310\3\2"+
		"\2\2\u0088\u0319\3\2\2\2\u008a\u031b\3\2\2\2\u008c\u031e\3\2\2\2\u008e"+
		"\u0328\3\2\2\2\u0090\u0330\3\2\2\2\u0092\u033c\3\2\2\2\u0094\u033f\3\2"+
		"\2\2\u0096\u0341\3\2\2\2\u0098\u0350\3\2\2\2\u009a\u0352\3\2\2\2\u009c"+
		"\u035a\3\2\2\2\u009e\u0362\3\2\2\2\u00a0\u0365\3\2\2\2\u00a2\u0369\3\2"+
		"\2\2\u00a4\u0377\3\2\2\2\u00a6\u037b\3\2\2\2\u00a8\u037d\3\2\2\2\u00aa"+
		"\u037f\3\2\2\2\u00ac\u0387\3\2\2\2\u00ae\u0389\3\2\2\2\u00b0\u038b\3\2"+
		"\2\2\u00b2\u0393\3\2\2\2\u00b4\u039b\3\2\2\2\u00b6\u03a0\3\2\2\2\u00b8"+
		"\u03a4\3\2\2\2\u00ba\u03ae\3\2\2\2\u00bc\u03bd\3\2\2\2\u00be\u03c0\3\2"+
		"\2\2\u00c0\u03c6\3\2\2\2\u00c2\u03ca\3\2\2\2\u00c4\u03cc\3\2\2\2\u00c6"+
		"\u03d2\3\2\2\2\u00c8\u03d4\3\2\2\2\u00ca\u03d8\3\2\2\2\u00cc\u03e0\3\2"+
		"\2\2\u00ce\u03ea\3\2\2\2\u00d0\u03ee\3\2\2\2\u00d2\u03f2\3\2\2\2\u00d4"+
		"\u03f6\3\2\2\2\u00d6\u03f8\3\2\2\2\u00d8\u0400\3\2\2\2\u00da\u0402\3\2"+
		"\2\2\u00dc\u040b\3\2\2\2\u00de\u042f\3\2\2\2\u00e0\u0436\3\2\2\2\u00e2"+
		"\u0444\3\2\2\2\u00e4\u0552\3\2\2\2\u00e6\u0554\3\2\2\2\u00e8\u055f\3\2"+
		"\2\2\u00ea\u056a\3\2\2\2\u00ec\u0577\3\2\2\2\u00ee\u057a\3\2\2\2\u00f0"+
		"\u05be\3\2\2\2\u00f2\u05c0\3\2\2\2\u00f4\u05c4\3\2\2\2\u00f6\u05cd\3\2"+
		"\2\2\u00f8\u05cf\3\2\2\2\u00fa\u05d1\3\2\2\2\u00fc\u05d3\3\2\2\2\u00fe"+
		"\u05d5\3\2\2\2\u0100\u05d7\3\2\2\2\u0102\u05db\3\2\2\2\u0104\u05dd\3\2"+
		"\2\2\u0106\u05e1\3\2\2\2\u0108\u05e3\3\2\2\2\u010a\u010f\5\4\3\2\u010b"+
		"\u0110\5\n\6\2\u010c\u0110\5\22\n\2\u010d\u0110\5\24\13\2\u010e\u0110"+
		"\5\26\f\2\u010f\u010b\3\2\2\2\u010f\u010c\3\2\2\2\u010f\u010d\3\2\2\2"+
		"\u010f\u010e\3\2\2\2\u010f\u0110\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0112"+
		"\5\60\31\2\u0112\u0113\7\2\2\3\u0113\u0119\3\2\2\2\u0114\u0116\5\62\32"+
		"\2\u0115\u0114\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0117\3\2\2\2\u0117\u0119"+
		"\7\2\2\3\u0118\u010a\3\2\2\2\u0118\u0115\3\2\2\2\u0119\3\3\2\2\2\u011a"+
		"\u011d\5\6\4\2\u011b\u011d\5\b\5\2\u011c\u011a\3\2\2\2\u011c\u011b\3\2"+
		"\2\2\u011d\u0120\3\2\2\2\u011e\u011c\3\2\2\2\u011e\u011f\3\2\2\2\u011f"+
		"\5\3\2\2\2\u0120\u011e\3\2\2\2\u0121\u0122\7\4\2\2\u0122\u0123\7u\2\2"+
		"\u0123\7\3\2\2\2\u0124\u0125\7\5\2\2\u0125\u0126\7v\2\2\u0126\u0127\7"+
		"u\2\2\u0127\t\3\2\2\2\u0128\u012c\5\16\b\2\u0129\u012b\5\30\r\2\u012a"+
		"\u0129\3\2\2\2\u012b\u012e\3\2\2\2\u012c\u012a\3\2\2\2\u012c\u012d\3\2"+
		"\2\2\u012d\u012f\3\2\2\2\u012e\u012c\3\2\2\2\u012f\u0130\5\32\16\2\u0130"+
		"\u0131\5\34\17\2\u0131\13\3\2\2\2\u0132\u0133\5\16\b\2\u0133\u0134\5\32"+
		"\16\2\u0134\u0135\5\34\17\2\u0135\u0136\5\60\31\2\u0136\r\3\2\2\2\u0137"+
		"\u0139\7\6\2\2\u0138\u013a\t\2\2\2\u0139\u0138\3\2\2\2\u0139\u013a\3\2"+
		"\2\2\u013a\u0141\3\2\2\2\u013b\u013d\5\20\t\2\u013c\u013b\3\2\2\2\u013d"+
		"\u013e\3\2\2\2\u013e\u013c\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u0142\3\2"+
		"\2\2\u0140\u0142\7\u009b\2\2\u0141\u013c\3\2\2\2\u0141\u0140\3\2\2\2\u0142"+
		"\17\3\2\2\2\u0143\u014b\5\u00d6l\2\u0144\u0145\7\u0091\2\2\u0145\u0146"+
		"\5\u00dco\2\u0146\u0147\7:\2\2\u0147\u0148\5\u00d6l\2\u0148\u0149\7\u0092"+
		"\2\2\u0149\u014b\3\2\2\2\u014a\u0143\3\2\2\2\u014a\u0144\3\2\2\2\u014b"+
		"\21\3\2\2\2\u014c\u0161\7\t\2\2\u014d\u0151\5\u0090I\2\u014e\u0150\5\30"+
		"\r\2\u014f\u014e\3\2\2\2\u0150\u0153\3\2\2\2\u0151\u014f\3\2\2\2\u0151"+
		"\u0152\3\2\2\2\u0152\u0154\3\2\2\2\u0153\u0151\3\2\2\2\u0154\u0155\5\32"+
		"\16\2\u0155\u0156\5\34\17\2\u0156\u0162\3\2\2\2\u0157\u0159\5\30\r\2\u0158"+
		"\u0157\3\2\2\2\u0159\u015c\3\2\2\2\u015a\u0158\3\2\2\2\u015a\u015b\3\2"+
		"\2\2\u015b\u015d\3\2\2\2\u015c\u015a\3\2\2\2\u015d\u015e\7\16\2\2\u015e"+
		"\u015f\5\u0090I\2\u015f\u0160\5\34\17\2\u0160\u0162\3\2\2\2\u0161\u014d"+
		"\3\2\2\2\u0161\u015a\3\2\2\2\u0162\23\3\2\2\2\u0163\u016a\7\n\2\2\u0164"+
		"\u0166\5\u00d4k\2\u0165\u0164\3\2\2\2\u0166\u0167\3\2\2\2\u0167\u0165"+
		"\3\2\2\2\u0167\u0168\3\2\2\2\u0168\u016b\3\2\2\2\u0169\u016b\7\u009b\2"+
		"\2\u016a\u0165\3\2\2\2\u016a\u0169\3\2\2\2\u016b\u016f\3\2\2\2\u016c\u016e"+
		"\5\30\r\2\u016d\u016c\3\2\2\2\u016e\u0171\3\2\2\2\u016f\u016d\3\2\2\2"+
		"\u016f\u0170\3\2\2\2\u0170\u0173\3\2\2\2\u0171\u016f\3\2\2\2\u0172\u0174"+
		"\5\32\16\2\u0173\u0172\3\2\2\2\u0173\u0174\3\2\2\2\u0174\u0175\3\2\2\2"+
		"\u0175\u0176\5\34\17\2\u0176\25\3\2\2\2\u0177\u017b\7\13\2\2\u0178\u017a"+
		"\5\30\r\2\u0179\u0178\3\2\2\2\u017a\u017d\3\2\2\2\u017b\u0179\3\2\2\2"+
		"\u017b\u017c\3\2\2\2\u017c\u017e\3\2\2\2\u017d\u017b\3\2\2\2\u017e\u0180"+
		"\5\32\16\2\u017f\u0181\5\36\20\2\u0180\u017f\3\2\2\2\u0180\u0181\3\2\2"+
		"\2\u0181\u0183\3\2\2\2\u0182\u0184\5\"\22\2\u0183\u0182\3\2\2\2\u0183"+
		"\u0184\3\2\2\2\u0184\27\3\2\2\2\u0185\u0187\7\f\2\2\u0186\u0188\7\r\2"+
		"\2\u0187\u0186\3\2\2\2\u0187\u0188\3\2\2\2\u0188\u0189\3\2\2\2\u0189\u018a"+
		"\5\u0102\u0082\2\u018a\31\3\2\2\2\u018b\u018d\7\16\2\2\u018c\u018b\3\2"+
		"\2\2\u018c\u018d\3\2\2\2\u018d\u018e\3\2\2\2\u018e\u018f\5d\63\2\u018f"+
		"\33\3\2\2\2\u0190\u0192\5\36\20\2\u0191\u0190\3\2\2\2\u0191\u0192\3\2"+
		"\2\2\u0192\u0194\3\2\2\2\u0193\u0195\5\"\22\2\u0194\u0193\3\2\2\2\u0194"+
		"\u0195\3\2\2\2\u0195\u0197\3\2\2\2\u0196\u0198\5&\24\2\u0197\u0196\3\2"+
		"\2\2\u0197\u0198\3\2\2\2\u0198\u019a\3\2\2\2\u0199\u019b\5*\26\2\u019a"+
		"\u0199\3\2\2\2\u019a\u019b\3\2\2\2\u019b\35\3\2\2\2\u019c\u019d\7;\2\2"+
		"\u019d\u019f\7\20\2\2\u019e\u01a0\5 \21\2\u019f\u019e\3\2\2\2\u01a0\u01a1"+
		"\3\2\2\2\u01a1\u019f\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\37\3\2\2\2\u01a3"+
		"\u01af\5\u00e4s\2\u01a4\u01af\5\u008aF\2\u01a5\u01a6\7\u0091\2\2\u01a6"+
		"\u01a9\5\u00dco\2\u01a7\u01a8\7:\2\2\u01a8\u01aa\5\u00d6l\2\u01a9\u01a7"+
		"\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01ac\7\u0092\2"+
		"\2\u01ac\u01af\3\2\2\2\u01ad\u01af\5\u00d6l\2\u01ae\u01a3\3\2\2\2\u01ae"+
		"\u01a4\3\2\2\2\u01ae\u01a5\3\2\2\2\u01ae\u01ad\3\2\2\2\u01af!\3\2\2\2"+
		"\u01b0\u01b2\7<\2\2\u01b1\u01b3\5$\23\2\u01b2\u01b1\3\2\2\2\u01b3\u01b4"+
		"\3\2\2\2\u01b4\u01b2\3\2\2\2\u01b4\u01b5\3\2\2\2\u01b5#\3\2\2\2\u01b6"+
		"\u01b7\5\u0088E\2\u01b7%\3\2\2\2\u01b8\u01b9\7\17\2\2\u01b9\u01bb\7\20"+
		"\2\2\u01ba\u01bc\5(\25\2\u01bb\u01ba\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd"+
		"\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\'\3\2\2\2\u01bf\u01c0\t\3\2\2"+
		"\u01c0\u01c1\7\u0091\2\2\u01c1\u01c2\5\u00dco\2\u01c2\u01c3\7\u0092\2"+
		"\2\u01c3\u01c7\3\2\2\2\u01c4\u01c7\5\u0088E\2\u01c5\u01c7\5\u00d6l\2\u01c6"+
		"\u01bf\3\2\2\2\u01c6\u01c4\3\2\2\2\u01c6\u01c5\3\2\2\2\u01c7)\3\2\2\2"+
		"\u01c8\u01ca\5,\27\2\u01c9\u01cb\5.\30\2\u01ca\u01c9\3\2\2\2\u01ca\u01cb"+
		"\3\2\2\2\u01cb\u01d1\3\2\2\2\u01cc\u01ce\5.\30\2\u01cd\u01cf\5,\27\2\u01ce"+
		"\u01cd\3\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01d1\3\2\2\2\u01d0\u01c8\3\2"+
		"\2\2\u01d0\u01cc\3\2\2\2\u01d1+\3\2\2\2\u01d2\u01d3\7\23\2\2\u01d3\u01d4"+
		"\7|\2\2\u01d4-\3\2\2\2\u01d5\u01d6\7\24\2\2\u01d6\u01d7\7|\2\2\u01d7/"+
		"\3\2\2\2\u01d8\u01d9\7\25\2\2\u01d9\u01db\5x=\2\u01da\u01d8\3\2\2\2\u01da"+
		"\u01db\3\2\2\2\u01db\61\3\2\2\2\u01dc\u01eb\5\4\3\2\u01dd\u01e4\5\64\33"+
		"\2\u01de\u01df\7\u0097\2\2\u01df\u01e0\5\4\3\2\u01e0\u01e1\5\64\33\2\u01e1"+
		"\u01e3\3\2\2\2\u01e2\u01de\3\2\2\2\u01e3\u01e6\3\2\2\2\u01e4\u01e2\3\2"+
		"\2\2\u01e4\u01e5\3\2\2\2\u01e5\u01e9\3\2\2\2\u01e6\u01e4\3\2\2\2\u01e7"+
		"\u01e8\7\u0097\2\2\u01e8\u01ea\5\4\3\2\u01e9\u01e7\3\2\2\2\u01e9\u01ea"+
		"\3\2\2\2\u01ea\u01ec\3\2\2\2\u01eb\u01dd\3\2\2\2\u01eb\u01ec\3\2\2\2\u01ec"+
		"\63\3\2\2\2\u01ed\u01f9\5\66\34\2\u01ee\u01f9\58\35\2\u01ef\u01f9\5:\36"+
		"\2\u01f0\u01f9\5> \2\u01f1\u01f9\5@!\2\u01f2\u01f9\5B\"\2\u01f3\u01f9"+
		"\5<\37\2\u01f4\u01f9\5D#\2\u01f5\u01f9\5F$\2\u01f6\u01f9\5H%\2\u01f7\u01f9"+
		"\5J&\2\u01f8\u01ed\3\2\2\2\u01f8\u01ee\3\2\2\2\u01f8\u01ef\3\2\2\2\u01f8"+
		"\u01f0\3\2\2\2\u01f8\u01f1\3\2\2\2\u01f8\u01f2\3\2\2\2\u01f8\u01f3\3\2"+
		"\2\2\u01f8\u01f4\3\2\2\2\u01f8\u01f5\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f8"+
		"\u01f7\3\2\2\2\u01f9\65\3\2\2\2\u01fa\u01fc\7)\2\2\u01fb\u01fd\7\63\2"+
		"\2\u01fc\u01fb\3\2\2\2\u01fc\u01fd\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u0201"+
		"\5\u0102\u0082\2\u01ff\u0200\78\2\2\u0200\u0202\5T+\2\u0201\u01ff\3\2"+
		"\2\2\u0201\u0202\3\2\2\2\u0202\67\3\2\2\2\u0203\u0205\7*\2\2\u0204\u0206"+
		"\7\63\2\2\u0205\u0204\3\2\2\2\u0205\u0206\3\2\2\2\u0206\u0207\3\2\2\2"+
		"\u0207\u0208\5V,\2\u02089\3\2\2\2\u0209\u020b\7+\2\2\u020a\u020c\7\63"+
		"\2\2\u020b\u020a\3\2\2\2\u020b\u020c\3\2\2\2\u020c\u020d\3\2\2\2\u020d"+
		"\u020e\5V,\2\u020e;\3\2\2\2\u020f\u0211\7/\2\2\u0210\u0212\7\63\2\2\u0211"+
		"\u0210\3\2\2\2\u0211\u0212\3\2\2\2\u0212\u0213\3\2\2\2\u0213\u0214\5T"+
		"+\2\u0214=\3\2\2\2\u0215\u0217\7,\2\2\u0216\u0218\7\63\2\2\u0217\u0216"+
		"\3\2\2\2\u0217\u0218\3\2\2\2\u0218\u0219\3\2\2\2\u0219\u021a\5R*\2\u021a"+
		"\u021b\79\2\2\u021b\u021c\5R*\2\u021c?\3\2\2\2\u021d\u021f\7-\2\2\u021e"+
		"\u0220\7\63\2\2\u021f\u021e\3\2\2\2\u021f\u0220\3\2\2\2\u0220\u0221\3"+
		"\2\2\2\u0221\u0222\5R*\2\u0222\u0223\79\2\2\u0223\u0224\5R*\2\u0224A\3"+
		"\2\2\2\u0225\u0227\7.\2\2\u0226\u0228\7\63\2\2\u0227\u0226\3\2\2\2\u0227"+
		"\u0228\3\2\2\2\u0228\u0229\3\2\2\2\u0229\u022a\5R*\2\u022a\u022b\79\2"+
		"\2\u022b\u022c\5R*\2\u022cC\3\2\2\2\u022d\u022e\7\61\2\2\u022e\u022f\7"+
		"\66\2\2\u022f\u0230\5Z.\2\u0230E\3\2\2\2\u0231\u0232\7\60\2\2\u0232\u0233"+
		"\7\66\2\2\u0233\u0234\5Z.\2\u0234G\3\2\2\2\u0235\u0236\7\60\2\2\u0236"+
		"\u0237\7\16\2\2\u0237\u0238\5X-\2\u0238I\3\2\2\2\u0239\u023a\7\67\2\2"+
		"\u023a\u023c\5\u0102\u0082\2\u023b\u0239\3\2\2\2\u023b\u023c\3\2\2\2\u023c"+
		"\u0242\3\2\2\2\u023d\u023f\5L\'\2\u023e\u0240\5N(\2\u023f\u023e\3\2\2"+
		"\2\u023f\u0240\3\2\2\2\u0240\u0243\3\2\2\2\u0241\u0243\5N(\2\u0242\u023d"+
		"\3\2\2\2\u0242\u0241\3\2\2\2\u0243\u0247\3\2\2\2\u0244\u0246\5P)\2\u0245"+
		"\u0244\3\2\2\2\u0246\u0249\3\2\2\2\u0247\u0245\3\2\2\2\u0247\u0248\3\2"+
		"\2\2\u0248\u024a\3\2\2\2\u0249\u0247\3\2\2\2\u024a\u024b\7\16\2\2\u024b"+
		"\u024c\5d\63\2\u024cK\3\2\2\2\u024d\u024e\7\60\2\2\u024e\u024f\5X-\2\u024f"+
		"M\3\2\2\2\u0250\u0251\7\61\2\2\u0251\u0252\5X-\2\u0252O\3\2\2\2\u0253"+
		"\u0255\7\62\2\2\u0254\u0256\7\r\2\2\u0255\u0254\3\2\2\2\u0255\u0256\3"+
		"\2\2\2\u0256\u0257\3\2\2\2\u0257\u0258\5\u0102\u0082\2\u0258Q\3\2\2\2"+
		"\u0259\u025f\7\64\2\2\u025a\u025c\7\27\2\2\u025b\u025a\3\2\2\2\u025b\u025c"+
		"\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025f\5\u0102\u0082\2\u025e\u0259\3"+
		"\2\2\2\u025e\u025b\3\2\2\2\u025fS\3\2\2\2\u0260\u0261\7\27\2\2\u0261\u0262"+
		"\5\u0102\u0082\2\u0262U\3\2\2\2\u0263\u0268\5T+\2\u0264\u0268\7\64\2\2"+
		"\u0265\u0268\7\r\2\2\u0266\u0268\7\65\2\2\u0267\u0263\3\2\2\2\u0267\u0264"+
		"\3\2\2\2\u0267\u0265\3\2\2\2\u0267\u0266\3\2\2\2\u0268W\3\2\2\2\u0269"+
		"\u026a\7\u0093\2\2\u026a\u026b\5\\/\2\u026b\u026c\7\u0094\2\2\u026cY\3"+
		"\2\2\2\u026d\u026e\7\u0093\2\2\u026e\u026f\5\\/\2\u026f\u0270\7\u0094"+
		"\2\2\u0270[\3\2\2\2\u0271\u0273\5b\62\2\u0272\u0271\3\2\2\2\u0272\u0273"+
		"\3\2\2\2\u0273\u0277\3\2\2\2\u0274\u0276\5^\60\2\u0275\u0274\3\2\2\2\u0276"+
		"\u0279\3\2\2\2\u0277\u0275\3\2\2\2\u0277\u0278\3\2\2\2\u0278]\3\2\2\2"+
		"\u0279\u0277\3\2\2\2\u027a\u027c\5`\61\2\u027b\u027d\7\u0098\2\2\u027c"+
		"\u027b\3\2\2\2\u027c\u027d\3\2\2\2\u027d\u027f\3\2\2\2\u027e\u0280\5b"+
		"\62\2\u027f\u027e\3\2\2\2\u027f\u0280\3\2\2\2\u0280_\3\2\2\2\u0281\u0282"+
		"\7\27\2\2\u0282\u0283\5\u00d4k\2\u0283\u0285\7\u0093\2\2\u0284\u0286\5"+
		"b\62\2\u0285\u0284\3\2\2\2\u0285\u0286\3\2\2\2\u0286\u0287\3\2\2\2\u0287"+
		"\u0288\7\u0094\2\2\u0288a\3\2\2\2\u0289\u0290\5\u0092J\2\u028a\u028c\7"+
		"\u0098\2\2\u028b\u028d\5\u0092J\2\u028c\u028b\3\2\2\2\u028c\u028d\3\2"+
		"\2\2\u028d\u028f\3\2\2\2\u028e\u028a\3\2\2\2\u028f\u0292\3\2\2\2\u0290"+
		"\u028e\3\2\2\2\u0290\u0291\3\2\2\2\u0291c\3\2\2\2\u0292\u0290\3\2\2\2"+
		"\u0293\u0296\7\u0093\2\2\u0294\u0297\5\f\7\2\u0295\u0297\5f\64\2\u0296"+
		"\u0294\3\2\2\2\u0296\u0295\3\2\2\2\u0297\u0298\3\2\2\2\u0298\u0299\7\u0094"+
		"\2\2\u0299e\3\2\2\2\u029a\u029c\5j\66\2\u029b\u029a\3\2\2\2\u029b\u029c"+
		"\3\2\2\2\u029c\u02a0\3\2\2\2\u029d\u029f\5h\65\2\u029e\u029d\3\2\2\2\u029f"+
		"\u02a2\3\2\2\2\u02a0\u029e\3\2\2\2\u02a0\u02a1\3\2\2\2\u02a1g\3\2\2\2"+
		"\u02a2\u02a0\3\2\2\2\u02a3\u02a5\5l\67\2\u02a4\u02a6\7\u0098\2\2\u02a5"+
		"\u02a4\3\2\2\2\u02a5\u02a6\3\2\2\2\u02a6\u02a8\3\2\2\2\u02a7\u02a9\5j"+
		"\66\2\u02a8\u02a7\3\2\2\2\u02a8\u02a9\3\2\2\2\u02a9i\3\2\2\2\u02aa\u02b1"+
		"\5\u009eP\2\u02ab\u02ad\7\u0098\2\2\u02ac\u02ae\5\u009eP\2\u02ad\u02ac"+
		"\3\2\2\2\u02ad\u02ae\3\2\2\2\u02ae\u02b0\3\2\2\2\u02af\u02ab\3\2\2\2\u02b0"+
		"\u02b3\3\2\2\2\u02b1\u02af\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2k\3\2\2\2"+
		"\u02b3\u02b1\3\2\2\2\u02b4\u02bd\5\u0084C\2\u02b5\u02bd\5n8\2\u02b6\u02bd"+
		"\5\u0082B\2\u02b7\u02bd\5p9\2\u02b8\u02bd\5r:\2\u02b9\u02bd\5\u0086D\2"+
		"\u02ba\u02bd\5t;\2\u02bb\u02bd\5v<\2\u02bc\u02b4\3\2\2\2\u02bc\u02b5\3"+
		"\2\2\2\u02bc\u02b6\3\2\2\2\u02bc\u02b7\3\2\2\2\u02bc\u02b8\3\2\2\2\u02bc"+
		"\u02b9\3\2\2\2\u02bc\u02ba\3\2\2\2\u02bc\u02bb\3\2\2\2\u02bdm\3\2\2\2"+
		"\u02be\u02bf\7\26\2\2\u02bf\u02c0\5d\63\2\u02c0o\3\2\2\2\u02c1\u02c2\7"+
		"\27\2\2\u02c2\u02c3\5\u00d4k\2\u02c3\u02c4\5d\63\2\u02c4q\3\2\2\2\u02c5"+
		"\u02c7\7?\2\2\u02c6\u02c8\7\63\2\2\u02c7\u02c6\3\2\2\2\u02c7\u02c8\3\2"+
		"\2\2\u02c8\u02c9\3\2\2\2\u02c9\u02ca\5\u00d4k\2\u02ca\u02cb\5d\63\2\u02cb"+
		"s\3\2\2\2\u02cc\u02cd\7@\2\2\u02cd\u02ce\7\u0091\2\2\u02ce\u02cf\5\u00dc"+
		"o\2\u02cf\u02d0\7:\2\2\u02d0\u02d1\5\u00d6l\2\u02d1\u02d2\7\u0092\2\2"+
		"\u02d2u\3\2\2\2\u02d3\u02d4\7\25\2\2\u02d4\u02d5\5x=\2\u02d5w\3\2\2\2"+
		"\u02d6\u02d9\5z>\2\u02d7\u02d9\5|?\2\u02d8\u02d6\3\2\2\2\u02d8\u02d7\3"+
		"\2\2\2\u02d9y\3\2\2\2\u02da\u02db\5\u00d6l\2\u02db\u02df\7\u0093\2\2\u02dc"+
		"\u02de\5\u0080A\2\u02dd\u02dc\3\2\2\2\u02de\u02e1\3\2\2\2\u02df\u02dd"+
		"\3\2\2\2\u02df\u02e0\3\2\2\2\u02e0\u02e2\3\2\2\2\u02e1\u02df\3\2\2\2\u02e2"+
		"\u02e3\7\u0094\2\2\u02e3{\3\2\2\2\u02e4\u02e8\7\u0091\2\2\u02e5\u02e7"+
		"\5\u00d6l\2\u02e6\u02e5\3\2\2\2\u02e7\u02ea\3\2\2\2\u02e8\u02e6\3\2\2"+
		"\2\u02e8\u02e9\3\2\2\2\u02e9\u02eb\3\2\2\2\u02ea\u02e8\3\2\2\2\u02eb\u02ec"+
		"\7\u0092\2\2\u02ec\u02f0\7\u0093\2\2\u02ed\u02ef\5~@\2\u02ee\u02ed\3\2"+
		"\2\2\u02ef\u02f2\3\2\2\2\u02f0\u02ee\3\2\2\2\u02f0\u02f1\3\2\2\2\u02f1"+
		"\u02f3\3\2\2\2\u02f2\u02f0\3\2\2\2\u02f3\u02f4\7\u0094\2\2\u02f4}\3\2"+
		"\2\2\u02f5\u02f9\7\u0091\2\2\u02f6\u02f8\5\u0080A\2\u02f7\u02f6\3\2\2"+
		"\2\u02f8\u02fb\3\2\2\2\u02f9\u02f7\3\2\2\2\u02f9\u02fa\3\2\2\2\u02fa\u02fc"+
		"\3\2\2\2\u02fb\u02f9\3\2\2\2\u02fc\u02fd\7\u0092\2\2\u02fd\177\3\2\2\2"+
		"\u02fe\u0304\5\u0102\u0082\2\u02ff\u0304\5\u00f4{\2\u0300\u0304\5\u00f6"+
		"|\2\u0301\u0304\5\u00fe\u0080\2\u0302\u0304\7=\2\2\u0303\u02fe\3\2\2\2"+
		"\u0303\u02ff\3\2\2\2\u0303\u0300\3\2\2\2\u0303\u0301\3\2\2\2\u0303\u0302"+
		"\3\2\2\2\u0304\u0081\3\2\2\2\u0305\u0306\7A\2\2\u0306\u0307\5d\63\2\u0307"+
		"\u0083\3\2\2\2\u0308\u030d\5d\63\2\u0309\u030a\7\30\2\2\u030a\u030c\5"+
		"d\63\2\u030b\u0309\3\2\2\2\u030c\u030f\3\2\2\2\u030d\u030b\3\2\2\2\u030d"+
		"\u030e\3\2\2\2\u030e\u0085\3\2\2\2\u030f\u030d\3\2\2\2\u0310\u0311\7\31"+
		"\2\2\u0311\u0312\5\u0088E\2\u0312\u0087\3\2\2\2\u0313\u0314\7\u0091\2"+
		"\2\u0314\u0315\5\u00dco\2\u0315\u0316\7\u0092\2\2\u0316\u031a\3\2\2\2"+
		"\u0317\u031a\5\u00e4s\2\u0318\u031a\5\u008aF\2\u0319\u0313\3\2\2\2\u0319"+
		"\u0317\3\2\2\2\u0319\u0318\3\2\2\2\u031a\u0089\3\2\2\2\u031b\u031c\5\u0102"+
		"\u0082\2\u031c\u031d\5\u008cG\2\u031d\u008b\3\2\2\2\u031e\u0324\7\u0091"+
		"\2\2\u031f\u0321\7\7\2\2\u0320\u031f\3\2\2\2\u0320\u0321\3\2\2\2\u0321"+
		"\u0322\3\2\2\2\u0322\u0325\5\u008eH\2\u0323\u0325\3\2\2\2\u0324\u0320"+
		"\3\2\2\2\u0324\u0323\3\2\2\2\u0325\u0326\3\2\2\2\u0326\u0327\7\u0092\2"+
		"\2\u0327\u008d\3\2\2\2\u0328\u032d\5\u00dco\2\u0329\u032a\7\u009d\2\2"+
		"\u032a\u032c\5\u00dco\2\u032b\u0329\3\2\2\2\u032c\u032f\3\2\2\2\u032d"+
		"\u032b\3\2\2\2\u032d\u032e\3\2\2\2\u032e\u008f\3\2\2\2\u032f\u032d\3\2"+
		"\2\2\u0330\u0332\7\u0093\2\2\u0331\u0333\5b\62\2\u0332\u0331\3\2\2\2\u0332"+
		"\u0333\3\2\2\2\u0333\u0334\3\2\2\2\u0334\u0335\7\u0094\2\2\u0335\u0091"+
		"\3\2\2\2\u0336\u0337\5\u00d2j\2\u0337\u0338\5\u0096L\2\u0338\u033d\3\2"+
		"\2\2\u0339\u033a\5\u00c2b\2\u033a\u033b\5\u0094K\2\u033b\u033d\3\2\2\2"+
		"\u033c\u0336\3\2\2\2\u033c\u0339\3\2\2\2\u033d\u0093\3\2\2\2\u033e\u0340"+
		"\5\u0096L\2\u033f\u033e\3\2\2\2\u033f\u0340\3\2\2\2\u0340\u0095\3\2\2"+
		"\2\u0341\u0342\5\u0098M\2\u0342\u034b\5\u009aN\2\u0343\u0347\7\u0097\2"+
		"\2\u0344\u0345\5\u0098M\2\u0345\u0346\5\u009aN\2\u0346\u0348\3\2\2\2\u0347"+
		"\u0344\3\2\2\2\u0347\u0348\3\2\2\2\u0348\u034a\3\2\2\2\u0349\u0343\3\2"+
		"\2\2\u034a\u034d\3\2\2\2\u034b\u0349\3\2\2\2\u034b\u034c\3\2\2\2\u034c"+
		"\u0097\3\2\2\2\u034d\u034b\3\2\2\2\u034e\u0351\5\u00d4k\2\u034f\u0351"+
		"\7\32\2\2\u0350\u034e\3\2\2\2\u0350\u034f\3\2\2\2\u0351\u0099\3\2\2\2"+
		"\u0352\u0357\5\u009cO\2\u0353\u0354\7\u009d\2\2\u0354\u0356\5\u009cO\2"+
		"\u0355\u0353\3\2\2\2\u0356\u0359\3\2\2\2\u0357\u0355\3\2\2\2\u0357\u0358"+
		"\3\2\2\2\u0358\u009b\3\2\2\2\u0359\u0357\3\2\2\2\u035a\u035b\5\u00ceh"+
		"\2\u035b\u009d\3\2\2\2\u035c\u035d\5\u00d2j\2\u035d\u035e\5\u00a2R\2\u035e"+
		"\u0363\3\2\2\2\u035f\u0360\5\u00c6d\2\u0360\u0361\5\u00a0Q\2\u0361\u0363"+
		"\3\2\2\2\u0362\u035c\3\2\2\2\u0362\u035f\3\2\2\2\u0363\u009f\3\2\2\2\u0364"+
		"\u0366\5\u00a2R\2\u0365\u0364\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u00a1"+
		"\3\2\2\2\u0367\u036a\5\u00a6T\2\u0368\u036a\5\u00a8U\2\u0369\u0367\3\2"+
		"\2\2\u0369\u0368\3\2\2\2\u036a\u036b\3\2\2\2\u036b\u0372\5\u00aaV\2\u036c"+
		"\u036e\7\u0097\2\2\u036d\u036f\5\u00a4S\2\u036e\u036d\3\2\2\2\u036e\u036f"+
		"\3\2\2\2\u036f\u0371\3\2\2\2\u0370\u036c\3\2\2\2\u0371\u0374\3\2\2\2\u0372"+
		"\u0370\3\2\2\2\u0372\u0373\3\2\2\2\u0373\u00a3\3\2\2\2\u0374\u0372\3\2"+
		"\2\2\u0375\u0378\5\u00a6T\2\u0376\u0378\5\u00a8U\2\u0377\u0375\3\2\2\2"+
		"\u0377\u0376\3\2\2\2\u0378\u0379\3\2\2\2\u0379\u037a\5\u00aaV\2\u037a"+
		"\u00a5\3\2\2\2\u037b\u037c\5\u00aeX\2\u037c\u00a7\3\2\2\2\u037d\u037e"+
		"\5\u00d6l\2\u037e\u00a9\3\2\2\2\u037f\u0384\5\u00acW\2\u0380\u0381\7\u009d"+
		"\2\2\u0381\u0383\5\u00acW\2\u0382\u0380\3\2\2\2\u0383\u0386\3\2\2\2\u0384"+
		"\u0382\3\2\2\2\u0384\u0385\3\2\2\2\u0385\u00ab\3\2\2\2\u0386\u0384\3\2"+
		"\2\2\u0387\u0388\5\u00d0i\2\u0388\u00ad\3\2\2\2\u0389\u038a\5\u00b0Y\2"+
		"\u038a\u00af\3\2\2\2\u038b\u0390\5\u00b2Z\2\u038c\u038d\7\u00a3\2\2\u038d"+
		"\u038f\5\u00b2Z\2\u038e\u038c\3\2\2\2\u038f\u0392\3\2\2\2\u0390\u038e"+
		"\3\2\2\2\u0390\u0391\3\2\2\2\u0391\u00b1\3\2\2\2\u0392\u0390\3\2\2\2\u0393"+
		"\u0398\5\u00b6\\\2\u0394\u0395\7\u009f\2\2\u0395\u0397\5\u00b6\\\2\u0396"+
		"\u0394\3\2\2\2\u0397\u039a\3\2\2\2\u0398\u0396\3\2\2\2\u0398\u0399\3\2"+
		"\2\2\u0399\u00b3\3\2\2\2\u039a\u0398\3\2\2\2\u039b\u039d\5\u00ba^\2\u039c"+
		"\u039e\5\u00b8]\2\u039d\u039c\3\2\2\2\u039d\u039e\3\2\2\2\u039e\u00b5"+
		"\3\2\2\2\u039f\u03a1\7\u0090\2\2\u03a0\u039f\3\2\2\2\u03a0\u03a1\3\2\2"+
		"\2\u03a1\u03a2\3\2\2\2\u03a2\u03a3\5\u00b4[\2\u03a3\u00b7\3\2\2\2\u03a4"+
		"\u03a5\t\4\2\2\u03a5\u00b9\3\2\2\2\u03a6\u03af\5\u0102\u0082\2\u03a7\u03af"+
		"\7\32\2\2\u03a8\u03a9\7\u009e\2\2\u03a9\u03af\5\u00bc_\2\u03aa\u03ab\7"+
		"\u0091\2\2\u03ab\u03ac\5\u00aeX\2\u03ac\u03ad\7\u0092\2\2\u03ad\u03af"+
		"\3\2\2\2\u03ae\u03a6\3\2\2\2\u03ae\u03a7\3\2\2\2\u03ae\u03a8\3\2\2\2\u03ae"+
		"\u03aa\3\2\2\2\u03af\u00bb\3\2\2\2\u03b0\u03be\5\u00be`\2\u03b1\u03ba"+
		"\7\u0091\2\2\u03b2\u03b7\5\u00be`\2\u03b3\u03b4\7\u00a3\2\2\u03b4\u03b6"+
		"\5\u00be`\2\u03b5\u03b3\3\2\2\2\u03b6\u03b9\3\2\2\2\u03b7\u03b5\3\2\2"+
		"\2\u03b7\u03b8\3\2\2\2\u03b8\u03bb\3\2\2\2\u03b9\u03b7\3\2\2\2\u03ba\u03b2"+
		"\3\2\2\2\u03ba\u03bb\3\2\2\2\u03bb\u03bc\3\2\2\2\u03bc\u03be\7\u0092\2"+
		"\2\u03bd\u03b0\3\2\2\2\u03bd\u03b1\3\2\2\2\u03be\u00bd\3\2\2\2\u03bf\u03c1"+
		"\7\u0090\2\2\u03c0\u03bf\3\2\2\2\u03c0\u03c1\3\2\2\2\u03c1\u03c4\3\2\2"+
		"\2\u03c2\u03c5\5\u0102\u0082\2\u03c3\u03c5\7\32\2\2\u03c4\u03c2\3\2\2"+
		"\2\u03c4\u03c3\3\2\2\2\u03c5\u00bf\3\2\2\2\u03c6\u03c7\7|\2\2\u03c7\u00c1"+
		"\3\2\2\2\u03c8\u03cb\5\u00caf\2\u03c9\u03cb\5\u00c4c\2\u03ca\u03c8\3\2"+
		"\2\2\u03ca\u03c9\3\2\2\2\u03cb\u00c3\3\2\2\2\u03cc\u03cd\7\u0095\2\2\u03cd"+
		"\u03ce\5\u0096L\2\u03ce\u03cf\7\u0096\2\2\u03cf\u00c5\3\2\2\2\u03d0\u03d3"+
		"\5\u00ccg\2\u03d1\u03d3\5\u00c8e\2\u03d2\u03d0\3\2\2\2\u03d2\u03d1\3\2"+
		"\2\2\u03d3\u00c7\3\2\2\2\u03d4\u03d5\7\u0095\2\2\u03d5\u03d6\5\u00a2R"+
		"\2\u03d6\u03d7\7\u0096\2\2\u03d7\u00c9\3\2\2\2\u03d8\u03da\7\u0091\2\2"+
		"\u03d9\u03db\5\u00ceh\2\u03da\u03d9\3\2\2\2\u03db\u03dc\3\2\2\2\u03dc"+
		"\u03da\3\2\2\2\u03dc\u03dd\3\2\2\2\u03dd\u03de\3\2\2\2\u03de\u03df\7\u0092"+
		"\2\2\u03df\u00cb\3\2\2\2\u03e0\u03e2\7\u0091\2\2\u03e1\u03e3\5\u00d0i"+
		"\2\u03e2\u03e1\3\2\2\2\u03e3\u03e4\3\2\2\2\u03e4\u03e2\3\2\2\2\u03e4\u03e5"+
		"\3\2\2\2\u03e5\u03e6\3\2\2\2\u03e6\u03e7\7\u0092\2\2\u03e7\u00cd\3\2\2"+
		"\2\u03e8\u03eb\5\u00d2j\2\u03e9\u03eb\5\u00c2b\2\u03ea\u03e8\3\2\2\2\u03ea"+
		"\u03e9\3\2\2\2\u03eb\u00cf\3\2\2\2\u03ec\u03ef\5\u00d2j\2\u03ed\u03ef"+
		"\5\u00c6d\2\u03ee\u03ec\3\2\2\2\u03ee\u03ed\3\2\2\2\u03ef\u00d1\3\2\2"+
		"\2\u03f0\u03f3\5\u00d6l\2\u03f1\u03f3\5\u00d8m\2\u03f2\u03f0\3\2\2\2\u03f2"+
		"\u03f1\3\2\2\2\u03f3\u00d3\3\2\2\2\u03f4\u03f7\5\u00d6l\2\u03f5\u03f7"+
		"\5\u0102\u0082\2\u03f6\u03f4\3\2\2\2\u03f6\u03f5\3\2\2\2\u03f7\u00d5\3"+
		"\2\2\2\u03f8\u03f9\t\5\2\2\u03f9\u00d7\3\2\2\2\u03fa\u0401\5\u0102\u0082"+
		"\2\u03fb\u0401\5\u00f4{\2\u03fc\u0401\5\u00f6|\2\u03fd\u0401\5\u00fe\u0080"+
		"\2\u03fe\u0401\5\u0106\u0084\2\u03ff\u0401\5\u00dan\2\u0400\u03fa\3\2"+
		"\2\2\u0400\u03fb\3\2\2\2\u0400\u03fc\3\2\2\2\u0400\u03fd\3\2\2\2\u0400"+
		"\u03fe\3\2\2\2\u0400\u03ff\3\2\2\2\u0401\u00d9\3\2\2\2\u0402\u0403\7\u0091"+
		"\2\2\u0403\u0404\7\u0092\2\2\u0404\u00db\3\2\2\2\u0405\u0406\bo\1\2\u0406"+
		"\u0407\t\6\2\2\u0407\u040c\5\u00dco\13\u0408\u0409\7\u009e\2\2\u0409\u040c"+
		"\5\u00dco\n\u040a\u040c\5\u00e2r\2\u040b\u0405\3\2\2\2\u040b\u0408\3\2"+
		"\2\2\u040b\u040a\3\2\2\2\u040c\u042a\3\2\2\2\u040d\u040e\f\t\2\2\u040e"+
		"\u040f\t\7\2\2\u040f\u0429\5\u00dco\n\u0410\u0411\f\b\2\2\u0411\u0412"+
		"\t\6\2\2\u0412\u0429\5\u00dco\t\u0413\u0414\f\5\2\2\u0414\u0415\t\b\2"+
		"\2\u0415\u0429\5\u00dco\6\u0416\u0417\f\7\2\2\u0417\u0429\5\u00dep\2\u0418"+
		"\u041a\f\6\2\2\u0419\u041b\7q\2\2\u041a\u0419\3\2\2\2\u041a\u041b\3\2"+
		"\2\2\u041b\u041c\3\2\2\2\u041c\u041d\7r\2\2\u041d\u041f\7\u0091\2\2\u041e"+
		"\u0420\5\u008eH\2\u041f\u041e\3\2\2\2\u041f\u0420\3\2\2\2\u0420\u0421"+
		"\3\2\2\2\u0421\u0429\7\u0092\2\2\u0422\u0423\f\4\2\2\u0423\u0424\7\u008e"+
		"\2\2\u0424\u0429\5\u00dco\2\u0425\u0426\f\3\2\2\u0426\u0427\7\u008f\2"+
		"\2\u0427\u0429\5\u00dco\2\u0428\u040d\3\2\2\2\u0428\u0410\3\2\2\2\u0428"+
		"\u0413\3\2\2\2\u0428\u0416\3\2\2\2\u0428\u0418\3\2\2\2\u0428\u0422\3\2"+
		"\2\2\u0428\u0425\3\2\2\2\u0429\u042c\3\2\2\2\u042a\u0428\3\2\2\2\u042a"+
		"\u042b\3\2\2\2\u042b\u00dd\3\2\2\2\u042c\u042a\3\2\2\2\u042d\u0430\5\u00fa"+
		"~\2\u042e\u0430\5\u00fc\177\2\u042f\u042d\3\2\2\2\u042f\u042e\3\2\2\2"+
		"\u0430\u0433\3\2\2\2\u0431\u0432\t\7\2\2\u0432\u0434\5\u00e0q\2\u0433"+
		"\u0431\3\2\2\2\u0433\u0434\3\2\2\2\u0434\u00df\3\2\2\2\u0435\u0437\t\t"+
		"\2\2\u0436\u0435\3\2\2\2\u0436\u0437\3\2\2\2\u0437\u0438\3\2\2\2\u0438"+
		"\u0439\5\u00e2r\2\u0439\u00e1\3\2\2\2\u043a\u043b\7\u0091\2\2\u043b\u043c"+
		"\5\u00dco\2\u043c\u043d\7\u0092\2\2\u043d\u0445\3\2\2\2\u043e\u0445\5"+
		"\u00e4s\2\u043f\u0445\5\u00f2z\2\u0440\u0445\5\u00f4{\2\u0441\u0445\5"+
		"\u00f6|\2\u0442\u0445\5\u00fe\u0080\2\u0443\u0445\5\u00d6l\2\u0444\u043a"+
		"\3\2\2\2\u0444\u043e\3\2\2\2\u0444\u043f\3\2\2\2\u0444\u0440\3\2\2\2\u0444"+
		"\u0441\3\2\2\2\u0444\u0442\3\2\2\2\u0444\u0443\3\2\2\2\u0445\u00e3\3\2"+
		"\2\2\u0446\u0553\5\u00f0y\2\u0447\u0448\7\33\2\2\u0448\u0449\7\u0091\2"+
		"\2\u0449\u044a\5\u00dco\2\u044a\u044b\7\u0092\2\2\u044b\u0553\3\2\2\2"+
		"\u044c\u044d\7\34\2\2\u044d\u044e\7\u0091\2\2\u044e\u044f\5\u00dco\2\u044f"+
		"\u0450\7\u0092\2\2\u0450\u0553\3\2\2\2\u0451\u0452\7\35\2\2\u0452\u0453"+
		"\7\u0091\2\2\u0453\u0454\5\u00dco\2\u0454\u0455\7\u009d\2\2\u0455\u0456"+
		"\5\u00dco\2\u0456\u0457\7\u0092\2\2\u0457\u0553\3\2\2\2\u0458\u0459\7"+
		"\36\2\2\u0459\u045a\7\u0091\2\2\u045a\u045b\5\u00dco\2\u045b\u045c\7\u0092"+
		"\2\2\u045c\u0553\3\2\2\2\u045d\u045e\7\37\2\2\u045e\u045f\7\u0091\2\2"+
		"\u045f\u0460\5\u00d6l\2\u0460\u0461\7\u0092\2\2\u0461\u0553\3\2\2\2\u0462"+
		"\u0463\7B\2\2\u0463\u0464\7\u0091\2\2\u0464\u0465\5\u00dco\2\u0465\u0466"+
		"\7\u0092\2\2\u0466\u0553\3\2\2\2\u0467\u0468\7C\2\2\u0468\u0469\7\u0091"+
		"\2\2\u0469\u046a\5\u00dco\2\u046a\u046b\7\u0092\2\2\u046b\u0553\3\2\2"+
		"\2\u046c\u046d\7D\2\2\u046d\u046f\7\u0091\2\2\u046e\u0470\5\u00dco\2\u046f"+
		"\u046e\3\2\2\2\u046f\u0470\3\2\2\2\u0470\u0471\3\2\2\2\u0471\u0553\7\u0092"+
		"\2\2\u0472\u0473\7E\2\2\u0473\u0474\7\u0091\2\2\u0474\u0553\7\u0092\2"+
		"\2\u0475\u0476\7F\2\2\u0476\u0477\7\u0091\2\2\u0477\u0478\5\u00dco\2\u0478"+
		"\u0479\7\u0092\2\2\u0479\u0553\3\2\2\2\u047a\u047b\7G\2\2\u047b\u047c"+
		"\7\u0091\2\2\u047c\u047d\5\u00dco\2\u047d\u047e\7\u0092\2\2\u047e\u0553"+
		"\3\2\2\2\u047f\u0480\7H\2\2\u0480\u0481\7\u0091\2\2\u0481\u0482\5\u00dc"+
		"o\2\u0482\u0483\7\u0092\2\2\u0483\u0553\3\2\2\2\u0484\u0485\7I\2\2\u0485"+
		"\u0486\7\u0091\2\2\u0486\u0487\5\u00dco\2\u0487\u0488\7\u0092\2\2\u0488"+
		"\u0553\3\2\2\2\u0489\u048a\7J\2\2\u048a\u048c\7\u0091\2\2\u048b\u048d"+
		"\5\u008eH\2\u048c\u048b\3\2\2\2\u048c\u048d\3\2\2\2\u048d\u048e\3\2\2"+
		"\2\u048e\u0553\7\u0092\2\2\u048f\u0553\5\u00e8u\2\u0490\u0491\7K\2\2\u0491"+
		"\u0492\7\u0091\2\2\u0492\u0493\5\u00dco\2\u0493\u0494\7\u0092\2\2\u0494"+
		"\u0553\3\2\2\2\u0495\u0553\5\u00eav\2\u0496\u0497\7L\2\2\u0497\u0498\7"+
		"\u0091\2\2\u0498\u0499\5\u00dco\2\u0499\u049a\7\u0092\2\2\u049a\u0553"+
		"\3\2\2\2\u049b\u049c\7M\2\2\u049c\u049d\7\u0091\2\2\u049d\u049e\5\u00dc"+
		"o\2\u049e\u049f\7\u0092\2\2\u049f\u0553\3\2\2\2\u04a0\u04a1\7N\2\2\u04a1"+
		"\u04a2\7\u0091\2\2\u04a2\u04a3\5\u00dco\2\u04a3\u04a4\7\u0092\2\2\u04a4"+
		"\u0553\3\2\2\2\u04a5\u04a6\7O\2\2\u04a6\u04a7\7\u0091\2\2\u04a7\u04a8"+
		"\5\u00dco\2\u04a8\u04a9\7\u009d\2\2\u04a9\u04aa\5\u00dco\2\u04aa\u04ab"+
		"\7\u0092\2\2\u04ab\u0553\3\2\2\2\u04ac\u04ad\7P\2\2\u04ad\u04ae\7\u0091"+
		"\2\2\u04ae\u04af\5\u00dco\2\u04af\u04b0\7\u009d\2\2\u04b0\u04b1\5\u00dc"+
		"o\2\u04b1\u04b2\7\u0092\2\2\u04b2\u0553\3\2\2\2\u04b3\u04b4\7Q\2\2\u04b4"+
		"\u04b5\7\u0091\2\2\u04b5\u04b6\5\u00dco\2\u04b6\u04b7\7\u009d\2\2\u04b7"+
		"\u04b8\5\u00dco\2\u04b8\u04b9\7\u0092\2\2\u04b9\u0553\3\2\2\2\u04ba\u04bb"+
		"\7R\2\2\u04bb\u04bc\7\u0091\2\2\u04bc\u04bd\5\u00dco\2\u04bd\u04be\7\u009d"+
		"\2\2\u04be\u04bf\5\u00dco\2\u04bf\u04c0\7\u0092\2\2\u04c0\u0553\3\2\2"+
		"\2\u04c1\u04c2\7S\2\2\u04c2\u04c3\7\u0091\2\2\u04c3\u04c4\5\u00dco\2\u04c4"+
		"\u04c5\7\u009d\2\2\u04c5\u04c6\5\u00dco\2\u04c6\u04c7\7\u0092\2\2\u04c7"+
		"\u0553\3\2\2\2\u04c8\u04c9\7U\2\2\u04c9\u04ca\7\u0091\2\2\u04ca\u04cb"+
		"\5\u00dco\2\u04cb\u04cc\7\u0092\2\2\u04cc\u0553\3\2\2\2\u04cd\u04ce\7"+
		"V\2\2\u04ce\u04cf\7\u0091\2\2\u04cf\u04d0\5\u00dco\2\u04d0\u04d1\7\u0092"+
		"\2\2\u04d1\u0553\3\2\2\2\u04d2\u04d3\7W\2\2\u04d3\u04d4\7\u0091\2\2\u04d4"+
		"\u04d5\5\u00dco\2\u04d5\u04d6\7\u0092\2\2\u04d6\u0553\3\2\2\2\u04d7\u04d8"+
		"\7X\2\2\u04d8\u04d9\7\u0091\2\2\u04d9\u04da\5\u00dco\2\u04da\u04db\7\u0092"+
		"\2\2\u04db\u0553\3\2\2\2\u04dc\u04dd\7Y\2\2\u04dd\u04de\7\u0091\2\2\u04de"+
		"\u04df\5\u00dco\2\u04df\u04e0\7\u0092\2\2\u04e0\u0553\3\2\2\2\u04e1\u04e2"+
		"\7Z\2\2\u04e2\u04e3\7\u0091\2\2\u04e3\u04e4\5\u00dco\2\u04e4\u04e5\7\u0092"+
		"\2\2\u04e5\u0553\3\2\2\2\u04e6\u04e7\7[\2\2\u04e7\u04e8\7\u0091\2\2\u04e8"+
		"\u04e9\5\u00dco\2\u04e9\u04ea\7\u0092\2\2\u04ea\u0553\3\2\2\2\u04eb\u04ec"+
		"\7\\\2\2\u04ec\u04ed\7\u0091\2\2\u04ed\u04ee\5\u00dco\2\u04ee\u04ef\7"+
		"\u0092\2\2\u04ef\u0553\3\2\2\2\u04f0\u04f1\7]\2\2\u04f1\u04f2\7\u0091"+
		"\2\2\u04f2\u0553\7\u0092\2\2\u04f3\u04f4\7^\2\2\u04f4\u04f5\7\u0091\2"+
		"\2\u04f5\u0553\7\u0092\2\2\u04f6\u04f7\7_\2\2\u04f7\u04f8\7\u0091\2\2"+
		"\u04f8\u0553\7\u0092\2\2\u04f9\u04fa\7`\2\2\u04fa\u04fb\7\u0091\2\2\u04fb"+
		"\u04fc\5\u00dco\2\u04fc\u04fd\7\u0092\2\2\u04fd\u0553\3\2\2\2\u04fe\u04ff"+
		"\7a\2\2\u04ff\u0500\7\u0091\2\2\u0500\u0501\5\u00dco\2\u0501\u0502\7\u0092"+
		"\2\2\u0502\u0553\3\2\2\2\u0503\u0504\7b\2\2\u0504\u0505\7\u0091\2\2\u0505"+
		"\u0506\5\u00dco\2\u0506\u0507\7\u0092\2\2\u0507\u0553\3\2\2\2\u0508\u0509"+
		"\7c\2\2\u0509\u050a\7\u0091\2\2\u050a\u050b\5\u00dco\2\u050b\u050c\7\u0092"+
		"\2\2\u050c\u0553\3\2\2\2\u050d\u050e\7d\2\2\u050e\u050f\7\u0091\2\2\u050f"+
		"\u0510\5\u00dco\2\u0510\u0511\7\u0092\2\2\u0511\u0553\3\2\2\2\u0512\u0513"+
		"\7e\2\2\u0513\u0515\7\u0091\2\2\u0514\u0516\5\u008eH\2\u0515\u0514\3\2"+
		"\2\2\u0515\u0516\3\2\2\2\u0516\u0517\3\2\2\2\u0517\u0553\7\u0092\2\2\u0518"+
		"\u0519\7f\2\2\u0519\u051a\7\u0091\2\2\u051a\u051b\5\u00dco\2\u051b\u051c"+
		"\7\u009d\2\2\u051c\u051d\5\u00dco\2\u051d\u051e\7\u009d\2\2\u051e\u051f"+
		"\5\u00dco\2\u051f\u0520\7\u0092\2\2\u0520\u0553\3\2\2\2\u0521\u0522\7"+
		"g\2\2\u0522\u0523\7\u0091\2\2\u0523\u0524\5\u00dco\2\u0524\u0525\7\u009d"+
		"\2\2\u0525\u0526\5\u00dco\2\u0526\u0527\7\u0092\2\2\u0527\u0553\3\2\2"+
		"\2\u0528\u0529\7h\2\2\u0529\u052a\7\u0091\2\2\u052a\u052b\5\u00dco\2\u052b"+
		"\u052c\7\u009d\2\2\u052c\u052d\5\u00dco\2\u052d\u052e\7\u0092\2\2\u052e"+
		"\u0553\3\2\2\2\u052f\u0530\7 \2\2\u0530\u0531\7\u0091\2\2\u0531\u0532"+
		"\5\u00dco\2\u0532\u0533\7\u009d\2\2\u0533\u0534\5\u00dco\2\u0534\u0535"+
		"\7\u0092\2\2\u0535\u0553\3\2\2\2\u0536\u0537\7!\2\2\u0537\u0538\7\u0091"+
		"\2\2\u0538\u0539\5\u00dco\2\u0539\u053a\7\u0092\2\2\u053a\u0553\3\2\2"+
		"\2\u053b\u053c\7\"\2\2\u053c\u053d\7\u0091\2\2\u053d\u053e\5\u00dco\2"+
		"\u053e\u053f\7\u0092\2\2\u053f\u0553\3\2\2\2\u0540\u0541\7#\2\2\u0541"+
		"\u0542\7\u0091\2\2\u0542\u0543\5\u00dco\2\u0543\u0544\7\u0092\2\2\u0544"+
		"\u0553\3\2\2\2\u0545\u0546\7$\2\2\u0546\u0547\7\u0091\2\2\u0547\u0548"+
		"\5\u00dco\2\u0548\u0549\7\u0092\2\2\u0549\u0553\3\2\2\2\u054a\u054b\7"+
		"i\2\2\u054b\u054c\7\u0091\2\2\u054c\u054d\5\u00dco\2\u054d\u054e\7\u0092"+
		"\2\2\u054e\u0553\3\2\2\2\u054f\u0553\5\u00e6t\2\u0550\u0553\5\u00ecw\2"+
		"\u0551\u0553\5\u00eex\2\u0552\u0446\3\2\2\2\u0552\u0447\3\2\2\2\u0552"+
		"\u044c\3\2\2\2\u0552\u0451\3\2\2\2\u0552\u0458\3\2\2\2\u0552\u045d\3\2"+
		"\2\2\u0552\u0462\3\2\2\2\u0552\u0467\3\2\2\2\u0552\u046c\3\2\2\2\u0552"+
		"\u0472\3\2\2\2\u0552\u0475\3\2\2\2\u0552\u047a\3\2\2\2\u0552\u047f\3\2"+
		"\2\2\u0552\u0484\3\2\2\2\u0552\u0489\3\2\2\2\u0552\u048f\3\2\2\2\u0552"+
		"\u0490\3\2\2\2\u0552\u0495\3\2\2\2\u0552\u0496\3\2\2\2\u0552\u049b\3\2"+
		"\2\2\u0552\u04a0\3\2\2\2\u0552\u04a5\3\2\2\2\u0552\u04ac\3\2\2\2\u0552"+
		"\u04b3\3\2\2\2\u0552\u04ba\3\2\2\2\u0552\u04c1\3\2\2\2\u0552\u04c8\3\2"+
		"\2\2\u0552\u04cd\3\2\2\2\u0552\u04d2\3\2\2\2\u0552\u04d7\3\2\2\2\u0552"+
		"\u04dc\3\2\2\2\u0552\u04e1\3\2\2\2\u0552\u04e6\3\2\2\2\u0552\u04eb\3\2"+
		"\2\2\u0552\u04f0\3\2\2\2\u0552\u04f3\3\2\2\2\u0552\u04f6\3\2\2\2\u0552"+
		"\u04f9\3\2\2\2\u0552\u04fe\3\2\2\2\u0552\u0503\3\2\2\2\u0552\u0508\3\2"+
		"\2\2\u0552\u050d\3\2\2\2\u0552\u0512\3\2\2\2\u0552\u0518\3\2\2\2\u0552"+
		"\u0521\3\2\2\2\u0552\u0528\3\2\2\2\u0552\u052f\3\2\2\2\u0552\u0536\3\2"+
		"\2\2\u0552\u053b\3\2\2\2\u0552\u0540\3\2\2\2\u0552\u0545\3\2\2\2\u0552"+
		"\u054a\3\2\2\2\u0552\u054f\3\2\2\2\u0552\u0550\3\2\2\2\u0552\u0551\3\2"+
		"\2\2\u0553\u00e5\3\2\2\2\u0554\u0555\7%\2\2\u0555\u0556\7\u0091\2\2\u0556"+
		"\u0557\5\u00dco\2\u0557\u0558\7\u009d\2\2\u0558\u055b\5\u00dco\2\u0559"+
		"\u055a\7\u009d\2\2\u055a\u055c\5\u00dco\2\u055b\u0559\3\2\2\2\u055b\u055c"+
		"\3\2\2\2\u055c\u055d\3\2\2\2\u055d\u055e\7\u0092\2\2\u055e\u00e7\3\2\2"+
		"\2\u055f\u0560\7&\2\2\u0560\u0561\7\u0091\2\2\u0561\u0562\5\u00dco\2\u0562"+
		"\u0563\7\u009d\2\2\u0563\u0566\5\u00dco\2\u0564\u0565\7\u009d\2\2\u0565"+
		"\u0567\5\u00dco\2\u0566\u0564\3\2\2\2\u0566\u0567\3\2\2\2\u0567\u0568"+
		"\3\2\2\2\u0568\u0569\7\u0092\2\2\u0569\u00e9\3\2\2\2\u056a\u056b\7T\2"+
		"\2\u056b\u056c\7\u0091\2\2\u056c\u056d\5\u00dco\2\u056d\u056e\7\u009d"+
		"\2\2\u056e\u056f\5\u00dco\2\u056f\u0570\7\u009d\2\2\u0570\u0573\5\u00dc"+
		"o\2\u0571\u0572\7\u009d\2\2\u0572\u0574\5\u00dco\2\u0573\u0571\3\2\2\2"+
		"\u0573\u0574\3\2\2\2\u0574\u0575\3\2\2\2\u0575\u0576\7\u0092\2\2\u0576"+
		"\u00eb\3\2\2\2\u0577\u0578\7s\2\2\u0578\u0579\5d\63\2\u0579\u00ed\3\2"+
		"\2\2\u057a\u057b\7q\2\2\u057b\u057c\7s\2\2\u057c\u057d\5d\63\2\u057d\u00ef"+
		"\3\2\2\2\u057e\u057f\7j\2\2\u057f\u0581\7\u0091\2\2\u0580\u0582\7\7\2"+
		"\2\u0581\u0580\3\2\2\2\u0581\u0582\3\2\2\2\u0582\u0585\3\2\2\2\u0583\u0586"+
		"\7\u009b\2\2\u0584\u0586\5\u00dco\2\u0585\u0583\3\2\2\2\u0585\u0584\3"+
		"\2\2\2\u0586\u0587\3\2\2\2\u0587\u05bf\7\u0092\2\2\u0588\u0589\7k\2\2"+
		"\u0589\u058b\7\u0091\2\2\u058a\u058c\7\7\2\2\u058b\u058a\3\2\2\2\u058b"+
		"\u058c\3\2\2\2\u058c\u058d\3\2\2\2\u058d\u058e\5\u00dco\2\u058e\u058f"+
		"\7\u0092\2\2\u058f\u05bf\3\2\2\2\u0590\u0591\7l\2\2\u0591\u0593\7\u0091"+
		"\2\2\u0592\u0594\7\7\2\2\u0593\u0592\3\2\2\2\u0593\u0594\3\2\2\2\u0594"+
		"\u0595\3\2\2\2\u0595\u0596\5\u00dco\2\u0596\u0597\7\u0092\2\2\u0597\u05bf"+
		"\3\2\2\2\u0598\u0599\7m\2\2\u0599\u059b\7\u0091\2\2\u059a\u059c\7\7\2"+
		"\2\u059b\u059a\3\2\2\2\u059b\u059c\3\2\2\2\u059c\u059d\3\2\2\2\u059d\u059e"+
		"\5\u00dco\2\u059e\u059f\7\u0092\2\2\u059f\u05bf\3\2\2\2\u05a0\u05a1\7"+
		"n\2\2\u05a1\u05a3\7\u0091\2\2\u05a2\u05a4\7\7\2\2\u05a3\u05a2\3\2\2\2"+
		"\u05a3\u05a4\3\2\2\2\u05a4\u05a5\3\2\2\2\u05a5\u05a6\5\u00dco\2\u05a6"+
		"\u05a7\7\u0092\2\2\u05a7\u05bf\3\2\2\2\u05a8\u05a9\7o\2\2\u05a9\u05ab"+
		"\7\u0091\2\2\u05aa\u05ac\7\7\2\2\u05ab\u05aa\3\2\2\2\u05ab\u05ac\3\2\2"+
		"\2\u05ac\u05ad\3\2\2\2\u05ad\u05ae\5\u00dco\2\u05ae\u05af\7\u0092\2\2"+
		"\u05af\u05bf\3\2\2\2\u05b0\u05b1\7p\2\2\u05b1\u05b3\7\u0091\2\2\u05b2"+
		"\u05b4\7\7\2\2\u05b3\u05b2\3\2\2\2\u05b3\u05b4\3\2\2\2\u05b4\u05b5\3\2"+
		"\2\2\u05b5\u05ba\5\u00dco\2\u05b6\u05b7\7\u0097\2\2\u05b7\u05b8\7t\2\2"+
		"\u05b8\u05b9\7\u00a0\2\2\u05b9\u05bb\5\u0100\u0081\2\u05ba\u05b6\3\2\2"+
		"\2\u05ba\u05bb\3\2\2\2\u05bb\u05bc\3\2\2\2\u05bc\u05bd\7\u0092\2\2\u05bd"+
		"\u05bf\3\2\2\2\u05be\u057e\3\2\2\2\u05be\u0588\3\2\2\2\u05be\u0590\3\2"+
		"\2\2\u05be\u0598\3\2\2\2\u05be\u05a0\3\2\2\2\u05be\u05a8\3\2\2\2\u05be"+
		"\u05b0\3\2\2\2\u05bf\u00f1\3\2\2\2\u05c0\u05c2\5\u0102\u0082\2\u05c1\u05c3"+
		"\5\u008cG\2\u05c2\u05c1\3\2\2\2\u05c2\u05c3\3\2\2\2\u05c3\u00f3\3\2\2"+
		"\2\u05c4\u05c8\5\u0100\u0081\2\u05c5\u05c9\7{\2\2\u05c6\u05c7\7\u008a"+
		"\2\2\u05c7\u05c9\5\u0102\u0082\2\u05c8\u05c5\3\2\2\2\u05c8\u05c6\3\2\2"+
		"\2\u05c8\u05c9\3\2\2\2\u05c9\u00f5\3\2\2\2\u05ca\u05ce\5\u00f8}\2\u05cb"+
		"\u05ce\5\u00fa~\2\u05cc\u05ce\5\u00fc\177\2\u05cd\u05ca\3\2\2\2\u05cd"+
		"\u05cb\3\2\2\2\u05cd\u05cc\3\2\2\2\u05ce\u00f7\3\2\2\2\u05cf\u05d0\t\n"+
		"\2\2\u05d0\u00f9\3\2\2\2\u05d1\u05d2\t\13\2\2\u05d2\u00fb\3\2\2\2\u05d3"+
		"\u05d4\t\f\2\2\u05d4\u00fd\3\2\2\2\u05d5\u05d6\t\r\2\2\u05d6\u00ff\3\2"+
		"\2\2\u05d7\u05d8\t\16\2\2\u05d8\u0101\3\2\2\2\u05d9\u05dc\7u\2\2\u05da"+
		"\u05dc\5\u0104\u0083\2\u05db\u05d9\3\2\2\2\u05db\u05da\3\2\2\2\u05dc\u0103"+
		"\3\2\2\2\u05dd\u05de\t\17\2\2\u05de\u0105\3\2\2\2\u05df\u05e2\7x\2\2\u05e0"+
		"\u05e2\5\u0108\u0085\2\u05e1\u05df\3\2\2\2\u05e1\u05e0\3\2\2\2\u05e2\u0107"+
		"\3\2\2\2\u05e3\u05e4\7\u0095\2\2\u05e4\u05e5\7\u0096\2\2\u05e5\u0109\3"+
		"\2\2\2\u0095\u010f\u0115\u0118\u011c\u011e\u012c\u0139\u013e\u0141\u014a"+
		"\u0151\u015a\u0161\u0167\u016a\u016f\u0173\u017b\u0180\u0183\u0187\u018c"+
		"\u0191\u0194\u0197\u019a\u01a1\u01a9\u01ae\u01b4\u01bd\u01c6\u01ca\u01ce"+
		"\u01d0\u01da\u01e4\u01e9\u01eb\u01f8\u01fc\u0201\u0205\u020b\u0211\u0217"+
		"\u021f\u0227\u023b\u023f\u0242\u0247\u0255\u025b\u025e\u0267\u0272\u0277"+
		"\u027c\u027f\u0285\u028c\u0290\u0296\u029b\u02a0\u02a5\u02a8\u02ad\u02b1"+
		"\u02bc\u02c7\u02d8\u02df\u02e8\u02f0\u02f9\u0303\u030d\u0319\u0320\u0324"+
		"\u032d\u0332\u033c\u033f\u0347\u034b\u0350\u0357\u0362\u0365\u0369\u036e"+
		"\u0372\u0377\u0384\u0390\u0398\u039d\u03a0\u03ae\u03b7\u03ba\u03bd\u03c0"+
		"\u03c4\u03ca\u03d2\u03dc\u03e4\u03ea\u03ee\u03f2\u03f6\u0400\u040b\u041a"+
		"\u041f\u0428\u042a\u042f\u0433\u0436\u0444\u046f\u048c\u0515\u0552\u055b"+
		"\u0566\u0573\u0581\u0585\u058b\u0593\u059b\u05a3\u05ab\u05b3\u05ba\u05be"+
		"\u05c2\u05c8\u05cd\u05db\u05e1";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}