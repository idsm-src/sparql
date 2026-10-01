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
 * intersection as its class.
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
