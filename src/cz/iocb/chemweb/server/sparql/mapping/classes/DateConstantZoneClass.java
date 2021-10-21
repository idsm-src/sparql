package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class DateConstantZoneClass extends LiteralClass
{
    private static final Hashtable<Integer, DateConstantZoneClass> instances = new Hashtable<Integer, DateConstantZoneClass>();

    private final int zone;


    private DateConstantZoneClass(int zone)
    {
        super("date$" + zone, asList("date"), asList(DATE), xsdDateIri);
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
    public List<Column> toColumns(Node node)
    {
        return asList(new ConstantColumn("'" + (String) ((Literal) node).getValue() + "'::date"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");
        builder.append(columns.get(1));
        builder.append(" = '");
        builder.append(zone);
        builder.append("'::int4 THEN ");
        builder.append(columns.get(0));
        builder.append(" END");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getGeneralClass().getColumnCount());

        result.add(columns.get(0));

        if(check == false)
        {
            result.add(new ConstantColumn("'" + zone + "'::int4"));
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");
            builder.append(columns.get(0));
            builder.append(" IS NOT NULL THEN '");
            builder.append(zone);
            builder.append("'::int4 END");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        String prefix = isBoxed ? "sparql.rdfbox_extract_date" : "sparql.zoneddate";

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE ");
            builder.append(prefix + "_zone(" + column + ")");
            builder.append(" WHEN '");
            builder.append(getZone());
            builder.append("'::int4 THEN ");
            builder.append(prefix + "_date(" + column + ")");
            builder.append(" END");
        }
        else
        {
            builder.append(prefix + "_date(" + column + ")");
        }

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ExpressionColumn("sparql.zoneddate_date('" + ((Literal) node).getValue() + "'::sparql.zoneddate)");
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_date(" + columns.get(0) + ", '" + zone + "'::int4)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return asList(new ExpressionColumn("sparql.zoneddate_create(" + columns.get(0) + ", '" + zone + "'::int4)"));
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(DateClass.getZone((Literal) node) != zone)
            return false;

        return true;
    }


    public int getZone()
    {
        return zone;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        DateConstantZoneClass other = (DateConstantZoneClass) object;

        if(zone != other.zone)
            return false;

        return true;
    }
}
