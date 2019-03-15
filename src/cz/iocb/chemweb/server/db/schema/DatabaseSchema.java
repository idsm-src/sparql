package cz.iocb.chemweb.server.db.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;



public abstract class DatabaseSchema
{
    public static class Pair<T>
    {
        protected final T left;
        protected final T right;

        public Pair(T left, T right)
        {
            this.left = left;
            this.right = right;
        }

        @Override
        public int hashCode()
        {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
                return true;

            if(obj == null || getClass() != obj.getClass())
                return false;

            @SuppressWarnings("rawtypes")
            Pair other = (Pair) obj;

            return left.equals(other.left) && right.equals(other.right);
        }

        public final T getLeft()
        {
            return left;
        }

        public final T getRight()
        {
            return right;
        }
    }


    public static class TablePair extends Pair<Table>
    {
        public TablePair(Table parent, Table foreign)
        {
            super(parent, foreign);
        }
    }


    public static class ColumnPair extends Pair<Column>
    {
        public ColumnPair(Column parent, Column foreign)
        {
            super(parent, foreign);
        }
    }


    private final HashMap<Table, List<List<Column>>> primaryKeys = new HashMap<Table, List<List<Column>>>();
    private final HashMap<TablePair, List<List<ColumnPair>>> foreignKeys = new HashMap<TablePair, List<List<ColumnPair>>>();
    private final HashMap<TablePair, List<List<ColumnPair>>> unjoinableColumns = new HashMap<TablePair, List<List<ColumnPair>>>();


    public void addPrimaryKeys(Table table, List<Column> columns)
    {
        List<List<Column>> primaryKeyList = primaryKeys.get(table);

        if(primaryKeyList == null)
        {
            primaryKeyList = new ArrayList<List<Column>>();
            primaryKeys.put(table, primaryKeyList);
        }


        ArrayList<Column> key = new ArrayList<Column>();
        key.addAll(columns);

        primaryKeyList.add(key);
    }


    public void addForeignKeys(Table parentTable, List<Column> parentColumns, Table foreignTable,
            List<Column> foreignColumns)
    {
        TablePair tablePair = new TablePair(parentTable, foreignTable);

        List<List<ColumnPair>> foreignKeyList = foreignKeys.get(tablePair);

        if(foreignKeyList == null)
        {
            foreignKeyList = new ArrayList<List<ColumnPair>>();
            foreignKeys.put(tablePair, foreignKeyList);
        }


        ArrayList<ColumnPair> keyPairs = new ArrayList<ColumnPair>();

        for(int i = 0; i < parentColumns.size(); i++)
            keyPairs.add(new ColumnPair(parentColumns.get(i), foreignColumns.get(i)));

        foreignKeyList.add(keyPairs);
    }


    public void addUnjoinableColumns(Table leftTable, List<Column> leftColumns, Table rightTable,
            List<Column> rightColumns)
    {
        TablePair tablePair = new TablePair(leftTable, rightTable);

        List<List<ColumnPair>> unjoinableList = unjoinableColumns.get(tablePair);

        if(unjoinableList == null)
        {
            unjoinableList = new ArrayList<List<ColumnPair>>();
            unjoinableColumns.put(tablePair, unjoinableList);
        }

        ArrayList<ColumnPair> columnPairs = new ArrayList<ColumnPair>();

        for(int i = 0; i < leftColumns.size(); i++)
            columnPairs.add(new ColumnPair(leftColumns.get(i), rightColumns.get(i)));

        unjoinableList.add(columnPairs);
    }


    public List<Column> getCompatibleKey(Table table, List<Column> columns)
    {
        List<List<Column>> keys = primaryKeys.get(table);

        if(keys == null)
            return null;

        loop:
        for(List<Column> key : keys)
        {
            for(Column part : key)
                if(!columns.contains(part))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<ColumnPair> getCompatibleForeignKey(Table parentTable, Table foreignTable,
            ArrayList<ColumnPair> columns, LinkedHashSet<Column> parentColumns)
    {
        List<List<ColumnPair>> keys = getForeignKeys(parentTable, foreignTable);

        if(keys == null)
            return null;


        loop:
        for(List<ColumnPair> key : keys)
        {
            for(ColumnPair keyPair : key)
                if(!columns.contains(keyPair))
                    continue loop;

            for(Column parentColumn : parentColumns)
                if(key.stream().noneMatch(keyPair -> keyPair.getLeft().equals(parentColumn)))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<ColumnPair> getUnjoinableColumns(Table leftTable, Table rightTable, List<ColumnPair> columns)
    {
        List<List<ColumnPair>> list = getUnjoinableColumns(leftTable, rightTable);

        if(list == null)
            return null;


        loop:
        for(List<ColumnPair> key : list)
        {
            for(ColumnPair keyPair : key)
                if(!columns.contains(keyPair))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<List<ColumnPair>> getForeignKeys(Table parentTable, Table foreignTable)
    {
        if(parentTable == null || foreignTable == null)
            return null;

        return foreignKeys.get(new TablePair(parentTable, foreignTable));
    }


    public List<List<ColumnPair>> getUnjoinableColumns(Table leftTable, Table rightTable)
    {
        if(leftTable == null || rightTable == null)
            return null;

        return unjoinableColumns.get(new TablePair(leftTable, rightTable));
    }
}
