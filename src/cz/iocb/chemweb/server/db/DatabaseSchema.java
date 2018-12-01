package cz.iocb.chemweb.server.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;



public abstract class DatabaseSchema
{
    public static class StringPair
    {
        protected final String left;
        protected final String right;

        public StringPair(String left, String right)
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

            StringPair other = (StringPair) obj;

            return left.equals(other.left) && right.equals(other.right);
        }

        public final String getLeft()
        {
            return left;
        }

        public final String getRight()
        {
            return right;
        }
    }


    public static class TablePair extends StringPair
    {
        public TablePair(String parent, String foreign)
        {
            super(parent, foreign);
        }
    }


    public static class ColumnPair extends StringPair
    {
        public ColumnPair(String parent, String foreign)
        {
            super(parent, foreign);
        }
    }


    private final HashMap<String, List<List<String>>> primaryKeys = new HashMap<String, List<List<String>>>();
    private final HashMap<TablePair, List<List<ColumnPair>>> foreignKeys = new HashMap<TablePair, List<List<ColumnPair>>>();
    private final HashMap<TablePair, List<List<ColumnPair>>> unjoinableColumns = new HashMap<TablePair, List<List<ColumnPair>>>();


    public void addPrimaryKeys(String table, List<String> columns)
    {
        List<List<String>> primaryKeyList = primaryKeys.get(table);

        if(primaryKeyList == null)
        {
            primaryKeyList = new ArrayList<List<String>>();
            primaryKeys.put(table, primaryKeyList);
        }


        ArrayList<String> key = new ArrayList<String>();
        key.addAll(columns);

        primaryKeyList.add(key);
    }


    public void addForeignKeys(String parentTable, List<String> parentColumns, String foreignTable,
            List<String> foreignColumns)
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


    public void addUnjoinableColumns(String leftTable, List<String> leftColumns, String rightTable,
            List<String> rightColumns)
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


    public List<String> getCompatibleKey(String table, List<String> columns)
    {
        List<List<String>> keys = primaryKeys.get(table);

        if(keys == null)
            return null;

        loop:
        for(List<String> key : keys)
        {
            for(String part : key)
                if(!columns.contains(part))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<ColumnPair> getCompatibleForeignKey(String parentTable, String foreignTable,
            ArrayList<ColumnPair> columns, LinkedHashSet<String> parentColumns)
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

            for(String parentColumn : parentColumns)
                if(key.stream().noneMatch(keyPair -> keyPair.getLeft().equals(parentColumn)))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<ColumnPair> getUnjoinableColumns(String leftTable, String rightTable, List<ColumnPair> columns)
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


    public List<List<ColumnPair>> getForeignKeys(String parentTable, String foreignTable)
    {
        if(parentTable == null || foreignTable == null)
            return null;

        return foreignKeys.get(new TablePair(parentTable, foreignTable));
    }


    public List<List<ColumnPair>> getUnjoinableColumns(String leftTable, String rightTable)
    {
        if(leftTable == null || rightTable == null)
            return null;

        return unjoinableColumns.get(new TablePair(leftTable, rightTable));
    }
}
