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
        // the elements are handled by their indexes, since hashing them (sets of classes) is costly
        List<T> list = new ArrayList<>(classes);
        int[] parent = new int[list.size()];

        for(int i = 0; i < parent.length; i++)
            parent[i] = i;

        // a pair already connected is not tested, the relation being used only to connect, and nothing is tested once
        // all the elements are connected
        int components = list.size();

        for(int i = 0; i < list.size() && components > 1; i++)
        {
            for(int j = i + 1; j < list.size() && components > 1; j++)
            {
                if(findRootOfComponent(parent, i) != findRootOfComponent(parent, j)
                        && relation.test(list.get(i), list.get(j)))
                {
                    parent[findRootOfComponent(parent, i)] = findRootOfComponent(parent, j);
                    components--;
                }
            }
        }

        Map<Integer, Set<T>> groups = new HashMap<>();

        for(int i = 0; i < list.size(); i++)
            groups.computeIfAbsent(findRootOfComponent(parent, i), _ -> new HashSet<>()).add(list.get(i));

        return groups.values();
    }


    /**
     * Follows the parent links from {@code root} to the representative of its component, compressing the path.
     *
     * @param parent parent links of the union-find structure
     * @param root the index of the element to start from
     * @return the index of the representative of the component
     */
    private static int findRootOfComponent(int[] parent, int root)
    {
        while(parent[root] != root)
        {
            parent[root] = parent[parent[root]];
            root = parent[root];
        }

        return root;
    }
}
