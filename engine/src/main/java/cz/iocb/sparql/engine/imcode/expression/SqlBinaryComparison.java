package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.GREATER_THAN;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.GREATER_THAN_OR_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.LESS_THAN;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.NOT_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.TRUE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivatedFromInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isRepresentableAsInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isRepresentableAsLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isRepresentableAsShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.estimateAsUnion;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getExpressionClass;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Comparison of two terms following the SPARQL operator mapping: numeric, boolean, string, date and date-time
 * comparison and RDF term equality; comparing incompatible types is an error. Dates and date-times of different
 * timezones are compared through their common representation.
 */
public final class SqlBinaryComparison extends SqlBinary implements SqlBooleanExpression
{
    /**
     * Comparison operator with its SPARQL spelling and the suffix of its {@code sparql.*} SQL function.
     */
    public static enum ComparisonOperator
    {
        /**
         * Equality.
         */
        EQUAL("=", "equal"),

        /**
         * Inequality.
         */
        NOT_EQUAL("!=", "not_equal"),

        /**
         * Less than.
         */
        LESS_THAN("<", "less_than"),

        /**
         * Greater than.
         */
        GREATER_THAN(">", "greater_than"),

        /**
         * Less than or equal.
         */
        LESS_THAN_OR_EQUAL("<=", "not_greater_than"),

        /**
         * Greater than or equal.
         */
        GREATER_THAN_OR_EQUAL(">=", "not_less_than");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * Suffix of the SQL function.
         */
        private final String code;

        /**
         * Creates the operator.
         *
         * @param text the text
         * @param code the SQL spelling
         */
        ComparisonOperator(String text, String code)
        {
            this.text = text;
            this.code = code;
        }


        /**
         * SPARQL spelling of the operator.
         *
         * @return SPARQL spelling of the operator
         */
        public String getText()
        {
            return text;
        }


        /**
         * Suffix of the {@code sparql.*} SQL function implementing the operator.
         *
         * @return suffix of the {@code sparql.*} SQL function implementing the operator
         */
        public String getName()
        {
            return code;
        }
    }


    /**
     * Possible outcome of comparing values of two classes: always different, always an error, never an error, or
     * anything.
     */
    protected static enum ComparisonType
    {
        /**
         * The values are never equal.
         */
        DIFFERENT,

        /**
         * The comparison is always an error.
         */
        NULL,

        /**
         * The comparison never fails.
         */
        NOT_NULL,

        /**
         * Anything can happen.
         */
        FULL
    }


    /**
     * How the SQL of a comparison is generated for a pair of classes.
     */
    private static enum ComparisonMode
    {
        /**
         * Always an error.
         */
        NULL,

        /**
         * Always different (equality operators on disjoint terms).
         */
        DIFF,

        /**
         * Boolean comparison.
         */
        BOOLEAN,

        /**
         * Floating-point comparison.
         */
        FLOAT,

        /**
         * Exact numeric comparison.
         */
        DECIMAL,

        /**
         * Date-time comparison.
         */
        DATETIME,

        /**
         * Dates with the left zone less than a day earlier than the right one.
         */
        DATE_EARLIER_TZ,

        /**
         * Dates with equal zones.
         */
        DATE_SAME_TZ,

        /**
         * Dates with the left zone less than a day later than the right one.
         */
        DATE_LATER_TZ,

        /**
         * General date comparison.
         */
        DATE,

        /**
         * Comparison of arbitrary literals through the box.
         */
        LITERAL,

        /**
         * Direct SQL comparison of the columns (strings, references).
         */
        DIRECT,

        /**
         * Comparison through the box.
         */
        BOX
    }


    /**
     * Seconds in a day.
     */
    private static final int SECS_PER_DAY = 24 * 60 * 60;

    /**
     * The operator.
     */
    private final ComparisonOperator operator;

    /**
     * Values the comparison can take besides an error.
     */
    private final NonConstantBooleanValue value;


    /**
     * Creates the expression.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param value values the expression can take besides an error
     */
    private SqlBinaryComparison(ComparisonOperator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull, NonConstantBooleanValue value)
    {
        super(left, right, mappings, canBeNull);

        this.operator = operator;
        this.value = value;
    }


    /**
     * Comparison of the operands.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @return comparison of the operands
     */
    public static SqlExpressionIntercode create(ComparisonOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return create(operator, left, right, Restriction.ALL);
    }


