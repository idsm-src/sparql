package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;
import cz.iocb.sparql.engine.request.Request;



/**
 * Describes how RDF terms of one kind are represented in SQL: how many columns of which types hold a value, how a
 * constant term becomes columns, and how columns are converted to and from more general classes, ultimately to
 * {@link BuiltinClasses#box}, the universal {@code sparql.rdfbox} representation. Classes form a hierarchy: a variable
 * whose possible classes are known keeps native columns instead of boxed values, and disjoint classes let joins,
 * filters and comparisons be pruned at translation time.
 */
public sealed abstract class ResourceClass permits PrimitiveResourceClass, DerivedClass
{
    /**
     * Unique name of the class.
     */
    protected final String name;


    /**
     * Creates the class with its name.
     *
     * @param name the name
     */
    protected ResourceClass(String name)
    {
        this.name = name;
    }


    /**
     * Class in which values of this class are delivered to the query result: the generated SELECT list holds one column
     * group per result class of each projected variable, and {@link cz.iocb.sparql.engine.request.Result} decodes the
     * columns of a result class into an RDF term. The result classes therefore form a closed set fixed by this sealed
     * hierarchy: the box (delivered as the text form of the {@code sparql.rdfbox} column and decoded by
     * {@link cz.iocb.sparql.engine.request.RdfBoxParser}), the scalar IRI class, the composite blank node and literal
     * classes, and the triple term classes built from them. A deployment can only add classes whose result class is
     * fixed: IRI classes deliver as {@link BuiltinClasses#iri}, user literal classes as {@link BuiltinClasses#userType}
     * and a {@link SubsetLiteralClass} as its original class.
     *
     * @return class in which values of this class are delivered to the query result
     */
    public abstract ResourceClass getResultResourceClass();


    /**
     * Check if the given RDF term can be represented in this resource class; the configuration and the database of the
     * request are consulted when the class alone cannot decide.
     *
     * @param request the current request
     * @param term RDF term
     * @return true the RDF term can be represented in this resource class, false otherwise
     */
    public abstract boolean match(Request request, RdfTerm term);


    /**
     * Create list of columns that represent the given RDF term; the configuration and the database of the request are
     * consulted when the class alone cannot build them.
     *
     * @param request the current request
     * @param term RDF term
     * @return list of columns representing the term value
     */
    public abstract List<Column> toColumns(Request request, RdfTerm term);


