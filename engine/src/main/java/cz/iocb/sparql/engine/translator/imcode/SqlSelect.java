package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloatPoint;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.scalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.determineResultClass;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
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


        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(limit != null && limit.compareTo(BigInteger.valueOf(0)) <= 0)
            return SqlNoSolution.get();

        if(optChild.equals(SqlEmptySolution.get()) && offset == null && limit == null)
            return SqlEmptySolution.get();

        if(optChild.equals(SqlEmptySolution.get()) && offset != null && offset.compareTo(BigInteger.valueOf(0)) > 0)
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
            builder.append(translateInnerSelectVariables(variables));
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


            Map<ResourceClass, Set<ResourceClass>> sortSet = new HashMap<ResourceClass, Set<ResourceClass>>();

            for(ResourceClass r : variable.getClasses())
            {
                if(isBlankNode(r))
                    sortSet.computeIfAbsent(scalarBlankNode, _ -> new HashSet<>()).add(r);
                else if(isIri(r))
                    sortSet.computeIfAbsent(iri, _ -> new HashSet<>()).add(r);
                else if(isNumeric(r))
                    sortSet.computeIfAbsent(numeric, _ -> new HashSet<>()).add(r);
                else if(isBoolean(r))
                    sortSet.computeIfAbsent(xsdBoolean, _ -> new HashSet<>()).add(r);
                else if(isString(r))
                    sortSet.computeIfAbsent(xsdString, _ -> new HashSet<>()).add(r);
                else if(isDate(r))
                    sortSet.computeIfAbsent(xsdScalarDate, _ -> new HashSet<>()).add(r);
                else if(isDateTime(r))
                    sortSet.computeIfAbsent(xsdScalarDateTime, _ -> new HashSet<>()).add(r);
                else if(hasBlankNode(r) || hasIri(r) || hasNumeric(r) || hasBoolean(r) || hasString(r) || hasDate(r)
                        || hasDateTime(r))
                    sortSet.computeIfAbsent(box, _ -> new HashSet<>()).add(r);
            }

            if(sortSet.get(box) != null)
                sortSet = Map.of(box, sortSet.values().stream().flatMap(r -> r.stream()).collect(toSet()));


            // order unbounded
            if(variable.canBeNull())
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getIsNotNull());

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order blank nodes
            if(sortSet.get(scalarBlankNode) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getIsNull(getUnionClass(sortSet.get(scalarBlankNode))));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order IRIs
            if(sortSet.get(iri) != null)
            {
                Set<ResourceClass> iris = sortSet.get(iri);

                if(iris.size() > 1 || !(iris.iterator().next() instanceof IriClass iriClass))
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(variable.deriveMapping(iri).get(0));

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
                else
                {
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
            if(sortSet.get(numeric) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                Set<ResourceClass> numerics = sortSet.get(numeric);

                if(numerics.stream().allMatch(r -> isFloatPoint(r)))
                {
                    ResourceClass sortClass = xsdFloat;

                    for(ResourceClass r : numerics)
                        sortClass = determineResultClass(sortClass, r);

                    builder.append(variable.promoteNumericAs(numerics, sortClass));
                }
                else if(numerics.stream().allMatch(r -> !isFloatPoint(r)))
                {
                    ResourceClass sortClass = xsdShort;

                    for(ResourceClass r : numerics)
                        sortClass = determineResultClass(sortClass, r);

                    builder.append(variable.promoteNumericAs(numerics, sortClass));
                }
                else
                {
                    builder.append(variable.deriveMapping(getUnionClass(numerics, box)));
                }

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:booleans
            if(sortSet.get(xsdBoolean) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.deriveMapping(xsdBoolean).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:strings
            if(sortSet.get(xsdString) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.deriveMapping(xsdString).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:dateTimes
            if(sortSet.get(xsdScalarDateTime) != null)
            {
                Set<ResourceClass> dateTimes = sortSet.get(xsdScalarDateTime);

                ResourceClass sortClass = dateTimes.stream().allMatch(r -> r.isSubclassOf(xsdCompositeDateTime)) ?
                        xsdCompositeDateTime : xsdScalarDateTime;

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.deriveMapping(sortClass).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:dates
            if(sortSet.get(xsdScalarDate) != null)
            {
                Set<ResourceClass> dates = sortSet.get(xsdScalarDate);

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dates.size() == 1 && dates.iterator().next().getEffectiveClass() instanceof DateConstantZoneClass c)
                    builder.append(variable.deriveMapping(c).get(0));
                else
                    builder.append(variable.deriveMapping(xsdScalarDate).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order boxed values
            if(sortSet.get(box) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.deriveMapping(getUnionClass(sortSet.get(box), box)).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            //TODO: sort other types of literals as well
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
