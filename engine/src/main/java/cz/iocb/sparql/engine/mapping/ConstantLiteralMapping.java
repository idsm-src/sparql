package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class ConstantLiteralMapping extends ConstantMapping
{
    public ConstantLiteralMapping(ResourceClass literalClass, Literal value)
    {
        super(value, literalClass, literalClass.toColumns(null, value));
    }
}
