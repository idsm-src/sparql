package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.request.Request;



/**
 * Arithmetic of the estimates over given statistics: the rows of a disjunct are the rows of its group multiplied by the
 * selectivities of its bound columns (the frequency of a most common value, the average of the remaining values
 * otherwise, the planner's default without statistics) and by the null fractions; the disjuncts add up; the distinct
 * values of a bound column are one, of a free column the skew-corrected count capped by the rows; a group column is
 * accounted for by its group; the effective number of distinct values follows from the frequencies.
 */
public class TableStatisticsTest
{
    private static final TableColumn key = new TableColumn("p", VARCHAR);

    private static final TableColumn s = new TableColumn("s", INT4);

    private static final TableColumn o = new TableColumn("o", INT4);

    private static final TableColumn n = new TableColumn("n", INT4);

    private static final Column one = new ValueColumn("1", INT4);

    private static final Column two = new ValueColumn("2", INT4);

    private static final Column a = new ValueColumn("a", VARCHAR);


    /**
     * Statistics of one group of 1000 rows: 100 distinct values of {@code s} with the value 1 held by a fifth of the
     * rows and the other values equally frequent, 10 distinct values of {@code o}, a column {@code n} null in half of
     * the rows and a column {@code u} nothing is known about; grouped by {@code p} when asked so.
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
                            ColumnStatistics.of(1000, 10, 0, 0, 0, 0), "n",
                            ColumnStatistics.of(1000, 2, 0.5, 0, 0, 0)));
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
        assertEquals(1, new TestStatistics(false).estimateDistinct(null, s, conditions(equal(s, one))));
        assertEquals(1, new TestStatistics(true).estimateDistinct(null, key, conditions(equal(key, a))));
    }


    @Test
    public void distinctOfAFreeColumn()
    {
        // the geometric mean of 100 distinct values and of their effective number
        double effective = 1 / (0.04 + 0.8 * 0.8 / 99);

        assertEquals(Math.sqrt(100 * effective),
                new TestStatistics(false).estimateDistinct(null, s, new Conditions(true)), 1e-9);
        assertEquals(10, new TestStatistics(false).estimateDistinct(null, o, new Conditions(true)), 1e-9);
    }


    @Test
    public void distinctIsCappedByTheRows()
    {
        // the 8.08 rows estimated for the value 2 of s cannot hold the 10 distinct values of o
        assertEquals(1000 * 0.8 / 99, new TestStatistics(false).estimateDistinct(null, o, conditions(equal(s, two))),
                1e-9);
    }


    @Test
    public void distinctOfAnUnknownColumn()
    {
        TableColumn u = new TableColumn("u", INT4);

        assertEquals(200, new TestStatistics(false).estimateDistinct(null, u, conditions(equal(s, one))), 1e-9);
    }


    @Test
    public void distinctOfAConstant()
    {
        assertEquals(1, new TestStatistics(false).estimateDistinct(null, one, new Conditions(true)));
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
    }
}
