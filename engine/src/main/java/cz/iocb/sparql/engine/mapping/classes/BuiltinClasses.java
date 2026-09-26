package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import java.util.List;



/**
 * Singletons of the built-in resource classes and the derived classes grouping them. Naming: {@code xsd*} classes hold
 * canonical literals only, {@code gen*} classes hold any valid literal together with its lexical form, {@code lex*}
 * classes hold just the non-canonical ones; scalar and composite variants represent the same terms in different column
 * layouts. The {@code is*} predicates test whether a class is a subclass of a group, the {@code
 * has*} predicates whether it overlaps with it.
 */
public class BuiltinClasses
{
    /**
     * The universal box class.
     */
    public static final RdfBoxClass box = new RdfBoxClass();

    /**
     * Result class of IRIs (full text).
     */
    public static final IriScalarClass iri = new IriScalarClass();

    /**
     * IRIs outside every user IRI class.
     */
    public static final UnsupportedIriClass unsupportedIri = new UnsupportedIriClass();

    /**
     * Integer blank nodes packed in one column.
     */
    public static final IntBlankNodeScalarClass intScalarBlankNode = new IntBlankNodeScalarClass();

    /**
     * Result class of integer blank nodes (value and segment).
     */
    public static final IntBlankNodeCompositeClass intBlankNode = new IntBlankNodeCompositeClass();

    /**
     * Integer blank nodes of the reserved segment used for BNODE() results.
     */
    public static final IntBlankNodeClass bnodeIntBlankNode = new IntBlankNodeInSegmentClass(Integer.MIN_VALUE);

    /**
     * String blank nodes packed in one column.
     */
    public static final StrBlankNodeScalarClass strScalarBlankNode = new StrBlankNodeScalarClass();

    /**
     * Result class of string blank nodes (value and segment).
     */
    public static final StrBlankNodeCompositeClass strBlankNode = new StrBlankNodeCompositeClass();

    /**
     * String blank nodes of the reserved segment used for BNODE() results.
     */
    public static final StrBlankNodeClass bnodeStrBlankNode = new StrBlankNodeInSegmentClass(Integer.MIN_VALUE);

    /**
     * Any valid xsd:boolean literal with its lexical form.
     */
    public static final BooleanBaseClass genBoolean = new BooleanBaseClass();

    /**
     * Any valid xsd:byte literal with its lexical form.
     */
    public static final ByteBaseClass genByte = new ByteBaseClass();

    /**
     * Any valid xsd:unsignedByte literal with its lexical form.
     */
    public static final UnsignedByteBaseClass genUnsignedByte = new UnsignedByteBaseClass();

    /**
     * Any valid xsd:short literal with its lexical form.
     */
    public static final ShortBaseClass genShort = new ShortBaseClass();

    /**
     * Any valid xsd:unsignedShort literal with its lexical form.
     */
    public static final UnsignedShortBaseClass genUnsignedShort = new UnsignedShortBaseClass();

    /**
     * Any valid xsd:int literal with its lexical form.
     */
    public static final IntBaseClass genInt = new IntBaseClass();

    /**
     * Any valid xsd:unsignedInt literal with its lexical form.
     */
    public static final UnsignedIntBaseClass genUnsignedInt = new UnsignedIntBaseClass();

    /**
     * Any valid xsd:long literal with its lexical form.
     */
    public static final LongBaseClass genLong = new LongBaseClass();

    /**
     * Any valid xsd:unsignedLong literal with its lexical form.
     */
    public static final UnsignedLongBaseClass genUnsignedLong = new UnsignedLongBaseClass();

    /**
     * Any valid xsd:integer literal with its lexical form.
     */
    public static final IntegerBaseClass genInteger = new IntegerBaseClass();

    /**
     * Any valid xsd:nonPositiveInteger literal with its lexical form.
     */
    public static final NonPositiveIntegerBaseClass genNonPositiveInteger = new NonPositiveIntegerBaseClass();

