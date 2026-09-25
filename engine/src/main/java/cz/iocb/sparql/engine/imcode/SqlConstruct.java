package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.OBJECT;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.SUBJECT;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasReference;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Instantiates the templates of a CONSTRUCT or DESCRIBE query for every solution of the child, producing the columns
 * subject, predicate and object as a union over the templates; blank nodes of the templates get labels unique per
 * solution and template. Templates that cannot be instantiated (an unbound or literal subject) are dropped.
 */
public final class SqlConstruct extends SqlIntercode
{
    /**
     * The three output columns of a construct.
     */
    public static enum ConstructColumn
    {
        /**
         * Subject column.
         */
        SUBJECT(new Variable("subject")),

        /**
         * Predicate column.
         */
        PREDICATE(new Variable("predicate")),

        /**
         * Object column.
         */
        OBJECT(new Variable("object"));

        /**
         * Variable exposing the column.
         */
        final Variable variable;

        /**
         * Creates the column with its variable.
         *
         * @param variable the variable
         */
        private ConstructColumn(Variable variable)
        {
            this.variable = variable;
        }


        /**
         * Variable exposing the column.
         *
         * @return variable exposing the column
         */
        public Variable getVariable()
        {
            return variable;
        }
    }


    /**
     * Term at a position of a triple template: a constant IRI or literal, a template blank node or a variable.
     *
     * @param <T> type of the value
     */
    public static abstract class RdfTermTemplate<T>
    {
        /**
         * The constant, label or variable.
         */
        private final T value;

        /**
         * Creates the template position.
         *
         * @param value the constant, label or variable
         */
        RdfTermTemplate(T value)
        {
            this.value = value;
        }


        /**
         * The constant, label or variable.
         *
         * @return the constant, label or variable
         */
        public T getValue()
        {
            return value;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            RdfTermTemplate<?> other = (RdfTermTemplate<?>) object;

            return Objects.equals(value, other.value);
        }
    }


    /**
     * Constant IRI in a template.
     */
    public static class IriTemplate extends RdfTermTemplate<Iri>
    {
        /**
         * Creates the template position.
         *
         * @param iri the IRI
         */
        public IriTemplate(Iri iri)
        {
            super(iri);
        }
    }


    /**
     * Constant literal in a template.
     */
    public static class LiteralTemplate extends RdfTermTemplate<Literal>
    {
        /**
         * Creates the template position.
         *
         * @param literal the literal
         */
        public LiteralTemplate(Literal literal)
        {
            super(literal);
        }
    }


    /**
     * Blank node of a template, identified by its label; a fresh node is created per solution.
     */
    public static class BlankNodeTemplate extends RdfTermTemplate<String>
    {
        /**
         * Creates the template position.
         *
         * @param label label of the blank node
         */
        public BlankNodeTemplate(String label)
        {
            super(label);
        }
    }


    /**
     * Variable of a template, taking the value from the solution.
     */
    public static class VariableTemplate extends RdfTermTemplate<Variable>
    {
        /**
         * Creates the template position.
         *
         * @param variable the variable
         */
        public VariableTemplate(Variable variable)
        {
            super(variable);
        }
    }



    /**
     * Triple template.
     *
     * @param subject the subject template
     * @param predicate the predicate template
     * @param object the object template
     */
    public static record Template(RdfTermTemplate<?> subject, RdfTermTemplate<?> predicate, RdfTermTemplate<?> object)
    {
        /**
         * Template position of the column.
         *
         * @param column the column
         * @return template position of the column
         */
        RdfTermTemplate<?> get(ConstructColumn column)
        {
            return switch(column)
            {
                case SUBJECT -> subject;
                case PREDICATE -> predicate;
                case OBJECT -> object;
            };
        }
    }


    /**
     * The output variables in order.
     */
    private static final List<Variable> columns = List.of(SUBJECT.getVariable(), PREDICATE.getVariable(),
            OBJECT.getVariable());

    /**
     * Solutions the templates are instantiated for.
     */
    private final SqlIntercode child;

