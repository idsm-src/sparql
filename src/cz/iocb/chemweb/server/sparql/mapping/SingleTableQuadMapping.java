package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.database.Table;



public class SingleTableQuadMapping extends QuadMapping
{
    private final Table table;
    private final String condition;


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject,
            ConstantIriMapping predicate, NodeMapping object)
    {
        this(table, graph, subject, predicate, object, null);
    }


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject,
            ConstantIriMapping predicate, NodeMapping object, String condition)
    {
        super(graph, subject, predicate, object);

        this.table = table;
        this.condition = condition;
    }


    public final Table getTable()
    {
        return table;
    }


    public final String getCondition()
    {
        return condition;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(!super.equals(obj))
            return false;

        SingleTableQuadMapping mapping = (SingleTableQuadMapping) obj;

        if(table == null ? mapping.table != null : !table.equals(mapping.table))
            return false;

        if(condition == null ? mapping.condition != null : !condition.equals(mapping.condition))
            return false;

        return true;
    }
}
