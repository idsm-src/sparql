package cz.iocb.chemweb.server.sparql.semanticcheck;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.semanticcheck.error.SemanticError;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.PropertiesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.VisitorCheckInferredTypes;
import cz.iocb.chemweb.server.sparql.translator.config.Config;



/**
 * Semantic checker provides methods to check SPARQL query for semantic errors.
 *
 */
public class SemanticChecker
{

    /** ontology info about classes */
    private final ClassesInfo classInfo;
    /** ontology info about properties */
    private final PropertiesInfo propInfo;
    /** parses must be not null if user wants to use check on string query */
    private Parser parser;
    /** visitor of the query */
    VisitorCheckInferredTypes visitor = null;

    /**
     * Create checker without parser info and external procedures.
     *
     * @param classInfo ontology classes info
     * @param propInfo ontology properties info
     */
    public SemanticChecker(ClassesInfo classInfo, PropertiesInfo propInfo)
    {
        this.classInfo = classInfo;
        this.propInfo = propInfo;
        this.visitor = new VisitorCheckInferredTypes(propInfo, classInfo);
    }

    /**
     * Create checker with parser but without info about external procedures.
     *
     * @param classInfo ontology classes info
     * @param propInfo ontology properties info
     * @param parser parser object
     */
    public SemanticChecker(ClassesInfo classInfo, PropertiesInfo propInfo, Parser parser)
    {
        this.classInfo = classInfo;
        this.propInfo = propInfo;
        this.parser = parser;
        this.visitor = new VisitorCheckInferredTypes(propInfo, classInfo);
    }

    /**
     * Create checker without parser but with info about external procedures.
     *
     * @param classInfo ontology classes info
     * @param propInfo ontology properties info
     * @param ProceduresConfig procedure config
     */
    public SemanticChecker(ClassesInfo classInfo, PropertiesInfo propInfo, Config ProceduresConfig)
    {
        this.classInfo = classInfo;
        this.propInfo = propInfo;
        this.visitor = new VisitorCheckInferredTypes(propInfo, classInfo, ProceduresConfig);
    }

    /**
     * Create checker with parser and info about external procedures.
     *
     * @param classInfo ontology classes info
     * @param propInfo ontology properties info
     * @param parser parser
     * @param proceduresConfig procedures config
     */
    public SemanticChecker(ClassesInfo classInfo, PropertiesInfo propInfo, Parser parser, Config proceduresConfig)
    {
        this.classInfo = classInfo;
        this.propInfo = propInfo;
        this.parser = parser;
        this.visitor = new VisitorCheckInferredTypes(propInfo, classInfo, proceduresConfig);
    }

    /**
     * Check query in a string - parser must be set up to not null.
     *
     * @param query query string
     * @return errors
     */
    public HashSet<SemanticError> checkQuery(String query) throws ParseExceptions
    {
        if(query != null && this.visitor != null && this.propInfo != null && this.classInfo != null
                && this.parser != null)
        {
            SelectQuery parsed = this.parser.parse(query);
            return checkQuery(parsed);
        }
        else
            return null;
    }

    /**
     * Check query syntax tree.
     *
     * @param syntaxTree query tree
     * @return errors
     */
    public HashSet<SemanticError> checkQuery(SelectQuery syntaxTree)
    {
        if(this.visitor != null && this.propInfo != null && this.classInfo != null && syntaxTree != null)
        {
            visitor.check(syntaxTree);
            return visitor.getSemErrors();
        }
        else
            return null;
    }

    /**
     * @return the ClassesInfo object
     */
    public ClassesInfo getClassInfo()
    {
        return classInfo;
    }

    /**
     * @return the PropertiesInfo object
     */
    public PropertiesInfo getPropInfo()
    {
        return propInfo;
    }

    /**
     * @return the parser
     */
    public Parser getParser()
    {
        return parser;
    }

    /**
     * @param parser the parser to set
     */
    public void setParser(Parser parser)
    {
        this.parser = parser;
    }
}
