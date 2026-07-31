package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strCompositeBlankNode;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.Variable;



public abstract class StrBlankNodeClass extends BlankNodeClass
{
    protected StrBlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract List<Column> toColumns(StrBlankNode bnode);


    public abstract boolean match(Statement statement, StrBlankNode term);


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(strCompositeBlankNode);
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case StrBlankNode bnode -> match(statement, bnode);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(BlankNode term)
    {
        if(term instanceof StrBlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}
