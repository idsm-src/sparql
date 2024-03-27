package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class EnumUserIriClass extends SimpleUserIriClass
{
    private final String pattern;
    private final HashMap<IRI, String> values;
    private final String prefix;


    public EnumUserIriClass(String name, String sqlType, HashMap<IRI, String> values)
    {
        super(name, sqlType);

        List<String> iris = values.keySet().stream().map(i -> i.getValue()).sorted().collect(toList());
        String first = iris.getFirst();
        String last = iris.getLast();

        int end = Math.min(first.length(), last.length());
        int length = 0;

        while(length < end && first.charAt(length) == last.charAt(length))
            length++;

        this.prefix = first.substring(0, length);
        this.pattern = values.keySet().stream().map(i -> Pattern.quote(i.getValue())).collect(joining("|"));
        this.values = values;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(node);

        return asList(new ConstantColumn(values.get(iri), sqlTypes.get(0)));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public boolean match(IRI iri)
    {
        return iri.getValue().matches(pattern);
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    protected Column generateFunction(Column parameter)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("CASE %s ", parameter));

        for(Entry<IRI, String> entry : values.entrySet())
            builder.append(String.format("WHEN '%s'::%s THEN '%s' ", entry.getValue().replaceAll("'", "''"),
                    sqlTypes.get(0), entry.getKey().getValue().replaceAll("'", "''")));

        builder.append("END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("CASE %s ", parameter));

        for(Entry<IRI, String> entry : values.entrySet())
            builder.append(String.format("WHEN '%s' THEN '%s'::%s ", entry.getKey().getValue().replaceAll("'", "''"),
                    entry.getValue().replaceAll("'", "''"), sqlTypes.get(0)));

        builder.append("END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        EnumUserIriClass other = (EnumUserIriClass) object;

        if(!pattern.equals(other.pattern))
            return false;

        if(!values.equals(other.values))
            return false;

        return true;
    }
}