    /**
     * Comparison materialising only the needed result; constant when the classes decide it.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param restriction the result classes the parent needs
     * @return comparison materialising only the needed result; constant when the classes decide it
     */
    private static SqlExpressionIntercode create(ComparisonOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right, Restriction restriction)
    {
        if(left instanceof SqlIri a && right instanceof SqlIri b)
        {
            if(operator == EQUAL)
                return a.getIri().equals(b.getIri()) ? trueValue : falseValue;
            else if(operator == NOT_EQUAL)
                return a.getIri().equals(b.getIri()) ? falseValue : trueValue;
        }

        //TODO: add compile-time evaluation for literals

        Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();
        Set<List<ResourceClass>> set = new HashSet<>();
        map.put(xsdBoolean, set);

        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(ResourceClass leftClass : left.getMappings().keySet())
        {
            for(ResourceClass rightClass : right.getMappings().keySet())
            {
                switch(areComparable(operator, leftClass, rightClass))
                {
                    case DIFFERENT ->
                    {
                        set.add(List.of(leftClass, rightClass));
                    }

                    case NULL ->
                    {
                        canBeNull = true;
                    }

                    case NOT_NULL ->
                    {
                        isAlwaysDifferentIfNotNull = false;
                        set.add(List.of(leftClass, rightClass));
                    }

                    case FULL ->
                    {
                        canBeNull = true;
                        isAlwaysDifferentIfNotNull = false;
                        set.add(List.of(leftClass, rightClass));
                    }
                }
            }
        }


        if(set.isEmpty())
            return SqlNull.get();

        if(operator == EQUAL && !canBeNull && isAlwaysDifferentIfNotNull)
            return falseValue;

        if(operator == NOT_EQUAL && !canBeNull && isAlwaysDifferentIfNotNull)
            return trueValue;


        List<SqlExpressionIntercode> operands = List.of(left, right);
        Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(operands, map, restriction);

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
            mappings.put(e.getKey(), e.getValue() == null ? null : translate(operator, e.getValue(), left, right));

        NonConstantBooleanValue value = switch(operator)
        {
            case EQUAL -> isAlwaysDifferentIfNotNull ? FALSE_OR_ERROR : ANY;
            case NOT_EQUAL -> isAlwaysDifferentIfNotNull ? TRUE_OR_ERROR : ANY;
            default -> ANY;
        };

        return new SqlBinaryComparison(operator, left, right, mappings, canBeNull, value);
    }


    /**
     * Outcome of comparing values of the two classes with the operator, decided from the classes alone.
     *
     * @param operator the operator
     * @param l class on the left side
     * @param r class on the right side
     * @return outcome of comparing values of the two classes with the operator, decided from the classes alone
     */
    protected static ComparisonType areComparable(ComparisonOperator operator, ResourceClass l, ResourceClass r)
    {
        boolean equalityComparison = (operator == EQUAL || operator == NOT_EQUAL);

        if(equalityComparison && l instanceof DateInZoneClass ld && r instanceof DateInZoneClass rd
                && getTimezoneDiff(ld, rd) % SECS_PER_DAY != 0)
            return ComparisonType.DIFFERENT;

        if(isNumeric(l) && isNumeric(r))
            return ComparisonType.NOT_NULL;

        if(isString(l) && isString(r))
            return (equalityComparison && areDisjunct(l, r)) ? ComparisonType.DIFFERENT : ComparisonType.NOT_NULL;

        if(isBoolean(l) && isBoolean(r))
            return ComparisonType.NOT_NULL;

        if(isDateTime(l) && isDateTime(r))
            return ComparisonType.NOT_NULL;

        if(isDate(l) && isDate(r))
            return ComparisonType.NOT_NULL;

        if(hasNumeric(l) && hasNumeric(r))
            return ComparisonType.FULL;

        if(hasString(l) && hasString(r))
            return ComparisonType.FULL;

        if(hasBoolean(l) && hasBoolean(r))
            return ComparisonType.FULL;

        if(hasDateTime(l) && hasDateTime(r))
            return ComparisonType.FULL;

        if(hasDate(l) && hasDate(r))
            return ComparisonType.FULL;

        if(!equalityComparison)
            return ComparisonType.NULL;

        if(isLiteral(l) && isLiteral(r) && areDisjunct(l, r))
            return ComparisonType.NULL;

        if(hasLiteral(l) && hasLiteral(r))
            return ComparisonType.FULL;

        if(areDisjunct(l, r))
            return ComparisonType.DIFFERENT;

        return ComparisonType.NOT_NULL;
    }


