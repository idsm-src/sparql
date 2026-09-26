package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numericBaseClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.toCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.imcode.SqlBaseClass;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.BooleanBaseClass;
import cz.iocb.sparql.engine.mapping.classes.BooleanClass;
import cz.iocb.sparql.engine.mapping.classes.ByteBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ByteClass;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateScalarClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeScalarClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleClass;
import cz.iocb.sparql.engine.mapping.classes.FloatBaseClass;
import cz.iocb.sparql.engine.mapping.classes.FloatClass;
import cz.iocb.sparql.engine.mapping.classes.IntBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerClass;
import cz.iocb.sparql.engine.mapping.classes.LongBaseClass;
import cz.iocb.sparql.engine.mapping.classes.LongClass;
import cz.iocb.sparql.engine.mapping.classes.NegativeIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NegativeIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.NonNegativeIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NonNegativeIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.NonPositiveIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NonPositiveIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.PositiveIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.PositiveIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ShortBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ShortClass;
import cz.iocb.sparql.engine.mapping.classes.StringClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedByteBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedByteClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedIntBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedIntClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedLongBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedLongClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedShortBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedShortClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Node of an expression in the intermediate code. Like a variable, an expression has a set of possible resource classes
 * with the SQL columns computing its value in each ({@link #getMappings}), a nullability (an error in SPARQL is NULL in
 * SQL) and a determinism flag. Nodes implement {@link #optimize}, which rebuilds the expression over the optimised
 * bindings of its variables and materialises only the classes the parent needs.
 */
public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    /**
     * Which result classes of an expression the parent needs; classes outside are still known but their columns are not
     * materialised (null).
     */
    public static class Restriction
    {
        /**
         * All classes are needed.
         */
        public static final Restriction ALL = new Restriction(box);

        /**
         * All classes are needed (same as {@link #ALL}).
         */
        public static final Restriction NONE = new Restriction(box);


        /**
         * The needed classes.
         */
        Set<ResourceClass> set = new HashSet<>();

        /**
         * Creates the restriction to the classes.
         *
         * @param classes the needed classes
         */
        public Restriction(Set<ResourceClass> classes)
        {
            add(classes);
        }


        /**
         * Creates the restriction to the classes.
         *
         * @param classes the needed classes
         */
        public Restriction(ResourceClass... classes)
        {
            add(Arrays.asList(classes));
        }


        /**
         * Adds needed classes.
         *
         * @param classes the needed classes
         */
        public void add(Collection<ResourceClass> classes)
        {
            if(classes != null)
                set.addAll(classes);
        }


        /**
         * Adds needed classes.
         *
         * @param classes the needed classes
         */
        public void add(ResourceClass... classes)
        {
            add(Arrays.asList(classes));
        }


        /**
         * True if some needed class overlaps the given one.
         *
         * @param resClass the resource class
         * @return true if some needed class overlaps the given one, false otherwise
         */
        public boolean contains(ResourceClass resClass)
        {
            return set.stream().anyMatch(r -> !areDisjunct(r, resClass));
        }


        /**
         * True if some needed class overlaps one of the given ones.
         *
         * @param resClasses the resource classes
         * @return true if some needed class overlaps one of the given ones, false otherwise
         */
        public boolean contains(Set<ResourceClass> resClasses)
        {
            return resClasses.stream().anyMatch(r -> contains(r));
        }


        /**
         * True if the binding materialises exactly the needed classes.
         *
         * @param binding the variable binding
         * @return true if the binding materialises exactly the needed classes, false otherwise
         */
        public boolean isOptimized(VariableBinding binding)
        {
            return restrict(binding.getMappings()).equals(binding.getMappings());
        }


        /**
         * The mappings with the columns of unneeded classes set to null.
         *
         * @param mappings columns per resource class
         * @return the mappings with the columns of unneeded classes set to null
         */
        public Map<ResourceClass, List<Column>> restrict(Map<ResourceClass, List<Column>> mappings)
        {
            Map<ResourceClass, List<Column>> result = new HashMap<>();

            for(Entry<ResourceClass, List<Column>> e : mappings.entrySet())
                result.put(e.getKey(), contains(e.getKey()) ? e.getValue() : null);

            return result;
        }
    }


    /**
     * Whether repeated evaluation gives the same value.
     */
    protected final boolean isDeterministic;

    /**
     * Variables the expression refers to.
     */
    protected final Set<Variable> referencedVariables = new HashSet<>();

    /**
     * Classes, columns and nullability of the value.
     */
    protected final VariableBinding variableBinding;


    /**
     * Creates the expression.
     *
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the node is deterministic
     */
    protected SqlExpressionIntercode(Map<ResourceClass, List<Column>> mappings, boolean canBeNull,
            boolean isDeterministic)
    {
        this.isDeterministic = isDeterministic;
        this.variableBinding = new VariableBinding(mappings, canBeNull);
    }


    /**
     * Variables (with classes) the expression needs from its bindings.
     *
     * @return variables (with classes) the expression needs from its bindings
     */
    public abstract Restrictions getRequirements();


    /**
     * Rebuilds the expression over the given (optimised) variable bindings, materialising only the classes in
     * {@code restriction}; with {@code evalServices}, patterns nested in EXISTS evaluate their SERVICE stubs.
     *
     * @param request the current request
     * @param bindings the variable bindings
     * @param restriction the result classes the parent needs
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the resulting expression
     */
    public abstract SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices);


    /**
     * Appends a human-readable form; {@code priority} is the precedence of the enclosing operator, deciding whether
     * parentheses are needed.
     *
     * @param builder the builder to append to
     * @param indent the current indentation of the explanation
     * @param priority precedence of the enclosing operator
     */
    protected abstract void generateExplanation(StringBuilder builder, String indent, int priority);


    /**
     * Appends a human-readable form of the expression.
     *
     * @param builder the builder to append to
     * @param indent the current indentation of the explanation
     */
    public void generateExplanation(StringBuilder builder, String indent)
    {
        generateExplanation(builder, indent, 10);
    }


    /**
     * True if some class overlapping the given ones is computed by an SQL expression rather than a plain column or
     * constant.
     *
     * @param resClasses the resource classes
     * @return true if some class overlapping the given ones is computed by an SQL expression rather than a plain column
     *         or constant, false otherwise
     */
    public boolean hasExpressionColumn(Set<ResourceClass> resClasses)
    {
        return resClasses.stream().anyMatch(r -> hasExpressionColumn(r));
    }


    /**
     * True if some class overlapping the given one is computed by an SQL expression rather than a plain column or
     * constant, in which case its evaluation should not be duplicated.
     *
     * @param resClass the resource class
     * @return true if some class overlapping the given one is computed by an SQL expression rather than a plain column
     *         or constant, in which case its evaluation should not be duplicated, false otherwise
     */
    public boolean hasExpressionColumn(ResourceClass resClass)
    {
        for(Entry<ResourceClass, List<Column>> e : variableBinding.getMappings().entrySet())
            if(!areDisjunct(resClass, e.getKey()) && e.getValue().stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

        return false;
    }


    /**
     * True if the value in {@code sourceClass} can be converted to {@code targetClass} without evaluating an SQL
     * expression more than once.
     *
     * @param sourceClass the class to convert from
     * @param targetClass the class to convert to
     * @return true if the value in {@code sourceClass} can be converted to {@code targetClass} without evaluating an
     *         SQL expression more than once, false otherwise
     */
    public boolean canSafelyGeneralize(ResourceClass sourceClass, ResourceClass targetClass)
    {
        return sourceClass.getEffectiveClass().equals(targetClass) || sourceClass.isSubclassOf(targetClass)
                && (!hasExpressionColumn(sourceClass) || targetClass.getColumnCount() == 1);
    }


    /**
     * Result class of a unary arithmetic operation: integer, decimal, float or double according to the operand.
     *
     * @param operandClass class of the operand
     * @return result class of a unary arithmetic operation: integer, decimal, float or double according to the operand
     */
    public static ResourceClass determineResultClass(ResourceClass operandClass)
    {
        if(isDouble(operandClass))
            return xsdDouble;
        else if(isFloat(operandClass))
            return xsdFloat;
        else if(isDecimal(operandClass))
            return xsdDecimal;
        else
            return xsdInteger;
    }


    /**
     * Result class of a binary arithmetic operation by numeric type promotion of the operands.
     *
     * @param leftClass class of the left operand
     * @param rightClass class of the right operand
     * @return result class of a binary arithmetic operation by numeric type promotion of the operands
     */
    public static ResourceClass determineResultClass(ResourceClass leftClass, ResourceClass rightClass)
    {
        if(isDouble(leftClass) || isDouble(rightClass))
            return xsdDouble;
        else if(isFloat(leftClass) || isFloat(rightClass))
            return xsdFloat;
        else if(isDecimal(leftClass) || isDecimal(rightClass))
            return xsdDecimal;
        else
            return xsdInteger;
    }


    /**
     * Numeric base classes overlapping the class.
     *
     * @param resClass the resource class
     * @return numeric base classes overlapping the class
     */
    public static Set<ResourceClass> getNumericClasses(ResourceClass resClass)
    {
        return numericBaseClasses.stream().filter(r -> !areDisjunct(r, resClass)).collect(toCollection(HashSet::new));
    }


    /**
     * Name of the (effective) class as used in the {@code sparql.*} function names of the pgsparql extension.
     *
     * @param resClass the resource class
     * @return name of the (effective) class as used in the {@code sparql.*} function names of the pgsparql extension
     */
    public static String getLiteralClassName(ResourceClass resClass)
    {
        return switch(resClass.getEffectiveClass())
        {
            case BooleanBaseClass _ -> "boolean";
            case BooleanClass _ -> "boolean";
            case ByteBaseClass _ -> "byte";
            case ByteClass _ -> "byte";
            case UnsignedByteBaseClass _ -> "unsignedbyte";
            case UnsignedByteClass _ -> "unsignedbyte";
            case ShortBaseClass _ -> "short";
            case ShortClass _ -> "short";
            case UnsignedShortBaseClass _ -> "unsignedshort";
            case UnsignedShortClass _ -> "unsignedshort";
            case IntBaseClass _ -> "int";
            case IntClass _ -> "int";
            case UnsignedIntBaseClass _ -> "unsignedint";
            case UnsignedIntClass _ -> "unsignedint";
            case LongBaseClass _ -> "long";
            case LongClass _ -> "long";
            case UnsignedLongBaseClass _ -> "unsignedlong";
            case UnsignedLongClass _ -> "unsignedlong";
            case IntegerBaseClass _ -> "integer";
            case IntegerClass _ -> "integer";
            case NonPositiveIntegerBaseClass _ -> "nonpositiveinteger";
            case NonPositiveIntegerClass _ -> "nonpositiveinteger";
            case NegativeIntegerBaseClass _ -> "negativeinteger";
            case NegativeIntegerClass _ -> "negativeinteger";
            case NonNegativeIntegerBaseClass _ -> "nonnegativeinteger";
            case NonNegativeIntegerClass _ -> "nonnegativeinteger";
            case PositiveIntegerBaseClass _ -> "positiveinteger";
            case PositiveIntegerClass _ -> "positiveinteger";
            case DecimalBaseClass _ -> "decimal";
            case DecimalClass _ -> "decimal";
            case FloatBaseClass _ -> "float";
            case FloatClass _ -> "float";
            case DoubleBaseClass _ -> "double";
            case DoubleClass _ -> "double";
            case DateTimeScalarClass _ -> "datetime";
            case DateTimeCompositeClass _ -> "datetime";
            case DateScalarClass _ -> "date";
            case DateCompositeClass _ -> "date";
            case DayTimeDurationBaseClass _ -> "daytimeduration";
            case DayTimeDurationClass _ -> "daytimeduration";
            case StringClass _ -> "string";

            default -> throw new IllegalArgumentException();
        };
    }


    /**
     * Groups the possible results of an operation given as a map from result class to the argument class combinations
     * producing it: combinations that would evaluate the same argument expression twice are merged, overlapping result
     * classes are unioned (stored in {@code unionClass} when given), and results outside the restriction get null
     * combinations.
     *
     * @param arguments the arguments
     * @param map argument class combinations per result class
     * @param restriction the result classes the parent needs
     * @param unionClass class storing the union of overlapping results, or null
     * @return grouped argument class combinations per result class, null where the result is not needed
     */
    protected static Map<ResourceClass, Set<List<Set<ResourceClass>>>> processResultMap(
            List<SqlExpressionIntercode> arguments, Map<ResourceClass, Set<List<ResourceClass>>> map,
            Restriction restriction, PrimitiveResourceClass unionClass)
    {
        record Input(ResourceClass result, List<ResourceClass> params)
        {
            public static boolean areInConflict(List<SqlExpressionIntercode> arguments, Restriction restriction,
                    Input l, Input r)
            {
                if(!restriction.contains(l.result) || !restriction.contains(r.result))
                    return false;

                for(int i = 0; i < arguments.size(); i++)
                {
                    SqlExpressionIntercode arg = arguments.get(i);
                    ResourceClass lc = l.params.get(i);
                    ResourceClass rc = l.params.get(i);

                    if(!areDisjunct(lc, rc) && (arg.hasExpressionColumn(lc) || arg.hasExpressionColumn(lc)))
                        return true;
                }

                return false;
            }
        }

        Set<Input> inputs = new HashSet<>();

        for(Entry<ResourceClass, Set<List<ResourceClass>>> e : map.entrySet())
            for(List<ResourceClass> c : e.getValue())
                inputs.add(new Input(e.getKey(), c));

        Collection<Set<Input>> groupParameters = UnionFind.getDisjunctEntries(inputs,
                (l, r) -> Input.areInConflict(arguments, restriction, l, r));


        record Midle(Set<ResourceClass> result, List<Set<ResourceClass>> params)
        {
        }

        Set<Midle> middles = new HashSet<>();

        for(Set<Input> s : groupParameters)
        {
            Set<ResourceClass> result = new HashSet<>();
            List<Set<ResourceClass>> params = new ArrayList<>();

            for(int i = 0; i < arguments.size(); i++)
                params.add(new HashSet<>());

            for(Input e : s)
            {
                result.add(e.result);

                for(int i = 0; i < arguments.size(); i++)
                    params.get(i).add(e.params.get(i));
            }

            middles.add(new Midle(result, params));
        }


        Collection<Set<Midle>> groupResults = UnionFind.getDisjunctEntries(middles,
                (l, r) -> !areDisjunct(l.result, r.result) && restriction.contains(l.result)
                        && restriction.contains(r.result));

        Map<ResourceClass, Set<List<Set<ResourceClass>>>> output = new HashMap<>();

        for(Set<Midle> s : groupResults)
        {
            Set<ResourceClass> result = new HashSet<>();
            Set<List<Set<ResourceClass>>> params = new HashSet<>();

            for(Midle e : s)
            {
                result.addAll(e.result);
                params.add(e.params);
            }

            if(!restriction.contains(result))
                params = null;

            if(result.size() == 1)
                output.put(result.iterator().next(), params);
            else if(unionClass != null)
                output.put(unionize(result, unionClass), params);
            else
                output.put(unionize(result), params);
        }

        return output;
    }


    /**
     * Groups the possible results without a fixed union class, see
     * {@link #processResultMap(List, Map, Restriction, PrimitiveResourceClass)}.
     *
     * @param arguments the arguments
     * @param map argument class combinations per result class
     * @param restriction the result classes the parent needs
     * @return grouped argument class combinations per result class, null where the result is not needed
     */
    protected static Map<ResourceClass, Set<List<Set<ResourceClass>>>> processResultMap(
            List<SqlExpressionIntercode> arguments, Map<ResourceClass, Set<List<ResourceClass>>> map,
            Restriction restriction)
    {
        return processResultMap(arguments, map, restriction, null);
    }


    /**
     * True if the column is a NULL constant.
     *
     * @param column the column
     * @return true if the column is a NULL constant, false otherwise
     */
    private static boolean isNullConstant(Column column)
    {
        return column instanceof NullColumn;
    }


    /**
     * SQL conditions, one per position, that two column tuples of the class represent the same term: the identity of
     * the type at the determining positions, its null-safe variant (or {@code IS NULL} against a NULL constant) at the
     * optional ones. Their conjunction is the identity of the terms.
     *
     * @param resClass the resource class of the columns
     * @param left the columns of the left term
     * @param right the columns of the right term
     * @return SQL conditions, one per position, that two column tuples of the class represent the same term
     */
    public static List<String> getIdentityConditions(ResourceClass resClass, List<Column> left, List<Column> right)
    {
        List<String> conditions = new ArrayList<>(left.size());

        for(int i = 0; i < left.size(); i++)
        {
            Column l = left.get(i);
            Column r = right.get(i);
            SqlType type = resClass.getSqlTypes().get(i);

            if(!resClass.isOptionalColumn(i))
                conditions.add(type.equal(l, r));
            else if(isNullConstant(l))
                conditions.add(r.canBeNull() ? "(" + r + " IS NULL)" : "false");
            else if(isNullConstant(r))
                conditions.add(l.canBeNull() ? "(" + l + " IS NULL)" : "false");
            else if(!l.canBeNull() || !r.canBeNull())
                conditions.add(type.equal(l, r));
            else
                conditions.add(type.notDistinct(l, r));
        }

        return conditions;
    }


    /**
     * SQL conditions, one per position, that two column tuples of the class represent different terms, the negations of
     * {@link #getIdentityConditions}. Their disjunction is the non-identity of the terms.
     *
     * @param resClass the resource class of the columns
     * @param left the columns of the left term
     * @param right the columns of the right term
     * @return SQL conditions, one per position, that two column tuples of the class represent different terms
     */
    public static List<String> getNonIdentityConditions(ResourceClass resClass, List<Column> left, List<Column> right)
    {
        List<String> conditions = new ArrayList<>(left.size());

        for(int i = 0; i < left.size(); i++)
        {
            Column l = left.get(i);
            Column r = right.get(i);
            SqlType type = resClass.getSqlTypes().get(i);

            if(!resClass.isOptionalColumn(i))
                conditions.add("(" + l + " != " + r + ")");
            else if(isNullConstant(l))
                conditions.add(r.canBeNull() ? "(" + r + " IS NOT NULL)" : "true");
            else if(isNullConstant(r))
                conditions.add(l.canBeNull() ? "(" + l + " IS NOT NULL)" : "true");
            else if(!l.canBeNull() || !r.canBeNull())
                conditions.add("(" + l + " != " + r + ")");
            else
                conditions.add(type.distinct(l, r));
        }

        return conditions;
    }


    /**
     * SQL condition that the value is NULL.
     *
     * @return SQL condition that the value is NULL
     */
    public String getIsNull()
    {
        return variableBinding.getIsNull();
    }


    /**
     * SQL condition that the value is not NULL.
     *
     * @return SQL condition that the value is not NULL
     */
    public String getIsNotNull()
    {
        return variableBinding.getIsNotNull();
    }


    /**
     * SQL condition that the value has no value of the given class.
     *
     * @param resClass the resource class
     * @return SQL condition that the value has no value of the given class
     */
    public String getIsNull(ResourceClass resClass)
    {
        return variableBinding.getIsNull(resClass);
    }


    /**
     * SQL condition that the value has a value of the given class.
     *
     * @param resClass the resource class
     * @return SQL condition that the value has a value of the given class
     */
    public String getIsNotNull(ResourceClass resClass)
    {
        return variableBinding.getIsNotNull(resClass);
    }


    /**
     * Expression yielding the string value when the value is a string literal.
     *
     * @return expression yielding the string value when the value is a string literal
     */
    protected Column getStringLiteral()
    {
        return variableBinding.getStringLiteral();
    }


    /**
     * Expression yielding the string value when the value belongs to one of the string literal classes.
     *
     * @param resClasses the resource classes
     * @return expression yielding the string value when the value belongs to one of the string literal classes
     */
    protected Column getStringLiteral(Set<ResourceClass> resClasses)
    {
        return variableBinding.getStringLiteral(resClasses);
    }


    /**
     * Expression yielding the numeric value taken in {@code source} promoted to {@code target}.
     *
     * @param source the class the value is taken in
     * @param target the numeric class to promote to
     * @return expression yielding the numeric value taken in {@code source} promoted to {@code target}
     */
    protected Column promoteNumericAs(ResourceClass source, ResourceClass target)
    {
        return variableBinding.promoteNumericAs(source, target);
    }


    /**
     * COALESCE of the promotions from the source classes to {@code target}.
     *
     * @param set the source classes
     * @param target the numeric class to promote to
     * @return COALESCE of the promotions from the source classes to {@code target}
     */
    public Column promoteNumericAs(Set<ResourceClass> set, ResourceClass target)
    {
        return variableBinding.promoteNumericAs(set, target);
    }


    /**
     * Columns of the value converted to the class, see {@link VariableBinding#deriveMapping(ResourceClass)}.
     *
     * @param resClass the resource class
     * @return columns of the value converted to the class, see {@link VariableBinding#deriveMapping(ResourceClass)}
     */
    public List<Column> get(ResourceClass resClass)
    {
        return variableBinding.deriveMapping(resClass);
    }


    /**
     * Columns per class; null columns mean the class is not materialised.
     *
     * @return columns per class; null columns mean the class is not materialised
     */
    public Map<ResourceClass, List<Column>> getMappings()
    {
        return variableBinding.getMappings();
    }


    /**
     * Columns of exactly the given class, or null.
     *
     * @param resClass the resource class
     * @return columns of exactly the given class, or null
     */
    public List<Column> getMapping(ResourceClass resClass)
    {
        return variableBinding.getMapping(resClass);
    }


    /**
     * Classes the value may take.
     *
     * @return classes the value may take
     */
    public Set<ResourceClass> getResourceClasses()
    {
        return variableBinding.getClasses();
    }


    /**
     * True if the expression may evaluate to NULL (an error, or an unbound variable).
     *
     * @return true if the expression may evaluate to NULL (an error, or an unbound variable), false otherwise
     */
    public boolean canBeNull()
    {
        return variableBinding.canBeNull();
    }


    /**
     * Classes, columns and nullability of the value.
     *
     * @return classes, columns and nullability of the value
     */
    public VariableBinding getBinding()
    {
        return variableBinding;
    }


    /**
     * True if repeated evaluation gives the same value.
     *
     * @return true if repeated evaluation gives the same value, false otherwise
     */
    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    /**
     * Variables the expression refers to.
     *
     * @return variables the expression refers to
     */
    public Set<Variable> getReferencedVariables()
    {
        return referencedVariables;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlExpressionIntercode imcode))
            return false;

        if(hashCode() != imcode.hashCode())
            return false;

        if(!Objects.equals(isDeterministic, imcode.isDeterministic))
            return false;

        if(!Objects.equals(variableBinding, imcode.variableBinding))
            return false;

        return true;
    }
}
