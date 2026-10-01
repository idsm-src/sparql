package cz.iocb.sparql.engine.mapping.classes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * Resource class with a fixed SQL column layout and an explicitly declared set of superclasses. Primitive classes are
 * the atoms that {@link DerivedClass} combines.
 */
public sealed abstract class PrimitiveResourceClass extends ResourceClass
        permits LiteralClass, IriClass, BlankNodeClass, RdfBoxClass, InternalResourceClass, TripleTermClass
{
    /**
     * SQL types of the columns.
     */
    protected final List<SqlType> sqlTypes;

    /**
     * Classes this class converts to, excluding itself.
     */
    protected final Set<PrimitiveResourceClass> superClasses;

    /**
     * Primitive class whose columns store the values: the class itself, or the original class of a subset class.
     */
    private final PrimitiveResourceClass effectiveClass;


    /**
     * Creates the class with its name, column types and superclasses; the class stores its values itself.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected PrimitiveResourceClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        this(name, sqlTypes, superClasses, null);
    }


    /**
     * Creates the class with its name, column types, superclasses and the class that effectively represents it.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     * @param effectiveClass primitive class whose columns store the values, or null for the class itself
     */
    protected PrimitiveResourceClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses,
            PrimitiveResourceClass effectiveClass)
    {
        super(name);

        this.sqlTypes = sqlTypes;
        this.superClasses = new HashSet<>(superClasses);
        this.effectiveClass = effectiveClass != null ? effectiveClass : this;
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


    /**
     * True if no term belongs to both this class and the given one. Two primitive classes are disjoint when neither is
     * a subclass of the other, unless a class refines the test: two triple term classes may overlap through their
     * components ({@link TripleTermClass}), and two user IRI classes may overlap when the declarations of the
     * configuration say so ({@link UserIriClass}). The test is symmetric.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param resClass the resource class
     * @return true if no term belongs to both this class and the given one, false otherwise
     */
    protected boolean isDisjunctWith(ClassRelations relations, PrimitiveResourceClass resClass)
    {
        return !isSubclassOf(resClass) && !resClass.isSubclassOf(this);
    }


    @Override
    public final List<SqlType> getSqlTypes()
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


    /**
     * Class through which values are converted between two primitive classes when neither converts to the other
     * directly: a minimal class among their common superclasses (the classes themselves included), chosen like the
     * effective class of a derived class when several are minimal. Every class of RDF terms converts to the box, so two
     * such classes always have one.
     *
     * @param a one class
     * @param b the other class
     * @return a minimal common superclass of the classes
     * @throws IllegalArgumentException if the classes have no common superclass
     */
    public static PrimitiveResourceClass getCommonSuperclass(PrimitiveResourceClass a, PrimitiveResourceClass b)
    {
        Set<PrimitiveResourceClass> candidates = new HashSet<>(a.superClasses);
        candidates.add(a);

        Set<PrimitiveResourceClass> others = new HashSet<>(b.superClasses);
        others.add(b);

        candidates.retainAll(others);

        PrimitiveResourceClass common = DerivedClass.select(DerivedClass.reduceSuperclasses(candidates));

        if(common == null)
            throw new IllegalArgumentException("classes " + a + " and " + b + " have no common superclass");

        return common;
    }


    @Override
    public final int getColumnCount()
    {
        return sqlTypes.size();
    }


    @Override
    public final PrimitiveResourceClass getEffectiveClass()
    {
        return effectiveClass;
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
