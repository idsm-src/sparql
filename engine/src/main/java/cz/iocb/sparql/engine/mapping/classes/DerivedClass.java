package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.RdfTerm;



/**
 * Resource class built from primitive classes by union, intersection and difference. It is kept as a disjunction of
 * conjunctions of possibly negated primitive classes, and its values are stored in the columns of an effective
 * primitive class that is a superclass of all its positive members (the box unless something narrower fits).
 */
public class DerivedClass extends ResourceClass
{
    /**
     * Disjunctive normal form: each term maps primitive classes to true (member) or false (excluded).
     */
    private final Set<Map<PrimitiveResourceClass, Boolean>> terms;

    /**
     * Primitive class whose columns store the values.
     */
    private final PrimitiveResourceClass effectiveClass;

    /**
     * Result classes the values may appear in.
     */
    private final Set<ResultResourceClass> resultClasses;


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
        this.resultClasses = generateResultClasses(terms);
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
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(union(getTerms(classes)));

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
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(union(getTerms(classes)));

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
     * Intersection of the classes; a result equal to a single primitive class is returned as that class.
     *
     * @param classes the classes
     * @return intersection of the classes; a result equal to a single primitive class is returned as that class
     */
    public static ResourceClass intersect(Set<ResourceClass> classes)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(intersection(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    /**
     * Intersection of the classes, see {@link #intersect(Set)}.
     *
     * @param classes the classes
     * @return intersection of the classes, see {@link #intersect(Set)}
     */
    public static ResourceClass intersect(ResourceClass... classes)
    {
        return intersect(new HashSet<>(Arrays.asList(classes)));
    }


    /**
     * The values of {@code a} that are not values of {@code b}.
     *
     * @param a one operand
     * @param b the other operand
     * @return the values of {@code a} that are not values of {@code b}
     */
    public static ResourceClass subtract(ResourceClass a, ResourceClass b)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(
                intersection(getTerms(a), complement(getTerms(b))));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    /**
     * Disjointness test on the normal forms of the classes (works for derived and primitive classes alike).
     *
     * @param a one operand
     * @param b the other operand
     * @return true if no term belongs to both classes, false otherwise
     */
    public static boolean areDisjunct(ResourceClass a, ResourceClass b)
    {
        return isEmptyIntersection(getTerms(a), getTerms(b));
    }


    /**
     * Subclass test on the normal forms of the classes (works for derived and primitive classes alike).
     *
     * @param a one operand
     * @param b the other operand
     * @return true if every value of {@code a} is a value of {@code b}, false otherwise
     */
    public static boolean isSubclassOf(ResourceClass a, ResourceClass b)
    {
        return isSubclassOf(getTerms(a), getTerms(b));
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
     * @param left the normal form to test
     * @param right the covering normal form
     * @return true if every value of {@code a} is a value of {@code b}, false otherwise
     */
    private static boolean isSubclassOf(Set<Map<PrimitiveResourceClass, Boolean>> left,
            Set<Map<PrimitiveResourceClass, Boolean>> right)
    {
        ArrayList<Map<PrimitiveResourceClass, Boolean>> rightTerms = new ArrayList<>(right);

        rightTerms.sort(Comparator.comparingInt(term -> term.size()));

        for(Map<PrimitiveResourceClass, Boolean> leftTerm : left)
        {
            assert !isEmpty(leftTerm);

            if(existsOutsideRight(leftTerm, rightTerms, 0))
                return false;
        }

        return true;
    }


    /**
     * True if some value of {@code left} lies outside all terms of {@code right} from {@code index} on; recursion
     * splits {@code left} by the literals of the current right term.
     *
     * @param <L> unused type parameter
     * @param left the term to test
     * @param right the covering terms
     * @param index index of the right term to start at
     * @return true if some value of {@code left} lies outside all terms of {@code right} from {@code index} on, false
     *         otherwise
     */
    private static <L> boolean existsOutsideRight(Map<PrimitiveResourceClass, Boolean> left,
            List<Map<PrimitiveResourceClass, Boolean>> right, int index)
    {
        if(isEmpty(left))
            return false;

        if(index == right.size())
            return true;

        Map<PrimitiveResourceClass, Boolean> rightTerm = right.get(index);

        if(isEmpty(left, rightTerm))
            return existsOutsideRight(left, right, index + 1);

        for(Entry<PrimitiveResourceClass, Boolean> literal : rightTerm.entrySet())
        {
            Map<PrimitiveResourceClass, Boolean> newCurrent = new HashMap<>(left);

            if(literal.getValue().equals(newCurrent.put(literal.getKey(), !literal.getValue())))
                continue;

            if(existsOutsideRight(newCurrent, right, index + 1))
                return true;
        }

        return false;
    }


    /**
     * True if no term of {@code a} overlaps a term of {@code b}.
     *
     * @param a one normal form
     * @param b the other normal form
     * @return true if no term of {@code a} overlaps a term of {@code b}, false otherwise
     */
    private static boolean isEmptyIntersection(Set<Map<PrimitiveResourceClass, Boolean>> a,
            Set<Map<PrimitiveResourceClass, Boolean>> b)
    {
        for(Map<PrimitiveResourceClass, Boolean> aTerm : a)
            for(Map<PrimitiveResourceClass, Boolean> bTerm : b)
                if(!isEmpty(aTerm, bTerm))
                    return false;

        return true;
    }


    /**
     * True if the conjunction of the two terms is empty; assumes the class hierarchy has the 2-Helly property (pairwise
     * overlapping literals overlap jointly).
     *
     * @param a one term
     * @param b the other term
     * @return true if the conjunction of the two terms is empty, false otherwise
     */
    private static boolean isEmpty(Map<PrimitiveResourceClass, Boolean> a, Map<PrimitiveResourceClass, Boolean> b)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : a.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : b.entrySet())
                if(isEmpty(e1, e2))
                    return true;

        return false;
    }


    /**
     * True if the term is contradictory.
     *
     * @param term the term
     * @return true if the term is contradictory, false otherwise
     */
    private static boolean isEmpty(Map<PrimitiveResourceClass, Boolean> term)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : term.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : term.entrySet())
                if(isEmpty(e1, e2))
                    return true;

