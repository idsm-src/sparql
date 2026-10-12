package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;



/**
 * Disjunction and conjunction of {@link Conditions}: false and true are their neutral elements and true and false their
 * absorbing ones, also for any number of operands; the sufficient test of an implication between disjunctions; the
 * columns equal to a column in every disjunct; and the resolution of disjuncts on complementary null tests.
 */
public class ConditionsTest
{
    private static final Column a = new TableColumn("a", INT4);

    private static final Column b = new TableColumn("b", INT4);

    private static final Column c = new TableColumn("c", INT4);


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
}