    /**
     * Comparison mode covering all pairs of operand classes.
     *
     * @param operator the operator
     * @param left classes of the left operand
     * @param right classes of the right operand
     * @return comparison mode covering all pairs of operand classes
     */
    private static ComparisonMode getComparisonMode(ComparisonOperator operator, Set<ResourceClass> left,
            Set<ResourceClass> right)
    {
        ComparisonMode result = null;

        for(ResourceClass l : estimateAsUnion(left))
            for(ResourceClass r : estimateAsUnion(right))
                result = mergeComparisonTypes(result, determineComparisonMode(operator, l, r));

        return result;
    }


    /**
     * Comparison mode of a pair of classes according to the SPARQL operator mapping; equality of dates in zones
     * differing by less than a day is handled specially.
     *
     * @param operator the operator
     * @param left class of the left operand
     * @param right class of the right operand
     * @return comparison mode of a pair of classes according to the SPARQL operator mapping; equality of dates in zones
     *         differing by less than a day is handled specially
     */
    private static ComparisonMode determineComparisonMode(ComparisonOperator operator, ResourceClass left,
            ResourceClass right)
    {
        // special treatment for dates with constant timezones

        if(left instanceof DateInZone l && right instanceof DateInZone r)
        {
            int diff = getTimezoneDiff(l, r);

            if(diff == 0)
                return ComparisonMode.DATE_SAME_TZ;
            else if(operator == EQUAL || operator == NOT_EQUAL)
                return ComparisonMode.DIFF;
            else if(diff < 0 && diff > -SECS_PER_DAY)
                return ComparisonMode.DATE_EARLIER_TZ;
            else if(diff > 0 && diff < SECS_PER_DAY)
                return ComparisonMode.DATE_LATER_TZ;
        }


        if(isNumeric(left) && isNumeric(right))
            return isFloatPoint(left) || isFloatPoint(right) ? ComparisonMode.FLOAT : ComparisonMode.DECIMAL;
        else if(isDateTime(left) && isDateTime(right))
            return ComparisonMode.DATETIME;
        else if(isDate(left) && isDate(right))
            return ComparisonMode.DATE;
        else if(isBoolean(left) && isBoolean(right))
            return ComparisonMode.BOOLEAN;
        else if(isString(left) && isString(right))
            return ComparisonMode.DIRECT;
        else if(operator != EQUAL && operator != NOT_EQUAL)
            return ComparisonMode.NULL;
        else if(hasNumeric(left) && hasNumeric(right))
            return ComparisonMode.BOX;
        else if(hasDateTime(left) && hasDateTime(right))
            return ComparisonMode.BOX;
        else if(hasDate(left) && hasDate(right))
            return ComparisonMode.BOX;
        else if(hasBoolean(left) && hasBoolean(right))
            return ComparisonMode.BOX;
        else if(hasString(left) && hasString(right))
            return ComparisonMode.BOX;
        else if(isLiteral(left) && isLiteral(right))
            return ResourceClass.areDisjunct(left, right) ? ComparisonMode.NULL : ComparisonMode.LITERAL;
        else if(!hasLiteral(left) && !hasLiteral(right))
            return ResourceClass.areDisjunct(left, right) ? ComparisonMode.DIFF : ComparisonMode.DIRECT;
        else
            return ComparisonMode.BOX;
    }


    /**
     * True for the date modes.
     *
     * @param mode the comparison mode
     * @return true for the date modes, false otherwise
     */
    private static boolean isDateMode(ComparisonMode mode)
    {
        return mode == ComparisonMode.DATE || mode == ComparisonMode.DATE_EARLIER_TZ
                || mode == ComparisonMode.DATE_SAME_TZ || mode == ComparisonMode.DATE_LATER_TZ;
    }


    /**
     * Combines the modes of two class pairs: equal modes stay, date modes merge to the general date mode, anything else
     * falls back to the box.
     *
     * @param a one mode
     * @param b the other mode
     * @return the comparison mode
     */
    private static ComparisonMode mergeComparisonTypes(ComparisonMode a, ComparisonMode b)
    {
        if(a == null)
            return b;

        if(b == null)
            return a;

        if(a == b)
            return a;

        if(isDateMode(a) && isDateMode(b))
            return ComparisonMode.DATE;

        return ComparisonMode.BOX;
    }


