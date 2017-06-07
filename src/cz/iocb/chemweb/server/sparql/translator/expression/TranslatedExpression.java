package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class TranslatedExpression
{
    private final ResourceClass resourceClass;
    private final boolean canBeNull;
    private final String code;


    public TranslatedExpression(ResourceClass resourceClass, boolean canBeNull, String code)
    {
        this.resourceClass = resourceClass;
        this.canBeNull = canBeNull;
        this.code = code;
    }


    public boolean isTrue()
    {
        return code.toLowerCase().matches("\\(*true\\)*");
    }


    public boolean isFalse()
    {
        return code.toLowerCase().matches("\\(*false\\)*");
    }


    public final ResourceClass getResourceClass()
    {
        return resourceClass;
    }


    public final boolean canBeNull()
    {
        return canBeNull;
    }


    public final String getCode()
    {
        return code;
    }
}
