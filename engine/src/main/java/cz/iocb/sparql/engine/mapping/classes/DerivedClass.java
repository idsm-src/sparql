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
import cz.iocb.sparql.engine.rdf.RdfTerm;



public class DerivedClass extends ResourceClass
{
    private final Set<Map<PrimitiveResourceClass, Boolean>> terms;
    private final PrimitiveResourceClass effectiveClass;
    private final Set<ResultResourceClass> resultClasses;


    private DerivedClass(Set<Map<PrimitiveResourceClass, Boolean>> terms, PrimitiveResourceClass effectiveClass)
    {
        super(generateName(terms, effectiveClass));

        this.terms = terms;
        this.effectiveClass = effectiveClass;
        this.resultClasses = generateResultClasses(terms);
    }


    private DerivedClass(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        this(terms, selectEfectiveClass(terms));
    }


    public static ResourceClass unionize(Set<ResourceClass> classes, PrimitiveResourceClass effectiveClass)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(union(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(effectiveClass.equals(single))
            return single;

        return new DerivedClass(internal, effectiveClass);
    }


    public static ResourceClass unionize(Set<ResourceClass> classes)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(union(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    public static ResourceClass unionize(ResourceClass... classes)
    {
        return unionize(new HashSet<>(Arrays.asList(classes)));
    }


    public static ResourceClass intersect(Set<ResourceClass> classes)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(intersection(getTerms(classes)));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    public static ResourceClass intersect(ResourceClass... classes)
    {
        return intersect(new HashSet<>(Arrays.asList(classes)));
    }


    public static ResourceClass subtract(ResourceClass a, ResourceClass b)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> internal = normalize(
                intersection(getTerms(a), complement(getTerms(b))));

        PrimitiveResourceClass single = extract(internal);

        if(single != null)
            return single;

        return new DerivedClass(internal);
    }


    public static boolean areDisjunct(ResourceClass a, ResourceClass b)
    {
        return isEmptyIntersection(getTerms(a), getTerms(b));
    }


    public static boolean isSubclassOf(ResourceClass a, ResourceClass b)
    {
        return isSubclassOf(getTerms(a), getTerms(b));
    }


    public static Set<PrimitiveResourceClass> estimateAsUnion(Set<ResourceClass> classes)
    {
        return reduceSubclasses(classes.stream().flatMap(c -> estimateAsUnion(c).stream()).collect(toSet()));
    }


    public static Set<PrimitiveResourceClass> estimateAsUnion(ResourceClass resClass)
    {
        if(resClass instanceof PrimitiveResourceClass primitive)
            return Set.of(primitive);

        return reduceSubclasses(getTerms(resClass).stream()
                .flatMap(t -> t.entrySet().stream().filter(e -> e.getValue()).map(e -> e.getKey())).collect(toSet()));
    }


    private static Set<Map<PrimitiveResourceClass, Boolean>> union(
            Set<Set<Map<PrimitiveResourceClass, Boolean>>> branches)
    {
        return branches.stream().flatMap(s -> s.stream()).collect(toSet());
    }


    private static Set<Map<PrimitiveResourceClass, Boolean>> intersection(
            Set<Set<Map<PrimitiveResourceClass, Boolean>>> branches)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = Set.of(Map.of());

        for(Set<Map<PrimitiveResourceClass, Boolean>> branch : branches)
            result = intersection(result, branch);

        return result;
    }


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


    private static boolean isEmptyIntersection(Set<Map<PrimitiveResourceClass, Boolean>> a,
            Set<Map<PrimitiveResourceClass, Boolean>> b)
    {
        for(Map<PrimitiveResourceClass, Boolean> aTerm : a)
            for(Map<PrimitiveResourceClass, Boolean> bTerm : b)
                if(!isEmpty(aTerm, bTerm))
                    return false;

        return true;
    }


    private static boolean isEmpty(Map<PrimitiveResourceClass, Boolean> a, Map<PrimitiveResourceClass, Boolean> b)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : a.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : b.entrySet())
                if(isEmpty(e1, e2))
                    return true;

        return false;
    }


    private static boolean isEmpty(Map<PrimitiveResourceClass, Boolean> term)
    {
        //NOTE: assume that the 2-helly property holds

        for(Entry<PrimitiveResourceClass, Boolean> e1 : term.entrySet())
            for(Entry<PrimitiveResourceClass, Boolean> e2 : term.entrySet())
                if(isEmpty(e1, e2))
                    return true;

        return false;
    }


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


    private static Set<Map<PrimitiveResourceClass, Boolean>> prepareTerms(
            Set<Map<PrimitiveResourceClass, Boolean>> input)
    {
        Set<Map<PrimitiveResourceClass, Boolean>> result = new HashSet<>();

        for(Map<PrimitiveResourceClass, Boolean> term : input)
            if(!isEmpty(term))
                result.add(term);

        return result;
    }


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


    private static Set<PrimitiveResourceClass> reduceSubclasses(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(c -> !r.equals(c) && r.isSubclassOf(c)))
                .collect(toSet());
    }


    private static Set<PrimitiveResourceClass> reduceSuperclasses(Set<PrimitiveResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(c -> !r.equals(c) && c.isSubclassOf(r)))
                .collect(toSet());
    }


    private static Set<Set<Map<PrimitiveResourceClass, Boolean>>> getTerms(Set<ResourceClass> classes)
    {
        return classes.stream().map(c -> getTerms(c)).collect(toSet());
    }


    private static Set<Map<PrimitiveResourceClass, Boolean>> getTerms(ResourceClass resClass)
    {
        if(resClass instanceof DerivedClass compositeClass)
            return compositeClass.terms;
        else if(resClass instanceof PrimitiveResourceClass primitiveClass)
            return Set.of(Map.of(primitiveClass, true));
        else
            throw new IllegalArgumentException();
    }


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


    private static String generateName(Set<Map<PrimitiveResourceClass, Boolean>> terms,
            PrimitiveResourceClass effectiveClass)
    {
        if(effectiveClass == null)
            return "null@()";

        return effectiveClass.getResourceName() + "@" + generateName(terms);
    }


    private static String generateName(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        return terms.stream().map(t -> generateName(t)).sorted().collect(joining("|"));
    }


    private static String generateName(Map<PrimitiveResourceClass, Boolean> term)
    {
        return term.entrySet().stream().map(e -> generateName(e)).sorted().collect(joining("&"));
    }


    private static String generateName(Entry<PrimitiveResourceClass, Boolean> e)
    {
        return (e.getValue() ? "" : "!") + e.getKey().getResourceName();
    }


    private static Set<ResultResourceClass> generateResultClasses(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        return terms.stream().flatMap(t -> generateResultClasses(t).stream()).collect(toSet());
    }


    private static Set<ResultResourceClass> generateResultClasses(Map<PrimitiveResourceClass, Boolean> term)
    {
        Set<ResultResourceClass> result = new HashSet<>(BuiltinClasses.resultClasses);

        for(Entry<PrimitiveResourceClass, Boolean> lit : term.entrySet())
            if(lit.getValue())
                result.retainAll(lit.getKey().getResultResourceClasses());

        return result;
    }


    private static PrimitiveResourceClass selectEfectiveClass(Set<Map<PrimitiveResourceClass, Boolean>> terms)
    {
        Set<PrimitiveResourceClass> candidates = reduceSuperclasses(getEfectiveClassCandidates(terms));

        if(candidates.size() == 0)
            return null;

        if(candidates.size() > 1)
            System.err.print("selected class is not unique: " + candidates); //FIXME: select the best candidate

        return candidates.iterator().next();
    }


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
    public List<String> getSqlTypes()
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
