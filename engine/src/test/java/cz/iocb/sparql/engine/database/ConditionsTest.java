package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;



/**
 * Disjunction and conjunction of {@link Conditions}: false and true are their neutral elements and true and false their
 * absorbing ones, also for any number of operands.
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
}
