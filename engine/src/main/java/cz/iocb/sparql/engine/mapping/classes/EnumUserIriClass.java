package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Fixed finite set of IRIs, each mapped to a constant of the SQL column; conversions are CASE expressions.
 */
public class EnumUserIriClass extends SimpleUserIriClass
{
    /**
     * Compiled alternative of all member IRIs.
     */
    private final Pattern pattern;

    /**
     * Regular expression matching exactly the member IRIs.
     */
    private final String regexp;

    /**
     * Stored value of each member IRI.
     */
    private final Map<Iri, String> values;

    /**
     * Longest common prefix of the member IRIs.
     */
    private final String prefix;


    /**
     * Creates the class mapping each IRI to its stored value of the given SQL type.
     *
     * @param name the name
     * @param sqlType the SQL type
     * @param values stored value of each member IRI
     */
    public EnumUserIriClass(String name, SqlType sqlType, Map<Iri, String> values)
    {
        super(name, sqlType);

        List<String> iris = values.keySet().stream().map(i -> i.getValue()).sorted().toList();
        String first = iris.getFirst();
        String last = iris.getLast();

        int end = Math.min(first.length(), last.length());
        int length = 0;

        while(length < end && first.charAt(length) == last.charAt(length))
            length++;

        this.prefix = first.substring(0, length);
        this.values = values;

        this.regexp = values.keySet().stream().map(i -> Pattern.quote(i.getValue())).collect(joining("|"));
        this.pattern = Pattern.compile(regexp);
    }


    /**
     * The member IRIs.
     *
     * @return the member IRIs
     */
    public Set<Iri> getMembers()
    {
        return Collections.unmodifiableSet(values.keySet());
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

        return List.of(constant(values.get(iri), sqlTypes.get(0)));
    }


    @Override
    protected Column generateFunction(Column column)
    {
        return expression(VARCHAR, "CASE %s %s END", column,
                values.entrySet().stream().map(e -> format("WHEN %s::%s THEN %s::varchar", string(e.getValue()),
                        sqlTypes.get(0), string(e.getKey().getValue()))).collect(joining(" ")));
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        return expression(
                sqlTypes.get(0), "CASE %s %s END", column, values
                        .entrySet().stream().map(e -> format("WHEN %s::varchar THEN %s::%s",
                                string(e.getKey().getValue()), string(e.getValue()), sqlTypes.get(0)))
                        .collect(joining(" ")));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        EnumUserIriClass other = (EnumUserIriClass) object;

        return Objects.equals(regexp, other.regexp) && Objects.equals(values, other.values);
    }
}
