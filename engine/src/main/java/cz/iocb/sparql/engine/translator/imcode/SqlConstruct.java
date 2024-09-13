package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
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
    public static record Template(Node subject, Node predicate, Node object)
    {
    }


    private final SqlIntercode child;
    private final List<Template> templates;
    private final List<Map<Column, Column>> columnMappings;


    protected SqlConstruct(UsedVariables variables, SqlIntercode child, List<Template> templates,
            List<Map<Column, Column>> columnMappings)
    {
        super(variables, child.isDeterministic);

        this.child = child;
        this.templates = templates;
        this.columnMappings = columnMappings;
    }


    public static SqlIntercode construct(Request request, List<Template> templates, SqlIntercode child)
    {
        List<UsedVariables> branches = new ArrayList<UsedVariables>(templates.size());
        List<Template> validTemplates = new ArrayList<Template>();
        Map<String, ResourceClass> bnClasses = new HashMap<String, ResourceClass>();

        for(Template template : templates)
        {
            UsedVariable subject = getUsedVariable(request, "subject", template.subject, bnClasses, child, true, false);
            UsedVariable predicate = getUsedVariable(request, "predicate", template.predicate, bnClasses, child, false,
                    false);
            UsedVariable object = getUsedVariable(request, "object", template.object, bnClasses, child, true, true);

            if(subject == null || predicate == null || object == null)
                continue;

            UsedVariables vars = new UsedVariables();
            vars.add(subject);
            vars.add(predicate);
            vars.add(object);

            branches.add(vars);
            validTemplates.add(template);
        }

        if(validTemplates.isEmpty())
            return SqlNoSolution.get();


        Map<String, Set<ResourceClass>> classes = new HashMap<String, Set<ResourceClass>>();

        for(String varName : Set.of("subject", "predicate", "object"))
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

        return new SqlConstruct(variables, child, validTemplates, columnMappings);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        Set<String> childRestrictions = new HashSet<String>();

        for(Template template : templates)
        {
            if(template.subject instanceof Variable variable)
                childRestrictions.add(variable.getSqlName());

            if(template.predicate instanceof Variable variable)
                childRestrictions.add(variable.getSqlName());

            if(template.object instanceof Variable variable)
                childRestrictions.add(variable.getSqlName());
        }

        return construct(request, templates, child.optimize(request, childRestrictions, true));
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        boolean canBeNull = variables.getValues().stream().anyMatch(v -> v.canBeNull());
        Set<Column> columns = variables.getNonConstantColumns();

        builder.append("SELECT DISTINCT ");

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


    private static UsedVariable getUsedVariable(Request request, String varName, Node node,
            Map<String, ResourceClass> bnResourceClasses, SqlIntercode child, boolean allowBlankNode,
            boolean allowLiteral)
    {
        switch(node)
        {
            case IRI iri -> {
                IriClass iriClass = request.getIriClass(iri);
                List<Column> columns = request.getColumns(iriClass, iri);
                return new UsedVariable(varName, iriClass, columns, false);
            }

            case Literal literal -> {
                if(!allowLiteral)
                    return null;

                DataType dataType = request.getConfiguration().getDataType(literal.getTypeIri());
                LiteralClass resClass = dataType == null ? unsupportedLiteral : dataType.getResourceClass(literal);
                List<Column> columns = request.getColumns(resClass, literal);
                return new UsedVariable(varName, resClass, columns, false);
            }

            case BlankNode bnode -> {
                if(!allowBlankNode)
                    return null;

                String name = bnode.getName();
                ResourceClass resClass = bnResourceClasses.get(name);

                if(resClass == null)
                {
                    resClass = new UserIntBlankNodeClass(-1 - bnResourceClasses.size());
                    bnResourceClasses.put(name, resClass);
                }

                List<Column> columns = List.of(new ExpressionColumn("(row_number() OVER ())::int4"));
                return new UsedVariable(varName, resClass, columns, false);
            }

            case Variable variable -> {
                UsedVariable var = child.getVariable(variable.getSqlName());

                Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

                if(var == null)
                    return null;

                for(Entry<ResourceClass, List<Column>> map : var.getMappings().entrySet())
                {
                    if(!allowLiteral && map.getKey() instanceof LiteralClass)
                        continue;

                    if(!allowBlankNode && map.getKey() instanceof BlankNodeClass)
                        continue;

                    mappings.put(map.getKey(), map.getValue());
                }

                if(mappings.isEmpty())
                    return null;

                boolean canBeNull = var.canBeNull() || mappings.size() < var.getMappings().size();
                return new UsedVariable(varName, mappings, canBeNull);
            }

            default -> {
                return null;
            }
        }
    }
}
