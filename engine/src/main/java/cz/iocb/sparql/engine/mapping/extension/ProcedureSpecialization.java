package cz.iocb.sparql.engine.mapping.extension;

import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Specialisation of a procedure call to the classes a query can use its results in (see {@link ProcedureSpecializer}):
 * the SQL function to call, its arguments, and the classes the function binds the results in. Every method has a
 * default standing for the call as declared, so that an implementation overrides what it changes only.
 */
public interface ProcedureSpecialization
{
    /**
     * SQL function to call instead of the declared one.
     *
     * @return SQL function to call, or null for the declared one
     */
    default Function getSqlProcedure()
    {
        return null;
    }


    /**
     * Arguments of the call, given the arguments of the declared parameters as rendered for the current solutions: one
     * SQL expression per column of a parameter, in declaration order. The default passes them as they are; an
     * implementation may add arguments or replace some.
     *
     * @param declared arguments of the declared parameters, one SQL expression per column of a parameter
     * @return arguments of the call
     */
    default List<String> getArguments(List<String> declared)
    {
        return declared;
    }


    /**
     * Classes the function binds the result in, when it leaves out some of those the query can use; the engine binds
     * the result to these classes only.
     *
     * @param result the result
     * @return classes the function binds the result in, or null when it binds it in every class the query can use
     */
    default Set<ResourceClass> getResultClasses(ResultDefinition result)
    {
        return null;
    }
}
