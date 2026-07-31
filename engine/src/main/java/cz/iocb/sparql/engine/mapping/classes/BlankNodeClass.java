package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;



public abstract class BlankNodeClass extends PrimitiveResourceClass
{
    protected BlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract List<Column> toColumns(BlankNode bnode);


    @Override
    public final List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(term instanceof BlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}
