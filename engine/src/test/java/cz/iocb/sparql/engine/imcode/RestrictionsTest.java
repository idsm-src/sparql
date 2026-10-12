package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.ClassRelations.NONE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Adding classes to {@link Restrictions}: the sets of needed classes may be immutable or shared with other
 * restrictions, so adding to a variable must neither fail nor change the other restrictions.
 */
public class RestrictionsTest
{
    private static final Variable x = new Variable("x");


    @Test
    void addToVariableRequiredInAnyClass()
    {
        Restrictions restrictions = new Restrictions(List.of(x));
        restrictions.add(x, Set.of(xsdString));
        restrictions.add(x, xsdInteger);

        assertTrue(restrictions.contains(NONE, x, xsdString));
        assertTrue(restrictions.contains(NONE, x, xsdInteger));
    }


    @Test
    void addToCopyKeepsOriginal()
    {
        Restrictions original = new Restrictions();
        original.add(x, xsdString);

        Restrictions copy = new Restrictions(original);
        copy.add(x, xsdInteger);

        Restrictions union = new Restrictions(original, new Restrictions());
        union.add(x, xsdInteger);

        assertTrue(copy.contains(NONE, x, xsdInteger));
        assertTrue(union.contains(NONE, x, xsdInteger));
        assertFalse(original.contains(NONE, x, xsdInteger));
    }


    @Test
    void addKeepsGivenSet()
    {
        Set<ResourceClass> given = new HashSet<>();
        given.add(xsdString);

        Restrictions restrictions = new Restrictions();
        restrictions.set(x, given);
        restrictions.add(x, xsdInteger);

        assertTrue(restrictions.contains(NONE, x, xsdInteger));
        assertFalse(given.contains(xsdInteger));
    }
}
