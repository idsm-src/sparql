package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UserStrBlankNodeClass extends StrBlankNodeClass
{
    static private int counter;

    private final int segment;


    public UserStrBlankNodeClass()
    {
        super("sparql.str_blanknode_" + Integer.toHexString(counter), asList("varchar"));
        this.segment = counter++;
    }


    public UserStrBlankNodeClass(int segment)
    {
        super("sparql.str_blanknode_" + Integer.toHexString(segment), asList("varchar"));
        this.segment = segment;

        if(segment >= 0)
            throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return asList(new ConstantColumn("'" + ((BlankNodeLiteral) node).getLabel().replace("'", "''") + "'::varchar"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN sparql.str_blanknode_segment(");
        builder.append(columns.get(1));
        builder.append(") = '");
        builder.append(segment);
        builder.append("'::int4 THEN sparql.str_blanknode_label(");
        builder.append(columns.get(0));
        builder.append(") END");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return asList(
                new ExpressionColumn("sparql.str_blanknode_create('" + segment + "'::int4, " + columns.get(0) + ")"));
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        String prefix = isBoxed ? "sparql.rdfbox_extract_str_blanknode" : "sparql.str_blanknode";

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE ");
            builder.append(prefix + "_segment(");
            builder.append(column);
            builder.append(") WHEN '");
            builder.append(getSegment());
            builder.append("'::int4 THEN ");
            builder.append(prefix + "_label(" + column + ")");
            builder.append(" END ");
        }
        else
        {
            builder.append(prefix + "_label(" + column + ")");
        }

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn(
                "sparql.cast_as_rdfbox_from_str_blanknode('" + segment + "'::int4, " + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return asList(
                new ExpressionColumn("sparql.str_blanknode_create('" + segment + "'::int4, " + columns.get(0) + ")"));
    }


    public int getSegment()
    {
        return segment;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        UserStrBlankNodeClass other = (UserStrBlankNodeClass) object;

        if(segment != other.segment)
            return false;

        return true;
    }
}
