package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.dirLanguageTaggedString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfDirLangStringIri;
import cz.iocb.sparql.engine.mapping.classes.DirLangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The rdf:dirLangString datatype; a literal is classified by its base direction and language tag. As each base
 * direction has a class of its own, the base and canonical class of the datatype is the union of the two.
 */
public final class DirLangStringDatatype extends StringLiteralDatatype
{
    /**
     * Creates the datatype.
     */
    protected DirLangStringDatatype()
    {
        super(rdfDirLangStringIri);
    }


    @Override
    public ResourceClass getBaseLiteralClass()
    {
        return dirLanguageTaggedString;
    }


    @Override
    public ResourceClass getCanonicalLiteralClass()
    {
        return dirLanguageTaggedString;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!(literal instanceof DirLangStringLiteral dirLiteral) || !isValidForm(literal.getValue()))
            return unsupportedType;

        return DirLangStringWithTagClass.get(dirLiteral.getDirection(), dirLiteral.getTag());
    }
}
