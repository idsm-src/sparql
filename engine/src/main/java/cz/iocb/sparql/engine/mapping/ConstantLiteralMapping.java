package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class ConstantLiteralMapping extends ConstantMapping
{
    public ConstantLiteralMapping(LiteralClass literalClass, Literal value)
    {
        super(value, literalClass, literalClass.toColumns(value));
    }
}
