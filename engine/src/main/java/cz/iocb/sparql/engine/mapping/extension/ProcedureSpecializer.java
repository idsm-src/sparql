package cz.iocb.sparql.engine.mapping.extension;

import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Specialises the calls of a procedure to the classes a query can use its results in (see
 * {@link ProcedureDefinition#setSpecializer}): a query joining a result with a pattern of one class, or filtering it by
 * a constant, accepts a value of that class only, and the function need not compute the values of the other classes.
 * How the function is told is up to the specialisation: another SQL function, or other arguments.
 */
@FunctionalInterface
public interface ProcedureSpecializer
{
    /**
     * Specialisation of a call for the given classes.
     *
     * @param classes for every result the call delivers, the classes of the result the query can use a value of (a
     *            subset of the classes of its definition, never empty)
     * @return the specialisation, or null to call the procedure as declared
     */
    ProcedureSpecialization specialize(Map<ResultDefinition, Set<ResourceClass>> classes);
}
