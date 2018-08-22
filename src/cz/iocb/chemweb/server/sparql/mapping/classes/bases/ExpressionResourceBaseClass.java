package cz.iocb.chemweb.server.sparql.mapping.classes.bases;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionLiteralClass;



public abstract class ExpressionResourceBaseClass extends ResourceBaseClass implements ExpressionLiteralClass
{
    protected ExpressionResourceBaseClass(String name)
    {
        super(name);
    }
}
