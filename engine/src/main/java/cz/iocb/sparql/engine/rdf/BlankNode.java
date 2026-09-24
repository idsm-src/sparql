package cz.iocb.sparql.engine.rdf;



/**
 * Blank node identified by a value within a segment. Segments keep apart blank nodes coming from different sources
 * (mappings, federated services), so equal values in different segments are different nodes.
 */
public abstract class BlankNode extends RdfTerm
{
    /**
     * Label of the node as shown in query results; encodes both the segment and the value.
     *
     * @return label of the node as shown in query results; encodes both the segment and the value
     */
    public abstract String getLabel();


    /**
     * Creates the node.
     */
    protected BlankNode()
    {
    }


    @Override
    public String toString()
    {
        return "_:" + getLabel();
    }
}
