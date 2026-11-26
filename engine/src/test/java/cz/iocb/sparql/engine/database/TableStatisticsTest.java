package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.request.Request;



/**
 * Arithmetic of the estimates over given statistics: the rows of a disjunct are the rows of its group multiplied by the
 * selectivities of its bound columns (the frequency of a most common value, the average of the remaining values
 * otherwise, which share the rows that are not null, the planner's default without statistics) and by the null
 * fractions, the columns required not to be null being null together; the disjuncts add up; the distinct values of a
 * bound column are one, of a free column the skew-corrected count capped by the rows, of several columns the
 * combinations kept for the free ones together, else those of the column that has the most of them; a group column is
 * accounted for by its group; the effective number of distinct values follows from the frequencies among the rows that
 * are not null.
 */
public class TableStatisticsTest
{
    private static final TableColumn key = new TableColumn("p", VARCHAR);

    private static final TableColumn s = new TableColumn("s", INT4);

    private static final TableColumn o = new TableColumn("o", INT4);

    private static final TableColumn n = new TableColumn("n", INT4);

    private static final TableColumn m = new TableColumn("m", INT4);

    private static final Column one = new ValueColumn("1", INT4);

    private static final Column two = new ValueColumn("2", INT4);

    private static final Column a = new ValueColumn("a", VARCHAR);


    /**
     * Statistics of one group of 1000 rows: 100 distinct values of {@code s} with the value 1 held by a fifth of the
     * rows and the other values equally frequent, 10 distinct values of {@code o}, a column {@code n} of two values
     * null in half of the rows, a column {@code m} of four values null in four fifths of the rows and a column
     * {@code u} nothing is known about, and 15 combinations of the values of {@code o} and {@code n} kept for the two
     * columns together; grouped by {@code p} when asked so.
     */
    private static class TestStatistics extends TableStatisticsBase
    {
        private final boolean grouped;

        private final Group group;

        TestStatistics(boolean grouped)
        {
            this.grouped = grouped;
            this.group = new Group(grouped ? "'a'" : null, 1000,
                    Map.of("s", ColumnStatistics.of(1000, 100, 0, 1, 0.2, 0.04), "o",
                            ColumnStatistics.of(1000, 10, 0, 0, 0, 0), "n", ColumnStatistics.of(1000, 2, 0.5, 0, 0, 0),
                            "m", ColumnStatistics.of(1000, 4, 0.8, 0, 0, 0)));
        }

        @Override
        protected boolean isGroupColumn(TableColumn column)
        {
            return grouped && key.equals(column);
        }

        @Override
        protected Group lookupGroup(Request request, Condition condition)
        {
            return group;
        }

        @Override
        protected double lookupFrequency(Request request, Group group, TableColumn column, Column value)
        {
            return column.equals(s) && value.equals(one) ? 0.2 : Double.NaN;
        }

        @Override
        protected double lookupDistinct(Request request, Group group, Set<TableColumn> columns)
        {
            return columns.equals(Set.of(o, n)) ? 15 : Double.NaN;
        }
    }


    private static Conditions conditions(Condition... disjuncts)
    {
        Conditions conditions = new Conditions(false);

        for(Condition disjunct : disjuncts)
            conditions.add(disjunct);

        return conditions;
    }


    private static Condition equal(Column column, Column value)
    {
        Condition condition = new Condition();
        condition.addAreEqual(column, value);

        return condition;
    }


    @Test
    public void rowsOfTheWholeGroup()
    {
        assertEquals(1000, new TestStatistics(false).estimateRows(null, new Conditions(true)));
    }


    @Test
    public void rowsOfAMostCommonValue()
    {
        assertEquals(200, new TestStatistics(false).estimateRows(null, conditions(equal(s, one))), 1e-9);
    }


    @Test
    public void rowsOfAnotherValue()
    {
        // the remaining 80 % of the rows over the remaining 99 values
        assertEquals(1000 * 0.8 / 99, new TestStatistics(false).estimateRows(null, conditions(equal(s, two))), 1e-9);
    }


    @Test
    public void rowsOfAValueOfANullableColumn()
    {
        // the half of the rows that is not null over the two values
        assertEquals(1000 * 0.5 / 2, new TestStatistics(false).estimateRows(null, conditions(equal(n, one))), 1e-9);

        // a test of the same column for a null adds nothing
        Condition condition = equal(n, one);
        condition.addIsNotNull(n);

        assertEquals(1000 * 0.5 / 2, new TestStatistics(false).estimateRows(null, conditions(condition)), 1e-9);
    }


