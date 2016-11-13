package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference;

import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;



/**
 * Occurrence of a variable in a query with its type as it is derived from local context. Occurrence is specified by a
 * triple, position in a triple and type constructed as {@link TypeElement}. Local context can imply more complicated
 * type structures than a single constraint.
 *
 */
public class Occurrence
{
    public final Element element;

    /** Type of a variable derived from this context */
    public TypeElement type;

    public Range range;


    public Occurrence(TypeElement type, Element element)
    {
        this.type = type;
        this.range = element.getRange();
        this.element = element;
    }
}
