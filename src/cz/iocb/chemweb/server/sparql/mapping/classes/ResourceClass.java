package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



/**
 * Describes a class of SPARQL resources.
 *
 * For each SPARQL resource class, there are three representations of its values:
 * <ul>
 * <li>pattern representation</li>
 * <li>expression representation</li>
 * <li>result representation</li>
 * </ul>
 */
public abstract class ResourceClass
{
    protected final String name;
    protected final List<String> sqlTypes;
    protected final List<ResultTag> resultTags;


    protected ResourceClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        this.name = name;
        this.sqlTypes = sqlTypes;
        this.resultTags = resultTags;
    }


    /**
     * Gets the name of the resource class.
     *
     * @return resource class name
     */
    public final String getName()
    {
        return name;
    }


    /**
     * Gets the general resource class corresponding to this resource class.
     *
     * @return corresponding general resource class
     */
    public abstract ResourceClass getGeneralClass();


    /**
     * Gets the number of parts of the pattern representation.
     *
     * @return number of parts
     */
    public final int getPatternPartsCount()
    {
        return sqlTypes.size();
    }


    /**
     * Gets the SQL type of the given part of the pattern representation.
     *
     * @param part part index
     * @return SQL type
     */
    public final String getSqlType(int part)
    {
        return sqlTypes.get(part);
    }


    /**
     * Gets the name of the SQL column that stores the given part of the pattern representation for the given SPARQL
     * variable.
     *
     * @param variable SPARQL variable name
     * @param part part index
     * @return SQL column name
     */
    public final String getSqlColumn(String variable, int part)
    {
        if(sqlTypes.size() > 1)
            return '"' + variable + '#' + name + "-par" + part + '"';
        else
            return '"' + variable + '#' + name + '"';
    }


    /**
     * Gets list of tags that represent final values in SQL.
     *
     * @return result tags
     */
    public final List<ResultTag> getResultTags()
    {
        return resultTags;
    }


    /**
     * Gets the number of parts of the result representation.
     *
     * @return number of parts
     */
    public final int getResultPartsCount()
    {
        return resultTags.size();
    }


    /**
     * Gets SQL code to obtain the given part of the pattern representation for the given SPARQL node.
     *
     * @param node SPARQL node
     * @param part part index
     * @return SQL code
     */
    public abstract String getPatternCode(Node node, int part);


    /**
     * Gets SQL code to obtain the given part of the pattern representation of corresponding general resource class for
     * the given SPARQL variable in the given table.
     *
     * @param table table name
     * @param var variable name
     * @param part part index
     * @param check generate case checks
     * @return SQL code
     */
    public abstract String getGeneralisedPatternCode(String table, String var, int part, boolean check);


    /**
     * Gets SQL code to obtain the given part of the pattern representation for the given general resource SPARQL
     * variable in the given table.
     *
     * @param table table name
     * @param var variable name
     * @param part part index
     * @return SQL code
     */
    public abstract String getSpecialisedPatternCode(String table, String var, int part);


    /**
     * Gets SQL code to obtain the given part of the pattern representation for expression values stored in the given
     * column.
     *
     * @param column SQL column name
     * @param part part index
     * @param isBoxed indicates whether expression type is boxed
     * @return SQL code
     */
    public abstract String getPatternCode(String column, int part, boolean isBoxed);


    /**
     * Gets SQL code to obtain the expression representation for the pattern values stored in the given SPARQL variable.
     *
     * @param variable SPARQL variable name
     * @param variableAccessor variable accessor
     * @param rdfbox indicates whether expression values have to be boxed
     * @return SQL code
     */
    public abstract String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox);


    /**
     * Gets SQL code to obtain the given part of the result representation for the given SPARQL variable.
     *
     * @param variable SPARQL variable name
     * @param part part index
     * @return SQL code
     */
    public abstract String getResultCode(String variable, int part);


    /**
     * Check whether values from this class can match the given SPARQL node
     *
     * @param node SPARQL node
     * @return true if values from this class can match the given SPARQL node
     */
    public abstract boolean match(Node node, Request request);
}
