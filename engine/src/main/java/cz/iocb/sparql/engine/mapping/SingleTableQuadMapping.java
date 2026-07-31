package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.Table;



public class SingleTableQuadMapping extends QuadMapping
{
    private final Table table;
    private final Conditions conditions;


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object)
    {
        this(table, graph, subject, predicate, object, new Conditions(true));
    }


    public SingleTableQuadMapping(Table table, ConstantIriMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions)
    {
        super(graph, subject, predicate, object);

        this.table = table;
        this.conditions = conditions;
    }


    @Override
    public QuadMapping asDefaultGraphMapping()
    {
        return new SingleTableQuadMapping(table, null, getSubject(), getPredicate(), getObject(), conditions);
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
