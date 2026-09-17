package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import java.util.Set;



public class BuiltinClasses
{
    public static final RdfBoxClass box = new RdfBoxClass();

    public static final IriScalarClass iri = new IriScalarClass();
    public static final UnsupportedIriClass unsupportedIri = new UnsupportedIriClass();

    public static final IntBlankNodeScalarClass intScalarBlankNode = new IntBlankNodeScalarClass();
    public static final IntBlankNodeCompositeClass intBlankNode = new IntBlankNodeCompositeClass();
    public static final IntBlankNodeClass bnodeIntBlankNode = new IntBlankNodeInSegmentClass(Integer.MIN_VALUE);
    public static final StrBlankNodeScalarClass strScalarBlankNode = new StrBlankNodeScalarClass();
    public static final StrBlankNodeCompositeClass strBlankNode = new StrBlankNodeCompositeClass();
    public static final StrBlankNodeClass bnodeStrBlankNode = new StrBlankNodeInSegmentClass(Integer.MIN_VALUE);

    public static final BooleanBaseClass genBoolean = new BooleanBaseClass();
    public static final ShortBaseClass genShort = new ShortBaseClass();
    public static final IntBaseClass genInt = new IntBaseClass();
    public static final LongBaseClass genLong = new LongBaseClass();
    public static final IntegerBaseClass genInteger = new IntegerBaseClass();
    public static final DecimalBaseClass genDecimal = new DecimalBaseClass();
    public static final FloatBaseClass genFloat = new FloatBaseClass();
    public static final DoubleBaseClass genDouble = new DoubleBaseClass();
    public static final DateTimeScalarBaseClass genScalarDateTime = new DateTimeScalarBaseClass();
    public static final DateTimeCompositeBaseClass genDateTime = new DateTimeCompositeBaseClass();
    public static final DateScalarBaseClass genScalarDate = new DateScalarBaseClass();
    public static final DateCompositeBaseClass genDate = new DateCompositeBaseClass();
    public static final DayTimeDurationBaseClass genDayTimeDuration = new DayTimeDurationBaseClass();
    public static final UserLiteralCompositeBaseClass genUserType = new UserLiteralCompositeBaseClass();

    public static final BooleanClass xsdBoolean = new BooleanClass();
    public static final ShortClass xsdShort = new ShortClass();
    public static final IntClass xsdInt = new IntClass();
    public static final LongClass xsdLong = new LongClass();
    public static final IntegerClass xsdInteger = new IntegerClass();
    public static final DecimalClass xsdDecimal = new DecimalClass();
    public static final FloatClass xsdFloat = new FloatClass();
    public static final DoubleClass xsdDouble = new DoubleClass();
    public static final StringClass xsdString = new StringClass();
    public static final DateTimeScalarClass xsdScalarDateTime = new DateTimeScalarClass();
    public static final DateTimeCompositeClass xsdDateTime = new DateTimeCompositeClass();
    public static final DateScalarClass xsdScalarDate = new DateScalarClass();
    public static final DateCompositeClass xsdDate = new DateCompositeClass();
    public static final DayTimeDurationClass xsdDayTimeDuration = new DayTimeDurationClass();

    public static final LangStringClass rdfLangString = new LangStringClass();
    public static final UserLiteralCompositeClass userType = new UserLiteralCompositeClass();
    public static final UnsupportedLiteralClass unsupportedType = new UnsupportedLiteralClass();

    public static final Set<ResultResourceClass> resultClasses = Set.of(iri, intBlankNode, strBlankNode, xsdBoolean,
            genBoolean, xsdShort, genShort, xsdInt, genInt, xsdLong, genLong, xsdInteger, genInteger, xsdDecimal,
            genDecimal, xsdFloat, genFloat, xsdDouble, genDouble, xsdString, xsdDayTimeDuration, genDayTimeDuration,
            xsdDateTime, genDateTime, xsdDate, genDate, rdfLangString, unsupportedType);

