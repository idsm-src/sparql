package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.database.SqlType.FLOAT4;
import static cz.iocb.sparql.engine.database.SqlType.FLOAT8;
import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasUnsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasUserLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isStringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsupportedLiteral;
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
import static java.util.stream.Collectors.toSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.config.EngineSettings;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.mapping.classes.CanonicalLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass.Component;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;
import info.adams.ryu.RyuFloat;



/**
 * Comparison of two terms following the SPARQL operator mapping: numeric, boolean, string, date and date-time
 * comparison, and the equality of RDF terms as the function sameValue() of SPARQL 1.2 defines it: triple terms are
 * compared by their components, literals of different handled datatypes are different values, and a literal of an
 * unknown datatype or an ill-typed one equals the same term only and is an error otherwise; ordering comparisons of
 * incompatible types are errors. Dates and date-times of different timezones are compared through their common
 * representation. An exact numeric stored in an integer type of SQL is compared with a constant in that type, so that
 * the database can look the comparison up by an index of the column.
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


        /**
         * The operator that gives the same result when the operands are swapped.
         *
         * @return the operator that gives the same result when the operands are swapped
         */
        public ComparisonOperator getSwapped()
        {
            return switch(this)
            {
                case LESS_THAN -> GREATER_THAN;
                case GREATER_THAN -> LESS_THAN;
                case LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL;
                case GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL;
                default -> this;
            };
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
         * Comparison of the values of durations, whatever their lexical forms are; the ordering operators, for which
         * SPARQL has no row in its operator mapping, compare them as the box does.
         */
        DURATION,

        /**
         * Equality of the values of literals of one user datatype, whatever their lexical forms are: the value column
         * of the base class of the datatype compared by the equality of its type.
         */
        USER_VALUE,

        /**
         * Equality of literals of the unsupported class: the same term is equal, anything else is an error.
         */
        LITERAL,

        /**
         * Comparison of the columns: the equality operators by the identity of the types of the columns, which is the
         * equality of the values for the classes whose equal values are the same terms (IRIs, blank nodes, strings,
         * language-tagged strings, the literals of one canonical class), the ordering operators of strings by the
         * operators of their type.
         */
        DIRECT,

        /**
         * Comparison of two triple terms by their components.
         */
        TRIPLE_TERM,

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
     * @param request the current request
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @return comparison of the operands
     */
    public static SqlExpressionIntercode create(Request request, ComparisonOperator operator,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        return create(request, operator, left, right, Restriction.ALL);
    }


    /**
     * The comparison of two constant literals decided at translation where their values decide it alike in every
     * datatype the extension would compare them in: exact numbers of any of their datatypes by their values and
     * booleans by every operator, two xsd:strings by equality, and two literals of unknown datatypes or of invalid
     * lexical forms by equality, which is true for one term and an error otherwise. Floating-point numbers, temporal
     * values, durations, user literals and the order of strings, which the database decides by its rules, are left to
     * SQL, as are pairs of other datatypes.
     *
     * @param operator the operator
     * @param left the left literal
     * @param right the right literal
     * @return the constant result of the comparison, or null when SQL compares the literals
     */
    private static SqlExpressionIntercode compareConstants(ComparisonOperator operator, SqlLiteral left,
            SqlLiteral right)
    {
        boolean equality = operator == EQUAL || operator == NOT_EQUAL;

        if(left.isUnsupported() && right.isUnsupported())
        {
            if(!equality)
                return null;

            return left.getLiteral().equals(right.getLiteral()) ? decide(operator, 0) : SqlNull.get();
        }

        if(left.getExactNumber() != null && right.getExactNumber() != null)
            return decide(operator, left.getExactNumber().compareTo(right.getExactNumber()));

        if(left.getBoolean() != null && right.getBoolean() != null)
            return decide(operator, left.getBoolean().compareTo(right.getBoolean()));

        if(equality && left.getString() != null && right.getString() != null)
            return decide(operator, left.getString().equals(right.getString()) ? 0 : 1);

        return null;
    }


    /**
     * The constant result of the operator over operands of the given order.
     *
     * @param operator the operator
     * @param order negative, zero or positive when the left operand is less than, equal to or greater than the other
     * @return the constant result of the operator
     */
    private static SqlExpressionIntercode decide(ComparisonOperator operator, int order)
    {
        boolean value = switch(operator)
        {
            case EQUAL -> order == 0;
            case NOT_EQUAL -> order != 0;
            case LESS_THAN -> order < 0;
            case GREATER_THAN -> order > 0;
            case LESS_THAN_OR_EQUAL -> order <= 0;
            case GREATER_THAN_OR_EQUAL -> order >= 0;
        };

        return value ? trueValue : falseValue;
    }


    /**
     * Comparison materialising only the needed result; constant when the classes decide it. Operands bound in more
     * combinations of classes than the settings of the engine allow (see {@link EngineSettings#boxComparisonLimit()})
     * are compared as boxes.
     *
     * @param request the current request
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param restriction the result classes the parent needs
     * @return comparison materialising only the needed result; constant when the classes decide it
     */
    private static SqlExpressionIntercode create(Request request, ComparisonOperator operator,
            SqlExpressionIntercode left, SqlExpressionIntercode right, Restriction restriction)
    {
        ClassRelations relations = request.getConfiguration();

        if(left instanceof SqlIri a && right instanceof SqlIri b)
        {
            if(operator == EQUAL)
                return a.getIri().equals(b.getIri()) ? trueValue : falseValue;
            else if(operator == NOT_EQUAL)
                return a.getIri().equals(b.getIri()) ? falseValue : trueValue;
        }

        // an operand always bound to a constant IRI, like a variable its pattern binds to one, compares like the IRI
        // (a literal equal to itself is another matter: a NaN is not)
        boolean iris = left.getMappings().keySet().stream().allMatch(c -> c instanceof IriClass);
        Boolean same = iris && (operator == EQUAL || operator == NOT_EQUAL) ? isSameConstantTerm(left, right) : null;

        if(same != null)
            return same == (operator == EQUAL) ? trueValue : falseValue;

        if(left instanceof SqlLiteral a && right instanceof SqlLiteral b)
        {
            SqlExpressionIntercode decided = compareConstants(operator, a, b);

            if(decided != null)
                return decided;
        }

        Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();
        Set<List<ResourceClass>> set = new HashSet<>();
        map.put(xsdBoolean, set);

        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();

        for(ResourceClass leftClass : left.getMappings().keySet())
        {
            for(ResourceClass rightClass : right.getMappings().keySet())
            {
                switch(areComparable(relations, operator, leftClass, rightClass))
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


        NonConstantBooleanValue value = switch(operator)
        {
            case EQUAL -> isAlwaysDifferentIfNotNull ? FALSE_OR_ERROR : ANY;
            case NOT_EQUAL -> isAlwaysDifferentIfNotNull ? TRUE_OR_ERROR : ANY;
            default -> ANY;
        };

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        if(set.size() > request.getEngineSettings().boxComparisonLimit())
        {
            // too many combinations to compare them one by one (operands bound in most classes of the configuration,
            // such as the variables of an unevaluated SERVICE call): the operands are compared as boxes, whose
            // operators implement the operator mapping of SPARQL for terms of every kind
            Set<ResourceClass> lset = set.stream().map(c -> c.get(0)).collect(toSet());
            Set<ResourceClass> rset = set.stream().map(c -> c.get(1)).collect(toSet());

            SqlCode code = SqlCode.of("(", getBox(left, lset), " operator(sparql.", operator.getText(), ") ",
                    getBox(right, rset), ")");

            mappings.put(xsdBoolean,
                    restriction.contains(relations, xsdBoolean) ? List.of(new ExpressionColumn(code, BOOL)) : null);
        }
        else
        {
            List<SqlExpressionIntercode> operands = List.of(left, right);
            Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, operands, map,
                    restriction);

            for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                mappings.put(e.getKey(),
                        e.getValue() == null ? null : translate(request, operator, e.getValue(), left, right));
        }

        return new SqlBinaryComparison(operator, left, right, mappings, canBeNull, value);
    }


    /**
     * Box holding the value of the operand taken in the given classes: the COALESCE of their conversions to the box.
     * Only the given classes are converted, the other classes of the operand being possibly not materialised.
     *
     * @param operand the operand
     * @param classes the classes of the operand to take
     * @return box holding the value of the operand taken in the given classes
     */
    private static Column getBox(SqlExpressionIntercode operand, Set<ResourceClass> classes)
    {
        return Column.coalesce(
                classes.stream().map(c -> c.toGeneralClass(box, operand.getMapping(c), true).get(0)).collect(toSet()));
    }


    /**
     * Outcome of comparing values of the two classes with the operator, decided from the classes alone.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param l class on the left side
     * @param r class on the right side
     * @return outcome of comparing values of the two classes with the operator, decided from the classes alone
     */
    protected static ComparisonType areComparable(ClassRelations relations, ComparisonOperator operator,
            ResourceClass l, ResourceClass r)
    {
        boolean equalityComparison = (operator == EQUAL || operator == NOT_EQUAL);

        if(equalityComparison && l instanceof DateInZoneClass ld && r instanceof DateInZoneClass rd
                && getTimezoneDiff(ld, rd) % SECS_PER_DAY != 0)
            return ComparisonType.DIFFERENT;

        if(isNumeric(l) && isNumeric(r))
            return ComparisonType.NOT_NULL;

        if(isString(l) && isString(r))
            return (equalityComparison && areDisjunct(relations, l, r)) ? ComparisonType.DIFFERENT :
                    ComparisonType.NOT_NULL;

        if(isBoolean(l) && isBoolean(r))
            return ComparisonType.NOT_NULL;

        if(isDateTime(l) && isDateTime(r))
            return ComparisonType.NOT_NULL;

        if(isDate(l) && isDate(r))
            return ComparisonType.NOT_NULL;

        // durations compare by value whatever their lexical forms are, as the canonical and the other ones are classes
        // of their own
        if(isDayTimeDuration(l) && isDayTimeDuration(r))
            return ComparisonType.NOT_NULL;

        // so do the literals of one user datatype, which the box orders by the comparison of their type, if it has one
        if(areOfOneUserDatatype(l, r))
            return equalityComparison ? ComparisonType.NOT_NULL : ComparisonType.FULL;

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

        if(hasDayTimeDuration(l) && hasDayTimeDuration(r))
            return ComparisonType.FULL;

        if(!equalityComparison && hasUserLiteral(l) && hasUserLiteral(r))
            return ComparisonType.FULL;

        if(!equalityComparison)
            return ComparisonType.NULL;

        if(l.getEffectiveClass() instanceof TripleTermClass lt && r.getEffectiveClass() instanceof TripleTermClass rt)
            return combineComponentComparisons(areComparable(relations, operator, lt.getSubject(), rt.getSubject()),
                    areComparable(relations, operator, lt.getPredicate(), rt.getPredicate()),
                    areComparable(relations, operator, lt.getObject(), rt.getObject()));

        if(isLiteral(l) && isLiteral(r))
        {
            // sameValue() of SPARQL 1.2: literals of two different handled datatypes are different values and literals
            // of the same handled datatype compare by value, whereas a literal of an unknown datatype or an ill-typed
            // one, both of the unsupported class, equals the same term only and is an error otherwise
            if(isUnsupportedLiteral(l) || isUnsupportedLiteral(r))
                return areDisjunct(relations, l, r) ? ComparisonType.NULL : ComparisonType.FULL;

            if(hasUnsupportedLiteral(l) || hasUnsupportedLiteral(r))
                return ComparisonType.FULL;

            return areDisjunct(relations, l, r) ? ComparisonType.DIFFERENT : ComparisonType.NOT_NULL;
        }

        if(hasLiteral(l) && hasLiteral(r))
            return ComparisonType.FULL;

        // triple terms of disjoint classes may still have equal values (through the box, the extension compares them)
        if(hasTripleTerm(l) && hasTripleTerm(r))
            return ComparisonType.FULL;

        if(areDisjunct(relations, l, r))
            return ComparisonType.DIFFERENT;

        return ComparisonType.NOT_NULL;
    }


    /**
     * The terms a value of the given classes can be for the value to be equal to the literal, or null when the equality
     * singles no terms out. A string, a language-tagged string and a literal of the unsupported class are equal to
     * themselves only, whatever the classes are. An exact number and a boolean are equal to the literals of their value
     * in every datatype they are compared with, of which a canonical class holds one at most, the literal of the
     * canonical lexical form: the value has to be one of those terms when the classes that can hold an equal value at
     * all are canonical, and that very term when they are of one datatype. No terms are singled out when a class holds
     * the value in several terms (in other lexical forms, or in a box), by a floating-point number on either side (the
     * zeros of both signs are equal), nor by the other literals compared by value (dates, date-times and durations).
     *
     * @param request the current request
     * @param classes the classes of the value
     * @param literal the literal
     * @return the terms a value of the classes can be to be equal to the literal, in the order of their datatypes, or
     *         null when the equality singles none out
     */
    public static Set<Literal> getEqualTerms(Request request, Set<ResourceClass> classes, Literal literal)
    {
        ClassRelations relations = request.getConfiguration();
        ResourceClass literalClass = request.getLiteralClass(literal);

        if(isStringLiteral(literalClass) || isUnsupportedLiteral(literalClass))
            return Set.of(literal);

        boolean number = isNumeric(literalClass) && !isFloatPoint(literalClass);

        if(!number && !isBoolean(literalClass))
            return null;

        // the lexical form of the value that every datatype holding the value takes: a number without a fractional
        // part is written as an integer
        String form = request.getConfiguration().getDatatype(literal.getType())
                .getCanonicalLexicalForm(literal.getValue());

        if(number)
        {
            BigDecimal value = new BigDecimal(form);
            form = value.stripTrailingZeros().scale() > 0 ? value.toPlainString() :
                    value.toBigIntegerExact().toString();
        }

        // the term of each datatype, kept by the IRI of the datatype, so that the terms do not come in the order the
        // classes happen to have
        Map<String, Literal> terms = new TreeMap<>();

        for(ResourceClass resClass : classes)
        {
            ComparisonType type = areComparable(relations, EQUAL, resClass, literalClass);

            // no value of the class is equal to the literal
            if(type == ComparisonType.DIFFERENT || type == ComparisonType.NULL)
                continue;

            if(!(resClass instanceof CanonicalLiteralClass canonical) || isFloatPoint(resClass))
                return null;

            Datatype datatype = canonical.getDatatype();

            if(!datatype.isValidForm(form))
                continue;

            Literal equal = new TypedLiteral(datatype.getCanonicalLexicalForm(form), datatype.getTypeIri());

            // a class narrowed by a deployment need not hold the literal
            if(canonical.match(request, equal))
                terms.put(datatype.getTypeIri().getValue(), equal);
        }

        return terms.isEmpty() ? null : new LinkedHashSet<>(terms.values());
    }


    /**
     * True if two values of the given classes can be equal only as the same term, so that an equality of them holds for
     * no other values than a test of the same term does: IRIs, blank nodes, strings, language-tagged strings and
     * literals of the unsupported class are equal to themselves only, and so are the exact numbers and the booleans of
     * the canonical classes of one datatype, which hold a value in a single term. Equal values may be different terms
     * where the classes are numbers of two datatypes or floating-point numbers (the zeros of both signs are equal),
     * where a class holds a value in several terms (in other lexical forms, or in a box), and among the other literals
     * compared by value (dates, date-times and durations) and the triple terms.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left class of the first value
     * @param right class of the second value
     * @return true if two values of the classes can be equal only as the same term, false otherwise
     */
    public static boolean isTermEquality(ClassRelations relations, ResourceClass left, ResourceClass right)
    {
        ComparisonType type = areComparable(relations, EQUAL, left, right);

        // no values of the classes are equal
        if(type == ComparisonType.DIFFERENT || type == ComparisonType.NULL)
            return true;

        if(isIri(left) && isIri(right) || isBlankNode(left) && isBlankNode(right))
            return true;

        if(isStringLiteral(left) && isStringLiteral(right) || isUnsupportedLiteral(left) && isUnsupportedLiteral(right))
            return true;

        if(!(left instanceof CanonicalLiteralClass l && right instanceof CanonicalLiteralClass r))
            return false;

        return (isNumeric(left) && !isFloatPoint(left) || isBoolean(left))
                && l.getDatatype().getTypeIri().equals(r.getDatatype().getTypeIri());
    }


    /**
     * Outcome of comparing two triple terms given the outcomes of comparing their components, combined by the
     * three-valued {@code &&} of the SPARQL 1.2 operator mapping: a component that is never equal makes the terms never
     * equal whatever the other components do, otherwise a possible error of a component is a possible error of the
     * whole.
     *
     * @param components outcomes of comparing the components
     * @return outcome of comparing two triple terms given the outcomes of comparing their components
     */
    private static ComparisonType combineComponentComparisons(ComparisonType... components)
    {
        boolean allErrors = true;
        boolean noErrors = true;

        for(ComparisonType component : components)
        {
            if(component == ComparisonType.DIFFERENT)
                return ComparisonType.DIFFERENT;

            allErrors &= component == ComparisonType.NULL;
            noErrors &= component == ComparisonType.NOT_NULL;
        }

        if(allErrors)
            return ComparisonType.NULL;

        return noErrors ? ComparisonType.NOT_NULL : ComparisonType.FULL;
    }


    /**
     * Comparison mode covering all pairs of operand classes.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param left classes of the left operand
     * @param right classes of the right operand
     * @return comparison mode covering all pairs of operand classes
     */
    private static ComparisonMode getComparisonMode(ClassRelations relations, ComparisonOperator operator,
            Set<ResourceClass> left, Set<ResourceClass> right)
    {
        ComparisonMode result = null;
        Set<PrimitiveResourceClass> rights = estimateAsUnion(right);

        for(ResourceClass l : estimateAsUnion(left))
        {
            for(ResourceClass r : rights)
                result = mergeComparisonTypes(result, determineComparisonMode(relations, operator, l, r));

            // the box absorbs every other mode
            if(result == ComparisonMode.BOX)
                return result;
        }

        return result;
    }


    /**
     * Comparison mode of a pair of classes according to the SPARQL operator mapping; equality of dates in zones
     * differing by less than a day is handled specially.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param left class of the left operand
     * @param right class of the right operand
     * @return comparison mode of a pair of classes according to the SPARQL operator mapping; equality of dates in zones
     *         differing by less than a day is handled specially
     */
    private static ComparisonMode determineComparisonMode(ClassRelations relations, ComparisonOperator operator,
            ResourceClass left, ResourceClass right)
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
        else if(isDayTimeDuration(left) && isDayTimeDuration(right))
            return ComparisonMode.DURATION;
        else if(areOfOneUserDatatype(left, right))
            return operator == EQUAL || operator == NOT_EQUAL ? ComparisonMode.USER_VALUE : ComparisonMode.BOX;
        else if(isBoolean(left) && isBoolean(right))
            return ComparisonMode.BOOLEAN;
        else if(isString(left) && isString(right))
            return ComparisonMode.DIRECT;
        else if(hasNumeric(left) && hasNumeric(right))
            return ComparisonMode.BOX;
        else if(hasDateTime(left) && hasDateTime(right))
            return ComparisonMode.BOX;
        else if(hasDate(left) && hasDate(right))
            return ComparisonMode.BOX;
        else if(hasDayTimeDuration(left) && hasDayTimeDuration(right))
            return ComparisonMode.BOX;
        else if(hasBoolean(left) && hasBoolean(right))
            return ComparisonMode.BOX;
        else if(hasString(left) && hasString(right))
            return ComparisonMode.BOX;
        else if(operator != EQUAL && operator != NOT_EQUAL && hasUserLiteral(left) && hasUserLiteral(right))
            return ComparisonMode.BOX;
        else if(operator != EQUAL && operator != NOT_EQUAL)
            return ComparisonMode.NULL;
        else if(left.getEffectiveClass() instanceof TripleTermClass
                && right.getEffectiveClass() instanceof TripleTermClass)
            return ComparisonMode.TRIPLE_TERM;
        else if(isLiteral(left) && isLiteral(right) && ResourceClass.areDisjunct(relations, left, right))
            return isUnsupportedLiteral(left) || isUnsupportedLiteral(right) ? ComparisonMode.NULL :
                    ComparisonMode.DIFF;
        else if(isLiteral(left) && isLiteral(right))
            return isUnsupportedLiteral(left) || isUnsupportedLiteral(right) ? ComparisonMode.LITERAL :
                    ComparisonMode.DIRECT;
        else if(!hasLiteral(left) && !hasLiteral(right))
            return ResourceClass.areDisjunct(relations, left, right) ? ComparisonMode.DIFF : ComparisonMode.DIRECT;
        else
            return ComparisonMode.BOX;
    }


    /**
     * The base class of the user datatype all literals of the class are of: the class of the valid literals of the
     * datatype, which holds the value of a literal in its first column and the lexical form of a literal in another
     * than the canonical form in the second one.
     *
     * @param resClass the class
     * @return the base class of the user datatype, or null if the class holds no literals of a single user datatype
     */
    private static LiteralClass getUserDatatypeBase(ResourceClass resClass)
    {
        ResourceClass effective = resClass.getEffectiveClass();

        if(effective instanceof UserLiteralClass canonical)
            return canonical.getBaseClass();

        if(effective instanceof UserLiteralBaseClass base)
            return base;

        return null;
    }


    /**
     * True if all literals of both classes are of one user datatype, whose canonical and other lexical forms are held
     * by classes of their own.
     *
     * @param left one class
     * @param right the other class
     * @return true if all literals of both classes are of one user datatype, false otherwise
     */
    private static boolean areOfOneUserDatatype(ResourceClass left, ResourceClass right)
    {
        LiteralClass base = getUserDatatypeBase(left);

        return base != null && base.equals(getUserDatatypeBase(right));
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
     * @param request the current request
     * @param operator the operator
     * @param variants combinations of argument classes
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison as a COALESCE over the argument class combinations
     */
    private static List<Column> translate(Request request, ComparisonOperator operator,
            Set<List<Set<ResourceClass>>> variants, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        if(variants == null)
            return null;

        return List.of(Column.coalesce(variants.stream()
                .map(v -> new ExpressionColumn(translate(request, operator, v.get(0), v.get(1), left, right), BOOL))
                .collect(toSet())));
    }


    /**
     * SQL comparing the operands taken in the given classes, according to their comparison mode.
     *
     * @param request the current request
     * @param operator the operator
     * @param lset classes of the left operand
     * @param rset classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL comparing the operands taken in the given classes, according to their comparison mode
     */
    private static SqlCode translate(Request request, ComparisonOperator operator, Set<ResourceClass> lset,
            Set<ResourceClass> rset, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        ClassRelations relations = request.getConfiguration();

        return switch(getComparisonMode(relations, operator, lset, rset))
        {
            case BOOLEAN ->
            {
                Column cl = left.get(relations, genBoolean).get(0);
                Column cr = right.get(relations, genBoolean).get(0);
                yield SqlCode.of("(", cl, " ", operator.getText(), " ", cr, ")");
            }

            case DECIMAL ->
            {
                // an operand stored in an integer type of SQL is compared with a constant in that type
                SqlCode code = translateWithConstant(operator, lset, rset, left, right);

                if(code != null)
                    yield code;

                // two operands stored in integer types of SQL are compared as they are stored
                code = translateStoredIntegers(operator, lset, rset, left, right);

                if(code != null)
                    yield code;

                ResourceClass cmp = determineNumericComparisonType(lset, rset);
                Column cl = left.promoteNumericAs(lset, cmp);
                Column cr = right.promoteNumericAs(rset, cmp);
                yield SqlCode.of("(", cl, " ", operator.getText(), " ", cr, ")");
            }

            case FLOAT ->
            {
                // an operand stored in another type than the comparison takes is compared with a constant in that type
                SqlCode code = translateWithFloatingConstant(operator, lset, rset, left, right);

                if(code != null)
                    yield code;

                ResourceClass cmp = determineNumericComparisonType(lset, rset);
                Column cl = left.promoteNumericAs(lset, cmp);
                Column cr = right.promoteNumericAs(rset, cmp);
                yield SqlCode.of("(", cl, " operator(sparql.", operator.getText(), ") ", cr, ")");
            }

            case DATETIME ->
            {
                //NOTE: genScalarDateTime and genDateTime hold the same values (see DateTimeScalarBaseClass), so every
                // operand is taken as genDateTime, whose instants the built-in operators of timestamptz compare
                Column cl = left.get(relations, genDateTime).get(0);
                Column cr = right.get(relations, genDateTime).get(0);
                yield SqlCode.of("(", cl, " ", operator.getText(), " ", cr, ")");
            }

            case DATE ->
            {
                Column cl = left.get(relations, genScalarDate).get(0);
                Column cr = right.get(relations, genScalarDate).get(0);
                yield SqlCode.of("(", cl, " operator(sparql.", operator.getText(), ") ", cr, ")");
            }

            case DURATION ->
            {
                // the length of the duration in microseconds, the first column of genDayTimeDuration
                Column cl = left.get(relations, genDayTimeDuration).get(0);
                Column cr = right.get(relations, genDayTimeDuration).get(0);
                yield SqlCode.of("(", cl, " ", operator.getText(), " ", cr, ")");
            }

            case USER_VALUE ->
            {
                // every class compared so is of the one datatype, a class of another one making the mode the box
                LiteralClass base = getUserDatatypeBase(lset.iterator().next());
                Column cl = left.get(relations, base).get(0);
                Column cr = right.get(relations, base).get(0);
                SqlType type = base.getSqlTypes().get(0);
                yield operator == EQUAL ? type.equal(cl, cr) : type.notEqual(cl, cr);
            }

            case DATE_SAME_TZ ->
            {
                Column cl = left.get(relations, genDate).get(0);
                Column cr = right.get(relations, genDate).get(0);
                yield SqlCode.of("(", cl, " ", operator.getText(), " ", cr, ")");
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

                Column cl = left.get(relations, genDate).get(0);
                Column cr = right.get(relations, genDate).get(0);
                yield SqlCode.of("(", cl, " ", effectiveOperator.getText(), " ", cr, ")");
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

                Column cl = left.get(relations, genDate).get(0);
                Column cr = right.get(relations, genDate).get(0);
                yield SqlCode.of("(", cl, " ", effectiveOperator.getText(), " ", cr, ")");
            }

            case TRIPLE_TERM ->
            {
                // the operator mapping of SPARQL 1.2: (A.subject = B.subject) && (A.predicate = B.predicate) &&
                // (A.object = B.object), the three-valued && of SPARQL being the AND of SQL, so that a component that
                // differs decides even when another one is an error
                List<SqlCode> parts = new ArrayList<>();
                boolean different = false;

                for(Component component : Component.values())
                {
                    SqlExpressionIntercode l = SqlValue.tripleTermComponent(relations, left, lset, component);
                    SqlExpressionIntercode r = SqlValue.tripleTermComponent(relations, right, rset, component);
                    SqlExpressionIntercode comparison = create(request, EQUAL, l, r);

                    if(comparison.equals(SqlNull.get()))
                        parts.add(new SqlCode("NULL::bool"));
                    else if(comparison.equals(falseValue))
                        different = true;
                    else if(!comparison.equals(trueValue))
                        parts.add(comparison.get(relations, xsdBoolean).get(0).getCode());
                }

                SqlCode equality;

                if(different)
                    equality = SqlCode.FALSE;
                else if(parts.isEmpty())
                    equality = SqlCode.TRUE;
                else
                    equality = parts.stream().collect(SqlCode.joining(" AND ", "(", ")"));

                yield operator == EQUAL ? equality : SqlCode.of("NOT ", equality);
            }

            case BOX ->
            {
                Column cl = left.get(relations, unionize(lset, box)).get(0);
                Column cr = right.get(relations, unionize(rset, box)).get(0);
                yield SqlCode.of("(", cl, " operator(sparql.", operator.getText(), ") ", cr, ")");
            }

            case DIFF ->
            {
                // decided only for values of the classes compared here, the other variants cover the other classes
                SqlCode present = Stream
                        .of(lset.stream().map(c -> left.getIsNotNull(relations, c)),
                                rset.stream().map(c -> right.getIsNotNull(relations, c)))
                        .map(s -> s.collect(SqlCode.joining(" OR ", "(", ")"))).collect(SqlCode.joining(" AND "));

                if(operator == EQUAL)
                    yield SqlCode.of("CASE WHEN ", present, " THEN false END");
                else if(operator == NOT_EQUAL)
                    yield SqlCode.of("CASE WHEN ", present, " THEN true END");
                else
                    throw new IllegalArgumentException();
            }

            case DIRECT ->
            {
                Set<ResourceClass> classes = Stream.concat(lset.stream(), rset.stream()).collect(toSet());
                ResourceClass cmp = unionize(classes);

                //NOTE: to ensure that the expression column is not used more than once during conversion
                if(left.hasExpressionColumn(relations, lset) || right.hasExpressionColumn(relations, rset))
                    cmp = getExpressionClass(classes);

                List<Column> cl = left.get(relations, cmp);
                List<Column> cr = right.get(relations, cmp);

                if(operator == EQUAL)
                    yield getIdentityConditions(cmp, cl, cr).stream().collect(SqlCode.joining(" AND ", "(", ")"));

                if(operator == NOT_EQUAL)
                {
                    // the operator of the type, which a box or another type of the extension has of its own
                    List<SqlCode> conditions = getNonIdentityConditions(cmp, cl, cr);

                    yield conditions.size() == 1 ? conditions.get(0) :
                            conditions.stream().collect(SqlCode.joining(" OR ", "(", ")"));
                }

                yield IntStream.range(0, cmp.getColumnCount())
                        .mapToObj(i -> SqlCode.of(cl.get(i), " ", operator.getText(), " ", cr.get(i)))
                        .collect(SqlCode.joining(" AND ", "(", ")"));
            }

            case LITERAL ->
            {
                Set<ResourceClass> classes = Stream.concat(lset.stream(), rset.stream()).collect(toSet());
                ResourceClass cmp = unionize(classes);

                //NOTE: to ensure that the expression column is not used more than once during conversion
                if(left.hasExpressionColumn(relations, lset) || right.hasExpressionColumn(relations, rset))
                    cmp = getExpressionClass(classes);

                List<Column> cl = left.get(relations, cmp);
                List<Column> cr = right.get(relations, cmp);

                if(operator == EQUAL)
                    yield getIdentityConditions(cmp, cl, cr).stream()
                            .collect(SqlCode.joining(" AND ", "NULLIF(", ", false)"));
                else if(operator == NOT_EQUAL)
                    yield getNonIdentityConditions(cmp, cl, cr).stream()
                            .collect(SqlCode.joining(" OR ", "NULLIF(", ", true)"));
                throw new IllegalArgumentException();
            }

            default ->
            {
                throw new IllegalArgumentException();
            }
        };
    }


    /**
     * SQL of an exact numeric comparison of a constant with an operand whose values are stored in an integer type of
     * SQL other than the type the two are compared in (see {@link #determineNumericComparisonType}), or null when the
     * comparison is no such one. Promoting the operand to the comparison type, as the operator mapping of SPARQL says,
     * wraps its column in a conversion function, so the database can neither look the rows up by an index of the column
     * nor estimate them by its statistics. But the promotion keeps the values and their order, so the comparison has
     * the same result in the type of the stored values, with the constant converted to that type instead (see
     * {@link #translateWithConstant(ComparisonOperator, Column, BigDecimal)}).
     *
     * @param operator the operator
     * @param lset exact numeric classes of the left operand
     * @param rset exact numeric classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison in the type the operand that is no constant is stored in, or null
     */
    private static SqlCode translateWithConstant(ComparisonOperator operator, Set<ResourceClass> lset,
            Set<ResourceClass> rset, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        SqlType type = determineNumericComparisonType(lset, rset).getSqlTypes().get(0);
        Column lvalue = left.getBinding().getStoredNumeric(lset);
        Column rvalue = right.getBinding().getStoredNumeric(rset);

        if(lvalue == null || rvalue == null)
            return null;

        BigDecimal lconstant = getExactConstant(left, lset, lvalue);
        BigDecimal rconstant = getExactConstant(right, rset, rvalue);

        if(rconstant != null && !lvalue.getType().equals(type))
            return translateWithConstant(operator, lvalue, rconstant);

        if(lconstant != null && !rvalue.getType().equals(type))
            return translateWithConstant(operator.getSwapped(), rvalue, lconstant);

        return null;
    }


    /**
     * SQL of an exact numeric comparison of two operands whose values are both stored in integer types of SQL, or null
     * when the comparison is no such one. A value stored in int2, int4 or int8 is the number itself, whatever its
     * datatype is (xsd:int and xsd:long, xsd:unsignedShort and xsd:int), and the database compares integers of
     * different types by their values, so the operands are compared as they are stored. Promoting them to the datatype
     * they are compared in (see {@link #determineNumericComparisonType}) would wrap a column of another type in a
     * conversion function, and then the database can neither look the rows up by an index of the column nor estimate
     * them by its statistics.
     *
     * @param operator the operator
     * @param lset exact numeric classes of the left operand
     * @param rset exact numeric classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison of the operands as they are stored, or null
     */
    private static SqlCode translateStoredIntegers(ComparisonOperator operator, Set<ResourceClass> lset,
            Set<ResourceClass> rset, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        Column lvalue = left.getBinding().getStoredNumeric(lset);
        Column rvalue = right.getBinding().getStoredNumeric(rset);

        if(lvalue == null || rvalue == null || getIntegerBits(lvalue.getType()) == 0
                || getIntegerBits(rvalue.getType()) == 0)
            return null;

        return SqlCode.of("(", lvalue, " ", operator.getText(), " ", rvalue, ")");
    }


    /**
     * The number of bits of an integer type of SQL (int2, int4, int8), or zero for any other type.
     *
     * @param type the SQL type
     * @return the number of bits of the integer type, or zero when the type is no integer type
     */
    private static int getIntegerBits(SqlType type)
    {
        return type.equals(INT2) ? 16 : type.equals(INT4) ? 32 : type.equals(INT8) ? 64 : 0;
    }


    /**
     * SQL comparing a stored value with an exact constant on its right side in the type of the value, or null when that
     * is no integer type of SQL. A constant the type holds is written in the type. Any other constant decides the
     * comparison by its position among the values of the type: a value is equal to no constant that is not an integer
     * of the type, and it is less than a constant when it is less than the least integer that is not less than the
     * constant, whatever lies beyond the range of the type being greater, or less, than every value. The constant is
     * never left to a cast of SQL, which fails for a value out of the range.
     *
     * @param operator the operator
     * @param value the left operand as it is stored
     * @param constant the right operand
     * @return SQL comparing the value with the constant in the type of the value, or null
     */
    private static SqlCode translateWithConstant(ComparisonOperator operator, Column value, BigDecimal constant)
    {
        SqlType type = value.getType();

        if(getIntegerBits(type) == 0)
            return null;

        // the integers the constant lies between; they are the same when the constant is an integer
        BigInteger floor = constant.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
        BigInteger ceiling = constant.setScale(0, RoundingMode.CEILING).toBigIntegerExact();

        return translateWithLimits(operator, value, getLowerLimit(type, ceiling), getUpperLimit(type, floor),
                floor.equals(ceiling), false);
    }


    /**
     * SQL of a numeric comparison of a floating-point constant with an operand whose values are stored in another type
     * of SQL than the datatype the two are compared in takes (see {@link #determineNumericComparisonType}): an integer
     * or a decimal compared with an xsd:float or an xsd:double, an xsd:float compared with an xsd:double. Null when the
     * comparison is no such one. As with an exact constant (see
     * {@link #translateWithConstant(ComparisonOperator, Set, Set, SqlExpressionIntercode, SqlExpressionIntercode)}),
     * promoting the operand wraps its column in a conversion function, which hides the column from the indexes and the
     * statistics of the database, whereas the constant can be replaced by the values of the stored type it lies between
     * (see {@link #translateWithFloatingConstant(ComparisonOperator, Column, double, boolean)}).
     *
     * @param operator the operator
     * @param lset numeric classes of the left operand
     * @param rset numeric classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison in the type the operand that is no constant is stored in, or null
     */
    private static SqlCode translateWithFloatingConstant(ComparisonOperator operator, Set<ResourceClass> lset,
            Set<ResourceClass> rset, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        boolean single = determineNumericComparisonType(lset, rset).equals(xsdFloat);
        Column lvalue = left.getBinding().getStoredNumeric(lset);
        Column rvalue = right.getBinding().getStoredNumeric(rset);

        if(lvalue == null || rvalue == null)
            return null;

        Double lconstant = getFloatingConstant(left, lset, lvalue);
        Double rconstant = getFloatingConstant(right, rset, rvalue);

        if(rconstant != null && lconstant == null)
            return translateWithFloatingConstant(operator, lvalue, rconstant, single);

        if(lconstant != null && rconstant == null)
            return translateWithFloatingConstant(operator.getSwapped(), rvalue, lconstant, single);

        return null;
    }


    /**
     * SQL comparing a stored value with a floating-point constant on its right side in the type of the value, or null
     * when the value is stored in the type the comparison takes (or in a type that is not handled). The comparison
     * converts the value to the floating-point datatype, and the conversion never reverses the order of two values, so
     * the values not less than the constant, and those not greater than it, are the values from, or up to, a limit that
     * the constant sets in the stored type:
     * <ul>
     * <li>an xsd:float is an xsd:double as it is, so the limits are the values of xsd:float nearest to the constant
     * from both sides, the same value when the constant is one of them;
     * <li>an integer or a decimal is rounded to the nearest value of the datatype, to the one whose last bit is zero
     * when it lies in the middle of two (the conversions of the extension are those of the C compiler and of
     * {@code strtod}), so the values rounded to the constant are those between its middles with its neighbours (see
     * {@link Rounded}): a comparison of an xsd:decimal with {@code 1.0e-1} holds for every decimal that is rounded to
     * that double, not for {@code 0.1} alone, and beyond the integers a datatype holds exactly, several integers are
     * equal to one constant.
     * </ul>
     * NaN is comparable with no value.
     *
     * @param operator the operator
     * @param value the left operand as it is stored
     * @param constant the right operand, a value of xsd:float when the comparison takes that datatype
     * @param single true when the operands are compared as values of xsd:float, false for xsd:double
     * @return SQL comparing the value with the constant in the type of the value, or null
     */
    private static SqlCode translateWithFloatingConstant(ComparisonOperator operator, Column value, double constant,
            boolean single)
    {
        SqlType type = value.getType();
        boolean integer = getIntegerBits(type) != 0;
        boolean decimal = type.equals(NUMERIC);

        if(!integer && !decimal && !(type.equals(FLOAT4) && !single))
            return null;

        if(Double.isNaN(constant))
            return decide(value, operator == NOT_EQUAL);

        if(!integer && !decimal)
        {
            float nearest = (float) constant;
            float least = nearest >= constant ? nearest : Math.nextUp(nearest);
            float greatest = nearest <= constant ? nearest : Math.nextDown(nearest);

            return translateWithLimits(operator, value, new Limit(getFloatConstant(least), true, false),
                    new Limit(getFloatConstant(greatest), true, false), least == greatest, true);
        }

        if(Double.isInfinite(constant))
        {
            // no integer type reaches the numbers rounded to an infinity, which start in the middle of the greatest
            // finite value and the value that would follow it
            BigDecimal start = Rounded.to(single ? Float.MAX_VALUE : Double.MAX_VALUE, single).upper();
            Limit reached = integer ? Limit.NONE :
                    new Limit(getDecimalConstant(constant > 0 ? start : start.negate()), true, false);

            return constant > 0 ? translateWithLimits(operator, value, reached, Limit.EVERY, decimal, false) :
                    translateWithLimits(operator, value, Limit.EVERY, reached, decimal, false);
        }

        Rounded rounded = Rounded.to(constant, single);

        if(decimal)
            return translateWithLimits(operator, value,
                    new Limit(getDecimalConstant(rounded.lower()), rounded.inclusive(), false),
                    new Limit(getDecimalConstant(rounded.upper()), rounded.inclusive(), false), true, false);

        // the least and the greatest integer rounded to the constant
        BigInteger floor = rounded.lower().setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
        BigInteger ceiling = rounded.upper().setScale(0, RoundingMode.CEILING).toBigIntegerExact();
        BigInteger least = rounded.inclusive() ? rounded.lower().setScale(0, RoundingMode.CEILING).toBigIntegerExact() :
                floor.add(BigInteger.ONE);
        BigInteger greatest = rounded.inclusive() ?
                rounded.upper().setScale(0, RoundingMode.FLOOR).toBigIntegerExact() : ceiling.subtract(BigInteger.ONE);

        return translateWithLimits(operator, value, getLowerLimit(type, least), getUpperLimit(type, greatest),
                least.compareTo(greatest) <= 0, false);
    }


    /**
     * The numbers that a conversion to a floating-point datatype rounds to one value of the datatype: those between the
     * middle of the value and its predecessor and the middle of the value and its successor, the middles themselves
     * when the last bit of the value is zero, as a number in the middle of two values is rounded to the even one. Past
     * the greatest finite value, the neighbour is taken as if the values went on at the same distance: the numbers from
     * that middle on are rounded to the infinity.
     *
     * @param lower the middle of the value and its predecessor
     * @param upper the middle of the value and its successor
     * @param inclusive whether the middles are rounded to the value
     */
    private record Rounded(BigDecimal lower, BigDecimal upper, boolean inclusive)
    {
        /**
         * The numbers rounded to the given finite value.
         *
         * @param value the value, a value of xsd:float when the datatype is xsd:float
         * @param single true for the values of xsd:float, false for those of xsd:double
         * @return the numbers that the conversion to the datatype rounds to the value
         */
        private static Rounded to(double value, boolean single)
        {
            BigDecimal exact = new BigDecimal(value);
            BigDecimal ulp = new BigDecimal(single ? Math.ulp((float) value) : Math.ulp(value));
            double below = single ? Math.nextDown((float) value) : Math.nextDown(value);
            double above = single ? Math.nextUp((float) value) : Math.nextUp(value);

            BigDecimal down = Double.isInfinite(below) ? ulp : exact.subtract(new BigDecimal(below));
            BigDecimal up = Double.isInfinite(above) ? ulp : new BigDecimal(above).subtract(exact);
            long bits = single ? Float.floatToIntBits((float) value) : Double.doubleToLongBits(value);

            return new Rounded(exact.subtract(down.divide(BigDecimal.TWO)), exact.add(up.divide(BigDecimal.TWO)),
                    (bits & 1) == 0);
        }
    }


    /**
     * A limit that a constant sets to the values of a stored type it is compared with: the values not less than the
     * constant are those from their limit up, and the values not greater than it are those up to their limit, the limit
     * itself being one of them when it is inclusive. When there is no limit, either every value of the type is among
     * them, or none is.
     *
     * @param value the limit as a constant of the stored type, or null when there is none
     * @param inclusive whether the limit itself is one of the values
     * @param every whether every value of the type is one of the values when there is no limit
     */
    private record Limit(Column value, boolean inclusive, boolean every)
    {
        /**
         * No limit, every value of the type being one of the values.
         */
        private static final Limit EVERY = new Limit(null, false, true);

        /**
         * No limit, no value of the type being one of the values.
         */
        private static final Limit NONE = new Limit(null, false, false);
    }


    /**
     * The limit of the values of an integer type of SQL that are not less than the given integer: the integer itself,
     * unless it lies beyond the range of the type.
     *
     * @param type the integer type of SQL
     * @param least the least integer the values are not less than
     * @return the limit of the values of the type that are not less than the integer
     */
    private static Limit getLowerLimit(SqlType type, BigInteger least)
    {
        BigInteger max = BigInteger.ONE.shiftLeft(getIntegerBits(type) - 1).subtract(BigInteger.ONE);

        if(least.compareTo(max) > 0)
            return Limit.NONE;

        if(least.compareTo(max.not()) < 0)
            return Limit.EVERY;

        return new Limit(new ValueColumn(least.toString(), type), true, false);
    }


    /**
     * The limit of the values of an integer type of SQL that are not greater than the given integer: the integer
     * itself, unless it lies beyond the range of the type.
     *
     * @param type the integer type of SQL
     * @param greatest the greatest integer the values are not greater than
     * @return the limit of the values of the type that are not greater than the integer
     */
    private static Limit getUpperLimit(SqlType type, BigInteger greatest)
    {
        BigInteger max = BigInteger.ONE.shiftLeft(getIntegerBits(type) - 1).subtract(BigInteger.ONE);

        if(greatest.compareTo(max.not()) < 0)
            return Limit.NONE;

        if(greatest.compareTo(max) > 0)
            return Limit.EVERY;

        return new Limit(new ValueColumn(greatest.toString(), type), true, false);
    }


    /**
     * The number as a constant of the numeric type of SQL.
     *
     * @param number the number
     * @return the number as a constant of the numeric type
     */
    private static Column getDecimalConstant(BigDecimal number)
    {
        return new ValueColumn(number.stripTrailingZeros().toPlainString(), NUMERIC);
    }


    /**
     * The value of xsd:float as a constant of the type of SQL storing the datatype, written in the canonical lexical
     * form, which the database reads back as the same value.
     *
     * @param number the value
     * @return the value as a constant of the type float4
     */
    private static Column getFloatConstant(float number)
    {
        String form = Float.isInfinite(number) ? number > 0 ? "INF" : "-INF" : RyuFloat.floatToString(number);

        return new ValueColumn(form, FLOAT4);
    }


    /**
     * SQL comparing a stored value with a constant on its right side, given the limits the constant sets to the values
     * of the stored type (see {@link Limit}): a value is less than the constant when it is not among the values that
     * are not less than it, and equal to it when it is neither less nor greater. The limits are constants of the stored
     * type, so the database looks the rows up by an index of the column and estimates them by its statistics. A
     * comparison no limit decides has the same result for every value.
     *
     * @param operator the operator
     * @param value the left operand as it is stored
     * @param lower the limit of the values that are not less than the constant
     * @param upper the limit of the values that are not greater than the constant
     * @param equal whether a value of the stored type can be equal to the constant
     * @param floating whether the value is a floating-point number, compared by the operators of the extension
     * @return SQL comparing the value with the constant
     */
    private static SqlCode translateWithLimits(ComparisonOperator operator, Column value, Limit lower, Limit upper,
            boolean equal, boolean floating)
    {
        return switch(operator)
        {
            case GREATER_THAN_OR_EQUAL -> translateLower(value, lower, false, floating);
            case LESS_THAN -> translateLower(value, lower, true, floating);
            case LESS_THAN_OR_EQUAL -> translateUpper(value, upper, false, floating);
            case GREATER_THAN -> translateUpper(value, upper, true, floating);

            case EQUAL, NOT_EQUAL ->
            {
                boolean negated = operator == NOT_EQUAL;

                if(!equal || lower.equals(Limit.NONE) || upper.equals(Limit.NONE))
                    yield decide(value, negated);

                if(lower.value() == null)
                    yield translateUpper(value, upper, negated, floating);

                if(upper.value() == null)
                    yield translateLower(value, lower, negated, floating);

                if(lower.inclusive() && upper.inclusive() && lower.value().equals(upper.value()))
                    yield compare(value, operator, lower.value(), floating);

                // a floating-point value has one limit, or is equal to no constant; the complements would not hold
                // for NaN
                assert !floating;

                yield SqlCode.of("(", translateLower(value, lower, negated, false), negated ? " OR " : " AND ",
                        translateUpper(value, upper, negated, false), ")");
            }
        };
    }


    /**
     * SQL testing that a stored value is not less than a constant, or that it is less than it, by the limit of the
     * values that are not less than the constant.
     *
     * @param value the value as it is stored
     * @param lower the limit of the values that are not less than the constant
     * @param negated true to test that the value is less than the constant
     * @param floating whether the value is a floating-point number, compared by the operators of the extension
     * @return SQL of the test
     */
    private static SqlCode translateLower(Column value, Limit lower, boolean negated, boolean floating)
    {
        if(lower.value() == null)
            return decide(value, lower.every() != negated);

        if(negated)
            return compare(value, lower.inclusive() ? LESS_THAN : LESS_THAN_OR_EQUAL, lower.value(), floating);

        return compare(value, lower.inclusive() ? GREATER_THAN_OR_EQUAL : GREATER_THAN, lower.value(), floating);
    }


    /**
     * SQL testing that a stored value is not greater than a constant, or that it is greater than it, by the limit of
     * the values that are not greater than the constant.
     *
     * @param value the value as it is stored
     * @param upper the limit of the values that are not greater than the constant
     * @param negated true to test that the value is greater than the constant
     * @param floating whether the value is a floating-point number, compared by the operators of the extension
     * @return SQL of the test
     */
    private static SqlCode translateUpper(Column value, Limit upper, boolean negated, boolean floating)
    {
        if(upper.value() == null)
            return decide(value, upper.every() != negated);

        if(negated)
            return compare(value, upper.inclusive() ? GREATER_THAN : GREATER_THAN_OR_EQUAL, upper.value(), floating);

        return compare(value, upper.inclusive() ? LESS_THAN_OR_EQUAL : LESS_THAN, upper.value(), floating);
    }


    /**
     * SQL comparing a stored value with a limit of its type: by the operator of SQL, or, for floating-point numbers, by
     * the operator of the extension, for which NaN is comparable with nothing.
     *
     * @param value the value as it is stored
     * @param operator the operator
     * @param limit the limit, a constant of the type of the value
     * @param floating whether the value is a floating-point number
     * @return SQL comparing the value with the limit
     */
    private static SqlCode compare(Column value, ComparisonOperator operator, Column limit, boolean floating)
    {
        if(floating)
            return SqlCode.of("(", value, " operator(sparql.", operator.getText(), ") ", limit, ")");

        return SqlCode.of("(", value, " ", operator.getText(), " ", limit, ")");
    }


    /**
     * SQL of a comparison that has the same result for every value of an operand: the result is decided for the values
     * the operand has in the classes compared only, as the other variants of the comparison cover the other classes.
     *
     * @param value the operand as it is stored
     * @param result the result
     * @return SQL giving the result for the values the operand has
     */
    private static SqlCode decide(Column value, boolean result)
    {
        return SqlCode.of("CASE WHEN ", value, " IS NOT NULL THEN ", Boolean.toString(result), " END");
    }


    /**
     * The value of an operand that is the same exact number in every solution: the operand is never unbound, it takes
     * the given exact numeric classes only and its value in them is a constant. Null for any other operand.
     *
     * @param operand the operand
     * @param classes exact numeric classes the operand is compared in
     * @param value the operand taken in the classes as it is stored
     * @return the value of an operand that is the same exact number in every solution, or null
     */
    private static BigDecimal getExactConstant(SqlExpressionIntercode operand, Set<ResourceClass> classes, Column value)
    {
        if(operand.canBeNull() || !classes.containsAll(operand.getMappings().keySet()))
            return null;

        return value instanceof ValueColumn constant ? new BigDecimal(constant.getValue()) : null;
    }


    /**
     * The value of an operand that is the same floating-point number in every solution: the operand is never unbound,
     * it takes the given classes only and its value in them is a constant of xsd:float or of xsd:double. Null for any
     * other operand.
     *
     * @param operand the operand
     * @param classes numeric classes the operand is compared in
     * @param value the operand taken in the classes as it is stored
     * @return the value of an operand that is the same floating-point number in every solution, or null
     */
    private static Double getFloatingConstant(SqlExpressionIntercode operand, Set<ResourceClass> classes, Column value)
    {
        if(operand.canBeNull() || !classes.containsAll(operand.getMappings().keySet())
                || !(value instanceof ValueColumn constant))
            return null;

        boolean single = value.getType().equals(FLOAT4);

        if(!single && !value.getType().equals(FLOAT8))
            return null;

        // the constant is written in the canonical lexical form of its datatype
        return switch(constant.getValue())
        {
            case "INF" -> Double.POSITIVE_INFINITY;
            case "-INF" -> Double.NEGATIVE_INFINITY;
            default -> single ? Float.parseFloat(constant.getValue()) : Double.parseDouble(constant.getValue());
        };
    }


    /**
     * Numeric class in which the operands are compared: the least datatype every class of the operands is derived from
     * or promoted to, so that taking the operands in it is the function conversion of XPath (see
     * {@link cz.iocb.sparql.engine.mapping.datatypes.Datatype#isPromotableTo}); xsd:short, xsd:int and xsd:long keep
     * the comparison of their own and their derived datatypes in their SQL types. A class of an operand may span
     * several datatypes (the value of an IF whose branches are an integer and a double), so the datatypes are taken
     * from the primitive classes the classes are built of.
     *
     * @param lset classes of the left operand
     * @param rset classes of the right operand
     * @return numeric class in which the operands are compared
     */
    private static ResourceClass determineNumericComparisonType(Set<ResourceClass> lset, Set<ResourceClass> rset)
    {
        Set<PrimitiveResourceClass> all = estimateAsUnion(Stream.concat(lset.stream(), rset.stream()).collect(toSet()));

        if(all.stream().anyMatch(c -> isDouble(c)))
            return xsdDouble;

        if(all.stream().anyMatch(c -> isFloat(c)))
            return xsdFloat;

        if(all.stream().anyMatch(c -> isDecimal(c)))
            return xsdDecimal;

        if(all.stream().allMatch(c -> isDerivedFromShort(c)))
            return xsdShort;

        if(all.stream().allMatch(c -> isDerivedFromInt(c)))
            return xsdInt;

        if(all.stream().allMatch(c -> isDerivedFromLong(c)))
            return xsdLong;

        if(all.stream().allMatch(c -> isDerivedFromInteger(c)))
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
    public SqlExpressionIntercode optimize(Request request, Scope scope, VariableBindings bindings,
            Restriction restriction, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        Restriction leftSet = new Restriction();
        Restriction rightSet = new Restriction();

        if(restriction.contains(relations, xsdBoolean))
        {
            for(ResourceClass leftClass : left.getMappings().keySet())
            {
                for(ResourceClass rightClass : right.getMappings().keySet())
                {
                    if(areComparable(relations, operator, leftClass, rightClass) != ComparisonType.NULL)
                    {
                        leftSet.add(leftClass);
                        rightSet.add(rightClass);
                    }
                }
            }
        }

        SqlExpressionIntercode optLeft = left.optimize(request, scope, bindings, leftSet, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, scope, bindings, rightSet, evalServices);

        if(optLeft == left && optRight == right && restriction.isOptimized(relations, variableBinding))
            return this;

        return create(request, operator, optLeft, optRight, restriction);
    }


    /**
     * The operator.
     *
     * @return the operator
     */
    public ComparisonOperator getOperator()
    {
        return operator;
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
        return Objects.hash(operator.name(), left, right);
    }
}
