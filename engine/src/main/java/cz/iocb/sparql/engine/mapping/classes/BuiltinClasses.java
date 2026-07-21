package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
import static java.util.stream.Collectors.toCollection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;



public class BuiltinClasses
{
    /* built-in root of class hierarchy */
    public static final RdfBoxClass box = new RdfBoxClass();

    /*built-in additional expression classes */
    public static final CommonIntBlankNodeScalarClass intScalarBlankNode = new CommonIntBlankNodeScalarClass();
    public static final CommonStrBlankNodeScalarClass strScalarBlankNode = new CommonStrBlankNodeScalarClass();
    public static final DateTimeScalarClass xsdScalarDateTime = new DateTimeScalarClass();
    public static final DateScalarClass xsdScalarDate = new DateScalarClass();

    /* built-in result classes */
    public static final CommonIriClass iri = new CommonIriClass();
    public static final CommonIntBlankNodeCompositeClass intCompositeBlankNode = new CommonIntBlankNodeCompositeClass();
    public static final CommonStrBlankNodeCompositeClass strCompositeBlankNode = new CommonStrBlankNodeCompositeClass();
    public static final BooleanClass xsdBoolean = new BooleanClass();
    public static final ShortClass xsdShort = new ShortClass();
    public static final IntClass xsdInt = new IntClass();
    public static final LongClass xsdLong = new LongClass();
    public static final IntegerClass xsdInteger = new IntegerClass();
    public static final DecimalClass xsdDecimal = new DecimalClass();
    public static final FloatClass xsdFloat = new FloatClass();
    public static final DoubleClass xsdDouble = new DoubleClass();
    public static final StringClass xsdString = new StringClass();
    public static final DayTimeDurationClass xsdDayTimeDuration = new DayTimeDurationClass();
    public static final DateTimeCompositeClass xsdCompositeDateTime = new DateTimeCompositeClass();
    public static final DateCompositeClass xsdCompositeDate = new DateCompositeClass();
    public static final LangStringClass rdfLangString = new LangStringClass();
    public static final UnsupportedLiteralClass unsupportedLiteral = new UnsupportedLiteralClass();

    /* built-in special classes */
    public static final UnsupportedIriClass unsupportedIri = new UnsupportedIriClass();
    public static final UserIntBlankNodeClass bnodeIntBlankNode = new UserIntBlankNodeClass();
    public static final UserStrBlankNodeClass bnodeStrBlankNode = new UserStrBlankNodeClass();

    /* built-in union classes */
    public static final ResourceClass scalarBlankNode = getUnionClass(intScalarBlankNode, strScalarBlankNode);

    public static final ResourceClass stringLiteral = getUnionClass(xsdString, rdfLangString);

    public static final ResourceClass integerNumeric = getUnionClass(xsdShort, xsdInt, xsdLong, xsdInteger);

    public static final ResourceClass floatPoint = getUnionClass(xsdFloat, xsdDouble);


    public static final ResourceClass numeric = getUnionClass(integerNumeric, xsdDecimal, xsdFloat, xsdDouble);

    public static final ResourceClass temporal = getUnionClass(xsdScalarDate, xsdScalarDateTime, xsdDayTimeDuration);

    public static final ResourceClass dateOrDateTime = getUnionClass(xsdScalarDateTime, xsdScalarDate);

    public static final ResourceClass literal = getUnionClass(xsdBoolean, numeric, stringLiteral, xsdDayTimeDuration,
            xsdScalarDateTime, xsdScalarDate, unsupportedLiteral);

    public static final Set<ResultResourceClass> resultClasses = Set.of(iri, intCompositeBlankNode,
            strCompositeBlankNode, xsdBoolean, xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble,
            xsdString, xsdDayTimeDuration, xsdCompositeDateTime, xsdCompositeDate, rdfLangString, unsupportedLiteral);



    public static Set<ResourceClass> getNumericClasses(ResourceClass resClass)
    {
        return Stream.of(xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble)
                .filter(r -> !areDisjunct(r, resClass)).collect(toCollection(HashSet::new));
    }


