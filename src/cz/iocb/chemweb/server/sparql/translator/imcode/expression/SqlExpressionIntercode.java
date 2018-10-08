package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlBaseClass;



public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    private static final List<ResourceClass> numericOrder = Arrays.asList(xsdShort, xsdInt, xsdLong, xsdInteger,
            xsdDecimal, xsdFloat, xsdDouble);

    protected final boolean canBeNull;
    protected final boolean isBoxed;
    protected final Set<ResourceClass> resourceClasses;


    protected SqlExpressionIntercode(Set<ResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
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


    public Set<ResourceClass> getResourceClasses()
    {
        return resourceClasses;
    }


    public ResourceClass getExpressionResourceClass()
    {
        if(isBoxed)
            return null;

        if(resourceClasses.size() == 1)
            return resourceClasses.iterator().next();

        if(resourceClasses.stream().allMatch(r -> isDateTime(r)))
            return xsdDateTime;

        if(resourceClasses.stream().allMatch(r -> isDate(r)))
            return xsdDate;

        if(resourceClasses.stream().allMatch(r -> isLangString(r)))
            return rdfLangString;

        if(resourceClasses.stream().allMatch(r -> isIri(r)))
            return iri;

        assert false;
        return null;
    }


    protected String getResourceName()
    {
        if(isBoxed)
            return "rdfbox";

        ResourceClass resourceClass = getExpressionResourceClass();

        if(resourceClass instanceof DateTimeConstantZoneClass)
            return "plain_datetime";

        if(resourceClass instanceof DateConstantZoneClass)
            return "plain_date";

        if(resourceClass instanceof LangStringConstantTagClass)
            return "plain_lang_string";

        if(resourceClass instanceof IriClass)
            return "iri";

        return resourceClass.getName();
    }


    protected static boolean isIri(ResourceClass resClass)
    {
        return resClass instanceof IriClass;
    }


    protected static boolean isDateTime(ResourceClass resClass)
    {
        return resClass == xsdDateTime || resClass instanceof DateTimeConstantZoneClass;
    }


    protected static boolean isDate(ResourceClass resClass)
    {
        return resClass == xsdDate || resClass instanceof DateConstantZoneClass;
    }


    protected static boolean isLangString(ResourceClass resClass)
    {
        return resClass == rdfLangString || resClass instanceof LangStringConstantTagClass;
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


    protected static Set<ResourceClass> asSet(ResourceClass... resourceClasses)
    {
        Set<ResourceClass> set = new HashSet<ResourceClass>();

        for(ResourceClass resourceClass : resourceClasses)
            set.add(resourceClass);

        return set;
    }
}
