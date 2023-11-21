package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LITERAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.TYPE;
import static java.util.Arrays.asList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UserLiteralClass extends LiteralClass
{
    private static final Hashtable<IRI, UserLiteralClass> instances = new Hashtable<IRI, UserLiteralClass>();


    private UserLiteralClass(int id, String sqlType, IRI type)
    {
        super("usertype" + id, asList(sqlType), asList(LITERAL, TYPE), type);
    }


    public static synchronized UserLiteralClass get(String sqlType, IRI type)
    {
        UserLiteralClass userClass = instances.get(type);

        if(userClass != null)
        {
            if(!userClass.sqlTypes.stream().allMatch(t -> t.equals(sqlType)))
                throw new IllegalArgumentException();
        }
        else
        {
            userClass = new UserLiteralClass(instances.size(), sqlType, type);
            instances.put(type, userClass);
        }

        return userClass;
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
    public List<Column> fromExpression(Column column)
    {
        return asList(column);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return columns.get(0);
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        //FIXME: can cause sql exception

        if(check)
            return asList(new ExpressionColumn(("sparql.rdfbox_get_typedliteral_value_of_type(" + column + ", '"
                    + typeIri.getValue().replaceAll("'", "''") + "'::varchar)::" + sqlTypes.get(0))));
        else
            return asList(
                    new ExpressionColumn(("sparql.rdfbox_get_typedliteral_value(" + column + ")::" + sqlTypes.get(0))));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_typedliteral(" + columns.get(0) + "::varchar, '"
                + typeIri.getValue().replaceAll("'", "''") + "'::varchar)");
    }


    @Override
    public Column toExpression(Node node)
    {
        Object value = ((Literal) node).getValue();

        return new ConstantColumn("'" + value.toString().replace("'", "''") + "'::" + sqlTypes.get(0));
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        String type = getTypeIri().getValue().replace("'", "''");
        String code = "CASE WHEN " + columns.get(0) + " IS NOT NULL THEN '" + type + "'::varchar END";

        return asList(new ExpressionColumn(columns.get(0) + "::varchar"), new ExpressionColumn(code));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_typedliteral(" + code + "::varchar, '"
                + typeIri.getValue().replaceAll("'", "''") + "'::varchar)";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        //FIXME: can cause sql exception

        if(check)
            return ("sparql.rdfbox_get_typedliteral_value_of_type(" + code + ", '"
                    + typeIri.getValue().replaceAll("'", "''") + "'::varchar)::" + sqlTypes.get(0));
        else
            return ("sparql.rdfbox_get_typedliteral_value(" + code + ")::" + sqlTypes.get(0));
    }
}
