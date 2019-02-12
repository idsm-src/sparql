package cz.iocb.chemweb.server.sparql.mapping.procedure;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class ResultDefinition
{
    private final String resultName;
    private final ResourceClass resultClass;
    private final String[] sqlTypeFields;


    public ResultDefinition(String resultName, ResourceClass resultClass, String[] sqlTypeFields)
    {
        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeFields = sqlTypeFields;
    }


    public ResultDefinition(String resultName, ResourceClass resultClass, String sqlTypeField)
    {
        String[] sqlTypeFields = { sqlTypeField };

        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeFields = sqlTypeFields;
    }


    public ResultDefinition(ResourceClass resultClass)
    {
        this.resultName = null;
        this.resultClass = resultClass;
        this.sqlTypeFields = null;
    }


    public final String getResultName()
    {
        return this.resultName;
    }


    public final ResourceClass getResultClass()
    {
        return resultClass;
    }


    public final String[] getSqlTypeFields()
    {
        return sqlTypeFields;
    }
}
