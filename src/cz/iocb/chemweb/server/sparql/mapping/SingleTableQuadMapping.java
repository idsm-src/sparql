package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.database.Condition;
import cz.iocb.chemweb.server.sparql.database.Conditions;
import cz.iocb.chemweb.server.sparql.database.Table;



public class SingleTableQuadMapping extends QuadMapping
{
    private final Table table;
    private final Conditions conditions;


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, NodeMapping predicate,
            NodeMapping object)
    {
        this(table, graph, subject, predicate, object, new Conditions(new Condition()));
    }


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, NodeMapping predicate,
            NodeMapping object, Conditions conditions)
    {
        super(graph, subject, predicate, object);

        this.table = table;
        this.conditions = conditions;
    }


    public final Table getTable()
    {
        return table;
    }


    public final Conditions getConditions()
    {
        return conditions;
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

        if(!conditions.equals(mapping.conditions))
            return false;

        return true;
    }
}
