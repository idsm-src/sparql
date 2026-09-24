package cz.iocb.sparql.engine.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.model.pattern.PatternElement;
import cz.iocb.sparql.engine.model.pattern.Values;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The part of {@link SelectQuery} containing the actual query. I.e. the part starting with SELECT.
 */
public class Select extends PatternElement implements GraphPattern
{
    /**
     * Projected variables, possibly with expressions.
     */
    private final List<Projection> projections;

    /**
     * The WHERE clause.
     */
    private final GraphPattern pattern;

    /**
     * Trailing VALUES clause, or null.
     */
    private final Values values;

    /**
     * True for a sub-select nested in a pattern.
     */
    private final boolean isSubSelect;

    /**
     * DISTINCT modifier.
     */
    private boolean isDistinct;

    /**
     * REDUCED modifier.
     */
    private boolean isReduced;

    /**
     * True if the select uses grouping or aggregates.
     */
    private boolean isInAggregateMode;

    /**
     * FROM and FROM NAMED clauses.
     */
    private List<DataSet> dataSets = new ArrayList<>();

    /**
     * GROUP BY conditions.
     */
    private List<GroupCondition> groupByConditions = new ArrayList<>();

    /**
     * HAVING conditions.
     */
    private List<Expression> havingConditions = new ArrayList<>();

    /**
     * ORDER BY conditions.
     */
    private List<OrderCondition> orderByConditions = new ArrayList<>();

    /**
     * LIMIT, or null.
     */
    private BigInteger limit;

    /**
     * OFFSET, or null.
     */
    private BigInteger offset;


    /**
     * Creates the select; the projected variables become the variables in scope.
     *
     * @param projections the projected variables
     * @param pattern the WHERE clause
     * @param values the trailing VALUES clause, or null
     * @param isSubSelect true for a sub-select
     */
    public Select(List<Projection> projections, GraphPattern pattern, Values values, boolean isSubSelect)
    {
        this.projections = Collections.unmodifiableList(projections);
        this.pattern = pattern;
        this.values = values;
        this.isSubSelect = isSubSelect;

        for(Projection projection : projections)
            variablesInScope.add(projection.getVariable());
    }


    /**
     * True if this is a sub-select nested in a graph pattern rather than the top-level select of a query.
     *
     * @return true if this is a sub-select nested in a graph pattern rather than the top-level select of a query, false
     *         otherwise
     */
    public boolean isSubSelect()
    {
        return isSubSelect;
    }


    /**
     * True if DISTINCT is specified.
     *
     * @return true if DISTINCT is specified, false otherwise
     */
    public boolean isDistinct()
    {
        return isDistinct;
    }


    /**
     * Sets DISTINCT; cannot be combined with REDUCED.
     *
     * @param isDistinct whether DISTINCT is specified
     */
    public void setDistinct(boolean isDistinct)
    {
        if(isReduced && isDistinct)
            throw new IllegalArgumentException();

        this.isDistinct = isDistinct;
    }


    /**
     * True if REDUCED is specified.
     *
     * @return true if REDUCED is specified, false otherwise
     */
    public boolean isReduced()
    {
        return isReduced;
    }


    /**
     * Sets REDUCED; cannot be combined with DISTINCT.
     *
     * @param isReduced whether REDUCED is specified
     */
    public void setReduced(boolean isReduced)
    {
        if(isDistinct && isReduced)
            throw new IllegalArgumentException();

        this.isReduced = isReduced;
    }


    /**
     * True if the select uses grouping or aggregates, i.e. its projection and modifiers operate on grouped solutions.
     *
     * @return true if the select uses grouping or aggregates, i.e. its projection and modifiers operate on grouped
     *         solutions, false otherwise
     */
    public boolean isInAggregateMode()
    {
        return isInAggregateMode;
    }


    /**
     * Sets whether the select uses grouping or aggregates.
     *
     * @param isInAggregateMode whether the select uses grouping or aggregates
     */
    public void setIsInAggregateMode(boolean isInAggregateMode)
    {
        this.isInAggregateMode = isInAggregateMode;
    }


    /**
     * Projected variables, possibly with expressions.
     *
     * @return projected variables, possibly with expressions
     */
    public List<Projection> getProjections()
    {
        return projections;
    }


    /**
     * FROM and FROM NAMED clauses.
     *
     * @return FROM and FROM NAMED clauses
     */
    public List<DataSet> getDataSets()
    {
        return dataSets;
    }


    /**
     * Replaces the dataset clauses (used for the protocol dataset parameters).
     *
     * @param dataSets the dataset clauses
     */
    public void setDataSets(List<DataSet> dataSets)
    {
        this.dataSets = dataSets;
    }


    /**
     * The WHERE clause.
     *
     * @return the WHERE clause
     */
    public GraphPattern getPattern()
    {
        return pattern;
    }


    /**
     * GROUP BY conditions.
     *
     * @return GROUP BY conditions
     */
    public List<GroupCondition> getGroupByConditions()
    {
        return groupByConditions;
    }


    /**
     * HAVING conditions.
     *
     * @return HAVING conditions
     */
    public List<Expression> getHavingConditions()
    {
        return havingConditions;
    }


    /**
     * ORDER BY conditions.
     *
     * @return ORDER BY conditions
     */
    public List<OrderCondition> getOrderByConditions()
    {
        return orderByConditions;
    }


    /**
     * LIMIT value, or null if not specified.
     *
     * @return LIMIT value, or null if not specified
     */
    public BigInteger getLimit()
    {
        return limit;
    }


    /**
     * Sets LIMIT; null for none.
     *
     * @param limit the upper bound
     */
    public void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }


    /**
     * OFFSET value, or null if not specified.
     *
     * @return OFFSET value, or null if not specified
     */
    public BigInteger getOffset()
    {
        return offset;
    }


    /**
     * Sets OFFSET; null for none.
     *
     * @param offset the offset, or null
     */
    public void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }


    /**
     * The trailing VALUES clause, or null.
     *
     * @return the trailing VALUES clause, or null
     */
    public Values getValues()
    {
        return values;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
