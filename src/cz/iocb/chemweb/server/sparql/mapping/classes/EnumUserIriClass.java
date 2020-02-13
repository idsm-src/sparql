package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.engine.Request;



public class EnumUserIriClass extends SimpleUserIriClass
{
    private final String pattern;
    private final HashMap<String, String> values;


    public EnumUserIriClass(String name, String sqlType, HashMap<String, String> values)
    {
        super(name, sqlType);

        StringBuffer buffer = new StringBuffer();

        for(String key : values.values())
        {
            if(buffer.length() > 0)
                buffer.append('|');

            buffer.append(Pattern.quote(key));
        }

        this.pattern = buffer.toString();
        this.values = values;
    }


    @Override
    public boolean match(String iri, Request request)
    {
        return iri.matches(pattern);
    }


    @Override
    public boolean match(String iri, DataSource connectionPool)
    {
        return iri.matches(pattern);
    }


    @Override
    protected String generateFunction(String parameter)
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append(String.format("(select case %s ", parameter));

        for(Entry<String, String> entry : values.entrySet())
            buffer.append(String.format("when '%s'::%s then '%s' ", entry.getKey().replaceAll("'", "''"),
                    sqlTypes.get(0), entry.getValue().replaceAll("'", "''")));

        buffer.append("end)");

        return buffer.toString();
    }


    @Override
    protected String generateInverseFunction(String parameter)
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append(String.format("(select case %s ", parameter));

        for(Entry<String, String> entry : values.entrySet())
            buffer.append(String.format("when '%s' then '%s'::%s ", entry.getValue().replaceAll("'", "''"),
                    entry.getKey().replaceAll("'", "''"), sqlTypes.get(0)));

        buffer.append("end)");

        return buffer.toString();
    }
}
