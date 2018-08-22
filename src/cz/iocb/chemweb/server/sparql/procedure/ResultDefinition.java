package cz.iocb.chemweb.server.sparql.procedure;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public class ResultDefinition
{
    private final String resultName;
    private final PatternResourceClass resultClass;
    private final String[] sqlTypeFields;


    public ResultDefinition(String resultName, PatternResourceClass resultClass, String[] sqlTypeFields)
    {
        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeFields = sqlTypeFields;
    }


    public ResultDefinition(String resultName, PatternResourceClass resultClass, String sqlTypeField)
    {
        String[] sqlTypeFields = { sqlTypeField };

        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeFields = sqlTypeFields;
    }


    public ResultDefinition(PatternResourceClass resultClass)
    {
        this.resultName = null;
        this.resultClass = resultClass;
        this.sqlTypeFields = null;
    }


    public final String getResultName()
    {
        return this.resultName;
    }


    public final PatternResourceClass getResultClass()
    {
        return resultClass;
    }


    public final String[] getSqlTypeFields()
    {
        return sqlTypeFields;
    }
}