    @Test
    public void rowsOfAnUnknownColumn()
    {
        TableColumn u = new TableColumn("u", INT4);

        assertEquals(1000 * 0.005, new TestStatistics(false).estimateRows(null, conditions(equal(u, one))), 1e-9);
    }


    @Test
    public void rowsOfTwoBoundColumns()
    {
        Condition condition = equal(s, one);
        condition.addAreEqual(o, two);

        assertEquals(200 * 0.1, new TestStatistics(false).estimateRows(null, conditions(condition)), 1e-9);
    }


    @Test
    public void rowsOfNullTests()
    {
        Condition isNull = new Condition();
        isNull.addIsNull(n);

        Condition isNotNull = new Condition();
        isNotNull.addIsNotNull(n);

        assertEquals(500, new TestStatistics(false).estimateRows(null, conditions(isNull)), 1e-9);
        assertEquals(500, new TestStatistics(false).estimateRows(null, conditions(isNotNull)), 1e-9);
    }


    @Test
    public void rowsOfColumnsNullTogether()
    {
        // the columns required not to be null leave the rows of the one with the most nulls
        Condition filled = new Condition();
        filled.addIsNotNull(n);
        filled.addIsNotNull(m);

        assertEquals(1000 * 0.2, new TestStatistics(false).estimateRows(null, conditions(filled)), 1e-9);

        // one of the two values of the other column is in a half of them, as it is in a half of the rows of its column
        Condition outer = equal(n, one);
        outer.addIsNotNull(m);

        assertEquals(1000 * 0.2 / 2, new TestStatistics(false).estimateRows(null, conditions(outer)), 1e-9);

        // the rows of a value of the column with the most nulls are among the rows the other column leaves
        Condition inner = equal(m, one);
        inner.addIsNotNull(n);

        assertEquals(1000 * 0.2 / 4, new TestStatistics(false).estimateRows(null, conditions(inner)), 1e-9);

        // the values of the two columns are independent of each other among those rows
        Condition both = equal(n, one);
        both.addAreEqual(m, two);

        assertEquals(1000 * 0.2 / 2 / 4, new TestStatistics(false).estimateRows(null, conditions(both)), 1e-9);

        // a column required to be null is independent of the others
        Condition mixed = new Condition();
        mixed.addIsNull(n);
        mixed.addIsNotNull(m);

        assertEquals(1000 * 0.5 * 0.2, new TestStatistics(false).estimateRows(null, conditions(mixed)), 1e-9);
    }


    @Test
    public void nullFractionOfAColumn()
    {
        TableColumn u = new TableColumn("u", INT4);

        assertEquals(0.5, new TestStatistics(false).estimateNullFraction(null, n));
        assertEquals(0, new TestStatistics(false).estimateNullFraction(null, s));

        // nothing is known about the column, and a constant is no column of the table
        assertEquals(0, new TestStatistics(false).estimateNullFraction(null, u));
        assertEquals(0, new TestStatistics(false).estimateNullFraction(null, one));
    }


    @Test
    public void rowsOfADisjunction()
    {
        assertEquals(200 + 1000 * 0.8 / 99,
                new TestStatistics(false).estimateRows(null, conditions(equal(s, one), equal(s, two))), 1e-9);
    }


    @Test
    public void rowsOfAContradiction()
    {
        Condition condition = equal(s, one);
        condition.addAreEqual(s, two);

        assertEquals(0, new TestStatistics(false).estimateRows(null, conditions(condition)));
        assertEquals(0, new TestStatistics(false).estimateRows(null, new Conditions(false)));
    }


    @Test
    public void groupColumnIsAccountedForByTheGroup()
    {
        assertEquals(1000, new TestStatistics(true).estimateRows(null, conditions(equal(key, a))), 1e-9);
        assertEquals(1000 * 0.005, new TestStatistics(false).estimateRows(null, conditions(equal(key, a))), 1e-9);
    }


    @Test
    public void distinctOfABoundColumn()
    {
        assertEquals(1, new TestStatistics(false).estimateDistinct(null, Set.of(s), conditions(equal(s, one))));
        assertEquals(1, new TestStatistics(true).estimateDistinct(null, Set.of(key), conditions(equal(key, a))));
    }


