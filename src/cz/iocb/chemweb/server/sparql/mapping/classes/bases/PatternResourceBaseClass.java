package cz.iocb.chemweb.server.sparql.mapping.classes.bases;

import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;



public abstract class PatternResourceBaseClass extends ResourceBaseClass implements PatternResourceClass
{
    protected final List<String> sqlTypes;
    protected final List<ResultTag> resultTags;


    protected PatternResourceBaseClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name);
        this.sqlTypes = sqlTypes;
        this.resultTags = resultTags;
    }


    @Override
    public final int getPartsCount()
    {
        return sqlTypes.size();
    }


    @Override
    public final String getSqlType(int part)
    {
        return sqlTypes.get(part);
    }


    @Override
    public final String getSqlColumn(String variable, int part)
    {
        if(sqlTypes.size() > 1)
            return '"' + variable + '#' + name + "-par" + part + '"';
        else
            return '"' + variable + '#' + name + '"';
    }


    @Override
    public final List<ResultTag> getResultTags()
    {
        return resultTags;
    }


    @Override
    public final int getResultPartsCount()
    {
        return resultTags.size();
    }
}