    /**
     * Templates that can produce triples.
     */
    private final List<Template> templates;

    /**
     * Per template, the output column each union column is taken from.
     */
    private final List<Map<Column, Column>> columnMappings;

    /**
     * Counter giving template blank nodes distinct segments, shared by nested constructs.
     */
    private final AtomicInteger bnOffset;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param child the child node
     * @param templates the triple templates
     * @param columnMappings per branch, the branch column each output column is taken from
     * @param bnOffset counter giving template blank nodes distinct segments
     */
    protected SqlConstruct(VariableBindings bindings, SqlIntercode child, List<Template> templates,
            List<Map<Column, Column>> columnMappings, AtomicInteger bnOffset)
    {
        super(bindings, child.isDeterministic);

        this.child = child;
        this.templates = templates;
        this.columnMappings = columnMappings;
        this.bnOffset = bnOffset;
    }


    /**
     * Construct of the templates over the child's solutions.
     *
     * @param request the current request
     * @param templates the triple templates
     * @param child the child node
     * @return construct of the templates over the child's solutions
     */
    public static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child)
    {
        return construct(request, templates, child, new AtomicInteger(0));
    }


    /**
     * Construct restricted to the valid templates; the output variables get the disjoint classes of all templates.
     *
     * @param request the current request
     * @param templates the triple templates
     * @param child the child node
     * @param bnOffset counter giving template blank nodes distinct segments
     * @return construct restricted to the valid templates; the output variables get the disjoint classes of all
     *         templates
     */
    protected static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child,
            AtomicInteger bnOffset)
    {
        List<VariableBindings> branches = new ArrayList<>(templates.size());
        Map<BlankNodeTemplate, ResourceClass> bnClasses = new HashMap<>();

        for(Template template : templates)
        {
            if(!isValidTemplate(template, child))
                continue;

            VariableBindings bindings = new VariableBindings();

            for(ConstructColumn column : ConstructColumn.values())
                bindings.add(getVariableBinding(request, column, template.get(column), bnOffset, bnClasses, child));

            branches.add(bindings);
        }


        Map<Variable, Set<ResourceClass>> classes = new HashMap<>();

        for(Variable var : columns)
        {
            Set<ResourceClass> resources = new HashSet<>();

            for(VariableBindings branche : branches)
            {
                VariableBinding binding = branche.get(var);

                if(binding != null)
                    resources.addAll(binding.getClasses());
            }

            classes.put(var, ResourceClass.getDisjunctClasses(resources));
        }


        Map<List<Column>, Column> unionColumns = new HashMap<>();
        List<Map<Column, Column>> columnMappings = new ArrayList<>(branches.size());

        for(int i = 0; i < branches.size(); i++)
            columnMappings.add(new HashMap<>());

        VariableBindings bindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> entry : classes.entrySet())
        {
            Variable variable = entry.getKey();
            List<VariableBinding> vars = branches.stream().map(c -> c.get(variable)).toList();

            boolean canBeNull = vars.stream().anyMatch(v -> v == null || v.canBeNull());
            VariableBinding variableBinding = new VariableBinding(variable, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                if(vars.stream().filter(Objects::nonNull).flatMap(v -> v.getMappings().entrySet().stream())
                        .anyMatch(r -> isNull(r.getValue()) && !areDisjunct(r.getKey(), resourceClass)))
                {
                    variableBinding.addMapping(resourceClass, null);
                    continue;
                }


                List<List<Column>> cols = new ArrayList<>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                    cols.add(new ArrayList<>(branches.size()));


                for(VariableBinding binding : vars)
                {
                    if(binding == null)
                    {
                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(new NullColumn(resourceClass.getSqlTypes().get(i)));
                    }
                    else
                    {
                        List<Column> c = binding.deriveMapping(resourceClass);

                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(c.get(i));
                    }
                }


                List<Column> columns = resourceClass.createColumns(request.getColumnMap(),
                        variableBinding.getVariable());
                List<Column> mapping = new ArrayList<>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> c = cols.get(i);

                    if(c.get(0) instanceof ConstantColumn c0 && c.stream().allMatch(d -> d.equals(c0)))
                    {
                        mapping.add(c.get(0));
                    }
                    else if(unionColumns.containsKey(c))
                    {
                        mapping.add(unionColumns.get(c));
                    }
                    else
                    {
                        Column col = columns.get(i);
                        unionColumns.put(c, col);
                        mapping.add(col);

                        for(int j = 0; j < branches.size(); j++)
                            columnMappings.get(j).put(col, c.get(j));
                    }
                }

                variableBinding.addMapping(resourceClass, mapping);
            }

            bindings.add(variableBinding);
        }

        return new SqlConstruct(bindings, child, templates, columnMappings, bnOffset);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        List<Template> optTemplates = templates;
        SqlIntercode optChild = child;
        Restrictions childRestrictions = getTemplateRestrictions(optTemplates, optChild.getVariableBindings());

        while(true)
        {
            optChild = optChild.optimize(request, childRestrictions, true, evalServices);
            optTemplates = getValidTemplates(optTemplates, optChild);

            Restrictions newRestrictions = getTemplateRestrictions(optTemplates, optChild.getVariableBindings());

            if(newRestrictions.equals(childRestrictions))
                break;

            childRestrictions = newRestrictions;
        }


        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optTemplates.isEmpty())
            return SqlNoSolution.get();

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                childs.add(construct(request, optTemplates, child, bnOffset));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }

        if(optTemplates.size() == 1 && optChild instanceof SqlTableAccess acc)
        {
            Template template = optTemplates.get(0);

            DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();
            Map<BlankNodeTemplate, ResourceClass> bnClasses = new HashMap<>();

            VariableBindings internal = new VariableBindings();
            Conditions conditions = acc.getConditions();

            for(ConstructColumn column : ConstructColumn.values())
            {
                if(template.get(column) instanceof VariableTemplate variable)
                {
                    VariableBinding original = acc.getInternalVariableBinding(variable.getValue());
                    VariableBinding mapped = getInternalVariableBinding(column, original.getMappings());
                    conditions = Conditions.and(conditions, createConditions(schema, acc.getTable(), mapped));
                    internal.add(mapped);
                }
                else
                {
                    internal.add(
                            getVariableBinding(request, column, template.get(column), bnOffset, bnClasses, optChild));
                }
            }

            return SqlTableAccess.create(request, acc.getTable(), conditions, internal, true, acc.getDistinctColumns())
                    .optimize(request, restrictions, reduced, evalServices);
        }


        if(optTemplates.equals(templates) && optChild == child)
            return this;

        return construct(request, optTemplates, optChild, bnOffset);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        boolean canBeNull = bindings.getValues().stream().anyMatch(v -> v.canBeNull());
        Set<Column> columns = bindings.getNonConstantColumns();

        builder.append("SELECT ");

        if(canBeNull)
        {
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));

            if(columns.isEmpty())
                builder.append("1");

            builder.append(" FROM (SELECT ");
        }

        boolean hasSelect = false;

        for(Column column : columns)
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            List<Column> cols = new ArrayList<>();

            for(int i = 0; i < columnMappings.size(); i++)
                cols.add(columnMappings.get(i).get(column));

            if(Collections.frequency(cols, cols.get(0)) == cols.size())
                builder.append(cols.get(0));
            else
                builder.append(cols.stream().map(Object::toString).collect(joining(", ", "unnest(array[", "])")));

            builder.append(" AS ");
            builder.append(column);
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(") AS tab");

        if(canBeNull)
        {
            builder.append(") AS tab WHERE ");

            boolean hasFilter = false;

            for(VariableBinding binding : bindings.getValues())
            {
                if(binding.canBeNull())
                {
                    appendAnd(builder, hasFilter);
                    hasFilter = true;

                    builder.append(binding.getIsNotNull());
                }
            }
        }

        return builder.toString();
    }


    /**
     * The output variables subject, predicate and object.
     *
     * @return the output variables subject, predicate and object
     */
    public static List<Variable> getColumns()
    {
        return columns;
    }


    /**
     * Restrictions for the child: each template variable in the classes usable at its positions.
     *
     * @param templates the triple templates
     * @param bindings the variable bindings
     * @return restrictions for the child: each template variable in the classes usable at its positions
     */
    private static Restrictions getTemplateRestrictions(Collection<Template> templates, VariableBindings bindings)
    {
        Restrictions restrictions = new Restrictions();

        for(Template template : templates)
            for(ConstructColumn column : ConstructColumn.values())
                if(template.get(column) instanceof VariableTemplate var)
                    restrictions.add(var.getValue(), filterResourceClasses(column, bindings.get(var.getValue())));

        return restrictions;
    }


    /**
     * Classes of the binding usable at the column: references for subjects, IRIs for predicates.
     *
     * @param column the output column
     * @param binding the variable binding
     * @return classes of the binding usable at the column: references for subjects, IRIs for predicates
     */
    private static Set<ResourceClass> filterResourceClasses(ConstructColumn column, VariableBinding binding)
    {
        Set<ResourceClass> result = new HashSet<>();

        if(binding != null)
        {
            for(ResourceClass resClass : binding.getMappings().keySet())
                if((column != SUBJECT || hasReference(resClass)) && (column != PREDICATE || hasIri(resClass)))
                    result.add(resClass);
        }

        return result;
    }


    /**
     * Templates that can produce triples for the child.
     *
     * @param templates the triple templates
     * @param child the child node
     * @return templates that can produce triples for the child
     */
    private static List<Template> getValidTemplates(Collection<Template> templates, SqlIntercode child)
    {
        return templates.stream().filter(t -> isValidTemplate(t, child)).toList();
    }


    /**
     * True if every position of the template can produce a term.
     *
     * @param template the triple template
     * @param child the child node
     * @return true if every position of the template can produce a term, false otherwise
     */
    private static boolean isValidTemplate(Template template, SqlIntercode child)
    {
        for(ConstructColumn column : ConstructColumn.values())
            if(!isValidTemplateVariable(column, template.get(column), child))
                return false;

        return true;
    }


    /**
     * True if the position can produce a term: literals only as objects, variables bound to a suitable class.
     *
     * @param column the output column
     * @param rdfTermTemplate the template position
     * @param child the child node
     * @return true if the position can produce a term, false otherwise
     */
    private static boolean isValidTemplateVariable(ConstructColumn column, RdfTermTemplate<?> rdfTermTemplate,
            SqlIntercode child)
    {
        switch(rdfTermTemplate)
        {
            case IriTemplate _ ->
            {
                return true;
            }

            case LiteralTemplate _ ->
            {
                return column == OBJECT;
            }

            case BlankNodeTemplate _ ->
            {
                return column != PREDICATE;
            }

            case VariableTemplate variable ->
            {
                VariableBinding binding = child.getVariable(variable.getValue());

                if(binding == null)
                    return false;

                for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
                    if((column != SUBJECT || hasReference(e.getKey())) && (column != PREDICATE || hasIri(e.getKey())))
                        return true;

                return false;
            }

            default ->
            {
                throw new IllegalArgumentException();
            }
        }
    }


    /**
     * Binding of the output column for the template position: constant columns for IRIs and literals, a blank node
     * built from the row number for blank node templates, the variable's usable classes otherwise.
     *
     * @param request the current request
     * @param column the output column
     * @param rdfTermTemplate the template position
     * @param bnOffset counter giving template blank nodes distinct segments
     * @param bnResourceClasses classes assigned to the template blank nodes so far
     * @param child the child node
     * @return binding of the output column for the template position: constant columns for IRIs and literals, a blank
     *         node built from the row number for blank node templates, the variable's usable classes otherwise
     */
    private static VariableBinding getVariableBinding(Request request, ConstructColumn column,
            RdfTermTemplate<?> rdfTermTemplate, AtomicInteger bnOffset,
            Map<BlankNodeTemplate, ResourceClass> bnResourceClasses, SqlIntercode child)
    {
        switch(rdfTermTemplate)
        {
            case IriTemplate template ->
            {
                Iri iri = template.getValue();
                ResourceClass iriClass = request.getIriClass(iri);
                List<Column> columns = request.getColumns(iriClass, iri);
                return new VariableBinding(column.getVariable(), iriClass, columns, false);
            }

            case LiteralTemplate template ->
            {
                if(column != OBJECT)
                    return null;

                Literal literal = template.getValue();
                ResourceClass resClass = request.getLiteralClass(literal);
                List<Column> columns = request.getColumns(resClass, literal);
                return new VariableBinding(column.getVariable(), resClass, columns, false);
            }

            case BlankNodeTemplate bnode ->
            {
                if(column == PREDICATE)
                    return null;

                ResourceClass resClass = bnResourceClasses.computeIfAbsent(bnode,
                        _ -> new IntBlankNodeInSegmentClass(bnOffset.decrementAndGet()));

                List<Column> columns = List.of(new ExpressionColumn("(row_number() OVER ())::int4"));
                return new VariableBinding(column.getVariable(), resClass, columns, false);
            }

            case VariableTemplate variable ->
            {
                VariableBinding binding = child.getVariable(variable.getValue());

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(binding == null)
                    return null;

                for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
                    if((column != SUBJECT || hasReference(e.getKey())) && (column != PREDICATE || hasIri(e.getKey())))
                        mappings.put(e.getKey(), e.getValue());

                if(mappings.isEmpty())
                    return null;

                boolean canBeNull = binding.canBeNull() || mappings.size() < binding.getMappings().size();
                return new VariableBinding(column.getVariable(), mappings, canBeNull);
            }

            default ->
            {
                return null;
            }
        }
    }


    /**
     * The mappings filtered to the classes usable at the column.
     *
     * @param column the output column
     * @param original the original mappings
     * @return the mappings filtered to the classes usable at the column
     */
    private static VariableBinding getInternalVariableBinding(ConstructColumn column,
            Map<ResourceClass, List<Column>> original)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, List<Column>> e : original.entrySet())
            if((column != SUBJECT || hasReference(e.getKey())) && (column != PREDICATE || hasIri(e.getKey())))
                mappings.put(e.getKey(), e.getValue());

        return new VariableBinding(column.getVariable(), mappings, false);
    }


    /**
     * Conditions that the nullable columns of some class of the binding are not null, i.e. the variable is bound.
     *
     * @param schema the database schema
     * @param table the table
     * @param binding the variable binding
     * @return conditions that the nullable columns of some class of the binding are not null, i.e. the variable is
     *         bound
     */
    private static Conditions createConditions(DatabaseSchema schema, SourceTable table, VariableBinding binding)
    {
        Conditions conditions = new Conditions(false);

        for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
        {
            ResourceClass resClass = entry.getKey();
            List<Column> cols = entry.getValue();

            Condition condition = new Condition();

            for(int i = 0; i < cols.size(); i++)
                if(!resClass.isOptionalColumn(i) && schema.isNullableColumn(table, cols.get(i)))
                    condition.addIsNotNull(cols.get(i));

            conditions.add(condition);
        }

        return conditions;
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
        builder.append("construct subject predicate object");

        for(Template template : templates)
        {
            indentInfo(builder, indent, true);

            for(ConstructColumn column : ConstructColumn.values())
            {
                if(column != ConstructColumn.SUBJECT)
                    builder.append(" ");

                builder.append(switch(template.get(column))
                {
                    case LiteralTemplate literal -> literal.getValue();
                    case IriTemplate iri -> iri.getValue().toString();
                    case BlankNodeTemplate bnode -> "_:" + bnode.getValue();
                    case VariableTemplate variable -> "?" + variable.getValue();
                    default -> throw new IllegalArgumentException();
                });
            }
        }

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlConstruct imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(bnOffset, imcode.bnOffset))
            return false;

        if(!Objects.equals(new Multiset<>(templates), new Multiset<>(imcode.templates)))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(bnOffset, new Multiset<>(templates), child);
    }
}
