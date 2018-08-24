package cz.iocb.chemweb.server.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;



public abstract class DatabaseSchema
{
    public static class KeyPair
    {
        private final String parent;
        private final String foreign;


        public KeyPair(String parent, String foreign)
        {
            this.parent = parent;
            this.foreign = foreign;
        }


        @Override
        public int hashCode()
        {
            return parent.hashCode() ^ foreign.hashCode();
        }


        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
                return true;

            if(obj == null || !(obj instanceof KeyPair))
                return false;

            KeyPair other = (KeyPair) obj;

            return parent.equals(other.parent) && foreign.equals(other.foreign);
        }


        public final String getParent()
        {
            return parent;
        }


        public final String getForeign()
        {
            return foreign;
        }
    }


    protected final HashMap<String, List<List<String>>> primaryKeys = new HashMap<String, List<List<String>>>();
    protected final HashMap<KeyPair, List<List<KeyPair>>> foreignKeys = new HashMap<KeyPair, List<List<KeyPair>>>();


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


    public List<KeyPair> getCompatibleForeignKey(String parentTable, String foreignTable, ArrayList<KeyPair> columns,
            LinkedHashSet<String> parentColumns)
    {
        List<List<KeyPair>> keys = getForeignKeys(parentTable, foreignTable);

        if(keys == null)
            return null;


        loop:
        for(List<KeyPair> key : keys)
        {
            for(KeyPair keyPair : key)
                if(!columns.contains(keyPair))
                    continue loop;

            for(String parentColumn : parentColumns)
                if(key.stream().noneMatch(keyPair -> keyPair.parent.equals(parentColumn)))
                    continue loop;

            return key;
        }

        return null;
    }


    public List<List<KeyPair>> getForeignKeys(String parentTable, String foreignTable)
    {
        if(parentTable == null || foreignTable == null)
            return null;

        return foreignKeys.get(new KeyPair(parentTable, foreignTable));
    }
}
