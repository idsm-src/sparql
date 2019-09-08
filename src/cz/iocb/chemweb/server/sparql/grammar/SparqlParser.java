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
		RULE_constructTemplate = 71, RULE_constructTriples = 72, RULE_triplesSameSubject = 73, 
		RULE_propertyList = 74, RULE_propertyListNotEmpty = 75, RULE_verb = 76, 
		RULE_objectList = 77, RULE_object = 78, RULE_triplesSameSubjectPath = 79, 
		RULE_propertyListPath = 80, RULE_propertyListPathNotEmpty = 81, RULE_propertyListPathNotEmptyList = 82, 
		RULE_verbPath = 83, RULE_verbSimple = 84, RULE_objectListPath = 85, RULE_objectPath = 86, 
		RULE_path = 87, RULE_pathAlternative = 88, RULE_pathSequence = 89, RULE_pathElt = 90, 
		RULE_pathEltOrInverse = 91, RULE_pathMod = 92, RULE_pathPrimary = 93, 
		RULE_pathNegatedPropertySet = 94, RULE_pathOneInPropertySet = 95, RULE_integer = 96, 
		RULE_triplesNode = 97, RULE_blankNodePropertyList = 98, RULE_triplesNodePath = 99, 
		RULE_blankNodePropertyListPath = 100, RULE_collection = 101, RULE_collectionPath = 102, 
		RULE_graphNode = 103, RULE_graphNodePath = 104, RULE_varOrTerm = 105, 
		RULE_varOrIRI = 106, RULE_var = 107, RULE_graphTerm = 108, RULE_nil = 109, 
		RULE_expression = 110, RULE_unaryLiteralExpression = 111, RULE_unaryExpression = 112, 
		RULE_primaryExpression = 113, RULE_builtInCall = 114, RULE_regexExpression = 115, 
		RULE_subStringExpression = 116, RULE_strReplaceExpression = 117, RULE_existsFunction = 118, 
		RULE_notExistsFunction = 119, RULE_aggregate = 120, RULE_iriRefOrFunction = 121, 
		RULE_rdfLiteral = 122, RULE_numericLiteral = 123, RULE_numericLiteralUnsigned = 124, 
		RULE_numericLiteralPositive = 125, RULE_numericLiteralNegative = 126, 
		RULE_booleanLiteral = 127, RULE_string = 128, RULE_iri = 129, RULE_prefixedName = 130, 
		RULE_blankNode = 131, RULE_anon = 132;
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
		"expressionList", "constructTemplate", "constructTriples", "triplesSameSubject", 
		"propertyList", "propertyListNotEmpty", "verb", "objectList", "object", 
		"triplesSameSubjectPath", "propertyListPath", "propertyListPathNotEmpty", 
		"propertyListPathNotEmptyList", "verbPath", "verbSimple", "objectListPath", 
		"objectPath", "path", "pathAlternative", "pathSequence", "pathElt", "pathEltOrInverse", 
		"pathMod", "pathPrimary", "pathNegatedPropertySet", "pathOneInPropertySet", 
		"integer", "triplesNode", "blankNodePropertyList", "triplesNodePath", 
		"blankNodePropertyListPath", "collection", "collectionPath", "graphNode", 
		"graphNodePath", "varOrTerm", "varOrIRI", "var", "graphTerm", "nil", "expression", 
		"unaryLiteralExpression", "unaryExpression", "primaryExpression", "builtInCall", 
		"regexExpression", "subStringExpression", "strReplaceExpression", "existsFunction", 
		"notExistsFunction", "aggregate", "iriRefOrFunction", "rdfLiteral", "numericLiteral", 
		"numericLiteralUnsigned", "numericLiteralPositive", "numericLiteralNegative", 
		"booleanLiteral", "string", "iri", "prefixedName", "blankNode", "anon"
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
			setState(280);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(266);
				prologue();
				setState(271);
				switch (_input.LA(1)) {
				case SELECT:
					{
					setState(267);
					selectQuery();
					}
					break;
				case CONSTRUCT:
					{
					setState(268);
					constructQuery();
					}
					break;
				case DESCRIBE:
					{
					setState(269);
					describeQuery();
					}
					break;
				case ASK:
					{
					setState(270);
					askQuery();
					}
					break;
				case EOF:
				case VALUES:
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(273);
				valuesClause();
				setState(274);
				match(EOF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(277);
				switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
				case 1:
					{
					setState(276);
					updateCommand();
					}
					break;
				}
				setState(279);
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
			setState(286);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BASE || _la==PREFIX) {
				{
				setState(284);
				switch (_input.LA(1)) {
				case BASE:
					{
					setState(282);
					baseDecl();
					}
					break;
				case PREFIX:
					{
					setState(283);
					prefixDecl();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(288);
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
			setState(289);
			match(BASE);
			setState(290);
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
			setState(292);
			match(PREFIX);
			setState(293);
			match(PNAME_NS);
			setState(294);
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
			setState(296);
			selectClause();
			setState(300);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(297);
				datasetClause();
				}
				}
				setState(302);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(303);
			whereClause();
			setState(304);
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
			setState(306);
			selectClause();
			setState(307);
			whereClause();
			setState(308);
			solutionModifier();
			setState(309);
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
			setState(311);
			match(SELECT);
			setState(313);
			_la = _input.LA(1);
			if (_la==DISTINCT || _la==REDUCED) {
				{
				setState(312);
				_la = _input.LA(1);
				if ( !(_la==DISTINCT || _la==REDUCED) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(321);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
			case OPEN_BRACE:
				{
				setState(316); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(315);
					selectVariable();
					}
					}
					setState(318); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 119)) & ~0x3f) == 0 && ((1L << (_la - 119)) & ((1L << (VAR1 - 119)) | (1L << (VAR2 - 119)) | (1L << (OPEN_BRACE - 119)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(320);
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
			setState(330);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(323);
				var();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(324);
				match(OPEN_BRACE);
				setState(325);
				expression(0);
				setState(326);
				match(AS);
				setState(327);
				var();
				setState(328);
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
			setState(332);
			match(CONSTRUCT);
			setState(356);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				{
				setState(333);
				constructTemplate();
				setState(337);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(334);
					datasetClause();
					}
					}
					setState(339);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(340);
				whereClause();
				setState(341);
				solutionModifier();
				}
				break;
			case FROM:
			case WHERE:
				{
				setState(346);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FROM) {
					{
					{
					setState(343);
					datasetClause();
					}
					}
					setState(348);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(349);
				match(WHERE);
				setState(350);
				match(OPEN_CURLY_BRACE);
				setState(352);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
					{
					setState(351);
					triplesTemplate();
					}
				}

				setState(354);
				match(CLOSE_CURLY_BRACE);
				setState(355);
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
			setState(358);
			match(DESCRIBE);
			setState(365);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				{
				setState(360); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(359);
					varOrIRI();
					}
					}
					setState(362); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0) );
				}
				break;
			case ASTERISK:
				{
				setState(364);
				match(ASTERISK);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(370);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(367);
				datasetClause();
				}
				}
				setState(372);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(374);
			_la = _input.LA(1);
			if (_la==WHERE || _la==OPEN_CURLY_BRACE) {
				{
				setState(373);
				whereClause();
				}
			}

			setState(376);
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
			setState(378);
			match(ASK);
			setState(382);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FROM) {
				{
				{
				setState(379);
				datasetClause();
				}
				}
				setState(384);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(385);
			whereClause();
			setState(387);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(386);
				groupClause();
				}
			}

			setState(390);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(389);
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
			setState(392);
			match(FROM);
			setState(394);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(393);
				match(NAMED);
				}
			}

			setState(396);
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
			setState(399);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(398);
				match(WHERE);
				}
			}

			setState(401);
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
			setState(404);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(403);
				groupClause();
				}
			}

			setState(407);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(406);
				havingClause();
				}
			}

			setState(410);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(409);
				orderClause();
				}
			}

			setState(413);
			_la = _input.LA(1);
			if (_la==LIMIT || _la==OFFSET) {
				{
				setState(412);
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
			setState(415);
			match(GROUP);
			setState(416);
			match(BY);
			setState(418); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(417);
				groupCondition();
				}
				}
				setState(420); 
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
			setState(433);
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
				setState(422);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(423);
				functionCall();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(424);
				match(OPEN_BRACE);
				setState(425);
				expression(0);
				setState(428);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(426);
					match(AS);
					setState(427);
					var();
					}
				}

				setState(430);
				match(CLOSE_BRACE);
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 4);
				{
				setState(432);
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
			setState(435);
			match(HAVING);
			setState(437); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(436);
				havingCondition();
				}
				}
				setState(439); 
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
			setState(441);
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
			setState(443);
			match(ORDER);
			setState(444);
			match(BY);
			setState(446); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(445);
				orderCondition();
				}
				}
				setState(448); 
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
			setState(457);
			switch (_input.LA(1)) {
			case ASC:
			case DESC:
				enterOuterAlt(_localctx, 1);
				{
				setState(450);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(451);
				match(OPEN_BRACE);
				setState(452);
				expression(0);
				setState(453);
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
				setState(455);
				constraint();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 3);
				{
				setState(456);
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
			setState(467);
			switch (_input.LA(1)) {
			case LIMIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(459);
				limitClause();
				setState(461);
				_la = _input.LA(1);
				if (_la==OFFSET) {
					{
					setState(460);
					offsetClause();
					}
				}

				}
				break;
			case OFFSET:
				enterOuterAlt(_localctx, 2);
				{
				setState(463);
				offsetClause();
				setState(465);
				_la = _input.LA(1);
				if (_la==LIMIT) {
					{
					setState(464);
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
			setState(469);
			match(LIMIT);
			setState(470);
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
			setState(472);
			match(OFFSET);
			setState(473);
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
			setState(477);
			_la = _input.LA(1);
			if (_la==VALUES) {
				{
				setState(475);
				match(VALUES);
				setState(476);
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
			setState(479);
			prologue();
			setState(494);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LOAD) | (1L << CLEAR) | (1L << DROP) | (1L << ADD) | (1L << MOVE) | (1L << COPY) | (1L << CREATE) | (1L << DELETE) | (1L << INSERT) | (1L << WITH))) != 0)) {
				{
				setState(480);
				update();
				setState(487);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(481);
						match(SEMICOLON);
						setState(482);
						prologue();
						setState(483);
						update();
						}
						} 
					}
					setState(489);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
				}
				setState(492);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(490);
					match(SEMICOLON);
					setState(491);
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
			setState(507);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(496);
				load();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(497);
				clear();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(498);
				drop();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(499);
				add();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(500);
				move();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(501);
				copy();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(502);
				create();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(503);
				insertData();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(504);
				deleteData();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(505);
				deleteWhere();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(506);
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
			setState(509);
			match(LOAD);
			setState(511);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(510);
				match(SILENT);
				}
			}

			setState(513);
			iri();
			setState(516);
			_la = _input.LA(1);
			if (_la==INTO) {
				{
				setState(514);
				match(INTO);
				setState(515);
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
			setState(518);
			match(CLEAR);
			setState(520);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(519);
				match(SILENT);
				}
			}

			setState(522);
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
			setState(524);
			match(DROP);
			setState(526);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(525);
				match(SILENT);
				}
			}

			setState(528);
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
			setState(530);
			match(CREATE);
			setState(532);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(531);
				match(SILENT);
				}
			}

			setState(534);
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
			setState(536);
			match(ADD);
			setState(538);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(537);
				match(SILENT);
				}
			}

			setState(540);
			graphOrDefault();
			setState(541);
			match(TO);
			setState(542);
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
			setState(544);
			match(MOVE);
			setState(546);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(545);
				match(SILENT);
				}
			}

			setState(548);
			graphOrDefault();
			setState(549);
			match(TO);
			setState(550);
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
			setState(552);
			match(COPY);
			setState(554);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(553);
				match(SILENT);
				}
			}

			setState(556);
			graphOrDefault();
			setState(557);
			match(TO);
			setState(558);
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
			setState(560);
			match(INSERT);
			setState(561);
			match(DATA);
			setState(562);
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
			setState(564);
			match(DELETE);
			setState(565);
			match(DATA);
			setState(566);
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
			setState(568);
			match(DELETE);
			setState(569);
			match(WHERE);
			setState(570);
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
			setState(574);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(572);
				match(WITH);
				setState(573);
				iri();
				}
			}

			setState(581);
			switch (_input.LA(1)) {
			case DELETE:
				{
				setState(576);
				deleteClause();
				setState(578);
				_la = _input.LA(1);
				if (_la==INSERT) {
					{
					setState(577);
					insertClause();
					}
				}

				}
				break;
			case INSERT:
				{
				setState(580);
				insertClause();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(586);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==USING) {
				{
				{
				setState(583);
				usingClause();
				}
				}
				setState(588);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(589);
			match(WHERE);
			setState(590);
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
			setState(592);
			match(DELETE);
			setState(593);
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
			setState(595);
			match(INSERT);
			setState(596);
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
			setState(598);
			match(USING);
			setState(600);
			_la = _input.LA(1);
			if (_la==NAMED) {
				{
				setState(599);
				match(NAMED);
				}
			}

			setState(602);
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
			setState(609);
			switch (_input.LA(1)) {
			case DEFAULT:
				enterOuterAlt(_localctx, 1);
				{
				setState(604);
				match(DEFAULT);
				}
				break;
			case GRAPH:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(606);
				_la = _input.LA(1);
				if (_la==GRAPH) {
					{
					setState(605);
					match(GRAPH);
					}
				}

				setState(608);
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
			setState(611);
			match(GRAPH);
			setState(612);
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
			setState(618);
			switch (_input.LA(1)) {
			case GRAPH:
				enterOuterAlt(_localctx, 1);
				{
				setState(614);
				graphRef();
				}
				break;
			case DEFAULT:
				enterOuterAlt(_localctx, 2);
				{
				setState(615);
				match(DEFAULT);
				}
				break;
			case NAMED:
				enterOuterAlt(_localctx, 3);
				{
				setState(616);
				match(NAMED);
				}
				break;
			case ALL:
				enterOuterAlt(_localctx, 4);
				{
				setState(617);
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
			setState(620);
			match(OPEN_CURLY_BRACE);
			setState(621);
			quads();
			setState(622);
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
			setState(624);
			match(OPEN_CURLY_BRACE);
			setState(625);
			quads();
			setState(626);
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
			setState(629);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(628);
				triplesTemplate();
				}
			}

			setState(634);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==GRAPH) {
				{
				{
				setState(631);
				quadsDetails();
				}
				}
				setState(636);
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
			setState(637);
			quadsNotTriples();
			setState(639);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(638);
				match(DOT);
				}
			}

			setState(642);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(641);
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
			setState(644);
			match(GRAPH);
			setState(645);
			varOrIRI();
			setState(646);
			match(OPEN_CURLY_BRACE);
			setState(648);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(647);
				triplesTemplate();
				}
			}

			setState(650);
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
			setState(652);
			triplesSameSubject();
			setState(659);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(653);
				match(DOT);
				setState(655);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
					{
					setState(654);
					triplesSameSubject();
					}
				}

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
			setState(662);
			match(OPEN_CURLY_BRACE);
			setState(665);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(663);
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
				setState(664);
				groupGraphPatternSub();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(667);
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
			setState(670);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(669);
				triplesBlock();
				}
			}

			setState(675);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VALUES) | (1L << OPTIONAL) | (1L << GRAPH) | (1L << FILTER) | (1L << SERVICE) | (1L << BIND) | (1L << MINUS))) != 0) || _la==OPEN_CURLY_BRACE) {
				{
				{
				setState(672);
				groupGraphPatternSubList();
				}
				}
				setState(677);
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
			setState(678);
			graphPatternNotTriples();
			setState(680);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(679);
				match(DOT);
				}
			}

			setState(683);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(682);
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
			setState(685);
			triplesSameSubjectPath();
			setState(692);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(686);
				match(DOT);
				setState(688);
				_la = _input.LA(1);
				if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
					{
					setState(687);
					triplesSameSubjectPath();
					}
				}

				}
				}
				setState(694);
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
			setState(703);
			switch (_input.LA(1)) {
			case OPEN_CURLY_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(695);
				groupOrUnionGraphPattern();
				}
				break;
			case OPTIONAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(696);
				optionalGraphPattern();
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(697);
				minusGraphPattern();
				}
				break;
			case GRAPH:
				enterOuterAlt(_localctx, 4);
				{
				setState(698);
				graphGraphPattern();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 5);
				{
				setState(699);
				serviceGraphPattern();
				}
				break;
			case FILTER:
				enterOuterAlt(_localctx, 6);
				{
				setState(700);
				filter();
				}
				break;
			case BIND:
				enterOuterAlt(_localctx, 7);
				{
				setState(701);
				bind();
				}
				break;
			case VALUES:
				enterOuterAlt(_localctx, 8);
				{
				setState(702);
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
			setState(705);
			match(OPTIONAL);
			setState(706);
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
			setState(708);
			match(GRAPH);
			setState(709);
			varOrIRI();
			setState(710);
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
			setState(712);
			match(SERVICE);
			setState(714);
			_la = _input.LA(1);
			if (_la==SILENT) {
				{
				setState(713);
				match(SILENT);
				}
			}

			setState(716);
			varOrIRI();
			setState(717);
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
			setState(719);
			match(BIND);
			setState(720);
			match(OPEN_BRACE);
			setState(721);
			expression(0);
			setState(722);
			match(AS);
			setState(723);
			var();
			setState(724);
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
			setState(726);
			match(VALUES);
			setState(727);
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
			setState(731);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(729);
				inlineDataOneVar();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(730);
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
			setState(733);
			var();
			setState(734);
			match(OPEN_CURLY_BRACE);
			setState(738);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)))) != 0)) {
				{
				{
				setState(735);
				dataBlockValue();
				}
				}
				setState(740);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(741);
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
			setState(743);
			match(OPEN_BRACE);
			setState(747);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==VAR1 || _la==VAR2) {
				{
				{
				setState(744);
				var();
				}
				}
				setState(749);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(750);
			match(CLOSE_BRACE);
			setState(751);
			match(OPEN_CURLY_BRACE);
			setState(755);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OPEN_BRACE) {
				{
				{
				setState(752);
				dataBlockValues();
				}
				}
				setState(757);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(758);
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
			setState(760);
			match(OPEN_BRACE);
			setState(764);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TRUE) | (1L << FALSE) | (1L << UNDEF))) != 0) || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)))) != 0)) {
				{
				{
				setState(761);
				dataBlockValue();
				}
				}
				setState(766);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(767);
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
			setState(774);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(769);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(770);
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
				setState(771);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(772);
				booleanLiteral();
				}
				break;
			case UNDEF:
				enterOuterAlt(_localctx, 5);
				{
				setState(773);
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
			setState(776);
			match(MINUS);
			setState(777);
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
			setState(779);
			groupGraphPattern();
			setState(784);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION) {
				{
				{
				setState(780);
				match(UNION);
				setState(781);
				groupGraphPattern();
				}
				}
				setState(786);
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
			setState(787);
			match(FILTER);
			setState(788);
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
			setState(796);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(790);
				match(OPEN_BRACE);
				setState(791);
				expression(0);
				setState(792);
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
				setState(794);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(795);
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
			setState(798);
			iri();
			setState(799);
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
			setState(801);
			match(OPEN_BRACE);
			setState(807);
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
				setState(803);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(802);
					match(DISTINCT);
					}
				}

				setState(805);
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
			setState(809);
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
			setState(811);
			expression(0);
			setState(816);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(812);
				match(COMMA);
				setState(813);
				expression(0);
				}
				}
				setState(818);
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
			setState(819);
			match(OPEN_CURLY_BRACE);
			setState(821);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
				{
				setState(820);
				constructTriples();
				}
			}

			setState(823);
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
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitConstructTriples(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructTriplesContext constructTriples() throws RecognitionException {
		ConstructTriplesContext _localctx = new ConstructTriplesContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_constructTriples);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(825);
			triplesSameSubject();
			setState(832);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(826);
					match(DOT);
					setState(828);
					_la = _input.LA(1);
					if (_la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0)) {
						{
						setState(827);
						constructTriples();
						}
					}

					}
					} 
				}
				setState(834);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
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
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparqlParserVisitor ) return ((SparqlParserVisitor<? extends T>)visitor).visitTriplesSameSubject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TriplesSameSubjectContext triplesSameSubject() throws RecognitionException {
		TriplesSameSubjectContext _localctx = new TriplesSameSubjectContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_triplesSameSubject);
		try {
			setState(841);
			switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(835);
				varOrTerm();
				setState(836);
				propertyListNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(838);
				triplesNode();
				setState(839);
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
		enterRule(_localctx, 148, RULE_propertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(844);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0)) {
				{
				setState(843);
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
		enterRule(_localctx, 150, RULE_propertyListNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(846);
			verb();
			setState(847);
			objectList();
			setState(856);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(848);
				match(SEMICOLON);
				setState(852);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)))) != 0)) {
					{
					setState(849);
					verb();
					setState(850);
					objectList();
					}
				}

				}
				}
				setState(858);
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
		enterRule(_localctx, 152, RULE_verb);
		try {
			setState(861);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(859);
				varOrIRI();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(860);
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
		enterRule(_localctx, 154, RULE_objectList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(863);
			object();
			setState(868);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(864);
				match(COMMA);
				setState(865);
				object();
				}
				}
				setState(870);
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
		enterRule(_localctx, 156, RULE_object);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
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
		enterRule(_localctx, 158, RULE_triplesSameSubjectPath);
		try {
			setState(879);
			switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(873);
				varOrTerm();
				setState(874);
				propertyListPathNotEmpty();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(876);
				triplesNodePath();
				setState(877);
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
		enterRule(_localctx, 160, RULE_propertyListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(882);
			_la = _input.LA(1);
			if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INVERSE - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (NEGATION - 115)))) != 0)) {
				{
				setState(881);
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
		enterRule(_localctx, 162, RULE_propertyListPathNotEmpty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(886);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(884);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(885);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(888);
			objectListPath();
			setState(895);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(889);
				match(SEMICOLON);
				setState(891);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INVERSE - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (NEGATION - 115)))) != 0)) {
					{
					setState(890);
					propertyListPathNotEmptyList();
					}
				}

				}
				}
				setState(897);
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
		enterRule(_localctx, 164, RULE_propertyListPathNotEmptyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(900);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
			case OPEN_BRACE:
			case NEGATION:
				{
				setState(898);
				verbPath();
				}
				break;
			case VAR1:
			case VAR2:
				{
				setState(899);
				verbSimple();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(902);
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
		enterRule(_localctx, 166, RULE_verbPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(904);
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
		enterRule(_localctx, 168, RULE_verbSimple);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(906);
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
		enterRule(_localctx, 170, RULE_objectListPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(908);
			objectPath();
			setState(913);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(909);
				match(COMMA);
				setState(910);
				objectPath();
				}
				}
				setState(915);
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
		enterRule(_localctx, 172, RULE_objectPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(916);
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
		enterRule(_localctx, 174, RULE_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(918);
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
		enterRule(_localctx, 176, RULE_pathAlternative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(920);
			pathSequence();
			setState(925);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(921);
				match(PIPE);
				setState(922);
				pathSequence();
				}
				}
				setState(927);
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
		enterRule(_localctx, 178, RULE_pathSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(928);
			pathEltOrInverse();
			setState(933);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIVIDE) {
				{
				{
				setState(929);
				match(DIVIDE);
				setState(930);
				pathEltOrInverse();
				}
				}
				setState(935);
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
		enterRule(_localctx, 180, RULE_pathElt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(936);
			pathPrimary();
			setState(938);
			_la = _input.LA(1);
			if (((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (ASTERISK - 151)) | (1L << (QUESTION_MARK - 151)))) != 0)) {
				{
				setState(937);
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
		enterRule(_localctx, 182, RULE_pathEltOrInverse);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(941);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(940);
				match(INVERSE);
				}
			}

			setState(943);
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
		enterRule(_localctx, 184, RULE_pathMod);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(945);
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
		enterRule(_localctx, 186, RULE_pathPrimary);
		try {
			setState(955);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(947);
				iri();
				}
				break;
			case A:
				enterOuterAlt(_localctx, 2);
				{
				setState(948);
				match(A);
				}
				break;
			case NEGATION:
				enterOuterAlt(_localctx, 3);
				{
				setState(949);
				match(NEGATION);
				setState(950);
				pathNegatedPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 4);
				{
				setState(951);
				match(OPEN_BRACE);
				setState(952);
				path();
				setState(953);
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
		enterRule(_localctx, 188, RULE_pathNegatedPropertySet);
		int _la;
		try {
			setState(970);
			switch (_input.LA(1)) {
			case A:
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
			case INVERSE:
				enterOuterAlt(_localctx, 1);
				{
				setState(957);
				pathOneInPropertySet();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(958);
				match(OPEN_BRACE);
				setState(967);
				_la = _input.LA(1);
				if (_la==A || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (INVERSE - 115)))) != 0)) {
					{
					setState(959);
					pathOneInPropertySet();
					setState(964);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==PIPE) {
						{
						{
						setState(960);
						match(PIPE);
						setState(961);
						pathOneInPropertySet();
						}
						}
						setState(966);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(969);
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
		enterRule(_localctx, 190, RULE_pathOneInPropertySet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(973);
			_la = _input.LA(1);
			if (_la==INVERSE) {
				{
				setState(972);
				match(INVERSE);
				}
			}

			setState(977);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				{
				setState(975);
				iri();
				}
				break;
			case A:
				{
				setState(976);
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
		enterRule(_localctx, 192, RULE_integer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(979);
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
		enterRule(_localctx, 194, RULE_triplesNode);
		try {
			setState(983);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(981);
				collection();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(982);
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
		enterRule(_localctx, 196, RULE_blankNodePropertyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(985);
			match(OPEN_SQUARE_BRACKET);
			setState(986);
			propertyListNotEmpty();
			setState(987);
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
		enterRule(_localctx, 198, RULE_triplesNodePath);
		try {
			setState(991);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(989);
				collectionPath();
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(990);
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
		enterRule(_localctx, 200, RULE_blankNodePropertyListPath);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(993);
			match(OPEN_SQUARE_BRACKET);
			setState(994);
			propertyListPathNotEmpty();
			setState(995);
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
		enterRule(_localctx, 202, RULE_collection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(997);
			match(OPEN_BRACE);
			setState(999); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(998);
				graphNode();
				}
				}
				setState(1001); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0) );
			setState(1003);
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
		enterRule(_localctx, 204, RULE_collectionPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1005);
			match(OPEN_BRACE);
			setState(1007); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1006);
				graphNodePath();
				}
				}
				setState(1009); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==TRUE || _la==FALSE || ((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (IRIREF - 115)) | (1L << (PNAME_NS - 115)) | (1L << (PNAME_LN - 115)) | (1L << (BLANK_NODE_LABEL - 115)) | (1L << (VAR1 - 115)) | (1L << (VAR2 - 115)) | (1L << (INTEGER - 115)) | (1L << (DECIMAL - 115)) | (1L << (DOUBLE - 115)) | (1L << (INTEGER_POSITIVE - 115)) | (1L << (DECIMAL_POSITIVE - 115)) | (1L << (DOUBLE_POSITIVE - 115)) | (1L << (INTEGER_NEGATIVE - 115)) | (1L << (DECIMAL_NEGATIVE - 115)) | (1L << (DOUBLE_NEGATIVE - 115)) | (1L << (STRING_LITERAL1 - 115)) | (1L << (STRING_LITERAL2 - 115)) | (1L << (STRING_LITERAL_LONG1 - 115)) | (1L << (STRING_LITERAL_LONG2 - 115)) | (1L << (OPEN_BRACE - 115)) | (1L << (OPEN_SQUARE_BRACKET - 115)))) != 0) );
			setState(1011);
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
		enterRule(_localctx, 206, RULE_graphNode);
		try {
			setState(1015);
			switch ( getInterpreter().adaptivePredict(_input,114,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1013);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1014);
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
		enterRule(_localctx, 208, RULE_graphNodePath);
		try {
			setState(1019);
			switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1017);
				varOrTerm();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1018);
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
		enterRule(_localctx, 210, RULE_varOrTerm);
		try {
			setState(1023);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1021);
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
				setState(1022);
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
		enterRule(_localctx, 212, RULE_varOrIRI);
		try {
			setState(1027);
			switch (_input.LA(1)) {
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1025);
				var();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1026);
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
		enterRule(_localctx, 214, RULE_var);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1029);
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
		enterRule(_localctx, 216, RULE_graphTerm);
		try {
			setState(1037);
			switch (_input.LA(1)) {
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1031);
				iri();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1032);
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
				setState(1033);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(1034);
				booleanLiteral();
				}
				break;
			case BLANK_NODE_LABEL:
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 5);
				{
				setState(1035);
				blankNode();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1036);
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
		enterRule(_localctx, 218, RULE_nil);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1039);
			match(OPEN_BRACE);
			setState(1040);
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
		int _startState = 220;
		enterRecursionRule(_localctx, 220, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1048);
			switch (_input.LA(1)) {
			case PLUS_SIGN:
			case MINUS_SIGN:
				{
				_localctx = new UnaryAdditiveExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1043);
				((UnaryAdditiveExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
					((UnaryAdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1044);
				expression(9);
				}
				break;
			case NEGATION:
				{
				_localctx = new UnaryNegationExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1045);
				match(NEGATION);
				setState(1046);
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
				setState(1047);
				primaryExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(1079);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,123,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1077);
					switch ( getInterpreter().adaptivePredict(_input,122,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicativeExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1050);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1051);
						((MultiplicativeExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==ASTERISK || _la==DIVIDE) ) {
							((MultiplicativeExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1052);
						expression(8);
						}
						break;
					case 2:
						{
						_localctx = new AdditiveExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1053);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1054);
						((AdditiveExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS_SIGN || _la==MINUS_SIGN) ) {
							((AdditiveExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1055);
						expression(7);
						}
						break;
					case 3:
						{
						_localctx = new RelationalExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1056);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1057);
						((RelationalExpressionContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (LESS_EQUAL - 137)) | (1L << (GREATER_EQUAL - 137)) | (1L << (NOT_EQUAL - 137)) | (1L << (EQUAL - 137)) | (1L << (LESS - 137)) | (1L << (GREATER - 137)))) != 0)) ) {
							((RelationalExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(1058);
						expression(4);
						}
						break;
					case 4:
						{
						_localctx = new UnarySignedLiteralExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1059);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1060);
						unaryLiteralExpression();
						}
						break;
					case 5:
						{
						_localctx = new RelationalSetExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1061);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1063);
						_la = _input.LA(1);
						if (_la==NOT) {
							{
							setState(1062);
							match(NOT);
							}
						}

						setState(1065);
						match(IN);
						setState(1066);
						match(OPEN_BRACE);
						setState(1068);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
							{
							setState(1067);
							expressionList();
							}
						}

						setState(1070);
						match(CLOSE_BRACE);
						}
						break;
					case 6:
						{
						_localctx = new ConditionalAndExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1071);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						{
						setState(1072);
						match(AND);
						setState(1073);
						expression(0);
						}
						}
						break;
					case 7:
						{
						_localctx = new ConditionalOrExpressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1074);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						{
						setState(1075);
						match(OR);
						setState(1076);
						expression(0);
						}
						}
						break;
					}
					} 
				}
				setState(1081);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,123,_ctx);
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
		enterRule(_localctx, 222, RULE_unaryLiteralExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1084);
			switch (_input.LA(1)) {
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				{
				setState(1082);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				{
				setState(1083);
				numericLiteralNegative();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1088);
			switch ( getInterpreter().adaptivePredict(_input,125,_ctx) ) {
			case 1:
				{
				setState(1086);
				((UnaryLiteralExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASTERISK || _la==DIVIDE) ) {
					((UnaryLiteralExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(1087);
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
		enterRule(_localctx, 224, RULE_unaryExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1091);
			_la = _input.LA(1);
			if (((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (MINUS_SIGN - 151)) | (1L << (NEGATION - 151)))) != 0)) {
				{
				setState(1090);
				((UnaryExpressionContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 151)) & ~0x3f) == 0 && ((1L << (_la - 151)) & ((1L << (PLUS_SIGN - 151)) | (1L << (MINUS_SIGN - 151)) | (1L << (NEGATION - 151)))) != 0)) ) {
					((UnaryExpressionContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(1093);
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
		enterRule(_localctx, 226, RULE_primaryExpression);
		try {
			setState(1105);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1095);
				match(OPEN_BRACE);
				setState(1096);
				expression(0);
				setState(1097);
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
				setState(1099);
				builtInCall();
				}
				break;
			case IRIREF:
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1100);
				iriRefOrFunction();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case STRING_LITERAL_LONG1:
			case STRING_LITERAL_LONG2:
				enterOuterAlt(_localctx, 4);
				{
				setState(1101);
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
				setState(1102);
				numericLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1103);
				booleanLiteral();
				}
				break;
			case VAR1:
			case VAR2:
				enterOuterAlt(_localctx, 7);
				{
				setState(1104);
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
		enterRule(_localctx, 228, RULE_builtInCall);
		int _la;
		try {
			setState(1375);
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
				setState(1107);
				aggregate();
				}
				break;
			case STR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1108);
				match(STR);
				setState(1109);
				match(OPEN_BRACE);
				setState(1110);
				expression(0);
				setState(1111);
				match(CLOSE_BRACE);
				}
				break;
			case LANG:
				enterOuterAlt(_localctx, 3);
				{
				setState(1113);
				match(LANG);
				setState(1114);
				match(OPEN_BRACE);
				setState(1115);
				expression(0);
				setState(1116);
				match(CLOSE_BRACE);
				}
				break;
			case LANGMATCHES:
				enterOuterAlt(_localctx, 4);
				{
				setState(1118);
				match(LANGMATCHES);
				setState(1119);
				match(OPEN_BRACE);
				setState(1120);
				expression(0);
				setState(1121);
				match(COMMA);
				setState(1122);
				expression(0);
				setState(1123);
				match(CLOSE_BRACE);
				}
				break;
			case DATATYPE:
				enterOuterAlt(_localctx, 5);
				{
				setState(1125);
				match(DATATYPE);
				setState(1126);
				match(OPEN_BRACE);
				setState(1127);
				expression(0);
				setState(1128);
				match(CLOSE_BRACE);
				}
				break;
			case BOUND:
				enterOuterAlt(_localctx, 6);
				{
				setState(1130);
				match(BOUND);
				setState(1131);
				match(OPEN_BRACE);
				setState(1132);
				var();
				setState(1133);
				match(CLOSE_BRACE);
				}
				break;
			case IRI:
				enterOuterAlt(_localctx, 7);
				{
				setState(1135);
				match(IRI);
				setState(1136);
				match(OPEN_BRACE);
				setState(1137);
				expression(0);
				setState(1138);
				match(CLOSE_BRACE);
				}
				break;
			case URI:
				enterOuterAlt(_localctx, 8);
				{
				setState(1140);
				match(URI);
				setState(1141);
				match(OPEN_BRACE);
				setState(1142);
				expression(0);
				setState(1143);
				match(CLOSE_BRACE);
				}
				break;
			case BNODE:
				enterOuterAlt(_localctx, 9);
				{
				setState(1145);
				match(BNODE);
				setState(1146);
				match(OPEN_BRACE);
				setState(1148);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1147);
					expression(0);
					}
				}

				setState(1150);
				match(CLOSE_BRACE);
				}
				break;
			case RAND:
				enterOuterAlt(_localctx, 10);
				{
				setState(1151);
				match(RAND);
				setState(1152);
				match(OPEN_BRACE);
				setState(1153);
				match(CLOSE_BRACE);
				}
				break;
			case ABS:
				enterOuterAlt(_localctx, 11);
				{
				setState(1154);
				match(ABS);
				setState(1155);
				match(OPEN_BRACE);
				setState(1156);
				expression(0);
				setState(1157);
				match(CLOSE_BRACE);
				}
				break;
			case CEIL:
				enterOuterAlt(_localctx, 12);
				{
				setState(1159);
				match(CEIL);
				setState(1160);
				match(OPEN_BRACE);
				setState(1161);
				expression(0);
				setState(1162);
				match(CLOSE_BRACE);
				}
				break;
			case FLOOR:
				enterOuterAlt(_localctx, 13);
				{
				setState(1164);
				match(FLOOR);
				setState(1165);
				match(OPEN_BRACE);
				setState(1166);
				expression(0);
				setState(1167);
				match(CLOSE_BRACE);
				}
				break;
			case ROUND:
				enterOuterAlt(_localctx, 14);
				{
				setState(1169);
				match(ROUND);
				setState(1170);
				match(OPEN_BRACE);
				setState(1171);
				expression(0);
				setState(1172);
				match(CLOSE_BRACE);
				}
				break;
			case CONCAT:
				enterOuterAlt(_localctx, 15);
				{
				setState(1174);
				match(CONCAT);
				setState(1175);
				match(OPEN_BRACE);
				setState(1177);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1176);
					expressionList();
					}
				}

				setState(1179);
				match(CLOSE_BRACE);
				}
				break;
			case SUBSTR:
				enterOuterAlt(_localctx, 16);
				{
				setState(1180);
				subStringExpression();
				}
				break;
			case STRLEN:
				enterOuterAlt(_localctx, 17);
				{
				setState(1181);
				match(STRLEN);
				setState(1182);
				match(OPEN_BRACE);
				setState(1183);
				expression(0);
				setState(1184);
				match(CLOSE_BRACE);
				}
				break;
			case REPLACE:
				enterOuterAlt(_localctx, 18);
				{
				setState(1186);
				strReplaceExpression();
				}
				break;
			case UCASE:
				enterOuterAlt(_localctx, 19);
				{
				setState(1187);
				match(UCASE);
				setState(1188);
				match(OPEN_BRACE);
				setState(1189);
				expression(0);
				setState(1190);
				match(CLOSE_BRACE);
				}
				break;
			case LCASE:
				enterOuterAlt(_localctx, 20);
				{
				setState(1192);
				match(LCASE);
				setState(1193);
				match(OPEN_BRACE);
				setState(1194);
				expression(0);
				setState(1195);
				match(CLOSE_BRACE);
				}
				break;
			case ENCODE_FOR_URI:
				enterOuterAlt(_localctx, 21);
				{
				setState(1197);
				match(ENCODE_FOR_URI);
				setState(1198);
				match(OPEN_BRACE);
				setState(1199);
				expression(0);
				setState(1200);
				match(CLOSE_BRACE);
				}
				break;
			case CONTAINS:
				enterOuterAlt(_localctx, 22);
				{
				setState(1202);
				match(CONTAINS);
				setState(1203);
				match(OPEN_BRACE);
				setState(1204);
				expression(0);
				setState(1205);
				match(COMMA);
				setState(1206);
				expression(0);
				setState(1207);
				match(CLOSE_BRACE);
				}
				break;
			case STRSTARTS:
				enterOuterAlt(_localctx, 23);
				{
				setState(1209);
				match(STRSTARTS);
				setState(1210);
				match(OPEN_BRACE);
				setState(1211);
				expression(0);
				setState(1212);
				match(COMMA);
				setState(1213);
				expression(0);
				setState(1214);
				match(CLOSE_BRACE);
				}
				break;
			case STRENDS:
				enterOuterAlt(_localctx, 24);
				{
				setState(1216);
				match(STRENDS);
				setState(1217);
				match(OPEN_BRACE);
				setState(1218);
				expression(0);
				setState(1219);
				match(COMMA);
				setState(1220);
				expression(0);
				setState(1221);
				match(CLOSE_BRACE);
				}
				break;
			case STRBEFORE:
				enterOuterAlt(_localctx, 25);
				{
				setState(1223);
				match(STRBEFORE);
				setState(1224);
				match(OPEN_BRACE);
				setState(1225);
				expression(0);
				setState(1226);
				match(COMMA);
				setState(1227);
				expression(0);
				setState(1228);
				match(CLOSE_BRACE);
				}
				break;
			case STRAFTER:
				enterOuterAlt(_localctx, 26);
				{
				setState(1230);
				match(STRAFTER);
				setState(1231);
				match(OPEN_BRACE);
				setState(1232);
				expression(0);
				setState(1233);
				match(COMMA);
				setState(1234);
				expression(0);
				setState(1235);
				match(CLOSE_BRACE);
				}
				break;
			case YEAR:
				enterOuterAlt(_localctx, 27);
				{
				setState(1237);
				match(YEAR);
				setState(1238);
				match(OPEN_BRACE);
				setState(1239);
				expression(0);
				setState(1240);
				match(CLOSE_BRACE);
				}
				break;
			case MONTH:
				enterOuterAlt(_localctx, 28);
				{
				setState(1242);
				match(MONTH);
				setState(1243);
				match(OPEN_BRACE);
				setState(1244);
				expression(0);
				setState(1245);
				match(CLOSE_BRACE);
				}
				break;
			case DAY:
				enterOuterAlt(_localctx, 29);
				{
				setState(1247);
				match(DAY);
				setState(1248);
				match(OPEN_BRACE);
				setState(1249);
				expression(0);
				setState(1250);
				match(CLOSE_BRACE);
				}
				break;
			case HOURS:
				enterOuterAlt(_localctx, 30);
				{
				setState(1252);
				match(HOURS);
				setState(1253);
				match(OPEN_BRACE);
				setState(1254);
				expression(0);
				setState(1255);
				match(CLOSE_BRACE);
				}
				break;
			case MINUTES:
				enterOuterAlt(_localctx, 31);
				{
				setState(1257);
				match(MINUTES);
				setState(1258);
				match(OPEN_BRACE);
				setState(1259);
				expression(0);
				setState(1260);
				match(CLOSE_BRACE);
				}
				break;
			case SECONDS:
				enterOuterAlt(_localctx, 32);
				{
				setState(1262);
				match(SECONDS);
				setState(1263);
				match(OPEN_BRACE);
				setState(1264);
				expression(0);
				setState(1265);
				match(CLOSE_BRACE);
				}
				break;
			case TIMEZONE:
				enterOuterAlt(_localctx, 33);
				{
				setState(1267);
				match(TIMEZONE);
				setState(1268);
				match(OPEN_BRACE);
				setState(1269);
				expression(0);
				setState(1270);
				match(CLOSE_BRACE);
				}
				break;
			case TZ:
				enterOuterAlt(_localctx, 34);
				{
				setState(1272);
				match(TZ);
				setState(1273);
				match(OPEN_BRACE);
				setState(1274);
				expression(0);
				setState(1275);
				match(CLOSE_BRACE);
				}
				break;
			case NOW:
				enterOuterAlt(_localctx, 35);
				{
				setState(1277);
				match(NOW);
				setState(1278);
				match(OPEN_BRACE);
				setState(1279);
				match(CLOSE_BRACE);
				}
				break;
			case UUID:
				enterOuterAlt(_localctx, 36);
				{
				setState(1280);
				match(UUID);
				setState(1281);
				match(OPEN_BRACE);
				setState(1282);
				match(CLOSE_BRACE);
				}
				break;
			case STRUUID:
				enterOuterAlt(_localctx, 37);
				{
				setState(1283);
				match(STRUUID);
				setState(1284);
				match(OPEN_BRACE);
				setState(1285);
				match(CLOSE_BRACE);
				}
				break;
			case MD5:
				enterOuterAlt(_localctx, 38);
				{
				setState(1286);
				match(MD5);
				setState(1287);
				match(OPEN_BRACE);
				setState(1288);
				expression(0);
				setState(1289);
				match(CLOSE_BRACE);
				}
				break;
			case SHA1:
				enterOuterAlt(_localctx, 39);
				{
				setState(1291);
				match(SHA1);
				setState(1292);
				match(OPEN_BRACE);
				setState(1293);
				expression(0);
				setState(1294);
				match(CLOSE_BRACE);
				}
				break;
			case SHA256:
				enterOuterAlt(_localctx, 40);
				{
				setState(1296);
				match(SHA256);
				setState(1297);
				match(OPEN_BRACE);
				setState(1298);
				expression(0);
				setState(1299);
				match(CLOSE_BRACE);
				}
				break;
			case SHA384:
				enterOuterAlt(_localctx, 41);
				{
				setState(1301);
				match(SHA384);
				setState(1302);
				match(OPEN_BRACE);
				setState(1303);
				expression(0);
				setState(1304);
				match(CLOSE_BRACE);
				}
				break;
			case SHA512:
				enterOuterAlt(_localctx, 42);
				{
				setState(1306);
				match(SHA512);
				setState(1307);
				match(OPEN_BRACE);
				setState(1308);
				expression(0);
				setState(1309);
				match(CLOSE_BRACE);
				}
				break;
			case COALESCE:
				enterOuterAlt(_localctx, 43);
				{
				setState(1311);
				match(COALESCE);
				setState(1312);
				match(OPEN_BRACE);
				setState(1314);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STR) | (1L << LANG) | (1L << LANGMATCHES) | (1L << DATATYPE) | (1L << BOUND) | (1L << SAMETERM) | (1L << ISIRI) | (1L << ISURI) | (1L << ISBLANK) | (1L << ISLITERAL) | (1L << REGEX) | (1L << SUBSTR) | (1L << TRUE) | (1L << FALSE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (IRI - 64)) | (1L << (URI - 64)) | (1L << (BNODE - 64)) | (1L << (RAND - 64)) | (1L << (ABS - 64)) | (1L << (CEIL - 64)) | (1L << (FLOOR - 64)) | (1L << (ROUND - 64)) | (1L << (CONCAT - 64)) | (1L << (STRLEN - 64)) | (1L << (UCASE - 64)) | (1L << (LCASE - 64)) | (1L << (ENCODE_FOR_URI - 64)) | (1L << (CONTAINS - 64)) | (1L << (STRSTARTS - 64)) | (1L << (STRENDS - 64)) | (1L << (STRBEFORE - 64)) | (1L << (STRAFTER - 64)) | (1L << (REPLACE - 64)) | (1L << (YEAR - 64)) | (1L << (MONTH - 64)) | (1L << (DAY - 64)) | (1L << (HOURS - 64)) | (1L << (MINUTES - 64)) | (1L << (SECONDS - 64)) | (1L << (TIMEZONE - 64)) | (1L << (TZ - 64)) | (1L << (NOW - 64)) | (1L << (UUID - 64)) | (1L << (STRUUID - 64)) | (1L << (MD5 - 64)) | (1L << (SHA1 - 64)) | (1L << (SHA256 - 64)) | (1L << (SHA384 - 64)) | (1L << (SHA512 - 64)) | (1L << (COALESCE - 64)) | (1L << (IF - 64)) | (1L << (STRLANG - 64)) | (1L << (STRDT - 64)) | (1L << (ISNUMERIC - 64)) | (1L << (COUNT - 64)) | (1L << (SUM - 64)) | (1L << (MIN - 64)) | (1L << (MAX - 64)) | (1L << (AVG - 64)) | (1L << (SAMPLE - 64)) | (1L << (GROUP_CONCAT - 64)) | (1L << (NOT - 64)) | (1L << (EXISTS - 64)) | (1L << (IRIREF - 64)) | (1L << (PNAME_NS - 64)) | (1L << (PNAME_LN - 64)) | (1L << (VAR1 - 64)) | (1L << (VAR2 - 64)) | (1L << (INTEGER - 64)) | (1L << (DECIMAL - 64)) | (1L << (DOUBLE - 64)) | (1L << (INTEGER_POSITIVE - 64)) | (1L << (DECIMAL_POSITIVE - 64)) | (1L << (DOUBLE_POSITIVE - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (INTEGER_NEGATIVE - 128)) | (1L << (DECIMAL_NEGATIVE - 128)) | (1L << (DOUBLE_NEGATIVE - 128)) | (1L << (STRING_LITERAL1 - 128)) | (1L << (STRING_LITERAL2 - 128)) | (1L << (STRING_LITERAL_LONG1 - 128)) | (1L << (STRING_LITERAL_LONG2 - 128)) | (1L << (OPEN_BRACE - 128)) | (1L << (PLUS_SIGN - 128)) | (1L << (MINUS_SIGN - 128)) | (1L << (NEGATION - 128)))) != 0)) {
					{
					setState(1313);
					expressionList();
					}
				}

				setState(1316);
				match(CLOSE_BRACE);
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 44);
				{
				setState(1317);
				match(IF);
				setState(1318);
				match(OPEN_BRACE);
				setState(1319);
				expression(0);
				setState(1320);
				match(COMMA);
				setState(1321);
				expression(0);
				setState(1322);
				match(COMMA);
				setState(1323);
				expression(0);
				setState(1324);
				match(CLOSE_BRACE);
				}
				break;
			case STRLANG:
				enterOuterAlt(_localctx, 45);
				{
				setState(1326);
				match(STRLANG);
				setState(1327);
				match(OPEN_BRACE);
				setState(1328);
				expression(0);
				setState(1329);
				match(COMMA);
				setState(1330);
				expression(0);
				setState(1331);
				match(CLOSE_BRACE);
				}
				break;
			case STRDT:
				enterOuterAlt(_localctx, 46);
				{
				setState(1333);
				match(STRDT);
				setState(1334);
				match(OPEN_BRACE);
				setState(1335);
				expression(0);
				setState(1336);
				match(COMMA);
				setState(1337);
				expression(0);
				setState(1338);
				match(CLOSE_BRACE);
				}
				break;
			case SAMETERM:
				enterOuterAlt(_localctx, 47);
				{
				setState(1340);
				match(SAMETERM);
				setState(1341);
				match(OPEN_BRACE);
				setState(1342);
				expression(0);
				setState(1343);
				match(COMMA);
				setState(1344);
				expression(0);
				setState(1345);
				match(CLOSE_BRACE);
				}
				break;
			case ISIRI:
				enterOuterAlt(_localctx, 48);
				{
				setState(1347);
				match(ISIRI);
				setState(1348);
				match(OPEN_BRACE);
				setState(1349);
				expression(0);
				setState(1350);
				match(CLOSE_BRACE);
				}
				break;
			case ISURI:
				enterOuterAlt(_localctx, 49);
				{
				setState(1352);
				match(ISURI);
				setState(1353);
				match(OPEN_BRACE);
				setState(1354);
				expression(0);
				setState(1355);
				match(CLOSE_BRACE);
				}
				break;
			case ISBLANK:
				enterOuterAlt(_localctx, 50);
				{
				setState(1357);
				match(ISBLANK);
				setState(1358);
				match(OPEN_BRACE);
				setState(1359);
				expression(0);
				setState(1360);
				match(CLOSE_BRACE);
				}
				break;
			case ISLITERAL:
				enterOuterAlt(_localctx, 51);
				{
				setState(1362);
				match(ISLITERAL);
				setState(1363);
				match(OPEN_BRACE);
				setState(1364);
				expression(0);
				setState(1365);
				match(CLOSE_BRACE);
				}
				break;
			case ISNUMERIC:
				enterOuterAlt(_localctx, 52);
				{
				setState(1367);
				match(ISNUMERIC);
				setState(1368);
				match(OPEN_BRACE);
				setState(1369);
				expression(0);
				setState(1370);
				match(CLOSE_BRACE);
				}
				break;
			case REGEX:
				enterOuterAlt(_localctx, 53);
				{
				setState(1372);
				regexExpression();
				}
				break;
			case EXISTS:
				enterOuterAlt(_localctx, 54);
				{
				setState(1373);
				existsFunction();
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 55);
				{
				setState(1374);
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
		enterRule(_localctx, 230, RULE_regexExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1377);
			match(REGEX);
			setState(1378);
			match(OPEN_BRACE);
			setState(1379);
			expression(0);
			setState(1380);
			match(COMMA);
			setState(1381);
			expression(0);
			setState(1384);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1382);
				match(COMMA);
				setState(1383);
				expression(0);
				}
			}

			setState(1386);
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
		enterRule(_localctx, 232, RULE_subStringExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1388);
			match(SUBSTR);
			setState(1389);
			match(OPEN_BRACE);
			setState(1390);
			expression(0);
			setState(1391);
			match(COMMA);
			setState(1392);
			expression(0);
			setState(1395);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1393);
				match(COMMA);
				setState(1394);
				expression(0);
				}
			}

			setState(1397);
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
		enterRule(_localctx, 234, RULE_strReplaceExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1399);
			match(REPLACE);
			setState(1400);
			match(OPEN_BRACE);
			setState(1401);
			expression(0);
			setState(1402);
			match(COMMA);
			setState(1403);
			expression(0);
			setState(1404);
			match(COMMA);
			setState(1405);
			expression(0);
			setState(1408);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1406);
				match(COMMA);
				setState(1407);
				expression(0);
				}
			}

			setState(1410);
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
		enterRule(_localctx, 236, RULE_existsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1412);
			match(EXISTS);
			setState(1413);
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
		enterRule(_localctx, 238, RULE_notExistsFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1415);
			match(NOT);
			setState(1416);
			match(EXISTS);
			setState(1417);
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
		enterRule(_localctx, 240, RULE_aggregate);
		int _la;
		try {
			setState(1483);
			switch (_input.LA(1)) {
			case COUNT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1419);
				match(COUNT);
				setState(1420);
				match(OPEN_BRACE);
				setState(1422);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1421);
					match(DISTINCT);
					}
				}

				setState(1426);
				switch (_input.LA(1)) {
				case ASTERISK:
					{
					setState(1424);
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
					setState(1425);
					expression(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1428);
				match(CLOSE_BRACE);
				}
				break;
			case SUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(1429);
				match(SUM);
				setState(1430);
				match(OPEN_BRACE);
				setState(1432);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1431);
					match(DISTINCT);
					}
				}

				setState(1434);
				expression(0);
				setState(1435);
				match(CLOSE_BRACE);
				}
				break;
			case MIN:
				enterOuterAlt(_localctx, 3);
				{
				setState(1437);
				match(MIN);
				setState(1438);
				match(OPEN_BRACE);
				setState(1440);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1439);
					match(DISTINCT);
					}
				}

				setState(1442);
				expression(0);
				setState(1443);
				match(CLOSE_BRACE);
				}
				break;
			case MAX:
				enterOuterAlt(_localctx, 4);
				{
				setState(1445);
				match(MAX);
				setState(1446);
				match(OPEN_BRACE);
				setState(1448);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1447);
					match(DISTINCT);
					}
				}

				setState(1450);
				expression(0);
				setState(1451);
				match(CLOSE_BRACE);
				}
				break;
			case AVG:
				enterOuterAlt(_localctx, 5);
				{
				setState(1453);
				match(AVG);
				setState(1454);
				match(OPEN_BRACE);
				setState(1456);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1455);
					match(DISTINCT);
					}
				}

				setState(1458);
				expression(0);
				setState(1459);
				match(CLOSE_BRACE);
				}
				break;
			case SAMPLE:
				enterOuterAlt(_localctx, 6);
				{
				setState(1461);
				match(SAMPLE);
				setState(1462);
				match(OPEN_BRACE);
				setState(1464);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1463);
					match(DISTINCT);
					}
				}

				setState(1466);
				expression(0);
				setState(1467);
				match(CLOSE_BRACE);
				}
				break;
			case GROUP_CONCAT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1469);
				match(GROUP_CONCAT);
				setState(1470);
				match(OPEN_BRACE);
				setState(1472);
				_la = _input.LA(1);
				if (_la==DISTINCT) {
					{
					setState(1471);
					match(DISTINCT);
					}
				}

				setState(1474);
				expression(0);
				setState(1479);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(1475);
					match(SEMICOLON);
					setState(1476);
					match(SEPARATOR);
					setState(1477);
					match(EQUAL);
					setState(1478);
					string();
					}
				}

				setState(1481);
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
		enterRule(_localctx, 242, RULE_iriRefOrFunction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1485);
			iri();
			setState(1487);
			switch ( getInterpreter().adaptivePredict(_input,145,_ctx) ) {
			case 1:
				{
				setState(1486);
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
		enterRule(_localctx, 244, RULE_rdfLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1489);
			string();
			setState(1493);
			switch ( getInterpreter().adaptivePredict(_input,146,_ctx) ) {
			case 1:
				{
				setState(1490);
				match(LANGTAG);
				}
				break;
			case 2:
				{
				{
				setState(1491);
				match(REFERENCE);
				setState(1492);
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
		enterRule(_localctx, 246, RULE_numericLiteral);
		try {
			setState(1498);
			switch (_input.LA(1)) {
			case INTEGER:
			case DECIMAL:
			case DOUBLE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1495);
				numericLiteralUnsigned();
				}
				break;
			case INTEGER_POSITIVE:
			case DECIMAL_POSITIVE:
			case DOUBLE_POSITIVE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1496);
				numericLiteralPositive();
				}
				break;
			case INTEGER_NEGATIVE:
			case DECIMAL_NEGATIVE:
			case DOUBLE_NEGATIVE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1497);
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
		enterRule(_localctx, 248, RULE_numericLiteralUnsigned);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1500);
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
		enterRule(_localctx, 250, RULE_numericLiteralPositive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1502);
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
		enterRule(_localctx, 252, RULE_numericLiteralNegative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1504);
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
		enterRule(_localctx, 254, RULE_booleanLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1506);
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
		enterRule(_localctx, 256, RULE_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1508);
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
		enterRule(_localctx, 258, RULE_iri);
		try {
			setState(1512);
			switch (_input.LA(1)) {
			case IRIREF:
				enterOuterAlt(_localctx, 1);
				{
				setState(1510);
				match(IRIREF);
				}
				break;
			case PNAME_NS:
			case PNAME_LN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1511);
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
		enterRule(_localctx, 260, RULE_prefixedName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1514);
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
		enterRule(_localctx, 262, RULE_blankNode);
		try {
			setState(1518);
			switch (_input.LA(1)) {
			case BLANK_NODE_LABEL:
				enterOuterAlt(_localctx, 1);
				{
				setState(1516);
				match(BLANK_NODE_LABEL);
				}
				break;
			case OPEN_SQUARE_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1517);
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
		enterRule(_localctx, 264, RULE_anon);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1520);
			match(OPEN_SQUARE_BRACKET);
			setState(1521);
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
		case 110:
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00a5\u05f6\4\2\t"+
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
		"\t\u0085\4\u0086\t\u0086\3\2\3\2\3\2\3\2\3\2\5\2\u0112\n\2\3\2\3\2\3\2"+
		"\3\2\5\2\u0118\n\2\3\2\5\2\u011b\n\2\3\3\3\3\7\3\u011f\n\3\f\3\16\3\u0122"+
		"\13\3\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\7\6\u012d\n\6\f\6\16\6\u0130"+
		"\13\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b\5\b\u013c\n\b\3\b\6\b\u013f"+
		"\n\b\r\b\16\b\u0140\3\b\5\b\u0144\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\t"+
		"\u014d\n\t\3\n\3\n\3\n\7\n\u0152\n\n\f\n\16\n\u0155\13\n\3\n\3\n\3\n\3"+
		"\n\7\n\u015b\n\n\f\n\16\n\u015e\13\n\3\n\3\n\3\n\5\n\u0163\n\n\3\n\3\n"+
		"\5\n\u0167\n\n\3\13\3\13\6\13\u016b\n\13\r\13\16\13\u016c\3\13\5\13\u0170"+
		"\n\13\3\13\7\13\u0173\n\13\f\13\16\13\u0176\13\13\3\13\5\13\u0179\n\13"+
		"\3\13\3\13\3\f\3\f\7\f\u017f\n\f\f\f\16\f\u0182\13\f\3\f\3\f\5\f\u0186"+
		"\n\f\3\f\5\f\u0189\n\f\3\r\3\r\5\r\u018d\n\r\3\r\3\r\3\16\5\16\u0192\n"+
		"\16\3\16\3\16\3\17\5\17\u0197\n\17\3\17\5\17\u019a\n\17\3\17\5\17\u019d"+
		"\n\17\3\17\5\17\u01a0\n\17\3\20\3\20\3\20\6\20\u01a5\n\20\r\20\16\20\u01a6"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u01af\n\21\3\21\3\21\3\21\5\21\u01b4"+
		"\n\21\3\22\3\22\6\22\u01b8\n\22\r\22\16\22\u01b9\3\23\3\23\3\24\3\24\3"+
		"\24\6\24\u01c1\n\24\r\24\16\24\u01c2\3\25\3\25\3\25\3\25\3\25\3\25\3\25"+
		"\5\25\u01cc\n\25\3\26\3\26\5\26\u01d0\n\26\3\26\3\26\5\26\u01d4\n\26\5"+
		"\26\u01d6\n\26\3\27\3\27\3\27\3\30\3\30\3\30\3\31\3\31\5\31\u01e0\n\31"+
		"\3\32\3\32\3\32\3\32\3\32\3\32\7\32\u01e8\n\32\f\32\16\32\u01eb\13\32"+
		"\3\32\3\32\5\32\u01ef\n\32\5\32\u01f1\n\32\3\33\3\33\3\33\3\33\3\33\3"+
		"\33\3\33\3\33\3\33\3\33\3\33\5\33\u01fe\n\33\3\34\3\34\5\34\u0202\n\34"+
		"\3\34\3\34\3\34\5\34\u0207\n\34\3\35\3\35\5\35\u020b\n\35\3\35\3\35\3"+
		"\36\3\36\5\36\u0211\n\36\3\36\3\36\3\37\3\37\5\37\u0217\n\37\3\37\3\37"+
		"\3 \3 \5 \u021d\n \3 \3 \3 \3 \3!\3!\5!\u0225\n!\3!\3!\3!\3!\3\"\3\"\5"+
		"\"\u022d\n\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3&\3"+
		"&\5&\u0241\n&\3&\3&\5&\u0245\n&\3&\5&\u0248\n&\3&\7&\u024b\n&\f&\16&\u024e"+
		"\13&\3&\3&\3&\3\'\3\'\3\'\3(\3(\3(\3)\3)\5)\u025b\n)\3)\3)\3*\3*\5*\u0261"+
		"\n*\3*\5*\u0264\n*\3+\3+\3+\3,\3,\3,\3,\5,\u026d\n,\3-\3-\3-\3-\3.\3."+
		"\3.\3.\3/\5/\u0278\n/\3/\7/\u027b\n/\f/\16/\u027e\13/\3\60\3\60\5\60\u0282"+
		"\n\60\3\60\5\60\u0285\n\60\3\61\3\61\3\61\3\61\5\61\u028b\n\61\3\61\3"+
		"\61\3\62\3\62\3\62\5\62\u0292\n\62\7\62\u0294\n\62\f\62\16\62\u0297\13"+
		"\62\3\63\3\63\3\63\5\63\u029c\n\63\3\63\3\63\3\64\5\64\u02a1\n\64\3\64"+
		"\7\64\u02a4\n\64\f\64\16\64\u02a7\13\64\3\65\3\65\5\65\u02ab\n\65\3\65"+
		"\5\65\u02ae\n\65\3\66\3\66\3\66\5\66\u02b3\n\66\7\66\u02b5\n\66\f\66\16"+
		"\66\u02b8\13\66\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\5\67\u02c2\n\67"+
		"\38\38\38\39\39\39\39\3:\3:\5:\u02cd\n:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;"+
		"\3<\3<\3<\3=\3=\5=\u02de\n=\3>\3>\3>\7>\u02e3\n>\f>\16>\u02e6\13>\3>\3"+
		">\3?\3?\7?\u02ec\n?\f?\16?\u02ef\13?\3?\3?\3?\7?\u02f4\n?\f?\16?\u02f7"+
		"\13?\3?\3?\3@\3@\7@\u02fd\n@\f@\16@\u0300\13@\3@\3@\3A\3A\3A\3A\3A\5A"+
		"\u0309\nA\3B\3B\3B\3C\3C\3C\7C\u0311\nC\fC\16C\u0314\13C\3D\3D\3D\3E\3"+
		"E\3E\3E\3E\3E\5E\u031f\nE\3F\3F\3F\3G\3G\5G\u0326\nG\3G\3G\5G\u032a\n"+
		"G\3G\3G\3H\3H\3H\7H\u0331\nH\fH\16H\u0334\13H\3I\3I\5I\u0338\nI\3I\3I"+
		"\3J\3J\3J\5J\u033f\nJ\7J\u0341\nJ\fJ\16J\u0344\13J\3K\3K\3K\3K\3K\3K\5"+
		"K\u034c\nK\3L\5L\u034f\nL\3M\3M\3M\3M\3M\3M\5M\u0357\nM\7M\u0359\nM\f"+
		"M\16M\u035c\13M\3N\3N\5N\u0360\nN\3O\3O\3O\7O\u0365\nO\fO\16O\u0368\13"+
		"O\3P\3P\3Q\3Q\3Q\3Q\3Q\3Q\5Q\u0372\nQ\3R\5R\u0375\nR\3S\3S\5S\u0379\n"+
		"S\3S\3S\3S\5S\u037e\nS\7S\u0380\nS\fS\16S\u0383\13S\3T\3T\5T\u0387\nT"+
		"\3T\3T\3U\3U\3V\3V\3W\3W\3W\7W\u0392\nW\fW\16W\u0395\13W\3X\3X\3Y\3Y\3"+
		"Z\3Z\3Z\7Z\u039e\nZ\fZ\16Z\u03a1\13Z\3[\3[\3[\7[\u03a6\n[\f[\16[\u03a9"+
		"\13[\3\\\3\\\5\\\u03ad\n\\\3]\5]\u03b0\n]\3]\3]\3^\3^\3_\3_\3_\3_\3_\3"+
		"_\3_\3_\5_\u03be\n_\3`\3`\3`\3`\3`\7`\u03c5\n`\f`\16`\u03c8\13`\5`\u03ca"+
		"\n`\3`\5`\u03cd\n`\3a\5a\u03d0\na\3a\3a\5a\u03d4\na\3b\3b\3c\3c\5c\u03da"+
		"\nc\3d\3d\3d\3d\3e\3e\5e\u03e2\ne\3f\3f\3f\3f\3g\3g\6g\u03ea\ng\rg\16"+
		"g\u03eb\3g\3g\3h\3h\6h\u03f2\nh\rh\16h\u03f3\3h\3h\3i\3i\5i\u03fa\ni\3"+
		"j\3j\5j\u03fe\nj\3k\3k\5k\u0402\nk\3l\3l\5l\u0406\nl\3m\3m\3n\3n\3n\3"+
		"n\3n\3n\5n\u0410\nn\3o\3o\3o\3p\3p\3p\3p\3p\3p\5p\u041b\np\3p\3p\3p\3"+
		"p\3p\3p\3p\3p\3p\3p\3p\3p\3p\5p\u042a\np\3p\3p\3p\5p\u042f\np\3p\3p\3"+
		"p\3p\3p\3p\3p\7p\u0438\np\fp\16p\u043b\13p\3q\3q\5q\u043f\nq\3q\3q\5q"+
		"\u0443\nq\3r\5r\u0446\nr\3r\3r\3s\3s\3s\3s\3s\3s\3s\3s\3s\3s\5s\u0454"+
		"\ns\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\5t\u047f\nt"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\5t\u049c\nt\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\5t\u0525\nt\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t"+
		"\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\3t\5t"+
		"\u0562\nt\3u\3u\3u\3u\3u\3u\3u\5u\u056b\nu\3u\3u\3v\3v\3v\3v\3v\3v\3v"+
		"\5v\u0576\nv\3v\3v\3w\3w\3w\3w\3w\3w\3w\3w\3w\5w\u0583\nw\3w\3w\3x\3x"+
		"\3x\3y\3y\3y\3y\3z\3z\3z\5z\u0591\nz\3z\3z\5z\u0595\nz\3z\3z\3z\3z\5z"+
		"\u059b\nz\3z\3z\3z\3z\3z\3z\5z\u05a3\nz\3z\3z\3z\3z\3z\3z\5z\u05ab\nz"+
		"\3z\3z\3z\3z\3z\3z\5z\u05b3\nz\3z\3z\3z\3z\3z\3z\5z\u05bb\nz\3z\3z\3z"+
		"\3z\3z\3z\5z\u05c3\nz\3z\3z\3z\3z\3z\5z\u05ca\nz\3z\3z\5z\u05ce\nz\3{"+
		"\3{\5{\u05d2\n{\3|\3|\3|\3|\5|\u05d8\n|\3}\3}\3}\5}\u05dd\n}\3~\3~\3\177"+
		"\3\177\3\u0080\3\u0080\3\u0081\3\u0081\3\u0082\3\u0082\3\u0083\3\u0083"+
		"\5\u0083\u05eb\n\u0083\3\u0084\3\u0084\3\u0085\3\u0085\5\u0085\u05f1\n"+
		"\u0085\3\u0086\3\u0086\3\u0086\3\u0086\2\3\u00de\u0087\2\4\6\b\n\f\16"+
		"\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bd"+
		"fhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092"+
		"\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa"+
		"\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2"+
		"\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da"+
		"\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0\u00f2"+
		"\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106\u0108\u010a"+
		"\2\20\3\2\7\b\3\2\21\22\4\2\u0099\u0099\u009b\u009c\3\2yz\3\2\u0099\u009a"+
		"\4\2\u009b\u009b\u009f\u009f\4\2\u008b\u008d\u00a0\u00a2\4\2\u0099\u009a"+
		"\u009e\u009e\3\2|~\3\2\177\u0081\3\2\u0082\u0084\3\2\'(\3\2\u0085\u0088"+
		"\3\2vw\u066e\2\u011a\3\2\2\2\4\u0120\3\2\2\2\6\u0123\3\2\2\2\b\u0126\3"+
		"\2\2\2\n\u012a\3\2\2\2\f\u0134\3\2\2\2\16\u0139\3\2\2\2\20\u014c\3\2\2"+
		"\2\22\u014e\3\2\2\2\24\u0168\3\2\2\2\26\u017c\3\2\2\2\30\u018a\3\2\2\2"+
		"\32\u0191\3\2\2\2\34\u0196\3\2\2\2\36\u01a1\3\2\2\2 \u01b3\3\2\2\2\"\u01b5"+
		"\3\2\2\2$\u01bb\3\2\2\2&\u01bd\3\2\2\2(\u01cb\3\2\2\2*\u01d5\3\2\2\2,"+
		"\u01d7\3\2\2\2.\u01da\3\2\2\2\60\u01df\3\2\2\2\62\u01e1\3\2\2\2\64\u01fd"+
		"\3\2\2\2\66\u01ff\3\2\2\28\u0208\3\2\2\2:\u020e\3\2\2\2<\u0214\3\2\2\2"+
		">\u021a\3\2\2\2@\u0222\3\2\2\2B\u022a\3\2\2\2D\u0232\3\2\2\2F\u0236\3"+
		"\2\2\2H\u023a\3\2\2\2J\u0240\3\2\2\2L\u0252\3\2\2\2N\u0255\3\2\2\2P\u0258"+
		"\3\2\2\2R\u0263\3\2\2\2T\u0265\3\2\2\2V\u026c\3\2\2\2X\u026e\3\2\2\2Z"+
		"\u0272\3\2\2\2\\\u0277\3\2\2\2^\u027f\3\2\2\2`\u0286\3\2\2\2b\u028e\3"+
		"\2\2\2d\u0298\3\2\2\2f\u02a0\3\2\2\2h\u02a8\3\2\2\2j\u02af\3\2\2\2l\u02c1"+
		"\3\2\2\2n\u02c3\3\2\2\2p\u02c6\3\2\2\2r\u02ca\3\2\2\2t\u02d1\3\2\2\2v"+
		"\u02d8\3\2\2\2x\u02dd\3\2\2\2z\u02df\3\2\2\2|\u02e9\3\2\2\2~\u02fa\3\2"+
		"\2\2\u0080\u0308\3\2\2\2\u0082\u030a\3\2\2\2\u0084\u030d\3\2\2\2\u0086"+
		"\u0315\3\2\2\2\u0088\u031e\3\2\2\2\u008a\u0320\3\2\2\2\u008c\u0323\3\2"+
		"\2\2\u008e\u032d\3\2\2\2\u0090\u0335\3\2\2\2\u0092\u033b\3\2\2\2\u0094"+
		"\u034b\3\2\2\2\u0096\u034e\3\2\2\2\u0098\u0350\3\2\2\2\u009a\u035f\3\2"+
		"\2\2\u009c\u0361\3\2\2\2\u009e\u0369\3\2\2\2\u00a0\u0371\3\2\2\2\u00a2"+
		"\u0374\3\2\2\2\u00a4\u0378\3\2\2\2\u00a6\u0386\3\2\2\2\u00a8\u038a\3\2"+
		"\2\2\u00aa\u038c\3\2\2\2\u00ac\u038e\3\2\2\2\u00ae\u0396\3\2\2\2\u00b0"+
		"\u0398\3\2\2\2\u00b2\u039a\3\2\2\2\u00b4\u03a2\3\2\2\2\u00b6\u03aa\3\2"+
		"\2\2\u00b8\u03af\3\2\2\2\u00ba\u03b3\3\2\2\2\u00bc\u03bd\3\2\2\2\u00be"+
		"\u03cc\3\2\2\2\u00c0\u03cf\3\2\2\2\u00c2\u03d5\3\2\2\2\u00c4\u03d9\3\2"+
		"\2\2\u00c6\u03db\3\2\2\2\u00c8\u03e1\3\2\2\2\u00ca\u03e3\3\2\2\2\u00cc"+
		"\u03e7\3\2\2\2\u00ce\u03ef\3\2\2\2\u00d0\u03f9\3\2\2\2\u00d2\u03fd\3\2"+
		"\2\2\u00d4\u0401\3\2\2\2\u00d6\u0405\3\2\2\2\u00d8\u0407\3\2\2\2\u00da"+
		"\u040f\3\2\2\2\u00dc\u0411\3\2\2\2\u00de\u041a\3\2\2\2\u00e0\u043e\3\2"+
		"\2\2\u00e2\u0445\3\2\2\2\u00e4\u0453\3\2\2\2\u00e6\u0561\3\2\2\2\u00e8"+
		"\u0563\3\2\2\2\u00ea\u056e\3\2\2\2\u00ec\u0579\3\2\2\2\u00ee\u0586\3\2"+
		"\2\2\u00f0\u0589\3\2\2\2\u00f2\u05cd\3\2\2\2\u00f4\u05cf\3\2\2\2\u00f6"+
		"\u05d3\3\2\2\2\u00f8\u05dc\3\2\2\2\u00fa\u05de\3\2\2\2\u00fc\u05e0\3\2"+
		"\2\2\u00fe\u05e2\3\2\2\2\u0100\u05e4\3\2\2\2\u0102\u05e6\3\2\2\2\u0104"+
		"\u05ea\3\2\2\2\u0106\u05ec\3\2\2\2\u0108\u05f0\3\2\2\2\u010a\u05f2\3\2"+
		"\2\2\u010c\u0111\5\4\3\2\u010d\u0112\5\n\6\2\u010e\u0112\5\22\n\2\u010f"+
		"\u0112\5\24\13\2\u0110\u0112\5\26\f\2\u0111\u010d\3\2\2\2\u0111\u010e"+
		"\3\2\2\2\u0111\u010f\3\2\2\2\u0111\u0110\3\2\2\2\u0111\u0112\3\2\2\2\u0112"+
		"\u0113\3\2\2\2\u0113\u0114\5\60\31\2\u0114\u0115\7\2\2\3\u0115\u011b\3"+
		"\2\2\2\u0116\u0118\5\62\32\2\u0117\u0116\3\2\2\2\u0117\u0118\3\2\2\2\u0118"+
		"\u0119\3\2\2\2\u0119\u011b\7\2\2\3\u011a\u010c\3\2\2\2\u011a\u0117\3\2"+
		"\2\2\u011b\3\3\2\2\2\u011c\u011f\5\6\4\2\u011d\u011f\5\b\5\2\u011e\u011c"+
		"\3\2\2\2\u011e\u011d\3\2\2\2\u011f\u0122\3\2\2\2\u0120\u011e\3\2\2\2\u0120"+
		"\u0121\3\2\2\2\u0121\5\3\2\2\2\u0122\u0120\3\2\2\2\u0123\u0124\7\4\2\2"+
		"\u0124\u0125\7u\2\2\u0125\7\3\2\2\2\u0126\u0127\7\5\2\2\u0127\u0128\7"+
		"v\2\2\u0128\u0129\7u\2\2\u0129\t\3\2\2\2\u012a\u012e\5\16\b\2\u012b\u012d"+
		"\5\30\r\2\u012c\u012b\3\2\2\2\u012d\u0130\3\2\2\2\u012e\u012c\3\2\2\2"+
		"\u012e\u012f\3\2\2\2\u012f\u0131\3\2\2\2\u0130\u012e\3\2\2\2\u0131\u0132"+
		"\5\32\16\2\u0132\u0133\5\34\17\2\u0133\13\3\2\2\2\u0134\u0135\5\16\b\2"+
		"\u0135\u0136\5\32\16\2\u0136\u0137\5\34\17\2\u0137\u0138\5\60\31\2\u0138"+
		"\r\3\2\2\2\u0139\u013b\7\6\2\2\u013a\u013c\t\2\2\2\u013b\u013a\3\2\2\2"+
		"\u013b\u013c\3\2\2\2\u013c\u0143\3\2\2\2\u013d\u013f\5\20\t\2\u013e\u013d"+
		"\3\2\2\2\u013f\u0140\3\2\2\2\u0140\u013e\3\2\2\2\u0140\u0141\3\2\2\2\u0141"+
		"\u0144\3\2\2\2\u0142\u0144\7\u009b\2\2\u0143\u013e\3\2\2\2\u0143\u0142"+
		"\3\2\2\2\u0144\17\3\2\2\2\u0145\u014d\5\u00d8m\2\u0146\u0147\7\u0091\2"+
		"\2\u0147\u0148\5\u00dep\2\u0148\u0149\7:\2\2\u0149\u014a\5\u00d8m\2\u014a"+
		"\u014b\7\u0092\2\2\u014b\u014d\3\2\2\2\u014c\u0145\3\2\2\2\u014c\u0146"+
		"\3\2\2\2\u014d\21\3\2\2\2\u014e\u0166\7\t\2\2\u014f\u0153\5\u0090I\2\u0150"+
		"\u0152\5\30\r\2\u0151\u0150\3\2\2\2\u0152\u0155\3\2\2\2\u0153\u0151\3"+
		"\2\2\2\u0153\u0154\3\2\2\2\u0154\u0156\3\2\2\2\u0155\u0153\3\2\2\2\u0156"+
		"\u0157\5\32\16\2\u0157\u0158\5\34\17\2\u0158\u0167\3\2\2\2\u0159\u015b"+
		"\5\30\r\2\u015a\u0159\3\2\2\2\u015b\u015e\3\2\2\2\u015c\u015a\3\2\2\2"+
		"\u015c\u015d\3\2\2\2\u015d\u015f\3\2\2\2\u015e\u015c\3\2\2\2\u015f\u0160"+
		"\7\16\2\2\u0160\u0162\7\u0093\2\2\u0161\u0163\5b\62\2\u0162\u0161\3\2"+
		"\2\2\u0162\u0163\3\2\2\2\u0163\u0164\3\2\2\2\u0164\u0165\7\u0094\2\2\u0165"+
		"\u0167\5\34\17\2\u0166\u014f\3\2\2\2\u0166\u015c\3\2\2\2\u0167\23\3\2"+
		"\2\2\u0168\u016f\7\n\2\2\u0169\u016b\5\u00d6l\2\u016a\u0169\3\2\2\2\u016b"+
		"\u016c\3\2\2\2\u016c\u016a\3\2\2\2\u016c\u016d\3\2\2\2\u016d\u0170\3\2"+
		"\2\2\u016e\u0170\7\u009b\2\2\u016f\u016a\3\2\2\2\u016f\u016e\3\2\2\2\u0170"+
		"\u0174\3\2\2\2\u0171\u0173\5\30\r\2\u0172\u0171\3\2\2\2\u0173\u0176\3"+
		"\2\2\2\u0174\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175\u0178\3\2\2\2\u0176"+
		"\u0174\3\2\2\2\u0177\u0179\5\32\16\2\u0178\u0177\3\2\2\2\u0178\u0179\3"+
		"\2\2\2\u0179\u017a\3\2\2\2\u017a\u017b\5\34\17\2\u017b\25\3\2\2\2\u017c"+
		"\u0180\7\13\2\2\u017d\u017f\5\30\r\2\u017e\u017d\3\2\2\2\u017f\u0182\3"+
		"\2\2\2\u0180\u017e\3\2\2\2\u0180\u0181\3\2\2\2\u0181\u0183\3\2\2\2\u0182"+
		"\u0180\3\2\2\2\u0183\u0185\5\32\16\2\u0184\u0186\5\36\20\2\u0185\u0184"+
		"\3\2\2\2\u0185\u0186\3\2\2\2\u0186\u0188\3\2\2\2\u0187\u0189\5\"\22\2"+
		"\u0188\u0187\3\2\2\2\u0188\u0189\3\2\2\2\u0189\27\3\2\2\2\u018a\u018c"+
		"\7\f\2\2\u018b\u018d\7\r\2\2\u018c\u018b\3\2\2\2\u018c\u018d\3\2\2\2\u018d"+
		"\u018e\3\2\2\2\u018e\u018f\5\u0104\u0083\2\u018f\31\3\2\2\2\u0190\u0192"+
		"\7\16\2\2\u0191\u0190\3\2\2\2\u0191\u0192\3\2\2\2\u0192\u0193\3\2\2\2"+
		"\u0193\u0194\5d\63\2\u0194\33\3\2\2\2\u0195\u0197\5\36\20\2\u0196\u0195"+
		"\3\2\2\2\u0196\u0197\3\2\2\2\u0197\u0199\3\2\2\2\u0198\u019a\5\"\22\2"+
		"\u0199\u0198\3\2\2\2\u0199\u019a\3\2\2\2\u019a\u019c\3\2\2\2\u019b\u019d"+
		"\5&\24\2\u019c\u019b\3\2\2\2\u019c\u019d\3\2\2\2\u019d\u019f\3\2\2\2\u019e"+
		"\u01a0\5*\26\2\u019f\u019e\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0\35\3\2\2"+
		"\2\u01a1\u01a2\7;\2\2\u01a2\u01a4\7\20\2\2\u01a3\u01a5\5 \21\2\u01a4\u01a3"+
		"\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\u01a4\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7"+
		"\37\3\2\2\2\u01a8\u01b4\5\u00e6t\2\u01a9\u01b4\5\u008aF\2\u01aa\u01ab"+
		"\7\u0091\2\2\u01ab\u01ae\5\u00dep\2\u01ac\u01ad\7:\2\2\u01ad\u01af\5\u00d8"+
		"m\2\u01ae\u01ac\3\2\2\2\u01ae\u01af\3\2\2\2\u01af\u01b0\3\2\2\2\u01b0"+
		"\u01b1\7\u0092\2\2\u01b1\u01b4\3\2\2\2\u01b2\u01b4\5\u00d8m\2\u01b3\u01a8"+
		"\3\2\2\2\u01b3\u01a9\3\2\2\2\u01b3\u01aa\3\2\2\2\u01b3\u01b2\3\2\2\2\u01b4"+
		"!\3\2\2\2\u01b5\u01b7\7<\2\2\u01b6\u01b8\5$\23\2\u01b7\u01b6\3\2\2\2\u01b8"+
		"\u01b9\3\2\2\2\u01b9\u01b7\3\2\2\2\u01b9\u01ba\3\2\2\2\u01ba#\3\2\2\2"+
		"\u01bb\u01bc\5\u0088E\2\u01bc%\3\2\2\2\u01bd\u01be\7\17\2\2\u01be\u01c0"+
		"\7\20\2\2\u01bf\u01c1\5(\25\2\u01c0\u01bf\3\2\2\2\u01c1\u01c2\3\2\2\2"+
		"\u01c2\u01c0\3\2\2\2\u01c2\u01c3\3\2\2\2\u01c3\'\3\2\2\2\u01c4\u01c5\t"+
		"\3\2\2\u01c5\u01c6\7\u0091\2\2\u01c6\u01c7\5\u00dep\2\u01c7\u01c8\7\u0092"+
		"\2\2\u01c8\u01cc\3\2\2\2\u01c9\u01cc\5\u0088E\2\u01ca\u01cc\5\u00d8m\2"+
		"\u01cb\u01c4\3\2\2\2\u01cb\u01c9\3\2\2\2\u01cb\u01ca\3\2\2\2\u01cc)\3"+
		"\2\2\2\u01cd\u01cf\5,\27\2\u01ce\u01d0\5.\30\2\u01cf\u01ce\3\2\2\2\u01cf"+
		"\u01d0\3\2\2\2\u01d0\u01d6\3\2\2\2\u01d1\u01d3\5.\30\2\u01d2\u01d4\5,"+
		"\27\2\u01d3\u01d2\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d6\3\2\2\2\u01d5"+
		"\u01cd\3\2\2\2\u01d5\u01d1\3\2\2\2\u01d6+\3\2\2\2\u01d7\u01d8\7\23\2\2"+
		"\u01d8\u01d9\7|\2\2\u01d9-\3\2\2\2\u01da\u01db\7\24\2\2\u01db\u01dc\7"+
		"|\2\2\u01dc/\3\2\2\2\u01dd\u01de\7\25\2\2\u01de\u01e0\5x=\2\u01df\u01dd"+
		"\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\61\3\2\2\2\u01e1\u01f0\5\4\3\2\u01e2"+
		"\u01e9\5\64\33\2\u01e3\u01e4\7\u0097\2\2\u01e4\u01e5\5\4\3\2\u01e5\u01e6"+
		"\5\64\33\2\u01e6\u01e8\3\2\2\2\u01e7\u01e3\3\2\2\2\u01e8\u01eb\3\2\2\2"+
		"\u01e9\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea\u01ee\3\2\2\2\u01eb\u01e9"+
		"\3\2\2\2\u01ec\u01ed\7\u0097\2\2\u01ed\u01ef\5\4\3\2\u01ee\u01ec\3\2\2"+
		"\2\u01ee\u01ef\3\2\2\2\u01ef\u01f1\3\2\2\2\u01f0\u01e2\3\2\2\2\u01f0\u01f1"+
		"\3\2\2\2\u01f1\63\3\2\2\2\u01f2\u01fe\5\66\34\2\u01f3\u01fe\58\35\2\u01f4"+
		"\u01fe\5:\36\2\u01f5\u01fe\5> \2\u01f6\u01fe\5@!\2\u01f7\u01fe\5B\"\2"+
		"\u01f8\u01fe\5<\37\2\u01f9\u01fe\5D#\2\u01fa\u01fe\5F$\2\u01fb\u01fe\5"+
		"H%\2\u01fc\u01fe\5J&\2\u01fd\u01f2\3\2\2\2\u01fd\u01f3\3\2\2\2\u01fd\u01f4"+
		"\3\2\2\2\u01fd\u01f5\3\2\2\2\u01fd\u01f6\3\2\2\2\u01fd\u01f7\3\2\2\2\u01fd"+
		"\u01f8\3\2\2\2\u01fd\u01f9\3\2\2\2\u01fd\u01fa\3\2\2\2\u01fd\u01fb\3\2"+
		"\2\2\u01fd\u01fc\3\2\2\2\u01fe\65\3\2\2\2\u01ff\u0201\7)\2\2\u0200\u0202"+
		"\7\63\2\2\u0201\u0200\3\2\2\2\u0201\u0202\3\2\2\2\u0202\u0203\3\2\2\2"+
		"\u0203\u0206\5\u0104\u0083\2\u0204\u0205\78\2\2\u0205\u0207\5T+\2\u0206"+
		"\u0204\3\2\2\2\u0206\u0207\3\2\2\2\u0207\67\3\2\2\2\u0208\u020a\7*\2\2"+
		"\u0209\u020b\7\63\2\2\u020a\u0209\3\2\2\2\u020a\u020b\3\2\2\2\u020b\u020c"+
		"\3\2\2\2\u020c\u020d\5V,\2\u020d9\3\2\2\2\u020e\u0210\7+\2\2\u020f\u0211"+
		"\7\63\2\2\u0210\u020f\3\2\2\2\u0210\u0211\3\2\2\2\u0211\u0212\3\2\2\2"+
		"\u0212\u0213\5V,\2\u0213;\3\2\2\2\u0214\u0216\7/\2\2\u0215\u0217\7\63"+
		"\2\2\u0216\u0215\3\2\2\2\u0216\u0217\3\2\2\2\u0217\u0218\3\2\2\2\u0218"+
		"\u0219\5T+\2\u0219=\3\2\2\2\u021a\u021c\7,\2\2\u021b\u021d\7\63\2\2\u021c"+
		"\u021b\3\2\2\2\u021c\u021d\3\2\2\2\u021d\u021e\3\2\2\2\u021e\u021f\5R"+
		"*\2\u021f\u0220\79\2\2\u0220\u0221\5R*\2\u0221?\3\2\2\2\u0222\u0224\7"+
		"-\2\2\u0223\u0225\7\63\2\2\u0224\u0223\3\2\2\2\u0224\u0225\3\2\2\2\u0225"+
		"\u0226\3\2\2\2\u0226\u0227\5R*\2\u0227\u0228\79\2\2\u0228\u0229\5R*\2"+
		"\u0229A\3\2\2\2\u022a\u022c\7.\2\2\u022b\u022d\7\63\2\2\u022c\u022b\3"+
		"\2\2\2\u022c\u022d\3\2\2\2\u022d\u022e\3\2\2\2\u022e\u022f\5R*\2\u022f"+
		"\u0230\79\2\2\u0230\u0231\5R*\2\u0231C\3\2\2\2\u0232\u0233\7\61\2\2\u0233"+
		"\u0234\7\66\2\2\u0234\u0235\5Z.\2\u0235E\3\2\2\2\u0236\u0237\7\60\2\2"+
		"\u0237\u0238\7\66\2\2\u0238\u0239\5Z.\2\u0239G\3\2\2\2\u023a\u023b\7\60"+
		"\2\2\u023b\u023c\7\16\2\2\u023c\u023d\5X-\2\u023dI\3\2\2\2\u023e\u023f"+
		"\7\67\2\2\u023f\u0241\5\u0104\u0083\2\u0240\u023e\3\2\2\2\u0240\u0241"+
		"\3\2\2\2\u0241\u0247\3\2\2\2\u0242\u0244\5L\'\2\u0243\u0245\5N(\2\u0244"+
		"\u0243\3\2\2\2\u0244\u0245\3\2\2\2\u0245\u0248\3\2\2\2\u0246\u0248\5N"+
		"(\2\u0247\u0242\3\2\2\2\u0247\u0246\3\2\2\2\u0248\u024c\3\2\2\2\u0249"+
		"\u024b\5P)\2\u024a\u0249\3\2\2\2\u024b\u024e\3\2\2\2\u024c\u024a\3\2\2"+
		"\2\u024c\u024d\3\2\2\2\u024d\u024f\3\2\2\2\u024e\u024c\3\2\2\2\u024f\u0250"+
		"\7\16\2\2\u0250\u0251\5d\63\2\u0251K\3\2\2\2\u0252\u0253\7\60\2\2\u0253"+
		"\u0254\5X-\2\u0254M\3\2\2\2\u0255\u0256\7\61\2\2\u0256\u0257\5X-\2\u0257"+
		"O\3\2\2\2\u0258\u025a\7\62\2\2\u0259\u025b\7\r\2\2\u025a\u0259\3\2\2\2"+
		"\u025a\u025b\3\2\2\2\u025b\u025c\3\2\2\2\u025c\u025d\5\u0104\u0083\2\u025d"+
		"Q\3\2\2\2\u025e\u0264\7\64\2\2\u025f\u0261\7\27\2\2\u0260\u025f\3\2\2"+
		"\2\u0260\u0261\3\2\2\2\u0261\u0262\3\2\2\2\u0262\u0264\5\u0104\u0083\2"+
		"\u0263\u025e\3\2\2\2\u0263\u0260\3\2\2\2\u0264S\3\2\2\2\u0265\u0266\7"+
		"\27\2\2\u0266\u0267\5\u0104\u0083\2\u0267U\3\2\2\2\u0268\u026d\5T+\2\u0269"+
		"\u026d\7\64\2\2\u026a\u026d\7\r\2\2\u026b\u026d\7\65\2\2\u026c\u0268\3"+
		"\2\2\2\u026c\u0269\3\2\2\2\u026c\u026a\3\2\2\2\u026c\u026b\3\2\2\2\u026d"+
		"W\3\2\2\2\u026e\u026f\7\u0093\2\2\u026f\u0270\5\\/\2\u0270\u0271\7\u0094"+
		"\2\2\u0271Y\3\2\2\2\u0272\u0273\7\u0093\2\2\u0273\u0274\5\\/\2\u0274\u0275"+
		"\7\u0094\2\2\u0275[\3\2\2\2\u0276\u0278\5b\62\2\u0277\u0276\3\2\2\2\u0277"+
		"\u0278\3\2\2\2\u0278\u027c\3\2\2\2\u0279\u027b\5^\60\2\u027a\u0279\3\2"+
		"\2\2\u027b\u027e\3\2\2\2\u027c\u027a\3\2\2\2\u027c\u027d\3\2\2\2\u027d"+
		"]\3\2\2\2\u027e\u027c\3\2\2\2\u027f\u0281\5`\61\2\u0280\u0282\7\u0098"+
		"\2\2\u0281\u0280\3\2\2\2\u0281\u0282\3\2\2\2\u0282\u0284\3\2\2\2\u0283"+
		"\u0285\5b\62\2\u0284\u0283\3\2\2\2\u0284\u0285\3\2\2\2\u0285_\3\2\2\2"+
		"\u0286\u0287\7\27\2\2\u0287\u0288\5\u00d6l\2\u0288\u028a\7\u0093\2\2\u0289"+
		"\u028b\5b\62\2\u028a\u0289\3\2\2\2\u028a\u028b\3\2\2\2\u028b\u028c\3\2"+
		"\2\2\u028c\u028d\7\u0094\2\2\u028da\3\2\2\2\u028e\u0295\5\u0094K\2\u028f"+
		"\u0291\7\u0098\2\2\u0290\u0292\5\u0094K\2\u0291\u0290\3\2\2\2\u0291\u0292"+
		"\3\2\2\2\u0292\u0294\3\2\2\2\u0293\u028f\3\2\2\2\u0294\u0297\3\2\2\2\u0295"+
		"\u0293\3\2\2\2\u0295\u0296\3\2\2\2\u0296c\3\2\2\2\u0297\u0295\3\2\2\2"+
		"\u0298\u029b\7\u0093\2\2\u0299\u029c\5\f\7\2\u029a\u029c\5f\64\2\u029b"+
		"\u0299\3\2\2\2\u029b\u029a\3\2\2\2\u029c\u029d\3\2\2\2\u029d\u029e\7\u0094"+
		"\2\2\u029ee\3\2\2\2\u029f\u02a1\5j\66\2\u02a0\u029f\3\2\2\2\u02a0\u02a1"+
		"\3\2\2\2\u02a1\u02a5\3\2\2\2\u02a2\u02a4\5h\65\2\u02a3\u02a2\3\2\2\2\u02a4"+
		"\u02a7\3\2\2\2\u02a5\u02a3\3\2\2\2\u02a5\u02a6\3\2\2\2\u02a6g\3\2\2\2"+
		"\u02a7\u02a5\3\2\2\2\u02a8\u02aa\5l\67\2\u02a9\u02ab\7\u0098\2\2\u02aa"+
		"\u02a9\3\2\2\2\u02aa\u02ab\3\2\2\2\u02ab\u02ad\3\2\2\2\u02ac\u02ae\5j"+
		"\66\2\u02ad\u02ac\3\2\2\2\u02ad\u02ae\3\2\2\2\u02aei\3\2\2\2\u02af\u02b6"+
		"\5\u00a0Q\2\u02b0\u02b2\7\u0098\2\2\u02b1\u02b3\5\u00a0Q\2\u02b2\u02b1"+
		"\3\2\2\2\u02b2\u02b3\3\2\2\2\u02b3\u02b5\3\2\2\2\u02b4\u02b0\3\2\2\2\u02b5"+
		"\u02b8\3\2\2\2\u02b6\u02b4\3\2\2\2\u02b6\u02b7\3\2\2\2\u02b7k\3\2\2\2"+
		"\u02b8\u02b6\3\2\2\2\u02b9\u02c2\5\u0084C\2\u02ba\u02c2\5n8\2\u02bb\u02c2"+
		"\5\u0082B\2\u02bc\u02c2\5p9\2\u02bd\u02c2\5r:\2\u02be\u02c2\5\u0086D\2"+
		"\u02bf\u02c2\5t;\2\u02c0\u02c2\5v<\2\u02c1\u02b9\3\2\2\2\u02c1\u02ba\3"+
		"\2\2\2\u02c1\u02bb\3\2\2\2\u02c1\u02bc\3\2\2\2\u02c1\u02bd\3\2\2\2\u02c1"+
		"\u02be\3\2\2\2\u02c1\u02bf\3\2\2\2\u02c1\u02c0\3\2\2\2\u02c2m\3\2\2\2"+
		"\u02c3\u02c4\7\26\2\2\u02c4\u02c5\5d\63\2\u02c5o\3\2\2\2\u02c6\u02c7\7"+
		"\27\2\2\u02c7\u02c8\5\u00d6l\2\u02c8\u02c9\5d\63\2\u02c9q\3\2\2\2\u02ca"+
		"\u02cc\7?\2\2\u02cb\u02cd\7\63\2\2\u02cc\u02cb\3\2\2\2\u02cc\u02cd\3\2"+
		"\2\2\u02cd\u02ce\3\2\2\2\u02ce\u02cf\5\u00d6l\2\u02cf\u02d0\5d\63\2\u02d0"+
		"s\3\2\2\2\u02d1\u02d2\7@\2\2\u02d2\u02d3\7\u0091\2\2\u02d3\u02d4\5\u00de"+
		"p\2\u02d4\u02d5\7:\2\2\u02d5\u02d6\5\u00d8m\2\u02d6\u02d7\7\u0092\2\2"+
		"\u02d7u\3\2\2\2\u02d8\u02d9\7\25\2\2\u02d9\u02da\5x=\2\u02daw\3\2\2\2"+
		"\u02db\u02de\5z>\2\u02dc\u02de\5|?\2\u02dd\u02db\3\2\2\2\u02dd\u02dc\3"+
		"\2\2\2\u02dey\3\2\2\2\u02df\u02e0\5\u00d8m\2\u02e0\u02e4\7\u0093\2\2\u02e1"+
		"\u02e3\5\u0080A\2\u02e2\u02e1\3\2\2\2\u02e3\u02e6\3\2\2\2\u02e4\u02e2"+
		"\3\2\2\2\u02e4\u02e5\3\2\2\2\u02e5\u02e7\3\2\2\2\u02e6\u02e4\3\2\2\2\u02e7"+
		"\u02e8\7\u0094\2\2\u02e8{\3\2\2\2\u02e9\u02ed\7\u0091\2\2\u02ea\u02ec"+
		"\5\u00d8m\2\u02eb\u02ea\3\2\2\2\u02ec\u02ef\3\2\2\2\u02ed\u02eb\3\2\2"+
		"\2\u02ed\u02ee\3\2\2\2\u02ee\u02f0\3\2\2\2\u02ef\u02ed\3\2\2\2\u02f0\u02f1"+
		"\7\u0092\2\2\u02f1\u02f5\7\u0093\2\2\u02f2\u02f4\5~@\2\u02f3\u02f2\3\2"+
		"\2\2\u02f4\u02f7\3\2\2\2\u02f5\u02f3\3\2\2\2\u02f5\u02f6\3\2\2\2\u02f6"+
		"\u02f8\3\2\2\2\u02f7\u02f5\3\2\2\2\u02f8\u02f9\7\u0094\2\2\u02f9}\3\2"+
		"\2\2\u02fa\u02fe\7\u0091\2\2\u02fb\u02fd\5\u0080A\2\u02fc\u02fb\3\2\2"+
		"\2\u02fd\u0300\3\2\2\2\u02fe\u02fc\3\2\2\2\u02fe\u02ff\3\2\2\2\u02ff\u0301"+
		"\3\2\2\2\u0300\u02fe\3\2\2\2\u0301\u0302\7\u0092\2\2\u0302\177\3\2\2\2"+
		"\u0303\u0309\5\u0104\u0083\2\u0304\u0309\5\u00f6|\2\u0305\u0309\5\u00f8"+
		"}\2\u0306\u0309\5\u0100\u0081\2\u0307\u0309\7=\2\2\u0308\u0303\3\2\2\2"+
		"\u0308\u0304\3\2\2\2\u0308\u0305\3\2\2\2\u0308\u0306\3\2\2\2\u0308\u0307"+
		"\3\2\2\2\u0309\u0081\3\2\2\2\u030a\u030b\7A\2\2\u030b\u030c\5d\63\2\u030c"+
		"\u0083\3\2\2\2\u030d\u0312\5d\63\2\u030e\u030f\7\30\2\2\u030f\u0311\5"+
		"d\63\2\u0310\u030e\3\2\2\2\u0311\u0314\3\2\2\2\u0312\u0310\3\2\2\2\u0312"+
		"\u0313\3\2\2\2\u0313\u0085\3\2\2\2\u0314\u0312\3\2\2\2\u0315\u0316\7\31"+
		"\2\2\u0316\u0317\5\u0088E\2\u0317\u0087\3\2\2\2\u0318\u0319\7\u0091\2"+
		"\2\u0319\u031a\5\u00dep\2\u031a\u031b\7\u0092\2\2\u031b\u031f\3\2\2\2"+
		"\u031c\u031f\5\u00e6t\2\u031d\u031f\5\u008aF\2\u031e\u0318\3\2\2\2\u031e"+
		"\u031c\3\2\2\2\u031e\u031d\3\2\2\2\u031f\u0089\3\2\2\2\u0320\u0321\5\u0104"+
		"\u0083\2\u0321\u0322\5\u008cG\2\u0322\u008b\3\2\2\2\u0323\u0329\7\u0091"+
		"\2\2\u0324\u0326\7\7\2\2\u0325\u0324\3\2\2\2\u0325\u0326\3\2\2\2\u0326"+
		"\u0327\3\2\2\2\u0327\u032a\5\u008eH\2\u0328\u032a\3\2\2\2\u0329\u0325"+
		"\3\2\2\2\u0329\u0328\3\2\2\2\u032a\u032b\3\2\2\2\u032b\u032c\7\u0092\2"+
		"\2\u032c\u008d\3\2\2\2\u032d\u0332\5\u00dep\2\u032e\u032f\7\u009d\2\2"+
		"\u032f\u0331\5\u00dep\2\u0330\u032e\3\2\2\2\u0331\u0334\3\2\2\2\u0332"+
		"\u0330\3\2\2\2\u0332\u0333\3\2\2\2\u0333\u008f\3\2\2\2\u0334\u0332\3\2"+
		"\2\2\u0335\u0337\7\u0093\2\2\u0336\u0338\5\u0092J\2\u0337\u0336\3\2\2"+
		"\2\u0337\u0338\3\2\2\2\u0338\u0339\3\2\2\2\u0339\u033a\7\u0094\2\2\u033a"+
		"\u0091\3\2\2\2\u033b\u0342\5\u0094K\2\u033c\u033e\7\u0098\2\2\u033d\u033f"+
		"\5\u0092J\2\u033e\u033d\3\2\2\2\u033e\u033f\3\2\2\2\u033f\u0341\3\2\2"+
		"\2\u0340\u033c\3\2\2\2\u0341\u0344\3\2\2\2\u0342\u0340\3\2\2\2\u0342\u0343"+
		"\3\2\2\2\u0343\u0093\3\2\2\2\u0344\u0342\3\2\2\2\u0345\u0346\5\u00d4k"+
		"\2\u0346\u0347\5\u0098M\2\u0347\u034c\3\2\2\2\u0348\u0349\5\u00c4c\2\u0349"+
		"\u034a\5\u0096L\2\u034a\u034c\3\2\2\2\u034b\u0345\3\2\2\2\u034b\u0348"+
		"\3\2\2\2\u034c\u0095\3\2\2\2\u034d\u034f\5\u0098M\2\u034e\u034d\3\2\2"+
		"\2\u034e\u034f\3\2\2\2\u034f\u0097\3\2\2\2\u0350\u0351\5\u009aN\2\u0351"+
		"\u035a\5\u009cO\2\u0352\u0356\7\u0097\2\2\u0353\u0354\5\u009aN\2\u0354"+
		"\u0355\5\u009cO\2\u0355\u0357\3\2\2\2\u0356\u0353\3\2\2\2\u0356\u0357"+
		"\3\2\2\2\u0357\u0359\3\2\2\2\u0358\u0352\3\2\2\2\u0359\u035c\3\2\2\2\u035a"+
		"\u0358\3\2\2\2\u035a\u035b\3\2\2\2\u035b\u0099\3\2\2\2\u035c\u035a\3\2"+
		"\2\2\u035d\u0360\5\u00d6l\2\u035e\u0360\7\32\2\2\u035f\u035d\3\2\2\2\u035f"+
		"\u035e\3\2\2\2\u0360\u009b\3\2\2\2\u0361\u0366\5\u009eP\2\u0362\u0363"+
		"\7\u009d\2\2\u0363\u0365\5\u009eP\2\u0364\u0362\3\2\2\2\u0365\u0368\3"+
		"\2\2\2\u0366\u0364\3\2\2\2\u0366\u0367\3\2\2\2\u0367\u009d\3\2\2\2\u0368"+
		"\u0366\3\2\2\2\u0369\u036a\5\u00d0i\2\u036a\u009f\3\2\2\2\u036b\u036c"+
		"\5\u00d4k\2\u036c\u036d\5\u00a4S\2\u036d\u0372\3\2\2\2\u036e\u036f\5\u00c8"+
		"e\2\u036f\u0370\5\u00a2R\2\u0370\u0372\3\2\2\2\u0371\u036b\3\2\2\2\u0371"+
		"\u036e\3\2\2\2\u0372\u00a1\3\2\2\2\u0373\u0375\5\u00a4S\2\u0374\u0373"+
		"\3\2\2\2\u0374\u0375\3\2\2\2\u0375\u00a3\3\2\2\2\u0376\u0379\5\u00a8U"+
		"\2\u0377\u0379\5\u00aaV\2\u0378\u0376\3\2\2\2\u0378\u0377\3\2\2\2\u0379"+
		"\u037a\3\2\2\2\u037a\u0381\5\u00acW\2\u037b\u037d\7\u0097\2\2\u037c\u037e"+
		"\5\u00a6T\2\u037d\u037c\3\2\2\2\u037d\u037e\3\2\2\2\u037e\u0380\3\2\2"+
		"\2\u037f\u037b\3\2\2\2\u0380\u0383\3\2\2\2\u0381\u037f\3\2\2\2\u0381\u0382"+
		"\3\2\2\2\u0382\u00a5\3\2\2\2\u0383\u0381\3\2\2\2\u0384\u0387\5\u00a8U"+
		"\2\u0385\u0387\5\u00aaV\2\u0386\u0384\3\2\2\2\u0386\u0385\3\2\2\2\u0387"+
		"\u0388\3\2\2\2\u0388\u0389\5\u00acW\2\u0389\u00a7\3\2\2\2\u038a\u038b"+
		"\5\u00b0Y\2\u038b\u00a9\3\2\2\2\u038c\u038d\5\u00d8m\2\u038d\u00ab\3\2"+
		"\2\2\u038e\u0393\5\u00aeX\2\u038f\u0390\7\u009d\2\2\u0390\u0392\5\u00ae"+
		"X\2\u0391\u038f\3\2\2\2\u0392\u0395\3\2\2\2\u0393\u0391\3\2\2\2\u0393"+
		"\u0394\3\2\2\2\u0394\u00ad\3\2\2\2\u0395\u0393\3\2\2\2\u0396\u0397\5\u00d2"+
		"j\2\u0397\u00af\3\2\2\2\u0398\u0399\5\u00b2Z\2\u0399\u00b1\3\2\2\2\u039a"+
		"\u039f\5\u00b4[\2\u039b\u039c\7\u00a3\2\2\u039c\u039e\5\u00b4[\2\u039d"+
		"\u039b\3\2\2\2\u039e\u03a1\3\2\2\2\u039f\u039d\3\2\2\2\u039f\u03a0\3\2"+
		"\2\2\u03a0\u00b3\3\2\2\2\u03a1\u039f\3\2\2\2\u03a2\u03a7\5\u00b8]\2\u03a3"+
		"\u03a4\7\u009f\2\2\u03a4\u03a6\5\u00b8]\2\u03a5\u03a3\3\2\2\2\u03a6\u03a9"+
		"\3\2\2\2\u03a7\u03a5\3\2\2\2\u03a7\u03a8\3\2\2\2\u03a8\u00b5\3\2\2\2\u03a9"+
		"\u03a7\3\2\2\2\u03aa\u03ac\5\u00bc_\2\u03ab\u03ad\5\u00ba^\2\u03ac\u03ab"+
		"\3\2\2\2\u03ac\u03ad\3\2\2\2\u03ad\u00b7\3\2\2\2\u03ae\u03b0\7\u0090\2"+
		"\2\u03af\u03ae\3\2\2\2\u03af\u03b0\3\2\2\2\u03b0\u03b1\3\2\2\2\u03b1\u03b2"+
		"\5\u00b6\\\2\u03b2\u00b9\3\2\2\2\u03b3\u03b4\t\4\2\2\u03b4\u00bb\3\2\2"+
		"\2\u03b5\u03be\5\u0104\u0083\2\u03b6\u03be\7\32\2\2\u03b7\u03b8\7\u009e"+
		"\2\2\u03b8\u03be\5\u00be`\2\u03b9\u03ba\7\u0091\2\2\u03ba\u03bb\5\u00b0"+
		"Y\2\u03bb\u03bc\7\u0092\2\2\u03bc\u03be\3\2\2\2\u03bd\u03b5\3\2\2\2\u03bd"+
		"\u03b6\3\2\2\2\u03bd\u03b7\3\2\2\2\u03bd\u03b9\3\2\2\2\u03be\u00bd\3\2"+
		"\2\2\u03bf\u03cd\5\u00c0a\2\u03c0\u03c9\7\u0091\2\2\u03c1\u03c6\5\u00c0"+
		"a\2\u03c2\u03c3\7\u00a3\2\2\u03c3\u03c5\5\u00c0a\2\u03c4\u03c2\3\2\2\2"+
		"\u03c5\u03c8\3\2\2\2\u03c6\u03c4\3\2\2\2\u03c6\u03c7\3\2\2\2\u03c7\u03ca"+
		"\3\2\2\2\u03c8\u03c6\3\2\2\2\u03c9\u03c1\3\2\2\2\u03c9\u03ca\3\2\2\2\u03ca"+
		"\u03cb\3\2\2\2\u03cb\u03cd\7\u0092\2\2\u03cc\u03bf\3\2\2\2\u03cc\u03c0"+
		"\3\2\2\2\u03cd\u00bf\3\2\2\2\u03ce\u03d0\7\u0090\2\2\u03cf\u03ce\3\2\2"+
		"\2\u03cf\u03d0\3\2\2\2\u03d0\u03d3\3\2\2\2\u03d1\u03d4\5\u0104\u0083\2"+
		"\u03d2\u03d4\7\32\2\2\u03d3\u03d1\3\2\2\2\u03d3\u03d2\3\2\2\2\u03d4\u00c1"+
		"\3\2\2\2\u03d5\u03d6\7|\2\2\u03d6\u00c3\3\2\2\2\u03d7\u03da\5\u00ccg\2"+
		"\u03d8\u03da\5\u00c6d\2\u03d9\u03d7\3\2\2\2\u03d9\u03d8\3\2\2\2\u03da"+
		"\u00c5\3\2\2\2\u03db\u03dc\7\u0095\2\2\u03dc\u03dd\5\u0098M\2\u03dd\u03de"+
		"\7\u0096\2\2\u03de\u00c7\3\2\2\2\u03df\u03e2\5\u00ceh\2\u03e0\u03e2\5"+
		"\u00caf\2\u03e1\u03df\3\2\2\2\u03e1\u03e0\3\2\2\2\u03e2\u00c9\3\2\2\2"+
		"\u03e3\u03e4\7\u0095\2\2\u03e4\u03e5\5\u00a4S\2\u03e5\u03e6\7\u0096\2"+
		"\2\u03e6\u00cb\3\2\2\2\u03e7\u03e9\7\u0091\2\2\u03e8\u03ea\5\u00d0i\2"+
		"\u03e9\u03e8\3\2\2\2\u03ea\u03eb\3\2\2\2\u03eb\u03e9\3\2\2\2\u03eb\u03ec"+
		"\3\2\2\2\u03ec\u03ed\3\2\2\2\u03ed\u03ee\7\u0092\2\2\u03ee\u00cd\3\2\2"+
		"\2\u03ef\u03f1\7\u0091\2\2\u03f0\u03f2\5\u00d2j\2\u03f1\u03f0\3\2\2\2"+
		"\u03f2\u03f3\3\2\2\2\u03f3\u03f1\3\2\2\2\u03f3\u03f4\3\2\2\2\u03f4\u03f5"+
		"\3\2\2\2\u03f5\u03f6\7\u0092\2\2\u03f6\u00cf\3\2\2\2\u03f7\u03fa\5\u00d4"+
		"k\2\u03f8\u03fa\5\u00c4c\2\u03f9\u03f7\3\2\2\2\u03f9\u03f8\3\2\2\2\u03fa"+
		"\u00d1\3\2\2\2\u03fb\u03fe\5\u00d4k\2\u03fc\u03fe\5\u00c8e\2\u03fd\u03fb"+
		"\3\2\2\2\u03fd\u03fc\3\2\2\2\u03fe\u00d3\3\2\2\2\u03ff\u0402\5\u00d8m"+
		"\2\u0400\u0402\5\u00dan\2\u0401\u03ff\3\2\2\2\u0401\u0400\3\2\2\2\u0402"+
		"\u00d5\3\2\2\2\u0403\u0406\5\u00d8m\2\u0404\u0406\5\u0104\u0083\2\u0405"+
		"\u0403\3\2\2\2\u0405\u0404\3\2\2\2\u0406\u00d7\3\2\2\2\u0407\u0408\t\5"+
		"\2\2\u0408\u00d9\3\2\2\2\u0409\u0410\5\u0104\u0083\2\u040a\u0410\5\u00f6"+
		"|\2\u040b\u0410\5\u00f8}\2\u040c\u0410\5\u0100\u0081\2\u040d\u0410\5\u0108"+
		"\u0085\2\u040e\u0410\5\u00dco\2\u040f\u0409\3\2\2\2\u040f\u040a\3\2\2"+
		"\2\u040f\u040b\3\2\2\2\u040f\u040c\3\2\2\2\u040f\u040d\3\2\2\2\u040f\u040e"+
		"\3\2\2\2\u0410\u00db\3\2\2\2\u0411\u0412\7\u0091\2\2\u0412\u0413\7\u0092"+
		"\2\2\u0413\u00dd\3\2\2\2\u0414\u0415\bp\1\2\u0415\u0416\t\6\2\2\u0416"+
		"\u041b\5\u00dep\13\u0417\u0418\7\u009e\2\2\u0418\u041b\5\u00dep\n\u0419"+
		"\u041b\5\u00e4s\2\u041a\u0414\3\2\2\2\u041a\u0417\3\2\2\2\u041a\u0419"+
		"\3\2\2\2\u041b\u0439\3\2\2\2\u041c\u041d\f\t\2\2\u041d\u041e\t\7\2\2\u041e"+
		"\u0438\5\u00dep\n\u041f\u0420\f\b\2\2\u0420\u0421\t\6\2\2\u0421\u0438"+
		"\5\u00dep\t\u0422\u0423\f\5\2\2\u0423\u0424\t\b\2\2\u0424\u0438\5\u00de"+
		"p\6\u0425\u0426\f\7\2\2\u0426\u0438\5\u00e0q\2\u0427\u0429\f\6\2\2\u0428"+
		"\u042a\7q\2\2\u0429\u0428\3\2\2\2\u0429\u042a\3\2\2\2\u042a\u042b\3\2"+
		"\2\2\u042b\u042c\7r\2\2\u042c\u042e\7\u0091\2\2\u042d\u042f\5\u008eH\2"+
		"\u042e\u042d\3\2\2\2\u042e\u042f\3\2\2\2\u042f\u0430\3\2\2\2\u0430\u0438"+
		"\7\u0092\2\2\u0431\u0432\f\4\2\2\u0432\u0433\7\u008e\2\2\u0433\u0438\5"+
		"\u00dep\2\u0434\u0435\f\3\2\2\u0435\u0436\7\u008f\2\2\u0436\u0438\5\u00de"+
		"p\2\u0437\u041c\3\2\2\2\u0437\u041f\3\2\2\2\u0437\u0422\3\2\2\2\u0437"+
		"\u0425\3\2\2\2\u0437\u0427\3\2\2\2\u0437\u0431\3\2\2\2\u0437\u0434\3\2"+
		"\2\2\u0438\u043b\3\2\2\2\u0439\u0437\3\2\2\2\u0439\u043a\3\2\2\2\u043a"+
		"\u00df\3\2\2\2\u043b\u0439\3\2\2\2\u043c\u043f\5\u00fc\177\2\u043d\u043f"+
		"\5\u00fe\u0080\2\u043e\u043c\3\2\2\2\u043e\u043d\3\2\2\2\u043f\u0442\3"+
		"\2\2\2\u0440\u0441\t\7\2\2\u0441\u0443\5\u00e2r\2\u0442\u0440\3\2\2\2"+
		"\u0442\u0443\3\2\2\2\u0443\u00e1\3\2\2\2\u0444\u0446\t\t\2\2\u0445\u0444"+
		"\3\2\2\2\u0445\u0446\3\2\2\2\u0446\u0447\3\2\2\2\u0447\u0448\5\u00e4s"+
		"\2\u0448\u00e3\3\2\2\2\u0449\u044a\7\u0091\2\2\u044a\u044b\5\u00dep\2"+
		"\u044b\u044c\7\u0092\2\2\u044c\u0454\3\2\2\2\u044d\u0454\5\u00e6t\2\u044e"+
		"\u0454\5\u00f4{\2\u044f\u0454\5\u00f6|\2\u0450\u0454\5\u00f8}\2\u0451"+
		"\u0454\5\u0100\u0081\2\u0452\u0454\5\u00d8m\2\u0453\u0449\3\2\2\2\u0453"+
		"\u044d\3\2\2\2\u0453\u044e\3\2\2\2\u0453\u044f\3\2\2\2\u0453\u0450\3\2"+
		"\2\2\u0453\u0451\3\2\2\2\u0453\u0452\3\2\2\2\u0454\u00e5\3\2\2\2\u0455"+
		"\u0562\5\u00f2z\2\u0456\u0457\7\33\2\2\u0457\u0458\7\u0091\2\2\u0458\u0459"+
		"\5\u00dep\2\u0459\u045a\7\u0092\2\2\u045a\u0562\3\2\2\2\u045b\u045c\7"+
		"\34\2\2\u045c\u045d\7\u0091\2\2\u045d\u045e\5\u00dep\2\u045e\u045f\7\u0092"+
		"\2\2\u045f\u0562\3\2\2\2\u0460\u0461\7\35\2\2\u0461\u0462\7\u0091\2\2"+
		"\u0462\u0463\5\u00dep\2\u0463\u0464\7\u009d\2\2\u0464\u0465\5\u00dep\2"+
		"\u0465\u0466\7\u0092\2\2\u0466\u0562\3\2\2\2\u0467\u0468\7\36\2\2\u0468"+
		"\u0469\7\u0091\2\2\u0469\u046a\5\u00dep\2\u046a\u046b\7\u0092\2\2\u046b"+
		"\u0562\3\2\2\2\u046c\u046d\7\37\2\2\u046d\u046e\7\u0091\2\2\u046e\u046f"+
		"\5\u00d8m\2\u046f\u0470\7\u0092\2\2\u0470\u0562\3\2\2\2\u0471\u0472\7"+
		"B\2\2\u0472\u0473\7\u0091\2\2\u0473\u0474\5\u00dep\2\u0474\u0475\7\u0092"+
		"\2\2\u0475\u0562\3\2\2\2\u0476\u0477\7C\2\2\u0477\u0478\7\u0091\2\2\u0478"+
		"\u0479\5\u00dep\2\u0479\u047a\7\u0092\2\2\u047a\u0562\3\2\2\2\u047b\u047c"+
		"\7D\2\2\u047c\u047e\7\u0091\2\2\u047d\u047f\5\u00dep\2\u047e\u047d\3\2"+
		"\2\2\u047e\u047f\3\2\2\2\u047f\u0480\3\2\2\2\u0480\u0562\7\u0092\2\2\u0481"+
		"\u0482\7E\2\2\u0482\u0483\7\u0091\2\2\u0483\u0562\7\u0092\2\2\u0484\u0485"+
		"\7F\2\2\u0485\u0486\7\u0091\2\2\u0486\u0487\5\u00dep\2\u0487\u0488\7\u0092"+
		"\2\2\u0488\u0562\3\2\2\2\u0489\u048a\7G\2\2\u048a\u048b\7\u0091\2\2\u048b"+
		"\u048c\5\u00dep\2\u048c\u048d\7\u0092\2\2\u048d\u0562\3\2\2\2\u048e\u048f"+
		"\7H\2\2\u048f\u0490\7\u0091\2\2\u0490\u0491\5\u00dep\2\u0491\u0492\7\u0092"+
		"\2\2\u0492\u0562\3\2\2\2\u0493\u0494\7I\2\2\u0494\u0495\7\u0091\2\2\u0495"+
		"\u0496\5\u00dep\2\u0496\u0497\7\u0092\2\2\u0497\u0562\3\2\2\2\u0498\u0499"+
		"\7J\2\2\u0499\u049b\7\u0091\2\2\u049a\u049c\5\u008eH\2\u049b\u049a\3\2"+
		"\2\2\u049b\u049c\3\2\2\2\u049c\u049d\3\2\2\2\u049d\u0562\7\u0092\2\2\u049e"+
		"\u0562\5\u00eav\2\u049f\u04a0\7K\2\2\u04a0\u04a1\7\u0091\2\2\u04a1\u04a2"+
		"\5\u00dep\2\u04a2\u04a3\7\u0092\2\2\u04a3\u0562\3\2\2\2\u04a4\u0562\5"+
		"\u00ecw\2\u04a5\u04a6\7L\2\2\u04a6\u04a7\7\u0091\2\2\u04a7\u04a8\5\u00de"+
		"p\2\u04a8\u04a9\7\u0092\2\2\u04a9\u0562\3\2\2\2\u04aa\u04ab\7M\2\2\u04ab"+
		"\u04ac\7\u0091\2\2\u04ac\u04ad\5\u00dep\2\u04ad\u04ae\7\u0092\2\2\u04ae"+
		"\u0562\3\2\2\2\u04af\u04b0\7N\2\2\u04b0\u04b1\7\u0091\2\2\u04b1\u04b2"+
		"\5\u00dep\2\u04b2\u04b3\7\u0092\2\2\u04b3\u0562\3\2\2\2\u04b4\u04b5\7"+
		"O\2\2\u04b5\u04b6\7\u0091\2\2\u04b6\u04b7\5\u00dep\2\u04b7\u04b8\7\u009d"+
		"\2\2\u04b8\u04b9\5\u00dep\2\u04b9\u04ba\7\u0092\2\2\u04ba\u0562\3\2\2"+
		"\2\u04bb\u04bc\7P\2\2\u04bc\u04bd\7\u0091\2\2\u04bd\u04be\5\u00dep\2\u04be"+
		"\u04bf\7\u009d\2\2\u04bf\u04c0\5\u00dep\2\u04c0\u04c1\7\u0092\2\2\u04c1"+
		"\u0562\3\2\2\2\u04c2\u04c3\7Q\2\2\u04c3\u04c4\7\u0091\2\2\u04c4\u04c5"+
		"\5\u00dep\2\u04c5\u04c6\7\u009d\2\2\u04c6\u04c7\5\u00dep\2\u04c7\u04c8"+
		"\7\u0092\2\2\u04c8\u0562\3\2\2\2\u04c9\u04ca\7R\2\2\u04ca\u04cb\7\u0091"+
		"\2\2\u04cb\u04cc\5\u00dep\2\u04cc\u04cd\7\u009d\2\2\u04cd\u04ce\5\u00de"+
		"p\2\u04ce\u04cf\7\u0092\2\2\u04cf\u0562\3\2\2\2\u04d0\u04d1\7S\2\2\u04d1"+
		"\u04d2\7\u0091\2\2\u04d2\u04d3\5\u00dep\2\u04d3\u04d4\7\u009d\2\2\u04d4"+
		"\u04d5\5\u00dep\2\u04d5\u04d6\7\u0092\2\2\u04d6\u0562\3\2\2\2\u04d7\u04d8"+
		"\7U\2\2\u04d8\u04d9\7\u0091\2\2\u04d9\u04da\5\u00dep\2\u04da\u04db\7\u0092"+
		"\2\2\u04db\u0562\3\2\2\2\u04dc\u04dd\7V\2\2\u04dd\u04de\7\u0091\2\2\u04de"+
		"\u04df\5\u00dep\2\u04df\u04e0\7\u0092\2\2\u04e0\u0562\3\2\2\2\u04e1\u04e2"+
		"\7W\2\2\u04e2\u04e3\7\u0091\2\2\u04e3\u04e4\5\u00dep\2\u04e4\u04e5\7\u0092"+
		"\2\2\u04e5\u0562\3\2\2\2\u04e6\u04e7\7X\2\2\u04e7\u04e8\7\u0091\2\2\u04e8"+
		"\u04e9\5\u00dep\2\u04e9\u04ea\7\u0092\2\2\u04ea\u0562\3\2\2\2\u04eb\u04ec"+
		"\7Y\2\2\u04ec\u04ed\7\u0091\2\2\u04ed\u04ee\5\u00dep\2\u04ee\u04ef\7\u0092"+
		"\2\2\u04ef\u0562\3\2\2\2\u04f0\u04f1\7Z\2\2\u04f1\u04f2\7\u0091\2\2\u04f2"+
		"\u04f3\5\u00dep\2\u04f3\u04f4\7\u0092\2\2\u04f4\u0562\3\2\2\2\u04f5\u04f6"+
		"\7[\2\2\u04f6\u04f7\7\u0091\2\2\u04f7\u04f8\5\u00dep\2\u04f8\u04f9\7\u0092"+
		"\2\2\u04f9\u0562\3\2\2\2\u04fa\u04fb\7\\\2\2\u04fb\u04fc\7\u0091\2\2\u04fc"+
		"\u04fd\5\u00dep\2\u04fd\u04fe\7\u0092\2\2\u04fe\u0562\3\2\2\2\u04ff\u0500"+
		"\7]\2\2\u0500\u0501\7\u0091\2\2\u0501\u0562\7\u0092\2\2\u0502\u0503\7"+
		"^\2\2\u0503\u0504\7\u0091\2\2\u0504\u0562\7\u0092\2\2\u0505\u0506\7_\2"+
		"\2\u0506\u0507\7\u0091\2\2\u0507\u0562\7\u0092\2\2\u0508\u0509\7`\2\2"+
		"\u0509\u050a\7\u0091\2\2\u050a\u050b\5\u00dep\2\u050b\u050c\7\u0092\2"+
		"\2\u050c\u0562\3\2\2\2\u050d\u050e\7a\2\2\u050e\u050f\7\u0091\2\2\u050f"+
		"\u0510\5\u00dep\2\u0510\u0511\7\u0092\2\2\u0511\u0562\3\2\2\2\u0512\u0513"+
		"\7b\2\2\u0513\u0514\7\u0091\2\2\u0514\u0515\5\u00dep\2\u0515\u0516\7\u0092"+
		"\2\2\u0516\u0562\3\2\2\2\u0517\u0518\7c\2\2\u0518\u0519\7\u0091\2\2\u0519"+
		"\u051a\5\u00dep\2\u051a\u051b\7\u0092\2\2\u051b\u0562\3\2\2\2\u051c\u051d"+
		"\7d\2\2\u051d\u051e\7\u0091\2\2\u051e\u051f\5\u00dep\2\u051f\u0520\7\u0092"+
		"\2\2\u0520\u0562\3\2\2\2\u0521\u0522\7e\2\2\u0522\u0524\7\u0091\2\2\u0523"+
		"\u0525\5\u008eH\2\u0524\u0523\3\2\2\2\u0524\u0525\3\2\2\2\u0525\u0526"+
		"\3\2\2\2\u0526\u0562\7\u0092\2\2\u0527\u0528\7f\2\2\u0528\u0529\7\u0091"+
		"\2\2\u0529\u052a\5\u00dep\2\u052a\u052b\7\u009d\2\2\u052b\u052c\5\u00de"+
		"p\2\u052c\u052d\7\u009d\2\2\u052d\u052e\5\u00dep\2\u052e\u052f\7\u0092"+
		"\2\2\u052f\u0562\3\2\2\2\u0530\u0531\7g\2\2\u0531\u0532\7\u0091\2\2\u0532"+
		"\u0533\5\u00dep\2\u0533\u0534\7\u009d\2\2\u0534\u0535\5\u00dep\2\u0535"+
		"\u0536\7\u0092\2\2\u0536\u0562\3\2\2\2\u0537\u0538\7h\2\2\u0538\u0539"+
		"\7\u0091\2\2\u0539\u053a\5\u00dep\2\u053a\u053b\7\u009d\2\2\u053b\u053c"+
		"\5\u00dep\2\u053c\u053d\7\u0092\2\2\u053d\u0562\3\2\2\2\u053e\u053f\7"+
		" \2\2\u053f\u0540\7\u0091\2\2\u0540\u0541\5\u00dep\2\u0541\u0542\7\u009d"+
		"\2\2\u0542\u0543\5\u00dep\2\u0543\u0544\7\u0092\2\2\u0544\u0562\3\2\2"+
		"\2\u0545\u0546\7!\2\2\u0546\u0547\7\u0091\2\2\u0547\u0548\5\u00dep\2\u0548"+
		"\u0549\7\u0092\2\2\u0549\u0562\3\2\2\2\u054a\u054b\7\"\2\2\u054b\u054c"+
		"\7\u0091\2\2\u054c\u054d\5\u00dep\2\u054d\u054e\7\u0092\2\2\u054e\u0562"+
		"\3\2\2\2\u054f\u0550\7#\2\2\u0550\u0551\7\u0091\2\2\u0551\u0552\5\u00de"+
		"p\2\u0552\u0553\7\u0092\2\2\u0553\u0562\3\2\2\2\u0554\u0555\7$\2\2\u0555"+
		"\u0556\7\u0091\2\2\u0556\u0557\5\u00dep\2\u0557\u0558\7\u0092\2\2\u0558"+
		"\u0562\3\2\2\2\u0559\u055a\7i\2\2\u055a\u055b\7\u0091\2\2\u055b\u055c"+
		"\5\u00dep\2\u055c\u055d\7\u0092\2\2\u055d\u0562\3\2\2\2\u055e\u0562\5"+
		"\u00e8u\2\u055f\u0562\5\u00eex\2\u0560\u0562\5\u00f0y\2\u0561\u0455\3"+
		"\2\2\2\u0561\u0456\3\2\2\2\u0561\u045b\3\2\2\2\u0561\u0460\3\2\2\2\u0561"+
		"\u0467\3\2\2\2\u0561\u046c\3\2\2\2\u0561\u0471\3\2\2\2\u0561\u0476\3\2"+
		"\2\2\u0561\u047b\3\2\2\2\u0561\u0481\3\2\2\2\u0561\u0484\3\2\2\2\u0561"+
		"\u0489\3\2\2\2\u0561\u048e\3\2\2\2\u0561\u0493\3\2\2\2\u0561\u0498\3\2"+
		"\2\2\u0561\u049e\3\2\2\2\u0561\u049f\3\2\2\2\u0561\u04a4\3\2\2\2\u0561"+
		"\u04a5\3\2\2\2\u0561\u04aa\3\2\2\2\u0561\u04af\3\2\2\2\u0561\u04b4\3\2"+
		"\2\2\u0561\u04bb\3\2\2\2\u0561\u04c2\3\2\2\2\u0561\u04c9\3\2\2\2\u0561"+
		"\u04d0\3\2\2\2\u0561\u04d7\3\2\2\2\u0561\u04dc\3\2\2\2\u0561\u04e1\3\2"+
		"\2\2\u0561\u04e6\3\2\2\2\u0561\u04eb\3\2\2\2\u0561\u04f0\3\2\2\2\u0561"+
		"\u04f5\3\2\2\2\u0561\u04fa\3\2\2\2\u0561\u04ff\3\2\2\2\u0561\u0502\3\2"+
		"\2\2\u0561\u0505\3\2\2\2\u0561\u0508\3\2\2\2\u0561\u050d\3\2\2\2\u0561"+
		"\u0512\3\2\2\2\u0561\u0517\3\2\2\2\u0561\u051c\3\2\2\2\u0561\u0521\3\2"+
		"\2\2\u0561\u0527\3\2\2\2\u0561\u0530\3\2\2\2\u0561\u0537\3\2\2\2\u0561"+
		"\u053e\3\2\2\2\u0561\u0545\3\2\2\2\u0561\u054a\3\2\2\2\u0561\u054f\3\2"+
		"\2\2\u0561\u0554\3\2\2\2\u0561\u0559\3\2\2\2\u0561\u055e\3\2\2\2\u0561"+
		"\u055f\3\2\2\2\u0561\u0560\3\2\2\2\u0562\u00e7\3\2\2\2\u0563\u0564\7%"+
		"\2\2\u0564\u0565\7\u0091\2\2\u0565\u0566\5\u00dep\2\u0566\u0567\7\u009d"+
		"\2\2\u0567\u056a\5\u00dep\2\u0568\u0569\7\u009d\2\2\u0569\u056b\5\u00de"+
		"p\2\u056a\u0568\3\2\2\2\u056a\u056b\3\2\2\2\u056b\u056c\3\2\2\2\u056c"+
		"\u056d\7\u0092\2\2\u056d\u00e9\3\2\2\2\u056e\u056f\7&\2\2\u056f\u0570"+
		"\7\u0091\2\2\u0570\u0571\5\u00dep\2\u0571\u0572\7\u009d\2\2\u0572\u0575"+
		"\5\u00dep\2\u0573\u0574\7\u009d\2\2\u0574\u0576\5\u00dep\2\u0575\u0573"+
		"\3\2\2\2\u0575\u0576\3\2\2\2\u0576\u0577\3\2\2\2\u0577\u0578\7\u0092\2"+
		"\2\u0578\u00eb\3\2\2\2\u0579\u057a\7T\2\2\u057a\u057b\7\u0091\2\2\u057b"+
		"\u057c\5\u00dep\2\u057c\u057d\7\u009d\2\2\u057d\u057e\5\u00dep\2\u057e"+
		"\u057f\7\u009d\2\2\u057f\u0582\5\u00dep\2\u0580\u0581\7\u009d\2\2\u0581"+
		"\u0583\5\u00dep\2\u0582\u0580\3\2\2\2\u0582\u0583\3\2\2\2\u0583\u0584"+
		"\3\2\2\2\u0584\u0585\7\u0092\2\2\u0585\u00ed\3\2\2\2\u0586\u0587\7s\2"+
		"\2\u0587\u0588\5d\63\2\u0588\u00ef\3\2\2\2\u0589\u058a\7q\2\2\u058a\u058b"+
		"\7s\2\2\u058b\u058c\5d\63\2\u058c\u00f1\3\2\2\2\u058d\u058e\7j\2\2\u058e"+
		"\u0590\7\u0091\2\2\u058f\u0591\7\7\2\2\u0590\u058f\3\2\2\2\u0590\u0591"+
		"\3\2\2\2\u0591\u0594\3\2\2\2\u0592\u0595\7\u009b\2\2\u0593\u0595\5\u00de"+
		"p\2\u0594\u0592\3\2\2\2\u0594\u0593\3\2\2\2\u0595\u0596\3\2\2\2\u0596"+
		"\u05ce\7\u0092\2\2\u0597\u0598\7k\2\2\u0598\u059a\7\u0091\2\2\u0599\u059b"+
		"\7\7\2\2\u059a\u0599\3\2\2\2\u059a\u059b\3\2\2\2\u059b\u059c\3\2\2\2\u059c"+
		"\u059d\5\u00dep\2\u059d\u059e\7\u0092\2\2\u059e\u05ce\3\2\2\2\u059f\u05a0"+
		"\7l\2\2\u05a0\u05a2\7\u0091\2\2\u05a1\u05a3\7\7\2\2\u05a2\u05a1\3\2\2"+
		"\2\u05a2\u05a3\3\2\2\2\u05a3\u05a4\3\2\2\2\u05a4\u05a5\5\u00dep\2\u05a5"+
		"\u05a6\7\u0092\2\2\u05a6\u05ce\3\2\2\2\u05a7\u05a8\7m\2\2\u05a8\u05aa"+
		"\7\u0091\2\2\u05a9\u05ab\7\7\2\2\u05aa\u05a9\3\2\2\2\u05aa\u05ab\3\2\2"+
		"\2\u05ab\u05ac\3\2\2\2\u05ac\u05ad\5\u00dep\2\u05ad\u05ae\7\u0092\2\2"+
		"\u05ae\u05ce\3\2\2\2\u05af\u05b0\7n\2\2\u05b0\u05b2\7\u0091\2\2\u05b1"+
		"\u05b3\7\7\2\2\u05b2\u05b1\3\2\2\2\u05b2\u05b3\3\2\2\2\u05b3\u05b4\3\2"+
		"\2\2\u05b4\u05b5\5\u00dep\2\u05b5\u05b6\7\u0092\2\2\u05b6\u05ce\3\2\2"+
		"\2\u05b7\u05b8\7o\2\2\u05b8\u05ba\7\u0091\2\2\u05b9\u05bb\7\7\2\2\u05ba"+
		"\u05b9\3\2\2\2\u05ba\u05bb\3\2\2\2\u05bb\u05bc\3\2\2\2\u05bc\u05bd\5\u00de"+
		"p\2\u05bd\u05be\7\u0092\2\2\u05be\u05ce\3\2\2\2\u05bf\u05c0\7p\2\2\u05c0"+
		"\u05c2\7\u0091\2\2\u05c1\u05c3\7\7\2\2\u05c2\u05c1\3\2\2\2\u05c2\u05c3"+
		"\3\2\2\2\u05c3\u05c4\3\2\2\2\u05c4\u05c9\5\u00dep\2\u05c5\u05c6\7\u0097"+
		"\2\2\u05c6\u05c7\7t\2\2\u05c7\u05c8\7\u00a0\2\2\u05c8\u05ca\5\u0102\u0082"+
		"\2\u05c9\u05c5\3\2\2\2\u05c9\u05ca\3\2\2\2\u05ca\u05cb\3\2\2\2\u05cb\u05cc"+
		"\7\u0092\2\2\u05cc\u05ce\3\2\2\2\u05cd\u058d\3\2\2\2\u05cd\u0597\3\2\2"+
		"\2\u05cd\u059f\3\2\2\2\u05cd\u05a7\3\2\2\2\u05cd\u05af\3\2\2\2\u05cd\u05b7"+
		"\3\2\2\2\u05cd\u05bf\3\2\2\2\u05ce\u00f3\3\2\2\2\u05cf\u05d1\5\u0104\u0083"+
		"\2\u05d0\u05d2\5\u008cG\2\u05d1\u05d0\3\2\2\2\u05d1\u05d2\3\2\2\2\u05d2"+
		"\u00f5\3\2\2\2\u05d3\u05d7\5\u0102\u0082\2\u05d4\u05d8\7{\2\2\u05d5\u05d6"+
		"\7\u008a\2\2\u05d6\u05d8\5\u0104\u0083\2\u05d7\u05d4\3\2\2\2\u05d7\u05d5"+
		"\3\2\2\2\u05d7\u05d8\3\2\2\2\u05d8\u00f7\3\2\2\2\u05d9\u05dd\5\u00fa~"+
		"\2\u05da\u05dd\5\u00fc\177\2\u05db\u05dd\5\u00fe\u0080\2\u05dc\u05d9\3"+
		"\2\2\2\u05dc\u05da\3\2\2\2\u05dc\u05db\3\2\2\2\u05dd\u00f9\3\2\2\2\u05de"+
		"\u05df\t\n\2\2\u05df\u00fb\3\2\2\2\u05e0\u05e1\t\13\2\2\u05e1\u00fd\3"+
		"\2\2\2\u05e2\u05e3\t\f\2\2\u05e3\u00ff\3\2\2\2\u05e4\u05e5\t\r\2\2\u05e5"+
		"\u0101\3\2\2\2\u05e6\u05e7\t\16\2\2\u05e7\u0103\3\2\2\2\u05e8\u05eb\7"+
		"u\2\2\u05e9\u05eb\5\u0106\u0084\2\u05ea\u05e8\3\2\2\2\u05ea\u05e9\3\2"+
		"\2\2\u05eb\u0105\3\2\2\2\u05ec\u05ed\t\17\2\2\u05ed\u0107\3\2\2\2\u05ee"+
		"\u05f1\7x\2\2\u05ef\u05f1\5\u010a\u0086\2\u05f0\u05ee\3\2\2\2\u05f0\u05ef"+
		"\3\2\2\2\u05f1\u0109\3\2\2\2\u05f2\u05f3\7\u0095\2\2\u05f3\u05f4\7\u0096"+
		"\2\2\u05f4\u010b\3\2\2\2\u0098\u0111\u0117\u011a\u011e\u0120\u012e\u013b"+
		"\u0140\u0143\u014c\u0153\u015c\u0162\u0166\u016c\u016f\u0174\u0178\u0180"+
		"\u0185\u0188\u018c\u0191\u0196\u0199\u019c\u019f\u01a6\u01ae\u01b3\u01b9"+
		"\u01c2\u01cb\u01cf\u01d3\u01d5\u01df\u01e9\u01ee\u01f0\u01fd\u0201\u0206"+
		"\u020a\u0210\u0216\u021c\u0224\u022c\u0240\u0244\u0247\u024c\u025a\u0260"+
		"\u0263\u026c\u0277\u027c\u0281\u0284\u028a\u0291\u0295\u029b\u02a0\u02a5"+
		"\u02aa\u02ad\u02b2\u02b6\u02c1\u02cc\u02dd\u02e4\u02ed\u02f5\u02fe\u0308"+
		"\u0312\u031e\u0325\u0329\u0332\u0337\u033e\u0342\u034b\u034e\u0356\u035a"+
		"\u035f\u0366\u0371\u0374\u0378\u037d\u0381\u0386\u0393\u039f\u03a7\u03ac"+
		"\u03af\u03bd\u03c6\u03c9\u03cc\u03cf\u03d3\u03d9\u03e1\u03eb\u03f3\u03f9"+
		"\u03fd\u0401\u0405\u040f\u041a\u0429\u042e\u0437\u0439\u043e\u0442\u0445"+
		"\u0453\u047e\u049b\u0524\u0561\u056a\u0575\u0582\u0590\u0594\u059a\u05a2"+
		"\u05aa\u05b2\u05ba\u05c2\u05c9\u05cd\u05d1\u05d7\u05dc\u05ea\u05f0";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}