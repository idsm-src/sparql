package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract non-sealed class UserDatatype extends Datatype
{
    private final LiteralClass baseClass;
    private final LiteralClass canonicalClass;
    private final ResourceClass nonCanonicalClass;


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
}
