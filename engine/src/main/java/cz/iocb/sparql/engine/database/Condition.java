package cz.iocb.sparql.engine.database;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;



/**
 * Conjunction of simple predicates over columns: {@code IS NULL}, {@code IS NOT NULL}, {@code =},
 * {@code IS NOT DISTINCT FROM} and {@code !=}.
 *
 * Columns compared by {@code =} or {@code IS NOT DISTINCT FROM} form equivalence classes that are kept transitively
 * closed. Whether a pair of a class is rendered as the strict {@code =} or as the null-safe
 * {@code IS NOT DISTINCT FROM} is not a property of the pair but of the class: as soon as some member of a class is
 * known to be not null (it is required to be not null, it is compared by {@code =} or {@code !=}, or it is a non-null
 * constant), all members are not null and every pair of the class is strict. A class containing a NULL constant
 * requires its members to be null instead. A column compared by {@code =} or {@code !=} drops its {@code IS NOT NULL}
 * predicate as redundant. Contradictory predicates are stored as they are and detected by {@link #isFalse()}.
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
     * Column pairs required to be equal by the strict {@code =}, i.e. pairs of equivalence classes known to be not
     * null.
     */
    final Set<ColumnComparison> areEqual = new HashSet<>();

    /**
     * Column pairs required to be equal by the null-safe {@code IS NOT DISTINCT FROM}, i.e. pairs of equivalence
     * classes not known to be not null.
     */
    final Set<ColumnComparison> areNotDistinct = new HashSet<>();

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
        areNotDistinct.addAll(condition.areNotDistinct);
        areNotEqual.addAll(condition.areNotEqual);
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
     * True if the column is known to be not null: it is a non-null constant, it is required to be not null, or it is
     * compared by {@code =} or {@code !=}.
     *
     * @param column the column
     * @return true if the column is known to be not null, false otherwise
     */
    private boolean isKnownNotNull(Column column)
    {
        if(column instanceof ConstantColumn)
            return !isNullConstant(column);

        if(isNotNull.contains(column))
            return true;

        if(areEqual.stream().anyMatch(p -> p.contains(column)))
            return true;

        if(areNotEqual.stream().anyMatch(p -> p.contains(column)))
            return true;

        return false;
    }


    /**
     * Makes the null-safe equalities of the equivalence class of the column strict, because the column is known to be
     * not null; the not-null predicates of the class become redundant.
     *
     * @param column the column
     */
    private void promote(Column column)
    {
        Set<ColumnComparison> pairs = new HashSet<>();

        for(Column member : getEqualColumns(column))
            areNotDistinct.stream().filter(p -> p.contains(member)).forEach(pairs::add);

        if(pairs.isEmpty())
            return;

        areNotDistinct.removeAll(pairs);
        areEqual.addAll(pairs);

        for(ColumnComparison pair : pairs)
        {
            isNotNull.remove(pair.getLeft());
            isNotNull.remove(pair.getRight());
        }
    }


    /**
     * Requires the column to be not null; constants are ignored. The null-safe equalities of its equivalence class
     * become strict.
     *
     * @param column the column
     */
    public void addIsNotNull(Column column)
    {
        if(column instanceof ConstantColumn)
            return;

        if(areNotDistinct.stream().anyMatch(p -> p.contains(column)))
            promote(column);
        else
            isNotNull.add(column);
    }


    /**
     * Requires the column to be null; a NULL constant is ignored as it always holds.
     *
     * @param column the column
     */
    public void addIsNull(Column column)
    {
        if(!isNullConstant(column))
            isNull.add(column);
    }


    /**
     * Requires the columns to be equal by the strict {@code =}, also to everything already equal to either of them;
     * their not-null predicates become redundant. A NULL constant can never be equal, such a predicate is kept and
     * makes the condition false.
     *
     * @param col1 the first column
     * @param col2 the second column
     */
    public void addAreEqual(Column col1, Column col2)
    {
        if(isNullConstant(col1) || isNullConstant(col2))
        {
            areEqual.add(new ColumnComparison(col1, col2));
            return;
        }

        if(col1.equals(col2))
            return;

        Set<Column> cols1 = getEqualColumns(col1);
        Set<Column> cols2 = getEqualColumns(col2);

        for(Column c1 : cols1)
            for(Column c2 : cols2)
                if(!c1.equals(c2))
                    areEqual.add(new ColumnComparison(c1, c2));

        isNotNull.remove(col1);
        isNotNull.remove(col2);

        promote(col1);
    }


    /**
     * Requires the columns to be equal by the null-safe {@code IS NOT DISTINCT FROM}, also to everything already equal
     * to either of them. When some member of the resulting equivalence class is known to be not null, the class is
     * strict and the requirement is the same as {@link #addAreEqual(Column, Column)}. When one of the columns is a NULL
     * constant, the members of the class are required to be null instead.
     *
     * @param col1 the first column
     * @param col2 the second column
     */
    public void addAreNotDistinct(Column col1, Column col2)
    {
        if(col1.equals(col2))
            return;

        if(isNullConstant(col1) || isNullConstant(col2))
        {
            Column column = isNullConstant(col1) ? col2 : col1;

            for(Column member : getEqualColumns(column))
                addIsNull(member);

            return;
        }

        Set<Column> cols1 = getEqualColumns(col1);
        Set<Column> cols2 = getEqualColumns(col2);

        boolean strict = cols1.stream().anyMatch(c -> isKnownNotNull(c))
                || cols2.stream().anyMatch(c -> isKnownNotNull(c));

        for(Column c1 : cols1)
            for(Column c2 : cols2)
                if(!c1.equals(c2))
                    (strict ? areEqual : areNotDistinct).add(new ColumnComparison(c1, c2));

        if(strict)
        {
            for(Column c : cols1)
                isNotNull.remove(c);

            for(Column c : cols2)
                isNotNull.remove(c);

            promote(col1);
        }
    }


    /**
     * Requires the columns to be different; their not-null predicates become redundant and the null-safe equalities of
     * their equivalence classes become strict.
     *
     * @param col1 the first column
     * @param col2 the second column
     */
    public void addAreNotEqual(Column col1, Column col2)
    {
        areNotEqual.add(new ColumnComparison(col1, col2));
        isNotNull.remove(col1);
        isNotNull.remove(col2);

        promote(col1);
        promote(col2);
    }


    /**
     * Requires the columns to be pairwise equal by the strict {@code =} (matched by position).
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
     * Requires the columns representing terms to be pairwise equal (matched by position): by the strict {@code =} at
     * the determining positions and by the null-safe {@code IS NOT DISTINCT FROM} at the optional positions.
     *
     * @param cols1 the first columns
     * @param cols2 the second columns
     * @param optional tells whether the position is optional (see
     *            {@code cz.iocb.sparql.engine.mapping.classes.ResourceClass#isOptionalColumn})
     */
    public void addAreEqual(List<Column> cols1, List<Column> cols2, IntPredicate optional)
    {
        assert cols1.size() == cols2.size();

        for(int i = 0; i < cols1.size(); i++)
            if(optional.test(i))
                addAreNotDistinct(cols1.get(i), cols2.get(i));
            else
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

        for(ColumnComparison p : condition.areNotDistinct)
            addAreNotDistinct(p.getLeft(), p.getRight());
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

        if(!areNotDistinct.isEmpty())
            return false;

        if(!areNotEqual.isEmpty())
            return false;

        return true;
    }


    /**
     * True if the condition is contradictory: a column both null and not null or compared while null, a non-null
     * constant required to be null, a NULL constant compared by {@code =}, a comparison both equal and not equal, or a
     * column equal to two different constants.
     *
     * @return true if the condition is contradictory, false otherwise
     */
    public boolean isFalse()
    {
        if(isNull.stream().anyMatch(c -> isNotNull.contains(c)))
            return true;

        if(isNull.stream().anyMatch(c -> c instanceof ConstantColumn))
            return true;

        if(areEqual.stream().anyMatch(p -> isNullConstant(p.getLeft()) || isNullConstant(p.getRight())))
            return true;

        if(areEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> areEqual.contains(p) || areNotDistinct.contains(p)))
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
     * Table columns equal to {@code col} by {@code =} or {@code IS NOT DISTINCT FROM} (including {@code col} itself if
     * it is a table column).
     *
     * @param col the column
     * @return table columns equal to {@code col} (including {@code col} itself if it is a table column)
     */
    public Set<Column> getEqualTableColumns(Column col)
    {
        Set<Column> set = new HashSet<>();

        if(col instanceof TableColumn)
            set.add(col);

        getEqualColumns(col).stream().filter(c -> c instanceof TableColumn).forEach(c -> set.add(c));

        return set;
    }


    /**
     * Columns equal to {@code col} by {@code =} or {@code IS NOT DISTINCT FROM}, including {@code col} itself.
     *
     * @param col the column
     * @return columns equal to {@code col}, including {@code col} itself
     */
    public Set<Column> getEqualColumns(Column col)
    {
        Set<Column> set = new HashSet<>();
        set.add(col);

        areEqual.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).forEach(c -> set.add(c));
        areNotDistinct.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).forEach(c -> set.add(c));

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

        for(Set<ColumnComparison> pairs : List.of(areNotEqual, areEqual, areNotDistinct))
        {
            pairs.stream().map(p -> p.getLeft()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
            pairs.stream().map(p -> p.getRight()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        }

        return columns;
    }


    @Override
    public int hashCode()
    {
        return isNotNull.hashCode() + isNull.hashCode() + areEqual.hashCode() + areNotDistinct.hashCode()
                + areNotEqual.hashCode();
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

        if(!areNotDistinct.equals(other.areNotDistinct))
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
     * Column pairs required to be equal by the strict {@code =}.
     *
     * @return column pairs required to be equal by the strict {@code =}
     */
    public Set<ColumnComparison> getAreEqual()
    {
        return Collections.unmodifiableSet(areEqual);
    }


    /**
     * Column pairs required to be equal by the null-safe {@code IS NOT DISTINCT FROM}.
     *
     * @return column pairs required to be equal by the null-safe {@code IS NOT DISTINCT FROM}
     */
    public Set<ColumnComparison> getAreNotDistinct()
    {
        return Collections.unmodifiableSet(areNotDistinct);
    }


    /**
     * Column pairs required to be equal, by the strict or by the null-safe equality.
     *
     * @return column pairs required to be equal, by the strict or by the null-safe equality
     */
    public Set<ColumnComparison> getEquivalences()
    {
        Set<ColumnComparison> result = new HashSet<>(areEqual);
        result.addAll(areNotDistinct);
        return Collections.unmodifiableSet(result);
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
