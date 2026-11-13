package cz.iocb.sparql.engine.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Resolution of IRI references against a base IRI by the algorithm of section 5.2 of RFC 3986, which performs no
 * normalization but the removal of dot segments. {@link java.net.URI#resolve(java.net.URI)} is no substitute: it drops
 * an empty authority, replaces the last segment of the base path also for an empty reference and for a reference
 * holding only a query, and keeps the dot segments leading above the root.
 */
public final class IriResolver
{
    /**
     * Regular expression of appendix B of RFC 3986, which matches every string and splits an IRI reference into its
     * components: the scheme (group 2), the authority (group 4), the path (group 5), the query (group 7) and the
     * fragment (group 9); the group of a component that is not present is null, but for the path, which is empty.
     */
    private static final Pattern components = Pattern
            .compile("(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?", Pattern.DOTALL);


    /**
     * Not to be instantiated.
     */
    private IriResolver()
    {
    }


    /**
     * Resolves an IRI reference against a base IRI (section 5.2.2 of RFC 3986, with the strict parser).
     *
     * @param base the base IRI
     * @param reference the IRI reference
     * @return the target IRI; it is relative if the base is not absolute and the reference has no scheme
     */
    public static String resolve(String base, String reference)
    {
        Matcher b = components.matcher(base);
        Matcher r = components.matcher(reference);

        if(!b.matches() || !r.matches())
            throw new IllegalStateException();

        String scheme = b.group(2);
        String authority = b.group(4);
        String path = b.group(5);
        String query = b.group(7);

        if(r.group(2) != null)
        {
            scheme = r.group(2);
            authority = r.group(4);
            path = removeDotSegments(r.group(5));
            query = r.group(7);
        }
        else if(r.group(4) != null)
        {
            authority = r.group(4);
            path = removeDotSegments(r.group(5));
            query = r.group(7);
        }
        else if(!r.group(5).isEmpty())
        {
            path = removeDotSegments(r.group(5).startsWith("/") ? r.group(5) : merge(authority, path, r.group(5)));
            query = r.group(7);
        }
        else if(r.group(7) != null)
        {
            query = r.group(7);
        }

        StringBuilder target = new StringBuilder();

        if(scheme != null)
            target.append(scheme).append(':');

        if(authority != null)
            target.append("//").append(authority);

        target.append(path);

        if(query != null)
            target.append('?').append(query);

        if(r.group(9) != null)
            target.append('#').append(r.group(9));

        return target.toString();
    }


    /**
     * Merges a relative path with the path of the base IRI (section 5.2.3 of RFC 3986): the relative path replaces the
     * last segment of the base path.
     *
     * @param authority the authority of the base IRI, or null if it has none
     * @param basePath the path of the base IRI
     * @param path the relative path
     * @return the merged path
     */
    private static String merge(String authority, String basePath, String path)
    {
        if(authority != null && basePath.isEmpty())
            return "/" + path;

        return basePath.substring(0, basePath.lastIndexOf('/') + 1) + path;
    }


    /**
     * Removes the segments {@code .} and {@code ..} of a path, the latter together with the segment preceding it
     * (section 5.2.4 of RFC 3986).
     *
     * @param path the path
     * @return the path without dot segments
     */
    private static String removeDotSegments(String path)
    {
        StringBuilder output = new StringBuilder(path.length());
        int length = path.length();
        int position = 0;

        while(position < length)
        {
            if(path.startsWith("../", position))
            {
                position += 3;
            }
            else if(path.startsWith("./", position) || path.startsWith("/./", position))
            {
                position += 2;
            }
            else if(path.startsWith("/../", position))
            {
                removeLastSegment(output);
                position += 3;
            }
            else if(path.startsWith("/.", position) && position + 2 == length)
            {
                output.append('/');
                position = length;
            }
            else if(path.startsWith("/..", position) && position + 3 == length)
            {
                removeLastSegment(output);
                output.append('/');
                position = length;
            }
            else if(path.startsWith(".", position) && position + 1 == length
                    || path.startsWith("..", position) && position + 2 == length)
            {
                position = length;
            }
            else
            {
                int end = path.indexOf('/', position + 1);

                if(end == -1)
                    end = length;

                output.append(path, position, end);
                position = end;
            }
        }

        return output.toString();
    }


    /**
     * Removes the last segment of the path together with the slash preceding it, if there is any.
     *
     * @param path the path
     */
    private static void removeLastSegment(StringBuilder path)
    {
        path.setLength(Math.max(path.lastIndexOf("/"), 0));
    }
}
