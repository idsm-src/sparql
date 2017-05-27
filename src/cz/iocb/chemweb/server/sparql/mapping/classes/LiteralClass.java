package cz.iocb.chemweb.server.sparql.mapping.classes;

import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class LiteralClass extends ResourceClass
{
    private static final String xsd = "http://www.w3.org/2001/XMLSchema#";

    public static final String booleanTag = "bln";
    public static final String integerTag = "int";
    public static final String decimalTag = "dec";
    public static final String floatTag = "flt";
    public static final String doubleTag = "dbl";
    public static final String dateTag = "date";
    public static final String dateTimeTag = "time";
    public static final String stringTag = "str";

    public static final LiteralClass xsdBoolean = new LiteralClass(booleanTag, "boolean", xsd + "boolean");
    public static final LiteralClass xsdInteger = new LiteralClass(integerTag, "integer", xsd + "integer");
    public static final LiteralClass xsdDecimal = new LiteralClass(decimalTag, "float8", xsd + "decimal");
    public static final LiteralClass xsdFloat = new LiteralClass(floatTag, "real", xsd + "float");
    public static final LiteralClass xsdDouble = new LiteralClass(doubleTag, "float8", xsd + "double");
    public static final LiteralClass xsdDate = new LiteralClass(dateTag, "date", xsd + "date");
    public static final LiteralClass xsdDateTime = new LiteralClass(dateTimeTag, "timestamptz", xsd + "dateTime");
    public static final LiteralClass xsdString = new LiteralClass(stringTag, "varchar", xsd + "string");

    private final String typeIri;
    private final String sqlType;


    protected LiteralClass(String name, String sqlType, String typeIri)
    {
        super(name);
        this.sqlType = sqlType;
        this.typeIri = typeIri;
    }


    @Override
    public final int getPartsCount()
    {
        return 1;
    }


    @Override
    public String getSqlColumn(String var, int par)
    {
        return '"' + var + '#' + name + '"';
    }


    @Override
    public String getSparqlValue(String var)
    {
        return getSqlColumn(var, 0);
    }


    @Override
    public String getSqlValue(Node node, int i)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), i);


        if(!(node instanceof Literal))
            return null;

        Literal literal = (Literal) node;

        if(literal.getLanguageTag() != null)
            return null;

        Object value = literal.getValue();

        if(value instanceof String)
            return "'" + value.toString().replaceAll("'", "\\'") + "'";
        else
            return value.toString();
    }


    @Override
    public String getSqlType(int i)
    {
        return sqlType;
    }


    @Override
    public boolean match(Node value)
    {
        if(!(value instanceof Literal))
            return false;


        Literal literal = (Literal) value;

        //TODO: fix Literal.getTypeIri() to not return null
        IRI iri = literal.getTypeIri();
        String type = iri != null ? iri.getUri().toString() : "http://www.w3.org/2001/XMLSchema#string";

        if(!typeIri.equals(type))
            return false;


        return true;
    }


    public final String getTypeIri()
    {
        return typeIri;
    }
}
