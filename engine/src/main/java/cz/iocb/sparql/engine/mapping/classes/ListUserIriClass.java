package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * IRIs stored as full text whose membership is defined by the values of a table column. The column may list an IRI
 * several times and may hold NULLs.
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
    public List<Column> toColumns(Request request, Iri iri)
    {
        assert match(request, iri);

        return List.of(constant(iri.getValue(), VARCHAR));
    }


    @Override
    public boolean match(Request request, Iri iri)
    {
        try
        {
            String sql = sqlQuery.replace("?", string(iri.getValue()));

            try(ResultSet result = request.getStatement().executeQuery(sql))
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
    protected Column generateFunction(Column value)
    {
        return value;
    }


    @Override
    protected Column generateInverseFunction(Column value, boolean check)
    {
        if(!check)
            return value;

        // the listed IRIs are read under a name of their own, so that no column of the table hides a column the value
        // refers to; one row is enough, because the column may list the IRI several times
        SqlCode access = SqlCode.format("(SELECT %s as \"@col\" FROM %s) as \"@rctab\"", column, table);

        return expression(VARCHAR, "(SELECT \"@col\"::varchar FROM %s WHERE \"@col\" = %s LIMIT 1)", access, value);
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
