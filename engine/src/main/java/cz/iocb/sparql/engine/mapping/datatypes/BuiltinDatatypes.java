package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



public class BuiltinDatatypes
{
    public static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";
    public static final String rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final Iri xsdBooleanIri = new Iri(xsdPrefix + "boolean");
    public static final Iri xsdByteIri = new Iri(xsdPrefix + "byte");
    public static final Iri xsdUnsignedByteIri = new Iri(xsdPrefix + "unsignedByte");
    public static final Iri xsdShortIri = new Iri(xsdPrefix + "short");
    public static final Iri xsdUnsignedShortIri = new Iri(xsdPrefix + "unsignedShort");
    public static final Iri xsdIntIri = new Iri(xsdPrefix + "int");
    public static final Iri xsdUnsignedIntIri = new Iri(xsdPrefix + "unsignedInt");
    public static final Iri xsdLongIri = new Iri(xsdPrefix + "long");
    public static final Iri xsdUnsignedLongIri = new Iri(xsdPrefix + "unsignedLong");
    public static final Iri xsdIntegerIri = new Iri(xsdPrefix + "integer");
    public static final Iri xsdNonPositiveIntegerIri = new Iri(xsdPrefix + "nonPositiveInteger");
    public static final Iri xsdNegativeIntegerIri = new Iri(xsdPrefix + "negativeInteger");
    public static final Iri xsdNonNegativeIntegerIri = new Iri(xsdPrefix + "nonNegativeInteger");
    public static final Iri xsdPositiveIntegerIri = new Iri(xsdPrefix + "positiveInteger");
    public static final Iri xsdDecimalIri = new Iri(xsdPrefix + "decimal");
    public static final Iri xsdFloatIri = new Iri(xsdPrefix + "float");
    public static final Iri xsdDoubleIri = new Iri(xsdPrefix + "double");
    public static final Iri xsdDateTimeIri = new Iri(xsdPrefix + "dateTime");
    public static final Iri xsdDateIri = new Iri(xsdPrefix + "date");
    public static final Iri xsdDayTimeDurationIri = new Iri(xsdPrefix + "dayTimeDuration");
    public static final Iri xsdStringIri = new Iri(xsdPrefix + "string");
    public static final Iri rdfLangStringIri = new Iri(rdfPrefix + "langString");

    public static final Datatype xsdBooleanType = new BooleanDatatype();
    public static final Datatype xsdByteType = new ByteDatatype();
    public static final Datatype xsdUnsignedByteType = new UnsignedByteDatatype();
    public static final Datatype xsdShortType = new ShortDatatype();
    public static final Datatype xsdUnsignedShortType = new UnsignedShortDatatype();
    public static final Datatype xsdIntType = new IntDatatype();
    public static final Datatype xsdUnsignedIntType = new UnsignedIntDatatype();
    public static final Datatype xsdLongType = new LongDatatype();
    public static final Datatype xsdUnsignedLongType = new UnsignedLongDatatype();
    public static final Datatype xsdIntegerType = new IntegerDatatype();
    public static final Datatype xsdNonPositiveIntegerType = new NonPositiveIntegerDatatype();
    public static final Datatype xsdNegativeIntegerType = new NegativeIntegerDatatype();
    public static final Datatype xsdNonNegativeIntegerType = new NonNegativeIntegerDatatype();
    public static final Datatype xsdPositiveIntegerType = new PositiveIntegerDatatype();
    public static final Datatype xsdDecimalType = new DecimalDatatype();
    public static final Datatype xsdFloatType = new FloatDatatype();
    public static final Datatype xsdDoubleType = new DoubleDatatype();
    public static final Datatype xsdDateTimeType = new DateTimeDatatype();
    public static final Datatype xsdDateType = new DateDatatype();
    public static final Datatype xsdDayTimeDurationType = new DayTimeDurationDatatype();
    public static final Datatype xsdStringType = new StringDatatype();
    public static final Datatype rdfLangStringType = new LangStringDatatype();
}
