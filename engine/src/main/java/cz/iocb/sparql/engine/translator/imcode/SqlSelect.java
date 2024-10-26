package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDate;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDateTime;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isIri;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumeric;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumericCompatibleWith;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultTag;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.parser.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<String> projections;
    private final LinkedHashMap<String, Direction> orderBy;
    private final List<String> simpleOrderBy;
    private final BigInteger offset;
    private final BigInteger limit;
    private final boolean distinct;


    protected SqlSelect(List<String> projections, SqlIntercode child, LinkedHashMap<String, Direction> orderBy,
            BigInteger offset, BigInteger limit, List<String> simpleOrderBy, boolean distinct)
    {
        super(child.getVariables().restrict(projections), child.isDeterministic());

        this.child = child;
        this.projections = projections;
        this.orderBy = orderBy;
        this.simpleOrderBy = simpleOrderBy;
        this.offset = offset;
        this.limit = limit;
        this.distinct = distinct;
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
    }


    public static SqlIntercode create(Request request, Set<String> variables, SqlIntercode child, boolean distinct,
            LinkedHashMap<String, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(limit != null && limit.compareTo(BigInteger.valueOf(0)) <= 0)
            return SqlNoSolution.get();

        if(child == SqlEmptySolution.get())
        {
            if(offset == null && limit == null)
                return SqlEmptySolution.get();

            if(offset != null && offset.compareTo(BigInteger.valueOf(0)) > 0)
                return SqlNoSolution.get();
        }

        LinkedHashMap<String, Direction> stripedOrderBy = new LinkedHashMap<String, Direction>();

        for(Entry<String, Direction> e : orderBy.entrySet())
            if(child.getVariables().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());

        if(distinct && variables.containsAll(stripedOrderBy.keySet()))
        {
            child = SqlDistinct.create(request, child, variables);
            distinct = false;
        }

        return new SqlSelect(child.getVariables().restrict(variables), child, distinct, stripedOrderBy, offset, limit);
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
        LinkedHashMap<String, Direction> stripedOrderBy = new LinkedHashMap<String, Direction>();

        for(Entry<String, Direction> e : orderBy.entrySet())
            if(child.getVariables().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());


        List<String> stripedSimpleOrderBy = new ArrayList<String>();

        for(String var : simpleOrderBy)
            if(child.getVariables().get(var) != null && !stripedOrderBy.containsKey(var))
                stripedSimpleOrderBy.add(var);

        if(child.getVariables().restrict(stripedSimpleOrderBy).getNonConstantColumns().isEmpty())
            stripedSimpleOrderBy = List.of();


        if(distinct && projections.containsAll(stripedOrderBy.keySet()))
        {
            child = SqlDistinct.create(request, child, new HashSet<String>(projections));
            distinct = false;
        }

        return new SqlSelect(projections, child, stripedOrderBy, offset, limit, stripedSimpleOrderBy, distinct);
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


    public SqlSelect optimize(Request request)
    {
        if(!isTopLevel())
            throw new UnsupportedOperationException();

        SqlIntercode optimizedChild = child.optimize(request, new HashSet<String>(projections), false);
        return createTopLevel(request, projections, optimizedChild, distinct, orderBy, offset, limit, simpleOrderBy);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        if(isTopLevel())
            throw new UnsupportedOperationException();

        if(restrictions == null)
            return this;

        HashSet<String> childRestrictions = new HashSet<String>(restrictions);
        childRestrictions.addAll(orderBy.keySet());

        SqlIntercode optimizedChild = child.optimize(request, childRestrictions, reduced);

        if(orderBy.isEmpty() && limit == null && (offset == null || offset.equals(BigInteger.ZERO)))
            return optimizedChild;

        return create(request, restrictions, optimizedChild, distinct, orderBy, offset, limit);
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
                builder.append(translateSelectVariables(projections, variables, branch.getVariables()));
                builder.append(" FROM (");
                builder.append(branch.translate(request));
                builder.append(") AS tab");
            }
        }
        else
        {
            builder.append("SELECT ");
            builder.append(translateSelectVariables(projections, variables, child.getVariables()));

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


    public static String translateSelectVariables(Collection<String> projections, UsedVariables variables,
            UsedVariables childVariables)
    {
        StringBuilder builder = new StringBuilder();
        boolean hasSelect = false;

        for(String variableName : projections)
        {
            UsedVariable variable = variables.get(variableName);
            UsedVariable childVariable = childVariables.get(variableName);

            if(variable == null || variable.getClasses().isEmpty())
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append("NULL AS \"");
                builder.append(variableName.replaceFirst("^@", ""));
                builder.append('#');
                builder.append(ResultTag.NULL.getTag());
                builder.append('"');
            }
            else
            {
                Set<ResourceClass> classes = variable.getClasses();

                LinkedHashMap<List<ResultTag>, List<ResourceClass>> resultClasses = new LinkedHashMap<>();

                for(ResourceClass resClass : classes)
                {
                    List<ResourceClass> list = resultClasses.get(resClass.getResultTags());

                    if(list == null)
                    {
                        list = new ArrayList<ResourceClass>();
                        resultClasses.put(resClass.getResultTags(), list);
                    }

                    list.add(resClass);
                }

                for(Entry<List<ResultTag>, List<ResourceClass>> entry : resultClasses.entrySet())
                {
                    List<ResultTag> tags = entry.getKey();
                    List<ResourceClass> fullClasses = entry.getValue();

                    Set<ResourceClass> childClasses = childVariable != null ? childVariable.getClasses() :
                            new HashSet<ResourceClass>();

                    List<ResourceClass> resClasses = fullClasses.stream().filter(c -> childClasses.contains(c))
                            .collect(toList());

                    for(int part = 0; part < tags.size(); part++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(resClasses.size() == 0)
                        {
                            builder.append("NULL::" + tags.get(part).getSqlType());
                        }
                        else
                        {
                            if(resClasses.size() > 1)
                                builder.append("coalesce(");

                            for(int i = 0; i < resClasses.size(); i++)
                            {
                                appendComma(builder, i > 0);

                                ResourceClass resClass = resClasses.get(i);
                                builder.append(resClass.toResult(childVariable.getMapping(resClass)).get(part));
                            }

                            if(resClasses.size() > 1)
                                builder.append(")");
                        }

                        builder.append(" AS \"");

                        builder.append(variableName.replaceFirst("^@", ""));
                        builder.append('#');
                        builder.append(tags.get(part).getTag());
                        builder.append('"');
                    }
                }
            }
        }

        if(!hasSelect)
        {
            builder.append("1 AS \"*#");
            builder.append(ResultTag.NULL.getTag());
            builder.append('"');
        }

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
                        builder.append("sparql.rdfbox_create_from_").append(numeric.getName()).append("(");

                    builder.append(variable.getMapping(numeric).get(0));

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append(")");
                }

                if(numerics.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:booleans
            if(variable.containsClass(xsdBoolean))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(xsdBoolean).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:strings
            if(variable.containsClass(xsdString))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(xsdString).get(0));

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

        return builder.toString();
    }


    private boolean isTopLevel()
    {
        return projections != null;
    }
}
