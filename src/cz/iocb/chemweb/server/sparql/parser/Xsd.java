package cz.iocb.chemweb.server.sparql.parser;

import java.net.URI;



/**
 * Static class that holds types in the XSD namespace.
 */
public abstract class Xsd
{
    private Xsd()
    {
    }

    /**
     * The XSD namespace.
     */
    public static final String NS = "http://www.w3.org/2001/XMLSchema#";

    public static final String ANY_URI = NS + "anyURI";
    public static final String BOOLEAN = NS + "boolean";
    public static final String DATE_TIME_STAMP = NS + "dateTimeStamp";
    public static final String DECIMAL = NS + "decimal";
    public static final String INTEGER = NS + "integer";
    public static final String LONG = NS + "long";
    public static final String INT = NS + "int";
    public static final String SHORT = NS + "short";
    public static final String BYTE = NS + "byte";
    public static final String DOUBLE = NS + "double";
    public static final String FLOAT = NS + "float";
    public static final String STRING = NS + "string";
    public static final URI STRING_URI = URI.create(STRING);
}
