package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class built from primitive classes by union, intersection and difference. It is kept as a disjunction of
 * conjunctions of possibly negated primitive classes, and its values are stored in the columns of an effective
 * primitive class holding all of them: a minimal class among the common superclasses of the terms, where a positive
 * member of a term counts as its own superclass (so an intersection of two overlapping classes is stored in the columns
 * of one of them, and a union in those of a common superclass, the box unless something narrower fits).
 *
 * <p>
 * Whether a conjunction of two unrelated user IRI classes is empty depends on the declarations of the configuration
 * ({@link ClassRelations}), which the classes do not keep. The intersection and the difference, which form such
 * conjunctions, and the disjointness test therefore take the declarations as a parameter. The subclass test and the
 * union do not: a class that exists has no empty term and no negated user IRI class, so every conjunction of two user
 * IRI classes they meet comes from an existing term and is non-empty; they assume that every two unrelated user IRI
 * classes may overlap, which gives the same result as the declarations would (and a sound approximation for a class
 * with a negated user IRI class).
 */
public final class DerivedClass extends ResourceClass
{
    /**
     * Declarations assumed by the operations that do not take them: every two unrelated user IRI classes may overlap.
     */
    private static final ClassRelations OVERLAPPING = (_, _) -> true;
    /**
     * Disjunctive normal form: each term maps primitive classes to true (member) or false (excluded).
     */
    private final Set<Map<PrimitiveResourceClass, Boolean>> terms;

    /**
     * Primitive class whose columns store the values.
     */
    private final PrimitiveResourceClass effectiveClass;


    /**
     * Creates the class with an explicit effective class.
     *
     * @param terms the normal form
     * @param effectiveClass primitive class storing the values
     */
    private DerivedClass(Set<Map<PrimitiveResourceClass, Boolean>> terms, PrimitiveResourceClass effectiveClass)
    {
        super(generateName(terms, effectiveClass));

        this.terms = terms;
        this.effectiveClass = effectiveClass;
    }


    /**
     * Creates the class, selecting the effective class automatically.
     *
     * @param terms the normal form
     */
    private DerivedClass(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        this(terms, selectEfectiveClass(terms));
    }


