package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * Quad mapping over a chain of tables, adjacent ones joined on {@link JoinColumns}. Each term position names by index
 * the table it is taken from (the convenience constructors take graph, subject and predicate from the first table and
 * the object from the last one), and every table has its own conditions and distinct flag (see
 * {@link SingleTableQuadMapping}).
 */
public class JoinTableQuadMapping extends QuadMapping
{
    /**
     * Columns joining two adjacent tables of the chain (left table columns to right table columns), with the SQL type
     * of each pair.
     */
    public static class JoinColumns
    {
        /**
         * Columns of the left table.
         */
        private final List<Column> leftColumns;

        /**
         * Columns of the right table, matched by position.
         */
        private final List<Column> rightColumns;

        /**
         * SQL type of each column pair.
         */
        private final List<SqlType> types;

        /**
         * Creates the join on several column pairs.
         *
         * @param leftColumns columns of the left table
         * @param rightColumns columns of the right table
         * @param types SQL types of the column pairs
         */
        public JoinColumns(List<Column> leftColumns, List<Column> rightColumns, List<SqlType> types)
        {
            this.leftColumns = leftColumns;
            this.rightColumns = rightColumns;
            this.types = types;

            assert leftColumns.size() == rightColumns.size();
        }


        /**
         * Creates the join on a single column pair.
         *
         * @param leftColumn column of the left table
         * @param rightColumn column of the right table
         * @param type the SQL type
         */
        public JoinColumns(Column leftColumn, Column rightColumn, SqlType type)
        {
            this.leftColumns = List.of(leftColumn);
            this.rightColumns = List.of(rightColumn);
            this.types = List.of(type);
        }


        /**
         * Columns of the left table.
         *
         * @return columns of the left table
         */
        public List<Column> getLeftColumns()
        {
            return leftColumns;
        }


        /**
         * Columns of the right table, matched by position.
         *
         * @return columns of the right table, matched by position
         */
        public List<Column> getRightColumns()
        {
            return rightColumns;
        }


        /**
         * SQL type of each column pair.
         *
         * @return SQL type of each column pair
         */
        public List<SqlType> getTypes()
        {
            return types;
        }

        @Override
        public int hashCode()
        {
            return leftColumns.hashCode() ^ rightColumns.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            JoinColumns pair = (JoinColumns) object;

            if(!leftColumns.equals(pair.leftColumns))
                return false;

            if(!rightColumns.equals(pair.rightColumns))
                return false;

            return true;
        }
    }


    /**
     * Tables of the chain in join order.
     */
    private final List<SourceTable> tables;

    /**
     * Join columns between adjacent tables; one fewer than tables.
     */
    private final List<JoinColumns> joinColumnsPairs;

    /**
     * Conditions on each table.
     */
    private final List<Conditions> conditions;

    /**
     * Distinct flag of each table.
     */
    private final List<Boolean> distinct;

    /**
     * Index of the table the graph is taken from.
     */
    private final int graphTableIdx;

    /**
     * Index of the table the subject is taken from.
     */
    private final int subjectTableIdx;

    /**
     * Index of the table the predicate is taken from.
     */
    private final int predicateTableIdx;

    /**
     * Index of the table the object is taken from.
     */
    private final int objectTableIdx;


