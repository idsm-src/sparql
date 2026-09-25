package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.database.Column.coalesce;
import static cz.iocb.sparql.engine.database.Table.toTableColumns;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.getLiteralClassName;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasStringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numericBaseClasses;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * How a variable is represented in a piece of intermediate code: for each resource class it may take, the columns
 * holding the value in that class (null columns mean the class is possible but not materialised), plus whether the
 * variable can be unbound. In a solution at most one class has non-null columns.
 */
public class VariableBinding
{
    /**
     * The variable; null for the binding of an expression.
     */
    private final Variable variable;

    /**
     * Whether the variable can be unbound.
     */
    private final boolean canBeNull;

    /**
     * Columns per resource class; null columns mean the class is not materialised.
     */
    private final Map<ResourceClass, List<Column>> mappings = new HashMap<>();


    /**
     * Copy constructor.
     *
     * @param other binding of the other variable
     */
    public VariableBinding(VariableBinding other)
    {
        this.variable = other.variable;
        this.canBeNull = other.canBeNull;
        this.mappings.putAll(other.mappings);
    }


    /**
     * Creates the binding of an expression (no variable).
     *
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    public VariableBinding(Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this.variable = null;
        this.canBeNull = canBeNull;
        this.mappings.putAll(mappings);
    }


    /**
     * Creates the binding with the given mappings (may be null).
     *
     * @param variable the variable
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    public VariableBinding(Variable variable, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this.variable = variable;
        this.canBeNull = canBeNull;

        if(mappings != null)
            this.mappings.putAll(mappings);
    }


    /**
     * Creates the binding with a single class.
     *
     * @param variable the variable
     * @param resClass the resource class
     * @param columns the columns
     * @param canBeNull whether the value may be null
     */
    public VariableBinding(Variable variable, ResourceClass resClass, List<Column> columns, boolean canBeNull)
    {
        this.variable = variable;
        this.canBeNull = canBeNull;
        this.mappings.put(resClass, columns);
    }


    /**
     * Creates the binding without classes.
     *
     * @param variable the variable
     * @param canBeNull whether the value may be null
     */
    public VariableBinding(Variable variable, boolean canBeNull)
    {
        this.variable = variable;
        this.canBeNull = canBeNull;
    }


    /**
     * Adds a class with its columns; the class must be new.
     *
     * @param resClass the resource class
     * @param columns the columns
     */
    public void addMapping(ResourceClass resClass, List<Column> columns)
    {
        assert !mappings.containsKey(resClass);

        mappings.put(resClass, columns);
    }


    /**
     * True if exactly this class is among the classes of the variable.
     *
     * @param resClass the resource class
     * @return true if exactly this class is among the classes of the variable, false otherwise
     */
    public boolean containsClass(ResourceClass resClass)
    {
        return mappings.containsKey(resClass);
    }


    /**
     * True if some class of the variable overlaps with the given class.
     *
     * @param resClass the resource class
     * @return true if some class of the variable overlaps with the given class, false otherwise
     */
    public boolean contains(ResourceClass resClass)
    {
        return mappings.keySet().stream().anyMatch(r -> !ResourceClass.areDisjunct(r, resClass));
    }


    /**
     * The variable; null for the binding of an expression.
     *
     * @return the variable; null for the binding of an expression
     */
    public final Variable getVariable()
    {
        return variable;
    }


    /**
     * True if the variable can be unbound.
     *
     * @return true if the variable can be unbound, false otherwise
     */
    public final boolean canBeNull()
    {
        return canBeNull;
    }


    /**
     * Columns per resource class; null columns mean the class is not materialised.
     *
     * @return columns per resource class; null columns mean the class is not materialised
     */
    public final Map<ResourceClass, List<Column>> getMappings()
    {
        return mappings;
    }


    /**
     * Columns of exactly the given class, or null.
     *
     * @param resourceClass the resource class
     * @return columns of exactly the given class, or null
     */
    public List<Column> getMapping(ResourceClass resourceClass)
    {
        return mappings.get(resourceClass);
    }


