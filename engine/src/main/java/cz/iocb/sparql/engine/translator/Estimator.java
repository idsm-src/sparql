package cz.iocb.sparql.engine.translator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Estimates row counts of intermediate code by asking PostgreSQL for the plan of its SQL ({@code EXPLAIN}). The
 * estimates asked for together cost one round trip to the database (see {@link Request#lookup}), and an estimate is
 * asked for once in a request (see {@link Request#getRowEstimates}).
 */
public class Estimator
{
    /**
     * Parser of the plans the database gives.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

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
     * comparison of a column with a constant from the statistics of the column, which the engine keeps no like of. The
     * two estimates are asked for together. {@link Double#NaN} when the planner cannot be asked, because the code
     * refers to a solution it is evaluated for, so that it is no statement by itself, or when an estimate fails.
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
        SqlCode all = imcode.translate(request, scope);
        SqlCode kept = SqlFilter.filter(request, conditions, imcode).translate(request, scope);

        if(!all.getOuterTables().isEmpty() || !kept.getOuterTables().isEmpty())
            return Double.NaN;

        try
        {
            List<Long> rows = estimateRowCounts(List.of(translateStatement(all), translateStatement(kept)));

            return rows.get(0) <= 0 ? Double.NaN : Math.min((double) rows.get(1) / rows.get(0), 1);
        }
        catch(SQLException e)
        {
            return Double.NaN;
        }
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
        return translateStatement(imcode.translate(request, scope));
    }


    /**
     * SQL of a statement evaluating the code (see {@link #translateStatement(Scope, SqlIntercode)}).
     *
     * @param code SQL code of a query
     * @return SQL of a statement evaluating the code
     */
    private String translateStatement(SqlCode code)
    {
        String with = SqlSelect.translateWithClause(request, code);

        return with.isEmpty() ? code.getText() : with + "SELECT * FROM (" + code.getText() + ") AS tab";
    }


    /**
     * Row count estimated by the PostgreSQL planner for the SQL (see {@link #estimateRowCounts}).
     *
     * @param sql the SQL to explain
     * @return row count estimated by the PostgreSQL planner for the SQL
     * @throws SQLException on database errors
     */
    private long estimateRowCount(String sql) throws SQLException
    {
        return estimateRowCounts(List.of(sql)).get(0);
    }


    /**
     * Row counts estimated by the PostgreSQL planner for the statements ({@code Plan Rows} of
     * {@code EXPLAIN (FORMAT JSON)}). A statement is explained once in a request (see {@link Request#getRowEstimates}),
     * and the statements not explained yet are explained together, by one round trip to the database, in a way that
     * does not abort the transaction of the request when a statement fails (see {@link Request#lookup}).
     *
     * @param statements SQL of the statements
     * @return row counts estimated by the PostgreSQL planner for the statements, in their order
     * @throws SQLException on database errors
     */
    private List<Long> estimateRowCounts(List<String> statements) throws SQLException
    {
        Map<String, Long> estimates = request.getRowEstimates();

        List<String> unknown = statements.stream().filter(s -> !estimates.containsKey(s)).distinct().toList();

        List<Long> rows = request.lookup(
                unknown.stream().map(s -> "EXPLAIN (ANALYZE FALSE, FORMAT JSON) " + s).toList(),
                Estimator::readRowCount);

        for(int i = 0; i < unknown.size(); i++)
            estimates.put(unknown.get(i), rows.get(i));

        return statements.stream().map(estimates::get).toList();
    }


    /**
     * Row count the planner has estimated for a statement, read from its plan.
     *
     * @param result the result of {@code EXPLAIN (FORMAT JSON)}
     * @return row count the planner has estimated for the statement
     * @throws SQLException when the plan cannot be read
     */
    private static long readRowCount(ResultSet result) throws SQLException
    {
        if(!result.next())
            throw new SQLException("empty result");

        try
        {
            JsonNode root = mapper.readTree(result.getString(1));

            long planRows = root.path(0).path("Plan").path("Plan Rows").asLong(-1L);

            if(planRows < 0)
                throw new SQLException("missing the \"Plan Rows\" field");

            return planRows;
        }
        catch(JsonProcessingException e)
        {
            throw new SQLException("cannot parse explain json", e);
        }
    }
}
