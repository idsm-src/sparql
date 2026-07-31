package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringIri;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;



public class LangStringDatatype extends StringLiteralDatatype
{
    protected LangStringDatatype()
    {
        super(rdfLangStringIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return rdfLangString;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!(literal instanceof LangStringLiteral lang) || !isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return LangStringConstantTagClass.get(lang.getTag());
    }
}
