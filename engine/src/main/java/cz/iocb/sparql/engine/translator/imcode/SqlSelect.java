package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.getExpressionBaseClass;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.getResourceName;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDate;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDateTime;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isIri;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumeric;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumericCompatibleWith;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isString;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.parser.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.request.ColumnMap;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<String> projections;
    private final LinkedHashMap<String, Direction> orderBy;
    private final List<String> simpleOrderBy;
    private final BigInteger offset;
    private final BigInteger limit;
    private final boolean distinct;
    private final Map<String, List<ResultResourceClass>> description;


    protected SqlSelect(List<String> projections, SqlIntercode child, LinkedHashMap<String, Direction> orderBy,
            BigInteger offset, BigInteger limit, List<String> simpleOrderBy, boolean distinct)
    {
        super(child.getVariables().restrict(new Restrictions(projections)), child.isDeterministic());

        this.child = child;
        this.projections = projections;
        this.orderBy = orderBy;
        this.simpleOrderBy = simpleOrderBy;
        this.offset = offset;
        this.limit = limit;
        this.distinct = distinct;
        this.description = new LinkedHashMap<>();

        for(String varName : projections)
        {
            UsedVariable var = variables.get(varName);

            if(var == null)
                description.put(varName, List.of());
            else
                description.put(varName, var.getClasses().stream().flatMap(c -> c.getResultResourceClasses().stream())
                        .distinct().toList());
        }
    }


    protected SqlSelect(UsedVariables variables, SqlIntercode child, boolean distinct,
            LinkedHashMap<String, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.projections = null;
        this.orderBy = orderBy;
        this.simpleOrderBy = List.of();
        this.offset = offset;
        this.limit = limit;
        this.distinct = distinct;
        this.description = null;
    }


    public static SqlIntercode create(Request request, Set<String> variables, SqlIntercode child, boolean distinct,
            LinkedHashMap<String, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return new SqlSelect(child.getVariables().restrict(new Restrictions(variables)), child, distinct, orderBy,
                offset, limit);
    }


    public static SqlIntercode create(Request request, Set<String> variables, SqlIntercode child, BigInteger offset,
            BigInteger limit, boolean distinct)
    {
        return create(request, variables, child, distinct, new LinkedHashMap<String, Direction>(), offset, limit);
    }


    public static SqlSelect createTopLevel(Request request, List<String> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<String, Direction> orderBy, BigInteger offset, BigInteger limit,
            List<String> simpleOrderBy)
    {
        return new SqlSelect(projections, child, orderBy, offset, limit, simpleOrderBy, distinct);
    }


    public static SqlSelect createTopLevel(Request request, List<String> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<String, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, distinct, orderBy, offset, limit, List.of());
    }


    public static SqlSelect createTopLevel(Request request, List<String> projections, SqlIntercode child,
            BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<String, Direction>(), offset, limit,
                List.of());
    }


    public static SqlSelect createTopLevel(Request request, List<String> projections, SqlIntercode child)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<String, Direction>(), null, null,
                List.of());
    }


    public SqlSelect addExternalLimits(BigInteger offset, BigInteger limit, List<String> order)
    {
        if(!isTopLevel() || !simpleOrderBy.isEmpty())
            throw new UnsupportedOperationException();

        BigInteger zero = BigInteger.valueOf(0);

        BigInteger innerOffset = this.offset == null ? zero : this.offset;
        BigInteger innerLimit = this.limit;

        BigInteger outerOffset = offset == null ? zero : offset;
        BigInteger outerLimit = limit;

        if(innerLimit != null)
            innerLimit = innerLimit.subtract(outerOffset).max(zero);

        BigInteger newOffset = outerOffset.add(innerOffset);

        if(newOffset.equals(zero))
            newOffset = null;

        BigInteger newLimit = null;

        if(innerLimit != null && outerLimit != null)
            newLimit = outerLimit.min(innerLimit);
        else if(innerLimit != null)
            newLimit = innerLimit;
        else
            newLimit = outerLimit;

        if(newLimit != null && newLimit.compareTo(zero) <= 0)
            return new SqlSelect(projections, SqlNoSolution.get(), new LinkedHashMap<>(), null, null, List.of(),
                    distinct);


        ArrayList<String> newOrderBy = new ArrayList<String>(order);

        if(newLimit != null || newOffset != null)
            for(String var : projections)
                if(!orderBy.containsKey(var) && !newOrderBy.contains(var))
                    newOrderBy.add(var);

        return new SqlSelect(projections, child, orderBy, newOffset, newLimit, newOrderBy, distinct);
    }


    public SqlSelect optimize(Request request, boolean evalServices)
    {
        if(!isTopLevel())
            throw new UnsupportedOperationException();


        Restrictions childRestrictions = new Restrictions(projections);
        childRestrictions.add(orderBy.keySet()); //TODO: not all resource classes are sortable
        childRestrictions.add(simpleOrderBy); //TODO: not all resource classes are sortable

        SqlIntercode optChild = child.optimize(request, childRestrictions, false, evalServices);


        LinkedHashMap<String, Direction> stripedOrderBy = new LinkedHashMap<String, Direction>();

        for(Entry<String, Direction> e : orderBy.entrySet())
            if(optChild.getVariables().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());


        List<String> stripedSimpleOrderBy = new ArrayList<String>();

        for(String var : simpleOrderBy)
            if(optChild.getVariables().get(var) != null && !stripedOrderBy.containsKey(var))
                stripedSimpleOrderBy.add(var);


        if(optChild.getVariables().restrict(new Restrictions(stripedSimpleOrderBy)).getNonConstantColumns().isEmpty())
            stripedSimpleOrderBy = List.of();


        boolean optDistinct = distinct;

        if(optDistinct && projections.containsAll(stripedOrderBy.keySet()))
        {
            optChild = SqlDistinct.create(request, optChild, new HashSet<String>(projections)).optimize(request,
                    childRestrictions, true, evalServices);
            optDistinct = false;
        }


        if(optChild == child && optDistinct == distinct && stripedOrderBy.equals(orderBy)
                && stripedSimpleOrderBy.equals(simpleOrderBy))
            return this;

        return createTopLevel(request, projections, optChild, optDistinct, stripedOrderBy, offset, limit,
                stripedSimpleOrderBy);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        if(isTopLevel())
            throw new UnsupportedOperationException();


        Restrictions childRestrictions = new Restrictions(restrictions);
        childRestrictions.add(orderBy.keySet()); // FIXME: not all resource classes are sortable
        childRestrictions.add(simpleOrderBy); // FIXME: not all resource classes are sortable

        SqlIntercode optChild = child.optimize(request, childRestrictions, reduced, evalServices);

        LinkedHashMap<String, Direction> stripedOrderBy = new LinkedHashMap<String, Direction>();

        for(Entry<String, Direction> e : orderBy.entrySet())
            if(optChild.getVariables().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());

        boolean optDistinct = distinct;

        if(optDistinct && variables.getNames().containsAll(stripedOrderBy.keySet()))
        {
            optChild = SqlDistinct.create(request, optChild, variables.getNames()).optimize(request, childRestrictions,
                    reduced, evalServices);
            optDistinct = false;
        }


        if(optChild == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(limit != null && limit.compareTo(BigInteger.valueOf(0)) <= 0)
            return SqlNoSolution.get();

        if(optChild == SqlEmptySolution.get() && offset == null && limit == null)
            return SqlEmptySolution.get();

        if(optChild == SqlEmptySolution.get() && offset != null && offset.compareTo(BigInteger.valueOf(0)) > 0)
            return SqlNoSolution.get();

        if(stripedOrderBy.isEmpty() && limit == null && (offset == null || offset.equals(BigInteger.ZERO)))
            return optChild;


        if(restrictions.isOptimized(variables) && optChild == child && optDistinct == distinct
                && stripedOrderBy.equals(orderBy))
            return this;

        return create(request, restrictions.getNames(), optChild, optDistinct, stripedOrderBy, offset, limit);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        if(!isTopLevel())
        {
            builder.append("SELECT ");
            builder.append(translateInnerSelectVariables(variables));

            if(distinct)
            {
                builder.append(" FROM (SELECT ");
                builder.append(translateInnerSelectVariables(variables));
                builder.append(", row_number() OVER (");
                builder.append(translateOrderBy(false));
                builder.append(") AS \"#rn\"");
            }

            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(") AS tab");
        }
        else if(child instanceof SqlUnion union && orderBy.isEmpty() && simpleOrderBy.isEmpty())
        {
            assert !distinct;

            for(int i = 0; i < union.getChilds().size(); i++)
            {
                if(i > 0)
                    builder.append(" UNION ALL ");

                SqlIntercode branch = union.getChilds().get(i);

                builder.append("SELECT ");
                builder.append(translateSelectVariables(description, branch.getVariables()));
                builder.append(" FROM (");
                builder.append(branch.translate(request));
                builder.append(") AS tab");
            }
        }
        else
        {
            builder.append("SELECT ");
            builder.append(translateSelectVariables(description, child.getVariables()));

            if(distinct)
            {
                builder.append(" FROM (SELECT ");
                builder.append(translateInnerSelectVariables(variables));
                builder.append(", row_number() OVER (");
                builder.append(translateOrderBy(false));
                builder.append(") AS \"#rn\"");
            }

            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(") AS tab");

        }

        if(distinct)
        {
            builder.append(") AS tab GROUP BY ");
            builder.append(translateInnerSelectVariables(variables)); // FIXME
            builder.append(" ORDER BY min(\"#rn\")");
        }
        else if(!orderBy.isEmpty() || !simpleOrderBy.isEmpty())
        {
            builder.append(translateOrderBy(true));
        }

        if(limit != null)
            builder.append(" LIMIT ").append(limit.toString());

        if(offset != null)
            builder.append(" OFFSET ").append(offset.toString());

        return builder.toString();
    }


    private static String translateSelectVariables(Map<String, List<ResultResourceClass>> description,
            UsedVariables variables)
    {
        ColumnMap columnMap = new ColumnMap();

        StringBuilder builder = new StringBuilder();
        boolean hasSelect = false;

        for(Entry<String, List<ResultResourceClass>> entry : description.entrySet())
        {
            String variableName = entry.getKey();
            UsedVariable variable = variables.get(variableName);

            if(variable == null)
                variable = new UsedVariable(variableName, true);

            for(ResultResourceClass resClass : entry.getValue())
            {
                List<Column> colNames = ((ResourceClass) resClass).createColumns(columnMap, variableName);
                List<Column> cols = variable.deriveMapping((ResourceClass) resClass);

                for(int i = 0; i < cols.size(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(cols.get(i));
                    builder.append(" AS ");
                    builder.append(colNames.get(i));
                }
            }
        }

        if(!hasSelect)
            builder.append("1");

        return builder.toString();
    }


    private String translateInnerSelectVariables(UsedVariables variables)
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = variables.getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        return builder.toString();
    }


    private String translateOrderBy(boolean withSimple)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(" ORDER BY ");
        boolean hasOrderCondition = false;

        for(Entry<String, Direction> order : orderBy.entrySet())
        {
            String varName = order.getKey();
            UsedVariable variable = child.getVariables().get(varName);

            if(variable == null || !variable.hasMapping())
                continue;

            Set<ResourceClass> classes = variable.getClasses();


            // order unbounded
            if(variable.canBeNull())
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                boolean hasOuterVariants = false;

                if(classes.size() > 1)
                    builder.append("(");

                for(ResourceClass resourceClass : classes)
                {
                    Set<Column> columns = variable.getNonConstantColumns(resourceClass);

                    if(columns.isEmpty())
                        continue;

                    appendOr(builder, hasOuterVariants);

                    hasOuterVariants = true;
                    boolean hasInnerVariants = false;

                    if(columns.size() > 1)
                        builder.append("(");

                    for(Column column : columns)
                    {
                        appendAnd(builder, hasInnerVariants);
                        hasInnerVariants = true;

                        builder.append(column);
                        builder.append(" IS NOT NULL");
                    }

                    if(columns.size() > 1)
                        builder.append(")");
                }

                if(classes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order blank nodes
            for(ResourceClass resClass : variable.getClasses())
            {
                if(resClass instanceof BlankNodeClass)
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(variable.getMapping(resClass).get(0));
                    builder.append(" IS NULL");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
            }


            // order IRIs
            Set<ResourceClass> iris = classes.stream().filter(r -> isIri(r)).collect(toSet());

            if(iris.size() > 0)
            {
                if(iris.size() > 1)
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append("coalesce(");

                    boolean hasVariants = false;

                    for(ResourceClass res : iris)
                    {
                        appendComma(builder, hasVariants);
                        hasVariants = true;

                        builder.append(res.toExpression(variable.getMapping(res)));
                    }

                    builder.append(")");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
                else
                {
                    IriClass iriClass = (IriClass) iris.iterator().next();

                    for(Column col : iriClass.toOrderColumns(variable.getMapping(iriClass)))
                    {
                        appendComma(builder, hasOrderCondition);
                        hasOrderCondition = true;

                        builder.append(col);

                        if(order.getValue() == Direction.Descending)
                            builder.append(" DESC");
                    }
                }
            }


            // order numerics
            Set<ResourceClass> numerics = classes.stream().filter(r -> isNumeric(r)).collect(toSet());

            if(numerics.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                Set<ResourceClass> decimals = classes.stream().filter(r -> isNumericCompatibleWith(r, xsdDecimal))
                        .collect(toSet());

                if(numerics.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass numeric : numerics)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append("sparql.rdfbox_create_from_").append(getResourceName(numeric)).append("(");

                    builder.append(variable.deriveMapping(getExpressionBaseClass(numeric)).get(0));

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append(")");
                }

                if(numerics.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:booleans
            Set<ResourceClass> bools = classes.stream().filter(r -> isBoolean(r)).collect(toSet());

            if(bools.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(bools.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass bool : bools)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(bool.toGeneralClass(xsdBoolean, variable.getMapping(bool), true).get(0));
                }

                if(bools.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:strings
            Set<ResourceClass> strings = classes.stream().filter(r -> isString(r)).collect(toSet());

            if(strings.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(strings.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass string : strings)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(string.toGeneralClass(xsdString, variable.getMapping(string), true).get(0));
                }

                if(strings.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dateTimes
            Set<ResourceClass> dateTimes = classes.stream().filter(r -> isDateTime(r)).collect(toSet());

            if(dateTimes.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dateTimes.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass dateTime : dateTimes)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(dateTime).get(0));
                }

                if(dateTimes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dates
            Set<ResourceClass> dates = classes.stream().filter(r -> isDate(r)).collect(toSet());

            if(dates.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dates.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass date : dates)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(date).get(0));
                }

                if(dates.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // user literals
            Set<ResourceClass> others = classes.stream().filter(r -> r instanceof UserLiteralClass).collect(toSet());

            if(others.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(others.size() > 1)
                    builder.append("coalesce(");

                boolean hasTypeVariants = false;

                for(ResourceClass other : others)
                {
                    appendComma(builder, hasTypeVariants);
                    hasTypeVariants = true;

                    builder.append("'" + ((UserLiteralClass) other).getTypeIri().getValue().replaceAll("'", "''")
                            + "'::varchar");
                }

                if(others.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");


                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(others.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass other : others)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(other).get(0) + "::varchar");
                }

                if(others.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // unsupported literal
            if(variable.containsClass(unsupportedLiteral))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(unsupportedLiteral).get(1));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(unsupportedLiteral).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }
        }

        if(withSimple && !simpleOrderBy.isEmpty())
        {
            HashSet<Column> usedColumns = new HashSet<Column>();

            for(String varName : simpleOrderBy)
            {
                for(Column column : child.getVariable(varName).getNonConstantColumns())
                {
                    if(usedColumns.add(column))
                    {
                        appendComma(builder, hasOrderCondition);
                        hasOrderCondition = true;

                        builder.append(column);
                    }
                }
            }
        }


        if(!hasOrderCondition)
            return "";

        return builder.toString();
    }


    private boolean isTopLevel()
    {
        return projections != null;
    }


    public Map<String, List<ResultResourceClass>> getResultDescription()
    {
        return description;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("select");

        if(distinct)
            builder.append(" distinct");

        if(projections != null)
            builder.append(projections.stream().collect(joining(" ", " ", "")));
        else if(!variables.getNames().isEmpty())
            builder.append(variables.getNames().stream().collect(joining(" ", " ", "")));

        if(!orderBy.isEmpty() || !simpleOrderBy.isEmpty())
        {
            builder.append(" order by");

            if(!orderBy.isEmpty())
                builder.append(orderBy.entrySet().stream()
                        .map(e -> (e.getValue() == Direction.Descending ? "desc" : "asc") + "(" + e.getKey() + ")")
                        .collect(joining(" ", " ", "")));

            if(!simpleOrderBy.isEmpty())
                builder.append(simpleOrderBy.stream().map(e -> e).collect(joining(" ", " ", "")));
        }

        if(offset != null)
            builder.append(" offset ").append(offset);

        if(limit != null)
            builder.append(" limit ").append(limit);

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlSelect imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(projections, imcode.projections))
            return false;

        if(!Objects.equals(orderBy, imcode.orderBy))
            return false;

        if(!Objects.equals(simpleOrderBy, imcode.simpleOrderBy))
            return false;

        if(!Objects.equals(offset, imcode.offset))
            return false;

        if(!Objects.equals(limit, imcode.limit))
            return false;

        if(!Objects.equals(distinct, imcode.distinct))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(projections, orderBy, simpleOrderBy, offset, limit, distinct, child);
    }
}
