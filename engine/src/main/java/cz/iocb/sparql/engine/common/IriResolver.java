package cz.iocb.sparql.engine.common;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
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
     * Regular expression of the IRI references of section 2.2 of RFC 3987 (the rule IRI-reference), which SPARQL
     * requires of the text of every IRI reference of a query after its escape sequences are processed.
     */
    private static final Pattern iriReference = Pattern.compile(getIriReferenceRegexp());

    /**
     * Regular expression of a percent sign that begins no percent-encoded octet, which no IRI reference holds.
     */
    private static final Pattern strayPercent = Pattern.compile("%(?![0-9A-Fa-f]{2})");


    /**
     * Not to be instantiated.
     */
    private IriResolver()
    {
    }


    /**
     * Whether the text is an IRI reference by the syntax of section 2.2 of RFC 3987 (the rule IRI-reference): an IRI or
     * a relative reference built of the characters of a URI reference, percent-encoded octets and the characters RFC
     * 3987 adds to them (ucschar, and iprivate in the query). It is the syntax SPARQL requires of IRI references
     * (section 19.5 of SPARQL 1.2); {@link java.net.URI} is no substitute, as it rejects characters an IRI may hold,
     * like the no-break space or the line separator, and an empty hierarchical part, like that of {@code urn:}.
     *
     * @param text the text
     * @return whether the text is an IRI reference
     */
    public static boolean isIriReference(String text)
    {
        return !strayPercent.matcher(text).find() && iriReference.matcher(text).matches();
    }


    /**
     * Whether an IRI reference has a scheme, which makes it an IRI rather than a relative reference.
     *
     * @param reference the IRI reference
     * @return whether the reference has a scheme
     */
    public static boolean hasScheme(String reference)
    {
        Matcher matcher = components.matcher(reference);

        return matcher.matches() && matcher.group(2) != null;
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


    /**
     * Regular expression of the rule IRI-reference of section 2.2 of RFC 3987, transcribed from its ABNF, but for the
     * percent-encoded octets: the expression takes a percent sign for a character wherever the ABNF allows an octet so
     * encoded, and {@link #strayPercent} finds a sign that begins none. So the expression repeats single character
     * classes only, which Java evaluates by iteration, whereas it recurses for every repetition of a group and would
     * overflow the stack on a long IRI. A path is then any sequence of the characters of its segments and slashes that
     * starts as the ABNF of the path says, and a host is an IP literal or a registered name, whose syntax covers that
     * of an IPv4 address.
     *
     * @return regular expression of the IRI references
     */
    private static String getIriReferenceRegexp()
    {
        StringBuilder ucschar = new StringBuilder("\\x{A0}-\\x{D7FF}\\x{F900}-\\x{FDCF}\\x{FDF0}-\\x{FFEF}");

        // the planes 1 to 14 without their last two code points, the plane 14 from U+E1000
        for(int plane = 1; plane <= 14; plane++)
            ucschar.append(format(ROOT, "\\x{%X%s}-\\x{%XFFFD}", plane, plane < 14 ? "0000" : "1000", plane));

        String iprivate = "\\x{E000}-\\x{F8FF}\\x{F0000}-\\x{FFFFD}\\x{100000}-\\x{10FFFD}";
        String iunreserved = "A-Za-z0-9\\-._~" + ucschar;
        String subDelims = "!$&'()*+,;=";

        // the characters of the components, a percent sign standing for a percent-encoded octet
        String ipchar = "[" + iunreserved + subDelims + ":@%]";
        String ipath = "[" + iunreserved + subDelims + ":@%/]*";
        String ipathAbempty = "(?:/" + ipath + ")?";
        String ipathAbsolute = "/(?:" + ipchar + ipath + ")?";
        String ipathRootless = ipchar + ipath;
        String ipathNoscheme = "[" + iunreserved + subDelims + "@%]+" + ipathAbempty;
        String iquery = "[" + iunreserved + subDelims + ":@%/?" + iprivate + "]*";
        String ifragment = "[" + iunreserved + subDelims + ":@%/?]*";

        String h16 = "[0-9A-Fa-f]{1,4}";
        String decOctet = "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])";
        String ls32 = format(ROOT, "(?:%s:%s|%s(?:\\.%s){3})", h16, h16, decOctet, decOctet);

        // the nine forms of an IPv6 address of section 3.2.2 of RFC 3986, written with P for a piece and its colon, H
        // for a piece and L for the last 32 bits; none of the expressions put in holds a later one of these letters
        String forms = "P{6}L|::P{5}L|(?:H)?::P{4}L|(?:P{0,1}H)?::P{3}L|(?:P{0,2}H)?::P{2}L|(?:P{0,3}H)?::PL"
                + "|(?:P{0,4}H)?::L|(?:P{0,5}H)?::H|(?:P{0,6}H)?::";
        String ipv6address = "(?:" + forms.replace("P", "(?:" + h16 + ":)").replace("H", h16).replace("L", ls32) + ")";

        String ipLiteral = format(ROOT, "\\[(?:%s|v[0-9A-Fa-f]+\\.[A-Za-z0-9\\-._~%s:]+)\\]", ipv6address, subDelims);
        String iuserinfo = "[" + iunreserved + subDelims + ":%]*";
        String iregName = "[" + iunreserved + subDelims + "%]*";
        String iauthority = format(ROOT, "(?:%s@)?(?:%s|%s)(?::[0-9]*)?", iuserinfo, ipLiteral, iregName);
        String tail = format(ROOT, "(?:\\?%s)?(?:#%s)?", iquery, ifragment);

        String iri = format(ROOT, "[A-Za-z][A-Za-z0-9+\\-.]*:(?://%s%s|%s|%s|)%s", iauthority, ipathAbempty,
                ipathAbsolute, ipathRootless, tail);
        String irelativeRef = format(ROOT, "(?://%s%s|%s|%s|)%s", iauthority, ipathAbempty, ipathAbsolute,
                ipathNoscheme, tail);

        return "(?:" + iri + "|" + irelativeRef + ")";
    }
}
