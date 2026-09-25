package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs stored as full text whose membership is defined by the values of a table column.
 */
public class ListUserIriClass extends SimpleUserIriClass
{
    /**
     * Table listing the member IRIs.
     */
    private final DatabaseTable table;

    /**
     * Column holding the member IRIs.
     */
    private final TableColumn column;

    /**
     * Query testing membership of a placeholder IRI.
     */
    private final String sqlQuery;


    /**
     * Creates the class over the given table column.
     *
     * @param name the name
     * @param table the table
     * @param column the column
     */
    public ListUserIriClass(String name, DatabaseTable table, TableColumn column)
    {
        super(name, VARCHAR);

        this.table = table;
        this.column = column;

        this.sqlQuery = String.format("(SELECT 1 FROM %s WHERE %s = ?::varchar)", table, column);
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        assert match(statement, iri);

        return List.of(constant(iri.getValue(), VARCHAR));
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        try
        {
            String sql = sqlQuery.replace("?", string(iri.getValue()));

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
    protected Column generateFunction(Column column)
    {
        return column;
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        if(!check)
            return column;

        Column access = expression("(SELECT %s as \"@col\" FROM %s) as \"@rctab\"", column, table);

        return expression("(SELECT \"@col\"::varchar FROM %s WHERE \"@col\" = %s)", access, column);
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return "";
    }


    @Override
    public int getCheckCost()
    {
        return 2;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        ListUserIriClass other = (ListUserIriClass) object;

        return Objects.equals(table, other.table) && Objects.equals(column, other.column);
    }
}
