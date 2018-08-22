package cz.iocb.chemweb.server.sparql.mapping.classes.interfaces;

import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public interface PatternResourceClass extends ResourceClass
{
    /**
     * Gets the number of parts that represent values in SQL.
     *
     * @return Number of parts
     */
    public int getPartsCount();


    /**
     * Gets the SQL type of the given part
     *
     * @param part Part index
     * @return SQL type
     */
    public String getSqlType(int part);


    /**
     * Gets the name of the SQL column that stores the given part of values of the given SPARQL variable
     *
     * @param variable SPARQL variable name
     * @param part Part index
     * @return SQL column name
     */
    public String getSqlColumn(String variable, int part);


    /**
     * Get the SQL value of the given part for the given SPARQL query node
     *
     * @param node SPARQL query node
     * @param part Part index
     * @return SQL value
     */
    public String getSqlValue(Node node, int part);


    /**
     * Get list of tags that represent final values in SQL.
     *
     * @return Result tags
     */
    public List<ResultTag> getResultTags();


    /**
     * Gets the number of parts that represent final values in SQL.
     *
     * @return Number of parts
     */
    public int getResultPartsCount();


    /**
     * Get the final SQL value of the given part for the given SPARQL variable
     *
     * @param variable SPARQL variable name
     * @param part Part index
     * @return SQL final value
     */
    public String getResultValue(String variable, int part);


    /**
     * Get resource class representing expression values of this resource class
     *
     * @return Expression resource class
     */
    public ExpressionResourceClass getExpressionResourceClass();


    /**
     * Get the SQL value for the given SPARQL variable usable in expressions
     *
     * @param variable SPARQL variable name
     * @param variableAccessor Variable accessor
     * @param rdfbox indicates whether expression type has to be rdfbox
     * @return SQL value
     */
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox);


    /**
     * Check whether values from this class can match the given SPARQL query node
     *
     * @param node SPARQL query node
     * @return true if values from this class can match the given SPARQL query node
     */
    public boolean match(Node node);
}
