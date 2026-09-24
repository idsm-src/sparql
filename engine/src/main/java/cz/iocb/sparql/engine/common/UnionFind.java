package cz.iocb.sparql.engine.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;



/**
 * Union-find partitioning of a set into the connected components of a relation.
 */
public class UnionFind
{
    /**
     * Not instantiable.
     */
    private UnionFind()
    {
    }


    /**
     * Splits {@code classes} into disjoint groups such that two elements share a group exactly when they are connected
     * by a chain of pairs satisfying {@code relation}.
     *
     * @param <T> the element type
     * @param classes the elements to partition
     * @param relation symmetric relation linking elements of one component
     * @return the connected components, each as a set of elements
     */
    public static <T> Collection<Set<T>> getDisjunctEntries(Set<T> classes, BiPredicate<T, T> relation)
    {
        Map<T, T> parent = new HashMap<>();

        for(T c : classes)
            parent.put(c, c);

        List<T> list = new ArrayList<>(classes);

        for(int i = 0; i < list.size(); i++)
            for(int j = i + 1; j < list.size(); j++)
                if(relation.test(list.get(i), list.get(j)))
                    parent.put(findRootOfComponent(parent, list.get(i)), findRootOfComponent(parent, list.get(j)));

        Map<T, Set<T>> groups = new HashMap<>();

        for(T c : classes)
            groups.computeIfAbsent(findRootOfComponent(parent, c), _ -> new HashSet<>()).add(c);

        return groups.values();
    }


    /**
     * Follows the parent links from {@code root} to the representative of its component.
     *
     * @param <T> the element type
     * @param parent parent links of the union-find structure
     * @param root the element to start from
     * @return the representative of the component
     */
    private static <T> T findRootOfComponent(Map<T, T> parent, T root)
    {
        while(!root.equals(parent.get(root)))
            root = parent.get(root);

        return root;
    }
}
