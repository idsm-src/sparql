package cz.iocb.sparql.engine.config;

import java.util.Objects;



/**
 * Settings of the calls of a remote SERVICE endpoint: which values of the shared variables are sent in the VALUES
 * clause of a call, whether the received literals are brought to their canonical forms, and the limits of the
 * evaluation. A configuration holds the settings of the endpoints with settings of their own and the defaults of all
 * the others ({@link SparqlDatabaseConfiguration#getServiceSettings(cz.iocb.sparql.engine.rdf.Iri)}). The settings are
 * immutable; the {@code with} methods derive changed copies.
 *
 * @param sendLiterals whether the literal values of the shared variables (and the triple terms containing a literal)
 *            are sent in the VALUES clause of a call; otherwise only the IRIs and the triple terms of IRIs are sent,
 *            and a literal value constrains the received solutions locally, so that the endpoint need not store the
 *            literal in the same lexical form
 * @param canonicalizeLiterals whether the received literals of the datatypes known to the configuration are brought to
 *            their canonical lexical forms (an invalid lexical form is kept)
 * @param contextLimit maximum number of context solutions a call may be evaluated for (the solutions of the pattern the
 *            call is joined with); exceeding it fails the query or makes the call be evaluated independently of its
 *            context, as {@code contextOverflow} says; with the reordering of calls, the context solutions are the
 *            distinct combinations of the shared variables the call is asked about, which keeps only as many of them as
 *            are estimated to fit into the limit
 * @param resultLimit maximum number of solutions one evaluation of a call may collect from the endpoint, counted after
 *            the merge with the context solutions; exceeding it fails the query
 * @param redirectLimit maximum number of HTTP redirects followed by one request to the endpoint
 * @param contextOverflow what a call does when its context has more solutions than {@code contextLimit}
 */
public record ServiceSettings(boolean sendLiterals, boolean canonicalizeLiterals, int contextLimit, int resultLimit,
        int redirectLimit, ContextOverflow contextOverflow)
{
    /**
     * What a call does when its context has more solutions than the context limit.
     */
    public enum ContextOverflow
    {
        /**
         * The query fails.
         */
        FAIL,

        /**
         * The pattern of the call is asked once without the values of the context (once for every endpoint the context
         * names, when a variable names the endpoint, whose distinct values must then fit into the limit), and the
         * received solutions are joined with the context locally. Such a request may be much heavier than the ones with
         * the values, so the endpoint may cut its answer, time out or exceed the result limit, and an endpoint that
         * needs the values, like a procedure taking its parameters from the context, answers otherwise.
         */
        INDEPENDENT
    }


    /**
     * Default settings: only IRIs are sent, received literals are canonicalized, 1000 context solutions, whose overflow
     * fails the query, 10 000 000 results and 3 redirects.
     */
    public static final ServiceSettings DEFAULT = new ServiceSettings(false, true, 1000, 10000000, 3,
            ContextOverflow.FAIL);


    /**
     * Checks the limits: the context and result limits have to be positive, the redirect limit non-negative, and the
     * behaviour on the overflow of the context has to be given.
     *
     * @throws IllegalArgumentException if a limit is out of its range
     * @throws NullPointerException if the behaviour on the overflow of the context is null
     */
    public ServiceSettings
    {
        Objects.requireNonNull(contextOverflow, "the behaviour on the overflow of the context");

        if(contextLimit < 1)
            throw new IllegalArgumentException("the context limit has to be positive");

        if(resultLimit < 1)
            throw new IllegalArgumentException("the result limit has to be positive");

        if(redirectLimit < 0)
            throw new IllegalArgumentException("the redirect limit cannot be negative");
    }


    /**
     * Copy with the given sending of literal values.
     *
     * @param sendLiterals whether the literal values of the shared variables are sent in the VALUES clause of a call
     * @return copy with the given sending of literal values
     */
    public ServiceSettings withSendLiterals(boolean sendLiterals)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }


    /**
     * Copy with the given canonicalization of received literals.
     *
     * @param canonicalizeLiterals whether the received literals are brought to their canonical lexical forms
     * @return copy with the given canonicalization of received literals
     */
    public ServiceSettings withCanonicalizeLiterals(boolean canonicalizeLiterals)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }


    /**
     * Copy with the given context limit.
     *
     * @param contextLimit maximum number of context solutions a call may be evaluated for
     * @return copy with the given context limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public ServiceSettings withContextLimit(int contextLimit)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }


    /**
     * Copy with the given result limit.
     *
     * @param resultLimit maximum number of solutions one evaluation of a call may collect from the endpoint
     * @return copy with the given result limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public ServiceSettings withResultLimit(int resultLimit)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }


    /**
     * Copy with the given redirect limit.
     *
     * @param redirectLimit maximum number of HTTP redirects followed by one request to the endpoint
     * @return copy with the given redirect limit
     * @throws IllegalArgumentException if the limit is negative
     */
    public ServiceSettings withRedirectLimit(int redirectLimit)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }


    /**
     * Copy with the given behaviour on the overflow of the context.
     *
     * @param contextOverflow what a call does when its context has more solutions than the context limit
     * @return copy with the given behaviour on the overflow of the context
     */
    public ServiceSettings withContextOverflow(ContextOverflow contextOverflow)
    {
        return new ServiceSettings(sendLiterals, canonicalizeLiterals, contextLimit, resultLimit, redirectLimit,
                contextOverflow);
    }
}
