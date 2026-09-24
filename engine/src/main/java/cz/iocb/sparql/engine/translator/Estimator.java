package cz.iocb.sparql.engine.translator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.imcode.SqlDistinct;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
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
     * @param imcode the intermediate code
     * @param var the variable
     * @return estimated number of distinct values of the variable in the solutions of the intermediate code
     * @throws SQLException on database errors
     */
    public long estimateDistinct(Request request, SqlIntercode imcode, Variable var) throws SQLException
    {
        Restrictions restrictions = new Restrictions();
        restrictions.add(var);

        SqlDistinct.create(request, imcode, Set.of(var)).optimize(request, restrictions, false, false);

        String code = imcode.translate(request);

        return estimateRowCount(code);
    }


    /**
     * Estimated number of distinct value combinations of the variables in the solutions of the intermediate code;
     * {@link Long#MAX_VALUE} when the estimate fails.
     *
     * @param request the current request
     * @param imcode the intermediate code
     * @param vars the variables
     * @return estimated number of distinct value combinations of the variables in the solutions of the intermediate
     *         code; {@link Long#MAX_VALUE} when the estimate fails
     */
    public long estimateDistinct(Request request, SqlIntercode imcode, Set<Variable> vars)
    {
        if(vars.isEmpty())
            return 0;

        try
        {
            Restrictions restrictions = new Restrictions(vars);

            SqlDistinct.create(request, imcode, vars).optimize(request, restrictions, false, false);

            String code = imcode.translate(request);

            return estimateRowCount(code);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return Long.MAX_VALUE;
        }
    }


    /**
     * Row count estimated by the PostgreSQL planner for the SQL ({@code Plan Rows} of {@code EXPLAIN (FORMAT JSON)}).
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

        try(ResultSet rs = request.getStatement().executeQuery(explainSql))
        {
            if(!rs.next())
                throw new SQLException("empty result");

            String json = rs.getString(1);

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
}
