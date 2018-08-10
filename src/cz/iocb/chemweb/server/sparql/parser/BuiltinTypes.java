package cz.iocb.chemweb.server.sparql.parser;

import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public abstract class BuiltinTypes
{
    private static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";
    private static final String rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final IRI xsdBooleanIri = new IRI(xsdPrefix + "boolean");
    public static final IRI xsdShortIri = new IRI(xsdPrefix + "short");
    public static final IRI xsdLongIri = new IRI(xsdPrefix + "long");
    public static final IRI xsdIntIri = new IRI(xsdPrefix + "int");
    public static final IRI xsdFloatIri = new IRI(xsdPrefix + "float");
    public static final IRI xsdDoubleIri = new IRI(xsdPrefix + "double");
    public static final IRI xsdIntegerIri = new IRI(xsdPrefix + "integer");
    public static final IRI xsdDecimalIri = new IRI(xsdPrefix + "decimal");
    public static final IRI xsdDateTimeIri = new IRI(xsdPrefix + "dateTime");
    public static final IRI xsdDateIri = new IRI(xsdPrefix + "date");
    public static final IRI xsdDayTimeDurationIri = new IRI(xsdPrefix + "dayTimeDuration");
    public static final IRI xsdStringIri = new IRI(xsdPrefix + "string");
    public static final IRI rdfLangStringIri = new IRI(rdfPrefix + "langString");
}
