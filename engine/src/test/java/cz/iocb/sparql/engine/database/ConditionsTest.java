package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;



/**
 * Disjunction and conjunction of {@link Conditions}: false and true are their neutral elements and true and false their
 * absorbing ones, also for any number of operands; the sufficient test of an implication between disjunctions; the
 * columns equal to a column in every disjunct; and the resolution of disjuncts on complementary null tests. A
 * contradictory disjunct is not kept, so the conjunction of two disjunctions has no disjunct for two disjuncts that
 * contradict each other, and a conjunction can be asked not to take more conjunctions of two disjuncts than a limit. A
 * disjunct is compared with every other one, so {@link Condition#contains} looks at their columns before it conjoins
 * them, which must not change what is absorbed and implied: random conditions and disjunctions are compared also by the
 * conjunctions alone.
 */
public class ConditionsTest
{
    private static final Column a = new TableColumn("a", INT4);

    private static final Column b = new TableColumn("b", INT4);

    private static final Column c = new TableColumn("c", INT4);

    private static final Column one = new ValueColumn("1", INT4);

    private static final Column two = new ValueColumn("2", INT4);

    private static final Column three = new ValueColumn("3", INT4);

    /**
     * The columns of the random conditions: the table columns, two constants and the NULL constant.
     */
    private static final List<Column> columns = List.of(a, b, c, one, two, new NullColumn(INT4));


    /**
     * A condition of up to three random predicates over the columns, of which there are few enough for one condition to
     * contain all predicates of another one often.
     *
     * @param random the source of randomness
     * @return the condition
     */
    private static Condition randomCondition(Random random)
    {
        Condition condition = new Condition();

        for(int i = random.nextInt(4); i > 0; i--)
        {
            Column first = columns.get(random.nextInt(columns.size()));
            Column second = columns.get(random.nextInt(columns.size()));

            switch(random.nextInt(5))
            {
                case 0 -> condition.addIsNull(first);
                case 1 -> condition.addIsNotNull(first);
                case 2 -> condition.addAreEqual(first, second);
                case 3 -> condition.addAreNotDistinct(first, second);
                default -> condition.addAreNotEqual(first, second);
            }
        }

        return condition;
    }


    /**
     * True if conjoining the second condition changes nothing in the first one, which is what
     * {@link Condition#contains} has to tell.
     *
     * @param first the first condition
     * @param second the second condition
     * @return true if the first condition contains all predicates of the second one, false otherwise
     */
    private static boolean contains(Condition first, Condition second)
    {
        return Condition.and(first, second).equals(first);
    }


    /**
     * Adds the disjunct as {@link Conditions#add(Condition)} does, comparing the disjuncts by their conjunctions alone.
     *
     * @param conditions the disjuncts
     * @param condition the disjunct to add
     */
    private static void add(Set<Condition> conditions, Condition condition)
    {
        if(condition.isFalse())
            return;

        for(Condition other : conditions)
            if(contains(condition, other))
                return;

        conditions.removeIf(other -> contains(other, condition));

        Condition resolvent = null;

        for(Condition other : conditions)
            if((resolvent = condition.resolve(other)) != null)
            {
                conditions.remove(other);
                break;
            }

        if(resolvent != null)
            add(conditions, resolvent);
        else
            conditions.add(condition);
    }


    private static Conditions notEqual(Column l, Column r)
    {
        Condition condition = new Condition();
        condition.addAreNotEqual(l, r);

        return new Conditions(condition);
    }


    private static Conditions equal(Column l, Column r)
    {
        Condition condition = new Condition();
        condition.addAreEqual(l, r);

        return new Conditions(condition);
    }


    private static Conditions isNull(Column c)
    {
        Condition condition = new Condition();
        condition.addIsNull(c);

        return new Conditions(condition);
    }


    private static Conditions isNotNull(Column c)
    {
        Condition condition = new Condition();
        condition.addIsNotNull(c);

        return new Conditions(condition);
    }


    @Test
    void disjunctionOfNoOperandIsFalse()
    {
        Conditions result = Conditions.or();

        assertTrue(result.isFalse());
        assertFalse(result.isTrue());
    }


    @Test
    void disjunctionKeepsEveryOperand()
    {
        Conditions ab = notEqual(a, b);
        Conditions bc = notEqual(b, c);
        Conditions ac = notEqual(a, c);

        assertEquals(ab, Conditions.or(ab));
        assertEquals(Conditions.or(Conditions.or(ab, bc), ac), Conditions.or(ab, bc, ac));
        assertEquals(3, Conditions.or(ab, bc, ac).getConditions().size());
        assertFalse(Conditions.or(ab, bc, ac).isTrue());
    }


    @Test
    void disjunctionWithTrueIsTrue()
    {
        assertTrue(Conditions.or(notEqual(a, b), new Conditions(true), notEqual(b, c)).isTrue());
    }


    @Test
    void conjunctionOfNoOperandIsTrue()
    {
        Conditions result = Conditions.and();

        assertTrue(result.isTrue());
        assertFalse(result.isFalse());
    }


