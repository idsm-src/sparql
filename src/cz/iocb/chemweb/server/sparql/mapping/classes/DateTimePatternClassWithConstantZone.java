package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DATETIME;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Hashtable;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DateTimePatternClassWithConstantZone extends PatternLiteralBaseClass implements DateTimeClass
{
    private static final Hashtable<Integer, DateTimePatternClassWithConstantZone> instances = new Hashtable<Integer, DateTimePatternClassWithConstantZone>();

    private final int zone;


    DateTimePatternClassWithConstantZone(int zone)
    {
        super("datetime$" + zone, Arrays.asList("timestamptz"), Arrays.asList(DATETIME), xsdDateTimeIri);
        this.zone = zone;
    }


    public static DateTimePatternClassWithConstantZone get(int zone)
    {
        DateTimePatternClassWithConstantZone instance = instances.get(zone);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(zone);

                if(instance == null)
                {
                    instance = new DateTimePatternClassWithConstantZone(zone);
                    instances.put(zone, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return "sparql.zoneddatetime_create(" + getSqlColumn(variable, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        return "sparql.zoneddatetime_datetime('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.xsdDateTimeExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_datetime" : "sparql.zoneddatetime_create") + "("
                + variableAccessor.variableAccess(variable, this, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        Literal literal = (Literal) node;

        int zone = Integer.MIN_VALUE;

        if(literal.getValue() instanceof OffsetDateTime)
            zone = ((OffsetDateTime) literal.getValue()).getOffset().getTotalSeconds();

        if(zone != this.zone)
            return false;

        return true;
    }


    public int getZone()
    {
        return zone;
    }
}