    /**
     * Any valid xsd:negativeInteger literal with its lexical form.
     */
    public static final NegativeIntegerBaseClass genNegativeInteger = new NegativeIntegerBaseClass();

    /**
     * Any valid xsd:nonNegativeInteger literal with its lexical form.
     */
    public static final NonNegativeIntegerBaseClass genNonNegativeInteger = new NonNegativeIntegerBaseClass();

    /**
     * Any valid xsd:positiveInteger literal with its lexical form.
     */
    public static final PositiveIntegerBaseClass genPositiveInteger = new PositiveIntegerBaseClass();

    /**
     * Any valid xsd:decimal literal with its lexical form.
     */
    public static final DecimalBaseClass genDecimal = new DecimalBaseClass();

    /**
     * Any valid xsd:float literal with its lexical form.
     */
    public static final FloatBaseClass genFloat = new FloatBaseClass();

    /**
     * Any valid xsd:double literal with its lexical form.
     */
    public static final DoubleBaseClass genDouble = new DoubleBaseClass();

    /**
     * Any valid xsd:dateTime (scalar layout) literal with its lexical form.
     */
    public static final DateTimeScalarBaseClass genScalarDateTime = new DateTimeScalarBaseClass();

    /**
     * Any valid xsd:dateTime (composite layout) literal with its lexical form.
     */
    public static final DateTimeCompositeBaseClass genDateTime = new DateTimeCompositeBaseClass();

    /**
     * Any valid xsd:date (scalar layout) literal with its lexical form.
     */
    public static final DateScalarBaseClass genScalarDate = new DateScalarBaseClass();

    /**
     * Any valid xsd:date (composite layout) literal with its lexical form.
     */
    public static final DateCompositeBaseClass genDate = new DateCompositeBaseClass();

    /**
     * Any valid xsd:dayTimeDuration literal with its lexical form.
     */
    public static final DayTimeDurationBaseClass genDayTimeDuration = new DayTimeDurationBaseClass();

    /**
     * Any valid user datatype (composite layout) literal with its lexical form.
     */
    public static final UserLiteralCompositeBaseClass genUserType = new UserLiteralCompositeBaseClass();

    /**
     * Canonical xsd:boolean literals.
     */
    public static final BooleanClass xsdBoolean = new BooleanClass();

    /**
     * Canonical xsd:byte literals.
     */
    public static final ByteClass xsdByte = new ByteClass();

    /**
     * Canonical xsd:unsignedByte literals.
     */
    public static final UnsignedByteClass xsdUnsignedByte = new UnsignedByteClass();

    /**
     * Canonical xsd:short literals.
     */
    public static final ShortClass xsdShort = new ShortClass();

    /**
     * Canonical xsd:unsignedShort literals.
     */
    public static final UnsignedShortClass xsdUnsignedShort = new UnsignedShortClass();

    /**
     * Canonical xsd:int literals.
     */
    public static final IntClass xsdInt = new IntClass();

    /**
     * Canonical xsd:unsignedInt literals.
     */
    public static final UnsignedIntClass xsdUnsignedInt = new UnsignedIntClass();

    /**
     * Canonical xsd:long literals.
     */
    public static final LongClass xsdLong = new LongClass();

    /**
     * Canonical xsd:unsignedLong literals.
     */
    public static final UnsignedLongClass xsdUnsignedLong = new UnsignedLongClass();

    /**
     * Canonical xsd:integer literals.
     */
    public static final IntegerClass xsdInteger = new IntegerClass();

    /**
     * Canonical xsd:nonPositiveInteger literals.
     */
    public static final NonPositiveIntegerClass xsdNonPositiveInteger = new NonPositiveIntegerClass();

    /**
     * Canonical xsd:negativeInteger literals.
     */
    public static final NegativeIntegerClass xsdNegativeInteger = new NegativeIntegerClass();

