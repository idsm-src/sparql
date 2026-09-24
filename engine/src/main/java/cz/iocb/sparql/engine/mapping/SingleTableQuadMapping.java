package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.Table;



/**
 * Quad mapping over a single table (or none, for quads made of constants only), restricted by {@code conditions}. With
 * {@code distinct}, the table is declared to hold no two rows equal in the mapped columns, so the access needs no
 * deduplication when the whole set of mapped columns is selected.
 */
public class SingleTableQuadMapping extends QuadMapping
{
    /**
     * The table; null for constant-only quads.
     */
    private final Table table;

    /**
     * Conditions restricting the rows.
     */
    private final Conditions conditions;

    /**
     * True if the mapped columns hold no duplicate rows.
     */
    private final boolean distinct;


    /**
     * Creates an unconditional mapping.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public SingleTableQuadMapping(Table table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object)
    {
        this(table, graph, subject, predicate, object, new Conditions(true));
    }


    /**
     * Creates a mapping restricted by conditions.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     */
    public SingleTableQuadMapping(Table table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions)
    {
        this(table, graph, subject, predicate, object, conditions, false);
    }


    /**
     * Creates a mapping restricted by conditions, with the distinct flag.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     * @param distinct whether the mapping declares distinct rows
     */
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


    /**
     * The table; null for constant-only quads.
     *
     * @return the table; null for constant-only quads
     */
    public final Table getTable()
    {
        return table;
    }


    /**
     * Conditions restricting the rows.
     *
     * @return conditions restricting the rows
     */
    public final Conditions getConditions()
    {
        return conditions;
    }


    /**
     * True if the mapped columns hold no duplicate rows.
     *
     * @return true if the mapped columns hold no duplicate rows, false otherwise
     */
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
