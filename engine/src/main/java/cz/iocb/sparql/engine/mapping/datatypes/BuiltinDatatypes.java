package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs and singleton {@link Datatype}s of the supported XSD datatypes and {@code rdf:langString}.
 */
public class BuiltinDatatypes
{
    /**
     * Namespace of the XML Schema datatypes.
     */
    public static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";

    /**
     * Namespace of the RDF vocabulary.
     */
    public static final String rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * IRI of xsd:boolean.
     */
    public static final Iri xsdBooleanIri = new Iri(xsdPrefix + "boolean");

    /**
     * IRI of xsd:byte.
     */
    public static final Iri xsdByteIri = new Iri(xsdPrefix + "byte");

    /**
     * IRI of xsd:unsignedByte.
     */
    public static final Iri xsdUnsignedByteIri = new Iri(xsdPrefix + "unsignedByte");

    /**
     * IRI of xsd:short.
     */
    public static final Iri xsdShortIri = new Iri(xsdPrefix + "short");

    /**
     * IRI of xsd:unsignedShort.
     */
    public static final Iri xsdUnsignedShortIri = new Iri(xsdPrefix + "unsignedShort");

    /**
     * IRI of xsd:int.
     */
    public static final Iri xsdIntIri = new Iri(xsdPrefix + "int");

    /**
     * IRI of xsd:unsignedInt.
     */
    public static final Iri xsdUnsignedIntIri = new Iri(xsdPrefix + "unsignedInt");

    /**
     * IRI of xsd:long.
     */
    public static final Iri xsdLongIri = new Iri(xsdPrefix + "long");

    /**
     * IRI of xsd:unsignedLong.
     */
    public static final Iri xsdUnsignedLongIri = new Iri(xsdPrefix + "unsignedLong");

    /**
     * IRI of xsd:integer.
     */
    public static final Iri xsdIntegerIri = new Iri(xsdPrefix + "integer");

    /**
     * IRI of xsd:nonPositiveInteger.
     */
    public static final Iri xsdNonPositiveIntegerIri = new Iri(xsdPrefix + "nonPositiveInteger");

    /**
     * IRI of xsd:negativeInteger.
     */
    public static final Iri xsdNegativeIntegerIri = new Iri(xsdPrefix + "negativeInteger");

    /**
     * IRI of xsd:nonNegativeInteger.
     */
    public static final Iri xsdNonNegativeIntegerIri = new Iri(xsdPrefix + "nonNegativeInteger");

    /**
     * IRI of xsd:positiveInteger.
     */
    public static final Iri xsdPositiveIntegerIri = new Iri(xsdPrefix + "positiveInteger");

    /**
     * IRI of xsd:decimal.
     */
    public static final Iri xsdDecimalIri = new Iri(xsdPrefix + "decimal");

    /**
     * IRI of xsd:float.
     */
    public static final Iri xsdFloatIri = new Iri(xsdPrefix + "float");

    /**
     * IRI of xsd:double.
     */
    public static final Iri xsdDoubleIri = new Iri(xsdPrefix + "double");

    /**
     * IRI of xsd:dateTime.
     */
    public static final Iri xsdDateTimeIri = new Iri(xsdPrefix + "dateTime");

    /**
     * IRI of xsd:date.
     */
    public static final Iri xsdDateIri = new Iri(xsdPrefix + "date");

    /**
     * IRI of xsd:dayTimeDuration.
     */
    public static final Iri xsdDayTimeDurationIri = new Iri(xsdPrefix + "dayTimeDuration");

    /**
     * IRI of xsd:string.
     */
    public static final Iri xsdStringIri = new Iri(xsdPrefix + "string");

    /**
     * IRI of rdf:langString.
     */
    public static final Iri rdfLangStringIri = new Iri(rdfPrefix + "langString");

    /**
     * The xsd:boolean datatype.
     */
    public static final Datatype xsdBooleanType = new BooleanDatatype();

    /**
     * The xsd:byte datatype.
     */
    public static final Datatype xsdByteType = new ByteDatatype();

    /**
     * The xsd:unsignedByte datatype.
     */
    public static final Datatype xsdUnsignedByteType = new UnsignedByteDatatype();

    /**
     * The xsd:short datatype.
     */
    public static final Datatype xsdShortType = new ShortDatatype();

    /**
     * The xsd:unsignedShort datatype.
     */
    public static final Datatype xsdUnsignedShortType = new UnsignedShortDatatype();

    /**
     * The xsd:int datatype.
     */
    public static final Datatype xsdIntType = new IntDatatype();

    /**
     * The xsd:unsignedInt datatype.
     */
    public static final Datatype xsdUnsignedIntType = new UnsignedIntDatatype();

    /**
     * The xsd:long datatype.
     */
    public static final Datatype xsdLongType = new LongDatatype();

    /**
     * The xsd:unsignedLong datatype.
     */
    public static final Datatype xsdUnsignedLongType = new UnsignedLongDatatype();

    /**
     * The xsd:integer datatype.
     */
    public static final Datatype xsdIntegerType = new IntegerDatatype();

    /**
     * The xsd:nonPositiveInteger datatype.
     */
    public static final Datatype xsdNonPositiveIntegerType = new NonPositiveIntegerDatatype();

    /**
     * The xsd:negativeInteger datatype.
     */
    public static final Datatype xsdNegativeIntegerType = new NegativeIntegerDatatype();

    /**
     * The xsd:nonNegativeInteger datatype.
     */
    public static final Datatype xsdNonNegativeIntegerType = new NonNegativeIntegerDatatype();

    /**
     * The xsd:positiveInteger datatype.
     */
    public static final Datatype xsdPositiveIntegerType = new PositiveIntegerDatatype();

    /**
     * The xsd:decimal datatype.
     */
    public static final Datatype xsdDecimalType = new DecimalDatatype();

    /**
     * The xsd:float datatype.
     */
    public static final Datatype xsdFloatType = new FloatDatatype();

    /**
     * The xsd:double datatype.
     */
    public static final Datatype xsdDoubleType = new DoubleDatatype();

    /**
     * The xsd:dateTime datatype.
     */
    public static final Datatype xsdDateTimeType = new DateTimeDatatype();

    /**
     * The xsd:date datatype.
     */
    public static final Datatype xsdDateType = new DateDatatype();

    /**
     * The xsd:dayTimeDuration datatype.
     */
    public static final Datatype xsdDayTimeDurationType = new DayTimeDurationDatatype();

    /**
     * The xsd:string datatype.
     */
    public static final Datatype xsdStringType = new StringDatatype();

    /**
     * The rdf:langString datatype.
     */
    public static final Datatype rdfLangStringType = new LangStringDatatype();


    /**
     * Not instantiable.
     */
    private BuiltinDatatypes()
    {
    }

}