    /**
     * SQL of the comparison as a COALESCE over the argument class combinations.
     *
     * @param operator the operator
     * @param variants combinations of argument classes
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison as a COALESCE over the argument class combinations
     */
    private static List<Column> translate(ComparisonOperator operator, Set<List<Set<ResourceClass>>> variants,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        if(variants == null)
            return null;

        return List.of(Column.coalesce(
                variants.stream().map(v -> new ExpressionColumn(translate(operator, v.get(0), v.get(1), left, right)))
                        .collect(toSet())));
    }


    /**
     * SQL comparing the operands taken in the given classes, according to their comparison mode.
     *
     * @param operator the operator
     * @param lset classes of the left operand
     * @param rset classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL comparing the operands taken in the given classes, according to their comparison mode
     */
    private static String translate(ComparisonOperator operator, Set<ResourceClass> lset, Set<ResourceClass> rset,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        return switch(getComparisonMode(operator, lset, rset))
        {
            case BOOLEAN ->
            {
                Column cl = left.get(genBoolean).get(0);
                Column cr = right.get(genBoolean).get(0);
                yield "(" + cl + " " + operator.getText() + " " + cr + ")";
            }

            case DECIMAL ->
            {
                ResourceClass cmp = determineNumericComparisonType(lset, rset);
                Column cl = left.promoteNumericAs(lset, cmp);
                Column cr = right.promoteNumericAs(rset, cmp);
                yield "(" + cl + " " + operator.getText() + " " + cr + ")";
            }

            case FLOAT ->
            {
                ResourceClass cmp = determineNumericComparisonType(lset, rset);
                Column cl = left.promoteNumericAs(lset, cmp);
                Column cr = right.promoteNumericAs(rset, cmp);
                yield "(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")";
            }

            case DATETIME ->
            {
                //FIXME: we assume that genScalarDateTime is equivalent to genDateTime
                Column cl = left.get(genDateTime).get(0);
                Column cr = right.get(genDateTime).get(0);
                yield "(" + cl + " " + operator.getText() + " " + cr + ")";
            }

            case DATE ->
            {
                Column cl = left.get(genScalarDate).get(0);
                Column cr = right.get(genScalarDate).get(0);
                yield "(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")";
            }

            case DATE_SAME_TZ ->
            {
                Column cl = left.get(genDate).get(0);
                Column cr = right.get(genDate).get(0);
                yield "(" + cl + " " + operator.getText() + " " + cr + ")";
            }

            case DATE_EARLIER_TZ ->
            {
                // if the dates are equal, the left one is actually earlier

                ComparisonOperator effectiveOperator = switch(operator)
                {
                    case GREATER_THAN_OR_EQUAL -> GREATER_THAN;
                    case LESS_THAN -> LESS_THAN_OR_EQUAL;
                    default -> operator;
                };

                Column cl = left.get(genDate).get(0);
                Column cr = right.get(genDate).get(0);
                yield "(" + cl + " " + effectiveOperator.getText() + " " + cr + ")";
            }

            case DATE_LATER_TZ ->
            {
                // if the dates are equal, the left one is actually later

                ComparisonOperator effectiveOperator = switch(operator)
                {
                    case LESS_THAN_OR_EQUAL -> LESS_THAN;
                    case GREATER_THAN -> GREATER_THAN_OR_EQUAL;
                    default -> operator;
                };

                Column cl = left.get(genDate).get(0);
                Column cr = right.get(genDate).get(0);
                yield "(" + cl + " " + effectiveOperator.getText() + " " + cr + ")";
            }

