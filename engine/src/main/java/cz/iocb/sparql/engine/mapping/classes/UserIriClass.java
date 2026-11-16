package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * IRI class defined by a deployment: IRIs of a recognisable shape whose identifying part is stored in native columns.
 * Two user IRI classes may be related (one a subclass of the other) or disjoint, and they may also overlap when the
 * configuration declares the pair as sharing IRIs
 * ({@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#addIriClassOverlap(UserIriClass, UserIriClass)});
 * the declarations ({@link ClassRelations}) are not kept by the classes, as they depend on the classes registered
 * together, but are supplied to the operations deciding disjointness. An IRI belonging to several classes gets their
 * intersection as its class. A class recognising its IRIs by a regular expression evaluates it in Java when it matches
 * an IRI and hands the same text to {@code sparql.regex_string} of the extension, which evaluates it by PCRE2, when it
 * converts a value with the representability check; it anchors the text by {@link #wholeIriRegexp}, so that both
 * dialects decide alike, and lists it by {@link #getSqlRegexps}, so that the configuration checks that PCRE2 compiles
 * it.
 */
public non-sealed abstract class UserIriClass extends IriClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected UserIriClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Relative cost of {@link #match}: 0 for a regular expression test only, 1 when a database lookup may be needed, 2
     * when it always is. The configuration tries cheaper classes first when detecting the class of an IRI.
     *
     * @return relative cost of {@link #match}: 0 for a regular expression test only, 1 when a database lookup may be
     *         needed, 2 when it always is
     */
    public abstract int getCheckCost();


    /**
     * The regular expressions the class hands to {@code sparql.regex_string} of the extension, which returns NULL for
     * an expression PCRE2 does not compile: the configuration checks them when it registers the class
     * ({@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#addIriClass}). None by default.
     *
     * @return the regular expressions the class hands to {@code sparql.regex_string}
     */
    public List<String> getSqlRegexps()
    {
        return List.of();
    }


    /**
     * Regular expression matching an IRI as a whole when the body matches it, written so that {@code java.util.regex}
     * and PCRE2 decide alike: anchored by {@code ^} and {@code \z} (the {@code $} of PCRE2 matches also before a final
     * line feed), with {@code .} matching every character ({@code (?s)}; Java does not match the carriage return and
     * the other line terminators otherwise), and the body in a group of its own, so that an alternation stays inside. A
     * class compiles in Java the same text it hands to {@code sparql.regex_string}, so the dialects decide the
     * membership of an IRI alike as long as the body keeps to their common subset.
     *
     * @param body regular expression of the IRIs
     * @return regular expression matching an IRI as a whole when the body matches it
     */
    protected static String wholeIriRegexp(String body)
    {
        return "(?s)^(?:" + body + ")\\z";
    }


    /**
     * True if the classes are unrelated and the declarations do not let them overlap
     * ({@link ClassRelations#mayOverlap}). A class other than a user IRI class is disjoint with this class whenever
     * they are unrelated: the unsupported IRI class holds the IRIs of no user class, and the classes of other kinds of
     * terms hold no IRI.
     */
    @Override
    protected final boolean isDisjunctWith(ClassRelations relations, PrimitiveResourceClass resClass)
    {
        if(!super.isDisjunctWith(relations, resClass))
            return false;

        return !(resClass instanceof UserIriClass other) || !relations.mayOverlap(this, other);
    }



    /**
     * SQL expression concatenating the (non-null) prefix, the value and the (non-null) suffix into a varchar.
     *
     * @param prefix the prefix
     * @param value the value column
     * @param suffix the suffix
     * @return SQL expression concatenating the (non-null) prefix, the value and the (non-null) suffix into a varchar
     */
    protected static Column addPrefixAndSuffix(String prefix, Column value, String suffix)
    {
        if(prefix != null && suffix != null)
            return expression(VARCHAR, "(%s || %s || %s)::varchar", string(prefix), value, string(suffix));
        else if(prefix != null)
            return expression(VARCHAR, "(%s || %s)::varchar", string(prefix), value);
        else if(suffix != null)
            return expression(VARCHAR, "(%s || %s)::varchar", value, string(suffix));
        else
            return value;
    }
}
