package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import cz.iocb.sparql.engine.parser.model.IRI;



public abstract class UserIriClass extends IriClass
{
    protected UserIriClass(String name, List<String> sqlTypes)
    {
        super(name, sqlTypes);
    }


    public abstract boolean match(Statement statement, IRI iri);


    public abstract int getCheckCost();


    protected static String sanitizeString(String value)
    {
        return "'" + value.replace("'", "''") + "'";
    }
}
