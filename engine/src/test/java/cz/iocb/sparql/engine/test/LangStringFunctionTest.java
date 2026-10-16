package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * String functions over a language-tagged string whose tag comes from a column of its own (the general language string
 * class of two columns): the functions preserving the string type keep the tag column next to the new value.
 */
public class LangStringFunctionTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code lang_string_test} schema with the table {@code item} of one labelled IRI and maps it, the
     * label getting its language tag from a column.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema lang_string_test");
            statement.execute("create table lang_string_test.item (iri varchar collate \"C\" primary key, "
                    + "label varchar collate \"C\" not null, lang varchar collate \"C\" not null)");
            statement.execute("insert into lang_string_test.item values ('http://example.org/item/1', 'banana', 'en')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        DatabaseTable table = new DatabaseTable("lang_string_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping(BuiltinClasses.iri, "iri"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(rdfLangString, "label", "lang"));

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
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    @Test
    @DisplayName("REPLACE keeps the language tag of the argument")
    void replace() throws Exception
    {
        assertThat(execute("SELECT ?r WHERE { ?x ex:label ?l BIND(REPLACE(?l, \"a\", \"o\") AS ?r) }"),
                contains(row(new LangStringLiteral("bonono", "en"))));
        assertThat(execute("SELECT ?r WHERE { ?x ex:label ?l BIND(REPLACE(?l, \"A\", \"o\", \"i\") AS ?r) }"),
                contains(row(new LangStringLiteral("bonono", "en"))));
    }


    @Test
    @DisplayName("UCASE, SUBSTR and STRLEN of a language-tagged string with a tag column")
    void otherFunctions() throws Exception
    {
        assertThat(execute("SELECT ?r WHERE { ?x ex:label ?l BIND(UCASE(?l) AS ?r) }"),
                contains(row(new LangStringLiteral("BANANA", "en"))));
        assertThat(execute("SELECT ?r WHERE { ?x ex:label ?l BIND(SUBSTR(?l, 2, 3) AS ?r) }"),
                contains(row(new LangStringLiteral("ana", "en"))));
        assertThat(execute("SELECT ?r WHERE { ?x ex:label ?l BIND(STRLEN(?l) AS ?r) }"),
                contains(row(new TypedLiteral("6", xsdIntegerIri))));
    }
}
