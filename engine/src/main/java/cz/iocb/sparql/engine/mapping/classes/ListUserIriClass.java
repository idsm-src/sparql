package cz.iocb.sparql.engine.mapping.classes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class ListUserIriClass extends SimpleUserIriClass
{
    private final Table table;
    private final TableColumn column;
    private final String sqlQuery;


    public ListUserIriClass(String name, Table table, TableColumn column)
    {
        super(name, "varchar");

        this.table = table;
        this.column = column;

        this.sqlQuery = String.format("(SELECT 1 FROM %s WHERE %s = ?::varchar)", table, column);
    }


    @Override
    public List<Column> toColumns(Statement statement, Node node)
    {
        IRI iri = (IRI) node;
        assert match(statement, iri);

        return List.of(new ConstantColumn(iri.getValue(), "varchar"));
    }


    @Override
    public String getPrefix(Statement statement, List<Column> columns)
    {
        return "";
    }


    @Override
    public boolean match(Statement statement, IRI iri)
    {
        try
        {
            String sql = sqlQuery.replaceAll("\\?", sanitizeString(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                return result.next();
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public int getCheckCost()
    {
        return 2;
    }


    @Override
    protected Column generateFunction(Column parameter)
    {
        return parameter;
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        if(!check)
            return parameter;

        String access = String.format("(SELECT %s as \"@col\" FROM %s) as \"@rctab\"", column, table);

        String code = String.format("(SELECT \"@col\"::varchar FROM %s WHERE \"@col\" = %s)", access, parameter);

        return new ExpressionColumn(code);
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        ListUserIriClass other = (ListUserIriClass) object;

        if(!table.equals(other.table))
            return false;

        if(!column.equals(other.column))
            return false;

        return true;
    }
}
