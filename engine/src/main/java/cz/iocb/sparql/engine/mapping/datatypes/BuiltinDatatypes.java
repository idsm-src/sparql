package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



public class BuiltinDatatypes
{
    public static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";
    public static final String rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final Iri xsdBooleanIri = new Iri(xsdPrefix + "boolean");
    public static final Iri xsdShortIri = new Iri(xsdPrefix + "short");
    public static final Iri xsdLongIri = new Iri(xsdPrefix + "long");
    public static final Iri xsdIntIri = new Iri(xsdPrefix + "int");
    public static final Iri xsdFloatIri = new Iri(xsdPrefix + "float");
    public static final Iri xsdDoubleIri = new Iri(xsdPrefix + "double");
    public static final Iri xsdIntegerIri = new Iri(xsdPrefix + "integer");
    public static final Iri xsdDecimalIri = new Iri(xsdPrefix + "decimal");
    public static final Iri xsdDateTimeIri = new Iri(xsdPrefix + "dateTime");
    public static final Iri xsdDateIri = new Iri(xsdPrefix + "date");
    public static final Iri xsdDayTimeDurationIri = new Iri(xsdPrefix + "dayTimeDuration");
    public static final Iri xsdStringIri = new Iri(xsdPrefix + "string");
    public static final Iri rdfLangStringIri = new Iri(rdfPrefix + "langString");

    public static final Datatype xsdBooleanType = new BooleanDatatype();
    public static final Datatype xsdShortType = new ShortDatatype();
    public static final Datatype xsdIntType = new IntDatatype();
    public static final Datatype xsdLongType = new LongDatatype();
    public static final Datatype xsdIntegerType = new IntegerDatatype();
    public static final Datatype xsdDecimalType = new DecimalDatatype();
    public static final Datatype xsdFloatType = new FloatDatatype();
    public static final Datatype xsdDoubleType = new DoubleDatatype();
    public static final Datatype xsdStringType = new StringDatatype();
    public static final Datatype xsdDayTimeDurationType = new DayTimeDurationDatatype();
    public static final Datatype xsdDateType = new DateDatatype();
    public static final Datatype xsdDateTimeType = new DateTimeDatatype();
    public static final Datatype rdfLangStringType = new LangStringDatatype();
}
