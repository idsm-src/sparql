package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUserType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.userType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.Locale.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.UserType;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
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
 * Casts to a user datatype with a lexical parser ({@link UserDatatype#getLexicalParser}): the IRI of the datatype names
 * the cast, which takes a literal of the datatype in any storage (a column of the type, a column of the type with a
 * lexical form, a box and the columns of the result classes of user literals) to its canonical literal and parses an
 * {@code xsd:string}, giving no value for an invalid form and for the other terms; the cast takes precedence over an
 * extension function of the same IRI, whereas the IRI of a user datatype without a parser still names its function. The
 * datatype {@code :size} has the sizes {@code small}, {@code medium} and {@code large}, the canonical forms being
 * lower-case, over an enum type; the datatype {@code :plain} over the same type has no parser, and its IRI names a
 * function parsing the size, as does the IRI of {@code :size} a function giving {@code large} always.
 */
public class UserLiteralCastTest
{
    /**
     * Datatype of the sizes over the enum type {@code user_literal_cast_test.size}, with the given lexical parser.
     */
    private static final class SizeDatatype extends UserDatatype
    {
        /**
         * Lexical parser of the datatype.
         */
        private final Function parser;


        /**
         * Creates the datatype of the IRI.
         *
         * @param typeIri the datatype IRI
         * @param name the name of the literal classes
         * @param parser lexical parser of the datatype, or null
         */
        SizeDatatype(Iri typeIri, String name, Function parser)
        {
            super(typeIri, name, new UserType("user_literal_cast_test.size"));
            this.parser = parser;
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


        @Override
        public Function getLexicalParser()
        {
            return parser;
        }
    }


    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * IRI of the datatype with a parser.
     */
    private static final Iri sizeIri = new Iri("http://example.org/size");

    /**
     * IRI of the datatype without a parser.
     */
    private static final Iri plainIri = new Iri("http://example.org/plain");

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the enum type, the parser, the functions and the table {@code item} of three rows holding a size in a
     * column of the type ({@code :direct}: medium, large, small), in a column of the type with a lexical form
     * ({@code :base}: the same, written {@code medium}, {@code LARGE} and {@code Small}), in a box ({@code :boxed}: the
     * same as {@code :base} in the first two rows, the string {@code Medium} in the third), in the columns of the
     * result classes of user literals ({@code :user} and {@code :general}: the same as {@code :base}) and a string
     * ({@code :string}: {@code Small}, {@code tiny}, {@code MEDIUM}), and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema user_literal_cast_test");
            statement.execute("create type user_literal_cast_test.size as enum ('small', 'medium', 'large')");
            statement.execute("""
                    create function user_literal_cast_test.parse(value varchar) returns user_literal_cast_test.size \
                    language sql immutable strict parallel safe as $$ select lower(value)::user_literal_cast_test.size \
                    where lower(value) in ('small', 'medium', 'large') $$""");
            statement.execute("""
                    create function user_literal_cast_test.large(value varchar) returns user_literal_cast_test.size \
                    language sql immutable strict parallel safe as \
                    $$ select 'large'::user_literal_cast_test.size $$""");
            statement.execute("""
                    create table user_literal_cast_test.item (iri varchar collate "C" primary key, \
                    direct user_literal_cast_test.size not null, base_value user_literal_cast_test.size not null, \
                    base_lexical varchar collate "C" not null, boxed sparql.rdfbox not null, \
                    user_value sparql.ubox not null, user_type varchar collate "C" not null, \
                    general_lexical varchar collate "C" not null, string varchar collate "C" not null)""");
            statement.execute("""
                    insert into user_literal_cast_test.item values \
                    ('http://example.org/item/1', 'medium', 'medium', '', \
                    sparql.rdfbox_create_from_userliteral('medium'::user_literal_cast_test.size, \
                    'http://example.org/size'), sparql.ubox_create('medium'::user_literal_cast_test.size), \
                    'http://example.org/size', '', 'Small'), \
                    ('http://example.org/item/2', 'large', 'large', 'LARGE', \
                    sparql.rdfbox_create_from_userliteral_with_lexical('large'::user_literal_cast_test.size, \
                    'http://example.org/size', 'LARGE'), sparql.ubox_create('large'::user_literal_cast_test.size), \
                    'http://example.org/size', 'LARGE', 'tiny'), \
                    ('http://example.org/item/3', 'small', 'small', 'Small', \
                    sparql.rdfbox_create_from_string('Medium'), \
                    sparql.ubox_create('small'::user_literal_cast_test.size), 'http://example.org/size', 'Small', \
                    'MEDIUM')""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");

        Function parser = new Function("user_literal_cast_test", "parse");
        UserDatatype size = new SizeDatatype(sizeIri, "size", parser);
        UserDatatype plain = new SizeDatatype(plainIri, "plain", null);
        config.addDatatype(size);
        config.addDatatype(plain);

        config.addFunction(new FunctionDefinition(sizeIri.getValue(), new Function("user_literal_cast_test", "large"),
                size.getCanonicalLiteralClass(), List.of(xsdString), true, true));
        config.addFunction(new FunctionDefinition(plainIri.getValue(), parser, plain.getCanonicalLiteralClass(),
                List.of(xsdString), true, true));

        DatabaseTable table = new DatabaseTable("user_literal_cast_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":direct"),
                config.createLiteralMapping(size.getCanonicalLiteralClass(), "direct"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":base"),
                config.createLiteralMapping(size.getBaseLiteralClass(), "base_value", "base_lexical"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":boxed"),
                config.createTermMapping(box, "boxed"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":user"),
                config.createLiteralMapping(userType, "user_value", "user_type"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":general"),
                config.createLiteralMapping(genUserType, "user_value", "user_type", "general_lexical"));
        config.addQuadMapping(table, graph, config.createIriMapping(iri, "iri"), config.createIriMapping(":string"),
                config.createLiteralMapping(xsdString, "string"));

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
     * Row of the item and the size.
     *
     * @param item the number of the item
     * @param size the canonical form of the size, or null for no value
     * @return row of the item and the size
     */
    private static List<RdfTerm> row(int item, String size)
    {
        return Arrays.asList(item(item), size == null ? null : size(size));
    }


    /**
     * The item.
     *
     * @param number the number of the item
     * @return the item
     */
    private static Iri item(int number)
    {
        return new Iri("http://example.org/item/" + number);
    }


    /**
     * The literal of the datatype with a parser.
     *
     * @param lexical the lexical form
     * @return the literal of the datatype
     */
    private static TypedLiteral size(String lexical)
    {
        return new TypedLiteral(lexical, sizeIri);
    }


    /**
     * Rows of the items and the cast of their values of the property.
     *
     * @param property the local name of the property
     * @return rows of the items and the cast values
     * @throws Exception on errors of the query
     */
    private static List<List<RdfTerm>> cast(String property) throws Exception
    {
        return execute("SELECT ?s ?c WHERE { ?s :" + property + " ?v BIND(:size(?v) AS ?c) }");
    }


    @Test
    @DisplayName("a literal of the datatype is cast to its canonical literal in any storage")
    void literals() throws Exception
    {
        for(String property : List.of("direct", "base", "user", "general"))
            assertThat(property, cast(property),
                    containsInAnyOrder(row(1, "medium"), row(2, "large"), row(3, "small")));

        // a box holds a literal of the datatype or a string, which is parsed
        assertThat(cast("boxed"), containsInAnyOrder(row(1, "medium"), row(2, "large"), row(3, "medium")));

        // the values of all storages at once
        assertThat(execute("SELECT DISTINCT ?c WHERE { ?s ?p ?v BIND(:size(?v) AS ?c) FILTER(BOUND(?c)) }").size(),
                is(3));
    }


    @Test
    @DisplayName("a string is parsed by the parser, and an invalid one gives no value")
    void strings() throws Exception
    {
        assertThat(cast("string"), containsInAnyOrder(row(1, "small"), row(2, null), row(3, "medium")));

        assertThat(execute("SELECT (:size('Large') AS ?c) WHERE {}"), is(List.of(List.of(size("large")))));
        assertThat(execute("SELECT ?c WHERE { VALUES ?v { 'Small' 'tiny' } BIND(:size(?v) AS ?c) }"),
                containsInAnyOrder(Arrays.asList(size("small")), Arrays.asList((RdfTerm) null)));
    }


    @Test
    @DisplayName("constant literals of the datatype are cast, and the other terms give no value")
    void constants() throws Exception
    {
        assertThat(
                execute("SELECT ?c WHERE { VALUES ?v { 'Small'^^:size 'tiny'^^:size 1 'medium'@en :medium } "
                        + "BIND(:size(?v) AS ?c) }"),
                containsInAnyOrder(Arrays.asList(size("small")), Arrays.asList((RdfTerm) null),
                        Arrays.asList((RdfTerm) null), Arrays.asList((RdfTerm) null), Arrays.asList((RdfTerm) null)));
    }


    @Test
    @DisplayName("the cast is ordered, compared and used in the patterns of expressions")
    void expressions() throws Exception
    {
        assertThat(execute("SELECT ?c WHERE { ?s :string ?v BIND(:size(?v) AS ?c) FILTER(BOUND(?c)) } ORDER BY ?c"),
                is(List.of(List.of(size("small")), List.of(size("medium")))));

        assertThat(execute("SELECT ?s WHERE { ?s :direct ?v FILTER EXISTS { ?t :string ?w FILTER(:size(?w) = ?v) } }"),
                containsInAnyOrder(List.of(item(1)), List.of(item(3))));
    }


    @Test
    @DisplayName("the cast takes precedence over a function, the IRI of a datatype without a parser names its function")
    void functions() throws Exception
    {
        assertThat(execute("SELECT (:size('small') AS ?c) WHERE {}"), is(List.of(List.of(size("small")))));
        assertThat(execute("SELECT (:plain('Small') AS ?c) WHERE {}"),
                is(List.of(List.of(new TypedLiteral("small", plainIri)))));
    }


    @Test
    @DisplayName("a cast takes one argument")
    void arity()
    {
        TranslateExceptions e = assertThrows(TranslateExceptions.class,
                () -> execute("SELECT (:size('small', 'large') AS ?c) WHERE {}"));

        assertThat(e.getMessages(), hasItem(hasProperty("message",
                is(String.format(ROOT, MessageType.wrongCountOfParameters.getText(), ":size", 1)))));
    }
}