    /**
     * Columns holding the value of the variable converted to {@code targetClass}, qualified by the table if given: the
     * stored columns when the class is present, otherwise a COALESCE over the conversions of all overlapping classes,
     * or NULL constants when no class overlaps. Returns null when a needed class has no materialised columns.
     *
     * @param targetClass the class to convert to
     * @param table the table
     * @return the derived columns, or null when a needed class has no materialised columns
     */
    public List<Column> deriveMapping(ResourceClass targetClass, Table table)
    {
        if(mappings.containsKey(targetClass))
            return toTableColumns(table, mappings.get(targetClass));

        List<List<Column>> variants = new ArrayList<>();
        boolean canBeNull = canBeNull() || mappings.size() > 1;

        for(Entry<ResourceClass, List<Column>> map : mappings.entrySet())
        {
            if(!ResourceClass.areDisjunct(targetClass, map.getKey()))
            {
                ResourceClass sourceClass = map.getKey();
                List<Column> cols = toTableColumns(table, map.getValue());

                if(cols == null)
                    return null;

                if(sourceClass.isSubclassOf(targetClass))
                    variants.add(sourceClass.toGeneralClass(targetClass, cols, canBeNull));
                else if(targetClass.isSubclassOf(sourceClass.getEffectiveClass()))
                    variants.add(targetClass.fromGeneralClass(sourceClass, cols, false));
                else
                    throw new UnsupportedOperationException();
            }
        }

        if(variants.isEmpty())
            return targetClass.getSqlTypes().stream().map(s -> (Column) new NullColumn(s)).toList();

        if(variants.size() == 1)
            return variants.get(0);

        List<Column> columns = new ArrayList<>();

        for(int i = 0; i < targetClass.getColumnCount(); i++)
        {
            List<Column> set = new ArrayList<>();

            for(List<Column> variant : variants)
                if(!set.contains(variant.get(i)))
                    set.add(variant.get(i));

            Collections.sort(set);

            if(set.size() == 1)
                columns.add(variants.get(0).get(i));
            else
                columns.add(new ExpressionColumn(
                        set.stream().sorted().map(Object::toString).collect(joining(",", "COALESCE(", ")"))));
        }

        return columns;
    }


    /**
     * Same as {@link #deriveMapping(ResourceClass, Table)} without table qualification.
     *
     * @param targetClass the class to convert to
     * @return the derived columns, or null when a needed class has no materialised columns
     */
    public List<Column> deriveMapping(ResourceClass targetClass)
    {
        return deriveMapping(targetClass, null);
    }


    /**
     * Column whose nullness decides whether a value of the class is present in the given columns: the first
     * non-constant column at a determining position (see {@link ResourceClass#isOptionalColumn}). Null when there is no
     * such column, i.e. when the presence is decided by the constants alone.
     *
     * @param resClass the resource class
     * @param columns the columns representing values of the class
     * @return the witness column, or null if the presence is decided by the constants alone
     */
    public static Column getNullWitness(ResourceClass resClass, List<Column> columns)
    {
        for(int i = 0; i < columns.size(); i++)
            if(!resClass.isOptionalColumn(i) && !(columns.get(i) instanceof ConstantColumn))
                return columns.get(i);

        return null;
    }


    /**
     * Column whose null test decides whether a value of the class is present in the given columns, or the constant
     * result of the test ({@code Boolean.TRUE} when the class is absent because a determining column is a NULL
     * constant, {@code Boolean.FALSE} when the class is present because the determining columns are constants).
     *
     * @param resClass the resource class
     * @param columns the columns representing values of the class
     * @return the witness column, or the constant result of the null test as a {@link Boolean}
     */
    private static Object getNullTest(ResourceClass resClass, List<Column> columns)
    {
        for(int i = 0; i < columns.size(); i++)
            if(!resClass.isOptionalColumn(i) && columns.get(i) instanceof NullColumn)
                return Boolean.TRUE;

        Column witness = getNullWitness(resClass, columns);

        return witness == null ? Boolean.FALSE : witness;
    }


