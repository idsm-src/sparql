package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Mapping of a position to a fixed literal, represented by the constant columns of its literal class.
 */
public class ConstantLiteralMapping extends ConstantMapping
{
    /**
     * Creates the mapping; the columns are the constant columns of the class for the literal.
     *
     * @param literalClass the literal class
     * @param value the literal
     */
    public ConstantLiteralMapping(ResourceClass literalClass, Literal value)
    {
        super(value, literalClass, literalClass.toColumns(null, value));
    }
}
