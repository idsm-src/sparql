package cz.iocb.chemweb.server.sparql.parser.model.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.StreamUtils;
import cz.iocb.chemweb.server.sparql.parser.Xsd;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



/**
 * Represents a literal value ({@link #getStringValue}) that can have a type ( {@link #getTypeIri}) or a language tag (
 * {@link #getLanguageTag}). This includes the shorthand forms for numeric and boolean literals.
 *
 * <p>
 * For some common types, a converted value ({@link #getValue}), along with its Java type ({@link #getJavaClass}) is
 * also provided.
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar:
 * <ul>
 * <li>[129] RDFLiteral
 * <li>[130] NumericLiteral
 * <li>[134] BooleanLiteral
 * </ul>
 */
public class Literal extends BaseComplexNode implements Expression, Node
{
    private static IRI stringIRI = new IRI("http://www.w3.org/2001/XMLSchema#string");

    private String stringValue;
    private Object value;
    private String languageTag;
    private Type<?> type = Type.getType(null);

    public Literal(String value)
    {
        setStringValue(value);
    }

    public Literal(String value, String languageTag)
    {
        this(value);
        setLanguageTag(languageTag);
    }

    public Literal(String value, IRI typeIri)
    {
        this(value);

        // NOTE: added by galgonek
        if(stringIRI.equals(typeIri))
            return; // FIXME: xsd:string is treated as simple literal

        setTypeIri(typeIri);
    }

    /**
     * The string value, including quotation marks.
     */
    public String getStringValue()
    {
        return stringValue;
    }

    public void setStringValue(String stringValue)
    {
        this.stringValue = stringValue;
        this.value = type.getParser().apply(stringValue);
    }

    public Object getValue()
    {
        return value;
    }

    public String getLanguageTag()
    {
        return languageTag;
    }

    public void setLanguageTag(String languageTag)
    {
        if(languageTag != null && getTypeIri() != null)
            throw new IllegalArgumentException();

        if(languageTag != null && languageTag.startsWith("@"))
            languageTag = languageTag.substring(1);

        this.languageTag = languageTag;
    }

    public IRI getTypeIri()
    {
        return type.getIri();
    }

    public void setTypeIri(IRI typeIri)
    {
        if(languageTag != null && typeIri != null)
            throw new IllegalArgumentException();

        // NOTE: added by galgonek
        if(stringIRI.equals(typeIri))
            return; // FIXME: xsd:string is treated as simple literal

        this.type = Type.getType(typeIri);

        // to recalculate this.value
        if(stringValue != null)
            setStringValue(stringValue);
    }

    public Class<?> getJavaClass()
    {
        return type.getJavaClass();
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}

/**
 * Helper type for mapping between XSD types ({@link #getIri}) and Java types ( {@link #getJavaClass}). Also provides
 * parser function from string to the Java type ({@link #getParser}).
 */
class Type<T>
{
    private static List<Type<?>> types = Collections.unmodifiableList(Arrays.asList(
            new Type<>(Xsd.ANY_URI, URI.class, URI::create), new Type<>(Xsd.BOOLEAN, Boolean.class, Type::parseBool),
            new Type<>(Xsd.DATE_TIME_STAMP, OffsetDateTime.class, OffsetDateTime::parse),
            new Type<>(Xsd.DECIMAL, BigDecimal.class, Type::parseDecimal),
            new Type<>(Xsd.INTEGER, BigInteger.class, BigInteger::new), new Type<>(Xsd.LONG, Long.class, Long::new),
            new Type<>(Xsd.INT, Integer.class, Integer::new), new Type<>(Xsd.SHORT, Short.class, Short::new),
            new Type<>(Xsd.BYTE, Byte.class, Byte::new), new Type<>(Xsd.DOUBLE, Double.class, Type::parseDouble),
            new Type<>(Xsd.FLOAT, Float.class, Type::parseFloat), new Type<>(Xsd.STRING, String.class, s -> s),
            new Type<>((String) null, String.class, s -> s)));

    private static Boolean parseBool(String s)
    {
        if(s.equalsIgnoreCase("true") || s.equals("1"))
            return true;
        if(s.equalsIgnoreCase("false") || s.equals("0"))
            return false;

        throw new IllegalArgumentException();
    }

    private static BigDecimal parseDecimal(String s)
    {
        // the format of new BigDecimal(s) accepts exponents, which xsd:decimal
        // doesn't

        if(s.contains("e") || s.contains("E"))
            throw new IllegalArgumentException();

        return new BigDecimal(s);
    }

    private static Double parseDouble(String s)
    {
        // the differences between the format of new Double(s) and xsd:double
        // are
        // hexadecimal literals and the format of infinity

        if(s.contains("0x") || s.contains("0X"))
            throw new IllegalArgumentException();

        s = s.replace("INF", "Infinity");

        return new Double(s);
    }

    private static Float parseFloat(String s)
    {
        // the differences between the format of new Float(s) and xsd:float are
        // hexadecimal literals and the format of infinity

        if(s.contains("0x") || s.contains("0X"))
            throw new IllegalArgumentException();

        s = s.replace("INF", "Infinity");

        return new Float(s);
    }

    /**
     * Finds {@link Type} for the given XSD type. If no corresponding Java type is found, {@link #getJavaClass} of the
     * returned {@link Type} returns {@link Void}.
     */
    public static Type<?> getType(IRI typeIri)
    {
        Optional<Type<?>> type = types.stream().filter(t -> Objects.equals(t.getIri(), typeIri))
                .collect(StreamUtils.singleCollector());

        return type.orElse(new Type<>(typeIri, Void.class, s -> null));
    }

    private IRI iri;
    private Class<T> javaClass;
    private Function<String, T> parser;

    private Type(String iri, Class<T> javaClass, Function<String, T> parser)
    {
        this(iri == null ? null : new IRI(iri), javaClass, parser);
    }

    private Type(IRI iri, Class<T> javaClass, Function<String, T> parser)
    {
        this.iri = iri;
        this.javaClass = javaClass;
        this.parser = parser;
    }

    public IRI getIri()
    {
        return iri;
    }

    public Class<T> getJavaClass()
    {
        return javaClass;
    }

    public Function<String, T> getParser()
    {
        return this::parseOrNull;
    }

    /**
     * Tries to parse the given input. If that fails, returns {@code null}.
     */
    private T parseOrNull(String input)
    {
        try
        {
            return parser.apply(input);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
}
