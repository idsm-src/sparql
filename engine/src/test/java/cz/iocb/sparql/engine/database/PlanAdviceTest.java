package cz.iocb.sparql.engine.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;



/**
 * The advice made of the advice the planner generates for a statement (see {@link PlanAdvice#build}): the join of an
 * access written under the alias of a lookup becomes a nested loop with the access on the inner side, whichever side
 * the planner has put it on, the hash joins and the uniqueness of the inner sides of semijoins are kept where the joins
 * are, and the rest is left to the planner.
 */
public class PlanAdviceTest
{
    @Test
    public void lookupOnInnerSide()
    {
        String generated = "JOIN_ORDER(a lookup_plain b) HASH_JOIN(lookup_plain b) SEQ_SCAN(a b) "
                + "INDEX_SCAN(lookup_plain public.c_idx) NO_GATHER(a lookup_plain b)";

        assertThat(PlanAdvice.build(generated, true), is("HASH_JOIN(b) NESTED_LOOP_PLAIN(lookup_plain)"));
    }


    @Test
    public void lookupsOnly()
    {
        String generated = "JOIN_ORDER(a lookup_plain (b lookup_memoized)) HASH_JOIN((b lookup_memoized)) "
                + "NESTED_LOOP_PLAIN(lookup_plain lookup_memoized) SEMIJOIN_UNIQUE((b lookup_memoized))";

        assertThat(PlanAdvice.build(generated, false),
                is("NESTED_LOOP_PLAIN(lookup_plain) NESTED_LOOP_MEMOIZE(lookup_memoized)"));
    }


    @Test
    public void lookupOnOuterSide()
    {
        // the planner has hashed the other side of the join, which is no longer hashed
        String generated = "JOIN_ORDER(lookup_memoized (a b) c) HASH_JOIN(b (a b) c) SEQ_SCAN(a b c)";

        assertThat(PlanAdvice.build(generated, true), is("HASH_JOIN(b c) NESTED_LOOP_MEMOIZE(lookup_memoized)"));
    }


    @Test
    public void lookupsInSubqueries()
    {
        String generated = """
                JOIN_ORDER(tab0 (lookup_plain#2@"Sub q" t@"Sub q") lookup_plain)
                   HASH_JOIN(t@"Sub q" (lookup_plain#2@"Sub q" t@"Sub q")
                   lookup_plain) GATHER((tab0 lookup_plain))""";

        assertThat(PlanAdvice.build(generated, true), is("""
                HASH_JOIN((lookup_plain#2@"Sub q" t@"Sub q")) \
                NESTED_LOOP_PLAIN(lookup_plain#2@"Sub q" lookup_plain)"""));
    }


    @Test
    public void nestedLoopsLeftToPlanner()
    {
        String generated = "JOIN_ORDER(a b c) NESTED_LOOP_PLAIN(b) HASH_JOIN(c) GATHER(a) BITMAP_HEAP_SCAN(c)";

        assertThat(PlanAdvice.build(generated, true), is("HASH_JOIN(c)"));
    }


    @Test
    public void semijoinsKept()
    {
        String generated = "JOIN_ORDER(a (b lookup_plain)) HASH_JOIN(lookup_plain (b lookup_plain)) "
                + "SEMIJOIN_NON_UNIQUE((b lookup_plain)) SEMIJOIN_UNIQUE(d)";

        assertThat(PlanAdvice.build(generated, true), is("""
                HASH_JOIN((b lookup_plain)) SEMIJOIN_NON_UNIQUE((b lookup_plain)) SEMIJOIN_UNIQUE(d) \
                NESTED_LOOP_PLAIN(lookup_plain)"""));
    }


    @Test
    public void unorderedGroupLeft()
    {
        // the members of a group whose order is open have no sides
        String generated = "JOIN_ORDER(a {lookup_plain b}) FOREIGN_JOIN((lookup_plain b))";

        assertThat(PlanAdvice.build(generated, true), is(""));
    }


    @Test
    public void aliasOfLookupOnlyCounts()
    {
        // a relation whose name merely starts like the alias of a lookup is no lookup
        String generated = "JOIN_ORDER(a lookup_plains \"lookup_plain\"\"\" lookup_plain@lookup_plain) "
                + "HASH_JOIN(lookup_plains \"lookup_plain\"\"\" lookup_plain@lookup_plain)";

        assertThat(PlanAdvice.build(generated, true),
                is("HASH_JOIN(lookup_plains \"lookup_plain\"\"\") NESTED_LOOP_PLAIN(lookup_plain@lookup_plain)"));
    }


    @Test
    public void noAdvice()
    {
        assertThat(PlanAdvice.build("", true), is(""));
        assertThat(PlanAdvice.build(" \n ", true), is(""));
    }


    @Test
    public void malformedAdvice()
    {
        assertThrows(IllegalArgumentException.class, () -> PlanAdvice.build("HASH_JOIN(a", true));
        assertThrows(IllegalArgumentException.class, () -> PlanAdvice.build("HASH_JOIN a", true));
        assertThrows(IllegalArgumentException.class, () -> PlanAdvice.build("JOIN_ORDER(a (b c})", true));
        assertThrows(IllegalArgumentException.class, () -> PlanAdvice.build("HASH_JOIN(\"a)", true));
    }
}