    /**
     * SQL condition that no value of the class is present in the given columns: {@code true} or {@code false} when the
     * determining columns are constants, the null test of the witness column otherwise.
     *
     * @param resClass the resource class
     * @param columns the columns representing values of the class
     * @param table the table the columns are accessed through, or null
     * @return SQL condition that no value of the class is present in the given columns
     */
    private static String getIsNull(ResourceClass resClass, List<Column> columns, Table table)
    {
        return switch(getNullTest(resClass, columns))
        {
            case Boolean b -> b.toString();
            case Column witness -> witness.fromTable(table) + " IS NULL";
            default -> throw new IllegalStateException();
        };
    }


    /**
     * SQL condition that a value of the class is present in the given columns, the negation of
     * {@link #getIsNull(ResourceClass, List, Table)}.
     *
     * @param resClass the resource class
     * @param columns the columns representing values of the class
     * @param table the table the columns are accessed through, or null
     * @return SQL condition that a value of the class is present in the given columns
     */
    private static String getIsNotNull(ResourceClass resClass, List<Column> columns, Table table)
    {
        return switch(getNullTest(resClass, columns))
        {
            case Boolean b -> Boolean.toString(!b);
            case Column witness -> witness.fromTable(table) + " IS NOT NULL";
            default -> throw new IllegalStateException();
        };
    }


    /**
     * Conjunction of the conditions: {@code false} if some condition is {@code false}, {@code true} if none remains
     * otherwise, the remaining conditions joined by {@code AND} in parentheses otherwise.
     *
     * @param conditions the conditions
     * @return the conjunction
     */
    private static String and(Stream<String> conditions)
    {
        List<String> list = conditions.filter(c -> !c.equals("true")).sorted().toList();

        if(list.contains("false"))
            return "false";

        if(list.isEmpty())
            return "true";

        return list.stream().collect(joining(" AND ", "(", ")"));
    }


    /**
     * Disjunction of the conditions: {@code true} if some condition is {@code true}, {@code false} if none remains
     * otherwise, the remaining conditions joined by {@code OR} in parentheses otherwise.
     *
     * @param conditions the conditions
     * @return the disjunction
     */
    private static String or(Stream<String> conditions)
    {
        List<String> list = conditions.filter(c -> !c.equals("false")).sorted().toList();

        if(list.contains("true"))
            return "true";

        if(list.isEmpty())
            return "false";

        return list.stream().collect(joining(" OR ", "(", ")"));
    }


    /**
     * SQL condition that the variable is unbound: no value of any of its classes is present, tested on the witness
     * columns of the classes.
     *
     * @param table the table the columns are accessed through, or null
     * @return SQL condition that the variable is unbound
     */
    public String getIsNull(Table table)
    {
        return and(mappings.entrySet().stream().filter(e -> e.getValue() != null)
                .map(e -> getIsNull(e.getKey(), e.getValue(), table)));
    }


    /**
     * SQL condition that the variable is unbound, see {@link #getIsNull(Table)}.
     *
     * @return SQL condition that the variable is unbound
     */
    public String getIsNull()
    {
        return getIsNull((Table) null);
    }


    /**
     * SQL condition that the variable is bound: a value of some of its classes is present, tested on the witness
     * columns of the classes.
     *
     * @param table the table the columns are accessed through, or null
     * @return SQL condition that the variable is bound
     */
    public String getIsNotNull(Table table)
    {
        return or(mappings.entrySet().stream().filter(e -> e.getValue() != null)
                .map(e -> getIsNotNull(e.getKey(), e.getValue(), table)));
    }


    /**
     * SQL condition that the variable is bound, see {@link #getIsNotNull(Table)}.
     *
     * @return SQL condition that the variable is bound
     */
    public String getIsNotNull()
    {
        return getIsNotNull((Table) null);
    }


