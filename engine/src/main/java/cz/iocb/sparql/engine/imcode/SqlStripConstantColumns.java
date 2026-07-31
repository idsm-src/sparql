package cz.iocb.sparql.engine.imcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlStripConstantColumns extends SqlIntercode
{
    private final SqlIntercode child;
    private final Map<TableColumn, ConstantColumn> map;


    protected SqlStripConstantColumns(VariableBindings bindings, SqlIntercode child,
            Map<TableColumn, ConstantColumn> map)
    {
        super(bindings, child.isDeterministic());

        this.child = child;
        this.map = map;
    }


    protected static SqlIntercode strip(SqlIntercode child)
    {
        Map<ConstantColumn, TableColumn> map = new HashMap<>();

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding variableBinding : child.getVariableBindings().getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : variableBinding.getMappings().entrySet())
            {
                if(entry.getValue() == null)
                {
                    binding.addMapping(entry.getKey(), null);
                }
                else
                {
                    List<Column> mapping = new ArrayList<>(entry.getValue().size());

                    for(Column column : entry.getValue())
                    {
                        if(column instanceof ConstantColumn constColumn)
                        {
                            TableColumn col = map.get(column);

                            if(col == null)
                            {
                                col = new TableColumn("#const" + map.size());
                                map.put(constColumn, col);
                            }

                            mapping.add(col);
                        }
                        else
                        {
                            mapping.add(column);
                        }
                    }

                    binding.addMapping(entry.getKey(), mapping);
                }
            }

            bindings.add(binding);
        }

        Map<TableColumn, ConstantColumn> remap = new HashMap<>();
        map.forEach((k, v) -> remap.put(v, k));

        return new SqlStripConstantColumns(bindings, child, remap);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child.optimize(request, restrictions, reduced, evalServices);

        boolean hasConstantColumn = false;

        for(VariableBinding binding : optChild.getVariableBindings().getValues())
            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null)
                    for(Column column : entry.getValue())
                        if(column instanceof ConstantColumn)
                            hasConstantColumn = true;

        if(!hasConstantColumn)
            return optChild;


        if(optChild == child)
            return this;

        return strip(optChild);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        boolean hasSelect = false;

        for(Column column : columns)
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            ConstantColumn constant = map.get(column);

            if(constant != null)
                builder.append(constant).append(" AS ");

            builder.append(column);
        }

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS tab");

        return builder.toString();
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return child.isDistinct(request, selected);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("strip constant columns");

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    public final SqlIntercode getChild()
    {
        return child;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlStripConstantColumns imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(child);
    }
}
