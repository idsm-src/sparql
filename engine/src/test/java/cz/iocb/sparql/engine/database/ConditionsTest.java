package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;



/**
 * Disjunction and conjunction of {@link Conditions}: false and true are their neutral elements and true and false their
 * absorbing ones, also for any number of operands; the sufficient test of an implication between disjunctions; the
 * columns equal to a column in every disjunct; and the resolution of disjuncts on complementary null tests. A disjunct
 * is compared with every other one, so {@link Condition#contains} looks at their columns before it conjoins them, which
 * must not change what is absorbed and implied: random conditions and disjunctions are compared also by the
 * conjunctions alone.
 */
public class ConditionsTest
{
    private static final Column a = new TableColumn("a", INT4);

    private static final Column b = new TableColumn("b", INT4);

    private static final Column c = new TableColumn("c", INT4);

    /**
     * The columns of the random conditions: the table columns, two constants and the NULL constant.
     */
    private static final List<Column> columns = List.of(a, b, c, new ValueColumn("1", INT4), new ValueColumn("2", INT4),
            new NullColumn(INT4));


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
        int absorbed = 0;
        int implied = 0;

        Conditions previous = new Conditions(false);
        Set<Condition> expectedPrevious = new HashSet<>();

        for(int i = 0; i < 50000; i++)
        {
            Conditions conditions = new Conditions(false);
            Set<Condition> expected = new HashSet<>();
            int added = 1 + random.nextInt(8);

            for(int j = 0; j < added; j++)
            {
                Condition condition = randomCondition(random);

                conditions.add(condition);
                add(expected, condition);

                assertEquals(expected, conditions.getConditions());
            }

            Set<Condition> implying = expectedPrevious;
            boolean implies = implying.stream().allMatch(l -> expected.stream().anyMatch(r -> contains(l, r)));

            assertEquals(implies, previous.implies(conditions));

            if(expected.size() < added)
                absorbed++;

            if(implies)
                implied++;

            previous = conditions;
            expectedPrevious = expected;
        }

        // the test is no good unless many of the disjunctions absorb or resolve a disjunct and imply the next one
        assertTrue(absorbed > 10000);
        assertTrue(implied > 1000);
    }
}