    /**
     * SQL condition that the variable has no value of the given class, tested on the witness column of the class.
     *
     * @param resClass the resource class
     * @return SQL condition that the variable has no value of the given class
     */
    public String getIsNull(ResourceClass resClass)
    {
        return and(Stream.of(getIsNull(resClass, deriveMapping(resClass), null)));
    }


    /**
     * SQL condition that the variable has a value of the given class, tested on the witness column of the class.
     *
     * @param resClass the resource class
     * @return SQL condition that the variable has a value of the given class
     */
    public String getIsNotNull(ResourceClass resClass)
    {
        return or(Stream.of(getIsNotNull(resClass, deriveMapping(resClass), null)));
    }


    /**
     * Expression yielding the string value of the variable when it is a string literal (plain or language-tagged), null
     * otherwise.
     *
     * @return expression yielding the string value of the variable when it is a string literal (plain or
     *         language-tagged), null otherwise
     */
    public Column getStringLiteral()
    {
        boolean literalCanBeNull = canBeNull || mappings.size() > 1;

        Set<Column> variants = new HashSet<>();

        for(Entry<ResourceClass, List<Column>> e : mappings.entrySet())
        {
            if(isString(e.getKey()))
                variants.add(e.getKey().toClass(xsdString, e.getValue(), literalCanBeNull).get(0));
            else if(isLangString(e.getKey()))
                variants.add(e.getKey().toClass(rdfLangString, e.getValue(), literalCanBeNull).get(0));
            else if(hasStringLiteral(e.getKey()))
                variants.add(new ExpressionColumn("sparql.rdfbox_get_string_literal("
                        + e.getKey().toClass(box, e.getValue(), literalCanBeNull).get(0) + ")"));
        }

        return coalesce(variants);
    }


    /**
     * Expression yielding the string value of the variable when it belongs to one of the given string literal classes.
     *
     * @param resClasses the resource classes
     * @return expression yielding the string value of the variable when it belongs to one of the given string literal
     *         classes
     */
    public Column getStringLiteral(Set<ResourceClass> resClasses)
    {
        boolean literalCanBeNull = canBeNull || mappings.size() > 1;

        Set<Column> variants = new HashSet<>();

        for(ResourceClass resClass : resClasses)
        {
            List<Column> columns = deriveMapping(resClass);

            if(isString(resClass))
                variants.add(resClass.toClass(xsdString, columns, literalCanBeNull).get(0));
            else if(isLangString(resClass))
                variants.add(resClass.toClass(rdfLangString, columns, literalCanBeNull).get(0));
            else if(hasStringLiteral(resClass))
                variants.add(new ExpressionColumn("sparql.rdfbox_get_string_literal("
                        + resClass.toClass(box, columns, literalCanBeNull).get(0) + ")"));
            else
                throw new IllegalArgumentException();
        }

        return coalesce(variants);
    }


    /**
     * Expression yielding the numeric value of the variable, taken in class {@code source}, promoted to the numeric
     * class {@code target} by the {@code sparql.cast_as_*_from_*} or {@code sparql.rdfbox_promote_to_*} functions.
     *
     * @param source the class the value is taken in
     * @param target the numeric class to promote to
     * @return expression yielding the numeric value of the variable, taken in class {@code source}, promoted to the
     *         numeric class {@code target} by the {@code sparql.cast_as_*_from_*} or {@code
     *         sparql.rdfbox_promote_to_*} functions
     */
    public Column promoteNumericAs(ResourceClass source, ResourceClass target)
    {
        ResourceClass base = Stream.concat(numericBaseClasses.stream(), Stream.of(box))
                .filter(r -> source.isSubclassOf(r)).findFirst().orElseThrow(IllegalArgumentException::new);

        Column value = deriveMapping(base).get(0);

        if(base.equals(box))
            return new ExpressionColumn("sparql.rdfbox_promote_to_" + getLiteralClassName(target) + "(" + value + ")");
        else if(target.isSubclassOf(base))
            return value;
        else
            return new ExpressionColumn("sparql.cast_as_" + target.getResourceName() + "_from_"
                    + getLiteralClassName(base) + "(" + value + ")");
    }


