package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasUnsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivedFromShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
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
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass.Component;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



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
         * Equality of literals of the unsupported class: the same term is equal, anything else is an error.
         */
        LITERAL,

        /**
         * Direct SQL comparison of the columns (strings, references, literals of one handled datatype).
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
     * Number of argument class combinations above which the operands are compared as boxes by a single expression
     * instead of combination by combination.
     */
    private static final int boxComparisonLimit = 256;

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
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @return comparison of the operands
     */
    public static SqlExpressionIntercode create(ClassRelations relations, ComparisonOperator operator,
            SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        return create(relations, operator, left, right, Restriction.ALL);
    }


    /**
     * Comparison materialising only the needed result; constant when the classes decide it.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param restriction the result classes the parent needs
     * @return comparison materialising only the needed result; constant when the classes decide it
     */
    private static SqlExpressionIntercode create(ClassRelations relations, ComparisonOperator operator,
            SqlExpressionIntercode left, SqlExpressionIntercode right, Restriction restriction)
    {
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

        if(set.size() > boxComparisonLimit)
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
                        e.getValue() == null ? null : translate(relations, operator, e.getValue(), left, right));
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
        else if(hasBoolean(left) && hasBoolean(right))
            return ComparisonMode.BOX;
        else if(hasString(left) && hasString(right))
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
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param variants combinations of argument classes
     * @param left the left operand
     * @param right the right operand
     * @return SQL of the comparison as a COALESCE over the argument class combinations
     */
    private static List<Column> translate(ClassRelations relations, ComparisonOperator operator,
            Set<List<Set<ResourceClass>>> variants, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        if(variants == null)
            return null;

        return List.of(Column.coalesce(variants.stream()
                .map(v -> new ExpressionColumn(translate(relations, operator, v.get(0), v.get(1), left, right), BOOL))
                .collect(toSet())));
    }


    /**
     * SQL comparing the operands taken in the given classes, according to their comparison mode.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operator the operator
     * @param lset classes of the left operand
     * @param rset classes of the right operand
     * @param left the left operand
     * @param right the right operand
     * @return SQL comparing the operands taken in the given classes, according to their comparison mode
     */
    private static SqlCode translate(ClassRelations relations, ComparisonOperator operator, Set<ResourceClass> lset,
            Set<ResourceClass> rset, SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
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
                ResourceClass cmp = determineNumericComparisonType(lset, rset);
                Column cl = left.promoteNumericAs(lset, cmp);
                Column cr = right.promoteNumericAs(rset, cmp);
                yield SqlCode.of("(", cl, " operator(sparql.", operator.getText(), ") ", cr, ")");
            }

            case DATETIME ->
            {
                //FIXME: we assume that genScalarDateTime is equivalent to genDateTime
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
                    SqlExpressionIntercode comparison = create(relations, EQUAL, l, r);

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

                //FIXME: use correct compare operator, when unionClass is rdfbox

                List<Column> cl = left.get(relations, cmp);
                List<Column> cr = right.get(relations, cmp);

                if(operator == EQUAL)
                    yield getIdentityConditions(cmp, cl, cr).stream().collect(SqlCode.joining(" AND ", "(", ")"));
                else if(operator == NOT_EQUAL && cmp.getColumnCount() > 1)
                    yield getNonIdentityConditions(cmp, cl, cr).stream().collect(SqlCode.joining(" OR ", "(", ")"));

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

                //FIXME: use correct compare operator, when unionClass is rdfbox or user type

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
        int bits = getIntegerBits(type);

        if(bits == 0)
            return null;

        BigInteger max = BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE);
        BigInteger min = max.not();

        // the integers the constant lies between; they are the same when the constant is an integer
        BigInteger floor = constant.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
        BigInteger ceiling = constant.setScale(0, RoundingMode.CEILING).toBigIntegerExact();

        // the integer the value is compared with instead of the constant
        BigInteger bound = operator == LESS_THAN || operator == GREATER_THAN_OR_EQUAL ? ceiling : floor;
        boolean equality = operator == EQUAL || operator == NOT_EQUAL;

        // the result for every value of the type when the constant is no integer of the type
        Boolean result = null;

        if(bound.compareTo(max) > 0)
            result = equality ? operator == NOT_EQUAL : operator == LESS_THAN || operator == LESS_THAN_OR_EQUAL;
        else if(bound.compareTo(min) < 0)
            result = equality ? operator == NOT_EQUAL : operator == GREATER_THAN || operator == GREATER_THAN_OR_EQUAL;
        else if(equality && !floor.equals(ceiling))
            result = operator == NOT_EQUAL;

        // decided for the values the operand has in the classes compared here only, as the other variants cover the
        // other classes
        if(result != null)
            return SqlCode.of("CASE WHEN ", value, " IS NOT NULL THEN ", result.toString(), " END");

        return SqlCode.of("(", value, " ", operator.getText(), " ", new ValueColumn(bound.toString(), type), ")");
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

        return create(relations, operator, optLeft, optRight, restriction);
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