    public static ResourceClass getBaseNumericClass(ResourceClass source)
    {
        return Stream.of(xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble, box)
                .filter(r -> source.isSubclassOf(r)).findFirst().orElseThrow(IllegalArgumentException::new);
    }


    public static ResourceClass getBaseExpressionClass(ResourceClass source)
    {
        return Stream
                .of(xsdBoolean, xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal, xsdFloat, xsdDouble, xsdString,
                        xsdScalarDateTime, xsdScalarDate, xsdDayTimeDuration, unsupportedLiteral, iri,
                        intScalarBlankNode, strScalarBlankNode, box)
                .filter(r -> source.isSubclassOf(r)).findFirst().orElseThrow(IllegalArgumentException::new);
    }


    public static boolean isIri(ResourceClass resClass)
    {
        return resClass.isSubclassOf(iri);
    }


    public static boolean isBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(scalarBlankNode);
    }


    public static boolean isIntBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(intScalarBlankNode);
    }


    public static boolean isStrBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(strScalarBlankNode);
    }


    public static boolean isLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(literal);
    }


    public static boolean isBoolean(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdBoolean);
    }


    public static boolean isNumeric(ResourceClass resClass)
    {
        return resClass.isSubclassOf(numeric);
    }


    public static boolean isDerivatedFromInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(integerNumeric);
    }


    public static boolean isShort(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdShort);
    }


    public static boolean isInt(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdInt);
    }


    public static boolean isLong(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdLong);
    }


    public static boolean isInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdInteger);
    }


    public static boolean isDecimal(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdDecimal);
    }


    public static boolean isFloatPoint(ResourceClass resClass)
    {
        return resClass.isSubclassOf(floatPoint);
    }


    public static boolean isFloat(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdFloat);
    }


    public static boolean isDouble(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdDouble);
    }


    public static boolean isDateOrDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(dateOrDateTime);
    }


    public static boolean isDate(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdScalarDate);
    }


    public static boolean isDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdScalarDateTime);
    }


    public static boolean isDayTimeDuration(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdDayTimeDuration);
    }


    public static boolean isStringLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(stringLiteral);
    }


    public static boolean isString(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdString);
    }


    public static boolean isLangString(ResourceClass resClass)
    {
        return resClass.isSubclassOf(rdfLangString);
    }


    public static boolean isUnsupportedLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(unsupportedLiteral);
    }


    public static boolean hasIri(ResourceClass resClass)
    {
        return !areDisjunct(resClass, iri);
    }


    public static boolean hasBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, scalarBlankNode);
    }


    public static boolean hasIntBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, intScalarBlankNode);
    }


    public static boolean hasStrBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, strScalarBlankNode);
    }


    public static boolean hasLiteral(ResourceClass resClass)
    {
        return !areDisjunct(resClass, literal);
    }


    public static boolean hasBoolean(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdBoolean);
    }


    public static boolean hasNumeric(ResourceClass resClass)
    {
        return !areDisjunct(resClass, numeric);
    }


    public static boolean hasDerivatedFromInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, integerNumeric);
    }


    public static boolean hasShort(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdShort);
    }


    public static boolean hasInt(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdInt);
    }


    public static boolean hasLong(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdLong);
    }


    public static boolean hasInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdInteger);
    }


    public static boolean hasDecimal(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdDecimal);
    }


    public static boolean hasFloatPoint(ResourceClass resClass)
    {
        return !areDisjunct(resClass, floatPoint);
    }


    public static boolean hasFloat(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdFloat);
    }


    public static boolean hasDouble(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdDouble);
    }


    public static boolean hasTemporalClass(ResourceClass resClass)
    {
        return !areDisjunct(resClass, temporal);
    }


    public static boolean hasDate(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdScalarDate);
    }


    public static boolean hasDateTime(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdScalarDateTime);
    }


    public static boolean hasDayTimeDuration(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdDayTimeDuration);
    }


    public static boolean hasLangString(ResourceClass resClass)
    {
        return !areDisjunct(resClass, rdfLangString);
    }


    public static boolean hasString(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdString);
    }


    public static boolean hasStringLiteral(ResourceClass resClass)
    {
        return !areDisjunct(resClass, stringLiteral);
    }
}
