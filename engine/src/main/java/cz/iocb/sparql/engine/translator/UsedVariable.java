package cz.iocb.sparql.engine.translator;

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
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class UsedVariable
{
    private final String name;
    private final boolean canBeNull;
    private final Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();


    public UsedVariable(UsedVariable other)
    {
        this.name = other.name;
        this.canBeNull = other.canBeNull;
        this.mappings.putAll(other.mappings);
    }


    public UsedVariable(String name, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.mappings.putAll(mappings);
    }


    public UsedVariable(String name, ResourceClass resClass, List<Column> columns, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.mappings.put(resClass, columns);
    }


    public UsedVariable(String name, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
    }


    public void addMapping(ResourceClass resClass, List<Column> columns)
    {
        assert !this.mappings.containsKey(resClass);

        this.mappings.put(resClass, columns);
    }


    public boolean containsClass(ResourceClass resClass)
    {
        return this.mappings.containsKey(resClass);
    }


    public final String getName()
    {
        return name;
    }


    public final boolean canBeNull()
    {
        return canBeNull;
    }


    public final Map<ResourceClass, List<Column>> getMappings()
    {
        return mappings;
    }


    public List<Column> getMapping(ResourceClass resourceClass)
    {
        return mappings.get(resourceClass);
    }


    public List<Column> deriveMapping(ResourceClass targetClass)
    {
        if(mappings.containsKey(targetClass))
            return mappings.get(targetClass);

        List<List<Column>> variants = new ArrayList<List<Column>>();
        boolean canBeNull = canBeNull() || mappings.size() > 1;

        for(Entry<ResourceClass, List<Column>> map : mappings.entrySet())
        {
            if(!ResourceClass.areDisjunct(targetClass, map.getKey()))
            {
                ResourceClass sourceClass = map.getKey();

                if(map.getValue() == null)
                    return null;

                ResourceClass targetEffectiveClass = targetClass.getEffectiveClass();
                ResourceClass effectiveSourceClass = sourceClass.getEffectiveClass();

                if(effectiveSourceClass.isSubclassOf(targetEffectiveClass))
                    variants.add(sourceClass.toGeneralClass(targetClass, map.getValue(), canBeNull));
                else if(targetEffectiveClass.isSubclassOf(effectiveSourceClass))
                    variants.add(targetClass.fromGeneralClass(sourceClass, map.getValue()));
                else
                    throw new UnsupportedOperationException();
            }
        }

        if(variants.isEmpty())
            return targetClass.getSqlTypes().stream().map(s -> (Column) new ConstantColumn(null, s)).toList();

        if(variants.size() == 1)
            return variants.get(0);

        List<Column> columns = new ArrayList<Column>();

        for(int i = 0; i < targetClass.getColumnCount(); i++)
        {
            List<Column> set = new ArrayList<Column>();

            for(List<Column> variant : variants)
                if(!set.contains(variant.get(i)))
                    set.add(variant.get(i));

            Collections.sort(set);

            if(set.size() == 1)
                columns.add(variants.get(0).get(i));
            else
                columns.add(new ExpressionColumn(
                        set.stream().map(Object::toString).collect(joining(",", "coalesce(", ")"))));
        }

        return columns;
    }


    public final Set<ResourceClass> getClasses()
    {
        return mappings.keySet();
    }


    public List<Column> getMapping()
    {
        return mappings.get(getResourceClass());
    }


    public final ResourceClass getResourceClass()
    {
        if(mappings.size() != 1)
            throw new IllegalArgumentException();

        return mappings.keySet().iterator().next();
    }


    public final Set<ResourceClass> getCompatibleClasses(ResourceClass resClass)
    {
        HashSet<ResourceClass> result = new HashSet<ResourceClass>();

        for(ResourceClass r : mappings.keySet())
            if(!ResourceClass.areDisjunct(r, resClass))
                result.add(r);

        return result;
    }


    public Set<Column> getNonConstantColumns()
    {
        Set<Column> result = new HashSet<Column>();

        for(List<Column> columns : mappings.values())
            if(columns != null)
                for(Column column : columns)
                    if(!(column instanceof ConstantColumn))
                        result.add(column);

        return result;
    }


    public Set<Column> getNonConstantColumns(ResourceClass resourceClass)
    {
        Set<Column> result = new HashSet<Column>();

        for(Column column : mappings.get(resourceClass))
            if(!(column instanceof ConstantColumn))
                result.add(column);

        return result;
    }


    public boolean isConstant()
    {
        for(List<Column> columns : mappings.values())
        {
            if(columns == null)
                return false;

            for(Column column : columns)
                if(!(column instanceof ConstantColumn))
                    return false;
        }

        return true;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        UsedVariable other = (UsedVariable) object;

        if(!name.equals(other.name))
            return false;

        if(canBeNull != other.canBeNull)
            return false;

        if(!mappings.equals(other.mappings))
            return false;

        return true;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode() + Boolean.hashCode(canBeNull) + mappings.hashCode();
    }


    public boolean hasMapping()
    {
        return mappings.values().stream().anyMatch(c -> c != null);
    }
}
