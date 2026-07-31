package cz.iocb.sparql.engine.database;

import static java.util.stream.Collectors.toSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;



public class Conditions
{
    private Set<Condition> conditions = new HashSet<>();


    public Conditions(boolean value)
    {
        if(value)
            add(new Condition());
    }


    public Conditions(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    public Conditions(Condition condition)
    {
        conditions.add(condition);
    }


    public void add(Condition condition)
    {
        conditions.add(condition);
    }


    public void add(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    public static Conditions and(Conditions left, Condition right)
    {
        Conditions result = new Conditions(false);

        for(Condition l : left.conditions)
            result.add(Condition.and(l, right));

        return result;
    }


    public static Conditions and(Conditions left, Conditions right)
    {
        Conditions result = new Conditions(false);

        for(Condition l : left.conditions)
            for(Condition r : right.conditions)
                result.add(Condition.and(l, r));

        return result;
    }


    public static Conditions or(Conditions left, Conditions right)
    {
        if(left.isTrue() || right.isTrue())
            return new Conditions(true);

        Conditions result = new Conditions(false);

        result.add(left);
        result.add(right);

        return result;
    }


    public static Conditions and(Conditions... conditions)
    {
        Conditions result = new Conditions(true);

        for(Conditions condition : conditions)
            result = and(result, condition);

        return result;
    }


    public static Conditions or(Conditions... conditions)
    {
        Conditions result = new Conditions(true);

        for(Conditions condition : conditions)
            result = or(result, condition);

        return result;
    }


    public boolean isTrue()
    {
        return conditions.stream().anyMatch(c -> c.isTrue());
    }


    public boolean isFalse()
    {
        return conditions.stream().allMatch(c -> c.isFalse());
    }


    public Set<Column> getEqualTableColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualTableColumns(col).stream()).collect(toSet());
    }


    public Set<Column> getEqualColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualColumns(col).stream()).collect(toSet());
    }


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


    public Set<Condition> getConditions()
    {
        return Collections.unmodifiableSet(conditions);
    }


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
