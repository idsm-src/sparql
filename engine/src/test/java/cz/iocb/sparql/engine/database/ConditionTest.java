package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;



/**
 * Normalisation of {@link Condition}: transitive closure of the equalities, redundancy of the not-null predicates,
 * promotion of the null-safe equalities to the strict ones, and contradiction detection.
 */
public class ConditionTest
{
    private static final Column a = new TableColumn("a");

    private static final Column b = new TableColumn("b");

    private static final Column c = new TableColumn("c");

    private static final Column one = new ConstantColumn("1", INT4);

    private static final Column two = new ConstantColumn("2", INT4);

    private static final Column nul = new ConstantColumn(null, INT4);


    private static ColumnComparison pair(Column l, Column r)
    {
        return new ColumnComparison(l, r);
    }


    @Test
    void emptyConditionIsTrue()
    {
        Condition condition = new Condition();

        assertTrue(condition.isTrue());
        assertFalse(condition.isFalse());
    }


    @Test
    void strictEqualityIsClosedAndDropsNotNull()
    {
        Condition condition = new Condition();
        condition.addIsNotNull(a);
        condition.addIsNotNull(c);
        condition.addAreEqual(a, b);
        condition.addAreEqual(b, c);

        assertEquals(Set.of(pair(a, b), pair(b, c), pair(a, c)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getAreNotDistinct());
        assertEquals(Set.of(), condition.getIsNotNull());
        assertFalse(condition.isFalse());
    }


    @Test
    void selfEqualityKeepsNotNull()
    {
        Condition condition = new Condition();
        condition.addIsNotNull(a);
        condition.addAreEqual(List.of(a, b), List.of(a, b));

        assertEquals(Set.of(a), condition.getIsNotNull());
        assertEquals(Set.of(), condition.getAreEqual());
    }


    @Test
    void nullSafeEqualityStaysNullSafeWithoutNotNullKnowledge()
    {
        Condition condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addAreNotDistinct(b, c);

        assertEquals(Set.of(), condition.getAreEqual());
        assertEquals(Set.of(pair(a, b), pair(b, c), pair(a, c)), condition.getAreNotDistinct());
        assertEquals(Set.of(a, b, c), condition.getEqualColumns(a));
        assertEquals(Set.of(a, b, c), condition.getEqualTableColumns(a));
    }


    @Test
    void notNullPromotesNullSafeEqualities()
    {
        Condition condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addIsNotNull(b);

        assertEquals(Set.of(pair(a, b)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getAreNotDistinct());
        assertEquals(Set.of(), condition.getIsNotNull());
    }


    @Test
    void knownNotNullMakesNullSafeEqualityStrict()
    {
        Condition condition = new Condition();
        condition.addIsNotNull(a);
        condition.addAreNotDistinct(a, b);

        assertEquals(Set.of(pair(a, b)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getIsNotNull());


        condition = new Condition();
        condition.addAreEqual(a, b);
        condition.addAreNotDistinct(b, c);

        assertEquals(Set.of(pair(a, b), pair(b, c), pair(a, c)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getAreNotDistinct());


        condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addAreEqual(b, c);

        assertEquals(Set.of(pair(a, b), pair(b, c), pair(a, c)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getAreNotDistinct());


        condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addAreNotEqual(b, c);

        assertEquals(Set.of(pair(a, b)), condition.getAreEqual());
        assertEquals(Set.of(pair(b, c)), condition.getAreNotEqual());
    }


    @Test
    void nonNullConstantMakesNullSafeEqualityStrict()
    {
        Condition condition = new Condition();
        condition.addAreNotDistinct(a, one);

        assertEquals(Set.of(pair(a, one)), condition.getAreEqual());
        assertEquals(Set.of(), condition.getAreNotDistinct());
    }


    @Test
    void nullConstantRequiresNull()
    {
        Condition condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addAreNotDistinct(b, nul);

        assertEquals(Set.of(a, b), condition.getIsNull());
        assertFalse(condition.isFalse());

        condition.addIsNotNull(a);

        assertTrue(condition.isFalse());
    }


    @Test
    void strictEqualityWithNullConstantIsFalse()
    {
        Condition condition = new Condition();
        condition.addAreEqual(a, nul);

        assertTrue(condition.isFalse());
    }


    @Test
    void nullConstantIsNullIsTrueAndOtherConstantIsFalse()
    {
        Condition condition = new Condition();
        condition.addIsNull(nul);

        assertTrue(condition.isTrue());

        condition.addIsNull(one);

        assertTrue(condition.isFalse());
    }


    @Test
    void contradictions()
    {
        Condition condition = new Condition();
        condition.addIsNull(a);
        condition.addIsNotNull(a);
        assertTrue(condition.isFalse());

        condition = new Condition();
        condition.addIsNull(a);
        condition.addAreEqual(a, b);
        assertTrue(condition.isFalse());

        condition = new Condition();
        condition.addAreEqual(a, one);
        condition.addAreEqual(a, two);
        assertTrue(condition.isFalse());

        condition = new Condition();
        condition.addAreEqual(a, b);
        condition.addAreNotEqual(a, b);
        assertTrue(condition.isFalse());

        condition = new Condition();
        condition.addAreNotDistinct(a, b);
        condition.addAreNotEqual(b, a);
        assertTrue(condition.isFalse());
    }


    @Test
    void conjunctionPropagatesKnowledge()
    {
        Condition left = new Condition();
        left.addAreNotDistinct(a, b);

        Condition right = new Condition();
        right.addIsNotNull(b);

        Condition result = Condition.and(left, right);

        assertEquals(Set.of(pair(a, b)), result.getAreEqual());
        assertEquals(Set.of(), result.getAreNotDistinct());
        assertEquals(Set.of(), result.getIsNotNull());
        assertEquals(result, Condition.and(right, left));
    }


    @Test
    void roleAwareEqualityUsesBothKinds()
    {
        Condition condition = new Condition();
        condition.addAreEqual(List.of(a, b), List.of(c, nul), i -> i == 1);

        assertEquals(Set.of(pair(a, c)), condition.getAreEqual());
        assertEquals(Set.of(b), condition.getIsNull());
        assertFalse(condition.isFalse());
    }
}