        return false;
    }


    /**
     * True if two signed classes exclude each other: the same class with opposite signs, a positive subclass with its
     * negated superclass, or two positive unrelated (hence disjoint) classes.
     *
     * @param a one signed class
     * @param b the other signed class
     * @return true if two signed classes exclude each other, false otherwise
     */
    private static boolean isEmpty(Entry<PrimitiveResourceClass, Boolean> a, Entry<PrimitiveResourceClass, Boolean> b)
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
        else // a.getKey() and b.getValue() are distinct
        {
            if(a.getValue() && b.getValue())
                return true;
        }

        //TODO: add the possibility that classes are not disjunct

        return false;
    }


    /**
     * Simplifies a normal form: drops empty and covered terms and redundant literals until nothing changes.
     *
     * @param input the normal form
     * @return the simplified normal form
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> normalize(Set<Map<PrimitiveResourceClass, Boolean>> input)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> terms = prepareTerms(input);

        while(true)
        {
            if(removeOneCoveredTerm(terms))
                continue;

            if(removeOneRedundantLiteral(terms))
                continue;

            return terms;
        }
    }


    /**
     * Copies the normal form without its contradictory terms.
     *
     * @param input the normal form
     * @return the normal form without its contradictory terms
     */
    private static Set<Map<PrimitiveResourceClass, Boolean>> prepareTerms(
            Set<Map<PrimitiveResourceClass, Boolean>> input)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = new HashSet<>();

        for(Map<PrimitiveResourceClass, Boolean> term : input)
            if(!isEmpty(term))
                result.add(term);

        return result;
    }


    /**
     * Removes one term implied by the other terms; true if some was removed.
     *
     * @param terms the normal form
     * @return true if a term was removed, false otherwise
     */
    private static boolean removeOneCoveredTerm(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        for(Map<PrimitiveResourceClass, Boolean> testedTerm : terms)
        {
            Set<Map<PrimitiveResourceClass, Boolean>> otherTerms = new HashSet<>(terms);
            otherTerms.remove(testedTerm);

            if(isSubclassOf(Set.of(testedTerm), otherTerms))
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
     * @param terms the normal form
     * @return true if a literal was dropped, false otherwise
     */
    private static boolean removeOneRedundantLiteral(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        for(Map<PrimitiveResourceClass, Boolean> originalTerm : terms)
        {
            for(PrimitiveResourceClass c : originalTerm.keySet())
            {
                Map<PrimitiveResourceClass, Boolean> reducedTerm = new HashMap<>(originalTerm);
                reducedTerm.remove(c);

                if(isSubclassOf(Set.of(reducedTerm), terms))
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
    private static Set<PrimitiveResourceClass> reduceSuperclasses(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(c -> !r.equals(c) && c.isSubclassOf(r)))
                .collect(toSet());
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
     * Result classes of a normal form: the union over its terms.
     *
     * @param terms the normal form
     * @return result classes of a normal form: the union over its terms
     */
    private static Set<ResultResourceClass> generateResultClasses(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        return terms.stream().flatMap(t -> generateResultClasses(t).stream()).collect(toSet());
    }


    /**
     * Result classes common to all positive classes of the term.
     *
     * @param term the term
     * @return result classes common to all positive classes of the term
     */
    private static Set<ResultResourceClass> generateResultClasses(Map<PrimitiveResourceClass, Boolean> term)
    {
        Set<ResultResourceClass> result = new HashSet<>(BuiltinClasses.resultClasses);

        for(Entry<PrimitiveResourceClass, Boolean> lit : term.entrySet())
            if(lit.getValue())
                result.retainAll(lit.getKey().getResultResourceClasses());

        return result;
    }


    /**
     * Most specific common superclass of the positive classes of every term; the box when there is none.
     *
     * @param terms the normal form
     * @return most specific common superclass of the positive classes of every term; the box when there is none
     */
    private static PrimitiveResourceClass selectEfectiveClass(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        Set<PrimitiveResourceClass> candidates = reduceSuperclasses(getEfectiveClassCandidates(terms));

        if(candidates.size() == 0)
            return null;

        if(candidates.size() > 1)
            System.err.print("selected class is not unique: " + candidates); //FIXME: select the best candidate

        return candidates.iterator().next();
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


    @Override
    public boolean match(Statement statement, RdfTerm term)
    {
        return terms.stream()
                .anyMatch(t -> t.entrySet().stream().allMatch(e -> e.getKey().match(statement, term) == e.getValue()));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return resultClasses;
    }


    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
    {
        return effectiveClass.toColumns(statement, term);
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        if(!effectiveClass.isSubclassOf(superClass.getEffectiveClass()))
            return superClass.getEffectiveClass().fromGeneralClass(effectiveClass, columns, true);

        return effectiveClass.toGeneralClass(superClass.getEffectiveClass(), columns, canBeNull);
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
    {
        if(superClass.equals(this))
            return columns;

        throw new IllegalArgumentException();
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
