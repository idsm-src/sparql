package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intCompositeBlankNode;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



public abstract class IntBlankNodeClass extends BlankNodeClass
{
    protected IntBlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract List<Column> toColumns(IntBlankNode bnode);


    public abstract boolean match(Statement statement, IntBlankNode term);


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(intCompositeBlankNode);
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case IntBlankNode bnode -> match(statement, bnode);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(BlankNode term)
    {
        if(term instanceof IntBlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}