    /**
     * Canonical xsd:nonNegativeInteger literals.
     */
    public static final NonNegativeIntegerClass xsdNonNegativeInteger = new NonNegativeIntegerClass();

    /**
     * Canonical xsd:positiveInteger literals.
     */
    public static final PositiveIntegerClass xsdPositiveInteger = new PositiveIntegerClass();

    /**
     * Canonical xsd:decimal literals.
     */
    public static final DecimalClass xsdDecimal = new DecimalClass();

    /**
     * Canonical xsd:float literals.
     */
    public static final FloatClass xsdFloat = new FloatClass();

    /**
     * Canonical xsd:double literals.
     */
    public static final DoubleClass xsdDouble = new DoubleClass();

    /**
     * Canonical xsd:dateTime (scalar layout) literals.
     */
    public static final DateTimeScalarClass xsdScalarDateTime = new DateTimeScalarClass();

    /**
     * Canonical xsd:dateTime (composite layout) literals.
     */
    public static final DateTimeCompositeClass xsdDateTime = new DateTimeCompositeClass();

    /**
     * Canonical xsd:date (scalar layout) literals.
     */
    public static final DateScalarClass xsdScalarDate = new DateScalarClass();

    /**
     * Canonical xsd:date (composite layout) literals.
     */
    public static final DateCompositeClass xsdDate = new DateCompositeClass();

    /**
     * Canonical xsd:dayTimeDuration literals.
     */
    public static final DayTimeDurationClass xsdDayTimeDuration = new DayTimeDurationClass();

    /**
     * Canonical xsd:string literals.
     */
    public static final StringClass xsdString = new StringClass();

    /**
     * Result class of language-tagged strings.
     */
    public static final LangStringClass rdfLangString = new LangStringClass();

    /**
     * Canonical literals of any user datatype.
     */
    public static final UserLiteralCompositeClass userType = new UserLiteralCompositeClass();

    /**
     * Literals of unknown datatypes or with invalid lexical forms.
     */
    public static final UnsupportedLiteralClass unsupportedType = new UnsupportedLiteralClass();

    /**
     * Valid but non-canonical xsd:boolean literals.
     */
    public static final ResourceClass lexBoolean = subtract(genBoolean, xsdBoolean);

    /**
     * Valid but non-canonical xsd:byte literals.
     */
    public static final ResourceClass lexByte = subtract(genByte, xsdByte);

    /**
     * Valid but non-canonical xsd:unsignedByte literals.
     */
    public static final ResourceClass lexUnsignedByte = subtract(genUnsignedByte, xsdUnsignedByte);

    /**
     * Valid but non-canonical xsd:short literals.
     */
    public static final ResourceClass lexShort = subtract(genShort, xsdShort);

    /**
     * Valid but non-canonical xsd:unsignedShort literals.
     */
    public static final ResourceClass lexUnsignedShort = subtract(genUnsignedShort, xsdUnsignedShort);

    /**
     * Valid but non-canonical xsd:int literals.
     */
    public static final ResourceClass lexInt = subtract(genInt, xsdInt);

    /**
     * Valid but non-canonical xsd:unsignedInt literals.
     */
    public static final ResourceClass lexUnsignedInt = subtract(genUnsignedInt, xsdUnsignedInt);

    /**
     * Valid but non-canonical xsd:long literals.
     */
    public static final ResourceClass lexLong = subtract(genLong, xsdLong);

    /**
     * Valid but non-canonical xsd:unsignedLong literals.
     */
    public static final ResourceClass lexUnsignedLong = subtract(genUnsignedLong, xsdUnsignedLong);

    /**
     * Valid but non-canonical xsd:integer literals.
     */
    public static final ResourceClass lexInteger = subtract(genInteger, xsdInteger);

    /**
     * Valid but non-canonical xsd:nonPositiveInteger literals.
     */
    public static final ResourceClass lexNonPositiveInteger = subtract(genNonPositiveInteger, xsdNonPositiveInteger);

