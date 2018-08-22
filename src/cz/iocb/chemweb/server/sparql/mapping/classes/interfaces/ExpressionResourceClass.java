package cz.iocb.chemweb.server.sparql.mapping.classes.interfaces;



public interface ExpressionResourceClass extends ResourceClass
{
    /**
     * Get resource class representing pattern values of this resource class
     *
     * @return Expression resource class
     */
    public PatternResourceClass getPatternResourceClass();


    /**
     * Get the SQL value of the given column usable in patterns
     *
     * @param column SQL column name
     * @param part Part index
     * @param isBoxed indicates whether expression type has to be rdfbox
     * @return SQL value
     */
    public String getSqlPatternValue(String column, int part, boolean isBoxed);
}
