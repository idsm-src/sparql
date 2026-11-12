package cz.iocb.sparql.engine.database;

import java.util.Objects;
import java.util.Set;



/**
 * SQL code of a query or of a part of one, together with the declared tables it reads (see {@link DeclaredTable}). The
 * code is no statement yet: it becomes a part of larger code, which then reads the tables as well (see
 * {@link SqlBuilder}), until a statement is made of it, whose {@code WITH} clause declares the tables. What the code
 * reads is recorded where the name of the table is written, so it never has to be found in the text again. The text
 * itself is given only by {@link #getText}; the string form of the object serves debugging and is no SQL, so that code
 * joined to a string by mistake, which would lose the tables, does not pass for SQL.
 */
public final class SqlCode
{
    /**
     * The SQL text.
     */
    private final String text;

    /**
     * The declared tables the text reads.
     */
    private final Set<DeclaredTable> tables;


    /**
     * Creates code that reads no declared table.
     *
     * @param text the SQL text
     */
    public SqlCode(String text)
    {
        this(text, Set.of());
    }


    /**
     * Creates code reading the given declared tables.
     *
     * @param text the SQL text
     * @param tables the declared tables the text reads
     */
    public SqlCode(String text, Set<? extends DeclaredTable> tables)
    {
        this.text = Objects.requireNonNull(text);
        this.tables = Set.copyOf(tables);
    }


    /**
     * The SQL text, without the declarations of the tables it reads.
     *
     * @return the SQL text
     */
    public String getText()
    {
        return text;
    }


    /**
     * The declared tables the text reads, which a statement holding the code has to declare in its {@code WITH} clause,
     * along with the declared tables their own queries read.
     *
     * @return the declared tables the text reads
     */
    public Set<DeclaredTable> getDeclaredTables()
    {
        return tables;
    }


    @Override
    public int hashCode()
    {
        return text.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlCode other))
            return false;

        return text.equals(other.text) && tables.equals(other.tables);
    }


    @Override
    public String toString()
    {
        return "SqlCode[text=" + text + ", tables=" + tables + "]";
    }
}
