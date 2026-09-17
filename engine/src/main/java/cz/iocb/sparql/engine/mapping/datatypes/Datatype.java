package cz.iocb.sparql.engine.mapping.datatypes;

import java.util.Objects;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract sealed class Datatype permits BooleanDatatype, GenericIntegerDataType, DecimalDatatype,
        FloatPointDatatype, TemporalDatatype, DayTimeDurationDatatype, StringLiteralDatatype, UserDatatype
{
    protected static final String WS = "[\\t\\n\\r ]*";

    protected final Iri typeIri;


    protected Datatype(Iri typeIri)
    {
        this.typeIri = typeIri;
    }


    public abstract LiteralClass getBaseLiteralClass();


    public abstract LiteralClass getCanonicalLiteralClass();


    public abstract ResourceClass getResourceClass(Literal literal);


    public abstract boolean isValidForm(String value);


    public abstract boolean isCanonicalForm(String value);


    public abstract String getCanonicalLexicalForm(String literalValue);


    public Iri getTypeIri()
    {
        return typeIri;
    }


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
