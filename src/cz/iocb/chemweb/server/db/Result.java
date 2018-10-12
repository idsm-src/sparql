package cz.iocb.chemweb.server.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;



public class Result implements Collection<Row>
{
    private final Vector<String> heads;
    private final ArrayList<Row> rows;
    private final List<String> warnings;
    private Row currentRow;


    public Result(Vector<String> heads, ArrayList<Row> rows, List<String> warnings)
    {
        this.heads = heads;
        this.rows = rows;
        this.warnings = warnings;

        if(!rows.isEmpty())
            currentRow = rows.get(0);
    }


    public int getCount()
    {
        return rows.size();
    }


    public Vector<String> getHeads()
    {
        return heads;
    }


    public RdfNode get(String name)
    {
        if(currentRow == null)
            return null;

        return currentRow.get(name);
    }


    @Override
    public boolean add(Row arg0)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean addAll(Collection<? extends Row> arg0)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean contains(Object arg0)
    {
        return rows.contains(arg0);
    }


    @Override
    public boolean containsAll(Collection<?> arg0)
    {
        return rows.containsAll(arg0);
    }


    @Override
    public boolean isEmpty()
    {
        return rows.isEmpty();
    }


    @Override
    public Iterator<Row> iterator()
    {
        return rows.iterator();
    }


    @Override
    public boolean remove(Object arg0)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean removeAll(Collection<?> arg0)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean retainAll(Collection<?> arg0)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public int size()
    {
        return rows.size();
    }


    @Override
    public Object[] toArray()
    {
        return rows.toArray();
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object[] toArray(Object[] arg0)
    {
        return rows.toArray(arg0);
    }


    public List<String> getWarnings()
    {
        return warnings;
    }
}