    /**
     * COALESCE of {@link #promoteNumericAs(ResourceClass, ResourceClass)} over the given source classes.
     *
     * @param set the classes
     * @param target the numeric class to promote to
     * @return COALESCE of {@link #promoteNumericAs(ResourceClass, ResourceClass)} over the given source classes
     */
    public Column promoteNumericAs(Set<ResourceClass> set, ResourceClass target)
    {
        return Column.coalesce(set.stream().map(r -> promoteNumericAs(r, target)).collect(toSet()));
    }


    /**
     * Resource classes the variable may take.
     *
     * @return resource classes the variable may take
     */
    public final Set<ResourceClass> getClasses()
    {
        return mappings.keySet();
    }


    /**
     * Classes of the variable that overlap with the given class.
     *
     * @param resClass the resource class
     * @return classes of the variable that overlap with the given class
     */
    public final Set<ResourceClass> getCompatibleClasses(ResourceClass resClass)
    {
        Set<ResourceClass> result = new HashSet<>();

        for(ResourceClass r : mappings.keySet())
            if(!ResourceClass.areDisjunct(r, resClass))
                result.add(r);

        return result;
    }


    /**
     * Non-constant columns of all classes.
     *
     * @return non-constant columns of all classes
     */
    public Set<Column> getNonConstantColumns()
    {
        Set<Column> result = new HashSet<>();

        for(List<Column> columns : mappings.values())
            if(columns != null)
                for(Column column : columns)
                    if(!(column instanceof ConstantColumn))
                        result.add(column);

        return result;
    }


    /**
     * Expression columns of all classes.
     *
     * @return expression columns of all classes
     */
    public Set<Column> getExpressionColumns()
    {
        Set<Column> result = new HashSet<>();

        for(List<Column> columns : mappings.values())
            if(columns != null)
                for(Column column : columns)
                    if(column instanceof ExpressionColumn)
                        result.add(column);

        return result;
    }


    /**
     * Non-constant columns of the given class.
     *
     * @param resourceClass the resource class
     * @return non-constant columns of the given class
     */
    public Set<Column> getNonConstantColumns(ResourceClass resourceClass)
    {
        Set<Column> result = new HashSet<>();

        for(Column column : mappings.get(resourceClass))
            if(!(column instanceof ConstantColumn))
                result.add(column);

        return result;
    }


    /**
     * True if every class is materialised by constant columns only.
     *
     * @return true if every class is materialised by constant columns only, false otherwise
     */
    public boolean isConstant()
    {
        for(List<Column> columns : mappings.values())
        {
            if(columns == null)
                return false;

            for(Column column : columns)
                if(!(column instanceof ConstantColumn))
                    return false;
        }

        return true;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        VariableBinding other = (VariableBinding) object;

        if(!Objects.equals(variable, other.variable))
            return false;

        if(canBeNull != other.canBeNull)
            return false;

        if(!mappings.equals(other.mappings))
            return false;

        return true;
    }


    @Override
    public int hashCode()
    {
        return variable.hashCode() + Boolean.hashCode(canBeNull) + mappings.hashCode();
    }


    /**
     * True if some class has materialised columns.
     *
     * @return true if some class has materialised columns, false otherwise
     */
    public boolean hasMapping()
    {
        return mappings.values().stream().anyMatch(c -> c != null);
    }


    /**
     * True if some column is an SQL expression rather than a plain column or constant.
     *
     * @return true if some column is an SQL expression rather than a plain column or constant, false otherwise
     */
    public boolean hasExpressionColumn()
    {
        return mappings.values().stream().flatMap(c -> c.stream()).anyMatch(c -> c instanceof ExpressionColumn);
    }
}
