package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * IRIs of the form {@code prefix + id + suffix} with a varchar id, optionally of a fixed length or constrained by a
 * regular expression.
 */
public class StringUserIriClass extends SimpleUserIriClass
{
    /**
     * Compiled regular expression of the IRIs.
     */
    private final Pattern pattern;

    /**
     * Regular expression of the IRIs, anchored to the whole IRI (see {@link #wholeIriRegexp}); Java evaluates it in
     * {@link #match} and PCRE2 in the conversions with the representability check.
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
     * Creates the class with all options; without {@code pattern}, the id is any text of the given (or any) length.
     *
     * @param name the name
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     * @param pattern regular expression constraining the id, or null; both {@code java.util.regex} and PCRE2 (in the
     *            UTF mode without UCP) evaluate it, so it has to keep to their common subset (not, e.g.,
     *            {@code \p{javaLowerCase}}, an intersection of character classes, a POSIX property name like
     *            {@code \p{Alpha}} or {@code (?U)})
     * @param suffix the suffix
     */
    public StringUserIriClass(String name, String prefix, int length, String pattern, String suffix)
    {
        super(name, VARCHAR);

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        StringBuilder builder = new StringBuilder();

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        if(pattern == null)
            pattern = length > 0 ? ".{" + length + "}" : ".*";

        builder.append("(?:" + pattern + ")");

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        this.regexp = wholeIriRegexp(builder.toString());
        this.pattern = Pattern.compile(regexp);
    }


    /**
     * Creates the class with an id of any length.
     *
     * @param name the name
     * @param prefix the prefix
     * @param pattern regular expression constraining the id, or null; both {@code java.util.regex} and PCRE2 (in the
     *            UTF mode without UCP) evaluate it, so it has to keep to their common subset (not, e.g.,
     *            {@code \p{javaLowerCase}}, an intersection of character classes, a POSIX property name like
     *            {@code \p{Alpha}} or {@code (?U)})
     * @param suffix the suffix
     */
    public StringUserIriClass(String name, String prefix, String pattern, String suffix)
    {
        this(name, prefix, 0, pattern, suffix);
    }


    /**
     * Creates the class without a suffix.
     *
     * @param name the name
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     * @param pattern regular expression constraining the id, or null; both {@code java.util.regex} and PCRE2 (in the
     *            UTF mode without UCP) evaluate it, so it has to keep to their common subset (not, e.g.,
     *            {@code \p{javaLowerCase}}, an intersection of character classes, a POSIX property name like
     *            {@code \p{Alpha}} or {@code (?U)})
     */
    public StringUserIriClass(String name, String prefix, int length, String pattern)
    {
        this(name, prefix, length, pattern, null);
    }


    /**
     * Creates the class with an id of any length and no suffix.
     *
     * @param name the name
     * @param prefix the prefix
     * @param pattern regular expression constraining the id, or null; both {@code java.util.regex} and PCRE2 (in the
     *            UTF mode without UCP) evaluate it, so it has to keep to their common subset (not, e.g.,
     *            {@code \p{javaLowerCase}}, an intersection of character classes, a POSIX property name like
     *            {@code \p{Alpha}} or {@code (?U)})
     */
    public StringUserIriClass(String name, String prefix, String pattern)
    {
        this(name, prefix, 0, pattern, null);
    }


    /**
     * Creates the class with an id of fixed length and no pattern or suffix.
     *
     * @param name the name
     * @param prefix the prefix
     * @param length fixed length of the id, or zero
     */
    public StringUserIriClass(String name, String prefix, int length)
    {
        this(name, prefix, length, null, null);
    }


    /**
     * Creates the class with a prefix only.
     *
     * @param name the name
     * @param prefix the prefix
     */
    public StringUserIriClass(String name, String prefix)
    {
        this(name, prefix, 0, null, null);
    }


    @Override
    public boolean match(Request request, Iri iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Request request, Iri iri)
    {
        assert match(request, iri);

        String value = iri.getValue();
        int start = prefix != null ? prefix.length() : 0;
        int end = value.length() - (suffix != null ? suffix.length() : 0);

        return List.of(constant(value.substring(start, end), VARCHAR));
    }


    @Override
    protected Column generateFunction(Column column)
    {
        return addPrefixAndSuffix(prefix, column, suffix);
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        Column func = generateNonCheckedInverseFunction(column);

        if(!check)
            return func;

        return expression(sqlTypes.get(0), "CASE WHEN sparql.regex_string(%s, %s) THEN %s END", column, string(regexp),
                func);
    }


    /**
     * SQL expression extracting the id from an IRI assumed to belong to the class.
     *
     * @param column the column
     * @return SQL expression extracting the id from an IRI assumed to belong to the class
     */
    private Column generateNonCheckedInverseFunction(Column column)
    {
        SqlType sqlType = sqlTypes.get(0);

        if(prefix == null && suffix == null)
            return column;
        else if(length > 0 && prefix == null)
            return expression(sqlType, "substring(%s, %d, %d)::%s", column, 1, length, sqlType);
        else if(length > 0)
            return expression(sqlType, "substring(%s, %d, %d)::%s", column, sqlLength(prefix) + 1, length, sqlType);
        else if(prefix == null)
            return expression(sqlType, "left(%s, -%d)::%s", column, sqlLength(suffix), sqlType);
        else if(suffix == null)
            return expression(sqlType, "right(%s, -%d)::%s", column, sqlLength(prefix), sqlType);
        else
            return expression(sqlType, "left(right(%s, -%d), -%d)::%s", column, sqlLength(prefix), sqlLength(suffix),
                    sqlType);
    }


    @Override
    public List<Column> toOrderColumns(List<Column> columns)
    {
        return List.of(addPrefixAndSuffix(null, columns.get(0), suffix));
    }


    @Override
    public List<Column> fromOrderColumns(List<Column> columns)
    {
        if(suffix == null)
            return columns;

        return List.of(expression(VARCHAR, "left(%s, -%d)::varchar", columns.get(0), sqlLength(suffix)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ValueColumn col)
            return (prefix != null ? prefix : "") + col.getValue() + (suffix != null ? suffix : "");

        return prefix != null ? prefix : "";
    }


    @Override
    public List<String> getSqlRegexps()
    {
        return List.of(regexp);
    }


    @Override
    public int getCheckCost()
    {
        return 0;
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

        StringUserIriClass other = (StringUserIriClass) object;

        return Objects.equals(regexp, other.regexp) && Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix) && Objects.equals(length, other.length);
    }
}
