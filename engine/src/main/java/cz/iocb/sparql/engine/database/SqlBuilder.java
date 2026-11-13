package cz.iocb.sparql.engine.database;

import java.util.HashSet;
import java.util.Set;



/**
 * Builder of SQL code (see {@link SqlCode}): the text is appended piece by piece, and the declared tables the pieces
 * read are collected along with it. A piece of code brings the tables it reads, so does a column whose expression reads
 * some, and a declared table written by its name is read by the code from then on, so writing a reference and recording
 * it are one step.
 */
public final class SqlBuilder
{
    /**
     * The SQL text built so far.
     */
    private final StringBuilder text = new StringBuilder();

    /**
     * The declared tables the text built so far reads; null while there is none.
     */
    private Set<DeclaredTable> tables;


    /**
     * Creates an empty builder.
     */
    public SqlBuilder()
    {
    }


    /**
     * Appends SQL text that reads no declared table.
     *
     * @param text the SQL text
     * @return this builder
     */
    public SqlBuilder append(String text)
    {
        this.text.append(text);

        return this;
    }


    /**
     * Appends a piece of SQL: code with the declared tables it reads, a column with the declared tables its expression
     * reads, a declared table by its name, which the code then reads, or anything else (another table, a type, a
     * number) by its string form.
     *
     * @param piece the piece of SQL
     * @return this builder
     */
    public SqlBuilder append(Object piece)
    {
        text.append(record(piece));

        return this;
    }


    /**
     * Records the declared tables a piece of SQL reads (see {@link #append(Object)}) as read by the code built here,
     * wherever the text of the piece is then written into it.
     *
     * @param piece the piece of SQL
     * @return the piece as it is written: the SQL text of code, of a column or of a declared table, the piece itself
     *         otherwise
     */
    Object record(Object piece)
    {
        return switch(piece)
        {
            case SqlCode code ->
            {
                addTables(code.getDeclaredTables());
                yield code.getText();
            }

            case ExpressionColumn column ->
            {
                addTables(column.getCode().getDeclaredTables());
                yield column.toString();
            }

            case DeclaredTable table ->
            {
                addTables(Set.of(table));
                yield table.toString();
            }

            case null, default -> piece;
        };
    }


    /**
     * Adds declared tables to those the code built here reads.
     *
     * @param tables the declared tables
     */
    private void addTables(Set<DeclaredTable> tables)
    {
        if(tables.isEmpty())
            return;

        if(this.tables == null)
            this.tables = new HashSet<>();

        this.tables.addAll(tables);
    }


    /**
     * True if no text has been appended yet.
     *
     * @return true if no text has been appended yet, false otherwise
     */
    public boolean isEmpty()
    {
        return text.isEmpty();
    }


    /**
     * The code built so far: the text with the declared tables it reads.
     *
     * @return the code built so far
     */
    public SqlCode toCode()
    {
        return new SqlCode(text.toString(), tables == null ? Set.of() : tables);
    }
}
