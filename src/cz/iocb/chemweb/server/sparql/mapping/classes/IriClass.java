package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import java.util.List;
import cz.iocb.chemweb.server.db.schema.Column;



public abstract class IriClass extends ResourceClass
{
    protected IriClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return iri;
    }


    public abstract String getIriValueCode(List<Column> columns);
}
