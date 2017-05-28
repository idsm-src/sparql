package cz.iocb.chemweb.server.sparql.procedure;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class ResultDefinition
{
    private final String resultName;
    private final ResourceClass resultClass;
    private final String sqlTypeField;


    public ResultDefinition(String resultName, ResourceClass resultClass, String sqlTypeField)
    {
        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeField = sqlTypeField;
    }


    public ResultDefinition(ResourceClass resultClass)
    {
        this.resultName = null;
        this.resultClass = resultClass;
        this.sqlTypeField = null;
    }


    public final String getResultName()
    {
        return this.resultName;
    }


    public final ResourceClass getResultClass()
    {
        return resultClass;
    }


    public final String getSqlTypeField()
    {
        return sqlTypeField;
    }
}
