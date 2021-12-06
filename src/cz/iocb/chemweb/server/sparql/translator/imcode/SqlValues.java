package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DataType;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    private final LinkedHashMap<Column, List<Column>> data;
    private final int size;


    protected SqlValues(UsedVariables usedVariables, LinkedHashMap<Column, List<Column>> data, int size)
    {
        super(usedVariables, true);

        this.data = data;
        this.size = size;
    }


    public static SqlIntercode create(List<String> variableNames, List<List<Node>> lines)
    {
        if(lines.size() == 0)
            return SqlNoSolution.get();


        LinkedHashMap<Column, List<Column>> data = new LinkedHashMap<Column, List<Column>>();
        Map<List<Column>, Column> revData = new HashMap<List<Column>, Column>();
        UsedVariables variables = new UsedVariables();

        for(int i = 0; i < variableNames.size(); i++)
        {
            if(variables.get(variableNames.get(i)) != null)
                continue; // ignore repeated occurrences of the variable

            LinkedHashSet<ResourceClass> resClasses = new LinkedHashSet<ResourceClass>();
            boolean canBeNull = false;

            Map<Node, ResourceClass> nodeTypes = new HashMap<Node, ResourceClass>();

            for(List<Node> line : lines)
            {
                Node node = line.get(i);

                if(node != null)
                {
                    ResourceClass resClass = getType(node);

                    resClasses.add(resClass);
                    nodeTypes.put(node, resClass);
                }
                else
                {
                    canBeNull = true;
                }
            }

            if(resClasses.size() == 0)
                continue;


            UsedVariable variable = new UsedVariable(variableNames.get(i), canBeNull);

            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> classColumns = new ArrayList<List<Column>>(resClass.getColumnCount());

                for(int j = 0; j < resClass.getColumnCount(); j++)
                    classColumns.add(new ArrayList<Column>(lines.size()));

                for(List<Node> line : lines)
                {
                    Node node = line.get(i);

                    if(node != null && resClass == nodeTypes.get(node))
                    {
                        List<Column> cols = resClass.toColumns(node);

                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            classColumns.get(j).add(cols.get(j));
                    }
                    else
                    {
                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            classColumns.get(j).add(new ConstantColumn("NULL::" + resClass.getSqlTypes().get(j)));
                    }
                }


                List<Column> tableColumns = resClass.createColumns(variableNames.get(i));
                List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

                for(int j = 0; j < resClass.getColumnCount(); j++)
                {
                    List<Column> columns = classColumns.get(j);

                    if(columns.stream().allMatch(c -> c.equals(columns.get(0))))
                    {
                        mapping.add(columns.get(0));
                    }
                    else
                    {
                        Column tableColumn = revData.get(columns);

                        if(tableColumn == null)
                        {
                            tableColumn = tableColumns.get(j);
                            revData.put(columns, tableColumn);
                            data.put(tableColumn, columns);
                        }

                        mapping.add(tableColumn);
                    }
                }

                variable.addMapping(resClass, mapping);
            }

            variables.add(variable);
        }

        return new SqlValues(variables, data, lines.size());
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        UsedVariables optimizedVariables = new UsedVariables();
        LinkedHashMap<Column, List<Column>> optimizedData = new LinkedHashMap<Column, List<Column>>();

        for(UsedVariable variable : variables.getValues())
        {
            if(restrictions.contains(variable.getName()))
            {
                optimizedVariables.add(variable);

                for(List<Column> mapping : variable.getMappings().values())
                    for(Column column : mapping)
                        if(column instanceof TableColumn)
                            optimizedData.put(column, data.get(column));
            }
        }

        return new SqlValues(optimizedVariables, optimizedData, size);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT * FROM (VALUES ");

        for(int i = 0; i < size; i++)
        {
            appendComma(builder, i > 0);
            builder.append("(");

            boolean hasValue = false;

            for(List<Column> column : data.values())
            {
                appendComma(builder, hasValue);
                hasValue = true;

                builder.append(column.get(i));
            }

            if(!hasValue)
                builder.append("1");

            builder.append(")");
        }

        builder.append(") AS tab (");

        if(data.size() > 0)
            builder.append(data.keySet().stream().map(Object::toString).collect(joining(",")));
        else
            builder.append("\"#null\"");

        builder.append(")");

        return builder.toString();
    }


    @SuppressWarnings("resource")
    private static ResourceClass getType(Node value)
    {
        if(value instanceof Literal)
        {
            Literal literal = (Literal) value;
            DataType datatype = Request.currentRequest().getConfiguration().getDataType(literal.getTypeIri());
            LiteralClass resourceClass = datatype == null ? unsupportedLiteral : datatype.getResourceClass(literal);

            return resourceClass;
        }
        else if(value instanceof IRI)
        {
            for(UserIriClass resClass : Request.currentRequest().getConfiguration().getIriClasses())
                if(resClass.match(value))
                    return resClass;

            return BuiltinClasses.unsupportedIri;
        }
        else if(value instanceof BlankNodeLiteral)
        {
            return ((BlankNodeLiteral) value).getResourceClass();
        }
        else
        {
            return null;
        }
    }
}
