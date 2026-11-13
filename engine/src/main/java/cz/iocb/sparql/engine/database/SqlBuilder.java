package cz.iocb.sparql.engine.database;

import java.util.HashSet;
import java.util.Set;



/**
 * Builder of SQL code (see {@link SqlCode}): the text is appended piece by piece, and the declared tables the pieces
 * read and the outer tables they refer to are collected along with it. A piece of code brings its tables, and so does a
 * column whose expression has some; a declared table written by its name is read by the code from then on, so writing a
 * reference and recording it are one step. An outer table is referred to where a column is addressed through it (see
 * {@link TableColumn#fromTable}), and such a column brings the reference with it. The references end in the code that
 * gives the alias of the table to a subquery (see {@link #appendAlias}): that code holds the query the alias belongs
 * to, so it refers to the table no longer.
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
    private Set<DeclaredTable> declaredTables;

    /**
     * The outer tables the text built so far refers to; null while there is none.
     */
    private Set<OuterTable> outerTables;

    /**
     * The outer tables whose aliases the text built so far gives to subqueries (see {@link #appendAlias}); null while
     * there is none.
     */
    private Set<OuterTable> givenTables;


    /**
     * Creates an empty builder.
     */
    public SqlBuilder()
    {
    }


    /**
     * Appends SQL text that reads no declared table and refers to no outer table.
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
     * Appends a piece of SQL: code with its tables, a column with the tables of its expression, a declared table by its
     * name, which the code then reads, or anything else (another table, a type, a number) by its string form.
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
     * Appends the name of a table as the alias the code gives to a subquery ({@code (...) AS "lateral0"}). The
     * references to an outer table in the code of the query the alias belongs to are references to that subquery, so
     * the code built here, which has to hold that query whole, does not refer to the table, whichever of its pieces
     * holds the references and whether they are appended before the alias or after it. An alias of another kind is only
     * written.
     *
     * @param table the table
     * @return this builder
     */
    public SqlBuilder appendAlias(Table table)
    {
        if(table instanceof OuterTable outer)
        {
            if(givenTables == null)
                givenTables = new HashSet<>();

            givenTables.add(outer);
        }

        return append(table);
    }


    /**
     * Appends the name of a table as the qualifier of a column, so the code refers to the table from then on: an outer
     * table is recorded here, and nowhere else, because this is how nested code refers to it.
     *
     * @param table the table
     * @return this builder
     */
    SqlBuilder appendQualifier(Table table)
    {
        if(table instanceof OuterTable outer)
        {
            if(outerTables == null)
                outerTables = new HashSet<>();

            outerTables.add(outer);
        }

        return append(table);
    }


    /**
     * Records the tables of a piece of SQL (see {@link #append(Object)}) as tables of the code built here, wherever the
     * text of the piece is then written into it.
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
                record(code);
                yield code.getText();
            }

            case ExpressionColumn column ->
            {
                record(column.getCode());
                yield column.toString();
            }

            case DeclaredTable table ->
            {
                if(declaredTables == null)
                    declaredTables = new HashSet<>();

                declaredTables.add(table);
                yield table.toString();
            }

            case null, default -> piece;
        };
    }


    /**
     * Records the tables of the code as tables of the code built here.
     *
     * @param code the code
     */
    private void record(SqlCode code)
    {
        if(!code.getDeclaredTables().isEmpty())
        {
            if(declaredTables == null)
                declaredTables = new HashSet<>();

            declaredTables.addAll(code.getDeclaredTables());
        }

        if(!code.getOuterTables().isEmpty())
        {
            if(outerTables == null)
                outerTables = new HashSet<>();

            outerTables.addAll(code.getOuterTables());
        }
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
     * The code built so far: the text with its tables. The outer tables whose aliases the text gives are not among them
     * (see {@link #appendAlias}).
     *
     * @return the code built so far
     */
    public SqlCode toCode()
    {
        Set<OuterTable> outer = outerTables == null ? Set.of() : outerTables;

        if(givenTables != null && !outer.isEmpty())
        {
            outer = new HashSet<>(outer);
            outer.removeAll(givenTables);
        }

        return new SqlCode(text.toString(), declaredTables == null ? Set.of() : declaredTables, outer);
    }
}
