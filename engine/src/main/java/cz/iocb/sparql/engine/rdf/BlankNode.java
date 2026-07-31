package cz.iocb.sparql.engine.rdf;



public abstract class BlankNode extends RdfTerm
{
    public abstract String getLabel();


    @Override
    public String toString()
    {
        return "_:" + getLabel();
    }
}
