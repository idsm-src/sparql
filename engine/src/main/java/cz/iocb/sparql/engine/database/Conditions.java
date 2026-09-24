package cz.iocb.sparql.engine.database;

import static java.util.stream.Collectors.toSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;



/**
 * Disjunction of {@link Condition}s, i.e. a condition in disjunctive normal form. No disjunct means false, a disjunct
 * without predicates means true.
 */
public class Conditions
{
    /**
     * The disjuncts.
     */
    private Set<Condition> conditions = new HashSet<>();


    /**
     * Creates the constant condition {@code true} or {@code false}.
     *
     * @param value the constant truth value
     */
    public Conditions(boolean value)
    {
        if(value)
            add(new Condition());
    }


    /**
     * Copy constructor.
     *
     * @param other the disjunction to add
     */
    public Conditions(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    /**
     * Creates the disjunction of a single condition.
     *
     * @param condition the disjunct
     */
    public Conditions(Condition condition)
    {
        conditions.add(condition);
    }


    /**
     * Adds a disjunct.
     *
     * @param condition the disjunct
     */
    public void add(Condition condition)
    {
        conditions.add(condition);
    }


    /**
     * Adds all disjuncts of the other disjunction.
     *
     * @param other the disjunction to add
     */
    public void add(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    /**
     * Conjunction of the disjunction with a single condition, distributed over the disjuncts.
     *
     * @param left the left condition
     * @param right the right condition
     * @return conjunction of the disjunction with a single condition, distributed over the disjuncts
     */
    public static Conditions and(Conditions left, Condition right)
    {
        Conditions result = new Conditions(false);

        for(Condition l : left.conditions)
            result.add(Condition.and(l, right));

        return result;
    }


    /**
     * Conjunction, distributed over the disjuncts.
     *
     * @param left the left condition
     * @param right the right condition
     * @return conjunction, distributed over the disjuncts
     */
    public static Conditions and(Conditions left, Conditions right)
    {
        Conditions result = new Conditions(false);

        for(Condition l : left.conditions)
            for(Condition r : right.conditions)
                result.add(Condition.and(l, r));

        return result;
    }


    /**
     * Disjunction.
     *
     * @param left the left condition
     * @param right the right condition
     * @return disjunction
     */
    public static Conditions or(Conditions left, Conditions right)
    {
        if(left.isTrue() || right.isTrue())
            return new Conditions(true);

        Conditions result = new Conditions(false);

        result.add(left);
        result.add(right);

        return result;
    }


    /**
     * Conjunction of all the disjunctions.
     *
     * @param conditions the conditions
     * @return conjunction of all the disjunctions
     */
    public static Conditions and(Conditions... conditions)
    {
        Conditions result = new Conditions(true);

        for(Conditions condition : conditions)
            result = and(result, condition);

        return result;
    }


    /**
     * Disjunction of all the disjunctions.
     *
     * @param conditions the conditions
     * @return disjunction of all the disjunctions
     */
    public static Conditions or(Conditions... conditions)
    {
        Conditions result = new Conditions(true);

        for(Conditions condition : conditions)
            result = or(result, condition);

        return result;
    }


    /**
     * True if some disjunct is trivially true.
     *
     * @return true if some disjunct is trivially true
     */
    public boolean isTrue()
    {
        return conditions.stream().anyMatch(c -> c.isTrue());
    }


    /**
     * True if every disjunct is contradictory.
     *
     * @return true if every disjunct is contradictory, false otherwise
     */
    public boolean isFalse()
    {
        return conditions.stream().allMatch(c -> c.isFalse());
    }


    /**
     * Table columns equal to {@code col} in some disjunct.
     *
     * @param col the column
     * @return table columns equal to {@code col} in some disjunct
     */
    public Set<Column> getEqualTableColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualTableColumns(col).stream()).collect(toSet());
    }


    /**
     * Columns equal to {@code col} in some disjunct.
     *
     * @param col the column
     * @return columns equal to {@code col} in some disjunct
     */
    public Set<Column> getEqualColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualColumns(col).stream()).collect(toSet());
    }


    /**
     * All non-constant columns referenced by any disjunct.
     *
     * @return all non-constant columns referenced by any disjunct
     */
    public Set<Column> getNonConstantColumns()
    {
        return conditions.stream().flatMap(c -> c.getNonConstantColumns().stream()).collect(toSet());
    }


    @Override
    public int hashCode()
    {
        return conditions.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object.getClass() != getClass())
            return false;

        return conditions.equals(((Conditions) object).conditions);
    }


    /**
     * The disjuncts.
     *
     * @return the disjuncts
     */
    public Set<Condition> getConditions()
    {
        return Collections.unmodifiableSet(conditions);
    }


    /**
     * Columns required to be not null by every disjunct.
     *
     * @return columns required to be not null by every disjunct
     */
    public Set<Column> getIsNotNull()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<>());

        Set<Column> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<>(condition.getIsNotNull());
            else
                result.retainAll(condition.getIsNotNull());
        }

        return Collections.unmodifiableSet(result);
    }


    /**
     * Columns required to be null by every disjunct.
     *
     * @return columns required to be null by every disjunct
     */
    public Set<Column> getIsNull()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<>());

        Set<Column> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<>(condition.getIsNull());
            else
                result.retainAll(condition.getIsNull());
        }

        return Collections.unmodifiableSet(result);
    }


    /**
     * Equalities holding in every disjunct.
     *
     * @return the resulting set
     */
    public Set<ColumnComparison> getAreEqual()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<>());

        Set<ColumnComparison> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<>(condition.getAreEqual());
            else
                result.retainAll(condition.getAreEqual());
        }

        return Collections.unmodifiableSet(result);
    }


    /**
     * Inequalities holding in every disjunct.
     *
     * @return the resulting set
     */
    public Set<ColumnComparison> getAreNotEqual()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<>());

        Set<ColumnComparison> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<>(condition.getAreNotEqual());
            else
                result.retainAll(condition.getAreNotEqual());
        }

        return Collections.unmodifiableSet(result);
    }
}
