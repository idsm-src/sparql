package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.SourceTable;



/**
 * Quad mapping over several tables joined as a tree: every join ({@link JoinColumns}) joins two of the tables, and the
 * joins connect all the tables without a cycle, so there is one fewer of them than tables. A join that does not name
 * its tables joins two adjacent ones, the join at the position {@code i} of the list the tables {@code i} and
 * {@code i + 1}, so that joins given so make a chain. Each term position names by index the table it is taken from (the
 * convenience constructors take graph, subject and predicate from the first table and the object from the last one),
 * and every table has its own conditions and distinct flag (see {@link SingleTableQuadMapping}).
 */
public class JoinTableQuadMapping extends QuadMapping
{
    /**
     * Columns joining two tables of the mapping (left table columns to right table columns, matched by position and of
     * the same SQL types). The tables are named by their indexes among the tables of the mapping, or left to the
     * position of the join in the list of the joins, which then joins two adjacent tables (see {@link #at}).
     */
    public static class JoinColumns
    {
        /**
         * Index of the left table among the tables of the mapping, or -1 when the position of the join gives it.
         */
        private final int leftTable;

        /**
         * Columns of the left table.
         */
        private final List<Column> leftColumns;

        /**
         * Index of the right table among the tables of the mapping, or -1 when the position of the join gives it.
         */
        private final int rightTable;

        /**
         * Columns of the right table, matched by position.
         */
        private final List<Column> rightColumns;

        /**
         * Creates the join of two adjacent tables on several column pairs.
         *
         * @param leftColumns columns of the left table
         * @param rightColumns columns of the right table
         */
        public JoinColumns(List<Column> leftColumns, List<Column> rightColumns)
        {
            this(-1, leftColumns, -1, rightColumns);
        }


        /**
         * Creates the join of two adjacent tables on a single column pair.
         *
         * @param leftColumn column of the left table
         * @param rightColumn column of the right table
         */
        public JoinColumns(Column leftColumn, Column rightColumn)
        {
            this(-1, List.of(leftColumn), -1, List.of(rightColumn));
        }


        /**
         * Creates the join of two given tables of the mapping on several column pairs.
         *
         * @param leftTable index of the left table among the tables of the mapping
         * @param leftColumns columns of the left table
         * @param rightTable index of the right table among the tables of the mapping
         * @param rightColumns columns of the right table
         */
        public JoinColumns(int leftTable, List<Column> leftColumns, int rightTable, List<Column> rightColumns)
        {
            this.leftTable = leftTable;
            this.leftColumns = leftColumns;
            this.rightTable = rightTable;
            this.rightColumns = rightColumns;

            assert leftColumns.size() == rightColumns.size();
            assert (leftTable < 0) == (rightTable < 0);
        }


        /**
         * Creates the join of two given tables of the mapping on a single column pair.
         *
         * @param leftTable index of the left table among the tables of the mapping
         * @param leftColumn column of the left table
         * @param rightTable index of the right table among the tables of the mapping
         * @param rightColumn column of the right table
         */
        public JoinColumns(int leftTable, Column leftColumn, int rightTable, Column rightColumn)
        {
            this(leftTable, List.of(leftColumn), rightTable, List.of(rightColumn));
        }


        /**
         * The join naming its tables: the join itself when it names them, and the join of the tables {@code position}
         * and {@code position + 1} on its columns otherwise.
         *
         * @param position position of the join in the list of the joins of a mapping
         * @return the join naming its tables
         */
        public JoinColumns at(int position)
        {
            return leftTable >= 0 ? this : new JoinColumns(position, leftColumns, position + 1, rightColumns);
        }


        /**
         * Index of the left table among the tables of the mapping; -1 when the join does not name its tables (the joins
         * a mapping gives name them always).
         *
         * @return index of the left table among the tables of the mapping, or -1
         */
        public int getLeftTable()
        {
            return leftTable;
        }


        /**
         * Index of the right table among the tables of the mapping; -1 when the join does not name its tables (the
         * joins a mapping gives name them always).
         *
         * @return index of the right table among the tables of the mapping, or -1
         */
        public int getRightTable()
        {
            return rightTable;
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


        @Override
        public int hashCode()
        {
            return leftColumns.hashCode() ^ rightColumns.hashCode() ^ leftTable ^ 31 * rightTable;
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            JoinColumns pair = (JoinColumns) object;

            if(leftTable != pair.leftTable || rightTable != pair.rightTable)
                return false;

            if(!leftColumns.equals(pair.leftColumns))
                return false;

            if(!rightColumns.equals(pair.rightColumns))
                return false;

            return true;
        }
    }


    /**
     * Tables of the mapping.
     */
    private final List<SourceTable> tables;

    /**
     * Joins of the tables, each naming its two tables; one fewer than tables.
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
     * @param joinColumnsPairs the joins of the tables
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
     * @param joinColumnsPairs the joins of the tables
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

        assert tables.size() == joinColumnsPairs.size() + 1;

        List<JoinColumns> joins = new ArrayList<>(joinColumnsPairs.size());

        for(int i = 0; i < joinColumnsPairs.size(); i++)
            joins.add(joinColumnsPairs.get(i).at(i));

        this.tables = tables;
        this.joinColumnsPairs = Collections.unmodifiableList(joins);
        this.conditions = conditions;
        this.distinct = distinct;
        this.graphTableIdx = graphTableIdx;
        this.subjectTableIdx = subjectTableIdx;
        this.predicateTableIdx = predicateTableIdx;
        this.objectTableIdx = objectTableIdx;

        assert tables.size() == conditions.size();
        assert tables.size() == distinct.size();
    }


    /**
     * Creates the mapping taking graph, subject and predicate from the first table and the object from the last one.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions);
    }


    /**
     * Creates the mapping taking graph, subject and predicate from the first table and the object from the last one,
     * with distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     * @param distinct distinct flag of each table
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object, List<Conditions> conditions,
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
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public JoinTableQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object)
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
     * Tables of the mapping.
     *
     * @return tables of the mapping
     */
    public final List<SourceTable> getTables()
    {
        return tables;
    }


    /**
     * Joins of the tables, each naming its two tables (see {@link JoinColumns#getLeftTable} and
     * {@link JoinColumns#getRightTable}).
     *
     * @return joins of the tables, each naming its two tables
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
     * Index of the table the graph is taken from.
     *
     * @return index of the table the graph is taken from
     */
    public final int getGraphTableIdx()
    {
        return graphTableIdx;
    }


    /**
     * Index of the table the subject is taken from.
     *
     * @return index of the table the subject is taken from
     */
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

        if(graphTableIdx != mapping.graphTableIdx || subjectTableIdx != mapping.subjectTableIdx
                || predicateTableIdx != mapping.predicateTableIdx || objectTableIdx != mapping.objectTableIdx)
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