    @Test
    void conjunctionWithFalseIsFalse()
    {
        assertTrue(Conditions.and(notEqual(a, b), new Conditions(false), notEqual(b, c)).isFalse());
    }


    @Test
    void implication()
    {
        Conditions ab = notEqual(a, b);
        Conditions ac = notEqual(a, c);
        Conditions abAndBc = Conditions.and(ab, notEqual(b, c));
        Conditions abOrAc = Conditions.or(ab, ac);

        assertTrue(ab.implies(ab));
        assertTrue(abAndBc.implies(ab));
        assertFalse(ab.implies(abAndBc));
        assertTrue(ab.implies(abOrAc));
        assertFalse(abOrAc.implies(ab));
        assertTrue(abOrAc.implies(abOrAc));

        assertTrue(new Conditions(false).implies(ab));
        assertTrue(ab.implies(new Conditions(true)));
        assertFalse(new Conditions(true).implies(ab));
        assertFalse(ab.implies(new Conditions(false)));
    }


    @Test
    void equalColumnsInEveryDisjunct()
    {
        Conditions ab = equal(a, b);
        Conditions ac = equal(a, c);
        Conditions abAndAc = Conditions.and(ab, ac);
        Conditions abOrAc = Conditions.or(ab, ac);

        assertEquals(Set.of(a, b), ab.getEqualTableColumns(a));
        assertEquals(Set.of(a, b, c), abAndAc.getEqualTableColumns(a));
        assertEquals(Set.of(a), abOrAc.getEqualTableColumns(a));
        assertEquals(Set.of(a, b), Conditions.or(ab, abAndAc).getEqualTableColumns(a));

        assertEquals(Set.of(a), new Conditions(true).getEqualTableColumns(a));
        assertEquals(Set.of(), new Conditions(false).getEqualTableColumns(a));
    }


    @Test
    void complementaryNullTests()
    {
        Conditions bc = notEqual(b, c);

        assertTrue(Conditions.or(isNull(a), isNotNull(a)).isTrue());
        assertEquals(bc, Conditions.or(Conditions.and(isNull(a), bc), Conditions.and(isNotNull(a), bc)));
        assertEquals(bc, Conditions.or(Conditions.and(isNotNull(a), bc), Conditions.and(isNull(a), bc)));

        assertEquals(2, Conditions.or(isNull(a), isNotNull(b)).getConditions().size());
        assertEquals(2, Conditions.or(Conditions.and(isNull(a), bc), isNotNull(a)).getConditions().size());
        assertEquals(2, Conditions.or(Conditions.and(isNull(a), isNull(b)), isNotNull(a)).getConditions().size());

        Conditions nested = Conditions.or(Conditions.and(isNull(a), isNull(b)), Conditions.and(isNull(a), isNotNull(b)),
                isNotNull(a));

        assertTrue(nested.isTrue());
        assertEquals(1, nested.getConditions().size());

        Conditions copy = new Conditions(Conditions.and(isNull(a), bc));
        copy.add(Conditions.and(isNotNull(a), bc));

        assertEquals(bc, copy);
    }


    @Test
    void absorption()
    {
        Conditions ab = Conditions.and(isNotNull(a), isNotNull(b));
        Conditions ac = Conditions.and(isNotNull(a), isNotNull(c));
        Conditions aNotB = Conditions.and(isNotNull(a), isNull(b));

        assertEquals(isNotNull(a), Conditions.or(isNotNull(a), ab));
        assertEquals(isNotNull(a), Conditions.or(ab, isNotNull(a)));
        assertEquals(isNotNull(a), Conditions.or(ab, ac, isNotNull(a), Conditions.and(ab, isNull(c))));
        assertEquals(2, Conditions.or(ab, isNotNull(c)).getConditions().size());
        assertEquals(2, Conditions.or(aNotB, ac).getConditions().size());

        // the resolvent of two disjuncts absorbs the others in both orders
        assertEquals(isNotNull(a), Conditions.or(aNotB, ab, ac));
        assertEquals(isNotNull(a), Conditions.or(ac, aNotB, ab));

        Conditions copy = new Conditions(ab);
        copy.add(new Condition());

        assertTrue(copy.isTrue());
        assertEquals(1, copy.getConditions().size());
    }


    @Test
    void containmentOfRandomConditions()
    {
        Random random = new Random(1);
        int contained = 0;

        for(int i = 0; i < 200000; i++)
        {
            Condition first = randomCondition(random);
            Condition second = randomCondition(random);

            assertEquals(contains(first, second), first.contains(second));

            if(first.contains(second))
                contained++;
        }

        // the test is no good unless many of the conditions contain the other one
        assertTrue(contained > 10000);
    }


