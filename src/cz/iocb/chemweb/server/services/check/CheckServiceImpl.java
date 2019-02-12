package cz.iocb.chemweb.server.services.check;

import java.sql.SQLException;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.sparql.SparqlEngine;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.check.CheckResult;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;



public class CheckServiceImpl extends RemoteServiceServlet implements CheckService
{
    private static final long serialVersionUID = 1L;

    private SparqlEngine engine;


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String resourceName = config.getInitParameter("resource");

        if(resourceName == null || resourceName.isEmpty())
            throw new ServletException("Resource name is not set");

        try
        {
            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            SparqlDatabaseConfiguration sparqlConfig = (SparqlDatabaseConfiguration) context.lookup(resourceName);
            engine = new SparqlEngine(sparqlConfig, null);
        }
        catch(NamingException e)
        {
            throw new ServletException(e);
        }

        super.init(config);
    }


    @Override
    public CheckResult check(String code) throws DatabaseException
    {
        try
        {
            CheckResult result = new CheckResult();

            List<TranslateMessage> messages = engine.check(code);

            for(TranslateMessage message : messages)
                addWarning(result, CheckerWarningFactory.create(message.getRange(), message.getCategory().getText(),
                        message.getMessage()));

            return result;
        }
        catch(SQLException e)
        {
            throw new DatabaseException(e);
        }
    }


    private static void addWarning(CheckResult result, CheckerWarning warning)
    {
        if(warning != null)
            result.warnings.add(warning);
    }
}
