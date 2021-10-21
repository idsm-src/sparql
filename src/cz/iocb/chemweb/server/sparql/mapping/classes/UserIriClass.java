package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public abstract class UserIriClass extends IriClass
{
    protected UserIriClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    public abstract boolean match(IRI iri);


    public abstract int getCheckCost();
}
