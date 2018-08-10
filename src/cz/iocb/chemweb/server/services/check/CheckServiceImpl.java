package cz.iocb.chemweb.server.services.check;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.parser.ParseResult;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateResult;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.check.CheckResult;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;



public class CheckServiceImpl extends RemoteServiceServlet implements CheckService
{
    private static final long serialVersionUID = 1L;

    private final SparqlDatabaseConfiguration dbConfig;
    private final Parser parser;


    public CheckServiceImpl() throws FileNotFoundException, IOException, ClassNotFoundException, PropertyVetoException,
            DatabaseException, SQLException
    {
        dbConfig = PubChemConfiguration.get();
        parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());
    }



    @Override
    public CheckResult check(String code)
    {
        CheckResult result = new CheckResult();

        try
        {
            /* parser */
            ParseResult parseResult = parser.tryParse(code);

            if(parseResult.getExceptions() != null)
                for(ParseException err : parseResult.getExceptions())
                    addWarning(result, CheckerWarningFactory.create(err.getRange(), "error", err.getMessage()));

            if(parseResult.getResult() == null)
                return result;


            /* translator */
            DatabaseSchema schema;
            schema = dbConfig.getSchema();
            LinkedHashMap<String, IriClass> classes = dbConfig.getIriClasses();
            List<QuadMapping> mappings = dbConfig.getMappings();
            LinkedHashMap<String, ProcedureDefinition> procedures = dbConfig.getProcedures();

            TranslateResult translateResult = new TranslateVisitor(classes, mappings, schema, procedures)
                    .tryTranslate(parseResult.getResult());

            for(TranslateException err : translateResult.getExceptions())
                addWarning(result, CheckerWarningFactory.create(err.getRange(), "error", err.getErrorMessage()));

            for(TranslateException err : translateResult.getWarnings())
                addWarning(result, CheckerWarningFactory.create(err.getRange(), "warning", err.getErrorMessage()));
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }


    private static void addWarning(CheckResult result, CheckerWarning warning)
    {
        if(warning != null)
            result.warnings.add(warning);
    }
}
