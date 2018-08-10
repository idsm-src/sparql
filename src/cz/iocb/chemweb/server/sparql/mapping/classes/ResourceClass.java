package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



/**
 * ResourceClass describes how resources of described class are represented in SQL.
 */
public abstract class ResourceClass
{
    /**
     * Resource name
     */
    protected final String name;


    /**
     * SQL types of individual parts
     */
    protected final List<String> sqlTypes;


    /**
     * Result tags of individual result value parts
     */
    protected final List<ResultTag> resultTags;


    /**
     * Resource class constructor
     *
     * @param name Resource name
     * @param sqlTypes List of SQL types
     */
    protected ResourceClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        this.name = name;
        this.sqlTypes = sqlTypes;
        this.resultTags = resultTags;
    }


    /**
     * Gets the number of parts that represent values in SQL.
     *
     * @return Number of parts
     */
    public final int getPartsCount()
    {
        return sqlTypes.size();
    }


    /**
     * Gets the SQL type of the given part
     *
     * @param part Part index
     * @return SQL type
     */
    public final String getSqlType(int part)
    {
        return sqlTypes.get(part);
    }


    /**
     * Gets the name of the SQL column that stores the given part of values of the given SPARQL variable
     *
     * @param variable SPARQL variable name
     * @param part Part index
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
     * Get the SQL value of the given part for the given SPARQL query node
     *
     * @param node SPARQL query node
     * @param part Part index
     * @return SQL value
     */
    public abstract String getSqlValue(Node node, int part);


    /**
     * Get list of tags that represent final values in SQL.
     *
     * @return Result tags
     */
    public final List<ResultTag> getResultTags()
    {
        return resultTags;
    }


    /**
     * Gets the number of parts that represent final values in SQL.
     *
     * @return Number of parts
     */
    public final int getResultPartsCount()
    {
        return resultTags.size();
    }


    /**
     * Get the final SQL value of the given part for the given SPARQL variable
     *
     * @param variable SPARQL variable name
     * @param part Part index
     * @return SQL final value
     */
    public abstract String getResultValue(String variable, int part);


    /**
     * Check whether values from this class can match the given SPARQL query node
     *
     * @param node SPARQL query node
     * @return true if values from this class can match the given SPARQL query node
     */
    public abstract boolean match(Node node);


    /**
     * Get the name of the resource class
     *
     * @return resource class name
     */
    public final String getName()
    {
        return name;
    }
}
