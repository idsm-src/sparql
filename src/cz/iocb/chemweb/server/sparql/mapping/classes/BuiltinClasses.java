package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.BOOLEAN;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DAYTIMEDURATION;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DECIMAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DOUBLE;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.FLOAT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.INT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.INTEGER;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LONG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.SHORT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.STRING;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
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
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternLiteralClass;



public class BuiltinClasses
{
    public static final SimpleLiteralClass xsdBoolean = new SimpleLiteralClass(BOOLEAN, "bool", xsdBooleanIri);
    public static final SimpleLiteralClass xsdShort = new SimpleLiteralClass(SHORT, "int2", xsdShortIri);
    public static final SimpleLiteralClass xsdInt = new SimpleLiteralClass(INT, "int4", xsdIntIri);
    public static final SimpleLiteralClass xsdLong = new SimpleLiteralClass(LONG, "int8", xsdLongIri);
    public static final SimpleLiteralClass xsdFloat = new SimpleLiteralClass(FLOAT, "float4", xsdFloatIri);
    public static final SimpleLiteralClass xsdDouble = new SimpleLiteralClass(DOUBLE, "float8", xsdDoubleIri);
    public static final SimpleLiteralClass xsdInteger = new SimpleLiteralClass(INTEGER, "decimal", xsdIntegerIri);
    public static final SimpleLiteralClass xsdDecimal = new SimpleLiteralClass(DECIMAL, "decimal", xsdDecimalIri);
    public static final SimpleLiteralClass xsdString = new SimpleLiteralClass(STRING, "varchar", xsdStringIri);
    public static final SimpleLiteralClass xsdDayTimeDuration = new SimpleLiteralClass(DAYTIMEDURATION, "int8",
            xsdDayTimeDurationIri);

    public static final DateTimePatternClass xsdDateTime = new DateTimePatternClass();
    public static final DatePatternClass xsdDate = new DatePatternClass();
    public static final LangStringPatternClass rdfLangString = new LangStringPatternClass();
    public static final UnsupportedLiteralClass unsupportedLiteral = new UnsupportedLiteralClass();

    public static final DateTimeExpressionClass xsdDateTimeExpr = new DateTimeExpressionClass();
    public static final DateExpressionClass xsdDateExpr = new DateExpressionClass();
    public static final LangStringExpressionClass rdfLangStringExpr = new LangStringExpressionClass();
    public static final UnsupportedLiteralExpressionClass unsupportedLiteralExpr = new UnsupportedLiteralExpressionClass();

    public static final BlankNodeIntClass intBlankNode = new BlankNodeIntClass();
    public static final BlankNodeStrClass strBlankNode = new BlankNodeStrClass();
    public static final CommonIriClass iri = new CommonIriClass();
    public static final CommonIriClass unsupportedIri = new CommonIriClass();


    private static final List<PatternLiteralClass> patternLiteralClasses = Arrays.asList(xsdBoolean, xsdShort, xsdInt,
            xsdLong, xsdFloat, xsdDouble, xsdInteger, xsdDecimal, xsdDateTime, xsdDate, xsdDayTimeDuration, xsdString);

    private static final List<ExpressionLiteralClass> expressionLiteralClasses = Arrays.asList(xsdBoolean, xsdShort,
            xsdInt, xsdLong, xsdFloat, xsdDouble, xsdInteger, xsdDecimal, xsdDateTimeExpr, xsdDateExpr,
            xsdDayTimeDuration, xsdString);


    public static List<PatternLiteralClass> getPatternLiteralClasses()
    {
        return patternLiteralClasses;
    }


    public static List<ExpressionLiteralClass> getExpressionLiteralClasses()
    {
        return expressionLiteralClasses;
    }
}
