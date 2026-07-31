package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.determineResultClass;
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
import cz.iocb.sparql.engine.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<Variable> projections;
    private final LinkedHashMap<Variable, Direction> orderBy;
    private final List<Variable> simpleOrderBy;
    private final BigInteger offset;
    private final BigInteger limit;
    private final boolean distinct;
    private final Map<Variable, List<ResultResourceClass>> description;


    protected SqlSelect(List<Variable> projections, SqlIntercode child, LinkedHashMap<Variable, Direction> orderBy,
            BigInteger offset, BigInteger limit, List<Variable> simpleOrderBy, boolean distinct)
    {
        super(child.getVariableBindings().restrict(new Restrictions(projections)), child.isDeterministic());

        this.child = child;
        this.projections = projections;
        this.orderBy = orderBy;
        this.simpleOrderBy = simpleOrderBy;
        this.offset = offset;
        this.limit = limit;
        this.distinct = distinct;
        this.description = new LinkedHashMap<>();

        for(Variable var : projections)
        {
            VariableBinding binding = bindings.get(var);

            if(binding == null)
                description.put(var, List.of());
            else
                description.put(var, binding.getClasses().stream().flatMap(c -> c.getResultResourceClasses().stream())
                        .distinct().toList());
        }
    }


    protected SqlSelect(VariableBindings bindings, SqlIntercode child, boolean distinct,
            LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        super(bindings, child.isDeterministic());

        this.child = child;
        this.projections = null;
        this.orderBy = orderBy;
        this.simpleOrderBy = List.of();
        this.offset = offset;
        this.limit = limit;
        this.distinct = distinct;
        this.description = null;
    }


    public static SqlIntercode create(Request request, Set<Variable> variables, SqlIntercode child, boolean distinct,
            LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return new SqlSelect(child.getVariableBindings().restrict(new Restrictions(variables)), child, distinct,
                orderBy, offset, limit);
    }


    public static SqlIntercode create(Request request, Set<Variable> variables, SqlIntercode child, BigInteger offset,
            BigInteger limit, boolean distinct)
    {
        return create(request, variables, child, distinct, new LinkedHashMap<>(), offset, limit);
    }


    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit,
            List<Variable> simpleOrderBy)
    {
        return new SqlSelect(projections, child, orderBy, offset, limit, simpleOrderBy, distinct);
    }


    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, distinct, orderBy, offset, limit, List.of());
    }


    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<>(), offset, limit, List.of());
    }


    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<>(), null, null, List.of());
    }


    public SqlSelect addExternalLimits(BigInteger offset, BigInteger limit, List<Variable> order)
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


        List<Variable> newOrderBy = new ArrayList<>(order);

        if(newLimit != null || newOffset != null)
            for(Variable var : projections)
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


        LinkedHashMap<Variable, Direction> stripedOrderBy = new LinkedHashMap<>();

        for(Entry<Variable, Direction> e : orderBy.entrySet())
            if(optChild.getVariableBindings().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());


        List<Variable> stripedSimpleOrderBy = new ArrayList<>();

        for(Variable var : simpleOrderBy)
            if(optChild.getVariableBindings().get(var) != null && !stripedOrderBy.containsKey(var))
                stripedSimpleOrderBy.add(var);


        if(optChild.getVariableBindings().restrict(new Restrictions(stripedSimpleOrderBy)).getNonConstantColumns()
                .isEmpty())
            stripedSimpleOrderBy = List.of();


        boolean optDistinct = distinct;

        if(optDistinct && projections.containsAll(stripedOrderBy.keySet()))
        {
            optChild = SqlDistinct.create(request, optChild, new HashSet<>(projections)).optimize(request,
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

        LinkedHashMap<Variable, Direction> stripedOrderBy = new LinkedHashMap<>();

        for(Entry<Variable, Direction> e : orderBy.entrySet())
            if(optChild.getVariableBindings().get(e.getKey()) != null)
                stripedOrderBy.put(e.getKey(), e.getValue());

        boolean optDistinct = distinct;

        if(optDistinct && bindings.getVariables().containsAll(stripedOrderBy.keySet()))
        {
            optChild = SqlDistinct.create(request, optChild, bindings.getVariables()).optimize(request,
                    childRestrictions, reduced, evalServices);
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


        if(restrictions.isOptimized(bindings) && optChild == child && optDistinct == distinct
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
            builder.append(translateInnerSelectVariables(bindings));

            if(distinct)
            {
                builder.append(" FROM (SELECT ");
                builder.append(translateInnerSelectVariables(bindings));
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
                builder.append(translateSelectVariables(description, branch.getVariableBindings()));
                builder.append(" FROM (");
                builder.append(branch.translate(request));
                builder.append(") AS tab");
            }
        }
        else
        {
            builder.append("SELECT ");
            builder.append(translateSelectVariables(description, child.getVariableBindings()));

            if(distinct)
            {
                builder.append(" FROM (SELECT ");
                builder.append(translateInnerSelectVariables(bindings));
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
            builder.append(translateInnerSelectVariables(bindings));
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


    private static String translateSelectVariables(Map<Variable, List<ResultResourceClass>> description,
            VariableBindings bindings)
    {
        ColumnMap columnMap = new ColumnMap();

        StringBuilder builder = new StringBuilder();
        boolean hasSelect = false;

        for(Entry<Variable, List<ResultResourceClass>> entry : description.entrySet())
        {
            Variable var = entry.getKey();
            VariableBinding binding = bindings.get(var);

            if(binding == null)
                binding = new VariableBinding(var, true);

            for(ResultResourceClass resClass : entry.getValue())
            {
                List<Column> colNames = ((ResourceClass) resClass).createColumns(columnMap, var);
                List<Column> cols = binding.deriveMapping((ResourceClass) resClass);

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


    private String translateInnerSelectVariables(VariableBindings bindings)
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = bindings.getNonConstantColumns();

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

        for(Entry<Variable, Direction> order : orderBy.entrySet())
        {
            Variable var = order.getKey();
            VariableBinding binding = child.getVariableBindings().get(var);

            if(binding == null || !binding.hasMapping())
                continue;


            Map<ResourceClass, Set<ResourceClass>> sortSet = new HashMap<>();

            for(ResourceClass r : binding.getClasses())
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
            if(binding.canBeNull())
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.getIsNotNull());

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order blank nodes
            if(sortSet.get(scalarBlankNode) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.getIsNull(getUnionClass(sortSet.get(scalarBlankNode))));

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

                    builder.append(binding.deriveMapping(iri).get(0));

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
                else
                {
                    for(Column col : iriClass.toOrderColumns(binding.getMapping(iriClass)))
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

                    builder.append(binding.promoteNumericAs(numerics, sortClass));
                }
                else if(numerics.stream().allMatch(r -> !isFloatPoint(r)))
                {
                    ResourceClass sortClass = xsdShort;

                    for(ResourceClass r : numerics)
                        sortClass = determineResultClass(sortClass, r);

                    builder.append(binding.promoteNumericAs(numerics, sortClass));
                }
                else
                {
                    builder.append(binding.deriveMapping(getUnionClass(numerics, box)));
                }

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:booleans
            if(sortSet.get(xsdBoolean) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(xsdBoolean).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:strings
            if(sortSet.get(xsdString) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(xsdString).get(0));

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

                builder.append(binding.deriveMapping(sortClass).get(0));

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
                    builder.append(binding.deriveMapping(c).get(0));
                else
                    builder.append(binding.deriveMapping(xsdScalarDate).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order boxed values
            if(sortSet.get(box) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(getUnionClass(sortSet.get(box), box)).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            //TODO: sort other types of literals as well
        }


        if(withSimple && !simpleOrderBy.isEmpty())
        {
            Set<Column> usedColumns = new HashSet<>();

            for(Variable var : simpleOrderBy)
            {
                for(Column column : child.getVariable(var).getNonConstantColumns())
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


    public Map<Variable, List<ResultResourceClass>> getResultDescription()
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
            builder.append(projections.stream().map(v -> v.toString()).collect(joining(" ", " ", "")));
        else if(!bindings.getVariables().isEmpty())
            builder.append(bindings.getVariables().stream().map(v -> v.toString()).collect(joining(" ", " ", "")));

        if(!orderBy.isEmpty() || !simpleOrderBy.isEmpty())
        {
            builder.append(" order by");

            if(!orderBy.isEmpty())
                builder.append(orderBy.entrySet().stream()
                        .map(e -> (e.getValue() == Direction.Descending ? "desc" : "asc") + "(" + e.getKey() + ")")
                        .collect(joining(" ", " ", "")));

            if(!simpleOrderBy.isEmpty())
                builder.append(simpleOrderBy.stream().map(v -> v.toString()).collect(joining(" ", " ", "")));
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
