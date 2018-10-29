package cz.iocb.chemweb.server.velocity;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class SparqlDirective extends Directive
{
    public static final String SPARQL_CONFIG = "SPARQL_CONFIG";

    private SparqlDatabaseConfiguration dbConfig;
    private Parser parser;
    private PostgresDatabase db;
    private Log log;


    @Override
    public String getName()
    {
        return "sparql";
    }


    @Override
    public int getType()
    {
        return BLOCK;
    }


    @Override
    public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException
    {
        super.init(rs, context, node);
        log = rs.getLog();

        dbConfig = (SparqlDatabaseConfiguration) rs.getApplicationAttribute(SPARQL_CONFIG);
        parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());
        db = new PostgresDatabase(dbConfig.getConnectionPool());
    }


    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        if(node.jjtGetNumChildren() != 2)
        {
            log.error("sparql directive: wrong number of arguments");
            return false;
        }


        SimpleNode sn = (SimpleNode) node.jjtGetChild(0);
        String varName = ((ASTReference) sn).getRootString();

        StringWriter blockContent = new StringWriter();
        node.jjtGetChild(node.jjtGetNumChildren() - 1).render(context, blockContent);
        String query = blockContent.toString();

        try
        {
            SelectQuery syntaxTree = parser.parse(query);
            String code = new TranslateVisitor(dbConfig).translate(syntaxTree);

            Result result = db.query(code);

            context.put(varName, result);
            return true;
        }
        catch(DatabaseException | ParseExceptions | TranslateExceptions e)
        {
            log.error("sparql directive: " + e.getMessage());

            context.put(varName, null);
            return false;
        }
    }
}
