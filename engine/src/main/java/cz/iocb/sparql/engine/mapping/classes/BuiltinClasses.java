package cz.iocb.sparql.engine.mapping.classes;

public class BuiltinClasses
{
    /* built-in (result) types */
    public static final CommonIriClass iri = new CommonIriClass();
    public static final CommonIntBlankNodeClass intBlankNode = new CommonIntBlankNodeClass();
    public static final CommonStrBlankNodeClass strBlankNode = new CommonStrBlankNodeClass();
    public static final SimpleLiteralClass xsdBoolean = new BooleanClass();
    public static final SimpleLiteralClass xsdShort = new ShortClass();
    public static final SimpleLiteralClass xsdInt = new IntClass();
    public static final SimpleLiteralClass xsdLong = new LongClass();
    public static final SimpleLiteralClass xsdFloat = new FloatClass();
    public static final SimpleLiteralClass xsdDouble = new DoubleClass();
    public static final SimpleLiteralClass xsdInteger = new IntegerClass();
    public static final SimpleLiteralClass xsdDecimal = new DecimalClass();
    public static final SimpleLiteralClass xsdString = new StringClass();
    public static final SimpleLiteralClass xsdDayTimeDuration = new DayTimeDurationClass();
    public static final DateTimeClass xsdDateTime = new DateTimeClass();
    public static final DateClass xsdDate = new DateClass();
    public static final LangStringClass rdfLangString = new LangStringClass();
    public static final UnsupportedLiteralClass unsupportedLiteral = new UnsupportedLiteralClass();

    /* special built-in types */
    public static final UnsupportedIriClass unsupportedIri = new UnsupportedIriClass();
    public static final IntBlankNodeClass bnodeIntBlankNode = new UserIntBlankNodeClass();
    public static final StrBlankNodeClass bnodeStrBlankNode = new UserStrBlankNodeClass();
}
