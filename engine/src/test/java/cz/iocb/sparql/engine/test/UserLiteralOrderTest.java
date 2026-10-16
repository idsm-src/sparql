package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.util.Locale.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
 * ORDER BY over literals of a user datatype: the terms are ordered by the total order of the box (the value by the
 * comparison of its PostgreSQL type, then the datatype, the canonical forms before the lexical ones), whether they are
 * stored in a column of the type, in a column of the type with their lexical form, or in a box, so that the order does
 * not depend on the storage; a variable holding such literals among terms of other kinds is ordered by the box as a
 * whole. The datatype is backed by an enum type, whose order is the order of its labels, not the order of their text; a
 * second datatype over the same type tells the literals of several datatypes apart by the datatype IRI.
 */
public class UserLiteralOrderTest
{
    /**
     * Datatype of the sizes {@code small}, {@code medium} and {@code large}, the canonical forms being lower-case.
     */
    private static final class SizeDatatype extends UserDatatype
    {
        /**
         * Creates the datatype of the IRI over the enum type {@code user_literal_test.size}.
         *
         * @param typeIri the datatype IRI
         * @param name the name of the literal classes
         */
        SizeDatatype(Iri typeIri, String name)
        {
            super(typeIri, name, new UserType("user_literal_test.size"));
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
     * IRI of the datatype.
     */
    private static final Iri sizeIri = new Iri("http://example.org/size");

    /**
     * IRI of the second datatype over the same type.
     */
    private static final Iri size2Iri = new Iri("http://example.org/size2");

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the enum type and the table {@code item} of three rows holding the sizes medium, large and small in a
     * column of the type ({@code :direct}), in a column of the type with a lexical form column ({@code :base}, the
     * forms being canonical, {@code LARGE} and {@code Small}) and in a box column ({@code :boxed}, the same forms), and
     * maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema user_literal_test");
            statement.execute("create type user_literal_test.size as enum ('small', 'medium', 'large')");
            statement.execute("""
                    create table user_literal_test.item (iri varchar collate "C" primary key, \
                    direct user_literal_test.size not null, base_value user_literal_test.size not null, \
                    base_lexical varchar collate "C" not null, boxed sparql.rdfbox not null)""");
            statement.execute("""
                    insert into user_literal_test.item values \
                    ('http://example.org/item/1', 'medium', 'medium', '', \
                    sparql.rdfbox_create_from_userliteral('medium'::user_literal_test.size, \
                    'http://example.org/size')), \
                    ('http://example.org/item/2', 'large', 'large', 'LARGE', \
                    sparql.rdfbox_create_from_userliteral_with_lexical('large'::user_literal_test.size, \
                    'http://example.org/size', 'LARGE')), \
                    ('http://example.org/item/3', 'small', 'small', 'Small', \
                    sparql.rdfbox_create_from_userliteral_with_lexical('small'::user_literal_test.size, \
                    'http://example.org/size', 'Small'))""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");

        UserDatatype size = new SizeDatatype(sizeIri, "size");
        config.addDatatype(size);
        config.addDatatype(new SizeDatatype(size2Iri, "size2"));

        DatabaseTable table = new DatabaseTable("user_literal_test", "item");
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
     * Values of {@code ?v} in the order the query returns them.
     *
     * @param query the query, without the prefix declaration
     * @return values of {@code ?v} in the order the query returns them
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<RdfTerm> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<RdfTerm> values = new ArrayList<>();

            while(result.next())
                values.add(result.getRow()[0]);

            return values;
        }
    }


    /**
     * The literal of the datatype with the lexical form.
     *
     * @param lexical the lexical form
     * @return the literal of the datatype with the lexical form
     */
    private static TypedLiteral size(String lexical)
    {
        return new TypedLiteral(lexical, sizeIri);
    }


    @Test
    @DisplayName("literals in a column of the user type are ordered by the type, not by their text")
    void direct() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { ?s :direct ?v } ORDER BY ?v"),
                contains(size("small"), size("medium"), size("large")));
        assertThat(execute("SELECT ?v WHERE { ?s :direct ?v } ORDER BY DESC(?v)"),
                contains(size("large"), size("medium"), size("small")));
    }


    @Test
    @DisplayName("literals with a lexical form column and boxed literals are ordered the same way")
    void baseAndBoxed() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { ?s :base ?v } ORDER BY ?v"),
                contains(size("Small"), size("medium"), size("LARGE")));
        assertThat(execute("SELECT ?v WHERE { ?s :boxed ?v } ORDER BY ?v"),
                contains(size("Small"), size("medium"), size("LARGE")));
    }


    @Test
    @DisplayName("literals of the type column and of the lexical form column are ordered together")
    void directAndBase() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { { ?s :direct ?v } UNION { ?s :base ?v } } ORDER BY ?v"),
                contains(size("small"), size("Small"), size("medium"), size("medium"), size("large"), size("LARGE")));
        assertThat(execute("SELECT ?v WHERE { { ?s :direct ?v } UNION { ?s :base ?v } } ORDER BY DESC(?v)"),
                contains(size("LARGE"), size("large"), size("medium"), size("medium"), size("Small"), size("small")));
    }


    @Test
    @DisplayName("literals of two datatypes over one type are ordered by the value, then by the datatype")
    void twoDatatypes() throws Exception
    {
        assertThat(
                execute("SELECT ?v WHERE { VALUES ?v { \"large\"^^:size \"small\"^^:size2 \"small\"^^:size "
                        + "\"Large\"^^:size2 } } ORDER BY ?v"),
                contains(size("small"), new TypedLiteral("small", size2Iri), size("large"),
                        new TypedLiteral("Large", size2Iri)));
    }


    @Test
    @DisplayName("literals of all three storages are ordered together, the canonical forms first")
    void together() throws Exception
    {
        assertThat(
                execute("SELECT ?v WHERE { { ?s :direct ?v } UNION { ?s :base ?v } UNION { ?s :boxed ?v } } "
                        + "ORDER BY ?v"),
                contains(size("small"), size("Small"), size("Small"), size("medium"), size("medium"), size("medium"),
                        size("large"), size("LARGE"), size("LARGE")));
    }


    @Test
    @DisplayName("terms of other kinds next to a user literal are ordered by the box as a whole")
    void mixed() throws Exception
    {
        assertThat(
                execute("SELECT ?v WHERE { VALUES ?v { 1 \"a\" \"medium\"^^:size <http://example.org/i> true } } "
                        + "ORDER BY ?v"),
                contains(new Iri("http://example.org/i"), size("medium"), new TypedLiteral("a", xsdStringIri),
                        new TypedLiteral("1", xsdIntegerIri), new TypedLiteral("true", xsdBooleanIri)));
    }
}
