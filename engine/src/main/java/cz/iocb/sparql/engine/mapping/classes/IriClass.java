package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



public abstract class IriClass extends PrimitiveResourceClass
{
    protected IriClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract boolean match(Statement statement, Iri iri);


    public abstract List<Column> toColumns(Statement statement, Iri iri);


    public abstract String getPrefix(List<Column> columns);


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(iri);
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Iri iri -> match(statement, iri);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(term instanceof Iri iri)
            return toColumns(statement, iri);
        else
            throw new IllegalArgumentException();
    }


    public List<Column> toOrderColumns(List<Column> columns)
    {
        return toGeneralClass(iri, columns, true);
    }
}
