package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



public abstract class StringLiteralDatatype extends Datatype
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
    public String getCanonicalLexicalForm(String value)
    {
        return value.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\b", "\b")
                .replace("\\f", "\f").replace("\\\"", "\"").replace("\\'", "\'").replace("\\\\", "\\");
    }
}