    /**
     * Creates the mapping with explicit table indexes for every position and no distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graphTableIdx index of the table providing the graph
     * @param graph the graph mapping, or null for the default graph
     * @param subjectTableIdx index of the table providing the subject
     * @param subject the subject mapping
     * @param predicateTableIdx index of the table providing the predicate
     * @param predicate the predicate mapping
     * @param objectTableIdx index of the table providing the object
     * @param object the object mapping
     * @param conditions conditions on each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, int graphTableIdx,
            TermMapping graph, int subjectTableIdx, TermMapping subject, int predicateTableIdx, TermMapping predicate,
            int objectTableIdx, TermMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, graphTableIdx, graph, subjectTableIdx, subject, predicateTableIdx, predicate,
                objectTableIdx, object, conditions, Collections.nCopies(tables.size(), false));
    }


    /**
     * Creates the mapping with explicit table indexes for every position.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graphTableIdx index of the table providing the graph
     * @param graph the graph mapping, or null for the default graph
     * @param subjectTableIdx index of the table providing the subject
     * @param subject the subject mapping
     * @param predicateTableIdx index of the table providing the predicate
     * @param predicate the predicate mapping
     * @param objectTableIdx index of the table providing the object
     * @param object the object mapping
     * @param conditions conditions on each table
     * @param distinct distinct flag of each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, int graphTableIdx,
            TermMapping graph, int subjectTableIdx, TermMapping subject, int predicateTableIdx, TermMapping predicate,
            int objectTableIdx, TermMapping object, List<Conditions> conditions, List<Boolean> distinct)
    {
        super(graph, subject, predicate, object);

        this.tables = tables;
        this.joinColumnsPairs = joinColumnsPairs;
        this.conditions = conditions;
        this.distinct = distinct;
        this.graphTableIdx = graphTableIdx;
        this.subjectTableIdx = subjectTableIdx;
        this.predicateTableIdx = predicateTableIdx;
        this.objectTableIdx = objectTableIdx;

        assert tables.size() == joinColumnsPairs.size() + 1;
        assert tables.size() == conditions.size();
        assert tables.size() == distinct.size();
    }


    /**
     * Creates the mapping taking graph, subject and predicate from the first table and the object from the last one.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions);
    }


    /**
     * Creates the mapping taking graph, subject and predicate from the first table and the object from the last one,
     * with distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     * @param distinct distinct flag of each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions,
            List<Boolean> distinct)
    {
        this(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions,
                distinct);
    }


    /**
     * Creates the mapping taking graph, subject and predicate from the first table and the object from the last one,
     * without conditions.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object)
    {
        this(tables, joinColumnsPairs, graph, subject, predicate, object,
                Collections.nCopies(tables.size(), new Conditions(true)));
    }


    @Override
    public QuadMapping asDefaultGraphMapping()
    {
        return new JoinTableQuadMapping(tables, joinColumnsPairs, graphTableIdx, null, subjectTableIdx, getSubject(),
                predicateTableIdx, getPredicate(), objectTableIdx, getObject(), conditions, distinct);
    }


    @Override
    public QuadMapping asDefaultGraphMapping(Conditions graphConditions)
    {
        List<Conditions> newConditions = new ArrayList<>(conditions);
        newConditions.set(graphTableIdx, Conditions.and(conditions.get(graphTableIdx), graphConditions));

        return new JoinTableQuadMapping(tables, joinColumnsPairs, graphTableIdx, null, subjectTableIdx, getSubject(),
                predicateTableIdx, getPredicate(), objectTableIdx, getObject(), newConditions, distinct);
    }


    @Override
    public QuadMapping asNamedGraphMapping(Conditions graphConditions)
    {
        List<Conditions> newConditions = new ArrayList<>(conditions);
        newConditions.set(graphTableIdx, Conditions.and(conditions.get(graphTableIdx), graphConditions));

        return new JoinTableQuadMapping(tables, joinColumnsPairs, graphTableIdx, getGraph(), subjectTableIdx,
                getSubject(), predicateTableIdx, getPredicate(), objectTableIdx, getObject(), newConditions, distinct);
    }


    /**
     * Tables of the chain in join order.
     *
     * @return tables of the chain in join order
     */
    public final List<SourceTable> getTables()
    {
        return tables;
    }


    /**
     * Join columns between adjacent tables.
     *
     * @return join columns between adjacent tables
     */
    public final List<JoinColumns> getJoinColumnsPairs()
    {
        return joinColumnsPairs;
    }


    /**
     * Conditions on each table.
     *
     * @return conditions on each table
     */
    public final List<Conditions> getConditions()
    {
        return conditions;
    }


    /**
     * Distinct flag of each table.
     *
     * @return distinct flag of each table
     */
    public final List<Boolean> getDistinct()
    {
        return distinct;
    }


    /**
     * Index of the table the subject is taken from.
     *
     * @return index of the table the subject is taken from
     */
    public final int getGraphTableIdx()
    {
        return graphTableIdx;
    }


    public final int getSubjectTableIdx()
    {
        return subjectTableIdx;
    }


    /**
     * Index of the table the predicate is taken from.
     *
     * @return index of the table the predicate is taken from
     */
    public final int getPredicateTableIdx()
    {
        return predicateTableIdx;
    }


    /**
     * Index of the table the object is taken from.
     *
     * @return index of the table the object is taken from
     */
    public final int getObjectTableIdx()
    {
        return objectTableIdx;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!super.equals(object))
            return false;

        JoinTableQuadMapping mapping = (JoinTableQuadMapping) object;

        if(!tables.equals(mapping.tables))
            return false;

        if(!joinColumnsPairs.equals(mapping.joinColumnsPairs))
            return false;

        if(!conditions.equals(mapping.conditions))
            return false;

        if(!distinct.equals(mapping.distinct))
            return false;

        return true;
    }
}
