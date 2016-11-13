package cz.iocb.chemweb.server.sparql.parser.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;



/**
 * The part of {@link SelectQuery} containing the actual query. I.e. the part starting with SELECT.
 */
public class Select extends BaseElement implements GraphPattern
{
    private boolean isDistinct;
    private boolean isReduced;
    private List<Projection> projections = new ArrayList<>();
    private List<DataSet> dataSets = new ArrayList<>();
    private GraphPattern pattern;
    private List<GroupCondition> groupByConditions = new ArrayList<>();
    private List<Expression> havingConditions = new ArrayList<>();
    private List<OrderCondition> orderByConditions = new ArrayList<>();
    private BigInteger limit;
    private BigInteger offset;
    private Values values;

    public Select()
    {
    }

    public Select(GraphPattern pattern)
    {
        this.pattern = pattern;
    }

    public boolean isDistinct()
    {
        return isDistinct;
    }

    public void setDistinct(boolean isDistinct)
    {
        if(isReduced && isDistinct)
            throw new IllegalArgumentException();

        this.isDistinct = isDistinct;
    }

    public boolean isReduced()
    {
        return isReduced;
    }

    public void setReduced(boolean isReduced)
    {
        if(isDistinct && isReduced)
            throw new IllegalArgumentException();

        this.isReduced = isReduced;
    }

    /**
     * Note: Empty list represents "SELECT *".
     */
    public List<Projection> getProjections()
    {
        return projections;
    }

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    public GraphPattern getPattern()
    {
        return pattern;
    }

    public void setPattern(GraphPattern pattern)
    {
        this.pattern = pattern;
    }

    public List<GroupCondition> getGroupByConditions()
    {
        return groupByConditions;
    }

    public List<Expression> getHavingConditions()
    {
        return havingConditions;
    }

    public List<OrderCondition> getOrderByConditions()
    {
        return orderByConditions;
    }

    public BigInteger getLimit()
    {
        return limit;
    }

    public void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }

    public BigInteger getOffset()
    {
        return offset;
    }

    public void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }

    public Values getValues()
    {
        return values;
    }

    public void setValues(Values values)
    {
        this.values = values;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
