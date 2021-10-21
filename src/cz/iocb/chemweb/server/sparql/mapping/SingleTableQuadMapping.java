package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess.Condition;



public class SingleTableQuadMapping extends QuadMapping
{
    private final Table table;
    private final Condition condition;


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject,
            ConstantIriMapping predicate, NodeMapping object)
    {
        this(table, graph, subject, predicate, object, null);
    }


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject,
            ConstantIriMapping predicate, NodeMapping object, Condition condition)
    {
        super(graph, subject, predicate, object);

        this.table = table;
        this.condition = condition;
    }


    public final Table getTable()
    {
        return table;
    }


    public final Condition getCondition()
    {
        return condition;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!super.equals(object))
            return false;

        SingleTableQuadMapping mapping = (SingleTableQuadMapping) object;

        if(table == null ? mapping.table != null : !table.equals(mapping.table))
            return false;

        if(condition == null ? mapping.condition != null : !condition.equals(mapping.condition))
            return false;

        return true;
    }
}
