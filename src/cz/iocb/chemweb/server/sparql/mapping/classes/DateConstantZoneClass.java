package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import java.util.Hashtable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DateConstantZoneClass extends LiteralClass
{
    private static final Hashtable<Integer, DateConstantZoneClass> instances = new Hashtable<Integer, DateConstantZoneClass>();

    private final int zone;


    DateConstantZoneClass(int zone)
    {
        super("date$" + zone, Arrays.asList("date"), Arrays.asList(DATE), xsdDateIri);
        this.zone = zone;
    }


    public static DateConstantZoneClass get(int zone)
    {
        DateConstantZoneClass instance = instances.get(zone);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(zone);

                if(instance == null)
                {
                    instance = new DateConstantZoneClass(zone);
                    instances.put(zone, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        return (isBoxed ? "sparql.rdfbox_extract_date_date(" : "sparql.zoneddate_date(") + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        String code = variableAccessor.getSqlVariableAccess(variable, this, 0);

        if(rdfbox)
            code = "sparql.cast_as_rdfbox_from_date(" + code + ", '" + zone + "'::int4)";

        return code;
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return "sparql.zoneddate_create(" + getSqlColumn(variable, 0) + ", '" + zone + "'::int4)";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(getZone((Literal) node) != this.zone)
            return false;

        return true;
    }


    public int getZone()
    {
        return zone;
    }


    public static int getZone(Literal literal)
    {
        String value = (String) literal.getValue();
        String[] parts = value.replaceFirst(".*(Z|(([+-])([0-9][0-9]):([0-9][0-9])))$", "$31#0$4#0$5").split("#");
        int zone = Integer.MIN_VALUE;

        if(parts.length == 3)
            zone = Integer.parseInt(parts[0]) * (Integer.parseInt(parts[1]) * 3600 + Integer.parseInt(parts[2]) * 60);

        return zone;
    }
}