    /**
     * Union of the classes stored in the columns of {@code effectiveClass}; collapses to a primitive class only when
     * the union is exactly {@code effectiveClass}.
     *
     * @param classes the classes
     * @param effectiveClass primitive class storing the values
     * @return union of the classes stored in the columns of {@code effectiveClass}; collapses to a primitive class only
     *         when the union is exactly {@code effectiveClass}
     */
    public static ResourceClass unionize(Set<ResourceClass> classes, PrimitiveResourceClass effectiveClass)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(OVERLAPPING, union(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(effectiveClass.equals(single))
            return single;

        return new DerivedClass(internal, effectiveClass);
    }


    /**
     * Union of the classes; a union equal to a single primitive class is returned as that class.
     *
     * @param classes the classes
     * @return union of the classes; a union equal to a single primitive class is returned as that class
     */
    public static ResourceClass unionize(Set<ResourceClass> classes)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(OVERLAPPING, union(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    /**
     * Union of the classes, see {@link #unionize(Set)}.
     *
     * @param classes the classes
     * @return union of the classes, see {@link #unionize(Set)}
     */
    public static ResourceClass unionize(ResourceClass... classes)
    {
        return unionize(new HashSet<>(Arrays.asList(classes)));
    }


    /**
     * Intersection of the classes; a result equal to a single primitive class is returned as that class, and an
     * intersection of disjoint classes is the empty class (no terms, null effective class).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param classes the classes
     * @return intersection of the classes; a result equal to a single primitive class is returned as that class
     */
    public static ResourceClass intersect(ClassRelations relations, Set<ResourceClass> classes)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(relations, intersection(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    /**
     * Intersection of the classes, see {@link #intersect(ClassRelations, Set)}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param classes the classes
     * @return intersection of the classes, see {@link #intersect(ClassRelations, Set)}
     */
    public static ResourceClass intersect(ClassRelations relations, ResourceClass... classes)
    {
        return intersect(relations, new HashSet<>(Arrays.asList(classes)));
    }


    /**
     * Intersection of classes whose result cannot depend on the declarations of a configuration, see
     * {@link #intersect(ClassRelations, Set)}: at most one user IRI class may occur among the classes of the operands,
     * as only the emptiness of a conjunction of two of them depends on the declarations.
     *
     * @param classes the classes
     * @return intersection of the classes
     * @throws IllegalArgumentException if two user IRI classes occur among the classes of the operands
     */
    public static ResourceClass intersect(Set<ResourceClass> classes)
    {
        checkIndependence(classes);

        return intersect(ClassRelations.NONE, classes);
    }


    /**
     * Intersection of the classes, see {@link #intersect(Set)}.
     *
     * @param classes the classes
     * @return intersection of the classes, see {@link #intersect(Set)}
     * @throws IllegalArgumentException if two user IRI classes occur among the classes of the operands
     */
    public static ResourceClass intersect(ResourceClass... classes)
    {
        return intersect(new HashSet<>(Arrays.asList(classes)));
    }


    /**
     * The values of {@code a} that are not values of {@code b}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one operand
     * @param b the other operand
     * @return the values of {@code a} that are not values of {@code b}
     */
    public static ResourceClass subtract(ClassRelations relations, ResourceClass a, ResourceClass b)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(relations,
                intersection(getTerms(a), complement(getTerms(b))));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    /**
     * The values of {@code a} that are not values of {@code b}, for classes whose result cannot depend on the
     * declarations of a configuration (see {@link #intersect(Set)}): at most one user IRI class may occur among the
     * classes of the operands.
     *
     * @param a one operand
     * @param b the other operand
     * @return the values of {@code a} that are not values of {@code b}
     * @throws IllegalArgumentException if two user IRI classes occur among the classes of the operands
     */
    public static ResourceClass subtract(ResourceClass a, ResourceClass b)
    {
        checkIndependence(List.of(a, b));

        return subtract(ClassRelations.NONE, a, b);
    }


    /**
     * Checks that the declarations of a configuration cannot influence an operation over the classes: at most one user
     * IRI class occurs among the classes of the operands.
     *
     * @param classes the operands
     * @throws IllegalArgumentException if two user IRI classes occur among the classes of the operands
     */
    private static void checkIndependence(Collection<ResourceClass> classes)
    {
        Set<UserIriClass> users = new HashSet<>();

        for(ResourceClass resClass : classes)
            collectUserIriClasses(resClass, users);

        if(users.size() > 1)
            throw new IllegalArgumentException("the operation on " + classes + " depends on the declarations of the"
                    + " configuration about " + users + ", which have to be supplied");
    }


    /**
     * User IRI classes occurring in the class: the class itself, the components of a triple term class (recursively),
     * or the literals of a derived class.
     *
     * @param resClass the resource class
     * @return user IRI classes occurring in the class
     */
    static Set<UserIriClass> getUserIriClasses(ResourceClass resClass)
    {
        Set<UserIriClass> result = new HashSet<>();

        collectUserIriClasses(resClass, result);

        return result;
    }


    /**
     * Adds the user IRI classes occurring in the class to the set, see {@link #getUserIriClasses}.
     *
     * @param resClass the resource class
     * @param result the set to add to
     */
    private static void collectUserIriClasses(ResourceClass resClass, Set<UserIriClass> result)
    {
        if(resClass instanceof UserIriClass user)
        {
            result.add(user);
        }
        else if(resClass instanceof TripleTermClass triple)
        {
            collectUserIriClasses(triple.getSubject(), result);
            collectUserIriClasses(triple.getPredicate(), result);
            collectUserIriClasses(triple.getObject(), result);
        }
        else if(resClass instanceof DerivedClass derived)
        {
            for(Map<PrimitiveResourceClass, Boolean> term : derived.terms)
                for(PrimitiveResourceClass literal : term.keySet())
                    collectUserIriClasses(literal, result);
        }
    }


    /**
     * Disjointness test on the normal forms of the classes (works for derived and primitive classes alike).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one operand
     * @param b the other operand
     * @return true if no term belongs to both classes, false otherwise
     */
    public static boolean areDisjunct(ClassRelations relations, ResourceClass a, ResourceClass b)
    {
        return isEmptyIntersection(relations, getTerms(a), getTerms(b));
    }


    /**
     * Subclass test on the normal forms of the classes (works for derived and primitive classes alike); it needs no
     * declarations, see the class comment.
     *
     * @param a one operand
     * @param b the other operand
     * @return true if every value of {@code a} is a value of {@code b}, false otherwise
     */
    public static boolean isSubclassOf(ResourceClass a, ResourceClass b)
    {
        return isSubclassOf(OVERLAPPING, getTerms(a), getTerms(b));
    }


    /**
     * Over-approximates the classes by the primitive classes occurring positively in them, dropping those that are
     * subclasses of other members.
     *
     * @param classes the classes
     * @return the approximating primitive classes
     */
    public static Set<PrimitiveResourceClass> estimateAsUnion(Set<ResourceClass> classes)
    {
        return reduceSubclasses(classes.stream().flatMap(c -> estimateAsUnion(c).stream()).collect(toSet()));
    }


    /**
     * Over-approximates the class by the primitive classes occurring positively in it (negations are ignored), dropping
     * those that are subclasses of other members.
     *
     * @param resClass the resource class
     * @return the approximating primitive classes
     */
    public static Set<PrimitiveResourceClass> estimateAsUnion(ResourceClass resClass)
    {
        if(resClass instanceof PrimitiveResourceClass primitive)
            return Set.of(primitive);

        return reduceSubclasses(getTerms(resClass).stream()
                .flatMap(t -> t.entrySet().stream().filter(e -> e.getValue()).map(e -> e.getKey())).collect(toSet()));
    }


    /**
     * Union of normal forms: all terms together.
     *
     * @param branches the normal forms to combine
     * @return the combined normal form
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> union(
            Set<Set<Map<PrimitiveResourceClass, Boolean>>> branches)
    {
        return branches.stream().flatMap(s -> s.stream()).collect(toSet());
    }


    /**
     * Intersection of several normal forms.
     *
     * @param branches the normal forms to combine
     * @return the intersected normal form
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> intersection(
            Set<Set<Map<PrimitiveResourceClass, Boolean>>> branches)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = Set.of(Map.of());

        for(Set<Map<PrimitiveResourceClass, Boolean>> branch : branches)
            result = intersection(result, branch);

        return result;
    }


    /**
     * Intersection of two normal forms: pairwise conjunction of their terms, dropping contradictory ones.
     *
     * @param a one normal form
     * @param b the other normal form
     * @return the intersected normal form
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> intersection(Set<Map<PrimitiveResourceClass, Boolean>> a,
            Set<Map<PrimitiveResourceClass, Boolean>> b)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = new HashSet<>();

        for(Map<PrimitiveResourceClass, Boolean> aTerm : a)
        {
            for(Map<PrimitiveResourceClass, Boolean> bTerm : b)
            {
                Map<PrimitiveResourceClass, Boolean> intersection = intersection(aTerm, bTerm);

                if(intersection != null)
                    result.add(intersection);
            }
        }

        return result;
    }


    /**
     * Conjunction of two terms; null when a class occurs with opposite signs.
     *
     * @param a one term
     * @param b the other term
     * @return the intersected normal form
     */
    private static Map<PrimitiveResourceClass, Boolean> intersection(Map<PrimitiveResourceClass, Boolean> a,
            Map<PrimitiveResourceClass, Boolean> b)
    {
        Map<PrimitiveResourceClass, Boolean> result = new HashMap<>();

        result.putAll(a);

        for(Entry<PrimitiveResourceClass, Boolean> e : b.entrySet())
            if(Objects.equals(result.put(e.getKey(), e.getValue()), !e.getValue()))
                return null;

        return result;
    }


    /**
     * Complement of a normal form (De Morgan): intersection of the negations of its terms.
     *
     * @param terms the normal form
     * @return complement of a normal form (De Morgan): intersection of the negations of its terms
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> complement(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        Set<Set<Map<PrimitiveResourceClass, Boolean>>> branches = new HashSet<>();

        for(Map<PrimitiveResourceClass, Boolean> term : terms)
        {
            Set<Map<PrimitiveResourceClass, Boolean>> branch = new HashSet<>();
            branches.add(branch);

            for(Entry<PrimitiveResourceClass, Boolean> literal : term.entrySet())
                branch.add(Map.of(literal.getKey(), !literal.getValue()));
        }

        return intersection(branches);
    }


    /**
     * True if every term of {@code left} is covered by the terms of {@code right}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the normal form to test
     * @param right the covering normal form
     * @return true if every value of {@code a} is a value of {@code b}, false otherwise
     */
    private static boolean isSubclassOf(ClassRelations relations, Set<Map<PrimitiveResourceClass, Boolean>> left,
            Set<Map<PrimitiveResourceClass, Boolean>> right)
    {
        ArrayList<Map<PrimitiveResourceClass, Boolean>> rightTerms = new ArrayList<>(right);

        rightTerms.sort(Comparator.comparingInt(term -> term.size()));

        for(Map<PrimitiveResourceClass, Boolean> leftTerm : left)
        {
            assert !isEmpty(relations, leftTerm);

            if(existsOutsideRight(relations, leftTerm, rightTerms, 0))
                return false;
        }

        return true;
    }


    /**
     * True if some value of {@code left} lies outside all terms of {@code right} from {@code index} on; recursion
     * splits {@code left} by the literals of the current right term.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the term to test
     * @param right the covering terms
     * @param index index of the right term to start at
     * @return true if some value of {@code left} lies outside all terms of {@code right} from {@code index} on, false
     *         otherwise
     */
    private static boolean existsOutsideRight(ClassRelations relations, Map<PrimitiveResourceClass, Boolean> left,
            List<Map<PrimitiveResourceClass, Boolean>> right, int index)
    {
        if(isEmpty(relations, left))
            return false;

        if(index == right.size())
            return true;

        Map<PrimitiveResourceClass, Boolean> rightTerm = right.get(index);

        if(isEmpty(relations, left, rightTerm))
            return existsOutsideRight(relations, left, right, index + 1);

        for(Entry<PrimitiveResourceClass, Boolean> literal : rightTerm.entrySet())
        {
            Map<PrimitiveResourceClass, Boolean> newCurrent = new HashMap<>(left);

            if(literal.getValue().equals(newCurrent.put(literal.getKey(), !literal.getValue())))
                continue;

            if(existsOutsideRight(relations, newCurrent, right, index + 1))
                return true;
        }

        return false;
    }


    /**
     * True if no term of {@code a} overlaps a term of {@code b}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one normal form
     * @param b the other normal form
     * @return true if no term of {@code a} overlaps a term of {@code b}, false otherwise
     */
    private static boolean isEmptyIntersection(ClassRelations relations, Set<Map<PrimitiveResourceClass, Boolean>> a,
            Set<Map<PrimitiveResourceClass, Boolean>> b)
    {
        for(Map<PrimitiveResourceClass, Boolean> aTerm : a)
            for(Map<PrimitiveResourceClass, Boolean> bTerm : b)
                if(!isEmpty(relations, aTerm, bTerm))
                    return false;

        return true;
    }


    /**
     * True if the conjunction of the two terms is empty; assumes the class hierarchy has the 2-Helly property (pairwise
     * overlapping literals overlap jointly).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one term
     * @param b the other term
     * @return true if the conjunction of the two terms is empty, false otherwise
     */
    private static boolean isEmpty(ClassRelations relations, Map<PrimitiveResourceClass, Boolean> a,
            Map<PrimitiveResourceClass, Boolean> b)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : a.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : b.entrySet())
                if(isEmpty(relations, e1, e2))
                    return true;

        return false;
    }


    /**
     * True if the term is contradictory.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param term the term
     * @return true if the term is contradictory, false otherwise
     */
    private static boolean isEmpty(ClassRelations relations, Map<PrimitiveResourceClass, Boolean> term)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : term.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : term.entrySet())
                if(isEmpty(relations, e1, e2))
                    return true;

        return false;
    }


    /**
     * True if two signed classes exclude each other: the same class with opposite signs, a positive subclass with its
     * negated superclass, or two positive unrelated classes that are disjoint (which unrelated primitive classes are,
     * except for two triple term classes with overlapping components and two user IRI classes the declarations let
     * overlap).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param a one signed class
     * @param b the other signed class
     * @return true if two signed classes exclude each other, false otherwise
     */
    private static boolean isEmpty(ClassRelations relations, Entry<PrimitiveResourceClass, Boolean> a,
            Entry<PrimitiveResourceClass, Boolean> b)
    {
        if(a.getKey().equals(b.getKey()))
        {
            if(!a.getValue().equals(b.getValue()))
                return true;
        }
        else if(a.getKey().isSubclassOf(b.getKey()))
        {
            if(a.getValue() && !b.getValue())
                return true;
        }
        else if(b.getKey().isSubclassOf(a.getKey()))
        {
            if(!a.getValue() && b.getValue())
                return true;
        }
        else // a.getKey() and b.getKey() are distinct
        {
            if(a.getValue() && b.getValue())
                return ResourceClass.areDisjunct(relations, a.getKey(), b.getKey());
        }

        return false;
    }


    /**
     * Simplifies a normal form: drops empty and covered terms and redundant literals until nothing changes.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param input the normal form
     * @return the simplified normal form
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> normalize(ClassRelations relations,
            Set<Map<PrimitiveResourceClass, Boolean>> input)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> terms = prepareTerms(relations, input);

        while(true)
        {
            if(removeOneCoveredTerm(relations, terms))
                continue;

            if(removeOneRedundantLiteral(relations, terms))
                continue;

            return terms;
        }
    }


    /**
     * Copies the normal form without its contradictory terms.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param input the normal form
     * @return the normal form without its contradictory terms
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> prepareTerms(ClassRelations relations,
            Set<Map<PrimitiveResourceClass, Boolean>> input)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = new HashSet<>();

        for(Map<PrimitiveResourceClass, Boolean> term : input)
            if(!isEmpty(relations, term))
                result.add(term);

        return result;
    }


    /**
     * Removes one term implied by the other terms; true if some was removed.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param terms the normal form
     * @return true if a term was removed, false otherwise
     */
    private static boolean removeOneCoveredTerm(ClassRelations relations,
            Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        for(Map<PrimitiveResourceClass, Boolean> testedTerm : terms)
        {
            Set<Map<PrimitiveResourceClass, Boolean>> otherTerms = new HashSet<>(terms);
            otherTerms.remove(testedTerm);

            if(isSubclassOf(relations, Set.of(testedTerm), otherTerms))
            {
                terms.remove(testedTerm);
                return true;
            }
        }

        return false;
    }


    /**
     * Drops one literal whose removal keeps the term within the union; true if some was dropped.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param terms the normal form
     * @return true if a literal was dropped, false otherwise
     */
    private static boolean removeOneRedundantLiteral(ClassRelations relations,
            Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        for(Map<PrimitiveResourceClass, Boolean> originalTerm : terms)
        {
            for(PrimitiveResourceClass c : originalTerm.keySet())
            {
                Map<PrimitiveResourceClass, Boolean> reducedTerm = new HashMap<>(originalTerm);
                reducedTerm.remove(c);

                if(isSubclassOf(relations, Set.of(reducedTerm), terms))
                {
                    terms.remove(originalTerm);
                    terms.add(reducedTerm);
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Removes the classes that are subclasses of other members.
     *
     * @param classes the classes
     * @return the remaining classes
     */
    private static Set<PrimitiveResourceClass> reduceSubclasses(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(c -> !r.equals(c) && r.isSubclassOf(c)))
                .collect(toSet());
    }


    /**
     * Removes the classes that are superclasses of other members.
     *
     * @param classes the classes
     * @return the remaining classes
     */
    static Set<PrimitiveResourceClass> reduceSuperclasses(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(c -> !r.equals(c) && c.isSubclassOf(r)))
                .collect(toSet());
    }


    /**
     * Deterministic choice among incomparable classes: the one with the fewest columns, and among those the first by
     * name; null when there is none.
     *
     * @param classes the classes
     * @return the chosen class, or null when there is none
     */
    static PrimitiveResourceClass select(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().min(Comparator.comparingInt((PrimitiveResourceClass c) -> c.getColumnCount())
                .thenComparing(c -> c.getResourceName())).orElse(null);
    }


    /**
     * Normal forms of the classes.
     *
     * @param classes the classes
     * @return normal forms of the classes
     */
    private static Set<Set<Map<PrimitiveResourceClass, Boolean>>> getTerms(Set<ResourceClass> classes)
    {
        return classes.stream().map(c -> getTerms(c)).collect(toSet());
    }


    /**
     * Normal form of a class: its own terms for a derived class, a single positive literal for a primitive one.
     *
     * @param resClass the resource class
     * @return normal form of a class: its own terms for a derived class, a single positive literal for a primitive one
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> getTerms(ResourceClass resClass)
    {
        if(resClass instanceof DerivedClass compositeClass)
            return compositeClass.terms;
        else if(resClass instanceof PrimitiveResourceClass primitiveClass)
            return Set.of(Map.of(primitiveClass, true));
        else
            throw new IllegalArgumentException();
    }


    /**
     * The primitive class a normal form denotes, or null when it is not a single positive literal.
     *
     * @param terms the normal form
     * @return the primitive class a normal form denotes, or null when it is not a single positive literal
     */
    private static PrimitiveResourceClass extract(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        if(terms.size() != 1)
            return null;

        Map<PrimitiveResourceClass, Boolean> term = terms.iterator().next();

        if(term.size() != 1)
            return null;

        Entry<PrimitiveResourceClass, Boolean> lit = term.entrySet().iterator().next();

        if(!lit.getValue())
            return null;

        return lit.getKey();
    }


    /**
     * Name of a derived class: the effective class followed by {@code @} and the normal form.
     *
     * @param terms the normal form
     * @param effectiveClass primitive class storing the values
     * @return name of a derived class: the effective class followed by {@code @} and the normal form
     */
    private static String generateName(Set<Map<PrimitiveResourceClass, Boolean>> terms,
            PrimitiveResourceClass effectiveClass)
    {
        if(effectiveClass == null)
            return "null@()";

        return effectiveClass.getResourceName() + "@" + generateName(terms);
    }


    /**
     * Terms joined by {@code |}, sorted.
     *
     * @param terms the normal form
     * @return terms joined by {@code |}, sorted
     */
    private static String generateName(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        return terms.stream().map(t -> generateName(t)).sorted().collect(joining("|"));
    }


    /**
     * Literals of a term joined by {@code &}, sorted.
     *
     * @param term the term
     * @return literals of a term joined by {@code &}, sorted
     */
    private static String generateName(Map<PrimitiveResourceClass, Boolean> term)
    {
        return term.entrySet().stream().map(e -> generateName(e)).sorted().collect(joining("&"));
    }


    /**
     * A literal: the class name, prefixed by {@code !} when negated.
     *
     * @param e the signed class
     * @return A literal: the class name, prefixed by {@code !} when negated
     */
    private static String generateName(Entry<PrimitiveResourceClass, Boolean> e)
    {
        return (e.getValue() ? "" : "!") + e.getKey().getResourceName();
    }


    /**
     * Class holding all values of the normal form: a minimal class among the superclasses common to all terms (see
     * {@link #getEfectiveClassCandidates(Map)}), the box at worst; null for an empty normal form. Several classes are
     * minimal for an intersection of overlapping unrelated classes, which any of them holds; {@link #select} then
     * decides.
     *
     * @param terms the normal form
     * @return class holding all values of the normal form, or null for an empty normal form
     */
    private static PrimitiveResourceClass selectEfectiveClass(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        return select(reduceSuperclasses(getEfectiveClassCandidates(terms)));
    }


    /**
     * Superclasses common to all terms.
     *
     * @param terms the normal form
     * @return superclasses common to all terms
     */
    private static Set<PrimitiveResourceClass> getEfectiveClassCandidates(
            Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        if(terms.size() == 0)
            return Set.of();

        Iterator<Map<PrimitiveResourceClass, Boolean>> it = terms.iterator();

        Set<PrimitiveResourceClass> candidates = getEfectiveClassCandidates(it.next());

        while(it.hasNext())
            candidates.retainAll(getEfectiveClassCandidates(it.next()));

        return candidates;
    }


    /**
     * Positive classes of the term with their superclasses, plus the box.
     *
     * @param term the term
     * @return positive classes of the term with their superclasses, plus the box
     */
    private static Set<PrimitiveResourceClass> getEfectiveClassCandidates(Map<PrimitiveResourceClass, Boolean> term)
    {
        Set<PrimitiveResourceClass> candidates = new HashSet<>(Set.of(box));

        for(Entry<PrimitiveResourceClass, Boolean> lit : term.entrySet())
        {
            if(lit.getValue())
            {
                candidates.addAll(lit.getKey().getSuperClasses());
                candidates.add(lit.getKey());
            }
        }

        return candidates;
    }


    /**
     * True if the term satisfies some term of the normal form; the primitive classes are matched through the request
     * when there is one, so that the detected class of an IRI is reused instead of matching each class anew.
     */
    @Override
    public boolean match(Request request, RdfTerm term)
    {
        return terms.stream()
                .anyMatch(t -> t.entrySet().stream().allMatch(e -> match(request, e.getKey(), term) == e.getValue()));
    }


    /**
     * True if the term is representable in the primitive class, decided by the request when there is one.
     *
     * @param request the current request, or null
     * @param resClass the primitive class
     * @param term the RDF term
     * @return true if the term is representable in the primitive class, false otherwise
     */
    private static boolean match(Request request, PrimitiveResourceClass resClass, RdfTerm term)
    {
        return request != null ? request.match(resClass, term) : resClass.match(null, term);
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return effectiveClass.getResultResourceClass();
    }


    @Override
    public List<Column> toColumns(Request request, RdfTerm term)
    {
        return effectiveClass.toColumns(request, term);
    }


    /**
     * Converts the columns of the effective class to the columns of the effective class of the superclass, through a
     * common superclass when they are unrelated; no representability check is needed, as every value of this class
     * belongs to the superclass.
     */
    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        return convert(effectiveClass, superClass.getEffectiveClass(), columns, canBeNull, true);
    }


    /**
     * Converts the columns of the effective class of the superclass to the columns of the effective class, through a
     * common superclass when they are unrelated. Unless the check may be skipped, a value outside this class yields
     * NULL columns: the conversion of the effective class yields them for a value outside it, and the columns are
     * wrapped in a CASE over the membership tests of the literals of the normal form the superclass does not imply.
     */
    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        assert isSubclassOf(superClass);

        List<Column> result = convert(superClass.getEffectiveClass(), effectiveClass, columns, true, checkOptional);

        if(checkOptional)
            return result;

        String condition = getMembershipCondition(superClass, columns);

        if(condition.equals("true"))
            return result;

        if(condition.equals("false"))
            return effectiveClass.getSqlTypes().stream().map(t -> (Column) new NullColumn(t)).toList();

        return result.stream().map(c -> expression(c.getType(), "CASE WHEN %s THEN %s END", condition, c)).toList();
    }


    /**
     * SQL condition that a value of the superclass, given in the columns of its effective class, belongs to this class:
     * a disjunction over the terms of the conjunctions of the tests of their literals; {@code true} when the membership
     * is implied by the superclass and the effective class, {@code false} when the conversions exclude it.
     *
     * @param superClass class of the value
     * @param columns the columns representing the value in the effective class of the superclass
     * @return SQL condition that the value belongs to this class
     */
    private String getMembershipCondition(ResourceClass superClass, List<Column> columns)
    {
        List<String> alternatives = new ArrayList<>();

        for(Map<PrimitiveResourceClass, Boolean> term : terms)
        {
            List<String> tests = new ArrayList<>();

            for(Entry<PrimitiveResourceClass, Boolean> literal : term.entrySet())
                tests.add(getMembershipCondition(superClass, columns, literal.getKey(), literal.getValue()));

            alternatives.add(and(tests));
        }

        return or(alternatives);
    }


    /**
     * SQL condition that a value of the superclass, given in the columns of its effective class, belongs (or, for a
     * negated literal, does not belong) to the primitive class: {@code true} or {@code false} when the superclass is a
     * subclass of the primitive class, or the effective class of this class is (a value outside the effective class
     * gets NULL columns anyway); otherwise the null test of a determining column of the value converted to the
     * primitive class, which is a constant when the conversion of the given columns is.
     *
     * @param superClass class of the value
     * @param columns the columns representing the value in the effective class of the superclass
     * @param resClass the primitive class
     * @param positive false for a negated literal
     * @return SQL condition that the value belongs (or does not belong) to the primitive class
     */
    private String getMembershipCondition(ResourceClass superClass, List<Column> columns,
            PrimitiveResourceClass resClass, boolean positive)
    {
        if(superClass.isSubclassOf(resClass) || effectiveClass.isSubclassOf(resClass))
            return Boolean.toString(positive);

        List<Column> converted = convert(superClass.getEffectiveClass(), resClass, columns, true, false);

        int index = IntStream.range(0, converted.size()).filter(i -> !resClass.isOptionalColumn(i)).findFirst()
                .orElseThrow();

        Column column = converted.get(index);

        if(column instanceof NullColumn)
            return Boolean.toString(!positive);

        if(column instanceof ConstantColumn)
            return Boolean.toString(positive);

        return column + (positive ? " IS NOT NULL" : " IS NULL");
    }


    /**
     * Conjunction of the conditions: {@code false} if some condition is {@code false}, {@code true} if none remains
     * otherwise, the remaining conditions joined by {@code AND} in parentheses otherwise.
     *
     * @param conditions the conditions
     * @return the conjunction
     */
    private static String and(List<String> conditions)
    {
        List<String> list = conditions.stream().filter(c -> !c.equals("true")).distinct().sorted().toList();

        if(list.contains("false"))
            return "false";

        if(list.isEmpty())
            return "true";

        if(list.size() == 1)
            return list.get(0);

        return list.stream().collect(joining(" AND ", "(", ")"));
    }


    /**
     * Disjunction of the conditions: {@code true} if some condition is {@code true}, {@code false} if none remains
     * otherwise, the remaining conditions joined by {@code OR} in parentheses otherwise.
     *
     * @param conditions the conditions
     * @return the disjunction
     */
    private static String or(List<String> conditions)
    {
        List<String> list = conditions.stream().filter(c -> !c.equals("false")).distinct().sorted().toList();

        if(list.contains("true"))
            return "true";

        if(list.isEmpty())
            return "false";

        if(list.size() == 1)
            return list.get(0);

        return list.stream().collect(joining(" OR ", "(", ")"));
    }


    @Override
    public PrimitiveResourceClass getEffectiveClass()
    {
        return effectiveClass;
    }


    @Override
    public List<SqlType> getSqlTypes()
    {
        return effectiveClass.getSqlTypes();
    }


    @Override
    public int getColumnCount()
    {
        return effectiveClass.getColumnCount();
    }


    @Override
    public boolean isOptionalColumn(int index)
    {
        return effectiveClass.isOptionalColumn(index);
    }


    @Override
    public int hashCode()
    {
        return terms.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || !super.equals(object) || getClass() != object.getClass())
            return false;

        DerivedClass other = (DerivedClass) object;

        return Objects.equals(effectiveClass, other.effectiveClass) && Objects.equals(terms, other.terms);
    }
}
