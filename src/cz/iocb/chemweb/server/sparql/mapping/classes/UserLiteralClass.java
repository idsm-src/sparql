package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LITERAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.TYPE;
import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UserLiteralClass extends LiteralClass
{
    public UserLiteralClass(String name, String sqlType, IRI type)
    {
        super(name, asList(sqlType), asList(LITERAL, TYPE), type);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        Object value = ((Literal) node).getValue();

        return asList(new ConstantColumn("'" + value.toString().replace("'", "''") + "'::" + sqlTypes.get(0)));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return columns;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return columns;
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE ");
            builder.append("sparql.rdfbox_extract_typed_literal_type" + "(" + column + ")");
            builder.append(" WHEN '");
            builder.append(getTypeIri().getValue().replace("'", "''"));
            builder.append("'::varchar THEN ");
            builder.append("sparql.rdfbox_extract_typed_literal_literal" + "(" + column + ")::");
            builder.append(sqlTypes.get(0));
            builder.append(" END");
        }
        else
        {
            builder.append(sqlTypes.get(0));
            builder.append("sparql.rdfbox_extract_typed_literal_literal" + "(" + column + ")::");
        }

        return asList(new ExpressionColumn(builder.toString()));
    }


    @Override
    public Column toExpression(Node node)
    {
        Object value = ((Literal) node).getValue();

        return new ConstantColumn("'" + value.toString().replace("'", "''") + "'::" + sqlTypes.get(0));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_typed_literal(" + columns.get(0) + "::varchar, '"
                + getTypeIri().getValue().replace("'", "''") + "'::varchar)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        String type = getTypeIri().getValue().replace("'", "''");
        String code = "CASE WHEN " + columns.get(0) + " IS NOT NULL THEN '" + type + "'::varchar END";

        return asList(new ExpressionColumn(columns.get(0) + "::varchar"), new ExpressionColumn(code));
    }
}
