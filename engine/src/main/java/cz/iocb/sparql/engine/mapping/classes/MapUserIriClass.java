package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * IRIs whose id (between prefix and suffix) is translated to the stored value through a mapping table: the {@code to}
 * column holds the id, the {@code from} column the value stored in the mapped tables.
 */
public class MapUserIriClass extends SimpleUserIriClass
{
    /**
     * Query translating a placeholder IRI to the stored value.
     */
    private final String sqlQuery;

    /**
     * Mapping table.
     */
    private final Table table;

    /**
     * Column holding the stored values.
     */
    private final TableColumn from;

    /**
     * Column holding the ids.
     */
    private final TableColumn to;

    /**
     * Compiled regular expression of the IRIs.
     */
    private final Pattern pattern;

    /**
     * Regular expression of the IRIs.
     */
    private final String regexp;

    /**
     * Text before the id, or null.
     */
    private final String prefix;

    /**
     * Text after the id, or null.
     */
    private final String suffix;

    /**
     * Fixed length of the id, or zero.
     */
    private final int length;


    /**
     * Creates the class with all options.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     * @param pattern regular expression constraining the id, or null
     * @param suffix the suffix
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.table = table;
        this.from = from;
        this.to = to;

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        String code;

        if(prefix == null && suffix == null)
            code = "?::varchar";
        else if(length > 0)
            code = String.format("substring(?, %d, %d)::varchar", prefix != null ? prefix.length() + 1 : 1, length);
        else if(prefix == null)
            code = String.format("left(?, -%d)::varchar", suffix.length());
        else if(suffix == null)
            code = String.format("right(?, -%d)::varchar", prefix.length());
        else
            code = String.format("left(right(?, -%d), -%d)::varchar", prefix.length(), suffix.length());

        this.sqlQuery = String.format("(SELECT %s::varchar FROM %s WHERE %s = %s)", from, table, to, code);


        StringBuilder builder = new StringBuilder();

        builder.append("^(");

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(" + pattern + ")");
        else if(length > 0)
            builder.append(".{").append(length).append("}");
        else
            builder.append(".*");

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        builder.append(")$");

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
    }


    /**
     * Creates the class with an id of any length.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     * @param pattern regular expression constraining the id, or null
     * @param suffix the suffix
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern, String suffix)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, suffix);
    }


    /**
     * Creates the class without a suffix.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     * @param pattern regular expression constraining the id, or null
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern)
    {
        this(name, sqlType, table, from, to, prefix, length, pattern, null);
    }


    /**
     * Creates the class with an id of any length and no suffix.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     * @param pattern regular expression constraining the id, or null
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, null);
    }


    /**
     * Creates the class with an id of fixed length and no pattern or suffix.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length)
    {
        this(name, sqlType, table, from, to, prefix, length, null, null);
    }


    /**
     * Creates the class with a prefix only.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param table the table
     * @param from column holding the stored values
     * @param to column holding the ids
     * @param prefix the prefix
     */
    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix)
    {
        this(name, sqlType, table, from, to, prefix, 0, null, null);
    }


    @Override
    public boolean match(Statement statement, Iri iri)
    {
        if(!pattern.matcher(iri.getValue()).matches())
            return false;

        try
        {
            String sql = sqlQuery.replaceAll("\\?", string(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                return result.next();
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public List<Column> toColumns(Statement statement, Iri iri)
    {
        assert match(statement, iri);

        try
        {
            String sql = sqlQuery.replaceAll("\\?", string(iri.getValue()));

            try(ResultSet result = statement.executeQuery(sql))
            {
                if(result.next())
                    return List.of(constant(result.getString(1), sqlTypes.get(0)));
                else
                    throw new RuntimeException();
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    protected Column generateFunction(Column column)
    {
        String access = String.format("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = addPrefixAndSuffix(prefix, expression("\"@to\""), suffix);

        return expression("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, column);
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        Column func = generateNonCheckedInverseFunction(column);

        if(!check)
            return func;

        return expression("CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp), func);
    }


    /**
     * SQL expression translating an IRI assumed to belong to the class to the stored value.
     *
     * @param column the column
     * @return SQL expression translating an IRI assumed to belong to the class to the stored value
     */
    protected Column generateNonCheckedInverseFunction(Column column)
    {
        Column access = expression("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = generateExtractionFunction(column);

        return expression("(SELECT \"@from\"::%s FROM %s WHERE \"@to\" = %s)", sqlTypes.get(0), access, code);
    }


    /**
     * SQL expression extracting the id from an IRI.
     *
     * @param column the column
     * @return SQL expression extracting the id from an IRI
     */
    protected Column generateExtractionFunction(Column column)
    {
        if(prefix == null && suffix == null)
            return column;
        else if(length > 0 && prefix == null)
            return expression("substring(%s, %d, %d)::varchar", column, 1, length);
        else if(length > 0 && prefix != null)
            return expression("substring(%s, %d, %d)::varchar", column, prefix.length() + 1, length);
        else if(prefix == null)
            return expression("left(%s, -%d)::varchar", column, suffix.length());
        else if(suffix == null)
            return expression("right(%s, -%d)::varchar", column, prefix.length());
        else
            return expression("left(right(%s, -%d), -%d)::varchar", column, prefix.length(), suffix.length());
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        Column access = expression("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);
        Column code = suffix != null ? code = expression("\"@to\" || %s", string(suffix)) : expression("\"@to\"");

        return List.of(expression("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, columns.get(0)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 1;
    }


    /**
     * Mapping table.
     *
     * @return mapping table
     */
    public Table getTable()
    {
        return table;
    }


    /**
     * Column holding the stored values.
     *
     * @return column holding the stored values
     */
    public TableColumn getFrom()
    {
        return from;
    }


    /**
     * Column holding the ids.
     *
     * @return column holding the ids
     */
    public TableColumn getTo()
    {
        return to;
    }


    /**
     * Text before the id, or null.
     *
     * @return text before the id, or null
     */
    public String getPrefix()
    {
        return prefix;
    }


    /**
     * Text after the id, or null.
     *
     * @return text after the id, or null
     */
    public String getSuffix()
    {
        return suffix;
    }


    /**
     * Fixed length of the id, or zero.
     *
     * @return fixed length of the id, or zero
     */
    public int getIdLength()
    {
        return length;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        MapUserIriClass other = (MapUserIriClass) object;

        return Objects.equals(table, other.table) && Objects.equals(from, other.from) && Objects.equals(to, other.to)
                && Objects.equals(regexp, other.regexp) && Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix) && Objects.equals(length, other.length);
    }
}
