package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;
import java.util.Objects;
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
     * Class holding every valid literal of the datatype together with its lexical form; a union of primitive classes
     * when the literals of the datatype are spread over several of them (rdf:dirLangString).
     *
     * @return class holding every valid literal of the datatype together with its lexical form
     */
    public abstract ResourceClass getBaseLiteralClass();


    /**
     * Class holding the literals in canonical form only; a union of primitive classes when the literals of the datatype
     * are spread over several of them (rdf:dirLangString).
     *
     * @return class holding the literals in canonical form only
     */
    public abstract ResourceClass getCanonicalLiteralClass();


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
     * Datatype this datatype is derived from by restriction in XML Schema (its base type definition): xsd:decimal for
     * xsd:integer, xsd:integer for xsd:long and xsd:nonNegativeInteger, and so on down to xsd:byte and
     * xsd:unsignedByte. Null for a primitive datatype and for a datatype whose derivation the engine does not model.
     *
     * @return datatype this datatype is derived from, or null
     */
    public Datatype getSuperType()
    {
        return null;
    }


    /**
     * True if this datatype is the given datatype or is derived from it by restriction, directly or through other
     * datatypes (see {@link #getSuperType}).
     *
     * @param datatype the datatype
     * @return true if this datatype is the given datatype or is derived from it, false otherwise
     */
    public final boolean isDerivedFrom(Datatype datatype)
    {
        for(Datatype type = this; type != null; type = type.getSuperType())
            if(type.equals(datatype))
                return true;

        return false;
    }


    /**
     * True if XPath takes a value of this datatype for a value of the given datatype as an argument of a function: this
     * datatype is derived from it (subtype substitution), or it is xsd:double and this datatype is derived from
     * xsd:float or xsd:decimal, or it is xsd:float and this datatype is derived from xsd:decimal (numeric type
     * promotion). A function of SPARQL takes such a value too, and so does an extension function (see
     * {@link cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode#getAcceptedClass}).
     *
     * @param datatype the datatype of the argument the function takes
     * @return true if XPath takes a value of this datatype for a value of the given datatype, false otherwise
     */
    public final boolean isPromotableTo(Datatype datatype)
    {
        if(isDerivedFrom(datatype))
            return true;

        if(datatype.equals(xsdDoubleType))
            return isDerivedFrom(xsdFloatType) || isDerivedFrom(xsdDecimalType);

        if(datatype.equals(xsdFloatType))
            return isDerivedFrom(xsdDecimalType);

        return false;
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
