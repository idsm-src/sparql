package cz.iocb.sparql.engine.mapping.classes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;



/**
 * Resource class with a fixed SQL column layout and an explicitly declared set of superclasses. Primitive classes are
 * the atoms that {@link DerivedClass} combines.
 */
public abstract class PrimitiveResourceClass extends ResourceClass
{
    /**
     * SQL types of the columns.
     */
    protected final List<String> sqlTypes;

    /**
     * Classes this class converts to, excluding itself.
     */
    protected final Set<PrimitiveResourceClass> superClasses;


    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected PrimitiveResourceClass(String name, List<String> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name);

        this.sqlTypes = sqlTypes;
        this.superClasses = new HashSet<>(superClasses);
    }


    /**
     * True if the class is the given one or lists it among its superclasses.
     *
     * @param resClass the resource class
     * @return true if the class is the given one or lists it among its superclasses, false otherwise
     */
    protected boolean isSubclassOf(PrimitiveResourceClass resClass)
    {
        return equals(resClass) || superClasses.contains(resClass);
    }


    @Override
    public final List<String> getSqlTypes()
    {
        return sqlTypes;
    }


    /**
     * Primitive classes this class can be converted to (the class itself excluded).
     *
     * @return primitive classes this class can be converted to (the class itself excluded)
     */
    public Set<PrimitiveResourceClass> getSuperClasses()
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
