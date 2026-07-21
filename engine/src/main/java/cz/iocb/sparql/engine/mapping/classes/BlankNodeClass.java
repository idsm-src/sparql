package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class BlankNodeClass extends PrimitiveResourceClass
{
    protected BlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract List<Column> toColumns(BlankNodeLiteral bnode);


    @Override
    public final boolean match(Statement statement, Node node)
    {
        return switch(node)
        {
            case VariableOrBlankNode _ -> true;
            case BlankNodeLiteral bnode -> bnode.getResourceClass().isSubclassOf(this);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, Node node)
    {
        if(node instanceof BlankNodeLiteral bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}
