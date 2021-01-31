package cz.iocb.chemweb.server.sparql.mapping.extension;

import java.util.Arrays;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class ResultDefinition
{
    private final String resultName;
    private final ResourceClass resultClass;
    private final List<Column> sqlTypeFields;


    public ResultDefinition(String resultName, ResourceClass resultClass, List<Column> sqlTypeFields)
    {
        this.resultName = resultName;
        this.resultClass = resultClass;
        this.sqlTypeFields = sqlTypeFields;
    }


    public ResultDefinition(String resultName, ResourceClass resultClass, String sqlTypeField)
    {
        List<Column> sqlTypeFields = Arrays.asList(new TableColumn(sqlTypeField));

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


    public final List<Column> getSqlTypeFields()
    {
        return sqlTypeFields;
    }
}
