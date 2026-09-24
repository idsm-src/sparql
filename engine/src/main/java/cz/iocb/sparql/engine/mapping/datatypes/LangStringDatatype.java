package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringIri;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The rdf:langString datatype; a literal is classified by its language tag.
 */
public final class LangStringDatatype extends StringLiteralDatatype
{
    /**
     * Creates the datatype.
     */
    protected LangStringDatatype()
    {
        super(rdfLangStringIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return rdfLangString;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return rdfLangString;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!(literal instanceof LangStringLiteral lang) || !isValidForm(literal.getValue()))
            return unsupportedType;

        return LangStringWithTagClass.get(lang.getTag());
    }
}
