package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import java.util.Hashtable;
import cz.iocb.chemweb.server.sparql.engine.Request;
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


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDate;
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
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        if(part == 0)
        {
            return (table != null ? table + "." : "") + getSqlColumn(var, part);
        }
        else if(check == false)
        {
            return "'" + zone + "'::int4";
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");

            if(table != null)
                builder.append(table).append(".");

            builder.append(getSqlColumn(var, part));
            builder.append(" IS NOT NULL THEN '");
            builder.append(zone);
            builder.append("'::int4 END");

            return builder.toString();
        }
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");

        if(table != null)
            builder.append(table).append(".");

        builder.append(xsdDate.getSqlColumn(var, 1));
        builder.append(" = '");
        builder.append(zone);
        builder.append("'::int4 THEN ");

        if(table != null)
            builder.append(table).append(".");

        builder.append(xsdDate.getSqlColumn(var, 0));
        builder.append(" END");

        return builder.toString();
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
    public boolean match(Node node, Request request)
    {
        if(!super.match(node, request))
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
