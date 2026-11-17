package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static java.util.Locale.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The comparison of literals of a user datatype, which a datatype holds in its canonical class, in its base class with
 * their lexical forms, or in a box: two literals of one datatype are equal when their values are, whatever their
 * lexical forms are and wherever they are stored, as sameValue() of SPARQL 1.2 says, and literals of two datatypes are
 * different values. The ordering operators compare the values of one datatype as the box does.
 *
 * The datatype of sizes is backed by an enum type ({@code small}, {@code medium}, {@code large}), the canonical forms
 * being lower-case; a second datatype over the same type has the same values. The table {@code item} holds the sizes
 * medium, large and small of three items in a column of the type ({@code :direct}), in a column of the type with a
 * lexical form column ({@code :base}, the forms being canonical, {@code LARGE} and {@code Small}) and in a box column
 * ({@code :boxed}, the same forms).
 */
public class UserLiteralEqualityTest
{
    /**
     * Datatype of the sizes {@code small}, {@code medium} and {@code large}, the canonical forms being lower-case.
     */
    private static final class SizeDatatype extends UserDatatype
    {
        /**
         * Creates the datatype of the IRI over the enum type {@code user_literal_equality_test.size}.
         *
         * @param typeIri the datatype IRI
         * @param name the name of the literal classes
         */
        SizeDatatype(Iri typeIri, String name)
        {
            super(typeIri, name, new UserType("user_literal_equality_test.size"));
        }


        @Override
        public boolean isValidForm(String value)
        {
            return List.of("small", "medium", "large").contains(value.toLowerCase(ROOT));
        }


        @Override
        public String getCanonicalLexicalForm(String value)
        {
            return isValidForm(value) ? value.toLowerCase(ROOT) : null;
        }
    }


    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * The value true.
     */
    private static final TypedLiteral yes = new TypedLiteral("true", xsdBooleanIri);

    /**
     * The value false.
     */
    private static final TypedLiteral no = new TypedLiteral("false", xsdBooleanIri);

    /**
     * The results of {@code =} and {@code !=} of equal values.
     */
    private static final List<RdfTerm> equal = Arrays.asList(yes, no);

    /**
     * The results of {@code =} and {@code !=} of different values.
     */
    private static final List<RdfTerm> different = Arrays.asList(no, yes);

    /**
     * The ways the table stores the sizes, as the predicates mapping them.
     */
    private static final List<String> storages = List.of(":direct", ":base", ":boxed");

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the enum type and the table {@code item} and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema user_literal_equality_test");
            statement.execute("create type user_literal_equality_test.size as enum ('small', 'medium', 'large')");
            statement.execute("""
                    create table user_literal_equality_test.item (iri varchar collate "C" primary key, \
                    direct user_literal_equality_test.size not null, \
                    base_value user_literal_equality_test.size not null, \
                    base_lexical varchar collate "C" not null, boxed sparql.rdfbox not null)""");
            statement.execute("""
                    insert into user_literal_equality_test.item values \
                    ('http://example.org/item/1', 'medium', 'medium', '', \
                    sparql.rdfbox_create_from_userliteral('medium'::user_literal_equality_test.size, \
                    'http://example.org/size')), \
                    ('http://example.org/item/2', 'large', 'large', 'LARGE', \
                    sparql.rdfbox_create_from_userliteral_with_lexical('large'::user_literal_equality_test.size, \
                    'http://example.org/size', 'LARGE')), \
                    ('http://example.org/item/3', 'small', 'small', 'Small', \
                    sparql.rdfbox_create_from_userliteral_with_lexical('small'::user_literal_equality_test.size, \
                    'http://example.org/size', 'Small'))""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");

        UserDatatype size = new SizeDatatype(new Iri("http://example.org/size"), "size");
        config.addDatatype(size);
        config.addDatatype(new SizeDatatype(new Iri("http://example.org/size2"), "size2"));

        DatabaseTable table = new DatabaseTable("user_literal_equality_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":direct"),
                config.createLiteralMapping(size.getCanonicalLiteralClass(), "direct"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":base"),
                config.createLiteralMapping(size.getBaseLiteralClass(), "base_value", "base_lexical"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":boxed"),
                config.createTermMapping(box, "boxed"));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Rows of pairs of items.
     *
     * @param pairs the numbers of the items, two for each row
     * @return rows of the pairs of items
     */
    private static Object[] items(int... pairs)
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        for(int i = 0; i < pairs.length; i += 2)
            rows.add(List.of(new Iri("http://example.org/item/" + pairs[i]),
                    new Iri("http://example.org/item/" + pairs[i + 1])));

        return rows.toArray();
    }


    @Test
    @DisplayName("literals of one user datatype are equal when their values are")
    void constants() throws Exception
    {
        assertThat(execute("""
                SELECT ?e ?n WHERE {
                  VALUES (?l ?r) {
                    ("large"^^:size "LARGE"^^:size) ("Large"^^:size "LARGE"^^:size) ("large"^^:size "large"^^:size)
                    ("large"^^:size "small"^^:size) ("Large"^^:size "Small"^^:size) ("large"^^:size "large"^^:size2)
                  }
                  BIND(?l = ?r AS ?e)
                  BIND(?l != ?r AS ?n)
                }"""), containsInAnyOrder(equal, equal, equal, different, different, different));
    }


    @Test
    @DisplayName("the equality of literals of one user datatype does not depend on their storage")
    void storages() throws Exception
    {
        for(String left : storages)
            for(String right : storages)
                assertThat(left + " " + right,
                        execute("SELECT ?a ?b WHERE { ?a " + left + " ?x . ?b " + right + " ?y FILTER(?x = ?y) }"),
                        containsInAnyOrder(items(1, 1, 2, 2, 3, 3)));
    }


    @Test
    @DisplayName("the ordering operators compare the values of one user datatype whatever their storage is")
    void ordering() throws Exception
    {
        // small < medium < large, the items holding medium, large and small
        for(String left : storages)
            for(String right : storages)
                assertThat(left + " " + right,
                        execute("SELECT ?a ?b WHERE { ?a " + left + " ?x . ?b " + right + " ?y FILTER(?x < ?y) }"),
                        containsInAnyOrder(items(1, 2, 3, 1, 3, 2)));
    }
}
