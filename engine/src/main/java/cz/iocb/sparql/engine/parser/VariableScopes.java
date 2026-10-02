package cz.iocb.sparql.engine.parser;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;



/**
 * Stack of variable scopes used while parsing. It decides which occurrences of a variable name denote the same
 * variable: e.g. a non-projected variable of a sub-select, or a variable introduced inside MINUS or EXISTS, is distinct
 * from a same-named variable outside. The resolved scope name becomes the scope of the
 * {@link cz.iocb.sparql.engine.model.VariableNode}.
 */
public class VariableScopes
{
    /**
     * One scope of the stack.
     */
    private static class Scope
    {
        /**
         * Name of the scope, becoming the scope of its variables.
         */
        String name;

        /**
         * Variable names shared with the enclosing scopes; null when all are shared.
         */
        Set<String> transientNames;

        /**
         * Variables bound in this scope.
         */
        Set<String> variables = new HashSet<>();

        /**
         * Whether the scope is the scope of an EXISTS pattern, which is evaluated for the current solution of the
         * enclosing scopes.
         */
        boolean exists;

        /**
         * Creates the scope; leading {@code ?} or {@code $} are stripped from the transient names.
         *
         * @param name the scope name
         * @param transientNames variable names shared with the enclosing scopes
         * @param exists whether the scope is the scope of an EXISTS pattern
         */
        public Scope(String name, Set<String> transientNames, boolean exists)
        {
            this.name = name;
            this.exists = exists;

            if(transientNames != null)
            {
                this.transientNames = new HashSet<>();

                for(String n : transientNames)
                {
                    if(n.startsWith("$") || n.startsWith("?"))
                        n = n.substring(1);

                    this.transientNames.add(n);

                }
            }
        }
    }


    /**
     * Counter of created scope names.
     */
    private int id = 0;

    /**
     * Scope stack, innermost first.
     */
    private LinkedList<Scope> scopes = new LinkedList<>();


    /**
     * Creates the stack with the top-level scope, whose variables have an empty scope name.
     */
    public VariableScopes()
    {
        scopes.add(new Scope("", new HashSet<>(), false));
    }


    /**
     * Opens a nested scope (for MINUS): variables already bound in enclosing scopes stay visible, variables first bound
     * inside are local to it.
     */
    public void addScope()
    {
        scopes.push(new Scope("ctx" + id++, null, false));
    }


    /**
     * Opens the nested scope of an EXISTS pattern: like {@link #addScope()}, but the pattern is evaluated for the
     * current solution of the enclosing scopes, so the variables bound in them stay visible even inside a sub-select
     * nested in the pattern that does not project them (see {@link #addToScope(String, boolean)}).
     */
    public void addExistsScope()
    {
        scopes.push(new Scope("ctx" + id++, null, true));
    }


    /**
     * Opens a nested scope for a sub-select: only the projected variables {@code transientNames} are shared with the
     * enclosing scopes, all other variables are local to it.
     *
     * @param transientNames variable names shared with the enclosing scopes
     */
    public void addScope(Set<String> transientNames)
    {
        scopes.push(new Scope("ctx" + id++, transientNames, false));
    }


    /**
     * Closes the innermost scope.
     */
    public void popScope()
    {
        scopes.pop();
    }


    /**
     * Resolves the variable {@code name} (leading {@code ?} or {@code $} is ignored) to the name of the scope it is
     * bound in, binding it in the innermost scope if it is not bound yet. With {@code asPrivate}, an unbound variable
     * is instead given a fresh unique scope without being bound, so it stays unbound for later occurrences (used for
     * variables in expressions). A variable hidden by the projection of a sub-select is still a variable of the current
     * solution of an EXISTS pattern the sub-select is nested in, which binds the variables of the scopes enclosing the
     * pattern: those scopes are searched as well.
     *
     * @param name the variable name
     * @param asPrivate whether an unbound variable gets a private scope
     * @return name of the scope the variable is bound in
     */
    public String addToScope(String name, boolean asPrivate)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        for(int i = 0; i < scopes.size(); i++)
        {
            Scope scope = scopes.get(i);

            if(scope.variables.contains(name))
                return scope.name;

            if(scope.transientNames != null && !scope.transientNames.contains(name))
            {
                //NOTE: continue outside the EXISTS pattern the sub-select is nested in, if there is any
                while(i < scopes.size() && !scopes.get(i).exists)
                    i++;
            }
        }

        if(asPrivate)
            return "ctx" + id++;

        scopes.peek().variables.add(name);
        return scopes.peek().name;
    }


    /**
     * Same as {@link #addToScope(String, boolean)} with {@code asPrivate} false.
     *
     * @param name the variable name
     * @return name of the scope the variable is bound in
     */
    public String addToScope(String name)
    {
        return addToScope(name, false);
    }
}