    @Test
    void randomDisjunctions()
    {
        Random random = new Random(2);
        int contradictory = 0;
        int absorbed = 0;
        int implied = 0;

        Conditions previous = new Conditions(false);
        Set<Condition> expectedPrevious = new HashSet<>();

        for(int i = 0; i < 50000; i++)
        {
            Conditions conditions = new Conditions(false);
            Set<Condition> expected = new HashSet<>();
            int consistent = 0;

            for(int j = random.nextInt(8); j >= 0; j--)
            {
                Condition condition = randomCondition(random);

                conditions.add(condition);
                add(expected, condition);

                assertEquals(expected, conditions.getConditions());
                assertEquals(expected.isEmpty(), conditions.isFalse());

                if(condition.isFalse())
                    contradictory++;
                else
                    consistent++;
            }

            Set<Condition> implying = expectedPrevious;
            boolean implies = implying.stream().allMatch(l -> expected.stream().anyMatch(r -> contains(l, r)));

            assertEquals(implies, previous.implies(conditions));

            if(expected.size() < consistent)
                absorbed++;

            if(implies)
                implied++;

            previous = conditions;
            expectedPrevious = expected;
        }

        // the test is no good unless many of the disjuncts are contradictory and many of the disjunctions absorb or
        // resolve a disjunct and imply the next one
        assertTrue(contradictory > 10000);
        assertTrue(absorbed > 10000);
        assertTrue(implied > 1000);
    }


    @Test
    void contradictoryDisjuncts()
    {
        Condition contradiction = new Condition();
        contradiction.addIsNull(a);
        contradiction.addIsNotNull(a);

        assertTrue(new Conditions(contradiction).isFalse());
        assertEquals(new Conditions(false), new Conditions(contradiction));
        assertEquals(equal(a, one), Conditions.or(equal(a, one), new Conditions(contradiction)));

        Conditions copy = new Conditions(equal(a, one));
        copy.add(contradiction);

        assertEquals(equal(a, one), copy);

        // a column equal to a constant is not equal to another one
        assertTrue(Conditions.and(equal(a, one), equal(a, two)).isFalse());
        assertEquals(Set.of(), Conditions.and(equal(a, one), equal(a, two)).getConditions());

        // so the conjunction of two lists of the values of a column is the list of their common values
        Conditions oneOrTwo = Conditions.or(equal(a, one), equal(a, two));
        Conditions twoOrThree = Conditions.or(equal(a, two), equal(a, three));

        assertEquals(equal(a, two), Conditions.and(oneOrTwo, twoOrThree));
        assertEquals(equal(a, two), Conditions.and(oneOrTwo, equal(a, two).getConditions().iterator().next()));
        assertTrue(Conditions.and(oneOrTwo, equal(a, three)).isFalse());

        // whereas the lists of the values of two columns give a disjunct for every pair of the values
        assertEquals(4, Conditions.and(oneOrTwo, Conditions.or(equal(b, two), equal(b, three))).getConditions().size());
    }


    @Test
    void conjunctionWithLimit()
    {
        Conditions oneOrTwo = Conditions.or(equal(a, one), equal(a, two));
        Conditions twoOrThree = Conditions.or(equal(a, two), equal(a, three));
        Conditions others = Conditions.or(equal(b, two), equal(b, three));

        // the disjuncts that contradict each other do not count
        assertEquals(equal(a, two), Conditions.and(oneOrTwo, twoOrThree, 1));
        assertNull(Conditions.and(oneOrTwo, twoOrThree, 0));
        assertEquals(new Conditions(false), Conditions.and(oneOrTwo, equal(a, three), 0));

        // the disjuncts over different columns combine each with each
        assertEquals(Conditions.and(oneOrTwo, others), Conditions.and(oneOrTwo, others, 4));
        assertNull(Conditions.and(oneOrTwo, others, 3));

        // equal conjunctions count once, also when one of them is absorbed in the end
        Conditions notNull = Conditions.or(isNotNull(a), isNotNull(b));

        assertEquals(notNull, Conditions.and(notNull, notNull, 3));
        assertNull(Conditions.and(notNull, notNull, 2));

        assertEquals(oneOrTwo, Conditions.and(oneOrTwo, new Conditions(true), 2));
        assertEquals(new Conditions(false), Conditions.and(oneOrTwo, new Conditions(false), 0));
    }


    @Test
    void randomConjunctionsWithLimit()
    {
        Random random = new Random(3);
        int exceeded = 0;
        int taken = 0;

        for(int i = 0; i < 50000; i++)
        {
            Conditions left = new Conditions(false);
            Conditions right = new Conditions(false);

            for(int j = random.nextInt(6); j >= 0; j--)
                left.add(randomCondition(random));

            for(int j = random.nextInt(6); j >= 0; j--)
                right.add(randomCondition(random));

            Set<Condition> conjunctions = new HashSet<>();

            for(Condition l : left.getConditions())
                for(Condition r : right.getConditions())
                    if(!Condition.and(l, r).isFalse())
                        conjunctions.add(Condition.and(l, r));

            int limit = random.nextInt(6);

            if(conjunctions.size() > limit)
            {
                assertNull(Conditions.and(left, right, limit));
                exceeded++;
            }
            else
            {
                assertEquals(Conditions.and(left, right), Conditions.and(left, right, limit));
                taken++;
            }
        }

        // the test is no good unless the limit is exceeded often and kept often
        assertTrue(exceeded > 5000);
        assertTrue(taken > 5000);
    }
}
