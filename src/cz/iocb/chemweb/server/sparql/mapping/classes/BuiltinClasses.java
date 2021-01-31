package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.BOOLEAN;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DAYTIMEDURATION;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DECIMAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DOUBLE;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.FLOAT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.INT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.INTEGER;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LONG;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.SHORT;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.STRING;
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

    public static final DateTimeClass xsdDateTime = new DateTimeClass();
    public static final DateClass xsdDate = new DateClass();
    public static final LangStringClass rdfLangString = new LangStringClass();
    public static final UnsupportedLiteralClass unsupportedLiteral = new UnsupportedLiteralClass();

    public static final IntBlankNodeClass intBlankNode = new CommonIntBlankNodeClass();
    public static final StrBlankNodeClass strBlankNode = new CommonStrBlankNodeClass();
    public static final CommonIriClass iri = new CommonIriClass();
    public static final CommonIriClass unsupportedIri = new CommonIriClass();

    public static final IntBlankNodeClass bnodeIntBlankNode = new UserIntBlankNodeClass();
    public static final StrBlankNodeClass bnodeStrBlankNode = new UserStrBlankNodeClass();


    private static final List<LiteralClass> literalClasses = Arrays.asList(xsdBoolean, xsdShort, xsdInt, xsdLong,
            xsdFloat, xsdDouble, xsdInteger, xsdDecimal, xsdDateTime, xsdDate, xsdDayTimeDuration, xsdString);

    private static final List<ResourceClass> classes = Arrays.asList(xsdBoolean, xsdShort, xsdInt, xsdLong, xsdFloat,
            xsdDouble, xsdInteger, xsdDecimal, xsdDateTime, xsdDate, xsdDayTimeDuration, xsdString, rdfLangString,
            unsupportedLiteral, intBlankNode, strBlankNode, iri);


    public static List<LiteralClass> getLiteralClasses()
    {
        return literalClasses;
    }


    public static List<ResourceClass> getClasses()
    {
        return classes;
    }
}
