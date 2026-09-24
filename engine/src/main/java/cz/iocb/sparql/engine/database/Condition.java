package cz.iocb.sparql.engine.database;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * Conjunction of simple predicates over columns: {@code IS NULL}, {@code IS NOT NULL}, {@code =} and {@code !=}.
 * Equalities are kept transitively closed, and a column compared by {@code =} or {@code !=} drops its
 * {@code IS NOT NULL} predicate as redundant.
 */
public class Condition
{
    /**
     * Unordered pair of compared columns.
     */
    public static class ColumnComparison
    {
        /**
         * One compared column.
         */
        private final Column left;

        /**
         * The other compared column.
         */
        private final Column right;

        /**
         * Creates the pair.
         *
         * @param left one compared column
         * @param right the other compared column
         */
        public ColumnComparison(Column left, Column right)
        {
            this.left = left;
            this.right = right;
        }


        /**
         * True if the column is one of the two.
         *
         * @param col the column
         * @return true if the column is one of the two, false otherwise
         */
        public boolean contains(Column col)
        {
            return col.equals(left) || col.equals(right);
        }


        /**
         * The column compared with {@code col}, or null if {@code col} is not part of the comparison.
         *
         * @param col the column
         * @return the column compared with {@code col}, or null if {@code col} is not part of the comparison
         */
        public Column getOther(Column col)
        {
            if(col.equals(left))
                return right;
            else if(col.equals(right))
                return left;
            else
                return null;
        }


        /**
         * One compared column.
         *
         * @return one compared column
         */
        public Column getLeft()
        {
            return left;
        }


        /**
         * The other compared column.
         *
         * @return the other compared column
         */
        public Column getRight()
        {
            return right;
        }

        @Override
        public int hashCode()
        {
            return left.hashCode() + right.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            ColumnComparison other = (ColumnComparison) object;

            if(left.equals(other.left) && right.equals(other.right))
                return true;

            if(left.equals(other.right) && right.equals(other.left))
                return true;

            return false;
        }
    }


    /**
     * Columns required to be not null.
     */
    final Set<Column> isNotNull = new HashSet<>();

    /**
     * Columns required to be null.
     */
    final Set<Column> isNull = new HashSet<>();

    /**
     * Column pairs required to be equal.
     */
    final Set<ColumnComparison> areEqual = new HashSet<>();

    /**
     * Column pairs required to be different.
     */
    final Set<ColumnComparison> areNotEqual = new HashSet<>();


    /**
     * Creates the empty (true) condition.
     */
    public Condition()
    {
    }


    /**
     * Copy constructor.
     *
     * @param condition the condition to copy or conjoin
     */
    public Condition(Condition condition)
    {
        isNotNull.addAll(condition.isNotNull);
        isNull.addAll(condition.isNull);
        areEqual.addAll(condition.areEqual);
        areNotEqual.addAll(condition.areNotEqual);
    }


    /**
     * Requires the column to be not null; constants are ignored.
     *
     * @param column the column
     */
    public void addIsNotNull(Column column)
    {
        if(!(column instanceof ConstantColumn))
            isNotNull.add(column);
    }


    /**
     * Requires the column to be null.
     *
     * @param column the column
     */
    public void addIsNull(Column column)
    {
        isNull.add(column);
    }


    /**
     * Requires the columns to be equal, also to everything already equal to either of them; their not-null predicates
     * become redundant.
     *
     * @param col1 the first column
     * @param col2 the second column
     */
    public void addAreEqual(Column col1, Column col2)
    {
        Set<Column> cols1 = getEqualColumns(col1);
        Set<Column> cols2 = getEqualColumns(col2);

        for(Column c1 : cols1)
            for(Column c2 : cols2)
                if(!c1.equals(c2))
                    areEqual.add(new ColumnComparison(c1, c2));

        isNotNull.remove(col1);
        isNotNull.remove(col2);
    }


    /**
     * Requires the columns to be different; their not-null predicates become redundant.
     *
     * @param col1 the first column
     * @param col2 the second column
     */
    public void addAreNotEqual(Column col1, Column col2)
    {
        areNotEqual.add(new ColumnComparison(col1, col2));
        isNotNull.remove(col1);
        isNotNull.remove(col2);
    }


    /**
     * Requires the columns to be pairwise equal (matched by position).
     *
     * @param cols1 the first columns
     * @param cols2 the second columns
     */
    public void addAreEqual(List<Column> cols1, List<Column> cols2)
    {
        assert cols1.size() == cols2.size();

        for(int i = 0; i < cols1.size(); i++)
            addAreEqual(cols1.get(i), cols2.get(i));
    }


