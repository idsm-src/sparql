package cz.iocb.sparql.engine.mapping.classes;

import static java.util.stream.Collectors.joining;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class UnionResourceClass extends CompoundResourceClass
{
    private final Set<ResourceClass> resClasses;
    private final Set<ResultResourceClass> resultClasses;


    UnionResourceClass(Set<ResourceClass> resClasses, PrimitiveResourceClass effectiveClass)
    {
        //TODO: use better (i.e. safer) approach
        String name = resClasses.stream().map(r -> r.getName()).sorted().collect(joining("@", "union@", ""));

        super(name, effectiveClass);

        this.resClasses = new HashSet<>(resClasses);

        this.resultClasses = resClasses.stream().flatMap(r -> r.getResultResourceClasses().stream())
                .collect(Collectors.toSet());
    }


    @Override
    public boolean match(Statement statement, Node node)
    {
        return resClasses.stream().anyMatch(r -> r.match(statement, node));
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        throw new IllegalArgumentException();
    }


    public Set<ResourceClass> getClasses()
    {
        return resClasses;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return resultClasses;
    }


    @Override
    public int hashCode()
    {
        return resClasses.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        UnionResourceClass other = (UnionResourceClass) object;

        return Objects.equals(resClasses, other.resClasses);
    }
}