            case BOX ->
            {
                Column cl = left.get(unionize(lset, box)).get(0);
                Column cr = right.get(unionize(rset, box)).get(0);
                yield "(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")";
            }

            case DIFF ->
            {
                if(operator == EQUAL)
                    yield "NULLIF(" + left.getIsNull() + " OR " + right.getIsNull() + ", true)";
                else if(operator == NOT_EQUAL)
                    yield "NULLIF(" + left.getIsNotNull() + " AND " + right.getIsNotNull() + ", false)";
                else
                    throw new IllegalArgumentException();
            }

            case DIRECT ->
            {
                Set<ResourceClass> classes = Stream.concat(lset.stream(), rset.stream()).collect(toSet());
                ResourceClass cmp = unionize(classes);

                //NOTE: to ensure that the expression column is not used more than once during conversion
                if(left.hasExpressionColumn(lset) || right.hasExpressionColumn(rset))
                    cmp = getExpressionClass(classes);

                //FIXME: use correct compare operator, when unionClass is rdfbox

                List<Column> cl = left.get(cmp);
                List<Column> cr = right.get(cmp);

                yield IntStream.range(0, cmp.getColumnCount())
                        .mapToObj(i -> cl.get(i) + " " + operator.getText() + " " + cr.get(i))
                        .collect(joining(" AND ", "(", ")"));
            }

            case LITERAL ->
            {
                Set<ResourceClass> classes = Stream.concat(lset.stream(), rset.stream()).collect(toSet());
                ResourceClass cmp = unionize(classes);

                //NOTE: to ensure that the expression column is not used more than once during conversion
                if(left.hasExpressionColumn(lset) || right.hasExpressionColumn(rset))
                    cmp = getExpressionClass(classes);

                //FIXME: use correct compare operator, when unionClass is rdfbox or user type

                List<Column> cl = left.get(cmp);
                List<Column> cr = right.get(cmp);

                if(operator == EQUAL)
                    yield IntStream.range(0, cmp.getColumnCount()).mapToObj(i -> cl.get(i) + " = " + cr.get(i))
                            .collect(joining(" AND ", "NULLIF(", ", false)"));
                else if(operator == NOT_EQUAL)
                    yield IntStream.range(0, cmp.getColumnCount()).mapToObj(i -> cl.get(i) + " != " + cr.get(i))
                            .collect(joining(" OR ", "NULLIF(", ", true)"));
                throw new IllegalArgumentException();
            }

            default ->
            {
                throw new IllegalArgumentException();
            }
        };
    }


    /**
     * Numeric class in which the operands are compared, by promotion of all their classes.
     *
     * @param lset classes of the left operand
     * @param rset classes of the right operand
     * @return numeric class in which the operands are compared, by promotion of all their classes
     */
    private static ResourceClass determineNumericComparisonType(Set<ResourceClass> lset, Set<ResourceClass> rset)
    {
        Set<ResourceClass> all = Stream.concat(lset.stream(), rset.stream()).collect(toSet());

        if(all.stream().anyMatch(c -> isDouble(c)))
            return xsdDouble;

        if(all.stream().anyMatch(c -> isFloat(c)))
            return xsdFloat;

        if(all.stream().anyMatch(c -> isDecimal(c)))
            return xsdDecimal;

        if(all.stream().allMatch(c -> isRepresentableAsShort(c)))
            return xsdShort;

        if(all.stream().allMatch(c -> isRepresentableAsInt(c)))
            return xsdInt;

        if(all.stream().allMatch(c -> isRepresentableAsLong(c)))
            return xsdLong;

        if(all.stream().allMatch(c -> isDerivatedFromInteger(c)))
            return xsdInteger;

        throw new IllegalArgumentException();
    }


    /**
     * Difference of the timezone offsets in seconds (right minus left); a missing zone counts as UTC.
     *
     * @param left class of the left operand
     * @param right class of the right operand
     * @return difference of the offsets in seconds
     */
    private static int getTimezoneDiff(DateInZone left, DateInZone right)
    {
        int leftZone = left.getZone();
        int rightZone = right.getZone();

        //NOTE: it is assumed that default timezone is UTC

        if(leftZone == Integer.MIN_VALUE)
            leftZone = 0;

        if(rightZone == Integer.MIN_VALUE)
            rightZone = 0;

        return rightZone - leftZone;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        Restriction leftSet = new Restriction();
        Restriction rightSet = new Restriction();

        if(restriction.contains(xsdBoolean))
        {
            for(ResourceClass leftClass : left.getMappings().keySet())
            {
                for(ResourceClass rightClass : right.getMappings().keySet())
                {
                    if(areComparable(operator, leftClass, rightClass) != ComparisonType.NULL)
                    {
                        leftSet.add(leftClass);
                        rightSet.add(rightClass);
                    }
                }
            }
        }

        SqlExpressionIntercode optLeft = left.optimize(request, bindings, leftSet, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, bindings, rightSet, evalServices);

        if(optLeft == left && optRight == right && restriction.isOptimized(variableBinding))
            return this;

        return create(operator, optLeft, optRight, restriction);
    }


    @Override
    public NonConstantBooleanValue getBooleanValue()
    {
        return value;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = 6;

        if(myPriortity > priority)
            builder.append("(");

        left.generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        right.generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinaryComparison imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(operator, imcode.operator))
            return false;

        if(!Objects.equals(value, imcode.value))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operator, left, right);
    }
}
