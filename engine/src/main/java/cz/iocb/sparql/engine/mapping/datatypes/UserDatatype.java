package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Datatype defined by a deployment and backed by a PostgreSQL user type. It creates its own canonical and base literal
 * classes; subclasses define validity and canonicalisation of the lexical forms.
 */
public abstract non-sealed class UserDatatype extends Datatype
{
    /**
     * Class of all valid literals with their lexical form.
     */
    private final LiteralClass baseClass;

    /**
     * Class of the canonical literals.
     */
    private final LiteralClass canonicalClass;

    /**
     * Class of the valid but non-canonical literals.
     */
    private final ResourceClass nonCanonicalClass;


    /**
     * Creates the datatype with literal classes named {@code name} and {@code base-name} over the SQL type.
     *
     * @param typeIri the datatype IRI
     * @param name the name
     * @param type the PostgreSQL user type
     */
    protected UserDatatype(Iri typeIri, String name, UserType type)
    {
        super(typeIri);

        baseClass = new UserLiteralBaseClass("base-" + name, type, this);
        canonicalClass = new UserLiteralClass(name, type, this, baseClass);
        nonCanonicalClass = subtract(baseClass, canonicalClass);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return baseClass;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return canonicalClass;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return nonCanonicalClass;

        return canonicalClass;
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return value.equals(getCanonicalLexicalForm(value));
    }


    /**
     * SQL function parsing a lexical form of the datatype, which makes the IRI of the datatype name a cast to it, like
     * the constructor functions of the XSD datatypes name casts to theirs ({@code <datatype>(?x)}), taking precedence
     * over an extension function of the same IRI; null, the default, for a datatype without a cast. The function takes
     * a {@code varchar} and returns a value of the user type: for exactly the forms {@link #isValidForm} accepts, the
     * value that {@link #getCanonicalLexicalForm} prints, and NULL for the other forms, never an error (an error ends
     * the query). It should be STRICT, PARALLEL SAFE and at least STABLE, at best IMMUTABLE, so that the database
     * evaluates the cast like any other expression.
     *
     * @return SQL function parsing a lexical form of the datatype, or null for a datatype without a cast
     */
    public Function getLexicalParser()
    {
        return null;
    }
}
