package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.stream.Collectors.toSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;



/**
 * Describes how RDF terms of one kind are represented in SQL: how many columns of which types hold a value, how a
 * constant term becomes columns, and how columns are converted to and from more general classes, ultimately to
 * {@link BuiltinClasses#box}, the universal {@code sparql.rdfbox} representation. Classes form a hierarchy: a variable
 * whose possible classes are known keeps native columns instead of boxed values, and disjoint classes let joins,
 * filters and comparisons be pruned at translation time.
 */
public abstract class ResourceClass
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
     * Return set of classes that represent values of this resource class in a query result.
     *
     * @return set of classes representing class values in a query result
     */
    public abstract Set<ResultResourceClass> getResultResourceClasses();


    /**
     * Check if the given RDF term can be represented in this resource class.
     *
     * @param statement database statement
     * @param term RDF term
     * @return true the RDF term can be represented in this resource class, false otherwise
     */
    public abstract boolean match(Statement statement, RdfTerm term);


    /**
     * Create list of columns that represent the given RDF term.
     *
     * @param statement database statement
     * @param term RDF term
     * @return list of columns representing the term value
     */
    public abstract List<Column> toColumns(Statement statement, RdfTerm term);


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
    public abstract List<String> getSqlTypes();


    /**
     * Number of SQL columns representing a value.
     *
     * @return number of SQL columns representing a value
     */
    public abstract int getColumnCount();


    /**
     * Convert the given columns of this resource class into the columns of the selected target resource class.
     *
     * @param targetClass the resource class to which is converted
     * @param columns the columns representing values of this resource class
     * @param canBeNull if true, a generated column cannot be a constant column
     * @return the columns representing values the target resource class
     *
     */
    public List<Column> toClass(ResourceClass targetClass, List<Column> columns, boolean canBeNull)
    {
        if(this.equals(targetClass))
            return columns;
        if(this.isSubclassOf(targetClass))
            return toGeneralClass(targetClass, columns, canBeNull);
        else if(targetClass.isSubclassOf(this))
            return targetClass.fromGeneralClass(this, columns, false);
        else
            throw new IllegalArgumentException();
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
     * True if no term belongs to both classes.
     *
     * @param a one operand
     * @param b the other operand
     * @return true if no term belongs to both classes, false otherwise
     */
    public static boolean areDisjunct(ResourceClass a, ResourceClass b)
    {
        if(a instanceof PrimitiveResourceClass pa && b instanceof PrimitiveResourceClass pb)
            return !pa.isSubclassOf(pb) && !pb.isSubclassOf(pa);

        return DerivedClass.areDisjunct(a, b);
    }


    /**
     * True if {@code resClass} is disjoint with every class of the set.
     *
     * @param resClass the resource class
     * @param resClasses the resource classes
     * @return true if {@code resClass} is disjoint with every class of the set, false otherwise
     */
    public static boolean areDisjunct(ResourceClass resClass, Set<ResourceClass> resClasses)
    {
        return resClasses.stream().allMatch(c -> areDisjunct(resClass, c));
    }


    /**
     * True if every class of the first set is disjoint with every class of the second one.
     *
     * @param classes1 the first set of classes
     * @param classes2 the second set of classes
     * @return true if every class of the first set is disjoint with every class of the second one, false otherwise
     */
    public static boolean areDisjunct(Set<ResourceClass> classes1, Set<ResourceClass> classes2)
    {
        return classes1.stream().allMatch(c1 -> ResourceClass.areDisjunct(c1, classes2));
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
                    new TableColumn(map.getSafeName(variable.getName() + "#" + name + (count > 0 ? "_par" + i : ""))));

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


    /**
     * The most specific of the given classes, which are expected to be mutually comparable.
     *
     * @param classes the classes
     * @return the most specific of the given classes, which are expected to be mutually comparable
     */
    public static ResourceClass getIntersectionClass(Set<ResourceClass> classes)
    {
        //TODO: take into account that a primitive class can be composed of (finitely many) other primitive classes
        //TODO: use more general approach

        ResourceClass result = null;

        for(ResourceClass c : classes)
            if(result == null || c.isSubclassOf(result))
                result = c;

        return result;
    }


    /**
     * Merges the given classes into pairwise disjoint ones: classes that overlap (transitively) are unioned together.
     *
     * @param classes the classes
     * @return the disjoint classes
     */
    public static Set<ResourceClass> getDisjunctClasses(Set<ResourceClass> classes)
    {
        Collection<Set<ResourceClass>> out = UnionFind.getDisjunctEntries(classes, (l, r) -> !areDisjunct(l, r));

        return out.stream().map(s -> unionize(s)).collect(toSet());
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
