package cz.iocb.sparql.engine.mapping.datatypes;

import java.util.Objects;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Describes a literal datatype: which lexical forms are valid and which canonical, how to canonicalise them, and which
 * resource classes its literals fall into.
 */
public abstract sealed class Datatype permits BooleanDatatype, GenericIntegerDataType, DecimalDatatype,
        FloatPointDatatype, TemporalDatatype, DayTimeDurationDatatype, StringLiteralDatatype, UserDatatype
{
    /**
     * Optional whitespace, as allowed around lexical forms by the XML Schema collapse facet.
     */
    protected static final String WS = "[\\t\\n\\r ]*";

    /**
     * IRI of the datatype.
     */
    protected final Iri typeIri;


    /**
     * Creates the datatype.
     *
     * @param typeIri the datatype IRI
     */
    protected Datatype(Iri typeIri)
    {
        this.typeIri = typeIri;
    }


    /**
     * Class holding every valid literal of the datatype together with its lexical form.
     *
     * @return class holding every valid literal of the datatype together with its lexical form
     */
    public abstract LiteralClass getBaseLiteralClass();


    /**
     * Class holding the literals in canonical form only.
     *
     * @return class holding the literals in canonical form only
     */
    public abstract LiteralClass getCanonicalLiteralClass();


    /**
     * Most specific class of the literal (which must have this datatype): the canonical class, the class of
     * non-canonical forms, or {@link cz.iocb.sparql.engine.mapping.classes.BuiltinClasses#unsupportedType} when the
     * lexical form is invalid.
     *
     * @param literal the literal
     * @return most specific class of the literal (which must have this datatype): the canonical class, the class of
     *         non-canonical forms, or {@link cz.iocb.sparql.engine.mapping.classes.BuiltinClasses#unsupportedType} when
     *         the lexical form is invalid
     */
    public abstract ResourceClass getResourceClass(Literal literal);


    /**
     * True if the lexical form is valid for the datatype (within the limits of its PostgreSQL representation).
     *
     * @param value the lexical form
     * @return true if the lexical form is valid for the datatype (within the limits of its PostgreSQL representation),
     *         false otherwise
     */
    public abstract boolean isValidForm(String value);


    /**
     * True if the (valid) lexical form is canonical.
     *
     * @param value the lexical form
     * @return true if the (valid) lexical form is canonical, false otherwise
     */
    public abstract boolean isCanonicalForm(String value);


    /**
     * Canonical lexical form of a valid lexical form.
     *
     * @param literalValue the lexical form
     * @return canonical lexical form of a valid lexical form
     */
    public abstract String getCanonicalLexicalForm(String literalValue);


    /**
     * IRI of the datatype.
     *
     * @return IRI of the datatype
     */
    public Iri getTypeIri()
    {
        return typeIri;
    }


    /**
     * Applies the XML Schema whitespace collapse: runs of whitespace become one space and leading and trailing spaces
     * are removed.
     *
     * @param value the lexical form
     * @return the collapsed form
     */
    public static String getCollapsedForm(String value)
    {
        return value.replaceAll("[\\t\\n\\r ]+", " ").replaceFirst("^ +", "").replaceFirst(" +$", "");
    }


    @Override
    public int hashCode()
    {
        return typeIri.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Datatype other = (Datatype) object;

        return Objects.equals(typeIri, other.typeIri);
    }
}
