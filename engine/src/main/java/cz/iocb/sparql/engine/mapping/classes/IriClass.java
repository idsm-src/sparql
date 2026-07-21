package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class IriClass extends PrimitiveResourceClass
{
    protected IriClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract boolean match(Statement statement, IRI iri);


    public abstract List<Column> toColumns(Statement statement, IRI iri);


    public abstract String getPrefix(List<Column> columns);


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(iri);
    }


    @Override
    public final boolean match(Statement statement, Node node)
    {
        return switch(node)
        {
            case VariableOrBlankNode _ -> true;
            case IRI iri -> match(statement, iri);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, Node node)
    {
        if(node instanceof IRI iri)
            return toColumns(statement, iri);
        else
            throw new IllegalArgumentException();
    }


    public List<Column> toOrderColumns(List<Column> columns)
    {
        return toGeneralClass(iri, columns, true);
    }
}
