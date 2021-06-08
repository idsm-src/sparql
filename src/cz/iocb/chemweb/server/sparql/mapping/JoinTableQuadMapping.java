package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class JoinTableQuadMapping extends QuadMapping
{
    private class InternalResourceClass extends ResourceClass
    {
        public InternalResourceClass(int size)
        {
            super("internal", new ArrayList<String>(Collections.nCopies(size, "any")),
                    new ArrayList<ResultTag>(Collections.nCopies(size, ResultTag.NULL)));
        }

        @Override
        public ResourceClass getGeneralClass()
        {
            return this;
        }

        @Override
        public String getPatternCode(Node node, int part)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSpecialisedPatternCode(String table, String var, int part)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPatternCode(String column, int part, boolean isBoxed)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getResultCode(String variable, int part)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean match(Node node, Request request)
        {
            throw new UnsupportedOperationException();
        }
    }


    private class InternalNodeMapping extends NodeMapping implements ParametrisedMapping
    {
        private final List<Column> columns;

        public InternalNodeMapping(ResourceClass resourceClass, List<Column> columns)
        {
            super(resourceClass);
            this.columns = columns;
        }

        @Override
        public boolean match(Node node, Request request)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSqlValueAccess(int part)
        {
            return columns.get(part).getCode();
        }

        @Override
        public Column getSqlColumn(int part)
        {
            return columns.get(part);
        }

        @Override
        public NodeMapping remapColumns(List<ColumnPair> columnMap)
        {
            ArrayList<Column> remappedColumns = new ArrayList<Column>();

            for(Column col : columns)
            {
                Column remapped = columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight();
                assert remapped != null;
                remappedColumns.add(remapped);
            }

            return new InternalNodeMapping(getResourceClass(), remappedColumns);
        }

        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
                return true;

            if(obj == null || !(obj instanceof InternalNodeMapping))
                return false;

            if(!super.equals(obj))
                return false;

            InternalNodeMapping other = (InternalNodeMapping) obj;

            return columns.equals(other.columns);
        }
    }


    private final Table subjectTable;
    private final Table objectTable;
    private final List<Column> subjectJoinColumns;
    private final List<Column> objectJoinColumns;
    private final NodeMapping subjectJoinMapping;
    private final NodeMapping objectJoinMapping;
    private final String subjectCondition;
    private final String objectCondition;


    public JoinTableQuadMapping(Table subjectTable, Table objectTable, List<Column> subjectJoinColumns,
            List<Column> objectJoinColumns, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        this(subjectTable, objectTable, subjectJoinColumns, objectJoinColumns, graph, subject, predicate, object, null,
                null);
    }


    public JoinTableQuadMapping(Table subjectTable, Table objectTable, List<Column> subjectJoinColumns,
            List<Column> objectJoinColumns, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, String subjectCondition, String objectCondition)
    {
        super(graph, subject, predicate, object);

        this.subjectTable = subjectTable;
        this.objectTable = objectTable;

        this.subjectJoinColumns = subjectJoinColumns;
        this.objectJoinColumns = objectJoinColumns;

        assert subjectJoinColumns.size() == objectJoinColumns.size();
        ResourceClass resourceClass = new InternalResourceClass(subjectJoinColumns.size());

        this.subjectJoinMapping = new InternalNodeMapping(resourceClass, subjectJoinColumns);
        this.objectJoinMapping = new InternalNodeMapping(resourceClass, objectJoinColumns);

        this.subjectCondition = subjectCondition;
        this.objectCondition = objectCondition;
    }


    public final Table getSubjectTable()
    {
        return subjectTable;
    }


    public final Table getObjectTable()
    {
        return objectTable;
    }


    public final List<Column> getSubjectJoinColumns()
    {
        return subjectJoinColumns;
    }


    public final List<Column> getObjectJoinColumns()
    {
        return objectJoinColumns;
    }


    public final String getSubjectCondition()
    {
        return subjectCondition;
    }


    public final String getObjectCondition()
    {
        return objectCondition;
    }


    public NodeMapping getSubjectJoinMapping()
    {
        return subjectJoinMapping;
    }


    public NodeMapping getObjectJoinMapping()
    {
        return objectJoinMapping;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(!super.equals(obj))
            return false;

        JoinTableQuadMapping mapping = (JoinTableQuadMapping) obj;

        if(!subjectTable.equals(mapping.subjectTable))
            return false;

        if(!objectTable.equals(mapping.objectTable))
            return false;

        if(!subjectJoinColumns.equals(mapping.subjectJoinColumns))
            return false;

        if(!objectJoinColumns.equals(mapping.objectJoinColumns))
            return false;

        if(!subjectJoinMapping.equals(mapping.subjectJoinMapping))
            return false;

        if(!objectJoinMapping.equals(mapping.objectJoinMapping))
            return false;

        if(subjectCondition == null ? mapping.subjectCondition != null :
                !subjectCondition.equals(mapping.subjectCondition))
            return false;

        if(objectCondition == null ? mapping.objectCondition != null : !objectCondition.equals(mapping.objectCondition))
            return false;

        return true;
    }
}
