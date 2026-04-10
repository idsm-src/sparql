package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.ColumnMap;



public abstract class ResourceClass
{
    protected final String name;
    protected final List<String> sqlTypes;


    protected ResourceClass(String name, List<String> sqlTypes)
    {
        this.name = name;
        this.sqlTypes = sqlTypes;
    }


    public final String getName()
    {
        return name;
    }


    public abstract ResourceClass getGeneralClass();


    public abstract Set<ResultResourceClass> getResultResourceClasses();


    public final int getColumnCount()
    {
        return sqlTypes.size();
    }


    public final List<String> getSqlTypes()
    {
        return sqlTypes;
    }


    public List<Column> createColumns(ColumnMap map, String variable)
    {
        List<Column> columns = new ArrayList<Column>(sqlTypes.size());

        for(int i = 0; i < sqlTypes.size(); i++)
            columns.add(
                    new TableColumn(map.getSafeName(variable + "#" + name + (sqlTypes.size() > 0 ? "_par" + i : ""))));

        return columns;
    }


    public abstract List<Column> toColumns(Statement statement, Node node);


    public abstract List<Column> fromGeneralClass(List<Column> columns);


    public abstract List<Column> toGeneralClass(List<Column> columns, boolean check);


    public abstract List<Column> fromExpression(Column column);


    public abstract Column toExpression(List<Column> columns);


    public abstract List<Column> fromBoxedExpression(Column column, boolean check);


    public abstract Column toBoxedExpression(List<Column> columns);


    public abstract Column toExpression(Statement statement, Node node);


    public abstract String fromGeneralExpression(String code);


    public abstract String toGeneralExpression(String code);


    public abstract String toBoxedExpression(String code);


    public abstract String toUnboxedExpression(String code, boolean check);


    public abstract boolean match(Statement statement, Node node);


    public boolean hasExpressionType()
    {
        return true;
    }


    public boolean canBeDerivatedFromGeneral()
    {
        return true;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ResourceClass other = (ResourceClass) object;

        if(!name.equals(other.name))
            return false;

        if(!sqlTypes.equals(other.sqlTypes))
            return false;

        return true;
    }


    public static boolean areDisjunct(ResourceClass class1, ResourceClass class2)
    {
        return class1 != class2 && class1.getGeneralClass() != class2 && class1 != class2.getGeneralClass();
    }


    public static boolean areDisjunct(ResourceClass resClass, Set<ResourceClass> resClasses)
    {
        return resClasses.stream().allMatch(c -> areDisjunct(resClass, c));
    }


    public static boolean areDisjunct(Set<ResourceClass> classes1, Set<ResourceClass> classes2)
    {
        return classes1.stream().allMatch(c1 -> ResourceClass.areDisjunct(c1, classes2));
    }


    public static Set<ResourceClass> getDisjunctClasses(Set<ResourceClass> resClasses)
    {
        Set<ResourceClass> result = new HashSet<ResourceClass>();

        for(ResourceClass resClass : resClasses)
        {
            if(resClasses.contains(resClass.getGeneralClass()))
                result.add(resClass.getGeneralClass());
            else
                result.add(resClass);
        }

        return result;
    }


    public static ResourceClass getIntersectionClass(Set<ResourceClass> resClasses)
    {
        ResourceClass result = null;

        for(ResourceClass resClass : resClasses)
        {
            if(result == null || result == resClass.getGeneralClass())
                result = resClass;
            else if(result != resClass && result.getGeneralClass() != resClass)
                return null;
        }

        return result;
    }


    public static ResourceClass getUnionClass(ResourceClass classes1, ResourceClass classes2)
    {
        if(classes1 == classes2)
            return classes1;
        else if(classes1.getGeneralClass() == classes2.getGeneralClass())
            return classes1.getGeneralClass();
        else
            return null;
    }


    public List<Column> fromGeneralClass(ResourceClass sourceClass, List<Column> columns, boolean hasToBeChecked)
    {
        if(this == sourceClass)
            return columns;

        return this.fromGeneralClass(columns);
    }


    public ResourceClass getEffectiveClass()
    {
        return this;
    }


    public boolean isSubclassOf(ResourceClass resClass)
    {
        return this == resClass || this.getGeneralClass() == resClass;
    }


    public List<Column> toGeneralClass(ResourceClass sourceClass, List<Column> columns, boolean canBeNull)
    {
        if(this == sourceClass)
            return columns;

        return this.toGeneralClass(columns, canBeNull);
    }


    public List<Column> fromGeneralClass(ResourceClass sourceClass, List<Column> columns)
    {
        if(this == sourceClass)
            return columns;

        return this.fromGeneralClass(columns);
    }
}
