package cz.iocb.sparql.engine.database;



/**
 * Statistics of one column over a set of rows: its distinct values, the skew of their frequencies, the fraction of null
 * values and what the list of its most common values covers.
 *
 * @param distinct number of distinct values of the column
 * @param effectiveDistinct effective number of distinct values, the inverse Simpson index of the frequencies of the
 *            values (the number of equally frequent values that would give the same probability of two rows sharing a
 *            value); equal to {@code distinct} when the values are equally frequent, smaller when they are skewed,
 *            {@code NaN} when unknown
 * @param nullFraction fraction of the rows whose value is null
 * @param mostCommonFraction fraction of the rows holding one of the most common values kept
 * @param mostCommonCount number of the most common values kept
 */
public record ColumnStatistics(double distinct, double effectiveDistinct, double nullFraction,
        double mostCommonFraction, int mostCommonCount)
{
    /**
     * Number of distinct values to use when the column is joined with values of an unknown distribution: the geometric
     * mean of the number of distinct values (right when the joined values are a uniform sample of the values) and of
     * the effective number (right when the joined values are distributed as the rows of the column), which has shown to
     * estimate the fan-out of joins through skewed columns better than either of them.
     *
     * @return number of distinct values to use when the column is joined
     */
    public double joinDistinct()
    {
        if(Double.isNaN(effectiveDistinct) || effectiveDistinct <= 0 || effectiveDistinct >= distinct)
            return distinct;

        return Math.sqrt(distinct * effectiveDistinct);
    }


    /**
     * Statistics derived from the parameters of the planner: the number of distinct values (negative values standing
     * for a fraction of the rows, as in {@code pg_stats.n_distinct}), the fraction of null values and the list of the
     * most common values summarised by their number, the sum of their frequencies and the sum of the squares of their
     * frequencies, from which the effective number of distinct values is derived with the remaining values taken as
     * equally frequent.
     *
     * @param rows number of rows
     * @param distinct number of distinct values, or minus the fraction of the rows that are distinct
     * @param nullFraction fraction of null values
     * @param mostCommonCount number of the most common values kept
     * @param mostCommonFraction sum of the frequencies of the most common values
     * @param mostCommonSquares sum of the squares of the frequencies of the most common values
     * @return the derived statistics
     */
    public static ColumnStatistics of(double rows, double distinct, double nullFraction, int mostCommonCount,
            double mostCommonFraction, double mostCommonSquares)
    {
        if(distinct < 0)
            distinct = -distinct * rows;

        distinct = Math.max(distinct, 1);

        double rest = Math.max(1 - mostCommonFraction, 0);
        double others = Math.max(distinct - mostCommonCount, 1);
        double effective = 1 / (mostCommonSquares + rest * rest / others);

        return new ColumnStatistics(distinct, Math.min(effective, distinct), nullFraction, mostCommonFraction,
                mostCommonCount);
    }
}
