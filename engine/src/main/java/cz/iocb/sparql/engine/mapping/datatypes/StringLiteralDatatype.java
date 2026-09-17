package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



public abstract sealed class StringLiteralDatatype extends Datatype permits StringDatatype, LangStringDatatype
{
    protected StringLiteralDatatype(Iri typeIri)
    {
        super(typeIri);
    }


    @Override
    public boolean isValidForm(String value)
    {
        return true;
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return true;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        return value;
    }
}