    /**
     * Conjoins all predicates of the other condition.
     *
     * @param condition the condition to copy or conjoin
     */
    public void add(Condition condition)
    {
        for(Column c : condition.isNotNull)
            addIsNotNull(c);

        for(Column c : condition.isNull)
            addIsNull(c);

        for(ColumnComparison p : condition.areNotEqual)
            addAreNotEqual(p.getLeft(), p.getRight());

        for(ColumnComparison p : condition.areEqual)
            addAreEqual(p.getLeft(), p.getRight());
    }


    /**
     * Conjunction of two conditions.
     *
     * @param left the left condition
     * @param right the right condition
     * @return conjunction of two conditions
     */
    public static Condition and(Condition left, Condition right)
    {
        if(left.isTrue())
            return new Condition(right);

        if(right.isTrue())
            return new Condition(left);

        Condition result = new Condition(left);
        result.add(right);
        return result;
    }


    /**
     * True if the condition contains no predicate.
     *
     * @return true if the condition contains no predicate, false otherwise
     */
    public boolean isTrue()
    {
        if(!isNotNull.isEmpty())
            return false;

        if(!isNull.isEmpty())
            return false;

        if(!areEqual.isEmpty())
            return false;

        if(!areNotEqual.isEmpty())
            return false;

        return true;
    }


    /**
     * True if the condition is contradictory: a column both null and not null or compared while null, a constant
     * required to be null, a comparison both equal and not equal, or a column equal to two different constants.
     *
     * @return true if the condition is contradictory, false otherwise
     */
    public boolean isFalse()
    {
        if(isNull.stream().anyMatch(c -> isNotNull.contains(c)))
            return true;

        if(isNull.stream().anyMatch(c -> c instanceof ConstantColumn))
            return true;

        if(areEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> areEqual.contains(p)))
            return true;

        if(areNotEqual.stream().anyMatch(p -> p.getLeft().equals(p.getRight())))
            return true;


        Set<Column> set = new HashSet<>();

        for(ColumnComparison p : areEqual)
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof ConstantColumn)
            {
                if(!p.getLeft().equals(p.getRight()))
                    return true;
            }
            else if(p.getLeft() instanceof ConstantColumn)
            {
                if(!set.add(p.getRight()))
                    return true;
            }
            else if(p.getRight() instanceof ConstantColumn)
            {
                if(!set.add(p.getLeft()))
                    return true;
            }
        }

        return false;
    }


    /**
     * Table columns equal to {@code col} (including {@code col} itself if it is a table column).
     *
     * @param col the column
     * @return table columns equal to {@code col} (including {@code col} itself if it is a table column)
     */
    public Set<Column> getEqualTableColumns(Column col)
    {
        Set<Column> set = new HashSet<>();

        if(col instanceof TableColumn)
            set.add(col);

        areEqual.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).filter(c -> c instanceof TableColumn)
                .forEach(c -> set.add(c));

        return set;
    }


    /**
     * Columns equal to {@code col}, including {@code col} itself.
     *
     * @param col the column
     * @return columns equal to {@code col}, including {@code col} itself
     */
    public Set<Column> getEqualColumns(Column col)
    {
        Set<Column> set = new HashSet<>();
        set.add(col);

        areEqual.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).forEach(c -> set.add(c));

        return set;
    }


    /**
     * All non-constant columns referenced by the predicates.
     *
     * @return all non-constant columns referenced by the predicates
     */
    public Set<Column> getNonConstantColumns()
    {
        Set<Column> columns = new HashSet<>();

        isNotNull.stream().filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        isNull.stream().filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        areNotEqual.stream().map(p -> p.getLeft()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        areNotEqual.stream().map(p -> p.getRight()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        areEqual.stream().map(p -> p.getLeft()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        areEqual.stream().map(p -> p.getRight()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        return columns;
    }


    @Override
    public int hashCode()
    {
        return isNotNull.hashCode() + isNull.hashCode() + areEqual.hashCode() + areNotEqual.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Condition other = (Condition) object;

        if(!isNotNull.equals(other.isNotNull))
            return false;

        if(!isNull.equals(other.isNull))
            return false;

        if(!areEqual.equals(other.areEqual))
            return false;

        if(!areNotEqual.equals(other.areNotEqual))
            return false;

        return true;
    }


    /**
     * Columns required to be not null.
     *
     * @return columns required to be not null
     */
    public Set<Column> getIsNotNull()
    {
        return Collections.unmodifiableSet(isNotNull);
    }


    /**
     * Columns required to be null.
     *
     * @return columns required to be null
     */
    public Set<Column> getIsNull()
    {
        return Collections.unmodifiableSet(isNull);
    }


    /**
     * Column pairs required to be equal.
     *
     * @return column pairs required to be equal
     */
    public Set<ColumnComparison> getAreEqual()
    {
        return Collections.unmodifiableSet(areEqual);
    }


    /**
     * Column pairs required to be different.
     *
     * @return column pairs required to be different
     */
    public Set<ColumnComparison> getAreNotEqual()
    {
        return Collections.unmodifiableSet(areNotEqual);
    }
}
