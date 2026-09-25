package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.determineResultClass;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.Comparator.comparing;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Projection with the solution modifiers DISTINCT, ORDER BY, OFFSET and LIMIT. The top-level instance additionally
 * records how each projected variable is delivered to the result ({@link #getResultDescription}) and is optimised
 * through {@link #optimize(Request, boolean)}.
 */
public final class SqlSelect extends SqlIntercode
{
    /**
     * Projected solutions.
     */
    private final SqlIntercode child;

    /**
     * Projected variables in order; null for a sub-select.
     */
    private final List<Variable> projections;

    /**
     * ORDER BY variables with their directions.
     */
    private final LinkedHashMap<Variable, Direction> orderBy;

    /**
     * Variables ordered by raw columns after the ORDER BY conditions.
     */
    private final List<Variable> simpleOrderBy;

    /**
     * OFFSET, or null.
     */
    private final BigInteger offset;

    /**
     * LIMIT, or null.
     */
    private final BigInteger limit;

    /**
     * DISTINCT modifier.
     */
    private final boolean distinct;

    /**
     * Result classes of each projected variable; null for a sub-select.
     */
    private final Map<Variable, List<ResultResourceClass>> description;


    /**
     * Creates a top-level select.
     *
     * @param projections the projected variables
     * @param child the child node
     * @param orderBy ORDER BY variables with their directions
     * @param offset the offset, or null
     * @param limit the upper bound
     * @param simpleOrderBy variables ordered by raw columns after ORDER BY
     * @param distinct whether the mapping declares distinct rows
     */
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


    /**
     * Creates a sub-select.
     *
     * @param bindings the variable bindings
     * @param child the child node
     * @param distinct whether the mapping declares distinct rows
     * @param orderBy ORDER BY variables with their directions
     * @param offset the offset, or null
     * @param limit the upper bound
     */
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


    /**
     * Sub-select projecting the given variables.
     *
     * @param request the current request
     * @param variables the projected variables
     * @param child the child node
     * @param distinct whether the mapping declares distinct rows
     * @param orderBy ORDER BY variables with their directions
     * @param offset the offset, or null
     * @param limit the upper bound
     * @return sub-select projecting the given variables
     */
    public static SqlIntercode create(Request request, Set<Variable> variables, SqlIntercode child, boolean distinct,
            LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return new SqlSelect(child.getVariableBindings().restrict(new Restrictions(variables)), child, distinct,
                orderBy, offset, limit);
    }


    /**
     * Sub-select projecting the given variables without ordering.
     *
     * @param request the current request
     * @param variables the projected variables
     * @param child the child node
     * @param offset the offset, or null
     * @param limit the upper bound
     * @param distinct whether the mapping declares distinct rows
     * @return sub-select projecting the given variables without ordering
     */
    public static SqlIntercode create(Request request, Set<Variable> variables, SqlIntercode child, BigInteger offset,
            BigInteger limit, boolean distinct)
    {
        return create(request, variables, child, distinct, new LinkedHashMap<>(), offset, limit);
    }


    /**
     * Top-level select. {@code simpleOrderBy} lists variables ordered by their raw columns after the ORDER BY
     * conditions, which makes paging by OFFSET and LIMIT stable.
     *
     * @param request the current request
     * @param projections the projected variables
     * @param child the child node
     * @param distinct whether the mapping declares distinct rows
     * @param orderBy ORDER BY variables with their directions
     * @param offset the offset, or null
     * @param limit the upper bound
     * @param simpleOrderBy variables ordered by raw columns after ORDER BY
     * @return top-level select
     */
    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit,
            List<Variable> simpleOrderBy)
    {
        return new SqlSelect(projections, child, orderBy, offset, limit, simpleOrderBy, distinct);
    }


    /**
     * Top-level select without simple ordering.
     *
     * @param request the current request
     * @param projections the projected variables
     * @param child the child node
     * @param distinct whether the mapping declares distinct rows
     * @param orderBy ORDER BY variables with their directions
     * @param offset the offset, or null
     * @param limit the upper bound
     * @return top-level select without simple ordering
     */
    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            boolean distinct, LinkedHashMap<Variable, Direction> orderBy, BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, distinct, orderBy, offset, limit, List.of());
    }


    /**
     * Top-level select without ordering and DISTINCT.
     *
     * @param request the current request
     * @param projections the projected variables
     * @param child the child node
     * @param offset the offset, or null
     * @param limit the upper bound
     * @return top-level select without ordering and DISTINCT
     */
    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child,
            BigInteger offset, BigInteger limit)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<>(), offset, limit, List.of());
    }


    /**
     * Top-level select without modifiers.
     *
     * @param request the current request
     * @param projections the projected variables
     * @param child the child node
     * @return top-level select without modifiers
     */
    public static SqlSelect createTopLevel(Request request, List<Variable> projections, SqlIntercode child)
    {
        return createTopLevel(request, projections, child, false, new LinkedHashMap<>(), null, null, List.of());
    }


    /**
     * Applies the offset, limit and ordering requested by the caller on top of the query's own: offsets add up, the
     * limits combine, and when paging is in effect all projected variables are appended to the ordering so that the
     * pages are stable.
     *
     * @param offset the offset, or null
     * @param limit the upper bound
     * @param order variables to order the results by, on top of the query's own ORDER BY
     * @return the resulting select
     */
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


    /**
     * Optimises the top-level select: the child is optimised for the projected and ordering variables, orderings on
     * unbound variables are dropped, and DISTINCT is pushed into a {@link SqlDistinct} when the ordering allows it.
     *
     * @param request the current request
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the resulting select
     */
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

        // a slice picks its rows from the multiset, so the multiplicities of the child rows have to be kept exact
        boolean childReduced = reduced && limit == null && (offset == null || offset.equals(BigInteger.ZERO));

        SqlIntercode optChild = child.optimize(request, childRestrictions, childReduced, evalServices);

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


    /**
     * {@code WITH} clause declaring the virtual tables the query reads and, transitively, the virtual tables their
     * definitions read, each one after the tables it depends on; empty when the query reads no virtual table.
     *
     * @param request the current request
     * @return the {@code WITH} clause followed by a space, or the empty string
     * @throws IllegalStateException if a virtual table has no definition in the configuration or the dependencies of
     *             the virtual tables are cyclic
     */
    private String translateWithClause(Request request)
    {
        Set<VirtualTable> tables = getVirtualTables();

        if(tables.isEmpty())
            return "";

        LinkedHashMap<VirtualTable, VirtualTableDefinition> ordered = new LinkedHashMap<>();

        for(VirtualTable table : tables.stream().sorted(comparing(VirtualTable::getName)).toList())
            orderVirtualTable(request.getConfiguration(), table, ordered, new HashSet<>());

        return ordered.entrySet().stream().map(e -> e.getKey() + " AS (" + e.getValue().getQuery() + ")")
                .collect(joining(", ", "WITH ", " "));
    }


    /**
     * Appends the table to the ordered definitions, preceded by the virtual tables its definition reads.
     *
     * @param config the configuration holding the definitions
     * @param table the virtual table
     * @param ordered the definitions collected so far, in declaration order
     * @param visiting the tables on the current dependency path, to detect cycles
     * @throws IllegalStateException if the table has no definition or its dependencies are cyclic
     */
    private static void orderVirtualTable(SparqlDatabaseConfiguration config, VirtualTable table,
            LinkedHashMap<VirtualTable, VirtualTableDefinition> ordered, Set<VirtualTable> visiting)
    {
        if(ordered.containsKey(table))
            return;

        if(!visiting.add(table))
            throw new IllegalStateException("cyclic dependency of virtual table " + table);

        VirtualTableDefinition definition = config.getVirtualTableDefinition(table);

        if(definition == null)
            throw new IllegalStateException("virtual table " + table + " is not defined");

        for(VirtualTable dependency : definition.getDependencies())
            orderVirtualTable(config, dependency, ordered, visiting);

        ordered.put(table, definition);
        visiting.remove(table);
    }



    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        if(isTopLevel())
            builder.append(translateWithClause(request));

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


    /**
     * SELECT list of a top-level select: for each variable, one column group per result class (NULL for classes it
     * cannot take), named by the class.
     *
     * @param description result classes of each projected variable
     * @param bindings the variable bindings
     * @return SELECT list of a top-level select: for each variable, one column group per result class (NULL for classes
     *         it cannot take), named by the class
     */
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


    /**
     * SELECT list of a sub-select: the non-constant columns of the bindings, or {@code 1}.
     *
     * @param bindings the variable bindings
     * @return SELECT list of a sub-select: the non-constant columns of the bindings, or {@code 1}
     */
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


    /**
     * True if the SQL condition is the constant {@code true} or {@code false}, as returned by the null tests of
     * {@link VariableBinding} when they are decided by constant columns.
     *
     * @param condition the SQL condition
     * @return true if the SQL condition is the constant {@code true} or {@code false}, false otherwise
     */
    private static boolean isConstantCondition(String condition)
    {
        return condition.equals("true") || condition.equals("false");
    }


    /**
     * ORDER BY clause over the sortable representation of the variables, optionally followed by the simple ordering.
     *
     * @param withSimple whether to append the simple ordering
     * @return ORDER BY clause over the sortable representation of the variables, optionally followed by the simple
     *         ordering
     */
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
                    sortSet.computeIfAbsent(genBoolean, _ -> new HashSet<>()).add(r);
                else if(isString(r))
                    sortSet.computeIfAbsent(xsdString, _ -> new HashSet<>()).add(r);
                else if(isDate(r))
                    sortSet.computeIfAbsent(genScalarDate, _ -> new HashSet<>()).add(r);
                else if(isDateTime(r))
                    sortSet.computeIfAbsent(genScalarDateTime, _ -> new HashSet<>()).add(r);
                else if(hasBlankNode(r) || hasIri(r) || hasNumeric(r) || hasBoolean(r) || hasString(r) || hasDate(r)
                        || hasDateTime(r))
                    sortSet.computeIfAbsent(box, _ -> new HashSet<>()).add(r);
            }

            if(sortSet.get(box) != null)
                sortSet = Map.of(box, sortSet.values().stream().flatMap(r -> r.stream()).collect(toSet()));


            // order unbounded (a constant condition does not order anything and is not allowed in ORDER BY)
            if(binding.canBeNull() && !isConstantCondition(binding.getIsNotNull()))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.getIsNotNull());

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order blank nodes
            if(sortSet.get(scalarBlankNode) != null
                    && !isConstantCondition(binding.getIsNull(unionize(sortSet.get(scalarBlankNode)))))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.getIsNull(unionize(sortSet.get(scalarBlankNode))));

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
                    builder.append(binding.deriveMapping(unionize(numerics, box)));
                }

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:booleans
            if(sortSet.get(genBoolean) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(genBoolean).get(0));

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
            if(sortSet.get(genScalarDateTime) != null)
            {
                Set<ResourceClass> dateTimes = sortSet.get(genScalarDateTime);

                //FIXME: should be optimized
                ResourceClass sortClass = dateTimes.stream().allMatch(r -> r.isSubclassOf(xsdDateTime)) ? xsdDateTime :
                        genScalarDateTime;

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(sortClass).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order xsd:dates
            if(sortSet.get(genScalarDate) != null)
            {
                Set<ResourceClass> dates = sortSet.get(genScalarDate);

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dates.size() == 1 && dates.iterator().next().getEffectiveClass() instanceof DateInZone)
                    builder.append(binding.deriveMapping(dates.iterator().next()).get(0));
                else
                    builder.append(binding.deriveMapping(genScalarDate).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }

            // order boxed values
            if(sortSet.get(box) != null)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(binding.deriveMapping(unionize(sortSet.get(box), box)).get(0));

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


    /**
     * True for the top-level select.
     *
     * @return true for the top-level select, false otherwise
     */
    private boolean isTopLevel()
    {
        return projections != null;
    }


    /**
     * For each projected variable, the result classes in which it is delivered, in column order (top-level only).
     *
     * @return for each projected variable, the result classes in which it is delivered, in column order (top-level
     *         only)
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        return child.getVirtualTables();
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
