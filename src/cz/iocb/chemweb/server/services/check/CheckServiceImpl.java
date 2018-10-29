package cz.iocb.chemweb.server.services.check;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.sparql.parser.ParseResult;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateResult;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.shared.services.check.CheckResult;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;



public class CheckServiceImpl extends RemoteServiceServlet implements CheckService
{
    private static final long serialVersionUID = 1L;

    private SparqlDatabaseConfiguration dbConfig;
    private Parser parser;


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String resourceName = config.getInitParameter("resource");

        if(resourceName == null || resourceName.isEmpty())
            throw new ServletException("Resource name is not set");

        try
        {
            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            dbConfig = (SparqlDatabaseConfiguration) context.lookup(resourceName);
            parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());
        }
        catch(NamingException e)
        {
            throw new ServletException(e);
        }

        super.init(config);
    }


    @Override
    public CheckResult check(String code)
    {
        CheckResult result = new CheckResult();

        /* parser */
        ParseResult parseResult = parser.tryParse(code);

        if(parseResult.getExceptions() != null)
            for(ParseException err : parseResult.getExceptions())
                addWarning(result, CheckerWarningFactory.create(err.getRange(), "error", err.getMessage()));

        if(parseResult.getResult() == null)
            return result;


        /* translator */
        TranslateResult translateResult = new TranslateVisitor(dbConfig).tryTranslate(parseResult.getResult());

        for(TranslateException err : translateResult.getExceptions())
            addWarning(result, CheckerWarningFactory.create(err.getRange(), "error", err.getErrorMessage()));

        for(TranslateException err : translateResult.getWarnings())
            addWarning(result, CheckerWarningFactory.create(err.getRange(), "warning", err.getErrorMessage()));

        return result;
    }


    private static void addWarning(CheckResult result, CheckerWarning warning)
    {
        if(warning != null)
            result.warnings.add(warning);
    }
}
