package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.BOOLEAN;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATETIME;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DAYTIMEDURATION;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DECIMAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DOUBLE;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.FLOAT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.INT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.INTEGER;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LANGSTRING;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LONG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.RDFTERM;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.SHORT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.STRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.rdfLangStringIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDayTimeDurationIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDecimalIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdLongIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdShortIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.util.Arrays;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class BuiltinClasses
{
    public static final LiteralClass xsdBoolean = new SimpleLiteralClass(BOOLEAN, "bool", xsdBooleanIri);
    public static final LiteralClass xsdShort = new SimpleLiteralClass(SHORT, "int2", xsdShortIri);
    public static final LiteralClass xsdInt = new SimpleLiteralClass(INT, "int4", xsdIntIri);
    public static final LiteralClass xsdLong = new SimpleLiteralClass(LONG, "int8", xsdLongIri);
    public static final LiteralClass xsdFloat = new SimpleLiteralClass(FLOAT, "float4", xsdFloatIri);
    public static final LiteralClass xsdDouble = new SimpleLiteralClass(DOUBLE, "float8", xsdDoubleIri);
    public static final LiteralClass xsdInteger = new SimpleLiteralClass(INTEGER, "decimal", xsdIntegerIri);
    public static final LiteralClass xsdDecimal = new SimpleLiteralClass(DECIMAL, "decimal", xsdDecimalIri);
    public static final LiteralClass xsdString = new SimpleLiteralClass(STRING, "varchar", xsdStringIri);
    public static final LiteralClass xsdDayTimeDuration = new SimpleLiteralClass(DAYTIMEDURATION, "int8",
            xsdDayTimeDurationIri);


    /* xsdDateTime is returned as zoneddatetime because there are many discrepancies in timestamp interpretations */
    public static final LiteralClass xsdDateTime = new LiteralClass("datetime", Arrays.asList("timestamptz", "int4"),
            Arrays.asList(DATETIME), xsdDateTimeIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return "sparql.zoneddatetime_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            if(part == 0)
                return "sparql.zoneddatetime_datetime('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
            else
                return "sparql.zoneddatetime_zone('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
        }
    };


    public static final LiteralClass xsdDateTimeExpr = new LiteralClass("datetime-expr", Arrays.asList("zoneddatetime"),
            Arrays.asList(DATETIME), xsdDateTimeIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return getSqlColumn(variable, part);
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            return "'" + literal.getStringValue().trim() + "'::sparql.zoneddatetime";
        }
    };


    /* xsdDateTime is returned as zoneddatetime because there are many discrepancies in date interpretations */
    public static final LiteralClass xsdDate = new LiteralClass("date", Arrays.asList("date", "int4"),
            Arrays.asList(DATE), xsdDateIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return "sparql.zoneddate_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            if(part == 0)
                return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
            else
                return "sparql.zoneddate_zone('" + (String) literal.getValue() + "'::sparql.zoneddate)";
        }
    };


    public static final LiteralClass xsdDateExpr = new LiteralClass("date-expr", Arrays.asList("zoneddate"),
            Arrays.asList(DATE), xsdDateIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return getSqlColumn(variable, part);
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            return "'" + (String) literal.getValue() + "'::sparql.zoneddate";
        }
    };


    public static final LiteralClass rdfLangString = new LiteralClass("lang", Arrays.asList("varchar", "varchar"),
            Arrays.asList(LANGSTRING, LANG), rdfLangStringIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return getSqlColumn(variable, part);
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            if(part == 0)
                return "'" + ((String) literal.getValue()).replaceAll("'", "''") + "'";
            else
                return "'" + literal.getLanguageTag() + "'";
        }
    };


    public static final LiteralClass rdfLangStringExp = new LiteralClass("lang-expr", Arrays.asList("rdfbox"),
            Arrays.asList(LANGSTRING, LANG), rdfLangStringIri)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            if(part == 0)
                return "sparql.cast_as_string_from_rdfbox(" + getSqlColumn(variable, 0) + ")";
            else
                return "sparql.lang_rdfbox(" + getSqlColumn(variable, 0) + ")";
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            return "sparql.cast_as_rdfbox_from_lang_string('" + ((String) literal.getValue()).replaceAll("'", "''")
                    + "', '" + literal.getLanguageTag() + "')";
        }
    };


    // common iri
    public static final IriClass iri = new IriClass("iri", Arrays.asList("varchar"))
    {
        @Override
        public String getResultValue(String var, int part)
        {
            return getSqlColumn(var, 0);
        }

        @Override
        public String getSqlValue(Node node, int part)
        {
            if(node instanceof VariableOrBlankNode)
                return getSqlColumn(((VariableOrBlankNode) node).getName(), part);

            IRI iri = (IRI) node;

            return "'" + iri.getUri().toString().replaceAll("'", "''") + "'";
        }
    };


    // common unsupported literal
    public static final LiteralClass unsupportedLiteral = new LiteralClass("literal", Arrays.asList("rdfbox"),
            Arrays.asList(RDFTERM), null)
    {
        @Override
        public String getResultValue(String variable, int part)
        {
            return getSqlColumn(variable, part);
        }

        @Override
        public String getSqlLiteralValue(Literal literal, int part)
        {
            String value = literal.getStringValue().replaceAll("'", "''");

            return "sparql.cast_as_rdfbox_from_typed_literal('" + value + "', '"
                    + literal.getTypeIri().getUri().toString() + "')";
        }
    };


    private static final List<LiteralClass> valuesClauseLiteralClasses = Arrays.asList(xsdBoolean, xsdShort, xsdInt,
            xsdLong, xsdFloat, xsdDouble, xsdInteger, xsdDecimal, xsdDateTime, xsdDate, xsdDayTimeDuration, xsdString);


    private static final List<LiteralClass> expressionLiteralClasses = Arrays.asList(xsdBoolean, xsdShort, xsdInt,
            xsdLong, xsdFloat, xsdDouble, xsdInteger, xsdDecimal, xsdDateTimeExpr, xsdDateExpr, xsdDayTimeDuration,
            xsdString);


    public static List<LiteralClass> getValuesClauseLiteralClasses()
    {
        return valuesClauseLiteralClasses;
    }


    public static List<LiteralClass> getExpressionLiteralClasses()
    {
        return expressionLiteralClasses;
    }
}
