package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class BlankNodeClass extends ResourceClass
{
    protected BlankNodeClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    public abstract List<Column> toColumns(Node node);


    @Override
    public final List<Column> toColumns(Statement statement, Node node)
    {
        return toColumns(node);
    }


    @Override
    public Column toExpression(Statement statement, Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Statement statement, Node node)
    {
        return node instanceof VariableOrBlankNode;
    }
}
