package cz.iocb.chemweb.server.sparql;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import org.antlr.v4.runtime.ParserRuleContext;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.db.postgresql.PostgresHandler;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.MessageCategory;
import cz.iocb.chemweb.server.sparql.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.visitor.QueryVisitor;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;



public class SparqlEngine
{
    private final SparqlDatabaseConfiguration config;
    private final SSLContext sslContext;
    private final PostgresDatabase db;


    public SparqlEngine(SparqlDatabaseConfiguration config, SSLContext sslContext)
    {
        this.config = config;
        this.sslContext = sslContext;
        this.db = new PostgresDatabase(config.getConnectionPool());
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int timeout,
            PostgresHandler handler) throws TranslateExceptions, SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        Parser parser = new Parser(messages);
        ParserRuleContext context = parser.parse(query);

        checkForErrors(messages);

        QueryVisitor queryVisitor = new QueryVisitor(config, messages);
        SelectQuery syntaxTree = (SelectQuery) queryVisitor.visit(context);

        checkForErrors(messages);

        if(dataSets != null && !dataSets.isEmpty())
            syntaxTree.getSelect().setDataSets(dataSets);

        if(limit >= 0)
        {
            Select originalSelect = syntaxTree.getSelect();
            Select limitedSelect = new Select();
            limitedSelect.getDataSets().addAll(originalSelect.getDataSets());
            limitedSelect.setPattern(originalSelect);
            limitedSelect.setOffset(BigInteger.valueOf(offset));
            limitedSelect.setLimit(BigInteger.valueOf(limit + 1));
            syntaxTree.setSelect(limitedSelect);
        }

        TranslateVisitor translateVisitor = new TranslateVisitor(config, sslContext, messages, true);
        String code = translateVisitor.translate(syntaxTree);

        checkForErrors(messages);

        return db.query(code, timeout, handler);
    }


    public Result execute(String query) throws TranslateExceptions, SQLException
    {
        return execute(query, null, -1, -1, 0, null);
    }


    public Result execute(String query, List<DataSet> dataSets) throws TranslateExceptions, SQLException
    {
        return execute(query, dataSets, -1, -1, 0, null);
    }


    public Result execute(String query, int offset, int limit, int timeout, PostgresHandler handler)
            throws TranslateExceptions, SQLException
    {
        return execute(query, null, offset, limit, timeout, handler);
    }


    private void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
    {
        List<TranslateMessage> errors = messages.stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .collect(Collectors.toList());

        if(!errors.isEmpty())
            throw new TranslateExceptions(errors);
    }


    public List<TranslateMessage> check(String query) throws SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        try
        {
            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            QueryVisitor visitor = new QueryVisitor(config, messages);
            SelectQuery syntaxTree = (SelectQuery) visitor.visit(context);

            new TranslateVisitor(config, sslContext, messages, false).translate(syntaxTree);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }

        return messages;
    }


    public PostgresHandler getHandler()
    {
        return db.getHandler();
    }
}
