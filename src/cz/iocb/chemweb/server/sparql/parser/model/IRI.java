package cz.iocb.chemweb.server.sparql.parser.model;

import java.net.URI;
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
public final class IRI extends BaseComplexNode implements VarOrIri, Path, Define.DefineValue
{
    private URI uri;

    public IRI(String uri)
    {
        if(hasBrackets(uri))
            uri = removeBrackets(uri);

        setUri(URI.create(uri));
    }

    public IRI(URI uri)
    {
        setUri(uri);
    }

    public URI getUri()
    {
        return uri;
    }

    public void setUri(URI uri)
    {
        if(uri == null)
            throw new IllegalArgumentException();

        this.uri = uri;
    }

    public URI getAbsoluteURI(Prologue prologue)
    {
        return prologue.getBase().getUri().resolve(uri);
    }

    public static boolean hasBrackets(String s)
    {
        return s.startsWith("<") && s.endsWith(">");
    }

    public static String removeBrackets(String s)
    {
        if(!hasBrackets(s))
            throw new IllegalArgumentException();

        return s.substring(1, s.length() - 1);
    }

    public static URI parseURI(String s)
    {
        if(s == null)
            return null;

        return URI.create(removeBrackets(s));
    }


    @Override
    public String toString()
    {
        return "<" + uri + ">";
    }


    public String toString(Prologue prologue)
    {
        if(prologue == null)
            return "<" + uri + ">";

        Collection<Prefix> prefixes = prologue.getAllPrefixes();

        if(!prefixes.isEmpty())
        {
            List<String> options = new ArrayList<>();

            String thisString = uri.toString();

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


        URI result;

        if(prologue.getBase() != null)
            result = prologue.getBase().getUri().relativize(uri);
        else
            result = uri;

        return "<" + result + ">";
    }

    /*
     * public String toString(Prologue prologue) { return toString(prologue,
     * false, true); }
     */

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

        return uri.equals(iri.uri);
    }

    @Override
    public int hashCode()
    {
        return uri.hashCode();
    }
}
