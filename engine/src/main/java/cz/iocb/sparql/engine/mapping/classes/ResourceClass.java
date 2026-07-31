package cz.iocb.sparql.engine.mapping.classes;

import static java.util.stream.Collectors.toSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.common.UnionFind;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;;



public abstract class ResourceClass
{
    protected final String name;


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
     * @return the columns representing values this resource class
     */
    public abstract List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns);


    /**
     * Returns a base class, i.e. one that is not a composite class, that effectively represents this resource class.
     *
     * @return the base class that effectively represents this resource class
     */
    public abstract PrimitiveResourceClass getEffectiveClass();


    /**
     * Returns a built-in class, i.e. one supported by built-in expressions, that is closest to this resource class.
     *
     * @return the built-in class that is closest to this resource class
     */
    public abstract ResourceClass getBuiltinClass();


    public abstract List<String> getSqlTypes();


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
            return targetClass.fromGeneralClass(this, columns);
        else
            throw new IllegalArgumentException();
    }


    public final boolean isSubclassOf(ResourceClass resClass)
    {
        return equals(resClass) || isSubclassOf(this, resClass);
    }


    private boolean isSubclassOf(ResourceClass a, ResourceClass b)
    {
        //TODO: take into account that a primitive class can be composed of (finitely many) other primitive classes

        if(a instanceof UnionResourceClass ua)
            return ua.getClasses().stream().allMatch(c -> isSubclassOf(c, b));
        else if(b instanceof UnionResourceClass ub)
            return ub.getClasses().stream().anyMatch(c -> isSubclassOf(a, c));
        else
            return ((PrimitiveResourceClass) a).isSubclassOf((PrimitiveResourceClass) b);
    }


    public static boolean areDisjunct(ResourceClass class1, ResourceClass class2)
    {
        //TODO: take into account that a primitive class can be composed of (finitely many) other primitive classes
        //TODO: two classes can be non-disjunct even though one is not a subset of the other

        for(ResourceClass c1 : expandUnionClass(class1))
            for(ResourceClass c2 : expandUnionClass(class2))
                if(c1.isSubclassOf(c2) || c2.isSubclassOf(c1))
                    return false;

        return true;
    }


    public static boolean areDisjunct(ResourceClass resClass, Set<ResourceClass> resClasses)
    {
        return resClasses.stream().allMatch(c -> areDisjunct(resClass, c));
    }


    public static boolean areDisjunct(Set<ResourceClass> classes1, Set<ResourceClass> classes2)
    {
        return classes1.stream().allMatch(c1 -> ResourceClass.areDisjunct(c1, classes2));
    }


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


    public final String getName()
    {
        return name;
    }


    public static ResourceClass getExpressionClass(Set<ResourceClass> resClasses)
    {
        return getUnionClass(resClasses, (PrimitiveResourceClass) getExpressionClass(getUnionClass(resClasses)));
    }


    public static ResourceClass getExpressionClass(ResourceClass resClass)
    {
        if(resClass instanceof UnionResourceClass u)
            resClass = u.getEffectiveClass();

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


    public static ResourceClass getUnionClass(Set<ResourceClass> classes, PrimitiveResourceClass effectiveClass)
    {
        Set<ResourceClass> reduced = reduceClasses(classes);

        if(reduced.size() == 1 && reduced.iterator().next().equals(effectiveClass))
            return effectiveClass;

        return new UnionResourceClass(reduced, effectiveClass);
    }


    public static ResourceClass getUnionClass(Set<ResourceClass> classes)
    {
        Set<ResourceClass> reduced = reduceClasses(classes);

        if(reduced.size() == 1)
            return reduced.iterator().next();

        PrimitiveResourceClass effectiveClass = findEfectiveClass(reduced);

        return new UnionResourceClass(reduced, effectiveClass);
    }


    public static ResourceClass getUnionClass(ResourceClass... classes)
    {
        return getUnionClass(new HashSet<>(Arrays.asList(classes)));
    }


    public static Set<ResourceClass> expandUnionClasses(Set<ResourceClass> resClasses)
    {
        return resClasses.stream().flatMap(c -> expandUnionClass(c).stream()).collect(toSet());
    }


    public static Set<ResourceClass> expandUnionClass(ResourceClass resClass)
    {
        if(resClass instanceof UnionResourceClass union)
            return expandUnionClasses(union.getClasses());

        return Set.of(resClass);
    }


    protected static Set<ResourceClass> reduceClasses(Set<ResourceClass> classes)
    {
        Set<ResourceClass> bases = classes.stream()
                .flatMap(r -> r instanceof UnionResourceClass u ? u.getClasses().stream() : Stream.of(r))
                .collect(toSet());

        return bases.stream().filter(r -> bases.stream().noneMatch(c -> !r.equals(c) && r.isSubclassOf(c)))
                .collect(toSet());
    }


    private static Set<PrimitiveResourceClass> getEfectiveClassCandidates(Set<ResourceClass> classes)
    {
        Iterator<ResourceClass> it = classes.iterator();

        Set<PrimitiveResourceClass> candidates = new HashSet<>();

        PrimitiveResourceClass first = (PrimitiveResourceClass) it.next();

        for(ResourceClass r : first.getSuperClasses())
            candidates.add((PrimitiveResourceClass) r);

        candidates.add(first);

        while(it.hasNext())
        {
            Set<ResourceClass> set = new HashSet<>();

            PrimitiveResourceClass other = (PrimitiveResourceClass) it.next();

            set.addAll(other.getSuperClasses());
            set.add(other);

            candidates.retainAll(set);
        }

        return candidates;
    }


    private static PrimitiveResourceClass findEfectiveClass(Set<ResourceClass> classes)
    {
        Set<PrimitiveResourceClass> candidates = getEfectiveClassCandidates(classes);

        Iterator<PrimitiveResourceClass> it = candidates.iterator();
        PrimitiveResourceClass effectiveClass = it.next();

        while(it.hasNext())
        {
            PrimitiveResourceClass next = it.next();

            if(next.isSubclassOf(effectiveClass))
                effectiveClass = next;
        }

        return effectiveClass;
    }


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


    public static Set<ResourceClass> getDisjunctClasses(Set<ResourceClass> classes)
    {
        Collection<Set<ResourceClass>> out = UnionFind.getDisjunctEntries(classes, (l, r) -> !areDisjunct(l, r));

        return out.stream().map(s -> getUnionClass(s)).collect(toSet());
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
