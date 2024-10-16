package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.translator.imcode.SqlConstruct.ConstructColumn.OBJECT;
import static cz.iocb.sparql.engine.translator.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.translator.imcode.SqlConstruct.ConstructColumn.SUBJECT;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.BlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlConstruct extends SqlIntercode
{
    protected static enum ConstructColumn
    {
        SUBJECT("subject", true, false), PREDICATE("predicate", false, false), OBJECT("object", true, true);

        final String name;
        final boolean allowBlankNode;
        final boolean allowLiteral;

        private ConstructColumn(String name, boolean allowBlankNode, boolean allowLiteral)
        {
            this.name = name;
            this.allowBlankNode = allowBlankNode;
            this.allowLiteral = allowLiteral;
        }

        String getName()
        {
            return name;
        }

        boolean isBlankNodeAllowed()
        {
            return allowBlankNode;
        }

        boolean isLiteralAllowed()
        {
            return allowLiteral;
        }
    }


    public static record Template(Node subject, Node predicate, Node object)
    {
        Node get(ConstructColumn column)
        {
            return switch(column)
            {
                case SUBJECT -> subject;
                case PREDICATE -> predicate;
                case OBJECT -> object;
            };
        }
    }


    private static final List<String> columns = List.of(SUBJECT.getName(), PREDICATE.getName(), OBJECT.getName());

    private final SqlIntercode child;
    private final List<Template> templates;
    private final List<Map<Column, Column>> columnMappings;
    private final AtomicInteger bnOffset;


    protected SqlConstruct(UsedVariables variables, SqlIntercode child, List<Template> templates,
            List<Map<Column, Column>> columnMappings, AtomicInteger bnOffset)
    {
        super(variables, child.isDeterministic);

        this.child = child;
        this.templates = templates;
        this.columnMappings = columnMappings;
        this.bnOffset = bnOffset;
    }


    public static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child)
    {
        return construct(request, templates, new AtomicInteger(), child);
    }


    public static SqlIntercode construct(Request request, List<Template> templates, AtomicInteger bnOffset,
            SqlIntercode child)
    {
        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child instanceof SqlUnion union)
            return SqlUnion.union(
                    union.getChilds().stream().map(c -> construct(request, templates, bnOffset, c)).collect(toList()));


        List<UsedVariables> branches = new ArrayList<UsedVariables>(templates.size());
        List<Template> validTemplates = new ArrayList<Template>();
        Map<String, ResourceClass> bnClasses = new HashMap<String, ResourceClass>();

        for(Template template : templates)
        {
            UsedVariables vars = new UsedVariables();

            for(ConstructColumn column : ConstructColumn.values())
            {
                UsedVariable var = getUsedVariable(request, column, template.get(column), bnOffset, bnClasses, child);

                if(var != null)
                    vars.add(var);
            }

            if(vars.getValues().size() != ConstructColumn.values().length)
                continue;

            branches.add(vars);
            validTemplates.add(template);
        }

        if(validTemplates.isEmpty())
            return SqlNoSolution.get();


        if(validTemplates.size() == 1 && child instanceof SqlTableAccess acc)
        {
            DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

            UsedVariables branch = branches.get(0);
            Template template = validTemplates.get(0);

            UsedVariables internal = new UsedVariables();
            Conditions conditions = acc.getConditions();

            for(ConstructColumn column : ConstructColumn.values())
            {
                if(template.get(column) instanceof Variable variable)
                {
                    UsedVariable original = acc.getInternalVariable(variable.getSqlName());
                    UsedVariable mapped = getInternalVariable(column, original.getMappings());
                    conditions = Conditions.and(conditions, createConditions(schema, acc.getTable(), mapped));
                    internal.add(mapped);
                }
                else
                {
                    internal.add(branch.get(column.getName()));
                }
            }

            return SqlTableAccess.create(acc.getTable(), conditions, internal, true);
        }


        Map<String, Set<ResourceClass>> classes = new HashMap<String, Set<ResourceClass>>();

        for(String varName : columns)
        {
            Set<ResourceClass> set = new HashSet<ResourceClass>();
            classes.put(varName, set);

            Set<ResourceClass> resources = new HashSet<ResourceClass>();

            for(UsedVariables branche : branches)
            {
                UsedVariable var = branche.get(varName);

                if(var != null)
                    resources.addAll(var.getClasses());
            }

            for(ResourceClass res : resources)
            {
                if(resources.contains(res.getGeneralClass()))
                    set.add(res.getGeneralClass());
                else
                    set.add(res);
            }
        }


        UsedVariables variables = new UsedVariables();
        Map<List<Column>, Column> unionColumns = new HashMap<List<Column>, Column>();
        List<Map<Column, Column>> columnMappings = new ArrayList<Map<Column, Column>>(branches.size());

        for(int i = 0; i < branches.size(); i++)
            columnMappings.add(new HashMap<Column, Column>());

        for(Entry<String, Set<ResourceClass>> entry : classes.entrySet())
        {
            String name = entry.getKey();
            boolean canBeNull = branches.stream().anyMatch(i -> i.get(name) == null || i.get(name).canBeNull());
            UsedVariable variable = new UsedVariable(name, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                List<Column> columns = resourceClass.createColumns(variable.getName());
                List<Column> mapping = new ArrayList<Column>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> cols = new ArrayList<Column>(branches.size());

                    for(UsedVariables branche : branches)
                    {
                        UsedVariable var = branche.get(name);

                        if(var == null)
                        {
                            cols.add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
                        }
                        else if(var.containsClass(resourceClass))
                        {
                            cols.add(var.getMapping(resourceClass).get(i));
                        }
                        else
                        {
                            Set<ResourceClass> variants = var.getCompatibleClasses(resourceClass);

                            if(variants.isEmpty())
                            {
                                cols.add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
                            }
                            else
                            {
                                StringBuilder builder = new StringBuilder();

                                if(variants.size() > 1)
                                    builder.append("coalesce(");

                                boolean hasAlternative = false;

                                for(ResourceClass variant : variants)
                                {
                                    appendComma(builder, hasAlternative);
                                    hasAlternative = true;

                                    builder.append(
                                            variant.toGeneralClass(var.getMapping(variant), var.canBeNull()).get(i));
                                }

                                if(variants.size() > 1)
                                    builder.append(")");

                                cols.add(new ExpressionColumn(builder.toString()));
                            }
                        }
                    }


                    if(cols.get(0) instanceof ConstantColumn && Collections.frequency(cols, cols.get(0)) == cols.size())
                    {
                        mapping.add(cols.get(0));
                    }
                    else if(unionColumns.containsKey(cols))
                    {
                        Column col = unionColumns.get(cols);
                        mapping.add(col);
                    }
                    else
                    {
                        Column col = columns.get(i);
                        unionColumns.put(cols, col);
                        mapping.add(col);

                        for(int j = 0; j < branches.size(); j++)
                            columnMappings.get(j).put(col, cols.get(j));
                    }
                }

                variable.addMapping(resourceClass, mapping);
            }

            variables.add(variable);
        }

        return new SqlConstruct(variables, child, validTemplates, columnMappings, bnOffset);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        Set<String> childRestrictions = new HashSet<String>();

        for(Template template : templates)
            for(ConstructColumn column : ConstructColumn.values())
                if(template.get(column) instanceof Variable variable)
                    childRestrictions.add(variable.getSqlName());

        return construct(request, templates, bnOffset, child.optimize(request, childRestrictions, true));
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        boolean canBeNull = variables.getValues().stream().anyMatch(v -> v.canBeNull());
        Set<Column> columns = variables.getNonConstantColumns();

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

            List<Column> cols = new ArrayList<Column>();

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

            for(UsedVariable variable : variables.getValues())
            {
                if(variable.canBeNull())
                {
                    appendAnd(builder, hasFilter);
                    hasFilter = true;

                    builder.append("(");

                    boolean hasVariant = false;

                    for(List<Column> cols : variable.getMappings().values())
                    {
                        appendOr(builder, hasVariant);
                        hasVariant = true;

                        builder.append(cols.stream().map(c -> c + " IS NOT NULL").collect(joining(" AND ", "(", ")")));
                    }

                    builder.append(")");
                }
            }
        }

        return builder.toString();
    }


    public static List<String> getColumns()
    {
        return columns;
    }


    private static UsedVariable getUsedVariable(Request request, ConstructColumn column, Node node,
            AtomicInteger bnOffset, Map<String, ResourceClass> bnResourceClasses, SqlIntercode child)
    {
        switch(node)
        {
            case IRI iri ->
            {
                IriClass iriClass = request.getIriClass(iri);
                List<Column> columns = request.getColumns(iriClass, iri);
                return new UsedVariable(column.getName(), iriClass, columns, false);
            }

            case Literal literal ->
            {
                if(!column.isLiteralAllowed())
                    return null;

                DataType dataType = request.getConfiguration().getDataType(literal.getTypeIri());
                LiteralClass resClass = dataType == null ? unsupportedLiteral : dataType.getResourceClass(literal);
                List<Column> columns = request.getColumns(resClass, literal);
                return new UsedVariable(column.getName(), resClass, columns, false);
            }

            case BlankNode bnode ->
            {
                if(!column.isBlankNodeAllowed())
                    return null;

                String name = bnode.getName();
                ResourceClass resClass = bnResourceClasses.get(name);

                if(resClass == null)
                {
                    resClass = new UserIntBlankNodeClass(bnOffset.decrementAndGet());
                    bnResourceClasses.put(name, resClass);
                }

                List<Column> columns = List.of(new ExpressionColumn("(row_number() OVER ())::int4"));
                return new UsedVariable(column.getName(), resClass, columns, false);
            }

            case Variable variable ->
            {
                UsedVariable var = child.getVariable(variable.getSqlName());

                Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

                if(var == null)
                    return null;

                for(Entry<ResourceClass, List<Column>> map : var.getMappings().entrySet())
                {
                    if(!column.isLiteralAllowed() && map.getKey() instanceof LiteralClass)
                        continue;

                    if(!column.isBlankNodeAllowed() && map.getKey() instanceof BlankNodeClass)
                        continue;

                    mappings.put(map.getKey(), map.getValue());
                }

                if(mappings.isEmpty())
                    return null;

                boolean canBeNull = var.canBeNull() || mappings.size() < var.getMappings().size();
                return new UsedVariable(column.getName(), mappings, canBeNull);
            }

            default ->
            {
                return null;
            }
        }
    }


    private static UsedVariable getInternalVariable(ConstructColumn column, Map<ResourceClass, List<Column>> original)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

        for(Entry<ResourceClass, List<Column>> map : original.entrySet())
        {
            if(!column.isLiteralAllowed() && map.getKey() instanceof LiteralClass)
                continue;

            if(!column.isBlankNodeAllowed() && map.getKey() instanceof BlankNodeClass)
                continue;

            mappings.put(map.getKey(), map.getValue());
        }

        return new UsedVariable(column.getName(), mappings, false);
    }


    private static Conditions createConditions(DatabaseSchema schema, Table table, UsedVariable var)
    {
        Conditions conditions = new Conditions();

        for(List<Column> cols : var.getMappings().values())
        {
            Condition condition = new Condition();

            for(Column column : cols)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

            conditions.add(condition);
        }

        return conditions;
    }
}
