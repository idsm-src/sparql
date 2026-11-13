package cz.iocb.sparql.engine.database;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;



/**
 * SQL code, together with the tables it depends on from outside: the declared tables it reads (see
 * {@link DeclaredTable}), which the statement the code becomes a part of declares in its {@code WITH} clause, and the
 * outer tables it refers to (see {@link OuterTable}), the aliases through which nested code addresses the columns of
 * the solutions of enclosing code. The code is a query, a condition, the expression of a column (see
 * {@link ExpressionColumn}) or any other part of a statement; it becomes a part of larger code, which then depends on
 * the tables as well. A dependence is recorded where it arises, a declared table where its name is written and an outer
 * table where a column is addressed through it, and it is passed on wherever code is made of pieces (see
 * {@link SqlBuilder}, {@link #of}, {@link #format} and {@link #joining(String)}), so it never has to be found in the
 * text again. Plain strings are pieces of SQL text without such a dependence, like keywords, operators and the names of
 * functions. The text itself is given only by {@link #getText}; the string form of the object serves debugging and is
 * no SQL, so that code joined to a string by mistake, which would lose the tables, does not pass for SQL.
 */
public final class SqlCode implements Comparable<SqlCode>
{
    /**
     * The condition that always holds.
     */
    public static final SqlCode TRUE = new SqlCode("true");

    /**
     * The condition that never holds.
     */
    public static final SqlCode FALSE = new SqlCode("false");

    /**
     * The SQL text.
     */
    private final String text;

    /**
     * The declared tables the text reads.
     */
    private final Set<DeclaredTable> declaredTables;

    /**
     * The outer tables the text refers to.
     */
    private final Set<OuterTable> outerTables;


    /**
     * Creates code that reads no declared table and refers to no outer table.
     *
     * @param text the SQL text
     */
    public SqlCode(String text)
    {
        this(text, Set.of(), Set.of());
    }


    /**
     * Creates code with the given tables; only a builder makes such code, as it records the tables where the code comes
     * to depend on them (see {@link SqlBuilder}).
     *
     * @param text the SQL text
     * @param declaredTables the declared tables the text reads
     * @param outerTables the outer tables the text refers to
     */
    SqlCode(String text, Set<DeclaredTable> declaredTables, Set<OuterTable> outerTables)
    {
        this.text = Objects.requireNonNull(text);
        this.declaredTables = Set.copyOf(declaredTables);
        this.outerTables = Set.copyOf(outerTables);
    }


    /**
     * Code made of the pieces written one after another (see {@link SqlBuilder#append(Object)}).
     *
     * @param pieces the pieces of SQL
     * @return code made of the pieces
     */
    public static SqlCode of(Object... pieces)
    {
        SqlBuilder builder = new SqlBuilder();

        for(Object piece : pieces)
            builder.append(piece);

        return builder.toCode();
    }


    /**
     * Code made of the format string, as {@link String#format} makes a string of it (in the root locale), with pieces
     * of SQL as the arguments: the code has the tables of the pieces (see {@link SqlBuilder#append(Object)}).
     *
     * @param format the format string
     * @param arguments the arguments, pieces of SQL
     * @return code made of the format string and the arguments
     */
    public static SqlCode format(String format, Object... arguments)
    {
        SqlBuilder builder = new SqlBuilder();
        Object[] texts = new Object[arguments.length];

        for(int i = 0; i < arguments.length; i++)
            texts[i] = builder.record(arguments[i]);

        return builder.append(String.format(ROOT, format, texts)).toCode();
    }


    /**
     * Collector making code of pieces of SQL (see {@link SqlBuilder#append(Object)}) separated by the delimiter.
     *
     * @param delimiter SQL text written between the pieces
     * @return collector making code of pieces of SQL separated by the delimiter
     */
    public static Collector<Object, ?, SqlCode> joining(String delimiter)
    {
        return joining(delimiter, "", "");
    }


    /**
     * Collector making code of pieces of SQL (see {@link SqlBuilder#append(Object)}) separated by the delimiter,
     * between the prefix and the suffix.
     *
     * @param delimiter SQL text written between the pieces
     * @param prefix SQL text written before the pieces
     * @param suffix SQL text written after the pieces
     * @return collector making code of pieces of SQL separated by the delimiter, between the prefix and the suffix
     */
    public static Collector<Object, ?, SqlCode> joining(String delimiter, String prefix, String suffix)
    {
        return collectingAndThen(toList(), pieces -> join(pieces, delimiter, prefix, suffix));
    }


    /**
     * Code made of the pieces separated by the delimiter, between the prefix and the suffix.
     *
     * @param pieces the pieces of SQL
     * @param delimiter SQL text written between the pieces
     * @param prefix SQL text written before the pieces
     * @param suffix SQL text written after the pieces
     * @return code made of the pieces
     */
    private static SqlCode join(List<Object> pieces, String delimiter, String prefix, String suffix)
    {
        SqlBuilder builder = new SqlBuilder().append(prefix);

        for(int i = 0; i < pieces.size(); i++)
        {
            if(i > 0)
                builder.append(delimiter);

            builder.append(pieces.get(i));
        }

        return builder.append(suffix).toCode();
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
     * True if the code has no text.
     *
     * @return true if the code has no text, false otherwise
     */
    public boolean isEmpty()
    {
        return text.isEmpty();
    }


    /**
     * The declared tables the text reads, which a statement holding the code has to declare in its {@code WITH} clause,
     * along with the declared tables their own queries read.
     *
     * @return the declared tables the text reads
     */
    public Set<DeclaredTable> getDeclaredTables()
    {
        return declaredTables;
    }


    /**
     * The outer tables the text refers to: the aliases through which its columns are addressed, whether enclosing code
     * gives them or the code itself. Code referring to an outer table that it does not give itself has a meaning only
     * inside the query that gives the alias.
     *
     * @return the outer tables the text refers to
     */
    public Set<OuterTable> getOuterTables()
    {
        return outerTables;
    }


    /**
     * Compares the texts, so that pieces of code are sorted as their texts are.
     *
     * @param other the other code
     * @return the result of comparing the texts
     */
    @Override
    public int compareTo(SqlCode other)
    {
        return text.compareTo(other.text);
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

        return text.equals(other.text) && declaredTables.equals(other.declaredTables)
                && outerTables.equals(other.outerTables);
    }


    @Override
    public String toString()
    {
        return "SqlCode[text=" + text + ", declaredTables=" + declaredTables + ", outerTables=" + outerTables + "]";
    }
}
