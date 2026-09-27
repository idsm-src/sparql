package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Common base of xsd:string, rdf:langString and rdf:dirLangString: every lexical form is valid and canonical.
 */
public abstract sealed class StringLiteralDatatype extends Datatype
        permits StringDatatype, LangStringDatatype, DirLangStringDatatype
{
    /**
     * Creates the datatype.
     *
     * @param typeIri the datatype IRI
     */
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
