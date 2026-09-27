package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Literals forming a subset of another canonical literal class, stored in the same columns and delivered to the result
 * in the same class. A deployment narrows a built-in or user literal class to the literals it actually holds by
 * subclassing this class and overriding {@link #match}; the original class stays the effective class of the subset, so
 * the engine keeps treating the values as those of the original, and everything but the matching is delegated to the
 * original and cannot be changed. The subset is a subclass of the original and of all its superclasses.
 */
public non-sealed class SubsetLiteralClass extends CanonicalLiteralClass
{
    /**
     * The class this class is a subset of.
     */
    private final CanonicalLiteralClass original;


    /**
     * Creates the subset of the original class under the given (unique) name.
     *
     * @param name the name
     * @param original the class this class is a subset of
     */
    public SubsetLiteralClass(String name, CanonicalLiteralClass original)
    {
        super(name, original.getDatatype(), original.getSqlTypes(), superClassesOf(original),
                original.getEffectiveClass());

        this.original = original;
    }


    /**
     * The original class together with its superclasses.
     *
     * @param original the class this class is a subset of
     * @return the original class together with its superclasses
     */
    private static Set<PrimitiveResourceClass> superClassesOf(CanonicalLiteralClass original)
    {
        Set<PrimitiveResourceClass> superClasses = new HashSet<>(original.getSuperClasses());
        superClasses.add(original);

        return superClasses;
    }


    /**
     * The class this class is a subset of.
     *
     * @return the class this class is a subset of
     */
    public final CanonicalLiteralClass getOriginal()
    {
        return original;
    }


    @Override
    public final ResourceClass getResultResourceClass()
    {
        return original.getResultResourceClass();
    }


    /**
     * True if the literal belongs to the original class; a subclass narrows it to the literals of the subset, which
     * must belong to the original class as well.
     */
    @Override
    public boolean match(Statement statement, Literal literal)
    {
        return original.match(statement, literal);
    }


    @Override
    public final List<Column> toColumns(Literal literal)
    {
        return original.toColumns(literal);
    }


    @Override
    public final List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        return original.toGeneralClass(superClass.getEffectiveClass(), columns, canBeNull);
    }


    @Override
    public final List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        return original.fromGeneralClass(superClass, columns, checkOptional);
    }


    @Override
    public final boolean isOptionalColumn(int index)
    {
        return original.isOptionalColumn(index);
    }


    @Override
    public final boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        SubsetLiteralClass other = (SubsetLiteralClass) object;

        return name.equals(other.name) && original.equals(other.original);
    }
}