    @Test
    public void distinctOfAFreeColumn()
    {
        // the geometric mean of 100 distinct values and of their effective number
        double effective = 1 / (0.04 + 0.8 * 0.8 / 99);

        assertEquals(Math.sqrt(100 * effective),
                new TestStatistics(false).estimateDistinct(null, Set.of(s), new Conditions(true)), 1e-9);
        assertEquals(10, new TestStatistics(false).estimateDistinct(null, Set.of(o), new Conditions(true)), 1e-9);
    }


    @Test
    public void distinctIsCappedByTheRows()
    {
        // the 8.08 rows estimated for the value 2 of s cannot hold the 10 distinct values of o
        assertEquals(1000 * 0.8 / 99,
                new TestStatistics(false).estimateDistinct(null, Set.of(o), conditions(equal(s, two))), 1e-9);
    }


    @Test
    public void distinctOfAnUnknownColumn()
    {
        TableColumn u = new TableColumn("u", INT4);

        assertEquals(200, new TestStatistics(false).estimateDistinct(null, Set.of(u), conditions(equal(s, one))), 1e-9);
    }


    @Test
    public void distinctOfAConstant()
    {
        assertEquals(1, new TestStatistics(false).estimateDistinct(null, Set.of(one), new Conditions(true)));
    }


    @Test
    public void distinctOfSeveralColumns()
    {
        double effective = 1 / (0.04 + 0.8 * 0.8 / 99);
        TableColumn u = new TableColumn("u", INT4);

        // the values of the column that has the most of them
        assertEquals(Math.sqrt(100 * effective),
                new TestStatistics(false).estimateDistinct(null, Set.of(s, o), new Conditions(true)), 1e-9);

        // a bound column and a constant have one value
        assertEquals(10, new TestStatistics(false).estimateDistinct(null, Set.of(s, o, one), conditions(equal(s, one))),
                1e-9);

        // a column nothing is known about has a value of its own in every row
        assertEquals(200, new TestStatistics(false).estimateDistinct(null, Set.of(o, u), conditions(equal(s, one))),
                1e-9);
    }


    @Test
    public void distinctOfColumnsTogether()
    {
        // the 15 combinations kept for o and n together, more than the 10 values of o
        assertEquals(15, new TestStatistics(false).estimateDistinct(null, Set.of(o, n), new Conditions(true)), 1e-9);
        assertEquals(15, new TestStatistics(false).estimateDistinct(null, Set.of(o, n), conditions(equal(s, one))),
                1e-9);

        // a column bound to a value is no part of the combination
        assertEquals(2, new TestStatistics(false).estimateDistinct(null, Set.of(o, n), conditions(equal(o, one))),
                1e-9);
    }


    @Test
    public void effectiveDistinctValues()
    {
        // equally frequent values: the effective number is the number
        assertEquals(10, ColumnStatistics.of(1000, 10, 0, 0, 0, 0).effectiveDistinct(), 1e-9);
        assertEquals(10, ColumnStatistics.of(1000, 10, 0, 0, 0, 0).joinDistinct(), 1e-9);

        // a fraction of the rows is distinct
        assertEquals(500, ColumnStatistics.of(1000, -0.5, 0, 0, 0, 0).distinct(), 1e-9);

        // one value in 90 % of the rows and nine others in the rest: 1 / (0.81 + 0.01 / 9)
        ColumnStatistics skewed = ColumnStatistics.of(1000, 10, 0, 1, 0.9, 0.81);
        assertEquals(1 / (0.81 + 0.01 / 9), skewed.effectiveDistinct(), 1e-9);
        assertEquals(Math.sqrt(10 * skewed.effectiveDistinct()), skewed.joinDistinct(), 1e-9);

        // the effective number never exceeds the number of distinct values
        assertEquals(1, ColumnStatistics.of(1000, 1, 0, 1, 1, 1).effectiveDistinct(), 1e-9);

        // a null is no value: of the 400 rows that are not null, one value in a half and 200 others in the rest
        ColumnStatistics nullable = ColumnStatistics.of(1000, 201, 0.6, 1, 0.2, 0.04);
        assertEquals(0.4 * 0.4 / (0.04 + 0.2 * 0.2 / 200), nullable.effectiveDistinct(), 1e-9);

        // so the number is that of the same values in a column without the null rows
        assertEquals(ColumnStatistics.of(400, 201, 0, 1, 0.5, 0.25).effectiveDistinct(), nullable.effectiveDistinct(),
                1e-9);

        // a column of nulls alone
        assertEquals(1, ColumnStatistics.of(1000, 0, 1, 0, 0, 0).effectiveDistinct(), 1e-9);
    }
}
