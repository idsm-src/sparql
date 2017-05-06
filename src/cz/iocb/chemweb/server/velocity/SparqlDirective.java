package cz.iocb.chemweb.server.velocity;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
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
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.Prefixes;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.VirtuosoDatabase;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.translator.config.Config;
import cz.iocb.chemweb.server.sparql.translator.config.ConfigFileParseException;
import cz.iocb.chemweb.server.sparql.translator.config.ConfigFileParser;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.translator.visitor.PropertyChains;
import cz.iocb.chemweb.server.sparql.translator.visitor.SparqlTranslateVisitor;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class SparqlDirective extends Directive
{
    private Log log;

    private final Config procedures;
    final Map<String, List<String>> propertyChains;
    private final Parser parser;
    private final VirtuosoDatabase db;


    public SparqlDirective() throws FileNotFoundException, IOException, ConfigFileParseException, SQLException,
            PropertyVetoException, DatabaseException
    {
        procedures = ConfigFileParser.parse(Utils.getConfigDirectory() + "/procedureCalls.ini");
        propertyChains = PropertyChains.get();
        parser = new Parser(procedures, Prefixes.getPrefixes());

        db = new VirtuosoDatabase();
    }

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
            List<String> translatedQuery = new SparqlTranslateVisitor(propertyChains).translate(syntaxTree, procedures);
            Result result = db.query(translatedQuery);

            context.put(varName, result);
            return true;
        }
        catch (DatabaseException | ParseExceptions | TranslateExceptions e)
        {

            log.error("sparql directive: " + e.getMessage());
            System.err.println(query);
            e.printStackTrace();

            context.put(varName, null);
            return false;
        }
    }
}
