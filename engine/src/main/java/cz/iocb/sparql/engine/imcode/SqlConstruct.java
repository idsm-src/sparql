package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.OBJECT;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.SUBJECT;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
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
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeConstantSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlConstruct extends SqlIntercode
{
    public static enum ConstructColumn
    {
        SUBJECT(new Variable("subject"), true, false),
        PREDICATE(new Variable("predicate"), false, false),
        OBJECT(new Variable("object"), true, true);

        final Variable variable;
        final boolean allowBlankNode;
        final boolean allowLiteral;

        private ConstructColumn(Variable variable, boolean allowBlankNode, boolean allowLiteral)
        {
            this.variable = variable;
            this.allowBlankNode = allowBlankNode;
            this.allowLiteral = allowLiteral;
        }

        public Variable getVariable()
        {
            return variable;
        }

        public boolean isBlankNodeAllowed()
        {
            return allowBlankNode;
        }

        public boolean isLiteralAllowed()
        {
            return allowLiteral;
        }
    }


    public static abstract class RdfTermTemplate<T>
    {
        private final T value;

        RdfTermTemplate(T value)
        {
            this.value = value;
        }

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


    public static class IriTemplate extends RdfTermTemplate<Iri>
    {
        public IriTemplate(Iri iri)
        {
            super(iri);
        }
    }


    public static class LiteralTemplate extends RdfTermTemplate<Literal>
    {
        public LiteralTemplate(Literal literal)
        {
            super(literal);
        }
    }


    public static class BlankNodeTemplate extends RdfTermTemplate<String>
    {
        public BlankNodeTemplate(String label)
        {
            super(label);
        }
    }


    public static class VariableTemplate extends RdfTermTemplate<Variable>
    {
        public VariableTemplate(Variable variable)
        {
            super(variable);
        }
    }



    public static record Template(RdfTermTemplate<?> subject, RdfTermTemplate<?> predicate, RdfTermTemplate<?> object)
    {
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


    private static final List<Variable> columns = List.of(SUBJECT.getVariable(), PREDICATE.getVariable(),
            OBJECT.getVariable());

    private final SqlIntercode child;
    private final List<Template> templates;
    private final List<Map<Column, Column>> columnMappings;
    private final AtomicInteger bnOffset;


    protected SqlConstruct(VariableBindings bindings, SqlIntercode child, List<Template> templates,
            List<Map<Column, Column>> columnMappings, AtomicInteger bnOffset)
    {
        super(bindings, child.isDeterministic);

        this.child = child;
        this.templates = templates;
        this.columnMappings = columnMappings;
        this.bnOffset = bnOffset;
    }


    public static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child)
    {
        return construct(request, templates, child, new AtomicInteger(0));
    }


    protected static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child,
            AtomicInteger bnOffset)
    {
        List<VariableBindings> branches = new ArrayList<>(templates.size());
        Map<BlankNodeTemplate, ResourceClass> bnClasses = new HashMap<>();

        for(Template template : templates)
        {
            VariableBindings bindings = new VariableBindings();

            for(ConstructColumn column : ConstructColumn.values())
            {
                VariableBinding binding = getVariableBinding(request, column, template.get(column), bnOffset, bnClasses,
                        child);

                if(binding != null)
                    bindings.add(binding);
            }

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
                            cols.get(i).add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
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

            return SqlTableAccess.create(acc.getTable(), conditions, internal, true).optimize(request, restrictions,
                    reduced, evalServices);
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

                    builder.append("(");

                    boolean hasVariant = false;

                    for(List<Column> cols : binding.getMappings().values())
                    {
                        appendOr(builder, hasVariant);
                        hasVariant = true;

                        builder.append(cols.stream().map(c -> c + " IS NOT NULL").collect(joining(" AND ", "(", ")")));
                    }

                    if(!hasVariant)
                        builder.append("false");

                    builder.append(")");
                }
            }
        }

        return builder.toString();
    }


    public static List<Variable> getColumns()
    {
        return columns;
    }


    private static Restrictions getTemplateRestrictions(Collection<Template> templates, VariableBindings bindings)
    {
        Restrictions restrictions = new Restrictions();

        for(Template template : templates)
            for(ConstructColumn column : ConstructColumn.values())
                if(template.get(column) instanceof VariableTemplate var)
                    restrictions.add(var.getValue(), filterResourceClasses(column, bindings.get(var.getValue())));

        return restrictions;
    }


    private static Set<ResourceClass> filterResourceClasses(ConstructColumn column, VariableBinding binding)
    {
        Set<ResourceClass> result = new HashSet<>();

        if(binding != null)
        {
            for(ResourceClass resClass : binding.getMappings().keySet())
                if((column.isLiteralAllowed() || !(resClass instanceof LiteralClass))
                        && (column.isBlankNodeAllowed() || !(resClass instanceof BlankNodeClass)))
                    result.add(resClass);
        }

        return result;
    }


    private static List<Template> getValidTemplates(Collection<Template> templates, SqlIntercode child)
    {
        return templates.stream().filter(t -> isValidTemplate(t, child)).toList();
    }


    private static boolean isValidTemplate(Template template, SqlIntercode child)
    {
        for(ConstructColumn column : ConstructColumn.values())
            if(!isValidTemplateVariable(column, template.get(column), child))
                return false;

        return true;
    }


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
                return column.isLiteralAllowed();
            }

            case BlankNodeTemplate _ ->
            {
                return column.isBlankNodeAllowed();
            }

            case VariableTemplate variable ->
            {
                VariableBinding binding = child.getVariable(variable.getValue());

                if(binding == null)
                    return false;

                for(Entry<ResourceClass, List<Column>> map : binding.getMappings().entrySet())
                    if((column.isLiteralAllowed() || !isLiteral(map.getKey()))
                            && (column.isBlankNodeAllowed() || !isBlankNode(map.getKey())))
                        return true;

                return false;
            }

            default ->
            {
                throw new IllegalArgumentException();
            }
        }
    }


    private static VariableBinding getVariableBinding(Request request, ConstructColumn column,
            RdfTermTemplate<?> rdfTermTemplate, AtomicInteger bnOffset,
            Map<BlankNodeTemplate, ResourceClass> bnResourceClasses, SqlIntercode child)
    {
        switch(rdfTermTemplate)
        {
            case IriTemplate template ->
            {
                Iri iri = template.getValue();
                IriClass iriClass = request.getIriClass(iri);
                List<Column> columns = request.getColumns(iriClass, iri);
                return new VariableBinding(column.getVariable(), iriClass, columns, false);
            }

            case LiteralTemplate template ->
            {
                if(!column.isLiteralAllowed())
                    return null;

                Literal literal = template.getValue();
                LiteralClass resClass = request.getLiteralClass(literal);
                List<Column> columns = request.getColumns(resClass, literal);
                return new VariableBinding(column.getVariable(), resClass, columns, false);
            }

            case BlankNodeTemplate bnode ->
            {
                if(!column.isBlankNodeAllowed())
                    return null;

                ResourceClass resClass = bnResourceClasses.computeIfAbsent(bnode,
                        _ -> new IntBlankNodeConstantSegmentClass(bnOffset.decrementAndGet()));

                List<Column> columns = List.of(new ExpressionColumn("(row_number() OVER ())::int4"));
                return new VariableBinding(column.getVariable(), resClass, columns, false);
            }

            case VariableTemplate variable ->
            {
                VariableBinding binding = child.getVariable(variable.getValue());

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(binding == null)
                    return null;

                for(Entry<ResourceClass, List<Column>> map : binding.getMappings().entrySet())
                {
                    if(!column.isLiteralAllowed() && map.getKey() instanceof LiteralClass)
                        continue;

                    if(!column.isBlankNodeAllowed() && map.getKey() instanceof BlankNodeClass)
                        continue;

                    mappings.put(map.getKey(), map.getValue());
                }

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


    private static VariableBinding getInternalVariableBinding(ConstructColumn column,
            Map<ResourceClass, List<Column>> original)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, List<Column>> map : original.entrySet())
        {
            if(!column.isLiteralAllowed() && map.getKey() instanceof LiteralClass)
                continue;

            if(!column.isBlankNodeAllowed() && map.getKey() instanceof BlankNodeClass)
                continue;

            mappings.put(map.getKey(), map.getValue());
        }

        return new VariableBinding(column.getVariable(), mappings, false);
    }


    private static Conditions createConditions(DatabaseSchema schema, Table table, VariableBinding binding)
    {
        Conditions conditions = new Conditions(false);

        for(List<Column> cols : binding.getMappings().values())
        {
            Condition condition = new Condition();

            for(Column column : cols)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

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
