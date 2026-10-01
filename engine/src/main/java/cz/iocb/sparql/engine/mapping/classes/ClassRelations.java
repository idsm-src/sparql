package cz.iocb.sparql.engine.mapping.classes;

import java.util.Set;



/**
 * Declarations of a configuration about its user IRI classes that the class algebra cannot read from the classes
 * themselves: whether two user IRI classes that are neither equal nor related (one a subclass of the other) may share
 * IRIs. Unrelated classes are disjoint unless declared otherwise, which lets the translator prune joins and comparisons
 * between them. The declarations depend on the classes registered together, so they are kept by the configuration
 * ({@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration}) and are supplied to every operation that has to
 * decide whether two unrelated user IRI classes are disjoint: {@link ResourceClass#areDisjunct},
 * {@link DerivedClass#intersect} and {@link DerivedClass#subtract}, and the methods built on them.
 */
public interface ClassRelations
{
    /**
     * Declarations declaring no overlap: every two unrelated user IRI classes are disjoint. They serve the algebra over
     * the built-in classes, whose disjointness does not depend on any declaration; the operations without the
     * declarations parameter ({@link ResourceClass#areDisjunct(ResourceClass, ResourceClass)},
     * {@link DerivedClass#intersect(Set)}, {@link DerivedClass#subtract(ResourceClass, ResourceClass)}) use them and
     * refuse operands whose result would depend on the declarations.
     */
    ClassRelations NONE = (_, _) -> false;


    /**
     * True if the two user IRI classes, which are neither equal nor related, may share IRIs.
     *
     * @param a one user IRI class
     * @param b the other user IRI class
     * @return true if the two user IRI classes may share IRIs, false otherwise
     */
    boolean mayOverlap(UserIriClass a, UserIriClass b);
}
