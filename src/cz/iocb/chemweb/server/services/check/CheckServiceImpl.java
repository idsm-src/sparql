package cz.iocb.chemweb.server.services.check;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.Prefixes;
import cz.iocb.chemweb.server.sparql.parser.ParseResult;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.semanticcheck.SemanticChecker;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.SemanticError;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfoFromOwl;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.PropertiesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.PropertiesInfoFromOwl;
import cz.iocb.chemweb.server.sparql.translator.TranslateResult;
import cz.iocb.chemweb.server.sparql.translator.config.Config;
import cz.iocb.chemweb.server.sparql.translator.config.ConfigFileParseException;
import cz.iocb.chemweb.server.sparql.translator.config.ConfigFileParser;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.visitor.PropertyChains;
import cz.iocb.chemweb.server.sparql.translator.visitor.SparqlTranslateVisitor;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.check.CheckResult;
import cz.iocb.chemweb.shared.services.check.CheckService;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;



public class CheckServiceImpl extends RemoteServiceServlet implements CheckService
{
    private static final long serialVersionUID = 1L;

    private final SemanticChecker semchecker;
    private final Parser parser;
    final Config proceduresConfig;
    final Map<String, List<String>> propertyChains;


    public CheckServiceImpl() throws FileNotFoundException, IOException, ClassNotFoundException,
            ConfigFileParseException, PropertyVetoException, DatabaseException, SQLException
    {
        proceduresConfig = ConfigFileParser.parse(Utils.getConfigDirectory() + "/procedureCalls.ini");
        propertyChains = PropertyChains.get();
        ClassesInfo classesInfo = ClassesInfoFromOwl.getInstance();
        PropertiesInfo propertiesInfo = PropertiesInfoFromOwl.getInstance();

        parser = new Parser(proceduresConfig, Prefixes.getPrefixes());
        semchecker = new SemanticChecker(classesInfo, propertiesInfo, parser, proceduresConfig);
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


        /* ontology checker */
        HashSet<SemanticError> errs = semchecker.checkQuery(parseResult.getResult());

        if(errs != null)
            for(SemanticError err : errs)
                addWarning(result, CheckerWarningFactory.create(err.getRange(), "warning", err.getErrorMessage()));


        /* translator */
        TranslateResult translateResult = new SparqlTranslateVisitor(propertyChains)
                .tryTranslate(parseResult.getResult(), proceduresConfig, false);

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
