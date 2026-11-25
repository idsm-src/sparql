package cz.iocb.sparql.engine.request;

import java.sql.SQLException;
import java.util.List;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * Rows of the result of a statement, which a {@link Result} reads one after another: the columns of the current row are
 * read by their positions, counted from 1, as a JDBC result set reads them.
 */
interface ResultRows extends AutoCloseable
{
    /**
     * Advances to the next row.
     *
     * @return true if a row is available, false at the end
     * @throws SQLException on database errors
     */
    boolean next() throws SQLException;


    /**
     * Text of the column of the current row.
     *
     * @param index position of the column
     * @return text of the column, or null if the column is NULL
     * @throws SQLException on database errors
     */
    String getString(int index) throws SQLException;


    /**
     * Integer of the column of the current row.
     *
     * @param index position of the column
     * @return integer of the column, or 0 if the column is NULL
     * @throws SQLException on database errors
     */
    int getInt(int index) throws SQLException;


    /**
     * Value of the column of the current row as the Java class of its SQL type ({@link SqlType#getJavaClass}).
     *
     * @param index position of the column
     * @param type SQL type of the column
     * @return value of the column, or null if the column is NULL
     * @throws SQLException on database errors, or when the value cannot be read as the class
     */
    Object getValue(int index, SqlType type) throws SQLException;


    /**
     * Warnings the database has raised for the statement.
     *
     * @return the warnings
     * @throws SQLException on database errors
     */
    List<String> getWarnings() throws SQLException;


    @Override
    void close() throws SQLException;
}
