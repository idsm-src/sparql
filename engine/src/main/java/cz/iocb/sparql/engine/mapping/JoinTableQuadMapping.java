package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.Table;



public class JoinTableQuadMapping extends QuadMapping
{
    public static class JoinColumns
    {
        private final List<Column> leftColumns;
        private final List<Column> rightColumns;
        private final List<String> types;

        public JoinColumns(List<Column> leftColumns, List<Column> rightColumns, List<String> types)
        {
            this.leftColumns = leftColumns;
            this.rightColumns = rightColumns;
            this.types = types;

            assert leftColumns.size() == rightColumns.size();
        }

        public JoinColumns(Column leftColumn, Column rightColumn, String type)
        {
            this.leftColumns = List.of(leftColumn);
            this.rightColumns = List.of(rightColumn);
            this.types = List.of(type);
        }

        public List<Column> getLeftColumns()
        {
            return leftColumns;
        }

        public List<Column> getRightColumns()
        {
            return rightColumns;
        }

        public List<String> getTypes()
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


    private final List<Table> tables;
    private final List<JoinColumns> joinColumnsPairs;
    private final List<Conditions> conditions;
    private final List<Boolean> distinct;
    private final int graphTableIdx;
    private final int subjectTableIdx;
    private final int predicateTableIdx;
    private final int objectTableIdx;


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, int graphTableIdx,
            TermMapping graph, int subjectTableIdx, TermMapping subject, int predicateTableIdx, TermMapping predicate,
            int objectTableIdx, TermMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, graphTableIdx, graph, subjectTableIdx, subject, predicateTableIdx, predicate,
                objectTableIdx, object, conditions, Collections.nCopies(tables.size(), false));
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, int graphTableIdx,
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


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions);
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions,
            List<Boolean> distinct)
    {
        this(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions,
                distinct);
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
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


    public final List<Table> getTables()
    {
        return tables;
    }


    public final List<JoinColumns> getJoinColumnsPairs()
    {
        return joinColumnsPairs;
    }


    public final List<Conditions> getConditions()
    {
        return conditions;
    }


    public final List<Boolean> getDistinct()
    {
        return distinct;
    }


    public final int getSubjectTableIdx()
    {
        return subjectTableIdx;
    }


    public final int getPredicateTableIdx()
    {
        return predicateTableIdx;
    }


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