    /**
     * Valid but non-canonical xsd:negativeInteger literals.
     */
    public static final ResourceClass lexNegativeInteger = subtract(genNegativeInteger, xsdNegativeInteger);

    /**
     * Valid but non-canonical xsd:nonNegativeInteger literals.
     */
    public static final ResourceClass lexNonNegativeInteger = subtract(genNonNegativeInteger, xsdNonNegativeInteger);

    /**
     * Valid but non-canonical xsd:positiveInteger literals.
     */
    public static final ResourceClass lexPositiveInteger = subtract(genPositiveInteger, xsdPositiveInteger);

    /**
     * Valid but non-canonical xsd:decimal literals.
     */
    public static final ResourceClass lexDecimal = subtract(genDecimal, xsdDecimal);

    /**
     * Valid but non-canonical xsd:float literals.
     */
    public static final ResourceClass lexFloat = subtract(genFloat, xsdFloat);

    /**
     * Valid but non-canonical xsd:double literals.
     */
    public static final ResourceClass lexDouble = subtract(genDouble, xsdDouble);

    /**
     * Valid but non-canonical xsd:dateTime (composite layout) literals.
     */
    public static final ResourceClass lexDateTime = subtract(genScalarDateTime, xsdScalarDateTime);

    /**
     * Valid but non-canonical xsd:date (composite layout) literals.
     */
    public static final ResourceClass lexDate = subtract(genScalarDate, xsdScalarDate);

    /**
     * Valid but non-canonical xsd:dayTimeDuration literals.
     */
    public static final ResourceClass lexDayTimeDuration = subtract(genDayTimeDuration, xsdDayTimeDuration);

    /**
     * Any blank node.
     */
    public static final ResourceClass scalarBlankNode = unionize(intScalarBlankNode, strScalarBlankNode);

    /**
     * xsd:string or rdf:langString.
     */
    public static final ResourceClass stringLiteral = unionize(xsdString, rdfLangString);

    /**
     * Numerics fitting in an SQL smallint.
     */
    public static final ResourceClass shortNumeric = unionize(genByte, genUnsignedByte, genShort);

    /**
     * Numerics fitting in an SQL integer.
     */
    public static final ResourceClass intNumeric = unionize(shortNumeric, genUnsignedShort, genInt);

    /**
     * Numerics fitting in an SQL bigint.
     */
    public static final ResourceClass longNumeric = unionize(intNumeric, genUnsignedInt, genLong);

    /**
     * All integer-derived numerics.
     */
    public static final ResourceClass integerNumeric = unionize(longNumeric, genUnsignedLong, genInteger,
            genNonPositiveInteger, genNegativeInteger, genNonNegativeInteger, genPositiveInteger);

    /**
     * xsd:float or xsd:double.
     */
    public static final ResourceClass floatPoint = unionize(genFloat, genDouble);

    /**
     * IRI or blank node.
     */
    public static final ResourceClass reference = unionize(iri, scalarBlankNode);

    /**
     * Any numeric literal.
     */
    public static final ResourceClass numeric = unionize(integerNumeric, genDecimal, genFloat, genDouble);

    /**
     * Date, date-time or duration.
     */
    public static final ResourceClass temporal = unionize(genScalarDateTime, genScalarDate, genDayTimeDuration);

    /**
     * xsd:date or xsd:dateTime.
     */
    public static final ResourceClass dateOrDateTime = unionize(genScalarDateTime, genScalarDate);

    /**
     * Any literal.
     */
    public static final ResourceClass literal = unionize(genBoolean, numeric, stringLiteral, temporal, unsupportedType);

    /* numeric base classes, each listed before the classes to which its values are promoted */

