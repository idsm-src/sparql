package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.Table;



public class SingleTableQuadMapping extends QuadMapping
{
    private final Table table;
    private final Conditions conditions;
    private final boolean distinct;


    public SingleTableQuadMapping(Table table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object)
    {
        this(table, graph, subject, predicate, object, new Conditions(true));
    }


    public SingleTableQuadMapping(Table table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions)
    {
        this(table, graph, subject, predicate, object, conditions, false);
    }


    public SingleTableQuadMapping(Table table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions, boolean distinct)
    {
        super(graph, subject, predicate, object);

        this.table = table;
        this.conditions = conditions;
        this.distinct = distinct;
    }


    @Override
    public QuadMapping asDefaultGraphMapping()
    {
        return new SingleTableQuadMapping(table, null, getSubject(), getPredicate(), getObject(), conditions, distinct);
    }


    @Override
    public QuadMapping asDefaultGraphMapping(Conditions graphConditions)
    {
        return new SingleTableQuadMapping(table, null, getSubject(), getPredicate(), getObject(),
                Conditions.and(conditions, graphConditions), distinct);
    }


    @Override
    public QuadMapping asNamedGraphMapping(Conditions graphConditions)
    {
        return new SingleTableQuadMapping(table, getGraph(), getSubject(), getPredicate(), getObject(),
                Conditions.and(conditions, graphConditions), distinct);
    }


    public final Table getTable()
    {
        return table;
    }


    public final Conditions getConditions()
    {
        return conditions;
    }


    public final boolean isDistinct()
    {
        return distinct;
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

        if(distinct != mapping.distinct)
            return false;

        return true;
    }
}
