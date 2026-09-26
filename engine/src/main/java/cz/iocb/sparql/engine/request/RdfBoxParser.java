package cz.iocb.sparql.engine.request;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



/**
 * Decoder of the text form of a {@code sparql.rdfbox} value, as the output function of the pgsparql extension prints
 * it: an IRI as {@code <iri>}, an integer blank node as {@code _:i} and sixteen hexadecimal digits packing the segment
 * and the value, a string blank node as {@code _:s} and the segment with the value escaped byte by byte, a
 * language-tagged string as {@code "value"@tag}, any other literal as {@code "lexical"^^<datatype>} and a user literal
 * as {@code 'lexical:sqltype'^^<datatype>}. The accepted syntax is exactly what the output function produces; anything
 * else is reported as an error, as it means the engine and the extension disagree.
 */
public final class RdfBoxParser
{
    /**
     * Not instantiable.
     */
    private RdfBoxParser()
    {
    }


    /**
     * Decodes the text form of a box into the term it holds.
     *
     * @param text text form of the box
     * @return the term held by the box
     * @throws IllegalArgumentException if the text is not a valid text form of a box
     */
    public static RdfTerm parse(String text)
    {
        if(text.startsWith("<") && text.endsWith(">"))
            return new Iri(text.substring(1, text.length() - 1));

        if(text.startsWith("_:i"))
            return parseIntBlankNode(text);

        if(text.startsWith("_:s"))
            return parseStrBlankNode(text);

        if(text.startsWith("\"") || text.startsWith("'"))
            return parseLiteral(text);

        throw new IllegalArgumentException("invalid rdfbox text: " + text);
    }


    /**
     * Decodes an integer blank node: the sixteen hexadecimal digits after the prefix hold the segment in the upper and
     * the value in the lower half.
     *
     * @param text text form of the box
     * @return the blank node
     * @throws IllegalArgumentException if the text is not a valid text form of an integer blank node
     */
    private static IntBlankNode parseIntBlankNode(String text)
    {
        if(text.length() != 19)
            throw new IllegalArgumentException("invalid rdfbox text: " + text);

        try
        {
            long packed = Long.parseUnsignedLong(text, 3, 19, 16);
            return new IntBlankNode((int) packed, (int) (packed >>> 32));
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("invalid rdfbox text: " + text, e);
        }
    }


    /**
     * Decodes a string blank node: after the prefix, every byte that is not an ASCII letter or digit is escaped as
     * {@code -XX}; the first eight decoded bytes are the segment in hexadecimal, the rest is the UTF-8 value.
     *
     * @param text text form of the box
     * @return the blank node
     * @throws IllegalArgumentException if the text is not a valid text form of a string blank node
     */
    private static StrBlankNode parseStrBlankNode(String text)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        for(int i = 3; i < text.length(); i++)
        {
            char chr = text.charAt(i);

            if(chr == '-')
            {
                if(i + 2 >= text.length())
                    throw new IllegalArgumentException("invalid rdfbox text: " + text);

                bytes.write(hexDigit(text, i + 1) << 4 | hexDigit(text, i + 2));
                i += 2;
            }
            else if(isAlphanumeric(chr))
            {
                bytes.write(chr);
            }
            else
            {
                throw new IllegalArgumentException("invalid rdfbox text: " + text);
            }
        }

        byte[] data = bytes.toByteArray();

        if(data.length < 8)
            throw new IllegalArgumentException("invalid rdfbox text: " + text);

        String segment = new String(data, 0, 8, StandardCharsets.US_ASCII);
        String value = new String(data, 8, data.length - 8, StandardCharsets.UTF_8);

        try
        {
            return new StrBlankNode(value, Integer.parseUnsignedInt(segment, 16));
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("invalid rdfbox text: " + text, e);
        }
    }


    /**
     * Decodes a literal: the quoted and escaped lexical form followed by a language tag or a datatype. Apostrophes
     * enclose a user literal, whose lexical form is followed by a colon and the name of the SQL type of the value.
     *
     * @param text text form of the box
     * @return the literal
     * @throws IllegalArgumentException if the text is not a valid text form of a literal
     */
    private static RdfTerm parseLiteral(String text)
    {
        char quote = text.charAt(0);
        StringBuilder value = new StringBuilder();
        int i = 1;

        while(true)
        {
            if(i >= text.length())
                throw new IllegalArgumentException("invalid rdfbox text: " + text);

            char chr = text.charAt(i++);

            if(chr == quote)
                break;

            if(chr == '\\')
            {
                if(i >= text.length())
                    throw new IllegalArgumentException("invalid rdfbox text: " + text);

                value.append(unescape(text, i++));
            }
            else
            {
                value.append(chr);
            }
        }

        String suffix = text.substring(i);

        if(quote == '"' && suffix.startsWith("@"))
            return new LangStringLiteral(value.toString(), suffix.substring(1));

        if(suffix.startsWith("^^<") && suffix.endsWith(">"))
        {
            Iri type = new Iri(suffix.substring(3, suffix.length() - 1));

            if(quote == '"')
                return new TypedLiteral(value.toString(), type);

            return new TypedLiteral(userLiteralLexical(text, value.toString()), type);
        }

        throw new IllegalArgumentException("invalid rdfbox text: " + text);
    }


    /**
     * Lexical form of a user literal from the text form of its {@code sparql.ubox} value ({@code lexical:sqltype}). The
     * lexical form may contain anything while the canonical type name cannot contain a colon outside of double quotes,
     * so the separator is the last colon not enclosed in double quotes.
     *
     * @param text text form of the box, for error messages
     * @param ubox text form of the ubox value
     * @return the lexical form
     * @throws IllegalArgumentException if the text has no separator
     */
    private static String userLiteralLexical(String text, String ubox)
    {
        boolean inQuotes = false;

        for(int pos = ubox.length() - 1; pos >= 0; pos--)
        {
            char chr = ubox.charAt(pos);

            if(chr == '"')
                inQuotes = !inQuotes;
            else if(chr == ':' && !inQuotes)
                return ubox.substring(0, pos);
        }

        throw new IllegalArgumentException("invalid rdfbox text: " + text);
    }


    /**
     * Character denoted by the escape sequence whose letter is at the given position.
     *
     * @param text text form of the box
     * @param index position of the letter following the backslash
     * @return the character denoted by the escape sequence
     * @throws IllegalArgumentException if the escape sequence is unknown
     */
    private static char unescape(String text, int index)
    {
        return switch(text.charAt(index))
        {
            case '\\' -> '\\';
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 'f' -> '\f';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> throw new IllegalArgumentException("invalid rdfbox text: " + text);
        };
    }


    /**
     * Value of the hexadecimal digit at the given position.
     *
     * @param text text form of the box
     * @param index position of the digit
     * @return value of the digit
     * @throws IllegalArgumentException if the character is not a hexadecimal digit
     */
    private static int hexDigit(String text, int index)
    {
        int value = Character.digit(text.charAt(index), 16);

        if(value < 0)
            throw new IllegalArgumentException("invalid rdfbox text: " + text);

        return value;
    }


    /**
     * True if the character is an ASCII letter or digit, i.e. a byte the string blank node output leaves unescaped.
     *
     * @param chr the character
     * @return true if the character is an ASCII letter or digit, false otherwise
     */
    private static boolean isAlphanumeric(char chr)
    {
        return chr >= '0' && chr <= '9' || chr >= 'A' && chr <= 'Z' || chr >= 'a' && chr <= 'z';
    }
}
