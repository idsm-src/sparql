package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.engine.Request;



public abstract class UserIriClass extends IriClass
{
    protected UserIriClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    public abstract boolean match(String iri, Request request);


    public abstract boolean match(String iri, DataSource connectionPool);


    public abstract int getCheckCost();
}
