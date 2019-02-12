package cz.iocb.chemweb.server.sparql.parser.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;



/**
 * Identifier used to represent a resource.
 */
public final class IRI extends BaseComplexNode implements VarOrIri, Path
{
    private final String value;


    public IRI(String value)
    {
        this.value = value;
    }


    public String getValue()
    {
        return value;
    }


    @Override
    public String toString()
    {
        return '<' + value + '>';
    }


    public String toString(Prologue prologue)
    {
        if(prologue == null)
            return '<' + value + '>';

        Collection<Prefix> prefixes = prologue.getPrefixes();

        if(!prefixes.isEmpty())
        {
            List<String> options = new ArrayList<>();

            String thisString = value.toString();

            for(Prefix prefix : prefixes)
            {
                String prefixString = prefix.getIri();

                if(thisString.startsWith(prefixString)
                /*&& thisString.substring(prefixString.length()).matches("[a-zA-Z0-9_]*")*/)
                {
                    options.add(prefix.getName() + ':' + thisString.substring(prefixString.length())
                            .replaceAll("[-~.!$&'()*+,;=/?#@%]", "\\\\$0"));
                }
            }

            if(!options.isEmpty())
                return options.stream().collect(Collectors.minBy(Comparator.comparingInt(s -> s.length()))).get();
        }


        if(prologue.getBase() != null)
        {
            try
            {
                return '<' + new URI(prologue.getBase().getValue()).relativize(new URI(value)).toString() + '>';
            }
            catch(URISyntaxException e)
            {
            }
        }

        return '<' + value + '>';
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;

        if(o == null || getClass() != o.getClass())
            return false;

        IRI iri = (IRI) o;

        return value.equals(iri.value);
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }
}
