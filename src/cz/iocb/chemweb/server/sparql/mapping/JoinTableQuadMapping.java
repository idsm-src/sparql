package cz.iocb.chemweb.server.sparql.mapping;

import java.util.Arrays;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;



public class JoinTableQuadMapping extends QuadMapping
{
    public static class JoinColumns
    {
        private List<Column> leftColumns;
        private List<Column> rightColumns;

        public JoinColumns(List<Column> leftColumns, List<Column> rightColumns)
        {
            this.leftColumns = leftColumns;
            this.rightColumns = rightColumns;

            assert leftColumns.size() == rightColumns.size();
        }

        public JoinColumns(String leftColumn, String rightColumn)
        {
            this.leftColumns = Arrays.asList(new TableColumn(leftColumn));
            this.rightColumns = Arrays.asList(new TableColumn(rightColumn));
        }

        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
                return true;

            if(obj == null || getClass() != obj.getClass())
                return false;

            JoinColumns pair = (JoinColumns) obj;

            if(!leftColumns.equals(pair.leftColumns))
                return false;

            if(!rightColumns.equals(pair.rightColumns))
                return false;

            return true;
        }

        public List<Column> getLeftColumns()
        {
            return leftColumns;
        }

        public List<Column> getRightColumns()
        {
            return rightColumns;
        }
    }


    private final List<Table> tables;
    private final List<JoinColumns> joinColumnsPairs;
    private final List<String> conditions;


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object, List<String> conditions)
    {
        super(graph, subject, predicate, object);

        this.tables = tables;
        this.joinColumnsPairs = joinColumnsPairs;
        this.conditions = conditions;

        assert tables.size() == joinColumnsPairs.size() + 1;
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object)
    {
        this(tables, joinColumnsPairs, graph, subject, predicate, object, null);
    }


    public final List<Table> getTables()
    {
        return tables;
    }


    public final List<JoinColumns> getJoinColumnsPairs()
    {
        return joinColumnsPairs;
    }


    public final List<String> getConditions()
    {
        return conditions;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(!super.equals(obj))
            return false;

        JoinTableQuadMapping mapping = (JoinTableQuadMapping) obj;

        if(!tables.equals(mapping.tables))
            return false;

        if(!joinColumnsPairs.equals(mapping.joinColumnsPairs))
            return false;

        if(conditions == null ? mapping.conditions != null : !conditions.equals(mapping.conditions))
            return false;

        return true;
    }
}
