package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.parser.model.IRI;



public class EnumUserIriClass extends SimpleUserIriClass
{
    private final Pattern pattern;
    private final String regexp;
    private final HashMap<IRI, String> values;
    private final String prefix;


    public EnumUserIriClass(String name, String sqlType, HashMap<IRI, String> values)
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

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = values.keySet().stream().map(i -> Pattern.quote(i.getValue())).collect(joining("|"));
        this.pattern = Pattern.compile(regexp);
    }


    @Override
    public boolean match(Statement statement, IRI iri)
    {
        return pattern.matcher(iri.getValue()).matches();
    }


    @Override
    public List<Column> toColumns(Statement statement, IRI iri)
    {
        assert match(statement, iri);

        return List.of(constant(values.get(iri), sqlTypes.get(0)));
    }


    @Override
    protected Column generateFunction(Column column)
    {
        return expression("CASE %s %s END", column,
                values.entrySet().stream().map(e -> format("WHEN %s::%s THEN %s::varchar", string(e.getValue()),
                        sqlTypes.get(0), string(e.getKey().getValue()))).collect(joining(" ")));
    }


    @Override
    protected Column generateInverseFunction(Column column, boolean check)
    {
        return expression(
                "CASE %s %s END", column, values
                        .entrySet().stream().map(e -> format("WHEN %s::varchar THEN %s::%s",
                                string(e.getKey().getValue()), string(e.getValue()), sqlTypes.get(0)))
                        .collect(joining(" ")));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ConstantColumn col)
            values.get(new IRI(col.getValue()));

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
