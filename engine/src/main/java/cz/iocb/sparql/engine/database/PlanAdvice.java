package cz.iocb.sparql.engine.database;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * Plan advice of the module {@code pg_plan_advice} of PostgreSQL 19 and later, by which the planner is made to look up
 * the table accesses the join optimizer of the engine looks up for every row of the other side of their joins (see
 * {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#setPlanAdvice}). The engine writes such an access as
 * a plain join of the statement under the alias {@link #LOOKUP} or {@link #MEMOIZED_LOOKUP}, and the advice makes the
 * planner join it by a nested loop with the access on the inner side, which keeps the rows found for the values looked
 * up (Memoize) for the latter. So a lookup is a join of the statement itself, which the database can hand to parallel
 * workers, whereas a lateral subquery fenced by {@code OFFSET 0}, which the settings of the optimizer alone need for a
 * lookup, is evaluated in the process reading the outer rows. The advice names the relations of a statement by
 * identifiers the planner derives from their aliases and from the names it gives the subqueries it plans apart, which
 * only the planner knows, so it is made from the advice the planner generates for the statement ({@code EXPLAIN} with
 * the option {@code PLAN_ADVICE}). The statement runs under the settings of the optimizer, which make the planner keep
 * the order of the joins as written and hash every join it can, as the advice only rules out what the planner would do
 * otherwise: the planner chooses the other joins, the scans and the parallelism as it does for the fenced lookups. A
 * memoized nested loop, however, is never considered with the nested loops off, so for a statement with a memoized
 * lookup the nested loops are allowed, and the advice keeps every hash join as the planner has chosen it for the
 * statement with its lookups advised, as well as whether it makes the inner side of a semijoin unique.
 */
public final class PlanAdvice
{
    /**
     * Alias of a table access looked up for every row of the other side of its join by a plain nested loop.
     */
    public static final String LOOKUP = "lookup_plain";

    /**
     * Alias of a table access looked up for every row of the other side of its join by a nested loop that keeps the
     * rows found for the values the access is looked up by (Memoize), as the values repeat in the rows.
     */
    public static final String MEMOIZED_LOOKUP = "lookup_memoized";

    /**
     * The tags of the generated advice that the advice of the statement keeps where it keeps the joins (see
     * {@link #build}): the hash joins, and whether the inner side of a semijoin is made unique.
     */
    private static final Set<String> keptTags = Set.of("HASH_JOIN", "SEMIJOIN_UNIQUE", "SEMIJOIN_NON_UNIQUE");


    /**
     * A target of an item of advice: the identifier of a relation, or a group of targets.
     */
    private sealed interface Target permits Identifier, Group
    {
        /**
         * Adds the identifiers of the relations of the target.
         *
         * @param identifiers the set the identifiers are added to
         */
        void collect(Set<String> identifiers);


        /**
         * Appends the target as the advice writes it.
         *
         * @param builder the text of the advice being built
         */
        void append(StringBuilder builder);
    }


    /**
     * The identifier of a relation.
     *
     * @param text the identifier as the advice writes it, the alias followed by the number of its occurrence, the
     *            partition and the name of the subquery it lies in, each where the planner needs it
     * @param alias the alias of the relation, unquoted
     */
    private record Identifier(String text, String alias) implements Target
    {
        @Override
        public void collect(Set<String> identifiers)
        {
            identifiers.add(text);
        }


        @Override
        public void append(StringBuilder builder)
        {
            builder.append(text);
        }
    }


    /**
     * Targets enclosed in parentheses, which make a join of the relations or a part of a join order, or in braces,
     * which make a part of a join order that leaves the order of its members open.
     *
     * @param members the targets of the group
     * @param ordered whether the group is enclosed in parentheses
     */
    private record Group(List<Target> members, boolean ordered) implements Target
    {
        @Override
        public void collect(Set<String> identifiers)
        {
            for(Target member : members)
                member.collect(identifiers);
        }


        @Override
        public void append(StringBuilder builder)
        {
            builder.append(ordered ? '(' : '{');
            appendTargets(builder, members);
            builder.append(ordered ? ')' : '}');
        }
    }


    /**
     * An item of advice: a tag applied to targets.
     *
     * @param tag the tag
     * @param targets the targets
     */
    private record Item(String tag, List<Target> targets)
    {
    }


    /**
     * Reader of the items of a text of advice.
     */
    private static final class Parser
    {
        /**
         * The text of the advice.
         */
        private final String text;

        /**
         * The position of the next character to read.
         */
        private int position = 0;


        /**
         * Creates the reader of the text.
         *
         * @param text the text of the advice
         */
        Parser(String text)
        {
            this.text = text;
        }


        /**
         * Reads the items of the text.
         *
         * @return the items
         * @throws IllegalArgumentException if the text is no advice
         */
        List<Item> parse()
        {
            List<Item> items = new ArrayList<>();

            for(skipSpaces(); position < text.length(); skipSpaces())
            {
                String tag = readWord();

                skipSpaces();

                if(position == text.length() || text.charAt(position) != '(')
                    throw new IllegalArgumentException("no targets of the advice " + tag);

                position++;
                items.add(new Item(tag, readTargets(')')));
            }

            return items;
        }


        /**
         * Reads the targets up to the character closing them, which is read as well.
         *
         * @param end the character closing the targets
         * @return the targets
         * @throws IllegalArgumentException if the targets are not closed
         */
        private List<Target> readTargets(char end)
        {
            List<Target> targets = new ArrayList<>();

            while(true)
            {
                skipSpaces();

                if(position == text.length())
                    throw new IllegalArgumentException("unterminated targets of advice");

                char c = text.charAt(position);

                if(c == end)
                {
                    position++;
                    return targets;
                }

                if(c == '(' || c == '{')
                {
                    position++;
                    targets.add(new Group(readTargets(c == '(' ? ')' : '}'), c == '('));
                }
                else if(c == ')' || c == '}')
                {
                    throw new IllegalArgumentException("unbalanced targets of advice");
                }
                else
                {
                    String word = readWord();
                    targets.add(new Identifier(word, getAlias(word)));
                }
            }
        }


        /**
         * Reads a word: a tag, the identifier of a relation or the name of an index, which may hold quoted parts.
         *
         * @return the word
         * @throws IllegalArgumentException if there is no word or a quoted part is not closed
         */
        private String readWord()
        {
            int begin = position;
            boolean quoted = false;

            for(; position < text.length(); position++)
            {
                char c = text.charAt(position);

                // a doubled quote inside a quoted part closes it and opens it again
                if(c == '"')
                    quoted = !quoted;
                else if(!quoted && (Character.isWhitespace(c) || c == '(' || c == ')' || c == '{' || c == '}'))
                    break;
            }

            if(quoted || position == begin)
                throw new IllegalArgumentException("malformed word of advice");

            return text.substring(begin, position);
        }


        /**
         * Skips the white space.
         */
        private void skipSpaces()
        {
            while(position < text.length() && Character.isWhitespace(text.charAt(position)))
                position++;
        }
    }


    /**
     * Creates nothing; the class has static members only.
     */
    private PlanAdvice()
    {
    }


    /**
     * The advice that makes the planner look up the accesses the join optimizer looks up, made of the advice the
     * planner has generated for the statement (see the description of the class): the join of every such access is a
     * nested loop with the access on the inner side, a memoized one for an access written under
     * {@link #MEMOIZED_LOOKUP}. An access is found where its join is: in a join order, a member that is not the first
     * one is the inner side of a join with the members before it, and the first one is the outer side of a join with
     * the second one. With the joins kept, the advice gives the hash joins of the generated advice as well, but for
     * those of the lookups, and the uniqueness of the inner sides of its semijoins, which the planner otherwise chooses
     * by itself with the nested loops off. The order of the joins, which the settings keep as written, the scans and
     * the parallelism are never given, so that the planner chooses them as it would without the advice; nor are the
     * nested loops of the generated advice, which the planner has chosen because it could not hash the join, so that it
     * may cache their inner sides as well.
     *
     * @param generated the advice the planner has generated for the statement
     * @param joins whether the advice keeps the hash joins and the uniqueness of the inner sides of the semijoins
     * @return the advice for the statement
     * @throws IllegalArgumentException if the generated advice cannot be read
     */
    public static String build(String generated, boolean joins)
    {
        List<Item> items = new Parser(generated).parse();

        // the inner sides of the joins of the lookups, which are no longer hashed, and the lookups by their tags
        Set<Set<String>> lookedUp = new HashSet<>();
        Map<String, List<Target>> lookups = new LinkedHashMap<>();

        for(Item item : items)
            if(item.tag().equals("JOIN_ORDER"))
                findLookups(new Group(item.targets(), true), lookedUp, lookups);

        StringBuilder builder = new StringBuilder();

        for(Item item : items)
        {
            if(!joins || !keptTags.contains(item.tag()))
                continue;

            List<Target> targets = !item.tag().equals("HASH_JOIN") ? item.targets() :
                    item.targets().stream().filter(t -> !lookedUp.contains(getIdentifiers(t))).toList();

            appendItem(builder, item.tag(), targets);
        }

        for(Map.Entry<String, List<Target>> entry : lookups.entrySet())
            appendItem(builder, entry.getKey(), entry.getValue());

        return builder.toString();
    }


    /**
     * Finds the accesses looked up by the optimizer in a part of a join order (see {@link #build}).
     *
     * @param group the part of the join order
     * @param lookedUp the inner sides of the joins of the accesses, as sets of identifiers, which the accesses found
     *            are added to
     * @param lookups the identifiers of the accesses by the tags of their joins, which the accesses found are added to
     */
    private static void findLookups(Group group, Set<Set<String>> lookedUp, Map<String, List<Target>> lookups)
    {
        List<Target> members = group.members();

        for(int i = 0; i < members.size(); i++)
        {
            Target member = members.get(i);

            if(member instanceof Group nested)
            {
                findLookups(nested, lookedUp, lookups);
                continue;
            }

            Identifier identifier = (Identifier) member;
            String tag = identifier.alias().equals(LOOKUP) ? "NESTED_LOOP_PLAIN" :
                    identifier.alias().equals(MEMOIZED_LOOKUP) ? "NESTED_LOOP_MEMOIZE" : null;

            // the members of a group leaving their order open have no sides
            if(tag == null || !group.ordered() || members.size() < 2)
                continue;

            lookedUp.add(getIdentifiers(i > 0 ? identifier : members.get(1)));
            lookups.computeIfAbsent(tag, _ -> new ArrayList<>()).add(identifier);
        }
    }


    /**
     * The identifiers of the relations of a target.
     *
     * @param target the target
     * @return the identifiers of the relations of the target
     */
    private static Set<String> getIdentifiers(Target target)
    {
        Set<String> identifiers = new HashSet<>();
        target.collect(identifiers);
        return identifiers;
    }


    /**
     * The unquoted alias of a relation from its identifier: the part before the number of the occurrence, the partition
     * and the name of the subquery.
     *
     * @param identifier the identifier as the advice writes it
     * @return the alias
     */
    private static String getAlias(String identifier)
    {
        StringBuilder alias = new StringBuilder();
        boolean quoted = false;

        for(int i = 0; i < identifier.length(); i++)
        {
            char c = identifier.charAt(i);

            if(c == '"')
            {
                // a doubled quote inside a quoted part stands for a quote
                if(quoted && i + 1 < identifier.length() && identifier.charAt(i + 1) == '"')
                    alias.append(identifier.charAt(++i));
                else
                    quoted = !quoted;
            }
            else if(!quoted && (c == '#' || c == '/' || c == '@'))
            {
                break;
            }
            else
            {
                alias.append(c);
            }
        }

        return alias.toString();
    }


    /**
     * Appends an item of advice, separated from the previous one by a space; an item without targets is left out.
     *
     * @param builder the text of the advice being built
     * @param tag the tag of the item
     * @param targets the targets of the item
     */
    private static void appendItem(StringBuilder builder, String tag, List<Target> targets)
    {
        if(targets.isEmpty())
            return;

        if(!builder.isEmpty())
            builder.append(' ');

        builder.append(tag).append('(');
        appendTargets(builder, targets);
        builder.append(')');
    }


    /**
     * Appends targets separated by spaces.
     *
     * @param builder the text of the advice being built
     * @param targets the targets
     */
    private static void appendTargets(StringBuilder builder, List<Target> targets)
    {
        for(int i = 0; i < targets.size(); i++)
        {
            if(i > 0)
                builder.append(' ');

            targets.get(i).append(builder);
        }
    }
}