    /**
     * Convert the given columns of this resource class into the columns of its selected superclass.
     *
     * @param superClass the resource class to which is converted
     * @param columns the columns representing values of this resource class
     * @param canBeNull if true, a generated column cannot be a constant column
     * @return the columns representing values the superclass
     */
    public abstract List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull);


    /**
     * Convert the given columns of selected superclass into the columns of this resource class.
     *
     * @param superClass the resource class from which is converted
     * @param columns the columns representing values of the superclass
     * @param checkOptional indicates whether the representability check may be skipped
     * @return the columns representing values this resource class
     */
    public abstract List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns,
            boolean checkOptional);


    /**
     * Returns a base class, i.e. one that is not a composite class, that effectively represents this resource class.
     *
     * @return the base class that effectively represents this resource class
     */
    public abstract PrimitiveResourceClass getEffectiveClass();


    /**
     * SQL types of the columns representing a value, in column order.
     *
     * @return SQL types of the columns representing a value, in column order
     */
    public abstract List<SqlType> getSqlTypes();


    /**
     * Number of SQL columns representing a value.
     *
     * @return number of SQL columns representing a value
     */
    public abstract int getColumnCount();


    /**
     * True if the column at the given position may be NULL while the term is bound. The other (determining) columns
     * decide whether a term is present: they are either all NULL, in which case the term is unbound and the optional
     * columns are NULL too, or all not NULL. A class has at least one determining column, and its mapping of columns to
     * terms must stay injective when an optional column is NULL, so that terms can be compared and deduplicated column
     * by column.
     *
     * @param index position of the column
     * @return true if the column at the given position may be NULL while the term is bound, false otherwise
     */
    public boolean isOptionalColumn(int index)
    {
        return false;
    }


    /**
     * Convert the given columns of this resource class into the columns of the selected target resource class: by
     * {@link #toGeneralClass} when the target is a superclass, by {@link #fromGeneralClass} when it is a subclass, and
     * through a common superclass of the effective classes otherwise, checking the representability, so that a value
     * outside the target class (also every value of a class disjoint with it) yields NULL columns.
     *
     * @param targetClass the resource class to which is converted
     * @param columns the columns representing values of this resource class
     * @param canBeNull if true, a generated column cannot be a constant column
     * @return the columns representing values the target resource class
     */
    public List<Column> toClass(ResourceClass targetClass, List<Column> columns, boolean canBeNull)
    {
        if(this.equals(targetClass))
            return columns;
        else if(this.isSubclassOf(targetClass))
            return toGeneralClass(targetClass, columns, canBeNull);
        else if(targetClass.isSubclassOf(this))
            return targetClass.fromGeneralClass(this, columns, false);

        PrimitiveResourceClass common = PrimitiveResourceClass.getCommonSuperclass(getEffectiveClass(),
                targetClass.getEffectiveClass());

        return targetClass.fromGeneralClass(common, toGeneralClass(common, columns, canBeNull), false);
    }


    /**
     * Columns of the target primitive class representing the value given in the columns of the source primitive class:
     * converted directly when one class is a subclass of the other, and through a common superclass otherwise. Unless
     * {@code checkOptional}, a value outside the target class yields NULL columns.
     *
     * @param source class of the given columns
     * @param target the class to convert to
     * @param columns the columns representing the value in the source class
     * @param canBeNull if true, a generated column cannot be a constant column
     * @param checkOptional indicates whether the representability check may be skipped
     * @return the columns representing the value in the target class
     */
    static List<Column> convert(PrimitiveResourceClass source, PrimitiveResourceClass target, List<Column> columns,
            boolean canBeNull, boolean checkOptional)
    {
        if(source.equals(target))
            return columns;

        if(source.isSubclassOf(target))
            return source.toGeneralClass(target, columns, canBeNull);

        if(target.isSubclassOf(source))
            return target.fromGeneralClass(source, columns, checkOptional);

        PrimitiveResourceClass common = PrimitiveResourceClass.getCommonSuperclass(source, target);

        return target.fromGeneralClass(common, source.toGeneralClass(common, columns, canBeNull), checkOptional);
    }


    /**
     * True if every value of this class is also a value of {@code resClass} (and can be converted to it by
     * {@link #toGeneralClass}); a class is a subclass of itself.
     *
     * @param resClass the resource class
     * @return true if {@code a} is a subclass of {@code b}, false otherwise
     */
    public final boolean isSubclassOf(ResourceClass resClass)
    {
        return equals(resClass) || isSubclassOf(this, resClass);
    }


    /**
     * Subclass test delegating to the primitive hierarchy or to the derived-class normal forms.
     *
     * @param a one operand
     * @param b the other operand
     * @return true if {@code a} is a subclass of {@code b}, false otherwise
     */
    private boolean isSubclassOf(ResourceClass a, ResourceClass b)
    {
        if(a instanceof PrimitiveResourceClass pa && b instanceof PrimitiveResourceClass pb)
            return pa.isSubclassOf(pb);

        return DerivedClass.isSubclassOf(a, b);
    }


    /**
     * True if no term belongs to both classes: two primitive classes decide by
     * {@link PrimitiveResourceClass#isDisjunctWith} (unrelated classes are disjoint, except for two triple term classes
     * with overlapping components and two user IRI classes the declarations let overlap), derived classes by their
     * normal forms. The declarations of the configuration have to be supplied, as the classes do not keep them; when
     * user IRI classes occur in at most one operand, the result does not depend on them and
     * {@link #areDisjunct(ResourceClass, ResourceClass)} serves.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one operand
     * @param b the other operand
     * @return true if no term belongs to both classes, false otherwise
     */
    public static boolean areDisjunct(ClassRelations relations, ResourceClass a, ResourceClass b)
    {
        if(a instanceof PrimitiveResourceClass pa && b instanceof PrimitiveResourceClass pb)
            return pa.isDisjunctWith(relations, pb);

        return DerivedClass.areDisjunct(relations, a, b);
    }


    /**
     * Disjointness of classes that cannot depend on the declarations of a configuration, see
     * {@link #areDisjunct(ClassRelations, ResourceClass, ResourceClass)}: user IRI classes may occur in at most one
     * operand, as only two of them on opposite sides could be declared to overlap. The built-in classes, and the
     * predicates of {@link BuiltinClasses} testing against them, therefore need no declarations.
     *
     * @param a one operand
     * @param b the other operand
     * @return true if no term belongs to both classes, false otherwise
     * @throws IllegalArgumentException if user IRI classes occur in both operands
     */
    public static boolean areDisjunct(ResourceClass a, ResourceClass b)
    {
        checkIndependence(List.of(a), List.of(b));

        return areDisjunct(ClassRelations.NONE, a, b);
    }


    /**
     * True if {@code resClass} is disjoint with every class of the set, see
     * {@link #areDisjunct(ResourceClass, ResourceClass)}.
     *
     * @param resClass the resource class
     * @param resClasses the resource classes
     * @return true if {@code resClass} is disjoint with every class of the set, false otherwise
     * @throws IllegalArgumentException if user IRI classes occur on both sides
     */
    public static boolean areDisjunct(ResourceClass resClass, Set<ResourceClass> resClasses)
    {
        checkIndependence(List.of(resClass), resClasses);

        return areDisjunct(ClassRelations.NONE, resClass, resClasses);
    }


    /**
     * True if every class of the first set is disjoint with every class of the second one, see
     * {@link #areDisjunct(ResourceClass, ResourceClass)}.
     *
     * @param classes1 the first set of classes
     * @param classes2 the second set of classes
     * @return true if every class of the first set is disjoint with every class of the second one, false otherwise
     * @throws IllegalArgumentException if user IRI classes occur on both sides
     */
    public static boolean areDisjunct(Set<ResourceClass> classes1, Set<ResourceClass> classes2)
    {
        checkIndependence(classes1, classes2);

        return areDisjunct(ClassRelations.NONE, classes1, classes2);
    }


    /**
     * Checks that the declarations of a configuration cannot influence the disjointness of the classes of the two
     * sides: user IRI classes occur on at most one of them.
     *
     * @param side1 classes of one side
     * @param side2 classes of the other side
     * @throws IllegalArgumentException if user IRI classes occur on both sides
     */
    private static void checkIndependence(Collection<ResourceClass> side1, Collection<ResourceClass> side2)
    {
        if(side1.stream().anyMatch(c -> !DerivedClass.getUserIriClasses(c).isEmpty())
                && side2.stream().anyMatch(c -> !DerivedClass.getUserIriClasses(c).isEmpty()))
            throw new IllegalArgumentException("the disjointness of " + side1 + " and " + side2
                    + " depends on the declarations of the configuration, which have to be supplied");
    }


    /**
     * True if {@code resClass} is disjoint with every class of the set.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param resClass the resource class
     * @param resClasses the resource classes
     * @return true if {@code resClass} is disjoint with every class of the set, false otherwise
     */
    public static boolean areDisjunct(ClassRelations relations, ResourceClass resClass, Set<ResourceClass> resClasses)
    {
        return resClasses.stream().allMatch(c -> areDisjunct(relations, resClass, c));
    }


    /**
     * True if every class of the first set is disjoint with every class of the second one.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param classes1 the first set of classes
     * @param classes2 the second set of classes
     * @return true if every class of the first set is disjoint with every class of the second one, false otherwise
     */
    public static boolean areDisjunct(ClassRelations relations, Set<ResourceClass> classes1,
            Set<ResourceClass> classes2)
    {
        return classes1.stream().allMatch(c1 -> ResourceClass.areDisjunct(relations, c1, classes2));
    }


    /**
     * Creates the column names under which a value of this class of the variable is exposed by a generated subquery
     * ({@code <variable>#<class>_par<i>}), shortened through the column map when too long for PostgreSQL.
     *
     * @param map map shortening long column names
     * @param variable the variable
     * @return the column names
     */
    public List<Column> createColumns(ColumnMap map, Variable variable)
    {
        //FIXME: consider whether there might be a collision of names with those generated in another part of the query

        int count = getColumnCount();

        List<Column> columns = new ArrayList<>(count);

        for(int i = 0; i < count; i++)
            columns.add(
                    new TableColumn(map.getSafeName(variable.getName() + "#" + name + (count > 0 ? "_par" + i : "")),
                            getSqlTypes().get(i)));

        return columns;
    }


    /**
     * Unique name of the class, also used in generated column names.
     *
     * @return unique name of the class, also used in generated column names
     */
    public final String getResourceName()
    {
        return name;
    }


    /**
     * Class in which an expression over values of the given classes is evaluated: their union, stored in the
     * single-column superclass selected by {@link #getExpressionClass(ResourceClass)}.
     *
     * @param resClasses the resource classes
     * @return class in which an expression over values of the given classes is evaluated: their union, stored in the
     *         single-column superclass selected by {@link #getExpressionClass(ResourceClass)}
     */
    public static ResourceClass getExpressionClass(Set<ResourceClass> resClasses)
    {
        return unionize(resClasses, (PrimitiveResourceClass) getExpressionClass(unionize(resClasses)));
    }


    /**
     * The most specific single-column primitive superclass of the (effective) class, i.e. the narrowest type able to
     * hold its values in one SQL expression; the class itself when it has a single column.
     *
     * @param resClass the resource class
     * @return the most specific single-column primitive superclass of the (effective) class, i.e. the narrowest type
     *         able to hold its values in one SQL expression; the class itself when it has a single column
     */
    public static ResourceClass getExpressionClass(ResourceClass resClass)
    {
        if(resClass instanceof DerivedClass c)
            resClass = c.getEffectiveClass();

        if(resClass.getColumnCount() == 1)
            return resClass;

        Set<ResourceClass> candidates = ((PrimitiveResourceClass) resClass).getSuperClasses().stream()
                .filter(r -> r.getColumnCount() == 1).collect(toSet());

        //FIXME: modify to work correctly even in case of multiple or cyclic inheritance

        Iterator<ResourceClass> it = candidates.iterator();
        ResourceClass effectiveClass = it.next();

        while(it.hasNext())
        {
            ResourceClass next = it.next();

            if(next.isSubclassOf(effectiveClass))
                effectiveClass = next;
        }

        return effectiveClass;
    }


    @Override
    public String toString()
    {
        return name;
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

        return Objects.equals(name, other.name);
    }
}
