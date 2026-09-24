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
         * Creates the scope; leading {@code ?} or {@code $} are stripped from the transient names.
         *
         * @param name the scope name
         * @param transientNames variable names shared with the enclosing scopes
         */
        public Scope(String name, Set<String> transientNames)
        {
            this.name = name;

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
        scopes.add(new Scope("", new HashSet<>()));
    }


    /**
     * Opens a nested scope (for MINUS and EXISTS): variables already bound in enclosing scopes stay visible, variables
     * first bound inside are local to it.
     */
    public void addScope()
    {
        scopes.push(new Scope("ctx" + id++, null));
    }


    /**
     * Opens a nested scope for a sub-select: only the projected variables {@code transientNames} are shared with the
     * enclosing scopes, all other variables are local to it.
     *
     * @param transientNames variable names shared with the enclosing scopes
     */
    public void addScope(Set<String> transientNames)
    {
        scopes.push(new Scope("ctx" + id++, transientNames));
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
     * variables in expressions).
     *
     * @param name the variable name
     * @param asPrivate whether an unbound variable gets a private scope
     * @return name of the scope the variable is bound in
     */
    public String addToScope(String name, boolean asPrivate)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        for(Scope scope : scopes)
        {
            if(scope.variables.contains(name))
                return scope.name;

            if(scope.transientNames != null && !scope.transientNames.contains(name))
                break;
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
