package cz.iocb.sparql.engine.translator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.imcode.SqlDistinct;
import cz.iocb.sparql.engine.imcode.SqlFilter;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Estimates row counts of intermediate code by asking PostgreSQL for the plan of its SQL ({@code EXPLAIN}).
 */
public class Estimator
{
    /**
     * Request whose statement runs the EXPLAIN queries.
     */
    private final Request request;


    /**
     * Creates the estimator.
     *
     * @param request the current request
     */
    public Estimator(Request request)
    {
        this.request = request;
    }


    /**
     * Estimated number of distinct values of the variable in the solutions of the intermediate code.
     *
     * @param request the current request
     * @param scope the scope of the intermediate code
     * @param imcode the intermediate code
     * @param var the variable
     * @return estimated number of distinct values of the variable in the solutions of the intermediate code
     * @throws SQLException on database errors
     */
    public long estimateDistinct(Request request, Scope scope, SqlIntercode imcode, Variable var) throws SQLException
    {
        Restrictions restrictions = new Restrictions();
        restrictions.add(var);

        SqlIntercode distinct = SqlDistinct.create(request, imcode, Set.of(var)).optimize(request, scope, restrictions,
                false, false);

        return estimateRowCount(translateStatement(scope, distinct));
    }


    /**
     * Estimated number of distinct value combinations of the variables in the solutions of the intermediate code;
     * {@link Long#MAX_VALUE} when the estimate fails.
     *
     * @param request the current request
     * @param scope the scope of the intermediate code
     * @param imcode the intermediate code
     * @param vars the variables
     * @return estimated number of distinct value combinations of the variables in the solutions of the intermediate
     *         code; {@link Long#MAX_VALUE} when the estimate fails
     */
    public long estimateDistinct(Request request, Scope scope, SqlIntercode imcode, Set<Variable> vars)
    {
        if(vars.isEmpty())
            return 0;

        try
        {
            Restrictions restrictions = new Restrictions(vars);

            SqlIntercode distinct = SqlDistinct.create(request, imcode, vars).optimize(request, scope, restrictions,
                    false, false);

            return estimateRowCount(translateStatement(scope, distinct));
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return Long.MAX_VALUE;
        }
    }


    /**
     * Fraction of the solutions of the intermediate code that the conditions are estimated to keep: the rows the
     * planner of the database estimates for the code filtered by the conditions, over the rows it estimates for the
     * code itself, so that what the planner misses about the code itself cancels out. The planner estimates a
     * comparison of a column with a constant from the statistics of the column, which the engine keeps no like of.
     * {@link Double#NaN} when the planner cannot be asked, because the code refers to a solution it is evaluated for,
     * or when an estimate fails.
     *
     * @param request the current request
     * @param scope the scope of the intermediate code
     * @param imcode the intermediate code
     * @param conditions conditions over the variable bindings of the intermediate code
     * @return fraction of the solutions of the intermediate code that the conditions are estimated to keep, or
     *         {@link Double#NaN}
     */
    public double estimateSelectivity(Request request, Scope scope, SqlIntercode imcode,
            List<SqlExpressionIntercode> conditions)
    {
        try
        {
            long all = estimateRowCount(scope, imcode);
            long kept = all <= 0 ? -1 : estimateRowCount(scope, SqlFilter.filter(request, conditions, imcode));

            return kept < 0 ? Double.NaN : Math.min((double) kept / all, 1);
        }
        catch(SQLException e)
        {
            return Double.NaN;
        }
    }


    /**
     * Row count estimated by the PostgreSQL planner for the intermediate code, asked for once in a request (see
     * {@link Request#getRowEstimates}); -1 when the code refers to a solution it is evaluated for, so that it is no
     * statement by itself.
     *
     * @param scope the scope of the intermediate code
     * @param imcode the intermediate code
     * @return row count estimated by the PostgreSQL planner for the intermediate code, or -1
     * @throws SQLException on database errors
     */
    private long estimateRowCount(Scope scope, SqlIntercode imcode) throws SQLException
    {
        SqlCode code = imcode.translate(request, scope);

        if(!code.getOuterTables().isEmpty())
            return -1;

        String with = SqlSelect.translateWithClause(request, code);
        String sql = with.isEmpty() ? code.getText() : with + "SELECT * FROM (" + code.getText() + ") AS tab";

        Long rows = request.getRowEstimates().get(sql);

        if(rows == null)
        {
            rows = estimateRowCount(sql);
            request.getRowEstimates().put(sql, rows);
        }

        return rows;
    }


    /**
     * SQL of a statement evaluating the intermediate code: its SQL preceded by the {@code WITH} clause declaring the
     * virtual tables and the common table expressions it reads (see {@link SqlSelect#translateWithClause}). The SQL of
     * the code is then wrapped in a query, since it need not be a plain {@code SELECT}: a walk begins with a
     * {@code WITH RECURSIVE} clause of its own, which cannot follow another {@code WITH} clause.
     *
     * @param scope the scope of the intermediate code
     * @param imcode the intermediate code
     * @return SQL of a statement evaluating the intermediate code
     */
    private String translateStatement(Scope scope, SqlIntercode imcode)
    {
        SqlCode code = imcode.translate(request, scope);
        String with = SqlSelect.translateWithClause(request, code);

        return with.isEmpty() ? code.getText() : with + "SELECT * FROM (" + code.getText() + ") AS tab";
    }


    /**
     * Row count estimated by the PostgreSQL planner for the SQL ({@code Plan Rows} of {@code EXPLAIN (FORMAT JSON)}),
     * explained under a savepoint, so that a failure does not abort the transaction of the request.
     *
     * @param sql the SQL to explain
     * @return row count estimated by the PostgreSQL planner for the SQL ({@code Plan Rows} of {@code EXPLAIN (FORMAT
     *         JSON)})
     * @throws SQLException on database errors
     */
    private long estimateRowCount(String sql) throws SQLException
    {
        ObjectMapper MAPPER = new ObjectMapper();

        String explainSql = "EXPLAIN (ANALYZE FALSE, FORMAT JSON) " + sql;

        Statement statement = request.getStatement();
        String json;

        statement.execute("SAVEPOINT estimate");

        try(ResultSet rs = statement.executeQuery(explainSql))
        {
            if(!rs.next())
                throw new SQLException("empty result");

            json = rs.getString(1);
        }
        catch(SQLException e)
        {
            statement.execute("ROLLBACK TO SAVEPOINT estimate");
            throw e;
        }

        statement.execute("RELEASE SAVEPOINT estimate");

        try
        {
            JsonNode root = MAPPER.readTree(json);

            long planRows = root.path(0).path("Plan").path("Plan Rows").asLong(-1L);

            if(planRows < 0)
                throw new SQLException("missing the \"Plan Rows\" field");

            return planRows;
        }
        catch(Exception parseEx)
        {
            throw new SQLException("cannot parse explain json", parseEx);
        }
    }
}