    public static final ResourceClass lexBoolean = subtract(genBoolean, xsdBoolean);
    public static final ResourceClass lexShort = subtract(genShort, xsdShort);
    public static final ResourceClass lexInt = subtract(genInt, xsdInt);
    public static final ResourceClass lexLong = subtract(genLong, xsdLong);
    public static final ResourceClass lexInteger = subtract(genInteger, xsdInteger);
    public static final ResourceClass lexDecimal = subtract(genDecimal, xsdDecimal);
    public static final ResourceClass lexFloat = subtract(genFloat, xsdFloat);
    public static final ResourceClass lexDouble = subtract(genDouble, xsdDouble);
    public static final ResourceClass lexDateTime = subtract(genScalarDateTime, xsdScalarDateTime);
    public static final ResourceClass lexDate = subtract(genScalarDate, xsdScalarDate);
    public static final ResourceClass lexDayTimeDuration = subtract(genDayTimeDuration, xsdDayTimeDuration);

    public static final ResourceClass scalarBlankNode = unionize(intScalarBlankNode, strScalarBlankNode);
    public static final ResourceClass stringLiteral = unionize(xsdString, rdfLangString);
    public static final ResourceClass integerNumeric = unionize(genShort, genInt, genLong, genInteger);
    public static final ResourceClass floatPoint = unionize(genFloat, genDouble);
    public static final ResourceClass reference = unionize(iri, scalarBlankNode);
    public static final ResourceClass numeric = unionize(integerNumeric, genDecimal, genFloat, genDouble);
    public static final ResourceClass temporal = unionize(genScalarDate, genScalarDateTime, genDayTimeDuration);
    public static final ResourceClass dateOrDateTime = unionize(genScalarDateTime, genScalarDate);
    public static final ResourceClass literal = unionize(genBoolean, numeric, stringLiteral, temporal, unsupportedType);


    public static boolean isReference(ResourceClass resClass)
    {
        return resClass.isSubclassOf(reference);
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
        return resClass.isSubclassOf(genBoolean);
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
        return resClass.isSubclassOf(genShort);
    }


    public static boolean isInt(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genInt);
    }


    public static boolean isLong(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genLong);
    }


    public static boolean isInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genInteger);
    }


    public static boolean isDecimal(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDecimal);
    }


    public static boolean isFloatPoint(ResourceClass resClass)
    {
        return resClass.isSubclassOf(floatPoint);
    }


    public static boolean isFloat(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genFloat);
    }


    public static boolean isDouble(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDouble);
    }


    public static boolean isDateOrDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(dateOrDateTime);
    }


    public static boolean isDate(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genScalarDate);
    }


    public static boolean isDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genScalarDateTime);
    }


    public static boolean isDayTimeDuration(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDayTimeDuration);
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
        return resClass.isSubclassOf(unsupportedType);
    }


    public static boolean hasReference(ResourceClass resClass)
    {
        return !areDisjunct(resClass, reference);
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
        return !areDisjunct(resClass, genBoolean);
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
        return !areDisjunct(resClass, genShort);
    }


    public static boolean hasInt(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genInt);
    }


    public static boolean hasLong(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genLong);
    }


    public static boolean hasInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genInteger);
    }


    public static boolean hasDecimal(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDecimal);
    }


    public static boolean hasFloatPoint(ResourceClass resClass)
    {
        return !areDisjunct(resClass, floatPoint);
    }


    public static boolean hasFloat(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genFloat);
    }


    public static boolean hasDouble(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDouble);
    }


    public static boolean hasTemporalClass(ResourceClass resClass)
    {
        return !areDisjunct(resClass, temporal);
    }


    public static boolean hasDate(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genScalarDate);
    }


    public static boolean hasDateTime(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genScalarDateTime);
    }


    public static boolean hasDayTimeDuration(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDayTimeDuration);
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
