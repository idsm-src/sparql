package cz.iocb.sparql.engine.request;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * Rows of the result of a statement read through a JDBC result set.
 */
final class ResultSetRows implements ResultRows
{
    /**
     * The result set.
     */
    private final ResultSet rs;


    /**
     * Creates the rows of the result set.
     *
     * @param rs the result set
     */
    ResultSetRows(ResultSet rs)
    {
        this.rs = rs;
    }


    @Override
    public boolean next() throws SQLException
    {
        return rs.next();
    }


    @Override
    public String getString(int index) throws SQLException
    {
        return rs.getString(index);
    }


    @Override
    public int getInt(int index) throws SQLException
    {
        return rs.getInt(index);
    }


    /**
     * {@inheritDoc} Text is read through {@link ResultSet#getString}, as the driver converts only its own types by
     * class, so that the text forms of {@code sparql.rdfbox} and {@code sparql.ubox} can be read too.
     */
    @Override
    public Object getValue(int index, SqlType type) throws SQLException
    {
        if(type.getJavaClass() == String.class)
            return rs.getString(index);

        return rs.getObject(index, type.getJavaClass());
    }


    @Override
    public List<String> getWarnings() throws SQLException
    {
        List<String> warnings = new ArrayList<>();

        for(SQLWarning warning = rs.getStatement().getWarnings(); warning != null; warning = warning.getNextWarning())
            warnings.add(warning.getMessage());

        return warnings;
    }


    @Override
    public void close() throws SQLException
    {
        rs.close();
    }
}
