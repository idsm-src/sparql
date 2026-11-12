package cz.iocb.sparql.engine.database;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



/**
 * Builder of SQL code (see {@link SqlCode}): the text is appended piece by piece, and the declared tables the pieces
 * read are collected along with it. A piece of code brings the tables it reads, and a declared table written by its
 * name is read by the code from then on, so writing a reference and recording it are one step.
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
     * Appends a piece of SQL: code with the declared tables it reads, a declared table by its name, which the code then
     * reads, or anything else (a column, another table, a number) by its string form.
     *
     * @param piece the piece of SQL
     * @return this builder
     */
    public SqlBuilder append(Object piece)
    {
        if(piece instanceof SqlCode code)
        {
            text.append(code.getText());
            addDeclaredTables(code.getDeclaredTables());
        }
        else
        {
            if(piece instanceof DeclaredTable table)
                addDeclaredTables(Set.of(table));

            text.append(piece);
        }

        return this;
    }


    /**
     * Adds declared tables read by SQL that is appended as plain text: the columns of an expression, whose SQL has been
     * rendered beforehand (see {@code SqlExpressionIntercode#getDeclaredTables}).
     *
     * @param tables the declared tables
     * @return this builder
     */
    public SqlBuilder addDeclaredTables(Collection<? extends DeclaredTable> tables)
    {
        if(!tables.isEmpty())
        {
            if(this.tables == null)
                this.tables = new HashSet<>();

            this.tables.addAll(tables);
        }

        return this;
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
