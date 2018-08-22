package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlBaseClass;



public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    private static final List<ResourceClass> numericOrder = Arrays.asList(xsdShort, xsdInt, xsdLong, xsdInteger,
            xsdDecimal, xsdFloat, xsdDouble);

    protected final boolean canBeNull;
    protected final boolean isBoxed;
    protected final Set<ExpressionResourceClass> resourceClasses;


    protected SqlExpressionIntercode(Set<ExpressionResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
    {
        this.canBeNull = canBeNull;
        this.isBoxed = isBoxed;
        this.resourceClasses = resourceClasses;
    }


    public abstract SqlExpressionIntercode optimize(VariableAccessor variableAccessor);


    public abstract String translate();


    public boolean canBeNull()
    {
        return canBeNull;
    }


    public boolean isBoxed()
    {
        return isBoxed;
    }


    public Set<ExpressionResourceClass> getResourceClasses()
    {
        return resourceClasses;
    }


    protected String getResourceName()
    {
        if(isBoxed)
            return "rdfbox";

        return resourceClasses.iterator().next().getName();
    }


    protected static boolean isNumeric(ResourceClass resClass)
    {
        return numericOrder.contains(resClass);
    }


    protected static boolean isNumericCompatibleWith(ResourceClass resClass, ResourceClass requestClass)
    {
        if(!isNumeric(resClass))
            return false;

        if(requestClass == null)
            return true;

        return numericOrder.indexOf(resClass) <= numericOrder.indexOf(requestClass);
    }


    protected static boolean isEffectiveBooleanClass(ResourceClass resClass)
    {
        return isNumeric(resClass) || resClass == xsdBoolean || resClass == xsdString || resClass == unsupportedLiteral;
    }
}
