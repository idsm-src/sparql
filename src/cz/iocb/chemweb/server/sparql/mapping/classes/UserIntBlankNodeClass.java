package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UserIntBlankNodeClass extends IntBlankNodeClass
{
    static private int counter;

    private final int segment;


    public UserIntBlankNodeClass()
    {
        super("sparql.int_blanknode_" + Integer.toHexString(counter), asList("int4"));
        this.segment = counter++;
    }


    public UserIntBlankNodeClass(int segment)
    {
        super("sparql.int_blanknode_" + Integer.toHexString(segment), asList("int4"));
        this.segment = segment;

        if(segment >= 0)
            throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return asList(new ConstantColumn("'" + ((BlankNodeLiteral) node).getLabel() + "'::int4"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN sparql.int_blanknode_segment(");
        builder.append(columns.get(1));
        builder.append(") = '");
        builder.append(segment);
        builder.append("'::int4 THEN sparql.int_blanknode_label(");
        builder.append(columns.get(0));
        builder.append(") END");

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return asList(
                new ExpressionColumn("sparql.int_blanknode_create('" + segment + "'::int4, " + columns.get(0) + ")"));
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        String prefix = isBoxed ? "sparql.rdfbox_extract_int_blanknode" : "sparql.int_blanknode";

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
                "sparql.cast_as_rdfbox_from_int_blanknode('" + segment + "'::int4, " + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return asList(
                new ExpressionColumn("sparql.int_blanknode_create('" + segment + "'::int4, " + columns.get(0) + ")"));
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

        UserIntBlankNodeClass other = (UserIntBlankNodeClass) object;

        if(segment != other.segment)
            return false;

        return true;
    }
}
