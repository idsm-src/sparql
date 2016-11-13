// Generated from SparqlParser.g4 by ANTLR 4.5.3
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
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, DEFINE=2, BASE=3, PREFIX=4, SELECT=5, DISTINCT=6, REDUCED=7, CONSTRUCT=8, 
		DESCRIBE=9, ASK=10, FROM=11, NAMED=12, WHERE=13, ORDER=14, BY=15, ASC=16, 
		DESC=17, LIMIT=18, OFFSET=19, VALUES=20, OPTIONAL=21, GRAPH=22, UNION=23, 
		FILTER=24, A=25, STR=26, LANG=27, LANGMATCHES=28, DATATYPE=29, BOUND=30, 
		SAMETERM=31, ISIRI=32, ISURI=33, ISBLANK=34, ISLITERAL=35, REGEX=36, SUBSTR=37, 
		TRUE=38, FALSE=39, LOAD=40, CLEAR=41, DROP=42, ADD=43, MOVE=44, COPY=45, 
		CREATE=46, DELETE=47, INSERT=48, USING=49, SILENT=50, DEFAULT=51, ALL=52, 
		DATA=53, WITH=54, INTO=55, TO=56, AS=57, GROUP=58, HAVING=59, UNDEF=60, 
		BINDINGS=61, SERVICE=62, BIND=63, MINUS=64, IRI=65, URI=66, BNODE=67, 
		RAND=68, ABS=69, CEIL=70, FLOOR=71, ROUND=72, CONCAT=73, STRLEN=74, UCASE=75, 
		LCASE=76, ENCODE_FOR_URI=77, CONTAINS=78, STRSTARTS=79, STRENDS=80, STRBEFORE=81, 
		STRAFTER=82, REPLACE=83, YEAR=84, MONTH=85, DAY=86, HOURS=87, MINUTES=88, 
		SECONDS=89, TIMEZONE=90, TZ=91, NOW=92, UUID=93, STRUUID=94, MD5=95, SHA1=96, 
		SHA256=97, SHA384=98, SHA512=99, COALESCE=100, IF=101, STRLANG=102, STRDT=103, 
		ISNUMERIC=104, COUNT=105, SUM=106, MIN=107, MAX=108, AVG=109, SAMPLE=110, 
		GROUP_CONCAT=111, NOT=112, IN=113, EXISTS=114, SEPARATOR=115, OPTION=116, 
		TABLE_OPTION=117, IRIREF=118, PNAME_NS=119, PNAME_LN=120, BLANK_NODE_LABEL=121, 
		VAR1=122, VAR2=123, LANGTAG=124, INTEGER=125, DECIMAL=126, DOUBLE=127, 
		INTEGER_POSITIVE=128, DECIMAL_POSITIVE=129, DOUBLE_POSITIVE=130, INTEGER_NEGATIVE=131, 
		DECIMAL_NEGATIVE=132, DOUBLE_NEGATIVE=133, STRING_LITERAL1=134, STRING_LITERAL2=135, 
		STRING_LITERAL_LONG1=136, STRING_LITERAL_LONG2=137, COMMENT=138, REFERENCE=139, 
		LESS_EQUAL=140, GREATER_EQUAL=141, NOT_EQUAL=142, AND=143, OR=144, INVERSE=145, 
		OPEN_BRACE=146, CLOSE_BRACE=147, OPEN_CURLY_BRACE=148, CLOSE_CURLY_BRACE=149, 
		OPEN_SQUARE_BRACKET=150, CLOSE_SQUARE_BRACKET=151, SEMICOLON=152, DOT=153, 
		PLUS_SIGN=154, MINUS_SIGN=155, ASTERISK=156, QUESTION_MARK=157, COMMA=158, 
		NEGATION=159, DIVIDE=160, EQUAL=161, LESS=162, GREATER=163, PIPE=164, 
		INVALID=165, ANY=166;
	public static final int
		RULE_query = 0, RULE_prologue = 1, RULE_defineDecl = 2, RULE_defineValue = 3, 
		RULE_baseDecl = 4, RULE_prefixDecl = 5, RULE_selectQuery = 6, RULE_subSelect = 7, 
		RULE_selectClause = 8, RULE_selectVariable = 9, RULE_constructQuery = 10, 
		RULE_describeQuery = 11, RULE_askQuery = 12, RULE_datasetClause = 13, 
		RULE_whereClause = 14, RULE_solutionModifier = 15, RULE_groupClause = 16, 
		RULE_groupCondition = 17, RULE_havingClause = 18, RULE_havingCondition = 19, 
		RULE_orderClause = 20, RULE_orderCondition = 21, RULE_limitOffsetClauses = 22, 
		RULE_limitClause = 23, RULE_offsetClause = 24, RULE_valuesClause = 25, 
		RULE_updateCommand = 26, RULE_update = 27, RULE_load = 28, RULE_clear = 29, 
		RULE_drop = 30, RULE_create = 31, RULE_add = 32, RULE_move = 33, RULE_copy = 34, 
		RULE_insertData = 35, RULE_deleteData = 36, RULE_deleteWhere = 37, RULE_modify = 38, 
		RULE_deleteClause = 39, RULE_insertClause = 40, RULE_usingClause = 41, 
		RULE_graphOrDefault = 42, RULE_graphRef = 43, RULE_graphRefAll = 44, RULE_quadPattern = 45, 
		RULE_quadData = 46, RULE_quads = 47, RULE_quadsDetails = 48, RULE_quadsNotTriples = 49, 
		RULE_triplesTemplate = 50, RULE_groupGraphPattern = 51, RULE_groupGraphPatternSub = 52, 
		RULE_groupGraphPatternSubList = 53, RULE_triplesBlock = 54, RULE_graphPatternNotTriples = 55, 
		RULE_optionalGraphPattern = 56, RULE_graphGraphPattern = 57, RULE_serviceGraphPattern = 58, 
		RULE_bind = 59, RULE_inlineData = 60, RULE_dataBlock = 61, RULE_inlineDataOneVar = 62, 
		RULE_inlineDataFull = 63, RULE_dataBlockValues = 64, RULE_dataBlockValue = 65, 
		RULE_minusGraphPattern = 66, RULE_groupOrUnionGraphPattern = 67, RULE_filter = 68, 
		RULE_constraint = 69, RULE_functionCall = 70, RULE_argList = 71, RULE_expressionList = 72, 
		RULE_constructTemplate = 73, RULE_constructTriples = 74, RULE_triplesSameSubject = 75, 
		RULE_propertyList = 76, RULE_propertyListNotEmpty = 77, RULE_verb = 78, 
		RULE_objectList = 79, RULE_object = 80, RULE_triplesSameSubjectPath = 81, 
		RULE_propertyListPath = 82, RULE_propertyListPathNotEmpty = 83, RULE_propertyListPathNotEmptyList = 84, 
		RULE_verbPath = 85, RULE_verbSimple = 86, RULE_objectListPath = 87, RULE_objectPath = 88, 
		RULE_tripleOptions = 89, RULE_tableOption = 90, RULE_path = 91, RULE_pathAlternative = 92, 
		RULE_pathSequence = 93, RULE_pathElt = 94, RULE_pathEltOrInverse = 95, 
		RULE_pathMod = 96, RULE_pathPrimary = 97, RULE_pathNegatedPropertySet = 98, 
		RULE_pathOneInPropertySet = 99, RULE_integer = 100, RULE_triplesNode = 101, 
		RULE_blankNodePropertyList = 102, RULE_triplesNodePath = 103, RULE_blankNodePropertyListPath = 104, 
		RULE_collection = 105, RULE_collectionPath = 106, RULE_graphNode = 107, 
		RULE_graphNodePath = 108, RULE_varOrTerm = 109, RULE_varOrIRI = 110, RULE_var = 111, 
		RULE_graphTerm = 112, RULE_nil = 113, RULE_expression = 114, RULE_unaryLiteralExpression = 115, 
		RULE_unaryExpression = 116, RULE_primaryExpression = 117, RULE_builtInCall = 118, 
		RULE_regexExpression = 119, RULE_subStringExpression = 120, RULE_strReplaceExpression = 121, 
		RULE_existsFunction = 122, RULE_notExistsFunction = 123, RULE_aggregate = 124, 
		RULE_iriRefOrFunction = 125, RULE_rdfLiteral = 126, RULE_numericLiteral = 127, 
		RULE_numericLiteralUnsigned = 128, RULE_numericLiteralPositive = 129, 
		RULE_numericLiteralNegative = 130, RULE_booleanLiteral = 131, RULE_string = 132, 
		RULE_iri = 133, RULE_prefixedName = 134, RULE_blankNode = 135, RULE_anon = 136;
	public static final String[] ruleNames = {
		"query", "prologue", "defineDecl", "defineValue", "baseDecl", "prefixDecl", 
		"selectQuery", "subSelect", "selectClause", "selectVariable", "constructQuery", 
		"describeQuery", "askQuery", "datasetClause", "whereClause", "solutionModifier", 
		"groupClause", "groupCondition", "havingClause", "havingCondition", "orderClause", 
		"orderCondition", "limitOffsetClauses", "limitClause", "offsetClause", 
		"valuesClause", "updateCommand", "update", "load", "clear", "drop", "create", 
		"add", "move", "copy", "insertData", "deleteData", "deleteWhere", "modify", 
		"deleteClause", "insertClause", "usingClause", "graphOrDefault", "graphRef", 
		"graphRefAll", "quadPattern", "quadData", "quads", "quadsDetails", "quadsNotTriples", 
		"triplesTemplate", "groupGraphPattern", "groupGraphPatternSub", "groupGraphPatternSubList", 
		"triplesBlock", "graphPatternNotTriples", "optionalGraphPattern", "graphGraphPattern", 
		"serviceGraphPattern", "bind", "inlineData", "dataBlock", "inlineDataOneVar", 
		"inlineDataFull", "dataBlockValues", "dataBlockValue", "minusGraphPattern", 
		"groupOrUnionGraphPattern", "filter", "constraint", "functionCall", "argList", 
		"expressionList", "constructTemplate", "constructTriples", "triplesSameSubject", 
		"propertyList", "propertyListNotEmpty", "verb", "objectList", "object", 
		"triplesSameSubjectPath", "propertyListPath", "propertyListPathNotEmpty", 
		"propertyListPathNotEmptyList", "verbPath", "verbSimple", "objectListPath", 
		"objectPath", "tripleOptions", "tableOption", "path", "pathAlternative", 
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
		null, "'a'", null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, "'^^'", "'<='", "'>='", "'!='", 
		"'&&'", "'||'", "'^'", "'('", "')'", "'{'", "'}'", "'['", "']'", "';'", 
		"'.'", "'+'", "'-'", "'*'", "'?'", "','", "'!'", "'/'", "'='", "'<'", 
		"'>'", "'|'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "DEFINE", "BASE", "PREFIX", "SELECT", "DISTINCT", "REDUCED", 
		"CONSTRUCT", "DESCRIBE", "ASK", "FROM", "NAMED", "WHERE", "ORDER", "BY", 
		"ASC", "DESC", "LIMIT", "OFFSET", "VALUES", "OPTIONAL", "GRAPH", "UNION", 
		"FILTER", "A", "STR", "LANG", "LANGMATCHES", "DATATYPE", "BOUND", "SAMETERM", 
		"ISIRI", "ISURI", "ISBLANK", "ISLITERAL", "REGEX", "SUBSTR", "TRUE", "FALSE", 
		"LOAD", "CLEAR", "DROP", "ADD", "MOVE", "COPY", "CREATE", "DELETE", "INSERT", 
		"USING", "SILENT", "DEFAULT", "ALL", "DATA", "WITH", "INTO", "TO", "AS", 
		"GROUP", "HAVING", "UNDEF", "BINDINGS", "SERVICE", "BIND", "MINUS", "IRI", 
		"URI", "BNODE", "RAND", "ABS", "CEIL", "FLOOR", "ROUND", "CONCAT", "STRLEN", 
		"UCASE", "LCASE", "ENCODE_FOR_URI", "CONTAINS", "STRSTARTS", "STRENDS", 
		"STRBEFORE", "STRAFTER", "REPLACE", "YEAR", "MONTH", "DAY", "HOURS", "MINUTES", 
		"SECONDS", "TIMEZONE", "TZ", "NOW", "UUID", "STRUUID", "MD5", "SHA1", 
		"SHA256", "SHA384", "SHA512", "COALESCE", "IF", "STRLANG", "STRDT", "ISNUMERIC", 
		"COUNT", "SUM", "MIN", "MAX", "AVG", "SAMPLE", "GROUP_CONCAT", "NOT", 
		"IN", "EXISTS", "SEPARATOR", "OPTION", "TABLE_OPTION", "IRIREF", "PNAME_NS", 
		"PNAME_LN", "BLANK_NODE_LABEL", "VAR1", "VAR2", "LANGTAG", "INTEGER", 
		"DECIMAL", "DOUBLE", "INTEGER_POSITIVE", "DECIMAL_POSITIVE", "DOUBLE_POSITIVE", 
		"INTEGER_NEGATIVE", "DECIMAL_NEGATIVE", "DOUBLE_NEGATIVE", "STRING_LITERAL1", 
		"STRING_LITERAL2", "STRING_LITERAL_LONG1", "STRING_LITERAL_LONG2", "COMMENT", 
		"REFERENCE", "LESS_EQUAL", "GREATER_EQUAL", "NOT_EQUAL", "AND", "OR", 
		"INVERSE", "OPEN_BRACE", "CLOSE_BRACE", "OPEN_CURLY_BRACE", "CLOSE_CURLY_BRACE", 
		"OPEN_SQUARE_BRACKET", "CLOSE_SQUARE_BRACKET", "SEMICOLON", "DOT", "PLUS_SIGN", 
		"MINUS_SIGN", "ASTERISK", "QUESTION_MARK", "COMMA", "NEGATION", "DIVIDE", 
		"EQUAL", "LESS", "GREATER", "PIPE", "INVALID", "ANY"
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuery(this);
		}
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
			setState(288);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(274);
				prologue();
				setState(279);
				switch (_input.LA(1)) {
				case SELECT:
					{
					setState(275);
					selectQuery();
					}
					break;
				case CONSTRUCT:
					{
					setState(276);
					constructQuery();
					}
					break;
				case DESCRIBE:
					{
					setState(277);
					describeQuery();
					}
					break;
				case ASK:
					{
					setState(278);
					askQuery();
					}
					break;
				case EOF:
				case VALUES:
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(281);
				valuesClause();
				setState(282);
				match(EOF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(285);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
				case 1:
					{
					setState(284);
					updateCommand();
					}
					break;
				}
				setState(287);
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
		public List<DefineDeclContext> defineDecl() {
			return getRuleContexts(DefineDeclContext.class);
		}
		public DefineDeclContext defineDecl(int i) {
			return getRuleContext(DefineDeclContext.class,i);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPrologue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPrologue(this);
		}
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
			setState(293);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DEFINE) {
				{
				{
				setState(290);
				defineDecl();
				}
				}
				setState(295);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(300);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BASE || _la==PREFIX) {
				{
				setState(298);
				switch (_input.LA(1)) {
				case BASE:
					{
					setState(296);
					baseDecl();
					}
					break;
				case PREFIX:
					{
					setState(297);
					prefixDecl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(302);
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

	public static class DefineDeclContext extends ParserRuleContext {
		public TerminalNode DEFINE() { return getToken(SparqlParser.DEFINE, 0); }
		public PrefixedNameContext prefixedName() {
			return getRuleContext(PrefixedNameContext.class,0);
		}
		public List<DefineValueContext> defineValue() {
			return getRuleContexts(DefineValueContext.class);
		}
		public DefineValueContext defineValue(int i) {
			return getRuleContext(DefineValueContext.class,i);
		}
		public DefineDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defineDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDefineDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDefineDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDefineDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefineDeclContext defineDecl() throws RecognitionException {
		DefineDeclContext _localctx = new DefineDeclContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_defineDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(303);
			match(DEFINE);
			setState(304);
			prefixedName();
			setState(305);
			defineValue();
			setState(310);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(306);
				match(COMMA);
				setState(307);
				defineValue();
				}
				}
				setState(312);
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

	public static class DefineValueContext extends ParserRuleContext {
		public IriContext iri() {
			return getRuleContext(IriContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TerminalNode INTEGER() { return getToken(SparqlParser.INTEGER, 0); }
		public DefineValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defineValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDefineValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDefineValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDefineValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefineValueContext defineValue() throws RecognitionException {
		DefineValueContext _localctx = new DefineValueContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_defineValue);
		try {
			setState(316);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(313);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(314);
				string();
				}
				break;
			case INTEGER:
				enterOuterAlt(_localctx, 3);
				{
				setState(315);
				match(INTEGER);
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

	public static class BaseDeclContext extends ParserRuleContext {
		public TerminalNode BASE() { return getToken(SparqlParser.BASE, 0); }
		public TerminalNode IRIREF() { return getToken(SparqlParser.IRIREF, 0); }
		public BaseDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_baseDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBaseDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBaseDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBaseDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BaseDeclContext baseDecl() throws RecognitionException {
		BaseDeclContext _localctx = new BaseDeclContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_baseDecl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			match(BASE);
			setState(319);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPrefixDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPrefixDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrefixDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrefixDeclContext prefixDecl() throws RecognitionException {
		PrefixDeclContext _localctx = new PrefixDeclContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_prefixDecl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(321);
			match(PREFIX);
			setState(322);
			match(PNAME_NS);
			setState(323);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSelectQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSelectQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectQueryContext selectQuery() throws RecognitionException {
		SelectQueryContext _localctx = new SelectQueryContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_selectQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			selectClause();
			setState(329);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(326);
				datasetClause();
				}
				}
				setState(331);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(332);
			whereClause();
			setState(333);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSubSelect(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSubSelect(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSubSelect(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubSelectContext subSelect() throws RecognitionException {
		SubSelectContext _localctx = new SubSelectContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_subSelect);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(335);
			selectClause();
			setState(336);
			whereClause();
			setState(337);
			solutionModifier();
			setState(338);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSelectClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSelectClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_selectClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			match(SELECT);
			setState(342);
			_la = _input.LA(1);
			if (_la==DISTINCT || _la==REDUCED) {
				{
				setState(341);
				_la = _input.LA(1);
				if ( !(_la==DISTINCT || _la==REDUCED) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(350);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
			case OPEN_BRACE:
				{
				setState(345); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(344);
					selectVariable();
					}
					}
					setState(347); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 122)) & ~0x3f) == 0 && ((1L << (_la - 122)) & ((1L << (VAR1 - 122)) | (1L << (VAR2 - 122)) | (1L << (OPEN_BRACE - 122)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(349);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSelectVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSelectVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSelectVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectVariableContext selectVariable() throws RecognitionException {
		SelectVariableContext _localctx = new SelectVariableContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_selectVariable);
		try {
			setState(359);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(352);
				var();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(353);
				match(OPEN_BRACE);
				setState(354);
				expression(0);
				setState(355);
				match(AS);
				setState(356);
				var();
				setState(357);
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
		public TriplesTemplateContext triplesTemplate() {
			return getRuleContext(TriplesTemplateContext.class,0);
		}
		public ConstructQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConstructQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConstructQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructQueryContext constructQuery() throws RecognitionException {
		ConstructQueryContext _localctx = new ConstructQueryContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_constructQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			match(CONSTRUCT);
			setState(385);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				{
				setState(362);
				constructTemplate();
				setState(366);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(363);
					datasetClause();
					}
					}
					setState(368);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(369);
				whereClause();
				setState(370);
				solutionModifier();
				}
				break;
			case FROM:
			case WHERE:
				{
				setState(375);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(372);
					datasetClause();
					}
					}
					setState(377);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(378);
				match(WHERE);
				setState(379);
				match(OPEN_CURLY_BRACE);
				setState(381);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
					{
					setState(380);
					triplesTemplate();
					}
				}

				setState(383);
				match(CLOSE_CURLY_BRACE);
				setState(384);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDescribeQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDescribeQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDescribeQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeQueryContext describeQuery() throws RecognitionException {
		DescribeQueryContext _localctx = new DescribeQueryContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_describeQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			match(DESCRIBE);
			setState(394);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				{
				setState(389); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(388);
					varOrIRI();
					}
					}
					setState(391); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(393);
				match(ASTERISK);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(399);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(396);
				datasetClause();
				}
				}
				setState(401);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(403);
			_la = _input.LA(1);
			if (_la==WHERE || _la==OPEN_CURLY_BRACE) {
				{
				setState(402);
				whereClause();
				}
			}

			setState(405);
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
		public SolutionModifierContext solutionModifier() {
			return getRuleContext(SolutionModifierContext.class,0);
		}
		public List<DatasetClauseContext> datasetClause() {
			return getRuleContexts(DatasetClauseContext.class);
		}
		public DatasetClauseContext datasetClause(int i) {
			return getRuleContext(DatasetClauseContext.class,i);
		}
		public AskQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_askQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterAskQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitAskQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAskQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AskQueryContext askQuery() throws RecognitionException {
		AskQueryContext _localctx = new AskQueryContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_askQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
			match(ASK);
			setState(411);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(408);
				datasetClause();
				}
				}
				setState(413);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(414);
			whereClause();
			setState(415);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDatasetClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDatasetClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDatasetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DatasetClauseContext datasetClause() throws RecognitionException {
		DatasetClauseContext _localctx = new DatasetClauseContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_datasetClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(417);
			match(FROM);
			setState(419);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(418);
				match(NAMED);
				}
			}

			setState(421);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitWhereClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitWhereClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_whereClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(424);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(423);
				match(WHERE);
				}
			}

			setState(426);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSolutionModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSolutionModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSolutionModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SolutionModifierContext solutionModifier() throws RecognitionException {
		SolutionModifierContext _localctx = new SolutionModifierContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_solutionModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(429);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(428);
				groupClause();
				}
			}

			setState(432);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(431);
				havingClause();
				}
			}

			setState(435);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(434);
				orderClause();
				}
			}

			setState(438);
			_la = _input.LA(1);
			if (_la==LIMIT || _la==OFFSET) {
				{
				setState(437);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupClauseContext groupClause() throws RecognitionException {
		GroupClauseContext _localctx = new GroupClauseContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_groupClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(440);
			match(GROUP);
			setState(441);
			match(BY);
			setState(443); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(442);
				groupCondition();
				}
				}
				setState(445); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 26)) & ~0x3f) == 0 && ((1L << (_la - 26)) & ((1L << (STR - 26)) | (1L << (LANG - 26)) | (1L << (LANGMATCHES - 26)) | (1L << (DATATYPE - 26)) | (1L << (BOUND - 26)) | (1L << (SAMETERM - 26)) | (1L << (ISIRI - 26)) | (1L << (ISURI - 26)) | (1L << (ISBLANK - 26)) | (1L << (ISLITERAL - 26)) | (1L << (REGEX - 26)) | (1L << (SUBSTR - 26)) | (1L << (IRI - 26)) | (1L << (URI - 26)) | (1L << (BNODE - 26)) | (1L << (RAND - 26)) | (1L << (ABS - 26)) | (1L << (CEIL - 26)) | (1L << (FLOOR - 26)) | (1L << (ROUND - 26)) | (1L << (CONCAT - 26)) | (1L << (STRLEN - 26)) | (1L << (UCASE - 26)) | (1L << (LCASE - 26)) | (1L << (ENCODE_FOR_URI - 26)) | (1L << (CONTAINS - 26)) | (1L << (STRSTARTS - 26)) | (1L << (STRENDS - 26)) | (1L << (STRBEFORE - 26)) | (1L << (STRAFTER - 26)) | (1L << (REPLACE - 26)) | (1L << (YEAR - 26)) | (1L << (MONTH - 26)) | (1L << (DAY - 26)) | (1L << (HOURS - 26)) | (1L << (MINUTES - 26)) | (1L << (SECONDS - 26)))) != 0) || ((((_la - 90)) & ~0x3f) == 0 && ((1L << (_la - 90)) & ((1L << (TIMEZONE - 90)) | (1L << (TZ - 90)) | (1L << (NOW - 90)) | (1L << (UUID - 90)) | (1L << (STRUUID - 90)) | (1L << (MD5 - 90)) | (1L << (SHA1 - 90)) | (1L << (SHA256 - 90)) | (1L << (SHA384 - 90)) | (1L << (SHA512 - 90)) | (1L << (COALESCE - 90)) | (1L << (IF - 90)) | (1L << (STRLANG - 90)) | (1L << (STRDT - 90)) | (1L << (ISNUMERIC - 90)) | (1L << (COUNT - 90)) | (1L << (SUM - 90)) | (1L << (MIN - 90)) | (1L << (MAX - 90)) | (1L << (AVG - 90)) | (1L << (SAMPLE - 90)) | (1L << (GROUP_CONCAT - 90)) | (1L << (NOT - 90)) | (1L << (EXISTS - 90)) | (1L << (IRIREF - 90)) | (1L << (PNAME_NS - 90)) | (1L << (PNAME_LN - 90)) | (1L << (VAR1 - 90)) | (1L << (VAR2 - 90)) | (1L << (OPEN_BRACE - 90)))) != 0) );
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupConditionContext groupCondition() throws RecognitionException {
		GroupConditionContext _localctx = new GroupConditionContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_groupCondition);
		int _la;
		try {
			setState(458);
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
				setState(447);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(448);
				functionCall();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(449);
				match(OPEN_BRACE);
				setState(450);
				expression(0);
				setState(453);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(451);
					match(AS);
					setState(452);
					var();
					}
				}

				setState(455);
				match(CLOSE_BRACE);
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 4);
				{
				setState(457);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterHavingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitHavingClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitHavingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_havingClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(460);
			match(HAVING);
			setState(462); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(461);
				havingCondition();
				}
				}
				setState(464); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 26)) & ~0x3f) == 0 && ((1L << (_la - 26)) & ((1L << (STR - 26)) | (1L << (LANG - 26)) | (1L << (LANGMATCHES - 26)) | (1L << (DATATYPE - 26)) | (1L << (BOUND - 26)) | (1L << (SAMETERM - 26)) | (1L << (ISIRI - 26)) | (1L << (ISURI - 26)) | (1L << (ISBLANK - 26)) | (1L << (ISLITERAL - 26)) | (1L << (REGEX - 26)) | (1L << (SUBSTR - 26)) | (1L << (IRI - 26)) | (1L << (URI - 26)) | (1L << (BNODE - 26)) | (1L << (RAND - 26)) | (1L << (ABS - 26)) | (1L << (CEIL - 26)) | (1L << (FLOOR - 26)) | (1L << (ROUND - 26)) | (1L << (CONCAT - 26)) | (1L << (STRLEN - 26)) | (1L << (UCASE - 26)) | (1L << (LCASE - 26)) | (1L << (ENCODE_FOR_URI - 26)) | (1L << (CONTAINS - 26)) | (1L << (STRSTARTS - 26)) | (1L << (STRENDS - 26)) | (1L << (STRBEFORE - 26)) | (1L << (STRAFTER - 26)) | (1L << (REPLACE - 26)) | (1L << (YEAR - 26)) | (1L << (MONTH - 26)) | (1L << (DAY - 26)) | (1L << (HOURS - 26)) | (1L << (MINUTES - 26)) | (1L << (SECONDS - 26)))) != 0) || ((((_la - 90)) & ~0x3f) == 0 && ((1L << (_la - 90)) & ((1L << (TIMEZONE - 90)) | (1L << (TZ - 90)) | (1L << (NOW - 90)) | (1L << (UUID - 90)) | (1L << (STRUUID - 90)) | (1L << (MD5 - 90)) | (1L << (SHA1 - 90)) | (1L << (SHA256 - 90)) | (1L << (SHA384 - 90)) | (1L << (SHA512 - 90)) | (1L << (COALESCE - 90)) | (1L << (IF - 90)) | (1L << (STRLANG - 90)) | (1L << (STRDT - 90)) | (1L << (ISNUMERIC - 90)) | (1L << (COUNT - 90)) | (1L << (SUM - 90)) | (1L << (MIN - 90)) | (1L << (MAX - 90)) | (1L << (AVG - 90)) | (1L << (SAMPLE - 90)) | (1L << (GROUP_CONCAT - 90)) | (1L << (NOT - 90)) | (1L << (EXISTS - 90)) | (1L << (IRIREF - 90)) | (1L << (PNAME_NS - 90)) | (1L << (PNAME_LN - 90)) | (1L << (OPEN_BRACE - 90)))) != 0) );
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterHavingCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitHavingCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitHavingCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingConditionContext havingCondition() throws RecognitionException {
		HavingConditionContext _localctx = new HavingConditionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_havingCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(466);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterOrderClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitOrderClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOrderClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderClauseContext orderClause() throws RecognitionException {
		OrderClauseContext _localctx = new OrderClauseContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_orderClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			match(ORDER);
			setState(469);
			match(BY);
			setState(471); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(470);
				orderCondition();
				}
				}
				setState(473); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ASC) | (1L << DESC) | (1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR))) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & ((1L << (IRI - 65)) | (1L << (URI - 65)) | (1L << (BNODE - 65)) | (1L << (RAND - 65)) | (1L << (ABS - 65)) | (1L << (CEIL - 65)) | (1L << (FLOOR - 65)) | (1L << (ROUND - 65)) | (1L << (CONCAT - 65)) | (1L << (STRLEN - 65)) | (1L << (UCASE - 65)) | (1L << (LCASE - 65)) | (1L << (ENCODE_FOR_URI - 65)) | (1L << (CONTAINS - 65)) | (1L << (STRSTARTS - 65)) | (1L << (STRENDS - 65)) | (1L << (STRBEFORE - 65)) | (1L << (STRAFTER - 65)) | (1L << (REPLACE - 65)) | (1L << (YEAR - 65)) | (1L << (MONTH - 65)) | (1L << (DAY - 65)) | (1L << (HOURS - 65)) | (1L << (MINUTES - 65)) | (1L << (SECONDS - 65)) | (1L << (TIMEZONE - 65)) | (1L << (TZ - 65)) | (1L << (NOW - 65)) | (1L << (UUID - 65)) | (1L << (STRUUID - 65)) | (1L << (MD5 - 65)) | (1L << (SHA1 - 65)) | (1L << (SHA256 - 65)) | (1L << (SHA384 - 65)) | (1L << (SHA512 - 65)) | (1L << (COALESCE - 65)) | (1L << (IF - 65)) | (1L << (STRLANG - 65)) | (1L << (STRDT - 65)) | (1L << (ISNUMERIC - 65)) | (1L << (COUNT - 65)) | (1L << (SUM - 65)) | (1L << (MIN - 65)) | (1L << (MAX - 65)) | (1L << (AVG - 65)) | (1L << (SAMPLE - 65)) | (1L << (GROUP_CONCAT - 65)) | (1L << (NOT - 65)) | (1L << (EXISTS - 65)) | (1L << (IRIREF - 65)) | (1L << (PNAME_NS - 65)) | (1L << (PNAME_LN - 65)) | (1L << (VAR1 - 65)) | (1L << (VAR2 - 65)))) != 0) || _la==OPEN_BRACE );
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterOrderCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitOrderCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOrderCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderConditionContext orderCondition() throws RecognitionException {
		OrderConditionContext _localctx = new OrderConditionContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_orderCondition);
		int _la;
		try {
			setState(482);
			switch (_input.LA(1)) {
			case ASC:
			case DESC:
				enterOuterAlt(_localctx, 1);
				{
				setState(475);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(476);
				match(OPEN_BRACE);
				setState(477);
				expression(0);
				setState(478);
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
				setState(480);
				constraint();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 3);
				{
				setState(481);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterLimitOffsetClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitLimitOffsetClauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLimitOffsetClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitOffsetClausesContext limitOffsetClauses() throws RecognitionException {
		LimitOffsetClausesContext _localctx = new LimitOffsetClausesContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_limitOffsetClauses);
		int _la;
		try {
			setState(492);
			switch (_input.LA(1)) {
			case LIMIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(484);
				limitClause();
				setState(486);
				_la = _input.LA(1);
				if (_la==OFFSET) {
					{
					setState(485);
					offsetClause();
					}
				}

				}
				break;
			case OFFSET:
				enterOuterAlt(_localctx, 2);
				{
				setState(488);
				offsetClause();
				setState(490);
				_la = _input.LA(1);
				if (_la==LIMIT) {
					{
					setState(489);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterLimitClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitLimitClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLimitClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitClauseContext limitClause() throws RecognitionException {
		LimitClauseContext _localctx = new LimitClauseContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_limitClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(494);
			match(LIMIT);
			setState(495);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterOffsetClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitOffsetClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOffsetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OffsetClauseContext offsetClause() throws RecognitionException {
		OffsetClauseContext _localctx = new OffsetClauseContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_offsetClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(497);
			match(OFFSET);
			setState(498);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterValuesClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitValuesClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitValuesClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValuesClauseContext valuesClause() throws RecognitionException {
		ValuesClauseContext _localctx = new ValuesClauseContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_valuesClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(502);
			_la = _input.LA(1);
			if (_la==VALUES) {
				{
				setState(500);
				match(VALUES);
				setState(501);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUpdateCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUpdateCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUpdateCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateCommandContext updateCommand() throws RecognitionException {
		UpdateCommandContext _localctx = new UpdateCommandContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_updateCommand);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(504);
			prologue();
			setState(519);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LOAD) | (1L << CLEAR) | (1L << DROP) | (1L << ADD) | (1L << MOVE) | (1L << COPY) | (1L << CREATE) | (1L << DELETE) | (1L << INSERT) | (1L << WITH))) != 0)) {
				{
				setState(505);
				update();
				setState(512);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(506);
						match(SEMICOLON);
						setState(507);
						prologue();
						setState(508);
						update();
						}
						} 
					}
					setState(514);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
				}
				setState(517);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(515);
					match(SEMICOLON);
					setState(516);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUpdate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUpdate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUpdate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateContext update() throws RecognitionException {
		UpdateContext _localctx = new UpdateContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_update);
		try {
			setState(532);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(521);
				load();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(522);
				clear();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(523);
				drop();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(524);
				add();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(525);
				move();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(526);
				copy();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(527);
				create();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(528);
				insertData();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(529);
				deleteData();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(530);
				deleteWhere();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(531);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterLoad(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitLoad(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitLoad(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LoadContext load() throws RecognitionException {
		LoadContext _localctx = new LoadContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_load);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(534);
			match(LOAD);
			setState(536);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(535);
				match(SILENT);
				}
			}

			setState(538);
			iri();
			setState(541);
			_la = _input.LA(1);
			if (_la==INTO) {
				{
				setState(539);
				match(INTO);
				setState(540);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterClear(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitClear(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitClear(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClearContext clear() throws RecognitionException {
		ClearContext _localctx = new ClearContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_clear);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(543);
			match(CLEAR);
			setState(545);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(544);
				match(SILENT);
				}
			}

			setState(547);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDrop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDrop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDrop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DropContext drop() throws RecognitionException {
		DropContext _localctx = new DropContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_drop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(549);
			match(DROP);
			setState(551);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(550);
				match(SILENT);
				}
			}

			setState(553);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterCreate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitCreate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCreate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateContext create() throws RecognitionException {
		CreateContext _localctx = new CreateContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_create);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(555);
			match(CREATE);
			setState(557);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(556);
				match(SILENT);
				}
			}

			setState(559);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterAdd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitAdd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAdd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AddContext add() throws RecognitionException {
		AddContext _localctx = new AddContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_add);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(561);
			match(ADD);
			setState(563);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(562);
				match(SILENT);
				}
			}

			setState(565);
			graphOrDefault();
			setState(566);
			match(TO);
			setState(567);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterMove(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitMove(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitMove(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MoveContext move() throws RecognitionException {
		MoveContext _localctx = new MoveContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_move);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			match(MOVE);
			setState(571);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(570);
				match(SILENT);
				}
			}

			setState(573);
			graphOrDefault();
			setState(574);
			match(TO);
			setState(575);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterCopy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitCopy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCopy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopyContext copy() throws RecognitionException {
		CopyContext _localctx = new CopyContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_copy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(577);
			match(COPY);
			setState(579);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(578);
				match(SILENT);
				}
			}

			setState(581);
			graphOrDefault();
			setState(582);
			match(TO);
			setState(583);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInsertData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInsertData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInsertData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertDataContext insertData() throws RecognitionException {
		InsertDataContext _localctx = new InsertDataContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_insertData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(585);
			match(INSERT);
			setState(586);
			match(DATA);
			setState(587);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDeleteData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDeleteData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteDataContext deleteData() throws RecognitionException {
		DeleteDataContext _localctx = new DeleteDataContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_deleteData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(589);
			match(DELETE);
			setState(590);
			match(DATA);
			setState(591);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDeleteWhere(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDeleteWhere(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteWhere(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteWhereContext deleteWhere() throws RecognitionException {
		DeleteWhereContext _localctx = new DeleteWhereContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_deleteWhere);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(593);
			match(DELETE);
			setState(594);
			match(WHERE);
			setState(595);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterModify(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitModify(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitModify(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifyContext modify() throws RecognitionException {
		ModifyContext _localctx = new ModifyContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_modify);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(599);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(597);
				match(WITH);
				setState(598);
				iri();
				}
			}

			setState(606);
			switch (_input.LA(1)) {
			case DELETE:
				{
				setState(601);
				deleteClause();
				setState(603);
				_la = _input.LA(1);
				if (_la==INSERT) {
					{
					setState(602);
					insertClause();
					}
				}

				}
				break;
			case INSERT:
				{
				setState(605);
				insertClause();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(611);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==USING) {
				{
				{
				setState(608);
				usingClause();
				}
				}
				setState(613);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(614);
			match(WHERE);
			setState(615);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDeleteClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDeleteClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDeleteClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeleteClauseContext deleteClause() throws RecognitionException {
		DeleteClauseContext _localctx = new DeleteClauseContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_deleteClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(617);
			match(DELETE);
			setState(618);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInsertClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInsertClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInsertClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertClauseContext insertClause() throws RecognitionException {
		InsertClauseContext _localctx = new InsertClauseContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_insertClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(620);
			match(INSERT);
			setState(621);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUsingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUsingClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUsingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UsingClauseContext usingClause() throws RecognitionException {
		UsingClauseContext _localctx = new UsingClauseContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_usingClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(623);
			match(USING);
			setState(625);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(624);
				match(NAMED);
				}
			}

			setState(627);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphOrDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphOrDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphOrDefault(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphOrDefaultContext graphOrDefault() throws RecognitionException {
		GraphOrDefaultContext _localctx = new GraphOrDefaultContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_graphOrDefault);
		int _la;
		try {
			setState(634);
			switch (_input.LA(1)) {
			case DEFAULT:
				enterOuterAlt(_localctx, 1);
				{
				setState(629);
				match(DEFAULT);
				}
				break;
			case GRAPH:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(631);
				_la = _input.LA(1);
				if (_la==GRAPH) {
					{
					setState(630);
					match(GRAPH);
					}
				}

				setState(633);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphRef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphRefContext graphRef() throws RecognitionException {
		GraphRefContext _localctx = new GraphRefContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_graphRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636);
			match(GRAPH);
			setState(637);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphRefAll(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphRefAll(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphRefAll(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphRefAllContext graphRefAll() throws RecognitionException {
		GraphRefAllContext _localctx = new GraphRefAllContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_graphRefAll);
		try {
			setState(643);
			switch (_input.LA(1)) {
			case GRAPH:
				enterOuterAlt(_localctx, 1);
				{
				setState(639);
				graphRef();
				}
				break;
			case DEFAULT:
				enterOuterAlt(_localctx, 2);
				{
				setState(640);
				match(DEFAULT);
				}
				break;
			case NAMED:
				enterOuterAlt(_localctx, 3);
				{
				setState(641);
				match(NAMED);
				}
				break;
			case ALL:
				enterOuterAlt(_localctx, 4);
				{
				setState(642);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuadPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuadPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadPatternContext quadPattern() throws RecognitionException {
		QuadPatternContext _localctx = new QuadPatternContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_quadPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(645);
			match(OPEN_CURLY_BRACE);
			setState(646);
			quads();
			setState(647);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuadData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuadData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadDataContext quadData() throws RecognitionException {
		QuadDataContext _localctx = new QuadDataContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_quadData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(649);
			match(OPEN_CURLY_BRACE);
			setState(650);
			quads();
			setState(651);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuads(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuads(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuads(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsContext quads() throws RecognitionException {
		QuadsContext _localctx = new QuadsContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_quads);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(654);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(653);
				triplesTemplate();
				}
			}

			setState(659);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==GRAPH) {
				{
				{
				setState(656);
				quadsDetails();
				}
				}
				setState(661);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuadsDetails(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuadsDetails(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadsDetails(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsDetailsContext quadsDetails() throws RecognitionException {
		QuadsDetailsContext _localctx = new QuadsDetailsContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_quadsDetails);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(662);
			quadsNotTriples();
			setState(664);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(663);
				match(DOT);
				}
			}

			setState(667);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(666);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterQuadsNotTriples(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitQuadsNotTriples(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitQuadsNotTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuadsNotTriplesContext quadsNotTriples() throws RecognitionException {
		QuadsNotTriplesContext _localctx = new QuadsNotTriplesContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_quadsNotTriples);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(669);
			match(GRAPH);
			setState(670);
			varOrIRI();
			setState(671);
			match(OPEN_CURLY_BRACE);
			setState(673);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(672);
				triplesTemplate();
				}
			}

			setState(675);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesTemplate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesTemplate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesTemplate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesTemplateContext triplesTemplate() throws RecognitionException {
		TriplesTemplateContext _localctx = new TriplesTemplateContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_triplesTemplate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(677);
			triplesSameSubject();
			setState(684);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(678);
				match(DOT);
				setState(680);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
					{
					setState(679);
					triplesSameSubject();
					}
				}

				}
				}
				setState(686);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternContext groupGraphPattern() throws RecognitionException {
		GroupGraphPatternContext _localctx = new GroupGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_groupGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(687);
			match(OPEN_CURLY_BRACE);
			setState(690);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(688);
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
				setState(689);
				groupGraphPatternSub();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(692);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupGraphPatternSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupGraphPatternSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPatternSub(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternSubContext groupGraphPatternSub() throws RecognitionException {
		GroupGraphPatternSubContext _localctx = new GroupGraphPatternSubContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_groupGraphPatternSub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(695);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(694);
				triplesBlock();
				}
			}

			setState(700);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 20)) & ~0x3f) == 0 && ((1L << (_la - 20)) & ((1L << (VALUES - 20)) | (1L << (OPTIONAL - 20)) | (1L << (GRAPH - 20)) | (1L << (FILTER - 20)) | (1L << (SERVICE - 20)) | (1L << (BIND - 20)) | (1L << (MINUS - 20)))) != 0) || _la==OPEN_CURLY_BRACE) {
				{
				{
				setState(697);
				groupGraphPatternSubList();
				}
				}
				setState(702);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupGraphPatternSubList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupGraphPatternSubList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupGraphPatternSubList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupGraphPatternSubListContext groupGraphPatternSubList() throws RecognitionException {
		GroupGraphPatternSubListContext _localctx = new GroupGraphPatternSubListContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_groupGraphPatternSubList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(703);
			graphPatternNotTriples();
			setState(705);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(704);
				match(DOT);
				}
			}

			setState(708);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(707);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesBlockContext triplesBlock() throws RecognitionException {
		TriplesBlockContext _localctx = new TriplesBlockContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_triplesBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(710);
			triplesSameSubjectPath();
			setState(717);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(711);
				match(DOT);
				setState(713);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
					{
					setState(712);
					triplesSameSubjectPath();
					}
				}

				}
				}
				setState(719);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphPatternNotTriples(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphPatternNotTriples(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphPatternNotTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphPatternNotTriplesContext graphPatternNotTriples() throws RecognitionException {
		GraphPatternNotTriplesContext _localctx = new GraphPatternNotTriplesContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_graphPatternNotTriples);
		try {
			setState(728);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(720);
				groupOrUnionGraphPattern();
				}
				break;
			case OPTIONAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(721);
				optionalGraphPattern();
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(722);
				minusGraphPattern();
				}
				break;
			case GRAPH:
				enterOuterAlt(_localctx, 4);
				{
				setState(723);
				graphGraphPattern();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 5);
				{
				setState(724);
				serviceGraphPattern();
				}
				break;
			case FILTER:
				enterOuterAlt(_localctx, 6);
				{
				setState(725);
				filter();
				}
				break;
			case BIND:
				enterOuterAlt(_localctx, 7);
				{
				setState(726);
				bind();
				}
				break;
			case VALUES:
				enterOuterAlt(_localctx, 8);
				{
				setState(727);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterOptionalGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitOptionalGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitOptionalGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalGraphPatternContext optionalGraphPattern() throws RecognitionException {
		OptionalGraphPatternContext _localctx = new OptionalGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_optionalGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(730);
			match(OPTIONAL);
			setState(731);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphGraphPatternContext graphGraphPattern() throws RecognitionException {
		GraphGraphPatternContext _localctx = new GraphGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_graphGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(733);
			match(GRAPH);
			setState(734);
			varOrIRI();
			setState(735);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterServiceGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitServiceGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitServiceGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ServiceGraphPatternContext serviceGraphPattern() throws RecognitionException {
		ServiceGraphPatternContext _localctx = new ServiceGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_serviceGraphPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(737);
			match(SERVICE);
			setState(739);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(738);
				match(SILENT);
				}
			}

			setState(741);
			varOrIRI();
			setState(742);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBind(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBind(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBind(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BindContext bind() throws RecognitionException {
		BindContext _localctx = new BindContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_bind);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(744);
			match(BIND);
			setState(745);
			match(OPEN_BRACE);
			setState(746);
			expression(0);
			setState(747);
			match(AS);
			setState(748);
			var();
			setState(749);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInlineData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInlineData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataContext inlineData() throws RecognitionException {
		InlineDataContext _localctx = new InlineDataContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_inlineData);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(751);
			match(VALUES);
			setState(752);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDataBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDataBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockContext dataBlock() throws RecognitionException {
		DataBlockContext _localctx = new DataBlockContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_dataBlock);
		try {
			setState(756);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(754);
				inlineDataOneVar();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(755);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInlineDataOneVar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInlineDataOneVar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineDataOneVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataOneVarContext inlineDataOneVar() throws RecognitionException {
		InlineDataOneVarContext _localctx = new InlineDataOneVarContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_inlineDataOneVar);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(758);
			var();
			setState(759);
			match(OPEN_CURLY_BRACE);
			setState(763);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)))) != 0)) {
				{
				{
				setState(760);
				dataBlockValue();
				}
				}
				setState(765);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(766);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInlineDataFull(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInlineDataFull(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInlineDataFull(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineDataFullContext inlineDataFull() throws RecognitionException {
		InlineDataFullContext _localctx = new InlineDataFullContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_inlineDataFull);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(768);
			match(OPEN_BRACE);
			setState(772);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==VAR1 || _la==VAR2) {
				{
				{
				setState(769);
				var();
				}
				}
				setState(774);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(775);
			match(CLOSE_BRACE);
			setState(776);
			match(OPEN_CURLY_BRACE);
			setState(780);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OPEN_BRACE) {
				{
				{
				setState(777);
				dataBlockValues();
				}
				}
				setState(782);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(783);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDataBlockValues(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDataBlockValues(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlockValues(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockValuesContext dataBlockValues() throws RecognitionException {
		DataBlockValuesContext _localctx = new DataBlockValuesContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_dataBlockValues);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(785);
			match(OPEN_BRACE);
			setState(789);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)))) != 0)) {
				{
				{
				setState(786);
				dataBlockValue();
				}
				}
				setState(791);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(792);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterDataBlockValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitDataBlockValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitDataBlockValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataBlockValueContext dataBlockValue() throws RecognitionException {
		DataBlockValueContext _localctx = new DataBlockValueContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_dataBlockValue);
		try {
			setState(799);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(794);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(795);
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
				setState(796);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(797);
				booleanLiteral();
				}
				break;
			case UNDEF:
				enterOuterAlt(_localctx, 5);
				{
				setState(798);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterMinusGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitMinusGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitMinusGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MinusGraphPatternContext minusGraphPattern() throws RecognitionException {
		MinusGraphPatternContext _localctx = new MinusGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_minusGraphPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(801);
			match(MINUS);
			setState(802);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGroupOrUnionGraphPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGroupOrUnionGraphPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGroupOrUnionGraphPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupOrUnionGraphPatternContext groupOrUnionGraphPattern() throws RecognitionException {
		GroupOrUnionGraphPatternContext _localctx = new GroupOrUnionGraphPatternContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_groupOrUnionGraphPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(804);
			groupGraphPattern();
			setState(809);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION) {
				{
				{
				setState(805);
				match(UNION);
				setState(806);
				groupGraphPattern();
				}
				}
				setState(811);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitFilter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitFilter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		FilterContext _localctx = new FilterContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_filter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(812);
			match(FILTER);
			setState(813);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstraintContext constraint() throws RecognitionException {
		ConstraintContext _localctx = new ConstraintContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_constraint);
		try {
			setState(821);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(815);
				match(OPEN_BRACE);
				setState(816);
				expression(0);
				setState(817);
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
				setState(819);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(820);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallContext functionCall() throws RecognitionException {
		FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_functionCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(823);
			iri();
			setState(824);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterArgList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitArgList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitArgList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgListContext argList() throws RecognitionException {
		ArgListContext _localctx = new ArgListContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_argList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(826);
			match(OPEN_BRACE);
			setState(832);
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
				setState(828);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(827);
					match(DISTINCT);
					}
				}

				setState(830);
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
			setState(834);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterExpressionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitExpressionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitExpressionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionListContext expressionList() throws RecognitionException {
		ExpressionListContext _localctx = new ExpressionListContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_expressionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(836);
			expression(0);
			setState(841);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(837);
				match(COMMA);
				setState(838);
				expression(0);
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

	public static class ConstructTemplateContext extends ParserRuleContext {
		public ConstructTriplesContext constructTriples() {
			return getRuleContext(ConstructTriplesContext.class,0);
		}
		public ConstructTemplateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructTemplate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConstructTemplate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConstructTemplate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructTemplate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructTemplateContext constructTemplate() throws RecognitionException {
		ConstructTemplateContext _localctx = new ConstructTemplateContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_constructTemplate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(844);
			match(OPEN_CURLY_BRACE);
			setState(846);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
				{
				setState(845);
				constructTriples();
				}
			}

			setState(848);
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

	public static class ConstructTriplesContext extends ParserRuleContext {
		public TriplesSameSubjectContext triplesSameSubject() {
			return getRuleContext(TriplesSameSubjectContext.class,0);
		}
		public List<ConstructTriplesContext> constructTriples() {
			return getRuleContexts(ConstructTriplesContext.class);
		}
		public ConstructTriplesContext constructTriples(int i) {
			return getRuleContext(ConstructTriplesContext.class,i);
		}
		public ConstructTriplesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructTriples; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConstructTriples(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConstructTriples(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructTriplesContext constructTriples() throws RecognitionException {
		ConstructTriplesContext _localctx = new ConstructTriplesContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_constructTriples);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(850);
			triplesSameSubject();
			setState(857);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(851);
					match(DOT);
					setState(853);
					_la = _input.LA(1);
					if (_la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0)) {
						{
						setState(852);
						constructTriples();
						}
					}

					}
					} 
				}
				setState(859);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesSameSubject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesSameSubject(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesSameSubject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesSameSubjectContext triplesSameSubject() throws RecognitionException {
		TriplesSameSubjectContext _localctx = new TriplesSameSubjectContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_triplesSameSubject);
		try {
			setState(866);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(860);
				varOrTerm();
				setState(861);
				propertyListNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(863);
				triplesNode();
				setState(864);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListContext propertyList() throws RecognitionException {
		PropertyListContext _localctx = new PropertyListContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_propertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(869);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)))) != 0)) {
				{
				setState(868);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPropertyListNotEmpty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPropertyListNotEmpty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListNotEmpty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListNotEmptyContext propertyListNotEmpty() throws RecognitionException {
		PropertyListNotEmptyContext _localctx = new PropertyListNotEmptyContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_propertyListNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			verb();
			setState(872);
			objectList();
			setState(881);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(873);
				match(SEMICOLON);
				setState(877);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)))) != 0)) {
					{
					setState(874);
					verb();
					setState(875);
					objectList();
					}
				}

				}
				}
				setState(883);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVerb(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVerb(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerb(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbContext verb() throws RecognitionException {
		VerbContext _localctx = new VerbContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_verb);
		try {
			setState(886);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(884);
				varOrIRI();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(885);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterObjectList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitObjectList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectListContext objectList() throws RecognitionException {
		ObjectListContext _localctx = new ObjectListContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_objectList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(888);
			object();
			setState(893);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(889);
				match(COMMA);
				setState(890);
				object();
				}
				}
				setState(895);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitObject(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectContext object() throws RecognitionException {
		ObjectContext _localctx = new ObjectContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_object);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(896);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesSameSubjectPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesSameSubjectPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesSameSubjectPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesSameSubjectPathContext triplesSameSubjectPath() throws RecognitionException {
		TriplesSameSubjectPathContext _localctx = new TriplesSameSubjectPathContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_triplesSameSubjectPath);
		try {
			setState(904);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(898);
				varOrTerm();
				setState(899);
				propertyListPathNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(901);
				triplesNodePath();
				setState(902);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPropertyListPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPropertyListPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathContext propertyListPath() throws RecognitionException {
		PropertyListPathContext _localctx = new PropertyListPathContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_propertyListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(907);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INVERSE - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (NEGATION - 118)))) != 0)) {
				{
				setState(906);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPropertyListPathNotEmpty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPropertyListPathNotEmpty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPathNotEmpty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathNotEmptyContext propertyListPathNotEmpty() throws RecognitionException {
		PropertyListPathNotEmptyContext _localctx = new PropertyListPathNotEmptyContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_propertyListPathNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(911);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(909);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(910);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(913);
			objectListPath();
			setState(920);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(914);
				match(SEMICOLON);
				setState(916);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INVERSE - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (NEGATION - 118)))) != 0)) {
					{
					setState(915);
					propertyListPathNotEmptyList();
					}
				}

				}
				}
				setState(922);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPropertyListPathNotEmptyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPropertyListPathNotEmptyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPropertyListPathNotEmptyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListPathNotEmptyListContext propertyListPathNotEmptyList() throws RecognitionException {
		PropertyListPathNotEmptyListContext _localctx = new PropertyListPathNotEmptyListContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_propertyListPathNotEmptyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(925);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(923);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(924);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(927);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVerbPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVerbPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerbPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbPathContext verbPath() throws RecognitionException {
		VerbPathContext _localctx = new VerbPathContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_verbPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(929);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVerbSimple(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVerbSimple(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVerbSimple(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VerbSimpleContext verbSimple() throws RecognitionException {
		VerbSimpleContext _localctx = new VerbSimpleContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_verbSimple);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(931);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterObjectListPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitObjectListPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectListPathContext objectListPath() throws RecognitionException {
		ObjectListPathContext _localctx = new ObjectListPathContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_objectListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(933);
			objectPath();
			setState(938);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(934);
				match(COMMA);
				setState(935);
				objectPath();
				}
				}
				setState(940);
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
		public TripleOptionsContext tripleOptions() {
			return getRuleContext(TripleOptionsContext.class,0);
		}
		public ObjectPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterObjectPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitObjectPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitObjectPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectPathContext objectPath() throws RecognitionException {
		ObjectPathContext _localctx = new ObjectPathContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_objectPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(941);
			graphNodePath();
			setState(943);
			_la = _input.LA(1);
			if (_la==OPTION) {
				{
				setState(942);
				tripleOptions();
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

	public static class TripleOptionsContext extends ParserRuleContext {
		public TerminalNode OPTION() { return getToken(SparqlParser.OPTION, 0); }
		public TableOptionContext tableOption() {
			return getRuleContext(TableOptionContext.class,0);
		}
		public TripleOptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tripleOptions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTripleOptions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTripleOptions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTripleOptions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TripleOptionsContext tripleOptions() throws RecognitionException {
		TripleOptionsContext _localctx = new TripleOptionsContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_tripleOptions);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(945);
			match(OPTION);
			setState(946);
			match(OPEN_BRACE);
			setState(947);
			tableOption();
			setState(948);
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

	public static class TableOptionContext extends ParserRuleContext {
		public TerminalNode TABLE_OPTION() { return getToken(SparqlParser.TABLE_OPTION, 0); }
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TableOptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableOption; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTableOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTableOption(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTableOption(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableOptionContext tableOption() throws RecognitionException {
		TableOptionContext _localctx = new TableOptionContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_tableOption);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(950);
			match(TABLE_OPTION);
			setState(951);
			string();
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathContext path() throws RecognitionException {
		PathContext _localctx = new PathContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(953);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathAlternative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathAlternativeContext pathAlternative() throws RecognitionException {
		PathAlternativeContext _localctx = new PathAlternativeContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_pathAlternative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(955);
			pathSequence();
			setState(960);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(956);
				match(PIPE);
				setState(957);
				pathSequence();
				}
				}
				setState(962);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathSequence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathSequence(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathSequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathSequenceContext pathSequence() throws RecognitionException {
		PathSequenceContext _localctx = new PathSequenceContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_pathSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(963);
			pathEltOrInverse();
			setState(968);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIVIDE) {
				{
				{
				setState(964);
				match(DIVIDE);
				setState(965);
				pathEltOrInverse();
				}
				}
				setState(970);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathElt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathElt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathElt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathEltContext pathElt() throws RecognitionException {
		PathEltContext _localctx = new PathEltContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_pathElt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(971);
			pathPrimary();
			setState(973);
			_la = _input.LA(1);
			if (((((_la - 154)) & ~0x3f) == 0 && ((1L << (_la - 154)) & ((1L << (PLUS_SIGN - 154)) | (1L << (ASTERISK - 154)) | (1L << (QUESTION_MARK - 154)))) != 0)) {
				{
				setState(972);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathEltOrInverse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathEltOrInverse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathEltOrInverse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathEltOrInverseContext pathEltOrInverse() throws RecognitionException {
		PathEltOrInverseContext _localctx = new PathEltOrInverseContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_pathEltOrInverse);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(976);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(975);
				match(INVERSE);
				}
			}

			setState(978);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathMod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathMod(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathMod(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathModContext pathMod() throws RecognitionException {
		PathModContext _localctx = new PathModContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_pathMod);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(980);
			((PathModContext)_localctx).op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(((((_la - 154)) & ~0x3f) == 0 && ((1L << (_la - 154)) & ((1L << (PLUS_SIGN - 154)) | (1L << (ASTERISK - 154)) | (1L << (QUESTION_MARK - 154)))) != 0)) ) {
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathPrimary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathPrimary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathPrimary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathPrimaryContext pathPrimary() throws RecognitionException {
		PathPrimaryContext _localctx = new PathPrimaryContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_pathPrimary);
		try {
			setState(990);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(982);
				iri();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(983);
				match(A);
				}
				break;
			case NEGATION:
				enterOuterAlt(_localctx, 3);
				{
				setState(984);
				match(NEGATION);
				setState(985);
				pathNegatedPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 4);
				{
				setState(986);
				match(OPEN_BRACE);
				setState(987);
				path();
				setState(988);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathNegatedPropertySet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathNegatedPropertySet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathNegatedPropertySet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathNegatedPropertySetContext pathNegatedPropertySet() throws RecognitionException {
		PathNegatedPropertySetContext _localctx = new PathNegatedPropertySetContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_pathNegatedPropertySet);
		int _la;
		try {
			setState(1005);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
				enterOuterAlt(_localctx, 1);
				{
				setState(992);
				pathOneInPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(993);
				match(OPEN_BRACE);
				setState(1002);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (INVERSE - 118)))) != 0)) {
					{
					setState(994);
					pathOneInPropertySet();
					setState(999);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==PIPE) {
						{
						{
						setState(995);
						match(PIPE);
						setState(996);
						pathOneInPropertySet();
						}
						}
						setState(1001);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1004);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPathOneInPropertySet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPathOneInPropertySet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPathOneInPropertySet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathOneInPropertySetContext pathOneInPropertySet() throws RecognitionException {
		PathOneInPropertySetContext _localctx = new PathOneInPropertySetContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_pathOneInPropertySet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1008);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(1007);
				match(INVERSE);
				}
			}

			setState(1012);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				{
				setState(1010);
				iri();
				}
				break;
			case A:
				{
				setState(1011);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterInteger(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitInteger(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitInteger(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntegerContext integer() throws RecognitionException {
		IntegerContext _localctx = new IntegerContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_integer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1014);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesNode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesNode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesNodeContext triplesNode() throws RecognitionException {
		TriplesNodeContext _localctx = new TriplesNodeContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_triplesNode);
		try {
			setState(1018);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1016);
				collection();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1017);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBlankNodePropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBlankNodePropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNodePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodePropertyListContext blankNodePropertyList() throws RecognitionException {
		BlankNodePropertyListContext _localctx = new BlankNodePropertyListContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_blankNodePropertyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1020);
			match(OPEN_SQUARE_BRACKET);
			setState(1021);
			propertyListNotEmpty();
			setState(1022);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterTriplesNodePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitTriplesNodePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesNodePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesNodePathContext triplesNodePath() throws RecognitionException {
		TriplesNodePathContext _localctx = new TriplesNodePathContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_triplesNodePath);
		try {
			setState(1026);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1024);
				collectionPath();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1025);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBlankNodePropertyListPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBlankNodePropertyListPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNodePropertyListPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodePropertyListPathContext blankNodePropertyListPath() throws RecognitionException {
		BlankNodePropertyListPathContext _localctx = new BlankNodePropertyListPathContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_blankNodePropertyListPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1028);
			match(OPEN_SQUARE_BRACKET);
			setState(1029);
			propertyListPathNotEmpty();
			setState(1030);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterCollection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitCollection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCollection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CollectionContext collection() throws RecognitionException {
		CollectionContext _localctx = new CollectionContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_collection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1032);
			match(OPEN_BRACE);
			setState(1034); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1033);
				graphNode();
				}
				}
				setState(1036); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0) );
			setState(1038);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterCollectionPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitCollectionPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitCollectionPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CollectionPathContext collectionPath() throws RecognitionException {
		CollectionPathContext _localctx = new CollectionPathContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_collectionPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1040);
			match(OPEN_BRACE);
			setState(1042); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1041);
				graphNodePath();
				}
				}
				setState(1044); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (IRIREF - 118)) | (1L << (PNAME_NS - 118)) | (1L << (PNAME_LN - 118)) | (1L << (BLANK_NODE_LABEL - 118)) | (1L << (VAR1 - 118)) | (1L << (VAR2 - 118)) | (1L << (INTEGER - 118)) | (1L << (DECIMAL - 118)) | (1L << (DOUBLE - 118)) | (1L << (INTEGER_POSITIVE - 118)) | (1L << (DECIMAL_POSITIVE - 118)) | (1L << (DOUBLE_POSITIVE - 118)) | (1L << (INTEGER_NEGATIVE - 118)) | (1L << (DECIMAL_NEGATIVE - 118)) | (1L << (DOUBLE_NEGATIVE - 118)) | (1L << (STRING_LITERAL1 - 118)) | (1L << (STRING_LITERAL2 - 118)) | (1L << (STRING_LITERAL_LONG1 - 118)) | (1L << (STRING_LITERAL_LONG2 - 118)) | (1L << (OPEN_BRACE - 118)) | (1L << (OPEN_SQUARE_BRACKET - 118)))) != 0) );
			setState(1046);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphNode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphNode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphNodeContext graphNode() throws RecognitionException {
		GraphNodeContext _localctx = new GraphNodeContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_graphNode);
		try {
			setState(1050);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,116,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1048);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1049);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphNodePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphNodePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphNodePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphNodePathContext graphNodePath() throws RecognitionException {
		GraphNodePathContext _localctx = new GraphNodePathContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_graphNodePath);
		try {
			setState(1054);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1052);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1053);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVarOrTerm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVarOrTerm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVarOrTerm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarOrTermContext varOrTerm() throws RecognitionException {
		VarOrTermContext _localctx = new VarOrTermContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_varOrTerm);
		try {
			setState(1058);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1056);
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
				setState(1057);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVarOrIRI(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVarOrIRI(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVarOrIRI(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarOrIRIContext varOrIRI() throws RecognitionException {
		VarOrIRIContext _localctx = new VarOrIRIContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_varOrIRI);
		try {
			setState(1062);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1060);
				var();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1061);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterVar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitVar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitVar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarContext var() throws RecognitionException {
		VarContext _localctx = new VarContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1064);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterGraphTerm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitGraphTerm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitGraphTerm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GraphTermContext graphTerm() throws RecognitionException {
		GraphTermContext _localctx = new GraphTermContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_graphTerm);
		try {
			setState(1072);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1066);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1067);
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
				setState(1068);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(1069);
				booleanLiteral();
				}
				break;
			case BLANK_NODE_LABEL:
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 5);
				{
				setState(1070);
				blankNode();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1071);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNil(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNil(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNil(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NilContext nil() throws RecognitionException {
		NilContext _localctx = new NilContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_nil);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1074);
			match(OPEN_BRACE);
			setState(1075);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUnarySignedLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUnarySignedLiteralExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConditionalOrExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConditionalOrExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterAdditiveExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitAdditiveExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUnaryAdditiveExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUnaryAdditiveExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterRelationalExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitRelationalExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterRelationalSetExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitRelationalSetExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBaseExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBaseExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterMultiplicativeExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitMultiplicativeExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterConditionalAndExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitConditionalAndExpression(this);
		}
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUnaryNegationExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUnaryNegationExpression(this);
		}
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
		int _startState = 228;
		enterRecursionRule(_localctx, 228, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1083);
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

				setState(1078);
				primaryExpression();
				}
				break;
			case PLUS_SIGN:
			case MINUS_SIGN:
				{
				_localctx = new UnaryAdditiveExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1079);
				((UnaryAdditiveExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
					((UnaryAdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1080);
				expression(9);
				}
				break;
			case NEGATION:
				{
				_localctx = new UnaryNegationExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1081);
				match(NEGATION);
				setState(1082);
				expression(8);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(1114);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,125,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1112);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,124,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicativeExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1085);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1086);
						((MultiplicativeExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==ASTERISK || _la==DIVIDE) ) {
							((MultiplicativeExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1087);
						expression(8);
						}
						break;
					case 2:
						{
						_localctx = new AdditiveExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1088);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1089);
						((AdditiveExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
							((AdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1090);
						expression(7);
						}
						break;
					case 3:
						{
						_localctx = new RelationalExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1091);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1092);
						((RelationalExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 140)) & ~0x3f) == 0 && ((1L << (_la - 140)) & ((1L << (LESS_EQUAL - 140)) | (1L << (GREATER_EQUAL - 140)) | (1L << (NOT_EQUAL - 140)) | (1L << (EQUAL - 140)) | (1L << (LESS - 140)) | (1L << (GREATER - 140)))) != 0)) ) {
							((RelationalExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1093);
						expression(4);
						}
						break;
					case 4:
						{
						_localctx = new UnarySignedLiteralExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1094);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1095);
						unaryLiteralExpression();
						}
						break;
					case 5:
						{
						_localctx = new RelationalSetExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1096);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1098);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(1097);
							match(NOT);
							}
						}

						setState(1100);
						match(IN);
						setState(1101);
						match(OPEN_BRACE);
						setState(1103);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & ((1L << (IRI - 65)) | (1L << (URI - 65)) | (1L << (BNODE - 65)) | (1L << (RAND - 65)) | (1L << (ABS - 65)) | (1L << (CEIL - 65)) | (1L << (FLOOR - 65)) | (1L << (ROUND - 65)) | (1L << (CONCAT - 65)) | (1L << (STRLEN - 65)) | (1L << (UCASE - 65)) | (1L << (LCASE - 65)) | (1L << (ENCODE_FOR_URI - 65)) | (1L << (CONTAINS - 65)) | (1L << (STRSTARTS - 65)) | (1L << (STRENDS - 65)) | (1L << (STRBEFORE - 65)) | (1L << (STRAFTER - 65)) | (1L << (REPLACE - 65)) | (1L << (YEAR - 65)) | (1L << (MONTH - 65)) | (1L << (DAY - 65)) | (1L << (HOURS - 65)) | (1L << (MINUTES - 65)) | (1L << (SECONDS - 65)) | (1L << (TIMEZONE - 65)) | (1L << (TZ - 65)) | (1L << (NOW - 65)) | (1L << (UUID - 65)) | (1L << (STRUUID - 65)) | (1L << (MD5 - 65)) | (1L << (SHA1 - 65)) | (1L << (SHA256 - 65)) | (1L << (SHA384 - 65)) | (1L << (SHA512 - 65)) | (1L << (COALESCE - 65)) | (1L << (IF - 65)) | (1L << (STRLANG - 65)) | (1L << (STRDT - 65)) | (1L << (ISNUMERIC - 65)) | (1L << (COUNT - 65)) | (1L << (SUM - 65)) | (1L << (MIN - 65)) | (1L << (MAX - 65)) | (1L << (AVG - 65)) | (1L << (SAMPLE - 65)) | (1L << (GROUP_CONCAT - 65)) | (1L << (NOT - 65)) | (1L << (EXISTS - 65)) | (1L << (IRIREF - 65)) | (1L << (PNAME_NS - 65)) | (1L << (PNAME_LN - 65)) | (1L << (VAR1 - 65)) | (1L << (VAR2 - 65)) | (1L << (INTEGER - 65)) | (1L << (DECIMAL - 65)) | (1L << (DOUBLE - 65)) | (1L << (INTEGER_POSITIVE - 65)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (DECIMAL_POSITIVE - 129)) | (1L << (DOUBLE_POSITIVE - 129)) | (1L << (INTEGER_NEGATIVE - 129)) | (1L << (DECIMAL_NEGATIVE - 129)) | (1L << (DOUBLE_NEGATIVE - 129)) | (1L << (STRING_LITERAL1 - 129)) | (1L << (STRING_LITERAL2 - 129)) | (1L << (STRING_LITERAL_LONG1 - 129)) | (1L << (STRING_LITERAL_LONG2 - 129)) | (1L << (OPEN_BRACE - 129)) | (1L << (PLUS_SIGN - 129)) | (1L << (MINUS_SIGN - 129)) | (1L << (NEGATION - 129)))) != 0)) {
							{
							setState(1102);
							expressionList();
							}
						}

						setState(1105);
						match(CLOSE_BRACE);
						}
						break;
					case 6:
						{
						_localctx = new ConditionalAndExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1106);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						{
						setState(1107);
						match(AND);
						setState(1108);
						expression(0);
						}
						}
						break;
					case 7:
						{
						_localctx = new ConditionalOrExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1109);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						{
						setState(1110);
						match(OR);
						setState(1111);
						expression(0);
						}
						}
						break;
					}
					} 
				}
				setState(1116);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,125,_ctx);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUnaryLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUnaryLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryLiteralExpressionContext unaryLiteralExpression() throws RecognitionException {
		UnaryLiteralExpressionContext _localctx = new UnaryLiteralExpressionContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_unaryLiteralExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1119);
			switch (_input.LA(1)) {
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				{
				setState(1117);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				{
				setState(1118);
				numericLiteralNegative();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1123);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,127,_ctx) ) {
			case 1:
				{
				setState(1121);
				((UnaryLiteralExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASTERISK || _la==DIVIDE) ) {
					((UnaryLiteralExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1122);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterUnaryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitUnaryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitUnaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryExpressionContext unaryExpression() throws RecognitionException {
		UnaryExpressionContext _localctx = new UnaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_unaryExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1126);
			_la = _input.LA(1);
			if (((((_la - 154)) & ~0x3f) == 0 && ((1L << (_la - 154)) & ((1L << (PLUS_SIGN - 154)) | (1L << (MINUS_SIGN - 154)) | (1L << (NEGATION - 154)))) != 0)) {
				{
				setState(1125);
				((UnaryExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 154)) & ~0x3f) == 0 && ((1L << (_la - 154)) & ((1L << (PLUS_SIGN - 154)) | (1L << (MINUS_SIGN - 154)) | (1L << (NEGATION - 154)))) != 0)) ) {
					((UnaryExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(1128);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPrimaryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPrimaryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrimaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExpressionContext primaryExpression() throws RecognitionException {
		PrimaryExpressionContext _localctx = new PrimaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_primaryExpression);
		try {
			setState(1140);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1130);
				match(OPEN_BRACE);
				setState(1131);
				expression(0);
				setState(1132);
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
				setState(1134);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1135);
				iriRefOrFunction();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 4);
				{
				setState(1136);
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
				setState(1137);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1138);
				booleanLiteral();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 7);
				{
				setState(1139);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBuiltInCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBuiltInCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBuiltInCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BuiltInCallContext builtInCall() throws RecognitionException {
		BuiltInCallContext _localctx = new BuiltInCallContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_builtInCall);
		int _la;
		try {
			setState(1410);
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
				setState(1142);
				aggregate();
				}
				break;
			case STR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1143);
				match(STR);
				setState(1144);
				match(OPEN_BRACE);
				setState(1145);
				expression(0);
				setState(1146);
				match(CLOSE_BRACE);
				}
				break;
			case LANG:
				enterOuterAlt(_localctx, 3);
				{
				setState(1148);
				match(LANG);
				setState(1149);
				match(OPEN_BRACE);
				setState(1150);
				expression(0);
				setState(1151);
				match(CLOSE_BRACE);
				}
				break;
			case LANGMATCHES:
				enterOuterAlt(_localctx, 4);
				{
				setState(1153);
				match(LANGMATCHES);
				setState(1154);
				match(OPEN_BRACE);
				setState(1155);
				expression(0);
				setState(1156);
				match(COMMA);
				setState(1157);
				expression(0);
				setState(1158);
				match(CLOSE_BRACE);
				}
				break;
			case DATATYPE:
				enterOuterAlt(_localctx, 5);
				{
				setState(1160);
				match(DATATYPE);
				setState(1161);
				match(OPEN_BRACE);
				setState(1162);
				expression(0);
				setState(1163);
				match(CLOSE_BRACE);
				}
				break;
			case BOUND:
				enterOuterAlt(_localctx, 6);
				{
				setState(1165);
				match(BOUND);
				setState(1166);
				match(OPEN_BRACE);
				setState(1167);
				var();
				setState(1168);
				match(CLOSE_BRACE);
				}
				break;
			case IRI:
				enterOuterAlt(_localctx, 7);
				{
				setState(1170);
				match(IRI);
				setState(1171);
				match(OPEN_BRACE);
				setState(1172);
				expression(0);
				setState(1173);
				match(CLOSE_BRACE);
				}
				break;
			case URI:
				enterOuterAlt(_localctx, 8);
				{
				setState(1175);
				match(URI);
				setState(1176);
				match(OPEN_BRACE);
				setState(1177);
				expression(0);
				setState(1178);
				match(CLOSE_BRACE);
				}
				break;
			case BNODE:
				enterOuterAlt(_localctx, 9);
				{
				setState(1180);
				match(BNODE);
				setState(1181);
				match(OPEN_BRACE);
				setState(1183);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & ((1L << (IRI - 65)) | (1L << (URI - 65)) | (1L << (BNODE - 65)) | (1L << (RAND - 65)) | (1L << (ABS - 65)) | (1L << (CEIL - 65)) | (1L << (FLOOR - 65)) | (1L << (ROUND - 65)) | (1L << (CONCAT - 65)) | (1L << (STRLEN - 65)) | (1L << (UCASE - 65)) | (1L << (LCASE - 65)) | (1L << (ENCODE_FOR_URI - 65)) | (1L << (CONTAINS - 65)) | (1L << (STRSTARTS - 65)) | (1L << (STRENDS - 65)) | (1L << (STRBEFORE - 65)) | (1L << (STRAFTER - 65)) | (1L << (REPLACE - 65)) | (1L << (YEAR - 65)) | (1L << (MONTH - 65)) | (1L << (DAY - 65)) | (1L << (HOURS - 65)) | (1L << (MINUTES - 65)) | (1L << (SECONDS - 65)) | (1L << (TIMEZONE - 65)) | (1L << (TZ - 65)) | (1L << (NOW - 65)) | (1L << (UUID - 65)) | (1L << (STRUUID - 65)) | (1L << (MD5 - 65)) | (1L << (SHA1 - 65)) | (1L << (SHA256 - 65)) | (1L << (SHA384 - 65)) | (1L << (SHA512 - 65)) | (1L << (COALESCE - 65)) | (1L << (IF - 65)) | (1L << (STRLANG - 65)) | (1L << (STRDT - 65)) | (1L << (ISNUMERIC - 65)) | (1L << (COUNT - 65)) | (1L << (SUM - 65)) | (1L << (MIN - 65)) | (1L << (MAX - 65)) | (1L << (AVG - 65)) | (1L << (SAMPLE - 65)) | (1L << (GROUP_CONCAT - 65)) | (1L << (NOT - 65)) | (1L << (EXISTS - 65)) | (1L << (IRIREF - 65)) | (1L << (PNAME_NS - 65)) | (1L << (PNAME_LN - 65)) | (1L << (VAR1 - 65)) | (1L << (VAR2 - 65)) | (1L << (INTEGER - 65)) | (1L << (DECIMAL - 65)) | (1L << (DOUBLE - 65)) | (1L << (INTEGER_POSITIVE - 65)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (DECIMAL_POSITIVE - 129)) | (1L << (DOUBLE_POSITIVE - 129)) | (1L << (INTEGER_NEGATIVE - 129)) | (1L << (DECIMAL_NEGATIVE - 129)) | (1L << (DOUBLE_NEGATIVE - 129)) | (1L << (STRING_LITERAL1 - 129)) | (1L << (STRING_LITERAL2 - 129)) | (1L << (STRING_LITERAL_LONG1 - 129)) | (1L << (STRING_LITERAL_LONG2 - 129)) | (1L << (OPEN_BRACE - 129)) | (1L << (PLUS_SIGN - 129)) | (1L << (MINUS_SIGN - 129)) | (1L << (NEGATION - 129)))) != 0)) {
					{
					setState(1182);
					expression(0);
					}
				}

				setState(1185);
				match(CLOSE_BRACE);
				}
				break;
			case RAND:
				enterOuterAlt(_localctx, 10);
				{
				setState(1186);
				match(RAND);
				setState(1187);
				match(OPEN_BRACE);
				setState(1188);
				match(CLOSE_BRACE);
				}
				break;
			case ABS:
				enterOuterAlt(_localctx, 11);
				{
				setState(1189);
				match(ABS);
				setState(1190);
				match(OPEN_BRACE);
				setState(1191);
				expression(0);
				setState(1192);
				match(CLOSE_BRACE);
				}
				break;
			case CEIL:
				enterOuterAlt(_localctx, 12);
				{
				setState(1194);
				match(CEIL);
				setState(1195);
				match(OPEN_BRACE);
				setState(1196);
				expression(0);
				setState(1197);
				match(CLOSE_BRACE);
				}
				break;
			case FLOOR:
				enterOuterAlt(_localctx, 13);
				{
				setState(1199);
				match(FLOOR);
				setState(1200);
				match(OPEN_BRACE);
				setState(1201);
				expression(0);
				setState(1202);
				match(CLOSE_BRACE);
				}
				break;
			case ROUND:
				enterOuterAlt(_localctx, 14);
				{
				setState(1204);
				match(ROUND);
				setState(1205);
				match(OPEN_BRACE);
				setState(1206);
				expression(0);
				setState(1207);
				match(CLOSE_BRACE);
				}
				break;
			case CONCAT:
				enterOuterAlt(_localctx, 15);
				{
				setState(1209);
				match(CONCAT);
				setState(1210);
				match(OPEN_BRACE);
				setState(1212);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & ((1L << (IRI - 65)) | (1L << (URI - 65)) | (1L << (BNODE - 65)) | (1L << (RAND - 65)) | (1L << (ABS - 65)) | (1L << (CEIL - 65)) | (1L << (FLOOR - 65)) | (1L << (ROUND - 65)) | (1L << (CONCAT - 65)) | (1L << (STRLEN - 65)) | (1L << (UCASE - 65)) | (1L << (LCASE - 65)) | (1L << (ENCODE_FOR_URI - 65)) | (1L << (CONTAINS - 65)) | (1L << (STRSTARTS - 65)) | (1L << (STRENDS - 65)) | (1L << (STRBEFORE - 65)) | (1L << (STRAFTER - 65)) | (1L << (REPLACE - 65)) | (1L << (YEAR - 65)) | (1L << (MONTH - 65)) | (1L << (DAY - 65)) | (1L << (HOURS - 65)) | (1L << (MINUTES - 65)) | (1L << (SECONDS - 65)) | (1L << (TIMEZONE - 65)) | (1L << (TZ - 65)) | (1L << (NOW - 65)) | (1L << (UUID - 65)) | (1L << (STRUUID - 65)) | (1L << (MD5 - 65)) | (1L << (SHA1 - 65)) | (1L << (SHA256 - 65)) | (1L << (SHA384 - 65)) | (1L << (SHA512 - 65)) | (1L << (COALESCE - 65)) | (1L << (IF - 65)) | (1L << (STRLANG - 65)) | (1L << (STRDT - 65)) | (1L << (ISNUMERIC - 65)) | (1L << (COUNT - 65)) | (1L << (SUM - 65)) | (1L << (MIN - 65)) | (1L << (MAX - 65)) | (1L << (AVG - 65)) | (1L << (SAMPLE - 65)) | (1L << (GROUP_CONCAT - 65)) | (1L << (NOT - 65)) | (1L << (EXISTS - 65)) | (1L << (IRIREF - 65)) | (1L << (PNAME_NS - 65)) | (1L << (PNAME_LN - 65)) | (1L << (VAR1 - 65)) | (1L << (VAR2 - 65)) | (1L << (INTEGER - 65)) | (1L << (DECIMAL - 65)) | (1L << (DOUBLE - 65)) | (1L << (INTEGER_POSITIVE - 65)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (DECIMAL_POSITIVE - 129)) | (1L << (DOUBLE_POSITIVE - 129)) | (1L << (INTEGER_NEGATIVE - 129)) | (1L << (DECIMAL_NEGATIVE - 129)) | (1L << (DOUBLE_NEGATIVE - 129)) | (1L << (STRING_LITERAL1 - 129)) | (1L << (STRING_LITERAL2 - 129)) | (1L << (STRING_LITERAL_LONG1 - 129)) | (1L << (STRING_LITERAL_LONG2 - 129)) | (1L << (OPEN_BRACE - 129)) | (1L << (PLUS_SIGN - 129)) | (1L << (MINUS_SIGN - 129)) | (1L << (NEGATION - 129)))) != 0)) {
					{
					setState(1211);
					expressionList();
					}
				}

				setState(1214);
				match(CLOSE_BRACE);
				}
				break;
			case SUBSTR:
				enterOuterAlt(_localctx, 16);
				{
				setState(1215);
				subStringExpression();
				}
				break;
			case STRLEN:
				enterOuterAlt(_localctx, 17);
				{
				setState(1216);
				match(STRLEN);
				setState(1217);
				match(OPEN_BRACE);
				setState(1218);
				expression(0);
				setState(1219);
				match(CLOSE_BRACE);
				}
				break;
			case REPLACE:
				enterOuterAlt(_localctx, 18);
				{
				setState(1221);
				strReplaceExpression();
				}
				break;
			case UCASE:
				enterOuterAlt(_localctx, 19);
				{
				setState(1222);
				match(UCASE);
				setState(1223);
				match(OPEN_BRACE);
				setState(1224);
				expression(0);
				setState(1225);
				match(CLOSE_BRACE);
				}
				break;
			case LCASE:
				enterOuterAlt(_localctx, 20);
				{
				setState(1227);
				match(LCASE);
				setState(1228);
				match(OPEN_BRACE);
				setState(1229);
				expression(0);
				setState(1230);
				match(CLOSE_BRACE);
				}
				break;
			case ENCODE_FOR_URI:
				enterOuterAlt(_localctx, 21);
				{
				setState(1232);
				match(ENCODE_FOR_URI);
				setState(1233);
				match(OPEN_BRACE);
				setState(1234);
				expression(0);
				setState(1235);
				match(CLOSE_BRACE);
				}
				break;
			case CONTAINS:
				enterOuterAlt(_localctx, 22);
				{
				setState(1237);
				match(CONTAINS);
				setState(1238);
				match(OPEN_BRACE);
				setState(1239);
				expression(0);
				setState(1240);
				match(COMMA);
				setState(1241);
				expression(0);
				setState(1242);
				match(CLOSE_BRACE);
				}
				break;
			case STRSTARTS:
				enterOuterAlt(_localctx, 23);
				{
				setState(1244);
				match(STRSTARTS);
				setState(1245);
				match(OPEN_BRACE);
				setState(1246);
				expression(0);
				setState(1247);
				match(COMMA);
				setState(1248);
				expression(0);
				setState(1249);
				match(CLOSE_BRACE);
				}
				break;
			case STRENDS:
				enterOuterAlt(_localctx, 24);
				{
				setState(1251);
				match(STRENDS);
				setState(1252);
				match(OPEN_BRACE);
				setState(1253);
				expression(0);
				setState(1254);
				match(COMMA);
				setState(1255);
				expression(0);
				setState(1256);
				match(CLOSE_BRACE);
				}
				break;
			case STRBEFORE:
				enterOuterAlt(_localctx, 25);
				{
				setState(1258);
				match(STRBEFORE);
				setState(1259);
				match(OPEN_BRACE);
				setState(1260);
				expression(0);
				setState(1261);
				match(COMMA);
				setState(1262);
				expression(0);
				setState(1263);
				match(CLOSE_BRACE);
				}
				break;
			case STRAFTER:
				enterOuterAlt(_localctx, 26);
				{
				setState(1265);
				match(STRAFTER);
				setState(1266);
				match(OPEN_BRACE);
				setState(1267);
				expression(0);
				setState(1268);
				match(COMMA);
				setState(1269);
				expression(0);
				setState(1270);
				match(CLOSE_BRACE);
				}
				break;
			case YEAR:
				enterOuterAlt(_localctx, 27);
				{
				setState(1272);
				match(YEAR);
				setState(1273);
				match(OPEN_BRACE);
				setState(1274);
				expression(0);
				setState(1275);
				match(CLOSE_BRACE);
				}
				break;
			case MONTH:
				enterOuterAlt(_localctx, 28);
				{
				setState(1277);
				match(MONTH);
				setState(1278);
				match(OPEN_BRACE);
				setState(1279);
				expression(0);
				setState(1280);
				match(CLOSE_BRACE);
				}
				break;
			case DAY:
				enterOuterAlt(_localctx, 29);
				{
				setState(1282);
				match(DAY);
				setState(1283);
				match(OPEN_BRACE);
				setState(1284);
				expression(0);
				setState(1285);
				match(CLOSE_BRACE);
				}
				break;
			case HOURS:
				enterOuterAlt(_localctx, 30);
				{
				setState(1287);
				match(HOURS);
				setState(1288);
				match(OPEN_BRACE);
				setState(1289);
				expression(0);
				setState(1290);
				match(CLOSE_BRACE);
				}
				break;
			case MINUTES:
				enterOuterAlt(_localctx, 31);
				{
				setState(1292);
				match(MINUTES);
				setState(1293);
				match(OPEN_BRACE);
				setState(1294);
				expression(0);
				setState(1295);
				match(CLOSE_BRACE);
				}
				break;
			case SECONDS:
				enterOuterAlt(_localctx, 32);
				{
				setState(1297);
				match(SECONDS);
				setState(1298);
				match(OPEN_BRACE);
				setState(1299);
				expression(0);
				setState(1300);
				match(CLOSE_BRACE);
				}
				break;
			case TIMEZONE:
				enterOuterAlt(_localctx, 33);
				{
				setState(1302);
				match(TIMEZONE);
				setState(1303);
				match(OPEN_BRACE);
				setState(1304);
				expression(0);
				setState(1305);
				match(CLOSE_BRACE);
				}
				break;
			case TZ:
				enterOuterAlt(_localctx, 34);
				{
				setState(1307);
				match(TZ);
				setState(1308);
				match(OPEN_BRACE);
				setState(1309);
				expression(0);
				setState(1310);
				match(CLOSE_BRACE);
				}
				break;
			case NOW:
				enterOuterAlt(_localctx, 35);
				{
				setState(1312);
				match(NOW);
				setState(1313);
				match(OPEN_BRACE);
				setState(1314);
				match(CLOSE_BRACE);
				}
				break;
			case UUID:
				enterOuterAlt(_localctx, 36);
				{
				setState(1315);
				match(UUID);
				setState(1316);
				match(OPEN_BRACE);
				setState(1317);
				match(CLOSE_BRACE);
				}
				break;
			case STRUUID:
				enterOuterAlt(_localctx, 37);
				{
				setState(1318);
				match(STRUUID);
				setState(1319);
				match(OPEN_BRACE);
				setState(1320);
				match(CLOSE_BRACE);
				}
				break;
			case MD5:
				enterOuterAlt(_localctx, 38);
				{
				setState(1321);
				match(MD5);
				setState(1322);
				match(OPEN_BRACE);
				setState(1323);
				expression(0);
				setState(1324);
				match(CLOSE_BRACE);
				}
				break;
			case SHA1:
				enterOuterAlt(_localctx, 39);
				{
				setState(1326);
				match(SHA1);
				setState(1327);
				match(OPEN_BRACE);
				setState(1328);
				expression(0);
				setState(1329);
				match(CLOSE_BRACE);
				}
				break;
			case SHA256:
				enterOuterAlt(_localctx, 40);
				{
				setState(1331);
				match(SHA256);
				setState(1332);
				match(OPEN_BRACE);
				setState(1333);
				expression(0);
				setState(1334);
				match(CLOSE_BRACE);
				}
				break;
			case SHA384:
				enterOuterAlt(_localctx, 41);
				{
				setState(1336);
				match(SHA384);
				setState(1337);
				match(OPEN_BRACE);
				setState(1338);
				expression(0);
				setState(1339);
				match(CLOSE_BRACE);
				}
				break;
			case SHA512:
				enterOuterAlt(_localctx, 42);
				{
				setState(1341);
				match(SHA512);
				setState(1342);
				match(OPEN_BRACE);
				setState(1343);
				expression(0);
				setState(1344);
				match(CLOSE_BRACE);
				}
				break;
			case COALESCE:
				enterOuterAlt(_localctx, 43);
				{
				setState(1346);
				match(COALESCE);
				setState(1347);
				match(OPEN_BRACE);
				setState(1349);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & ((1L << (IRI - 65)) | (1L << (URI - 65)) | (1L << (BNODE - 65)) | (1L << (RAND - 65)) | (1L << (ABS - 65)) | (1L << (CEIL - 65)) | (1L << (FLOOR - 65)) | (1L << (ROUND - 65)) | (1L << (CONCAT - 65)) | (1L << (STRLEN - 65)) | (1L << (UCASE - 65)) | (1L << (LCASE - 65)) | (1L << (ENCODE_FOR_URI - 65)) | (1L << (CONTAINS - 65)) | (1L << (STRSTARTS - 65)) | (1L << (STRENDS - 65)) | (1L << (STRBEFORE - 65)) | (1L << (STRAFTER - 65)) | (1L << (REPLACE - 65)) | (1L << (YEAR - 65)) | (1L << (MONTH - 65)) | (1L << (DAY - 65)) | (1L << (HOURS - 65)) | (1L << (MINUTES - 65)) | (1L << (SECONDS - 65)) | (1L << (TIMEZONE - 65)) | (1L << (TZ - 65)) | (1L << (NOW - 65)) | (1L << (UUID - 65)) | (1L << (STRUUID - 65)) | (1L << (MD5 - 65)) | (1L << (SHA1 - 65)) | (1L << (SHA256 - 65)) | (1L << (SHA384 - 65)) | (1L << (SHA512 - 65)) | (1L << (COALESCE - 65)) | (1L << (IF - 65)) | (1L << (STRLANG - 65)) | (1L << (STRDT - 65)) | (1L << (ISNUMERIC - 65)) | (1L << (COUNT - 65)) | (1L << (SUM - 65)) | (1L << (MIN - 65)) | (1L << (MAX - 65)) | (1L << (AVG - 65)) | (1L << (SAMPLE - 65)) | (1L << (GROUP_CONCAT - 65)) | (1L << (NOT - 65)) | (1L << (EXISTS - 65)) | (1L << (IRIREF - 65)) | (1L << (PNAME_NS - 65)) | (1L << (PNAME_LN - 65)) | (1L << (VAR1 - 65)) | (1L << (VAR2 - 65)) | (1L << (INTEGER - 65)) | (1L << (DECIMAL - 65)) | (1L << (DOUBLE - 65)) | (1L << (INTEGER_POSITIVE - 65)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (DECIMAL_POSITIVE - 129)) | (1L << (DOUBLE_POSITIVE - 129)) | (1L << (INTEGER_NEGATIVE - 129)) | (1L << (DECIMAL_NEGATIVE - 129)) | (1L << (DOUBLE_NEGATIVE - 129)) | (1L << (STRING_LITERAL1 - 129)) | (1L << (STRING_LITERAL2 - 129)) | (1L << (STRING_LITERAL_LONG1 - 129)) | (1L << (STRING_LITERAL_LONG2 - 129)) | (1L << (OPEN_BRACE - 129)) | (1L << (PLUS_SIGN - 129)) | (1L << (MINUS_SIGN - 129)) | (1L << (NEGATION - 129)))) != 0)) {
					{
					setState(1348);
					expressionList();
					}
				}

				setState(1351);
				match(CLOSE_BRACE);
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 44);
				{
				setState(1352);
				match(IF);
				setState(1353);
				match(OPEN_BRACE);
				setState(1354);
				expression(0);
				setState(1355);
				match(COMMA);
				setState(1356);
				expression(0);
				setState(1357);
				match(COMMA);
				setState(1358);
				expression(0);
				setState(1359);
				match(CLOSE_BRACE);
				}
				break;
			case STRLANG:
				enterOuterAlt(_localctx, 45);
				{
				setState(1361);
				match(STRLANG);
				setState(1362);
				match(OPEN_BRACE);
				setState(1363);
				expression(0);
				setState(1364);
				match(COMMA);
				setState(1365);
				expression(0);
				setState(1366);
				match(CLOSE_BRACE);
				}
				break;
			case STRDT:
				enterOuterAlt(_localctx, 46);
				{
				setState(1368);
				match(STRDT);
				setState(1369);
				match(OPEN_BRACE);
				setState(1370);
				expression(0);
				setState(1371);
				match(COMMA);
				setState(1372);
				expression(0);
				setState(1373);
				match(CLOSE_BRACE);
				}
				break;
			case SAMETERM:
				enterOuterAlt(_localctx, 47);
				{
				setState(1375);
				match(SAMETERM);
				setState(1376);
				match(OPEN_BRACE);
				setState(1377);
				expression(0);
				setState(1378);
				match(COMMA);
				setState(1379);
				expression(0);
				setState(1380);
				match(CLOSE_BRACE);
				}
				break;
			case ISIRI:
				enterOuterAlt(_localctx, 48);
				{
				setState(1382);
				match(ISIRI);
				setState(1383);
				match(OPEN_BRACE);
				setState(1384);
				expression(0);
				setState(1385);
				match(CLOSE_BRACE);
				}
				break;
			case ISURI:
				enterOuterAlt(_localctx, 49);
				{
				setState(1387);
				match(ISURI);
				setState(1388);
				match(OPEN_BRACE);
				setState(1389);
				expression(0);
				setState(1390);
				match(CLOSE_BRACE);
				}
				break;
			case ISBLANK:
				enterOuterAlt(_localctx, 50);
				{
				setState(1392);
				match(ISBLANK);
				setState(1393);
				match(OPEN_BRACE);
				setState(1394);
				expression(0);
				setState(1395);
				match(CLOSE_BRACE);
				}
				break;
			case ISLITERAL:
				enterOuterAlt(_localctx, 51);
				{
				setState(1397);
				match(ISLITERAL);
				setState(1398);
				match(OPEN_BRACE);
				setState(1399);
				expression(0);
				setState(1400);
				match(CLOSE_BRACE);
				}
				break;
			case ISNUMERIC:
				enterOuterAlt(_localctx, 52);
				{
				setState(1402);
				match(ISNUMERIC);
				setState(1403);
				match(OPEN_BRACE);
				setState(1404);
				expression(0);
				setState(1405);
				match(CLOSE_BRACE);
				}
				break;
			case REGEX:
				enterOuterAlt(_localctx, 53);
				{
				setState(1407);
				regexExpression();
				}
				break;
			case EXISTS:
				enterOuterAlt(_localctx, 54);
				{
				setState(1408);
				existsFunction();
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 55);
				{
				setState(1409);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterRegexExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitRegexExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRegexExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegexExpressionContext regexExpression() throws RecognitionException {
		RegexExpressionContext _localctx = new RegexExpressionContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_regexExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1412);
			match(REGEX);
			setState(1413);
			match(OPEN_BRACE);
			setState(1414);
			expression(0);
			setState(1415);
			match(COMMA);
			setState(1416);
			expression(0);
			setState(1419);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1417);
				match(COMMA);
				setState(1418);
				expression(0);
				}
			}

			setState(1421);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterSubStringExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitSubStringExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitSubStringExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubStringExpressionContext subStringExpression() throws RecognitionException {
		SubStringExpressionContext _localctx = new SubStringExpressionContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_subStringExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1423);
			match(SUBSTR);
			setState(1424);
			match(OPEN_BRACE);
			setState(1425);
			expression(0);
			setState(1426);
			match(COMMA);
			setState(1427);
			expression(0);
			setState(1430);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1428);
				match(COMMA);
				setState(1429);
				expression(0);
				}
			}

			setState(1432);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterStrReplaceExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitStrReplaceExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitStrReplaceExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrReplaceExpressionContext strReplaceExpression() throws RecognitionException {
		StrReplaceExpressionContext _localctx = new StrReplaceExpressionContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_strReplaceExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1434);
			match(REPLACE);
			setState(1435);
			match(OPEN_BRACE);
			setState(1436);
			expression(0);
			setState(1437);
			match(COMMA);
			setState(1438);
			expression(0);
			setState(1439);
			match(COMMA);
			setState(1440);
			expression(0);
			setState(1443);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1441);
				match(COMMA);
				setState(1442);
				expression(0);
				}
			}

			setState(1445);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterExistsFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitExistsFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitExistsFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExistsFunctionContext existsFunction() throws RecognitionException {
		ExistsFunctionContext _localctx = new ExistsFunctionContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_existsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1447);
			match(EXISTS);
			setState(1448);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNotExistsFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNotExistsFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNotExistsFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotExistsFunctionContext notExistsFunction() throws RecognitionException {
		NotExistsFunctionContext _localctx = new NotExistsFunctionContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_notExistsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1450);
			match(NOT);
			setState(1451);
			match(EXISTS);
			setState(1452);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterAggregate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitAggregate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAggregate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregateContext aggregate() throws RecognitionException {
		AggregateContext _localctx = new AggregateContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_aggregate);
		int _la;
		try {
			setState(1518);
			switch (_input.LA(1)) {
			case COUNT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1454);
				match(COUNT);
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

				setState(1461);
				switch (_input.LA(1)) {
				case ASTERISK:
					{
					setState(1459);
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
					setState(1460);
					expression(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1463);
				match(CLOSE_BRACE);
				}
				break;
			case SUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(1464);
				match(SUM);
				setState(1465);
				match(OPEN_BRACE);
				setState(1467);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1466);
					match(DISTINCT);
					}
				}

				setState(1469);
				expression(0);
				setState(1470);
				match(CLOSE_BRACE);
				}
				break;
			case MIN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1472);
				match(MIN);
				setState(1473);
				match(OPEN_BRACE);
				setState(1475);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1474);
					match(DISTINCT);
					}
				}

				setState(1477);
				expression(0);
				setState(1478);
				match(CLOSE_BRACE);
				}
				break;
			case MAX:
				enterOuterAlt(_localctx, 4);
				{
				setState(1480);
				match(MAX);
				setState(1481);
				match(OPEN_BRACE);
				setState(1483);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1482);
					match(DISTINCT);
					}
				}

				setState(1485);
				expression(0);
				setState(1486);
				match(CLOSE_BRACE);
				}
				break;
			case AVG:
				enterOuterAlt(_localctx, 5);
				{
				setState(1488);
				match(AVG);
				setState(1489);
				match(OPEN_BRACE);
				setState(1491);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1490);
					match(DISTINCT);
					}
				}

				setState(1493);
				expression(0);
				setState(1494);
				match(CLOSE_BRACE);
				}
				break;
			case SAMPLE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1496);
				match(SAMPLE);
				setState(1497);
				match(OPEN_BRACE);
				setState(1499);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1498);
					match(DISTINCT);
					}
				}

				setState(1501);
				expression(0);
				setState(1502);
				match(CLOSE_BRACE);
				}
				break;
			case GROUP_CONCAT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1504);
				match(GROUP_CONCAT);
				setState(1505);
				match(OPEN_BRACE);
				setState(1507);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1506);
					match(DISTINCT);
					}
				}

				setState(1509);
				expression(0);
				setState(1514);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(1510);
					match(SEMICOLON);
					setState(1511);
					match(SEPARATOR);
					setState(1512);
					match(EQUAL);
					setState(1513);
					string();
					}
				}

				setState(1516);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterIriRefOrFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitIriRefOrFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitIriRefOrFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IriRefOrFunctionContext iriRefOrFunction() throws RecognitionException {
		IriRefOrFunctionContext _localctx = new IriRefOrFunctionContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_iriRefOrFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1520);
			iri();
			setState(1522);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
			case 1:
				{
				setState(1521);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterRdfLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitRdfLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitRdfLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RdfLiteralContext rdfLiteral() throws RecognitionException {
		RdfLiteralContext _localctx = new RdfLiteralContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_rdfLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1524);
			string();
			setState(1528);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
			case 1:
				{
				setState(1525);
				match(LANGTAG);
				}
				break;
			case 2:
				{
				{
				setState(1526);
				match(REFERENCE);
				setState(1527);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralContext numericLiteral() throws RecognitionException {
		NumericLiteralContext _localctx = new NumericLiteralContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_numericLiteral);
		try {
			setState(1533);
			switch (_input.LA(1)) {
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1530);
				numericLiteralUnsigned();
				}
				break;
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1531);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1532);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNumericLiteralUnsigned(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNumericLiteralUnsigned(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralUnsigned(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralUnsignedContext numericLiteralUnsigned() throws RecognitionException {
		NumericLiteralUnsignedContext _localctx = new NumericLiteralUnsignedContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_numericLiteralUnsigned);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1535);
			_la = _input.LA(1);
			if ( !(((((_la - 125)) & ~0x3f) == 0 && ((1L << (_la - 125)) & ((1L << (INTEGER - 125)) | (1L << (DECIMAL - 125)) | (1L << (DOUBLE - 125)))) != 0)) ) {
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNumericLiteralPositive(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNumericLiteralPositive(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralPositive(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralPositiveContext numericLiteralPositive() throws RecognitionException {
		NumericLiteralPositiveContext _localctx = new NumericLiteralPositiveContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_numericLiteralPositive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1537);
			_la = _input.LA(1);
			if ( !(((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_POSITIVE - 128)) | (1L << (DECIMAL_POSITIVE - 128)) | (1L << (DOUBLE_POSITIVE - 128)))) != 0)) ) {
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterNumericLiteralNegative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitNumericLiteralNegative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitNumericLiteralNegative(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralNegativeContext numericLiteralNegative() throws RecognitionException {
		NumericLiteralNegativeContext _localctx = new NumericLiteralNegativeContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_numericLiteralNegative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1539);
			_la = _input.LA(1);
			if ( !(((((_la - 131)) & ~0x3f) == 0 && ((1L << (_la - 131)) & ((1L << (INTEGER_NEGATIVE - 131)) | (1L << (DECIMAL_NEGATIVE - 131)) | (1L << (DOUBLE_NEGATIVE - 131)))) != 0)) ) {
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanLiteralContext booleanLiteral() throws RecognitionException {
		BooleanLiteralContext _localctx = new BooleanLiteralContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_booleanLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1541);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1543);
			_la = _input.LA(1);
			if ( !(((((_la - 134)) & ~0x3f) == 0 && ((1L << (_la - 134)) & ((1L << (STRING_LITERAL1 - 134)) | (1L << (STRING_LITERAL2 - 134)) | (1L << (STRING_LITERAL_LONG1 - 134)) | (1L << (STRING_LITERAL_LONG2 - 134)))) != 0)) ) {
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterIri(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitIri(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitIri(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IriContext iri() throws RecognitionException {
		IriContext _localctx = new IriContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_iri);
		try {
			setState(1547);
			switch (_input.LA(1)) {
			case IRIREF:
				enterOuterAlt(_localctx, 1);
				{
				setState(1545);
				match(IRIREF);
				}
				break;
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1546);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterPrefixedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitPrefixedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitPrefixedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrefixedNameContext prefixedName() throws RecognitionException {
		PrefixedNameContext _localctx = new PrefixedNameContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_prefixedName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1549);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterBlankNode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitBlankNode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitBlankNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlankNodeContext blankNode() throws RecognitionException {
		BlankNodeContext _localctx = new BlankNodeContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_blankNode);
		try {
			setState(1553);
			switch (_input.LA(1)) {
			case BLANK_NODE_LABEL:
				enterOuterAlt(_localctx, 1);
				{
				setState(1551);
				match(BLANK_NODE_LABEL);
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1552);
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
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).enterAnon(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparqlParserListener ) ((SparqlParserListener)listener).exitAnon(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitAnon(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnonContext anon() throws RecognitionException {
		AnonContext _localctx = new AnonContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_anon);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1555);
			match(OPEN_SQUARE_BRACKET);
			setState(1556);
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
		case 114:
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00a8\u0619\4\2\t"+
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
		"\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089\t\u0089"+
		"\4\u008a\t\u008a\3\2\3\2\3\2\3\2\3\2\5\2\u011a\n\2\3\2\3\2\3\2\3\2\5\2"+
		"\u0120\n\2\3\2\5\2\u0123\n\2\3\3\7\3\u0126\n\3\f\3\16\3\u0129\13\3\3\3"+
		"\3\3\7\3\u012d\n\3\f\3\16\3\u0130\13\3\3\4\3\4\3\4\3\4\3\4\7\4\u0137\n"+
		"\4\f\4\16\4\u013a\13\4\3\5\3\5\3\5\5\5\u013f\n\5\3\6\3\6\3\6\3\7\3\7\3"+
		"\7\3\7\3\b\3\b\7\b\u014a\n\b\f\b\16\b\u014d\13\b\3\b\3\b\3\b\3\t\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\5\n\u0159\n\n\3\n\6\n\u015c\n\n\r\n\16\n\u015d\3\n"+
		"\5\n\u0161\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u016a\n\13\3\f"+
		"\3\f\3\f\7\f\u016f\n\f\f\f\16\f\u0172\13\f\3\f\3\f\3\f\3\f\7\f\u0178\n"+
		"\f\f\f\16\f\u017b\13\f\3\f\3\f\3\f\5\f\u0180\n\f\3\f\3\f\5\f\u0184\n\f"+
		"\3\r\3\r\6\r\u0188\n\r\r\r\16\r\u0189\3\r\5\r\u018d\n\r\3\r\7\r\u0190"+
		"\n\r\f\r\16\r\u0193\13\r\3\r\5\r\u0196\n\r\3\r\3\r\3\16\3\16\7\16\u019c"+
		"\n\16\f\16\16\16\u019f\13\16\3\16\3\16\3\16\3\17\3\17\5\17\u01a6\n\17"+
		"\3\17\3\17\3\20\5\20\u01ab\n\20\3\20\3\20\3\21\5\21\u01b0\n\21\3\21\5"+
		"\21\u01b3\n\21\3\21\5\21\u01b6\n\21\3\21\5\21\u01b9\n\21\3\22\3\22\3\22"+
		"\6\22\u01be\n\22\r\22\16\22\u01bf\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u01c8"+
		"\n\23\3\23\3\23\3\23\5\23\u01cd\n\23\3\24\3\24\6\24\u01d1\n\24\r\24\16"+
		"\24\u01d2\3\25\3\25\3\26\3\26\3\26\6\26\u01da\n\26\r\26\16\26\u01db\3"+
		"\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u01e5\n\27\3\30\3\30\5\30\u01e9"+
		"\n\30\3\30\3\30\5\30\u01ed\n\30\5\30\u01ef\n\30\3\31\3\31\3\31\3\32\3"+
		"\32\3\32\3\33\3\33\5\33\u01f9\n\33\3\34\3\34\3\34\3\34\3\34\3\34\7\34"+
		"\u0201\n\34\f\34\16\34\u0204\13\34\3\34\3\34\5\34\u0208\n\34\5\34\u020a"+
		"\n\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\5\35\u0217"+
		"\n\35\3\36\3\36\5\36\u021b\n\36\3\36\3\36\3\36\5\36\u0220\n\36\3\37\3"+
		"\37\5\37\u0224\n\37\3\37\3\37\3 \3 \5 \u022a\n \3 \3 \3!\3!\5!\u0230\n"+
		"!\3!\3!\3\"\3\"\5\"\u0236\n\"\3\"\3\"\3\"\3\"\3#\3#\5#\u023e\n#\3#\3#"+
		"\3#\3#\3$\3$\5$\u0246\n$\3$\3$\3$\3$\3%\3%\3%\3%\3&\3&\3&\3&\3\'\3\'\3"+
		"\'\3\'\3(\3(\5(\u025a\n(\3(\3(\5(\u025e\n(\3(\5(\u0261\n(\3(\7(\u0264"+
		"\n(\f(\16(\u0267\13(\3(\3(\3(\3)\3)\3)\3*\3*\3*\3+\3+\5+\u0274\n+\3+\3"+
		"+\3,\3,\5,\u027a\n,\3,\5,\u027d\n,\3-\3-\3-\3.\3.\3.\3.\5.\u0286\n.\3"+
		"/\3/\3/\3/\3\60\3\60\3\60\3\60\3\61\5\61\u0291\n\61\3\61\7\61\u0294\n"+
		"\61\f\61\16\61\u0297\13\61\3\62\3\62\5\62\u029b\n\62\3\62\5\62\u029e\n"+
		"\62\3\63\3\63\3\63\3\63\5\63\u02a4\n\63\3\63\3\63\3\64\3\64\3\64\5\64"+
		"\u02ab\n\64\7\64\u02ad\n\64\f\64\16\64\u02b0\13\64\3\65\3\65\3\65\5\65"+
		"\u02b5\n\65\3\65\3\65\3\66\5\66\u02ba\n\66\3\66\7\66\u02bd\n\66\f\66\16"+
		"\66\u02c0\13\66\3\67\3\67\5\67\u02c4\n\67\3\67\5\67\u02c7\n\67\38\38\3"+
		"8\58\u02cc\n8\78\u02ce\n8\f8\168\u02d1\138\39\39\39\39\39\39\39\39\59"+
		"\u02db\n9\3:\3:\3:\3;\3;\3;\3;\3<\3<\5<\u02e6\n<\3<\3<\3<\3=\3=\3=\3="+
		"\3=\3=\3=\3>\3>\3>\3?\3?\5?\u02f7\n?\3@\3@\3@\7@\u02fc\n@\f@\16@\u02ff"+
		"\13@\3@\3@\3A\3A\7A\u0305\nA\fA\16A\u0308\13A\3A\3A\3A\7A\u030d\nA\fA"+
		"\16A\u0310\13A\3A\3A\3B\3B\7B\u0316\nB\fB\16B\u0319\13B\3B\3B\3C\3C\3"+
		"C\3C\3C\5C\u0322\nC\3D\3D\3D\3E\3E\3E\7E\u032a\nE\fE\16E\u032d\13E\3F"+
		"\3F\3F\3G\3G\3G\3G\3G\3G\5G\u0338\nG\3H\3H\3H\3I\3I\5I\u033f\nI\3I\3I"+
		"\5I\u0343\nI\3I\3I\3J\3J\3J\7J\u034a\nJ\fJ\16J\u034d\13J\3K\3K\5K\u0351"+
		"\nK\3K\3K\3L\3L\3L\5L\u0358\nL\7L\u035a\nL\fL\16L\u035d\13L\3M\3M\3M\3"+
		"M\3M\3M\5M\u0365\nM\3N\5N\u0368\nN\3O\3O\3O\3O\3O\3O\5O\u0370\nO\7O\u0372"+
		"\nO\fO\16O\u0375\13O\3P\3P\5P\u0379\nP\3Q\3Q\3Q\7Q\u037e\nQ\fQ\16Q\u0381"+
		"\13Q\3R\3R\3S\3S\3S\3S\3S\3S\5S\u038b\nS\3T\5T\u038e\nT\3U\3U\5U\u0392"+
		"\nU\3U\3U\3U\5U\u0397\nU\7U\u0399\nU\fU\16U\u039c\13U\3V\3V\5V\u03a0\n"+
		"V\3V\3V\3W\3W\3X\3X\3Y\3Y\3Y\7Y\u03ab\nY\fY\16Y\u03ae\13Y\3Z\3Z\5Z\u03b2"+
		"\nZ\3[\3[\3[\3[\3[\3\\\3\\\3\\\3]\3]\3^\3^\3^\7^\u03c1\n^\f^\16^\u03c4"+
		"\13^\3_\3_\3_\7_\u03c9\n_\f_\16_\u03cc\13_\3`\3`\5`\u03d0\n`\3a\5a\u03d3"+
		"\na\3a\3a\3b\3b\3c\3c\3c\3c\3c\3c\3c\3c\5c\u03e1\nc\3d\3d\3d\3d\3d\7d"+
		"\u03e8\nd\fd\16d\u03eb\13d\5d\u03ed\nd\3d\5d\u03f0\nd\3e\5e\u03f3\ne\3"+
		"e\3e\5e\u03f7\ne\3f\3f\3g\3g\5g\u03fd\ng\3h\3h\3h\3h\3i\3i\5i\u0405\n"+
		"i\3j\3j\3j\3j\3k\3k\6k\u040d\nk\rk\16k\u040e\3k\3k\3l\3l\6l\u0415\nl\r"+
		"l\16l\u0416\3l\3l\3m\3m\5m\u041d\nm\3n\3n\5n\u0421\nn\3o\3o\5o\u0425\n"+
		"o\3p\3p\5p\u0429\np\3q\3q\3r\3r\3r\3r\3r\3r\5r\u0433\nr\3s\3s\3s\3t\3"+
		"t\3t\3t\3t\3t\5t\u043e\nt\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\5t\u044d"+
		"\nt\3t\3t\3t\5t\u0452\nt\3t\3t\3t\3t\3t\3t\3t\7t\u045b\nt\ft\16t\u045e"+
		"\13t\3u\3u\5u\u0462\nu\3u\3u\5u\u0466\nu\3v\5v\u0469\nv\3v\3v\3w\3w\3"+
		"w\3w\3w\3w\3w\3w\3w\3w\5w\u0477\nw\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\5x\u04a2\nx\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\5x\u04bf\nx\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\5x\u0548\nx\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\3x\3x\3x\3x\3x\3x\3x\3x\3x\5x\u0585\nx\3y\3y\3y\3y\3y\3y\3y\5y\u058e"+
		"\ny\3y\3y\3z\3z\3z\3z\3z\3z\3z\5z\u0599\nz\3z\3z\3{\3{\3{\3{\3{\3{\3{"+
		"\3{\3{\5{\u05a6\n{\3{\3{\3|\3|\3|\3}\3}\3}\3}\3~\3~\3~\5~\u05b4\n~\3~"+
		"\3~\5~\u05b8\n~\3~\3~\3~\3~\5~\u05be\n~\3~\3~\3~\3~\3~\3~\5~\u05c6\n~"+
		"\3~\3~\3~\3~\3~\3~\5~\u05ce\n~\3~\3~\3~\3~\3~\3~\5~\u05d6\n~\3~\3~\3~"+
		"\3~\3~\3~\5~\u05de\n~\3~\3~\3~\3~\3~\3~\5~\u05e6\n~\3~\3~\3~\3~\3~\5~"+
		"\u05ed\n~\3~\3~\5~\u05f1\n~\3\177\3\177\5\177\u05f5\n\177\3\u0080\3\u0080"+
		"\3\u0080\3\u0080\5\u0080\u05fb\n\u0080\3\u0081\3\u0081\3\u0081\5\u0081"+
		"\u0600\n\u0081\3\u0082\3\u0082\3\u0083\3\u0083\3\u0084\3\u0084\3\u0085"+
		"\3\u0085\3\u0086\3\u0086\3\u0087\3\u0087\5\u0087\u060e\n\u0087\3\u0088"+
		"\3\u0088\3\u0089\3\u0089\5\u0089\u0614\n\u0089\3\u008a\3\u008a\3\u008a"+
		"\3\u008a\2\3\u00e6\u008b\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&("+
		"*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084"+
		"\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c"+
		"\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4"+
		"\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc"+
		"\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4"+
		"\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc"+
		"\u00fe\u0100\u0102\u0104\u0106\u0108\u010a\u010c\u010e\u0110\u0112\2\20"+
		"\3\2\b\t\3\2\22\23\4\2\u009c\u009c\u009e\u009f\3\2|}\3\2\u009c\u009d\4"+
		"\2\u009e\u009e\u00a2\u00a2\4\2\u008e\u0090\u00a3\u00a5\4\2\u009c\u009d"+
		"\u00a1\u00a1\3\2\177\u0081\3\2\u0082\u0084\3\2\u0085\u0087\3\2()\3\2\u0088"+
		"\u008b\3\2yz\u0690\2\u0122\3\2\2\2\4\u0127\3\2\2\2\6\u0131\3\2\2\2\b\u013e"+
		"\3\2\2\2\n\u0140\3\2\2\2\f\u0143\3\2\2\2\16\u0147\3\2\2\2\20\u0151\3\2"+
		"\2\2\22\u0156\3\2\2\2\24\u0169\3\2\2\2\26\u016b\3\2\2\2\30\u0185\3\2\2"+
		"\2\32\u0199\3\2\2\2\34\u01a3\3\2\2\2\36\u01aa\3\2\2\2 \u01af\3\2\2\2\""+
		"\u01ba\3\2\2\2$\u01cc\3\2\2\2&\u01ce\3\2\2\2(\u01d4\3\2\2\2*\u01d6\3\2"+
		"\2\2,\u01e4\3\2\2\2.\u01ee\3\2\2\2\60\u01f0\3\2\2\2\62\u01f3\3\2\2\2\64"+
		"\u01f8\3\2\2\2\66\u01fa\3\2\2\28\u0216\3\2\2\2:\u0218\3\2\2\2<\u0221\3"+
		"\2\2\2>\u0227\3\2\2\2@\u022d\3\2\2\2B\u0233\3\2\2\2D\u023b\3\2\2\2F\u0243"+
		"\3\2\2\2H\u024b\3\2\2\2J\u024f\3\2\2\2L\u0253\3\2\2\2N\u0259\3\2\2\2P"+
		"\u026b\3\2\2\2R\u026e\3\2\2\2T\u0271\3\2\2\2V\u027c\3\2\2\2X\u027e\3\2"+
		"\2\2Z\u0285\3\2\2\2\\\u0287\3\2\2\2^\u028b\3\2\2\2`\u0290\3\2\2\2b\u0298"+
		"\3\2\2\2d\u029f\3\2\2\2f\u02a7\3\2\2\2h\u02b1\3\2\2\2j\u02b9\3\2\2\2l"+
		"\u02c1\3\2\2\2n\u02c8\3\2\2\2p\u02da\3\2\2\2r\u02dc\3\2\2\2t\u02df\3\2"+
		"\2\2v\u02e3\3\2\2\2x\u02ea\3\2\2\2z\u02f1\3\2\2\2|\u02f6\3\2\2\2~\u02f8"+
		"\3\2\2\2\u0080\u0302\3\2\2\2\u0082\u0313\3\2\2\2\u0084\u0321\3\2\2\2\u0086"+
		"\u0323\3\2\2\2\u0088\u0326\3\2\2\2\u008a\u032e\3\2\2\2\u008c\u0337\3\2"+
		"\2\2\u008e\u0339\3\2\2\2\u0090\u033c\3\2\2\2\u0092\u0346\3\2\2\2\u0094"+
		"\u034e\3\2\2\2\u0096\u0354\3\2\2\2\u0098\u0364\3\2\2\2\u009a\u0367\3\2"+
		"\2\2\u009c\u0369\3\2\2\2\u009e\u0378\3\2\2\2\u00a0\u037a\3\2\2\2\u00a2"+
		"\u0382\3\2\2\2\u00a4\u038a\3\2\2\2\u00a6\u038d\3\2\2\2\u00a8\u0391\3\2"+
		"\2\2\u00aa\u039f\3\2\2\2\u00ac\u03a3\3\2\2\2\u00ae\u03a5\3\2\2\2\u00b0"+
		"\u03a7\3\2\2\2\u00b2\u03af\3\2\2\2\u00b4\u03b3\3\2\2\2\u00b6\u03b8\3\2"+
		"\2\2\u00b8\u03bb\3\2\2\2\u00ba\u03bd\3\2\2\2\u00bc\u03c5\3\2\2\2\u00be"+
		"\u03cd\3\2\2\2\u00c0\u03d2\3\2\2\2\u00c2\u03d6\3\2\2\2\u00c4\u03e0\3\2"+
		"\2\2\u00c6\u03ef\3\2\2\2\u00c8\u03f2\3\2\2\2\u00ca\u03f8\3\2\2\2\u00cc"+
		"\u03fc\3\2\2\2\u00ce\u03fe\3\2\2\2\u00d0\u0404\3\2\2\2\u00d2\u0406\3\2"+
		"\2\2\u00d4\u040a\3\2\2\2\u00d6\u0412\3\2\2\2\u00d8\u041c\3\2\2\2\u00da"+
		"\u0420\3\2\2\2\u00dc\u0424\3\2\2\2\u00de\u0428\3\2\2\2\u00e0\u042a\3\2"+
		"\2\2\u00e2\u0432\3\2\2\2\u00e4\u0434\3\2\2\2\u00e6\u043d\3\2\2\2\u00e8"+
		"\u0461\3\2\2\2\u00ea\u0468\3\2\2\2\u00ec\u0476\3\2\2\2\u00ee\u0584\3\2"+
		"\2\2\u00f0\u0586\3\2\2\2\u00f2\u0591\3\2\2\2\u00f4\u059c\3\2\2\2\u00f6"+
		"\u05a9\3\2\2\2\u00f8\u05ac\3\2\2\2\u00fa\u05f0\3\2\2\2\u00fc\u05f2\3\2"+
		"\2\2\u00fe\u05f6\3\2\2\2\u0100\u05ff\3\2\2\2\u0102\u0601\3\2\2\2\u0104"+
		"\u0603\3\2\2\2\u0106\u0605\3\2\2\2\u0108\u0607\3\2\2\2\u010a\u0609\3\2"+
		"\2\2\u010c\u060d\3\2\2\2\u010e\u060f\3\2\2\2\u0110\u0613\3\2\2\2\u0112"+
		"\u0615\3\2\2\2\u0114\u0119\5\4\3\2\u0115\u011a\5\16\b\2\u0116\u011a\5"+
		"\26\f\2\u0117\u011a\5\30\r\2\u0118\u011a\5\32\16\2\u0119\u0115\3\2\2\2"+
		"\u0119\u0116\3\2\2\2\u0119\u0117\3\2\2\2\u0119\u0118\3\2\2\2\u0119\u011a"+
		"\3\2\2\2\u011a\u011b\3\2\2\2\u011b\u011c\5\64\33\2\u011c\u011d\7\2\2\3"+
		"\u011d\u0123\3\2\2\2\u011e\u0120\5\66\34\2\u011f\u011e\3\2\2\2\u011f\u0120"+
		"\3\2\2\2\u0120\u0121\3\2\2\2\u0121\u0123\7\2\2\3\u0122\u0114\3\2\2\2\u0122"+
		"\u011f\3\2\2\2\u0123\3\3\2\2\2\u0124\u0126\5\6\4\2\u0125\u0124\3\2\2\2"+
		"\u0126\u0129\3\2\2\2\u0127\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u012e"+
		"\3\2\2\2\u0129\u0127\3\2\2\2\u012a\u012d\5\n\6\2\u012b\u012d\5\f\7\2\u012c"+
		"\u012a\3\2\2\2\u012c\u012b\3\2\2\2\u012d\u0130\3\2\2\2\u012e\u012c\3\2"+
		"\2\2\u012e\u012f\3\2\2\2\u012f\5\3\2\2\2\u0130\u012e\3\2\2\2\u0131\u0132"+
		"\7\4\2\2\u0132\u0133\5\u010e\u0088\2\u0133\u0138\5\b\5\2\u0134\u0135\7"+
		"\u00a0\2\2\u0135\u0137\5\b\5\2\u0136\u0134\3\2\2\2\u0137\u013a\3\2\2\2"+
		"\u0138\u0136\3\2\2\2\u0138\u0139\3\2\2\2\u0139\7\3\2\2\2\u013a\u0138\3"+
		"\2\2\2\u013b\u013f\5\u010c\u0087\2\u013c\u013f\5\u010a\u0086\2\u013d\u013f"+
		"\7\177\2\2\u013e\u013b\3\2\2\2\u013e\u013c\3\2\2\2\u013e\u013d\3\2\2\2"+
		"\u013f\t\3\2\2\2\u0140\u0141\7\5\2\2\u0141\u0142\7x\2\2\u0142\13\3\2\2"+
		"\2\u0143\u0144\7\6\2\2\u0144\u0145\7y\2\2\u0145\u0146\7x\2\2\u0146\r\3"+
		"\2\2\2\u0147\u014b\5\22\n\2\u0148\u014a\5\34\17\2\u0149\u0148\3\2\2\2"+
		"\u014a\u014d\3\2\2\2\u014b\u0149\3\2\2\2\u014b\u014c\3\2\2\2\u014c\u014e"+
		"\3\2\2\2\u014d\u014b\3\2\2\2\u014e\u014f\5\36\20\2\u014f\u0150\5 \21\2"+
		"\u0150\17\3\2\2\2\u0151\u0152\5\22\n\2\u0152\u0153\5\36\20\2\u0153\u0154"+
		"\5 \21\2\u0154\u0155\5\64\33\2\u0155\21\3\2\2\2\u0156\u0158\7\7\2\2\u0157"+
		"\u0159\t\2\2\2\u0158\u0157\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u0160\3\2"+
		"\2\2\u015a\u015c\5\24\13\2\u015b\u015a\3\2\2\2\u015c\u015d\3\2\2\2\u015d"+
		"\u015b\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u0161\3\2\2\2\u015f\u0161\7\u009e"+
		"\2\2\u0160\u015b\3\2\2\2\u0160\u015f\3\2\2\2\u0161\23\3\2\2\2\u0162\u016a"+
		"\5\u00e0q\2\u0163\u0164\7\u0094\2\2\u0164\u0165\5\u00e6t\2\u0165\u0166"+
		"\7;\2\2\u0166\u0167\5\u00e0q\2\u0167\u0168\7\u0095\2\2\u0168\u016a\3\2"+
		"\2\2\u0169\u0162\3\2\2\2\u0169\u0163\3\2\2\2\u016a\25\3\2\2\2\u016b\u0183"+
		"\7\n\2\2\u016c\u0170\5\u0094K\2\u016d\u016f\5\34\17\2\u016e\u016d\3\2"+
		"\2\2\u016f\u0172\3\2\2\2\u0170\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171"+
		"\u0173\3\2\2\2\u0172\u0170\3\2\2\2\u0173\u0174\5\36\20\2\u0174\u0175\5"+
		" \21\2\u0175\u0184\3\2\2\2\u0176\u0178\5\34\17\2\u0177\u0176\3\2\2\2\u0178"+
		"\u017b\3\2\2\2\u0179\u0177\3\2\2\2\u0179\u017a\3\2\2\2\u017a\u017c\3\2"+
		"\2\2\u017b\u0179\3\2\2\2\u017c\u017d\7\17\2\2\u017d\u017f\7\u0096\2\2"+
		"\u017e\u0180\5f\64\2\u017f\u017e\3\2\2\2\u017f\u0180\3\2\2\2\u0180\u0181"+
		"\3\2\2\2\u0181\u0182\7\u0097\2\2\u0182\u0184\5 \21\2\u0183\u016c\3\2\2"+
		"\2\u0183\u0179\3\2\2\2\u0184\27\3\2\2\2\u0185\u018c\7\13\2\2\u0186\u0188"+
		"\5\u00dep\2\u0187\u0186\3\2\2\2\u0188\u0189\3\2\2\2\u0189\u0187\3\2\2"+
		"\2\u0189\u018a\3\2\2\2\u018a\u018d\3\2\2\2\u018b\u018d\7\u009e\2\2\u018c"+
		"\u0187\3\2\2\2\u018c\u018b\3\2\2\2\u018d\u0191\3\2\2\2\u018e\u0190\5\34"+
		"\17\2\u018f\u018e\3\2\2\2\u0190\u0193\3\2\2\2\u0191\u018f\3\2\2\2\u0191"+
		"\u0192\3\2\2\2\u0192\u0195\3\2\2\2\u0193\u0191\3\2\2\2\u0194\u0196\5\36"+
		"\20\2\u0195\u0194\3\2\2\2\u0195\u0196\3\2\2\2\u0196\u0197\3\2\2\2\u0197"+
		"\u0198\5 \21\2\u0198\31\3\2\2\2\u0199\u019d\7\f\2\2\u019a\u019c\5\34\17"+
		"\2\u019b\u019a\3\2\2\2\u019c\u019f\3\2\2\2\u019d\u019b\3\2\2\2\u019d\u019e"+
		"\3\2\2\2\u019e\u01a0\3\2\2\2\u019f\u019d\3\2\2\2\u01a0\u01a1\5\36\20\2"+
		"\u01a1\u01a2\5 \21\2\u01a2\33\3\2\2\2\u01a3\u01a5\7\r\2\2\u01a4\u01a6"+
		"\7\16\2\2\u01a5\u01a4\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\u01a7\3\2\2\2"+
		"\u01a7\u01a8\5\u010c\u0087\2\u01a8\35\3\2\2\2\u01a9\u01ab\7\17\2\2\u01aa"+
		"\u01a9\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01ad\5h"+
		"\65\2\u01ad\37\3\2\2\2\u01ae\u01b0\5\"\22\2\u01af\u01ae\3\2\2\2\u01af"+
		"\u01b0\3\2\2\2\u01b0\u01b2\3\2\2\2\u01b1\u01b3\5&\24\2\u01b2\u01b1\3\2"+
		"\2\2\u01b2\u01b3\3\2\2\2\u01b3\u01b5\3\2\2\2\u01b4\u01b6\5*\26\2\u01b5"+
		"\u01b4\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\u01b8\3\2\2\2\u01b7\u01b9\5."+
		"\30\2\u01b8\u01b7\3\2\2\2\u01b8\u01b9\3\2\2\2\u01b9!\3\2\2\2\u01ba\u01bb"+
		"\7<\2\2\u01bb\u01bd\7\21\2\2\u01bc\u01be\5$\23\2\u01bd\u01bc\3\2\2\2\u01be"+
		"\u01bf\3\2\2\2\u01bf\u01bd\3\2\2\2\u01bf\u01c0\3\2\2\2\u01c0#\3\2\2\2"+
		"\u01c1\u01cd\5\u00eex\2\u01c2\u01cd\5\u008eH\2\u01c3\u01c4\7\u0094\2\2"+
		"\u01c4\u01c7\5\u00e6t\2\u01c5\u01c6\7;\2\2\u01c6\u01c8\5\u00e0q\2\u01c7"+
		"\u01c5\3\2\2\2\u01c7\u01c8\3\2\2\2\u01c8\u01c9\3\2\2\2\u01c9\u01ca\7\u0095"+
		"\2\2\u01ca\u01cd\3\2\2\2\u01cb\u01cd\5\u00e0q\2\u01cc\u01c1\3\2\2\2\u01cc"+
		"\u01c2\3\2\2\2\u01cc\u01c3\3\2\2\2\u01cc\u01cb\3\2\2\2\u01cd%\3\2\2\2"+
		"\u01ce\u01d0\7=\2\2\u01cf\u01d1\5(\25\2\u01d0\u01cf\3\2\2\2\u01d1\u01d2"+
		"\3\2\2\2\u01d2\u01d0\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\'\3\2\2\2\u01d4"+
		"\u01d5\5\u008cG\2\u01d5)\3\2\2\2\u01d6\u01d7\7\20\2\2\u01d7\u01d9\7\21"+
		"\2\2\u01d8\u01da\5,\27\2\u01d9\u01d8\3\2\2\2\u01da\u01db\3\2\2\2\u01db"+
		"\u01d9\3\2\2\2\u01db\u01dc\3\2\2\2\u01dc+\3\2\2\2\u01dd\u01de\t\3\2\2"+
		"\u01de\u01df\7\u0094\2\2\u01df\u01e0\5\u00e6t\2\u01e0\u01e1\7\u0095\2"+
		"\2\u01e1\u01e5\3\2\2\2\u01e2\u01e5\5\u008cG\2\u01e3\u01e5\5\u00e0q\2\u01e4"+
		"\u01dd\3\2\2\2\u01e4\u01e2\3\2\2\2\u01e4\u01e3\3\2\2\2\u01e5-\3\2\2\2"+
		"\u01e6\u01e8\5\60\31\2\u01e7\u01e9\5\62\32\2\u01e8\u01e7\3\2\2\2\u01e8"+
		"\u01e9\3\2\2\2\u01e9\u01ef\3\2\2\2\u01ea\u01ec\5\62\32\2\u01eb\u01ed\5"+
		"\60\31\2\u01ec\u01eb\3\2\2\2\u01ec\u01ed\3\2\2\2\u01ed\u01ef\3\2\2\2\u01ee"+
		"\u01e6\3\2\2\2\u01ee\u01ea\3\2\2\2\u01ef/\3\2\2\2\u01f0\u01f1\7\24\2\2"+
		"\u01f1\u01f2\7\177\2\2\u01f2\61\3\2\2\2\u01f3\u01f4\7\25\2\2\u01f4\u01f5"+
		"\7\177\2\2\u01f5\63\3\2\2\2\u01f6\u01f7\7\26\2\2\u01f7\u01f9\5|?\2\u01f8"+
		"\u01f6\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9\65\3\2\2\2\u01fa\u0209\5\4\3"+
		"\2\u01fb\u0202\58\35\2\u01fc\u01fd\7\u009a\2\2\u01fd\u01fe\5\4\3\2\u01fe"+
		"\u01ff\58\35\2\u01ff\u0201\3\2\2\2\u0200\u01fc\3\2\2\2\u0201\u0204\3\2"+
		"\2\2\u0202\u0200\3\2\2\2\u0202\u0203\3\2\2\2\u0203\u0207\3\2\2\2\u0204"+
		"\u0202\3\2\2\2\u0205\u0206\7\u009a\2\2\u0206\u0208\5\4\3\2\u0207\u0205"+
		"\3\2\2\2\u0207\u0208\3\2\2\2\u0208\u020a\3\2\2\2\u0209\u01fb\3\2\2\2\u0209"+
		"\u020a\3\2\2\2\u020a\67\3\2\2\2\u020b\u0217\5:\36\2\u020c\u0217\5<\37"+
		"\2\u020d\u0217\5> \2\u020e\u0217\5B\"\2\u020f\u0217\5D#\2\u0210\u0217"+
		"\5F$\2\u0211\u0217\5@!\2\u0212\u0217\5H%\2\u0213\u0217\5J&\2\u0214\u0217"+
		"\5L\'\2\u0215\u0217\5N(\2\u0216\u020b\3\2\2\2\u0216\u020c\3\2\2\2\u0216"+
		"\u020d\3\2\2\2\u0216\u020e\3\2\2\2\u0216\u020f\3\2\2\2\u0216\u0210\3\2"+
		"\2\2\u0216\u0211\3\2\2\2\u0216\u0212\3\2\2\2\u0216\u0213\3\2\2\2\u0216"+
		"\u0214\3\2\2\2\u0216\u0215\3\2\2\2\u02179\3\2\2\2\u0218\u021a\7*\2\2\u0219"+
		"\u021b\7\64\2\2\u021a\u0219\3\2\2\2\u021a\u021b\3\2\2\2\u021b\u021c\3"+
		"\2\2\2\u021c\u021f\5\u010c\u0087\2\u021d\u021e\79\2\2\u021e\u0220\5X-"+
		"\2\u021f\u021d\3\2\2\2\u021f\u0220\3\2\2\2\u0220;\3\2\2\2\u0221\u0223"+
		"\7+\2\2\u0222\u0224\7\64\2\2\u0223\u0222\3\2\2\2\u0223\u0224\3\2\2\2\u0224"+
		"\u0225\3\2\2\2\u0225\u0226\5Z.\2\u0226=\3\2\2\2\u0227\u0229\7,\2\2\u0228"+
		"\u022a\7\64\2\2\u0229\u0228\3\2\2\2\u0229\u022a\3\2\2\2\u022a\u022b\3"+
		"\2\2\2\u022b\u022c\5Z.\2\u022c?\3\2\2\2\u022d\u022f\7\60\2\2\u022e\u0230"+
		"\7\64\2\2\u022f\u022e\3\2\2\2\u022f\u0230\3\2\2\2\u0230\u0231\3\2\2\2"+
		"\u0231\u0232\5X-\2\u0232A\3\2\2\2\u0233\u0235\7-\2\2\u0234\u0236\7\64"+
		"\2\2\u0235\u0234\3\2\2\2\u0235\u0236\3\2\2\2\u0236\u0237\3\2\2\2\u0237"+
		"\u0238\5V,\2\u0238\u0239\7:\2\2\u0239\u023a\5V,\2\u023aC\3\2\2\2\u023b"+
		"\u023d\7.\2\2\u023c\u023e\7\64\2\2\u023d\u023c\3\2\2\2\u023d\u023e\3\2"+
		"\2\2\u023e\u023f\3\2\2\2\u023f\u0240\5V,\2\u0240\u0241\7:\2\2\u0241\u0242"+
		"\5V,\2\u0242E\3\2\2\2\u0243\u0245\7/\2\2\u0244\u0246\7\64\2\2\u0245\u0244"+
		"\3\2\2\2\u0245\u0246\3\2\2\2\u0246\u0247\3\2\2\2\u0247\u0248\5V,\2\u0248"+
		"\u0249\7:\2\2\u0249\u024a\5V,\2\u024aG\3\2\2\2\u024b\u024c\7\62\2\2\u024c"+
		"\u024d\7\67\2\2\u024d\u024e\5^\60\2\u024eI\3\2\2\2\u024f\u0250\7\61\2"+
		"\2\u0250\u0251\7\67\2\2\u0251\u0252\5^\60\2\u0252K\3\2\2\2\u0253\u0254"+
		"\7\61\2\2\u0254\u0255\7\17\2\2\u0255\u0256\5\\/\2\u0256M\3\2\2\2\u0257"+
		"\u0258\78\2\2\u0258\u025a\5\u010c\u0087\2\u0259\u0257\3\2\2\2\u0259\u025a"+
		"\3\2\2\2\u025a\u0260\3\2\2\2\u025b\u025d\5P)\2\u025c\u025e\5R*\2\u025d"+
		"\u025c\3\2\2\2\u025d\u025e\3\2\2\2\u025e\u0261\3\2\2\2\u025f\u0261\5R"+
		"*\2\u0260\u025b\3\2\2\2\u0260\u025f\3\2\2\2\u0261\u0265\3\2\2\2\u0262"+
		"\u0264\5T+\2\u0263\u0262\3\2\2\2\u0264\u0267\3\2\2\2\u0265\u0263\3\2\2"+
		"\2\u0265\u0266\3\2\2\2\u0266\u0268\3\2\2\2\u0267\u0265\3\2\2\2\u0268\u0269"+
		"\7\17\2\2\u0269\u026a\5h\65\2\u026aO\3\2\2\2\u026b\u026c\7\61\2\2\u026c"+
		"\u026d\5\\/\2\u026dQ\3\2\2\2\u026e\u026f\7\62\2\2\u026f\u0270\5\\/\2\u0270"+
		"S\3\2\2\2\u0271\u0273\7\63\2\2\u0272\u0274\7\16\2\2\u0273\u0272\3\2\2"+
		"\2\u0273\u0274\3\2\2\2\u0274\u0275\3\2\2\2\u0275\u0276\5\u010c\u0087\2"+
		"\u0276U\3\2\2\2\u0277\u027d\7\65\2\2\u0278\u027a\7\30\2\2\u0279\u0278"+
		"\3\2\2\2\u0279\u027a\3\2\2\2\u027a\u027b\3\2\2\2\u027b\u027d\5\u010c\u0087"+
		"\2\u027c\u0277\3\2\2\2\u027c\u0279\3\2\2\2\u027dW\3\2\2\2\u027e\u027f"+
		"\7\30\2\2\u027f\u0280\5\u010c\u0087\2\u0280Y\3\2\2\2\u0281\u0286\5X-\2"+
		"\u0282\u0286\7\65\2\2\u0283\u0286\7\16\2\2\u0284\u0286\7\66\2\2\u0285"+
		"\u0281\3\2\2\2\u0285\u0282\3\2\2\2\u0285\u0283\3\2\2\2\u0285\u0284\3\2"+
		"\2\2\u0286[\3\2\2\2\u0287\u0288\7\u0096\2\2\u0288\u0289\5`\61\2\u0289"+
		"\u028a\7\u0097\2\2\u028a]\3\2\2\2\u028b\u028c\7\u0096\2\2\u028c\u028d"+
		"\5`\61\2\u028d\u028e\7\u0097\2\2\u028e_\3\2\2\2\u028f\u0291\5f\64\2\u0290"+
		"\u028f\3\2\2\2\u0290\u0291\3\2\2\2\u0291\u0295\3\2\2\2\u0292\u0294\5b"+
		"\62\2\u0293\u0292\3\2\2\2\u0294\u0297\3\2\2\2\u0295\u0293\3\2\2\2\u0295"+
		"\u0296\3\2\2\2\u0296a\3\2\2\2\u0297\u0295\3\2\2\2\u0298\u029a\5d\63\2"+
		"\u0299\u029b\7\u009b\2\2\u029a\u0299\3\2\2\2\u029a\u029b\3\2\2\2\u029b"+
		"\u029d\3\2\2\2\u029c\u029e\5f\64\2\u029d\u029c\3\2\2\2\u029d\u029e\3\2"+
		"\2\2\u029ec\3\2\2\2\u029f\u02a0\7\30\2\2\u02a0\u02a1\5\u00dep\2\u02a1"+
		"\u02a3\7\u0096\2\2\u02a2\u02a4\5f\64\2\u02a3\u02a2\3\2\2\2\u02a3\u02a4"+
		"\3\2\2\2\u02a4\u02a5\3\2\2\2\u02a5\u02a6\7\u0097\2\2\u02a6e\3\2\2\2\u02a7"+
		"\u02ae\5\u0098M\2\u02a8\u02aa\7\u009b\2\2\u02a9\u02ab\5\u0098M\2\u02aa"+
		"\u02a9\3\2\2\2\u02aa\u02ab\3\2\2\2\u02ab\u02ad\3\2\2\2\u02ac\u02a8\3\2"+
		"\2\2\u02ad\u02b0\3\2\2\2\u02ae\u02ac\3\2\2\2\u02ae\u02af\3\2\2\2\u02af"+
		"g\3\2\2\2\u02b0\u02ae\3\2\2\2\u02b1\u02b4\7\u0096\2\2\u02b2\u02b5\5\20"+
		"\t\2\u02b3\u02b5\5j\66\2\u02b4\u02b2\3\2\2\2\u02b4\u02b3\3\2\2\2\u02b5"+
		"\u02b6\3\2\2\2\u02b6\u02b7\7\u0097\2\2\u02b7i\3\2\2\2\u02b8\u02ba\5n8"+
		"\2\u02b9\u02b8\3\2\2\2\u02b9\u02ba\3\2\2\2\u02ba\u02be\3\2\2\2\u02bb\u02bd"+
		"\5l\67\2\u02bc\u02bb\3\2\2\2\u02bd\u02c0\3\2\2\2\u02be\u02bc\3\2\2\2\u02be"+
		"\u02bf\3\2\2\2\u02bfk\3\2\2\2\u02c0\u02be\3\2\2\2\u02c1\u02c3\5p9\2\u02c2"+
		"\u02c4\7\u009b\2\2\u02c3\u02c2\3\2\2\2\u02c3\u02c4\3\2\2\2\u02c4\u02c6"+
		"\3\2\2\2\u02c5\u02c7\5n8\2\u02c6\u02c5\3\2\2\2\u02c6\u02c7\3\2\2\2\u02c7"+
		"m\3\2\2\2\u02c8\u02cf\5\u00a4S\2\u02c9\u02cb\7\u009b\2\2\u02ca\u02cc\5"+
		"\u00a4S\2\u02cb\u02ca\3\2\2\2\u02cb\u02cc\3\2\2\2\u02cc\u02ce\3\2\2\2"+
		"\u02cd\u02c9\3\2\2\2\u02ce\u02d1\3\2\2\2\u02cf\u02cd\3\2\2\2\u02cf\u02d0"+
		"\3\2\2\2\u02d0o\3\2\2\2\u02d1\u02cf\3\2\2\2\u02d2\u02db\5\u0088E\2\u02d3"+
		"\u02db\5r:\2\u02d4\u02db\5\u0086D\2\u02d5\u02db\5t;\2\u02d6\u02db\5v<"+
		"\2\u02d7\u02db\5\u008aF\2\u02d8\u02db\5x=\2\u02d9\u02db\5z>\2\u02da\u02d2"+
		"\3\2\2\2\u02da\u02d3\3\2\2\2\u02da\u02d4\3\2\2\2\u02da\u02d5\3\2\2\2\u02da"+
		"\u02d6\3\2\2\2\u02da\u02d7\3\2\2\2\u02da\u02d8\3\2\2\2\u02da\u02d9\3\2"+
		"\2\2\u02dbq\3\2\2\2\u02dc\u02dd\7\27\2\2\u02dd\u02de\5h\65\2\u02des\3"+
		"\2\2\2\u02df\u02e0\7\30\2\2\u02e0\u02e1\5\u00dep\2\u02e1\u02e2\5h\65\2"+
		"\u02e2u\3\2\2\2\u02e3\u02e5\7@\2\2\u02e4\u02e6\7\64\2\2\u02e5\u02e4\3"+
		"\2\2\2\u02e5\u02e6\3\2\2\2\u02e6\u02e7\3\2\2\2\u02e7\u02e8\5\u00dep\2"+
		"\u02e8\u02e9\5h\65\2\u02e9w\3\2\2\2\u02ea\u02eb\7A\2\2\u02eb\u02ec\7\u0094"+
		"\2\2\u02ec\u02ed\5\u00e6t\2\u02ed\u02ee\7;\2\2\u02ee\u02ef\5\u00e0q\2"+
		"\u02ef\u02f0\7\u0095\2\2\u02f0y\3\2\2\2\u02f1\u02f2\7\26\2\2\u02f2\u02f3"+
		"\5|?\2\u02f3{\3\2\2\2\u02f4\u02f7\5~@\2\u02f5\u02f7\5\u0080A\2\u02f6\u02f4"+
		"\3\2\2\2\u02f6\u02f5\3\2\2\2\u02f7}\3\2\2\2\u02f8\u02f9\5\u00e0q\2\u02f9"+
		"\u02fd\7\u0096\2\2\u02fa\u02fc\5\u0084C\2\u02fb\u02fa\3\2\2\2\u02fc\u02ff"+
		"\3\2\2\2\u02fd\u02fb\3\2\2\2\u02fd\u02fe\3\2\2\2\u02fe\u0300\3\2\2\2\u02ff"+
		"\u02fd\3\2\2\2\u0300\u0301\7\u0097\2\2\u0301\177\3\2\2\2\u0302\u0306\7"+
		"\u0094\2\2\u0303\u0305\5\u00e0q\2\u0304\u0303\3\2\2\2\u0305\u0308\3\2"+
		"\2\2\u0306\u0304\3\2\2\2\u0306\u0307\3\2\2\2\u0307\u0309\3\2\2\2\u0308"+
		"\u0306\3\2\2\2\u0309\u030a\7\u0095\2\2\u030a\u030e\7\u0096\2\2\u030b\u030d"+
		"\5\u0082B\2\u030c\u030b\3\2\2\2\u030d\u0310\3\2\2\2\u030e\u030c\3\2\2"+
		"\2\u030e\u030f\3\2\2\2\u030f\u0311\3\2\2\2\u0310\u030e\3\2\2\2\u0311\u0312"+
		"\7\u0097\2\2\u0312\u0081\3\2\2\2\u0313\u0317\7\u0094\2\2\u0314\u0316\5"+
		"\u0084C\2\u0315\u0314\3\2\2\2\u0316\u0319\3\2\2\2\u0317\u0315\3\2\2\2"+
		"\u0317\u0318\3\2\2\2\u0318\u031a\3\2\2\2\u0319\u0317\3\2\2\2\u031a\u031b"+
		"\7\u0095\2\2\u031b\u0083\3\2\2\2\u031c\u0322\5\u010c\u0087\2\u031d\u0322"+
		"\5\u00fe\u0080\2\u031e\u0322\5\u0100\u0081\2\u031f\u0322\5\u0108\u0085"+
		"\2\u0320\u0322\7>\2\2\u0321\u031c\3\2\2\2\u0321\u031d\3\2\2\2\u0321\u031e"+
		"\3\2\2\2\u0321\u031f\3\2\2\2\u0321\u0320\3\2\2\2\u0322\u0085\3\2\2\2\u0323"+
		"\u0324\7B\2\2\u0324\u0325\5h\65\2\u0325\u0087\3\2\2\2\u0326\u032b\5h\65"+
		"\2\u0327\u0328\7\31\2\2\u0328\u032a\5h\65\2\u0329\u0327\3\2\2\2\u032a"+
		"\u032d\3\2\2\2\u032b\u0329\3\2\2\2\u032b\u032c\3\2\2\2\u032c\u0089\3\2"+
		"\2\2\u032d\u032b\3\2\2\2\u032e\u032f\7\32\2\2\u032f\u0330\5\u008cG\2\u0330"+
		"\u008b\3\2\2\2\u0331\u0332\7\u0094\2\2\u0332\u0333\5\u00e6t\2\u0333\u0334"+
		"\7\u0095\2\2\u0334\u0338\3\2\2\2\u0335\u0338\5\u00eex\2\u0336\u0338\5"+
		"\u008eH\2\u0337\u0331\3\2\2\2\u0337\u0335\3\2\2\2\u0337\u0336\3\2\2\2"+
		"\u0338\u008d\3\2\2\2\u0339\u033a\5\u010c\u0087\2\u033a\u033b\5\u0090I"+
		"\2\u033b\u008f\3\2\2\2\u033c\u0342\7\u0094\2\2\u033d\u033f\7\b\2\2\u033e"+
		"\u033d\3\2\2\2\u033e\u033f\3\2\2\2\u033f\u0340\3\2\2\2\u0340\u0343\5\u0092"+
		"J\2\u0341\u0343\3\2\2\2\u0342\u033e\3\2\2\2\u0342\u0341\3\2\2\2\u0343"+
		"\u0344\3\2\2\2\u0344\u0345\7\u0095\2\2\u0345\u0091\3\2\2\2\u0346\u034b"+
		"\5\u00e6t\2\u0347\u0348\7\u00a0\2\2\u0348\u034a\5\u00e6t\2\u0349\u0347"+
		"\3\2\2\2\u034a\u034d\3\2\2\2\u034b\u0349\3\2\2\2\u034b\u034c\3\2\2\2\u034c"+
		"\u0093\3\2\2\2\u034d\u034b\3\2\2\2\u034e\u0350\7\u0096\2\2\u034f\u0351"+
		"\5\u0096L\2\u0350\u034f\3\2\2\2\u0350\u0351\3\2\2\2\u0351\u0352\3\2\2"+
		"\2\u0352\u0353\7\u0097\2\2\u0353\u0095\3\2\2\2\u0354\u035b\5\u0098M\2"+
		"\u0355\u0357\7\u009b\2\2\u0356\u0358\5\u0096L\2\u0357\u0356\3\2\2\2\u0357"+
		"\u0358\3\2\2\2\u0358\u035a\3\2\2\2\u0359\u0355\3\2\2\2\u035a\u035d\3\2"+
		"\2\2\u035b\u0359\3\2\2\2\u035b\u035c\3\2\2\2\u035c\u0097\3\2\2\2\u035d"+
		"\u035b\3\2\2\2\u035e\u035f\5\u00dco\2\u035f\u0360\5\u009cO\2\u0360\u0365"+
		"\3\2\2\2\u0361\u0362\5\u00ccg\2\u0362\u0363\5\u009aN\2\u0363\u0365\3\2"+
		"\2\2\u0364\u035e\3\2\2\2\u0364\u0361\3\2\2\2\u0365\u0099\3\2\2\2\u0366"+
		"\u0368\5\u009cO\2\u0367\u0366\3\2\2\2\u0367\u0368\3\2\2\2\u0368\u009b"+
		"\3\2\2\2\u0369\u036a\5\u009eP\2\u036a\u0373\5\u00a0Q\2\u036b\u036f\7\u009a"+
		"\2\2\u036c\u036d\5\u009eP\2\u036d\u036e\5\u00a0Q\2\u036e\u0370\3\2\2\2"+
		"\u036f\u036c\3\2\2\2\u036f\u0370\3\2\2\2\u0370\u0372\3\2\2\2\u0371\u036b"+
		"\3\2\2\2\u0372\u0375\3\2\2\2\u0373\u0371\3\2\2\2\u0373\u0374\3\2\2\2\u0374"+
		"\u009d\3\2\2\2\u0375\u0373\3\2\2\2\u0376\u0379\5\u00dep\2\u0377\u0379"+
		"\7\33\2\2\u0378\u0376\3\2\2\2\u0378\u0377\3\2\2\2\u0379\u009f\3\2\2\2"+
		"\u037a\u037f\5\u00a2R\2\u037b\u037c\7\u00a0\2\2\u037c\u037e\5\u00a2R\2"+
		"\u037d\u037b\3\2\2\2\u037e\u0381\3\2\2\2\u037f\u037d\3\2\2\2\u037f\u0380"+
		"\3\2\2\2\u0380\u00a1\3\2\2\2\u0381\u037f\3\2\2\2\u0382\u0383\5\u00d8m"+
		"\2\u0383\u00a3\3\2\2\2\u0384\u0385\5\u00dco\2\u0385\u0386\5\u00a8U\2\u0386"+
		"\u038b\3\2\2\2\u0387\u0388\5\u00d0i\2\u0388\u0389\5\u00a6T\2\u0389\u038b"+
		"\3\2\2\2\u038a\u0384\3\2\2\2\u038a\u0387\3\2\2\2\u038b\u00a5\3\2\2\2\u038c"+
		"\u038e\5\u00a8U\2\u038d\u038c\3\2\2\2\u038d\u038e\3\2\2\2\u038e\u00a7"+
		"\3\2\2\2\u038f\u0392\5\u00acW\2\u0390\u0392\5\u00aeX\2\u0391\u038f\3\2"+
		"\2\2\u0391\u0390\3\2\2\2\u0392\u0393\3\2\2\2\u0393\u039a\5\u00b0Y\2\u0394"+
		"\u0396\7\u009a\2\2\u0395\u0397\5\u00aaV\2\u0396\u0395\3\2\2\2\u0396\u0397"+
		"\3\2\2\2\u0397\u0399\3\2\2\2\u0398\u0394\3\2\2\2\u0399\u039c\3\2\2\2\u039a"+
		"\u0398\3\2\2\2\u039a\u039b\3\2\2\2\u039b\u00a9\3\2\2\2\u039c\u039a\3\2"+
		"\2\2\u039d\u03a0\5\u00acW\2\u039e\u03a0\5\u00aeX\2\u039f\u039d\3\2\2\2"+
		"\u039f\u039e\3\2\2\2\u03a0\u03a1\3\2\2\2\u03a1\u03a2\5\u00b0Y\2\u03a2"+
		"\u00ab\3\2\2\2\u03a3\u03a4\5\u00b8]\2\u03a4\u00ad\3\2\2\2\u03a5\u03a6"+
		"\5\u00e0q\2\u03a6\u00af\3\2\2\2\u03a7\u03ac\5\u00b2Z\2\u03a8\u03a9\7\u00a0"+
		"\2\2\u03a9\u03ab\5\u00b2Z\2\u03aa\u03a8\3\2\2\2\u03ab\u03ae\3\2\2\2\u03ac"+
		"\u03aa\3\2\2\2\u03ac\u03ad\3\2\2\2\u03ad\u00b1\3\2\2\2\u03ae\u03ac\3\2"+
		"\2\2\u03af\u03b1\5\u00dan\2\u03b0\u03b2\5\u00b4[\2\u03b1\u03b0\3\2\2\2"+
		"\u03b1\u03b2\3\2\2\2\u03b2\u00b3\3\2\2\2\u03b3\u03b4\7v\2\2\u03b4\u03b5"+
		"\7\u0094\2\2\u03b5\u03b6\5\u00b6\\\2\u03b6\u03b7\7\u0095\2\2\u03b7\u00b5"+
		"\3\2\2\2\u03b8\u03b9\7w\2\2\u03b9\u03ba\5\u010a\u0086\2\u03ba\u00b7\3"+
		"\2\2\2\u03bb\u03bc\5\u00ba^\2\u03bc\u00b9\3\2\2\2\u03bd\u03c2\5\u00bc"+
		"_\2\u03be\u03bf\7\u00a6\2\2\u03bf\u03c1\5\u00bc_\2\u03c0\u03be\3\2\2\2"+
		"\u03c1\u03c4\3\2\2\2\u03c2\u03c0\3\2\2\2\u03c2\u03c3\3\2\2\2\u03c3\u00bb"+
		"\3\2\2\2\u03c4\u03c2\3\2\2\2\u03c5\u03ca\5\u00c0a\2\u03c6\u03c7\7\u00a2"+
		"\2\2\u03c7\u03c9\5\u00c0a\2\u03c8\u03c6\3\2\2\2\u03c9\u03cc\3\2\2\2\u03ca"+
		"\u03c8\3\2\2\2\u03ca\u03cb\3\2\2\2\u03cb\u00bd\3\2\2\2\u03cc\u03ca\3\2"+
		"\2\2\u03cd\u03cf\5\u00c4c\2\u03ce\u03d0\5\u00c2b\2\u03cf\u03ce\3\2\2\2"+
		"\u03cf\u03d0\3\2\2\2\u03d0\u00bf\3\2\2\2\u03d1\u03d3\7\u0093\2\2\u03d2"+
		"\u03d1\3\2\2\2\u03d2\u03d3\3\2\2\2\u03d3\u03d4\3\2\2\2\u03d4\u03d5\5\u00be"+
		"`\2\u03d5\u00c1\3\2\2\2\u03d6\u03d7\t\4\2\2\u03d7\u00c3\3\2\2\2\u03d8"+
		"\u03e1\5\u010c\u0087\2\u03d9\u03e1\7\33\2\2\u03da\u03db\7\u00a1\2\2\u03db"+
		"\u03e1\5\u00c6d\2\u03dc\u03dd\7\u0094\2\2\u03dd\u03de\5\u00b8]\2\u03de"+
		"\u03df\7\u0095\2\2\u03df\u03e1\3\2\2\2\u03e0\u03d8\3\2\2\2\u03e0\u03d9"+
		"\3\2\2\2\u03e0\u03da\3\2\2\2\u03e0\u03dc\3\2\2\2\u03e1\u00c5\3\2\2\2\u03e2"+
		"\u03f0\5\u00c8e\2\u03e3\u03ec\7\u0094\2\2\u03e4\u03e9\5\u00c8e\2\u03e5"+
		"\u03e6\7\u00a6\2\2\u03e6\u03e8\5\u00c8e\2\u03e7\u03e5\3\2\2\2\u03e8\u03eb"+
		"\3\2\2\2\u03e9\u03e7\3\2\2\2\u03e9\u03ea\3\2\2\2\u03ea\u03ed\3\2\2\2\u03eb"+
		"\u03e9\3\2\2\2\u03ec\u03e4\3\2\2\2\u03ec\u03ed\3\2\2\2\u03ed\u03ee\3\2"+
		"\2\2\u03ee\u03f0\7\u0095\2\2\u03ef\u03e2\3\2\2\2\u03ef\u03e3\3\2\2\2\u03f0"+
		"\u00c7\3\2\2\2\u03f1\u03f3\7\u0093\2\2\u03f2\u03f1\3\2\2\2\u03f2\u03f3"+
		"\3\2\2\2\u03f3\u03f6\3\2\2\2\u03f4\u03f7\5\u010c\u0087\2\u03f5\u03f7\7"+
		"\33\2\2\u03f6\u03f4\3\2\2\2\u03f6\u03f5\3\2\2\2\u03f7\u00c9\3\2\2\2\u03f8"+
		"\u03f9\7\177\2\2\u03f9\u00cb\3\2\2\2\u03fa\u03fd\5\u00d4k\2\u03fb\u03fd"+
		"\5\u00ceh\2\u03fc\u03fa\3\2\2\2\u03fc\u03fb\3\2\2\2\u03fd\u00cd\3\2\2"+
		"\2\u03fe\u03ff\7\u0098\2\2\u03ff\u0400\5\u009cO\2\u0400\u0401\7\u0099"+
		"\2\2\u0401\u00cf\3\2\2\2\u0402\u0405\5\u00d6l\2\u0403\u0405\5\u00d2j\2"+
		"\u0404\u0402\3\2\2\2\u0404\u0403\3\2\2\2\u0405\u00d1\3\2\2\2\u0406\u0407"+
		"\7\u0098\2\2\u0407\u0408\5\u00a8U\2\u0408\u0409\7\u0099\2\2\u0409\u00d3"+
		"\3\2\2\2\u040a\u040c\7\u0094\2\2\u040b\u040d\5\u00d8m\2\u040c\u040b\3"+
		"\2\2\2\u040d\u040e\3\2\2\2\u040e\u040c\3\2\2\2\u040e\u040f\3\2\2\2\u040f"+
		"\u0410\3\2\2\2\u0410\u0411\7\u0095\2\2\u0411\u00d5\3\2\2\2\u0412\u0414"+
		"\7\u0094\2\2\u0413\u0415\5\u00dan\2\u0414\u0413\3\2\2\2\u0415\u0416\3"+
		"\2\2\2\u0416\u0414\3\2\2\2\u0416\u0417\3\2\2\2\u0417\u0418\3\2\2\2\u0418"+
		"\u0419\7\u0095\2\2\u0419\u00d7\3\2\2\2\u041a\u041d\5\u00dco\2\u041b\u041d"+
		"\5\u00ccg\2\u041c\u041a\3\2\2\2\u041c\u041b\3\2\2\2\u041d\u00d9\3\2\2"+
		"\2\u041e\u0421\5\u00dco\2\u041f\u0421\5\u00d0i\2\u0420\u041e\3\2\2\2\u0420"+
		"\u041f\3\2\2\2\u0421\u00db\3\2\2\2\u0422\u0425\5\u00e0q\2\u0423\u0425"+
		"\5\u00e2r\2\u0424\u0422\3\2\2\2\u0424\u0423\3\2\2\2\u0425\u00dd\3\2\2"+
		"\2\u0426\u0429\5\u00e0q\2\u0427\u0429\5\u010c\u0087\2\u0428\u0426\3\2"+
		"\2\2\u0428\u0427\3\2\2\2\u0429\u00df\3\2\2\2\u042a\u042b\t\5\2\2\u042b"+
		"\u00e1\3\2\2\2\u042c\u0433\5\u010c\u0087\2\u042d\u0433\5\u00fe\u0080\2"+
		"\u042e\u0433\5\u0100\u0081\2\u042f\u0433\5\u0108\u0085\2\u0430\u0433\5"+
		"\u0110\u0089\2\u0431\u0433\5\u00e4s\2\u0432\u042c\3\2\2\2\u0432\u042d"+
		"\3\2\2\2\u0432\u042e\3\2\2\2\u0432\u042f\3\2\2\2\u0432\u0430\3\2\2\2\u0432"+
		"\u0431\3\2\2\2\u0433\u00e3\3\2\2\2\u0434\u0435\7\u0094\2\2\u0435\u0436"+
		"\7\u0095\2\2\u0436\u00e5\3\2\2\2\u0437\u0438\bt\1\2\u0438\u043e\5\u00ec"+
		"w\2\u0439\u043a\t\6\2\2\u043a\u043e\5\u00e6t\13\u043b\u043c\7\u00a1\2"+
		"\2\u043c\u043e\5\u00e6t\n\u043d\u0437\3\2\2\2\u043d\u0439\3\2\2\2\u043d"+
		"\u043b\3\2\2\2\u043e\u045c\3\2\2\2\u043f\u0440\f\t\2\2\u0440\u0441\t\7"+
		"\2\2\u0441\u045b\5\u00e6t\n\u0442\u0443\f\b\2\2\u0443\u0444\t\6\2\2\u0444"+
		"\u045b\5\u00e6t\t\u0445\u0446\f\5\2\2\u0446\u0447\t\b\2\2\u0447\u045b"+
		"\5\u00e6t\6\u0448\u0449\f\7\2\2\u0449\u045b\5\u00e8u\2\u044a\u044c\f\6"+
		"\2\2\u044b\u044d\7r\2\2\u044c\u044b\3\2\2\2\u044c\u044d\3\2\2\2\u044d"+
		"\u044e\3\2\2\2\u044e\u044f\7s\2\2\u044f\u0451\7\u0094\2\2\u0450\u0452"+
		"\5\u0092J\2\u0451\u0450\3\2\2\2\u0451\u0452\3\2\2\2\u0452\u0453\3\2\2"+
		"\2\u0453\u045b\7\u0095\2\2\u0454\u0455\f\4\2\2\u0455\u0456\7\u0091\2\2"+
		"\u0456\u045b\5\u00e6t\2\u0457\u0458\f\3\2\2\u0458\u0459\7\u0092\2\2\u0459"+
		"\u045b\5\u00e6t\2\u045a\u043f\3\2\2\2\u045a\u0442\3\2\2\2\u045a\u0445"+
		"\3\2\2\2\u045a\u0448\3\2\2\2\u045a\u044a\3\2\2\2\u045a\u0454\3\2\2\2\u045a"+
		"\u0457\3\2\2\2\u045b\u045e\3\2\2\2\u045c\u045a\3\2\2\2\u045c\u045d\3\2"+
		"\2\2\u045d\u00e7\3\2\2\2\u045e\u045c\3\2\2\2\u045f\u0462\5\u0104\u0083"+
		"\2\u0460\u0462\5\u0106\u0084\2\u0461\u045f\3\2\2\2\u0461\u0460\3\2\2\2"+
		"\u0462\u0465\3\2\2\2\u0463\u0464\t\7\2\2\u0464\u0466\5\u00eav\2\u0465"+
		"\u0463\3\2\2\2\u0465\u0466\3\2\2\2\u0466\u00e9\3\2\2\2\u0467\u0469\t\t"+
		"\2\2\u0468\u0467\3\2\2\2\u0468\u0469\3\2\2\2\u0469\u046a\3\2\2\2\u046a"+
		"\u046b\5\u00ecw\2\u046b\u00eb\3\2\2\2\u046c\u046d\7\u0094\2\2\u046d\u046e"+
		"\5\u00e6t\2\u046e\u046f\7\u0095\2\2\u046f\u0477\3\2\2\2\u0470\u0477\5"+
		"\u00eex\2\u0471\u0477\5\u00fc\177\2\u0472\u0477\5\u00fe\u0080\2\u0473"+
		"\u0477\5\u0100\u0081\2\u0474\u0477\5\u0108\u0085\2\u0475\u0477\5\u00e0"+
		"q\2\u0476\u046c\3\2\2\2\u0476\u0470\3\2\2\2\u0476\u0471\3\2\2\2\u0476"+
		"\u0472\3\2\2\2\u0476\u0473\3\2\2\2\u0476\u0474\3\2\2\2\u0476\u0475\3\2"+
		"\2\2\u0477\u00ed\3\2\2\2\u0478\u0585\5\u00fa~\2\u0479\u047a\7\34\2\2\u047a"+
		"\u047b\7\u0094\2\2\u047b\u047c\5\u00e6t\2\u047c\u047d\7\u0095\2\2\u047d"+
		"\u0585\3\2\2\2\u047e\u047f\7\35\2\2\u047f\u0480\7\u0094\2\2\u0480\u0481"+
		"\5\u00e6t\2\u0481\u0482\7\u0095\2\2\u0482\u0585\3\2\2\2\u0483\u0484\7"+
		"\36\2\2\u0484\u0485\7\u0094\2\2\u0485\u0486\5\u00e6t\2\u0486\u0487\7\u00a0"+
		"\2\2\u0487\u0488\5\u00e6t\2\u0488\u0489\7\u0095\2\2\u0489\u0585\3\2\2"+
		"\2\u048a\u048b\7\37\2\2\u048b\u048c\7\u0094\2\2\u048c\u048d\5\u00e6t\2"+
		"\u048d\u048e\7\u0095\2\2\u048e\u0585\3\2\2\2\u048f\u0490\7 \2\2\u0490"+
		"\u0491\7\u0094\2\2\u0491\u0492\5\u00e0q\2\u0492\u0493\7\u0095\2\2\u0493"+
		"\u0585\3\2\2\2\u0494\u0495\7C\2\2\u0495\u0496\7\u0094\2\2\u0496\u0497"+
		"\5\u00e6t\2\u0497\u0498\7\u0095\2\2\u0498\u0585\3\2\2\2\u0499\u049a\7"+
		"D\2\2\u049a\u049b\7\u0094\2\2\u049b\u049c\5\u00e6t\2\u049c\u049d\7\u0095"+
		"\2\2\u049d\u0585\3\2\2\2\u049e\u049f\7E\2\2\u049f\u04a1\7\u0094\2\2\u04a0"+
		"\u04a2\5\u00e6t\2\u04a1\u04a0\3\2\2\2\u04a1\u04a2\3\2\2\2\u04a2\u04a3"+
		"\3\2\2\2\u04a3\u0585\7\u0095\2\2\u04a4\u04a5\7F\2\2\u04a5\u04a6\7\u0094"+
		"\2\2\u04a6\u0585\7\u0095\2\2\u04a7\u04a8\7G\2\2\u04a8\u04a9\7\u0094\2"+
		"\2\u04a9\u04aa\5\u00e6t\2\u04aa\u04ab\7\u0095\2\2\u04ab\u0585\3\2\2\2"+
		"\u04ac\u04ad\7H\2\2\u04ad\u04ae\7\u0094\2\2\u04ae\u04af\5\u00e6t\2\u04af"+
		"\u04b0\7\u0095\2\2\u04b0\u0585\3\2\2\2\u04b1\u04b2\7I\2\2\u04b2\u04b3"+
		"\7\u0094\2\2\u04b3\u04b4\5\u00e6t\2\u04b4\u04b5\7\u0095\2\2\u04b5\u0585"+
		"\3\2\2\2\u04b6\u04b7\7J\2\2\u04b7\u04b8\7\u0094\2\2\u04b8\u04b9\5\u00e6"+
		"t\2\u04b9\u04ba\7\u0095\2\2\u04ba\u0585\3\2\2\2\u04bb\u04bc\7K\2\2\u04bc"+
		"\u04be\7\u0094\2\2\u04bd\u04bf\5\u0092J\2\u04be\u04bd\3\2\2\2\u04be\u04bf"+
		"\3\2\2\2\u04bf\u04c0\3\2\2\2\u04c0\u0585\7\u0095\2\2\u04c1\u0585\5\u00f2"+
		"z\2\u04c2\u04c3\7L\2\2\u04c3\u04c4\7\u0094\2\2\u04c4\u04c5\5\u00e6t\2"+
		"\u04c5\u04c6\7\u0095\2\2\u04c6\u0585\3\2\2\2\u04c7\u0585\5\u00f4{\2\u04c8"+
		"\u04c9\7M\2\2\u04c9\u04ca\7\u0094\2\2\u04ca\u04cb\5\u00e6t\2\u04cb\u04cc"+
		"\7\u0095\2\2\u04cc\u0585\3\2\2\2\u04cd\u04ce\7N\2\2\u04ce\u04cf\7\u0094"+
		"\2\2\u04cf\u04d0\5\u00e6t\2\u04d0\u04d1\7\u0095\2\2\u04d1\u0585\3\2\2"+
		"\2\u04d2\u04d3\7O\2\2\u04d3\u04d4\7\u0094\2\2\u04d4\u04d5\5\u00e6t\2\u04d5"+
		"\u04d6\7\u0095\2\2\u04d6\u0585\3\2\2\2\u04d7\u04d8\7P\2\2\u04d8\u04d9"+
		"\7\u0094\2\2\u04d9\u04da\5\u00e6t\2\u04da\u04db\7\u00a0\2\2\u04db\u04dc"+
		"\5\u00e6t\2\u04dc\u04dd\7\u0095\2\2\u04dd\u0585\3\2\2\2\u04de\u04df\7"+
		"Q\2\2\u04df\u04e0\7\u0094\2\2\u04e0\u04e1\5\u00e6t\2\u04e1\u04e2\7\u00a0"+
		"\2\2\u04e2\u04e3\5\u00e6t\2\u04e3\u04e4\7\u0095\2\2\u04e4\u0585\3\2\2"+
		"\2\u04e5\u04e6\7R\2\2\u04e6\u04e7\7\u0094\2\2\u04e7\u04e8\5\u00e6t\2\u04e8"+
		"\u04e9\7\u00a0\2\2\u04e9\u04ea\5\u00e6t\2\u04ea\u04eb\7\u0095\2\2\u04eb"+
		"\u0585\3\2\2\2\u04ec\u04ed\7S\2\2\u04ed\u04ee\7\u0094\2\2\u04ee\u04ef"+
		"\5\u00e6t\2\u04ef\u04f0\7\u00a0\2\2\u04f0\u04f1\5\u00e6t\2\u04f1\u04f2"+
		"\7\u0095\2\2\u04f2\u0585\3\2\2\2\u04f3\u04f4\7T\2\2\u04f4\u04f5\7\u0094"+
		"\2\2\u04f5\u04f6\5\u00e6t\2\u04f6\u04f7\7\u00a0\2\2\u04f7\u04f8\5\u00e6"+
		"t\2\u04f8\u04f9\7\u0095\2\2\u04f9\u0585\3\2\2\2\u04fa\u04fb\7V\2\2\u04fb"+
		"\u04fc\7\u0094\2\2\u04fc\u04fd\5\u00e6t\2\u04fd\u04fe\7\u0095\2\2\u04fe"+
		"\u0585\3\2\2\2\u04ff\u0500\7W\2\2\u0500\u0501\7\u0094\2\2\u0501\u0502"+
		"\5\u00e6t\2\u0502\u0503\7\u0095\2\2\u0503\u0585\3\2\2\2\u0504\u0505\7"+
		"X\2\2\u0505\u0506\7\u0094\2\2\u0506\u0507\5\u00e6t\2\u0507\u0508\7\u0095"+
		"\2\2\u0508\u0585\3\2\2\2\u0509\u050a\7Y\2\2\u050a\u050b\7\u0094\2\2\u050b"+
		"\u050c\5\u00e6t\2\u050c\u050d\7\u0095\2\2\u050d\u0585\3\2\2\2\u050e\u050f"+
		"\7Z\2\2\u050f\u0510\7\u0094\2\2\u0510\u0511\5\u00e6t\2\u0511\u0512\7\u0095"+
		"\2\2\u0512\u0585\3\2\2\2\u0513\u0514\7[\2\2\u0514\u0515\7\u0094\2\2\u0515"+
		"\u0516\5\u00e6t\2\u0516\u0517\7\u0095\2\2\u0517\u0585\3\2\2\2\u0518\u0519"+
		"\7\\\2\2\u0519\u051a\7\u0094\2\2\u051a\u051b\5\u00e6t\2\u051b\u051c\7"+
		"\u0095\2\2\u051c\u0585\3\2\2\2\u051d\u051e\7]\2\2\u051e\u051f\7\u0094"+
		"\2\2\u051f\u0520\5\u00e6t\2\u0520\u0521\7\u0095\2\2\u0521\u0585\3\2\2"+
		"\2\u0522\u0523\7^\2\2\u0523\u0524\7\u0094\2\2\u0524\u0585\7\u0095\2\2"+
		"\u0525\u0526\7_\2\2\u0526\u0527\7\u0094\2\2\u0527\u0585\7\u0095\2\2\u0528"+
		"\u0529\7`\2\2\u0529\u052a\7\u0094\2\2\u052a\u0585\7\u0095\2\2\u052b\u052c"+
		"\7a\2\2\u052c\u052d\7\u0094\2\2\u052d\u052e\5\u00e6t\2\u052e\u052f\7\u0095"+
		"\2\2\u052f\u0585\3\2\2\2\u0530\u0531\7b\2\2\u0531\u0532\7\u0094\2\2\u0532"+
		"\u0533\5\u00e6t\2\u0533\u0534\7\u0095\2\2\u0534\u0585\3\2\2\2\u0535\u0536"+
		"\7c\2\2\u0536\u0537\7\u0094\2\2\u0537\u0538\5\u00e6t\2\u0538\u0539\7\u0095"+
		"\2\2\u0539\u0585\3\2\2\2\u053a\u053b\7d\2\2\u053b\u053c\7\u0094\2\2\u053c"+
		"\u053d\5\u00e6t\2\u053d\u053e\7\u0095\2\2\u053e\u0585\3\2\2\2\u053f\u0540"+
		"\7e\2\2\u0540\u0541\7\u0094\2\2\u0541\u0542\5\u00e6t\2\u0542\u0543\7\u0095"+
		"\2\2\u0543\u0585\3\2\2\2\u0544\u0545\7f\2\2\u0545\u0547\7\u0094\2\2\u0546"+
		"\u0548\5\u0092J\2\u0547\u0546\3\2\2\2\u0547\u0548\3\2\2\2\u0548\u0549"+
		"\3\2\2\2\u0549\u0585\7\u0095\2\2\u054a\u054b\7g\2\2\u054b\u054c\7\u0094"+
		"\2\2\u054c\u054d\5\u00e6t\2\u054d\u054e\7\u00a0\2\2\u054e\u054f\5\u00e6"+
		"t\2\u054f\u0550\7\u00a0\2\2\u0550\u0551\5\u00e6t\2\u0551\u0552\7\u0095"+
		"\2\2\u0552\u0585\3\2\2\2\u0553\u0554\7h\2\2\u0554\u0555\7\u0094\2\2\u0555"+
		"\u0556\5\u00e6t\2\u0556\u0557\7\u00a0\2\2\u0557\u0558\5\u00e6t\2\u0558"+
		"\u0559\7\u0095\2\2\u0559\u0585\3\2\2\2\u055a\u055b\7i\2\2\u055b\u055c"+
		"\7\u0094\2\2\u055c\u055d\5\u00e6t\2\u055d\u055e\7\u00a0\2\2\u055e\u055f"+
		"\5\u00e6t\2\u055f\u0560\7\u0095\2\2\u0560\u0585\3\2\2\2\u0561\u0562\7"+
		"!\2\2\u0562\u0563\7\u0094\2\2\u0563\u0564\5\u00e6t\2\u0564\u0565\7\u00a0"+
		"\2\2\u0565\u0566\5\u00e6t\2\u0566\u0567\7\u0095\2\2\u0567\u0585\3\2\2"+
		"\2\u0568\u0569\7\"\2\2\u0569\u056a\7\u0094\2\2\u056a\u056b\5\u00e6t\2"+
		"\u056b\u056c\7\u0095\2\2\u056c\u0585\3\2\2\2\u056d\u056e\7#\2\2\u056e"+
		"\u056f\7\u0094\2\2\u056f\u0570\5\u00e6t\2\u0570\u0571\7\u0095\2\2\u0571"+
		"\u0585\3\2\2\2\u0572\u0573\7$\2\2\u0573\u0574\7\u0094\2\2\u0574\u0575"+
		"\5\u00e6t\2\u0575\u0576\7\u0095\2\2\u0576\u0585\3\2\2\2\u0577\u0578\7"+
		"%\2\2\u0578\u0579\7\u0094\2\2\u0579\u057a\5\u00e6t\2\u057a\u057b\7\u0095"+
		"\2\2\u057b\u0585\3\2\2\2\u057c\u057d\7j\2\2\u057d\u057e\7\u0094\2\2\u057e"+
		"\u057f\5\u00e6t\2\u057f\u0580\7\u0095\2\2\u0580\u0585\3\2\2\2\u0581\u0585"+
		"\5\u00f0y\2\u0582\u0585\5\u00f6|\2\u0583\u0585\5\u00f8}\2\u0584\u0478"+
		"\3\2\2\2\u0584\u0479\3\2\2\2\u0584\u047e\3\2\2\2\u0584\u0483\3\2\2\2\u0584"+
		"\u048a\3\2\2\2\u0584\u048f\3\2\2\2\u0584\u0494\3\2\2\2\u0584\u0499\3\2"+
		"\2\2\u0584\u049e\3\2\2\2\u0584\u04a4\3\2\2\2\u0584\u04a7\3\2\2\2\u0584"+
		"\u04ac\3\2\2\2\u0584\u04b1\3\2\2\2\u0584\u04b6\3\2\2\2\u0584\u04bb\3\2"+
		"\2\2\u0584\u04c1\3\2\2\2\u0584\u04c2\3\2\2\2\u0584\u04c7\3\2\2\2\u0584"+
		"\u04c8\3\2\2\2\u0584\u04cd\3\2\2\2\u0584\u04d2\3\2\2\2\u0584\u04d7\3\2"+
		"\2\2\u0584\u04de\3\2\2\2\u0584\u04e5\3\2\2\2\u0584\u04ec\3\2\2\2\u0584"+
		"\u04f3\3\2\2\2\u0584\u04fa\3\2\2\2\u0584\u04ff\3\2\2\2\u0584\u0504\3\2"+
		"\2\2\u0584\u0509\3\2\2\2\u0584\u050e\3\2\2\2\u0584\u0513\3\2\2\2\u0584"+
		"\u0518\3\2\2\2\u0584\u051d\3\2\2\2\u0584\u0522\3\2\2\2\u0584\u0525\3\2"+
		"\2\2\u0584\u0528\3\2\2\2\u0584\u052b\3\2\2\2\u0584\u0530\3\2\2\2\u0584"+
		"\u0535\3\2\2\2\u0584\u053a\3\2\2\2\u0584\u053f\3\2\2\2\u0584\u0544\3\2"+
		"\2\2\u0584\u054a\3\2\2\2\u0584\u0553\3\2\2\2\u0584\u055a\3\2\2\2\u0584"+
		"\u0561\3\2\2\2\u0584\u0568\3\2\2\2\u0584\u056d\3\2\2\2\u0584\u0572\3\2"+
		"\2\2\u0584\u0577\3\2\2\2\u0584\u057c\3\2\2\2\u0584\u0581\3\2\2\2\u0584"+
		"\u0582\3\2\2\2\u0584\u0583\3\2\2\2\u0585\u00ef\3\2\2\2\u0586\u0587\7&"+
		"\2\2\u0587\u0588\7\u0094\2\2\u0588\u0589\5\u00e6t\2\u0589\u058a\7\u00a0"+
		"\2\2\u058a\u058d\5\u00e6t\2\u058b\u058c\7\u00a0\2\2\u058c\u058e\5\u00e6"+
		"t\2\u058d\u058b\3\2\2\2\u058d\u058e\3\2\2\2\u058e\u058f\3\2\2\2\u058f"+
		"\u0590\7\u0095\2\2\u0590\u00f1\3\2\2\2\u0591\u0592\7\'\2\2\u0592\u0593"+
		"\7\u0094\2\2\u0593\u0594\5\u00e6t\2\u0594\u0595\7\u00a0\2\2\u0595\u0598"+
		"\5\u00e6t\2\u0596\u0597\7\u00a0\2\2\u0597\u0599\5\u00e6t\2\u0598\u0596"+
		"\3\2\2\2\u0598\u0599\3\2\2\2\u0599\u059a\3\2\2\2\u059a\u059b\7\u0095\2"+
		"\2\u059b\u00f3\3\2\2\2\u059c\u059d\7U\2\2\u059d\u059e\7\u0094\2\2\u059e"+
		"\u059f\5\u00e6t\2\u059f\u05a0\7\u00a0\2\2\u05a0\u05a1\5\u00e6t\2\u05a1"+
		"\u05a2\7\u00a0\2\2\u05a2\u05a5\5\u00e6t\2\u05a3\u05a4\7\u00a0\2\2\u05a4"+
		"\u05a6\5\u00e6t\2\u05a5\u05a3\3\2\2\2\u05a5\u05a6\3\2\2\2\u05a6\u05a7"+
		"\3\2\2\2\u05a7\u05a8\7\u0095\2\2\u05a8\u00f5\3\2\2\2\u05a9\u05aa\7t\2"+
		"\2\u05aa\u05ab\5h\65\2\u05ab\u00f7\3\2\2\2\u05ac\u05ad\7r\2\2\u05ad\u05ae"+
		"\7t\2\2\u05ae\u05af\5h\65\2\u05af\u00f9\3\2\2\2\u05b0\u05b1\7k\2\2\u05b1"+
		"\u05b3\7\u0094\2\2\u05b2\u05b4\7\b\2\2\u05b3\u05b2\3\2\2\2\u05b3\u05b4"+
		"\3\2\2\2\u05b4\u05b7\3\2\2\2\u05b5\u05b8\7\u009e\2\2\u05b6\u05b8\5\u00e6"+
		"t\2\u05b7\u05b5\3\2\2\2\u05b7\u05b6\3\2\2\2\u05b8\u05b9\3\2\2\2\u05b9"+
		"\u05f1\7\u0095\2\2\u05ba\u05bb\7l\2\2\u05bb\u05bd\7\u0094\2\2\u05bc\u05be"+
		"\7\b\2\2\u05bd\u05bc\3\2\2\2\u05bd\u05be\3\2\2\2\u05be\u05bf\3\2\2\2\u05bf"+
		"\u05c0\5\u00e6t\2\u05c0\u05c1\7\u0095\2\2\u05c1\u05f1\3\2\2\2\u05c2\u05c3"+
		"\7m\2\2\u05c3\u05c5\7\u0094\2\2\u05c4\u05c6\7\b\2\2\u05c5\u05c4\3\2\2"+
		"\2\u05c5\u05c6\3\2\2\2\u05c6\u05c7\3\2\2\2\u05c7\u05c8\5\u00e6t\2\u05c8"+
		"\u05c9\7\u0095\2\2\u05c9\u05f1\3\2\2\2\u05ca\u05cb\7n\2\2\u05cb\u05cd"+
		"\7\u0094\2\2\u05cc\u05ce\7\b\2\2\u05cd\u05cc\3\2\2\2\u05cd\u05ce\3\2\2"+
		"\2\u05ce\u05cf\3\2\2\2\u05cf\u05d0\5\u00e6t\2\u05d0\u05d1\7\u0095\2\2"+
		"\u05d1\u05f1\3\2\2\2\u05d2\u05d3\7o\2\2\u05d3\u05d5\7\u0094\2\2\u05d4"+
		"\u05d6\7\b\2\2\u05d5\u05d4\3\2\2\2\u05d5\u05d6\3\2\2\2\u05d6\u05d7\3\2"+
		"\2\2\u05d7\u05d8\5\u00e6t\2\u05d8\u05d9\7\u0095\2\2\u05d9\u05f1\3\2\2"+
		"\2\u05da\u05db\7p\2\2\u05db\u05dd\7\u0094\2\2\u05dc\u05de\7\b\2\2\u05dd"+
		"\u05dc\3\2\2\2\u05dd\u05de\3\2\2\2\u05de\u05df\3\2\2\2\u05df\u05e0\5\u00e6"+
		"t\2\u05e0\u05e1\7\u0095\2\2\u05e1\u05f1\3\2\2\2\u05e2\u05e3\7q\2\2\u05e3"+
		"\u05e5\7\u0094\2\2\u05e4\u05e6\7\b\2\2\u05e5\u05e4\3\2\2\2\u05e5\u05e6"+
		"\3\2\2\2\u05e6\u05e7\3\2\2\2\u05e7\u05ec\5\u00e6t\2\u05e8\u05e9\7\u009a"+
		"\2\2\u05e9\u05ea\7u\2\2\u05ea\u05eb\7\u00a3\2\2\u05eb\u05ed\5\u010a\u0086"+
		"\2\u05ec\u05e8\3\2\2\2\u05ec\u05ed\3\2\2\2\u05ed\u05ee\3\2\2\2\u05ee\u05ef"+
		"\7\u0095\2\2\u05ef\u05f1\3\2\2\2\u05f0\u05b0\3\2\2\2\u05f0\u05ba\3\2\2"+
		"\2\u05f0\u05c2\3\2\2\2\u05f0\u05ca\3\2\2\2\u05f0\u05d2\3\2\2\2\u05f0\u05da"+
		"\3\2\2\2\u05f0\u05e2\3\2\2\2\u05f1\u00fb\3\2\2\2\u05f2\u05f4\5\u010c\u0087"+
		"\2\u05f3\u05f5\5\u0090I\2\u05f4\u05f3\3\2\2\2\u05f4\u05f5\3\2\2\2\u05f5"+
		"\u00fd\3\2\2\2\u05f6\u05fa\5\u010a\u0086\2\u05f7\u05fb\7~\2\2\u05f8\u05f9"+
		"\7\u008d\2\2\u05f9\u05fb\5\u010c\u0087\2\u05fa\u05f7\3\2\2\2\u05fa\u05f8"+
		"\3\2\2\2\u05fa\u05fb\3\2\2\2\u05fb\u00ff\3\2\2\2\u05fc\u0600\5\u0102\u0082"+
		"\2\u05fd\u0600\5\u0104\u0083\2\u05fe\u0600\5\u0106\u0084\2\u05ff\u05fc"+
		"\3\2\2\2\u05ff\u05fd\3\2\2\2\u05ff\u05fe\3\2\2\2\u0600\u0101\3\2\2\2\u0601"+
		"\u0602\t\n\2\2\u0602\u0103\3\2\2\2\u0603\u0604\t\13\2\2\u0604\u0105\3"+
		"\2\2\2\u0605\u0606\t\f\2\2\u0606\u0107\3\2\2\2\u0607\u0608\t\r\2\2\u0608"+
		"\u0109\3\2\2\2\u0609\u060a\t\16\2\2\u060a\u010b\3\2\2\2\u060b\u060e\7"+
		"x\2\2\u060c\u060e\5\u010e\u0088\2\u060d\u060b\3\2\2\2\u060d\u060c\3\2"+
		"\2\2\u060e\u010d\3\2\2\2\u060f\u0610\t\17\2\2\u0610\u010f\3\2\2\2\u0611"+
		"\u0614\7{\2\2\u0612\u0614\5\u0112\u008a\2\u0613\u0611\3\2\2\2\u0613\u0612"+
		"\3\2\2\2\u0614\u0111\3\2\2\2\u0615\u0616\7\u0098\2\2\u0616\u0617\7\u0099"+
		"\2\2\u0617\u0113\3\2\2\2\u009a\u0119\u011f\u0122\u0127\u012c\u012e\u0138"+
		"\u013e\u014b\u0158\u015d\u0160\u0169\u0170\u0179\u017f\u0183\u0189\u018c"+
		"\u0191\u0195\u019d\u01a5\u01aa\u01af\u01b2\u01b5\u01b8\u01bf\u01c7\u01cc"+
		"\u01d2\u01db\u01e4\u01e8\u01ec\u01ee\u01f8\u0202\u0207\u0209\u0216\u021a"+
		"\u021f\u0223\u0229\u022f\u0235\u023d\u0245\u0259\u025d\u0260\u0265\u0273"+
		"\u0279\u027c\u0285\u0290\u0295\u029a\u029d\u02a3\u02aa\u02ae\u02b4\u02b9"+
		"\u02be\u02c3\u02c6\u02cb\u02cf\u02da\u02e5\u02f6\u02fd\u0306\u030e\u0317"+
		"\u0321\u032b\u0337\u033e\u0342\u034b\u0350\u0357\u035b\u0364\u0367\u036f"+
		"\u0373\u0378\u037f\u038a\u038d\u0391\u0396\u039a\u039f\u03ac\u03b1\u03c2"+
		"\u03ca\u03cf\u03d2\u03e0\u03e9\u03ec\u03ef\u03f2\u03f6\u03fc\u0404\u040e"+
		"\u0416\u041c\u0420\u0424\u0428\u0432\u043d\u044c\u0451\u045a\u045c\u0461"+
		"\u0465\u0468\u0476\u04a1\u04be\u0547\u0584\u058d\u0598\u05a5\u05b3\u05b7"+
		"\u05bd\u05c5\u05cd\u05d5\u05dd\u05e5\u05ec\u05f0\u05f4\u05fa\u05ff\u060d"+
		"\u0613";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}