    /**
     * Numeric base classes ordered so that each class precedes the classes its values are promoted to.
     */
    public static final List<ResourceClass> numericBaseClasses = List.of(genByte, genUnsignedByte, genShort,
            genUnsignedShort, genInt, genUnsignedInt, genLong, genUnsignedLong, genInteger, genNonPositiveInteger,
            genNegativeInteger, genNonNegativeInteger, genPositiveInteger, genDecimal, genFloat, genDouble);


    /**
     * Not instantiable.
     */
    private BuiltinClasses()
    {
    }


    /**
     * True if every value of the class is an IRI or a blank node.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an IRI or a blank node, false otherwise
     */
    public static boolean isReference(ResourceClass resClass)
    {
        return resClass.isSubclassOf(reference);
    }


    /**
     * True if every value of the class is an IRI.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an IRI, false otherwise
     */
    public static boolean isIri(ResourceClass resClass)
    {
        return resClass.isSubclassOf(iri);
    }


    /**
     * True if every value of the class is a blank node.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a blank node, false otherwise
     */
    public static boolean isBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(scalarBlankNode);
    }


    /**
     * True if every value of the class is an integer blank node.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an integer blank node, false otherwise
     */
    public static boolean isIntBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(intScalarBlankNode);
    }


    /**
     * True if every value of the class is a string blank node.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a string blank node, false otherwise
     */
    public static boolean isStrBlankNode(ResourceClass resClass)
    {
        return resClass.isSubclassOf(strScalarBlankNode);
    }


    /**
     * True if every value of the class is a literal.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a literal, false otherwise
     */
    public static boolean isLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(literal);
    }


    /**
     * True if every value of the class is a xsd:boolean.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isBoolean(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genBoolean);
    }


    /**
     * True if every value of the class is numeric.
     *
     * @param resClass the resource class
     * @return true if every value of the class is numeric, false otherwise
     */
    public static boolean isNumeric(ResourceClass resClass)
    {
        return resClass.isSubclassOf(numeric);
    }


    /**
     * True if every value of the class is an integer-derived numeric.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an integer-derived numeric, false otherwise
     */
    public static boolean isDerivatedFromInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(integerNumeric);
    }


    /**
     * True if every value of the class is a numeric representable as smallint.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a numeric representable as smallint, false otherwise
     */
    public static boolean isRepresentableAsShort(ResourceClass resClass)
    {
        return resClass.isSubclassOf(shortNumeric);
    }


    /**
     * True if every value of the class is a numeric representable as integer.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a numeric representable as integer, false otherwise
     */
    public static boolean isRepresentableAsInt(ResourceClass resClass)
    {
        return resClass.isSubclassOf(intNumeric);
    }


    /**
     * True if every value of the class is a numeric representable as bigint.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a numeric representable as bigint, false otherwise
     */
    public static boolean isRepresentableAsLong(ResourceClass resClass)
    {
        return resClass.isSubclassOf(longNumeric);
    }


    /**
     * True if every value of the class is a xsd:byte.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isByte(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genByte);
    }


    /**
     * True if every value of the class is an xsd:unsignedByte.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isUnsignedByte(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genUnsignedByte);
    }


    /**
     * True if every value of the class is a xsd:short.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isShort(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genShort);
    }


    /**
     * True if every value of the class is an xsd:unsignedShort.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isUnsignedShort(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genUnsignedShort);
    }


    /**
     * True if every value of the class is an xsd:int.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isInt(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genInt);
    }


    /**
     * True if every value of the class is an xsd:unsignedInt.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isUnsignedInt(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genUnsignedInt);
    }


    /**
     * True if every value of the class is a xsd:long.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isLong(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genLong);
    }


    /**
     * True if every value of the class is an xsd:unsignedLong.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isUnsignedLong(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genUnsignedLong);
    }


    /**
     * True if every value of the class is an xsd:integer.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genInteger);
    }


    /**
     * True if every value of the class is a xsd:nonPositiveInteger.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isNonPositiveInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genNonPositiveInteger);
    }


    /**
     * True if every value of the class is a xsd:negativeInteger.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isNegativeInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genNegativeInteger);
    }


    /**
     * True if every value of the class is a xsd:nonNegativeInteger.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isNonNegativeInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genNonNegativeInteger);
    }


    /**
     * True if every value of the class is a xsd:positiveInteger.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isPositiveInteger(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genPositiveInteger);
    }


    /**
     * True if every value of the class is a xsd:decimal.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isDecimal(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDecimal);
    }


    /**
     * True if every value of the class is a floating-point numeric.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a floating-point numeric, false otherwise
     */
    public static boolean isFloatPoint(ResourceClass resClass)
    {
        return resClass.isSubclassOf(floatPoint);
    }


    /**
     * True if every value of the class is a xsd:float.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isFloat(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genFloat);
    }


    /**
     * True if every value of the class is a xsd:double.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isDouble(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDouble);
    }


    /**
     * True if every value of the class is a date or a date-time.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a date or a date-time, false otherwise
     */
    public static boolean isDateOrDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(dateOrDateTime);
    }


    /**
     * True if every value of the class is a xsd:dateTime (scalar layout).
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isDateTime(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genScalarDateTime);
    }


    /**
     * True if every value of the class is a xsd:date (scalar layout).
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isDate(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genScalarDate);
    }


    /**
     * True if every value of the class is a xsd:dayTimeDuration.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a xsd, false otherwise
     */
    public static boolean isDayTimeDuration(ResourceClass resClass)
    {
        return resClass.isSubclassOf(genDayTimeDuration);
    }


    /**
     * True if every value of the class is a string literal.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a string literal, false otherwise
     */
    public static boolean isStringLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(stringLiteral);
    }


    /**
     * True if every value of the class is an xsd:string.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an xsd, false otherwise
     */
    public static boolean isString(ResourceClass resClass)
    {
        return resClass.isSubclassOf(xsdString);
    }


    /**
     * True if every value of the class is a language-tagged string.
     *
     * @param resClass the resource class
     * @return true if every value of the class is a language-tagged string, false otherwise
     */
    public static boolean isLangString(ResourceClass resClass)
    {
        return resClass.isSubclassOf(rdfLangString);
    }


    /**
     * True if every value of the class is an unsupported literal.
     *
     * @param resClass the resource class
     * @return true if every value of the class is an unsupported literal, false otherwise
     */
    public static boolean isUnsupportedLiteral(ResourceClass resClass)
    {
        return resClass.isSubclassOf(unsupportedType);
    }


    /**
     * True if some value of the class may be an IRI or a blank node.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an IRI or a blank node, false otherwise
     */
    public static boolean hasReference(ResourceClass resClass)
    {
        return !areDisjunct(resClass, reference);
    }


    /**
     * True if some value of the class may be an IRI.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an IRI, false otherwise
     */
    public static boolean hasIri(ResourceClass resClass)
    {
        return !areDisjunct(resClass, iri);
    }


    /**
     * True if some value of the class may be a blank node.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a blank node, false otherwise
     */
    public static boolean hasBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, scalarBlankNode);
    }


    /**
     * True if some value of the class may be an integer blank node.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an integer blank node, false otherwise
     */
    public static boolean hasIntBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, intScalarBlankNode);
    }


    /**
     * True if some value of the class may be a string blank node.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a string blank node, false otherwise
     */
    public static boolean hasStrBlankNode(ResourceClass resClass)
    {
        return !areDisjunct(resClass, strScalarBlankNode);
    }


    /**
     * True if some value of the class may be a literal.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a literal, false otherwise
     */
    public static boolean hasLiteral(ResourceClass resClass)
    {
        return !areDisjunct(resClass, literal);
    }


    /**
     * True if some value of the class may be a xsd:boolean.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasBoolean(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genBoolean);
    }


    /**
     * True if some value of the class may be numeric.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be numeric, false otherwise
     */
    public static boolean hasNumeric(ResourceClass resClass)
    {
        return !areDisjunct(resClass, numeric);
    }


    /**
     * True if some value of the class may be an integer-derived numeric.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an integer-derived numeric, false otherwise
     */
    public static boolean hasDerivatedFromInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, integerNumeric);
    }


    /**
     * True if some value of the class may be a xsd:byte.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasByte(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genByte);
    }


    /**
     * True if some value of the class may be an xsd:unsignedByte.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasUnsignedByte(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genUnsignedByte);
    }


    /**
     * True if some value of the class may be a xsd:short.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasShort(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genShort);
    }


    /**
     * True if some value of the class may be an xsd:unsignedShort.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasUnsignedShort(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genUnsignedShort);
    }


    /**
     * True if some value of the class may be an xsd:int.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasInt(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genInt);
    }


    /**
     * True if some value of the class may be an xsd:unsignedInt.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasUnsignedInt(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genUnsignedInt);
    }


    /**
     * True if some value of the class may be a xsd:long.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasLong(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genLong);
    }


    /**
     * True if some value of the class may be an xsd:unsignedLong.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasUnsignedLong(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genUnsignedLong);
    }


    /**
     * True if some value of the class may be an xsd:integer.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genInteger);
    }


    /**
     * True if some value of the class may be a xsd:nonPositiveInteger.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasNonPositiveInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genNonPositiveInteger);
    }


    /**
     * True if some value of the class may be a xsd:negativeInteger.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasNegativeInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genNegativeInteger);
    }


    /**
     * True if some value of the class may be a xsd:nonNegativeInteger.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasNonNegativeInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genNonNegativeInteger);
    }


    /**
     * True if some value of the class may be a xsd:positiveInteger.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasPositiveInteger(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genPositiveInteger);
    }


    /**
     * True if some value of the class may be a xsd:decimal.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasDecimal(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDecimal);
    }


    /**
     * True if some value of the class may be a floating-point numeric.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a floating-point numeric, false otherwise
     */
    public static boolean hasFloatPoint(ResourceClass resClass)
    {
        return !areDisjunct(resClass, floatPoint);
    }


    /**
     * True if some value of the class may be a xsd:float.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasFloat(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genFloat);
    }


    /**
     * True if some value of the class may be a xsd:double.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasDouble(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDouble);
    }


    /**
     * True if some value of the class may be a temporal value.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a temporal value, false otherwise
     */
    public static boolean hasTemporalClass(ResourceClass resClass)
    {
        return !areDisjunct(resClass, temporal);
    }


    /**
     * True if some value of the class may be a xsd:dateTime (scalar layout).
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasDateTime(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genScalarDateTime);
    }


    /**
     * True if some value of the class may be a xsd:date (scalar layout).
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasDate(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genScalarDate);
    }


    /**
     * True if some value of the class may be a xsd:dayTimeDuration.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a xsd, false otherwise
     */
    public static boolean hasDayTimeDuration(ResourceClass resClass)
    {
        return !areDisjunct(resClass, genDayTimeDuration);
    }


    /**
     * True if some value of the class may be a string literal.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a string literal, false otherwise
     */
    public static boolean hasStringLiteral(ResourceClass resClass)
    {
        return !areDisjunct(resClass, stringLiteral);
    }


    /**
     * True if some value of the class may be an xsd:string.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be an xsd, false otherwise
     */
    public static boolean hasString(ResourceClass resClass)
    {
        return !areDisjunct(resClass, xsdString);
    }


    /**
     * True if some value of the class may be a language-tagged string.
     *
     * @param resClass the resource class
     * @return true if some value of the class may be a language-tagged string, false otherwise
     */
    public static boolean hasLangString(ResourceClass resClass)
    {
        return !areDisjunct(resClass, rdfLangString);
    }
}
