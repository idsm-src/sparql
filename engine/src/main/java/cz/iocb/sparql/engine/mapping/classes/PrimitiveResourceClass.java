package cz.iocb.sparql.engine.mapping.classes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;



public abstract class PrimitiveResourceClass extends ResourceClass
{
    protected final List<String> sqlTypes;
    protected final Set<ResourceClass> superClasses;


    protected PrimitiveResourceClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name);

        this.sqlTypes = sqlTypes;
        this.superClasses = new HashSet<>(superClasses);
    }


    protected boolean isSubclassOf(PrimitiveResourceClass resClass)
    {
        return equals(resClass) || superClasses.contains(resClass);
    }


    @Override
    public final List<String> getSqlTypes()
    {
        return sqlTypes;
    }


    public Set<ResourceClass> getSuperClasses()
    {
        return superClasses;
    }


    @Override
    public final int getColumnCount()
    {
        return sqlTypes.size();
    }


    @Override
    public PrimitiveResourceClass getEffectiveClass()
    {
        return this;
    }


    @Override
    public ResourceClass getBuiltinClass()
    {
        return this;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        PrimitiveResourceClass other = (PrimitiveResourceClass) object;

        return Objects.equals(superClasses, other.superClasses) && Objects.equals(sqlTypes, other.sqlTypes);
    }
}
