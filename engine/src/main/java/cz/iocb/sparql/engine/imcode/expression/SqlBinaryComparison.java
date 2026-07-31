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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.expandUnionClasses;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
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
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlBinaryComparison extends SqlBinary implements SqlBooleanExpression
{
    public static enum ComparisonOperator
    {
        EQUAL("=", "equal"),
        NOT_EQUAL("!=", "not_equal"),
        LESS_THAN("<", "less_than"),
        GREATER_THAN(">", "greater_than"),
        LESS_THAN_OR_EQUAL("<=", "not_greater_than"),
        GREATER_THAN_OR_EQUAL(">=", "not_less_than");

        private final String text;
        private final String code;

        ComparisonOperator(String text, String code)
        {
            this.text = text;
            this.code = code;
        }

        public String getText()
        {
            return text;
        }

        public String getName()
        {
            return code;
        }
    }


    protected static enum ComparisonType
    {
        DIFFERENT, NULL, NOT_NULL, FULL
    }


    private static enum ComparisonMode
    {
        NULL,
        DIFF,
        BOOLEAN,
        FLOAT,
        DECIMAL,
        DATETIME,
        DATE_EARLIER_TZ,
        DATE_SAME_TZ,
        DATE_LATER_TZ,
        DATE,
        LITERAL,
        DIRECT,
        BOX
    }


    private static final int SECS_PER_DAY = 24 * 60 * 60;

    private final ComparisonOperator operator;
    private final NonConstantBooleanValue value;


    private SqlBinaryComparison(ComparisonOperator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull, NonConstantBooleanValue value)
    {
        super(left, right, mappings, canBeNull);

        this.operator = operator;
        this.value = value;
    }


    public static SqlExpressionIntercode create(ComparisonOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return create(operator, left, right, Restriction.ALL);
    }


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


    protected static ComparisonType areComparable(ComparisonOperator operator, ResourceClass l, ResourceClass r)
    {
        boolean equalityComparison = (operator == EQUAL || operator == NOT_EQUAL);

        if(equalityComparison && l instanceof DateConstantZoneClass ld && r instanceof DateConstantZoneClass rd
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


    private static ComparisonMode getComparisonMode(ComparisonOperator operator, Set<ResourceClass> left,
            Set<ResourceClass> right)
    {
        ComparisonMode result = null;

        for(ResourceClass l : expandUnionClasses(left))
            for(ResourceClass r : expandUnionClasses(right))
                result = mergeComparisonTypes(result, determineComparisonMode(operator, l, r));

        return result;
    }


    private static ComparisonMode determineComparisonMode(ComparisonOperator operator, ResourceClass left,
            ResourceClass right)
    {
        // special treatment for dates with constant timezones

        if(left instanceof DateConstantZoneClass l && right instanceof DateConstantZoneClass r)
        {
            int diff = getTimezoneDiff(l, r);

            if(diff == 0)
                return ComparisonMode.DATE_SAME_TZ;
            else if(operator == EQUAL || operator == NOT_EQUAL)
                return ComparisonMode.DIFF;
            else if(diff < 0 && diff > -SECS_PER_DAY)
                return ComparisonMode.DATE_EARLIER_TZ;
            else if(diff > 0 && diff < SECS_PER_DAY)
                return ComparisonMode.DATE;
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


    private static boolean isDateMode(ComparisonMode mode)
    {
        return mode == ComparisonMode.DATE || mode == ComparisonMode.DATE_EARLIER_TZ
                || mode == ComparisonMode.DATE_SAME_TZ || mode == ComparisonMode.DATE_LATER_TZ;
    }


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


    private static List<Column> translate(ComparisonOperator operator, Set<List<Set<ResourceClass>>> variants,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        if(variants == null)
            return null;

        return List.of(Column.coalesce(
                variants.stream().map(v -> new ExpressionColumn(translate(operator, v.get(0), v.get(1), left, right)))
                        .collect(toSet())));
    }


    private static String translate(ComparisonOperator operator, Set<ResourceClass> lset, Set<ResourceClass> rset,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        return switch(getComparisonMode(operator, lset, rset))
        {
            case BOOLEAN ->
            {
                Column cl = left.get(xsdBoolean).get(0);
                Column cr = right.get(xsdBoolean).get(0);
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
                Column cl = left.get(xsdCompositeDateTime).get(0);
                Column cr = right.get(xsdCompositeDateTime).get(0);
                yield "(" + cl + " " + operator.getText() + " " + cr + ")";
            }

            case DATE ->
            {
                Column cl = left.get(xsdScalarDate).get(0);
                Column cr = right.get(xsdScalarDate).get(0);
                yield "(" + cl + " operator(sparql." + operator.getText() + ") " + cr + ")";
            }

            case DATE_SAME_TZ ->
            {
                Column cl = left.get(xsdCompositeDate).get(0);
                Column cr = right.get(xsdCompositeDate).get(0);
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

                Column cl = left.get(xsdCompositeDate).get(0);
                Column cr = right.get(xsdCompositeDate).get(0);
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

                Column cl = left.get(xsdCompositeDate).get(0);
                Column cr = right.get(xsdCompositeDate).get(0);
                yield "(" + cl + " " + effectiveOperator.getText() + " " + cr + ")";
            }

            case BOX ->
            {
                Column cl = left.get(getUnionClass(lset, box)).get(0);
                Column cr = right.get(getUnionClass(rset, box)).get(0);

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
                ResourceClass cmp = getUnionClass(Stream.concat(lset.stream(), rset.stream()).collect(toSet()));
                List<Column> cl = left.get(cmp);
                List<Column> cr = right.get(cmp);

                yield IntStream.range(0, cmp.getColumnCount())
                        .mapToObj(i -> cl.get(i) + " " + operator.getText() + " " + cr.get(i))
                        .collect(joining(" AND ", "(", ")"));
            }

            case LITERAL ->
            {
                ResourceClass cmp = getUnionClass(Stream.concat(lset.stream(), rset.stream()).collect(toSet()));
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


    private static ResourceClass determineNumericComparisonType(Set<ResourceClass> lset, Set<ResourceClass> rset)
    {
        Set<ResourceClass> all = Stream.concat(lset.stream(), rset.stream()).collect(toSet());

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdDouble)))
            return xsdDouble;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdFloat)))
            return xsdFloat;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdDecimal)))
            return xsdDecimal;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdInteger)))
            return xsdInteger;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdLong)))
            return xsdLong;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdInt)))
            return xsdInt;

        if(all.stream().anyMatch(c -> c.isSubclassOf(xsdShort)))
            return xsdShort;

        throw new IllegalArgumentException();
    }


    private static int getTimezoneDiff(DateConstantZoneClass left, DateConstantZoneClass right)
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
