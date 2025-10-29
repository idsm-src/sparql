package cz.iocb.sparql.engine.translator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlDistinct;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class Estimator
{
    private final Request request;


    public Estimator(Request request)
    {
        this.request = request;
    }


    public long estimateDistinct(Request request, SqlIntercode imcode, String var) throws SQLException
    {
        Restrictions restrictions = new Restrictions();
        restrictions.add(var);

        SqlDistinct.create(request, imcode, Set.of(var)).optimize(request, restrictions, false, false);

        String code = imcode.translate(request);

        return estimateRowCount(code);
    }


    public long estimateDistinct(Request request, SqlIntercode imcode, Set<String> vars)
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
