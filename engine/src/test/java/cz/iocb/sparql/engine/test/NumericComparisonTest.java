package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
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
 * Numerics of different datatypes are compared in the least datatype every operand is derived from or promoted to (the
 * hierarchy of XML Schema), so that the operands are taken in it as XPath takes the arguments of a function: an
 * unsigned datatype meets a signed one of a bigger size in xsd:integer, not in the SQL type holding both, also when the
 * operand holds values of several datatypes in a box.
 */
public class NumericComparisonTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Configuration without mappings.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the configuration.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();
        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * Literal of the datatype in the given lexical form.
     */
    private static TypedLiteral literal(String value, Iri datatype)
    {
        return new TypedLiteral(value, datatype);
    }


    @Test
    @DisplayName("an unsigned datatype is compared with a signed one by value")
    void mixedDatatypes() throws Exception
    {
        String query = """
                SELECT ?a ?b WHERE { VALUES (?a ?b) { \
                ('200'^^xsd:unsignedByte '100'^^xsd:short) ('-1'^^xsd:short '0'^^xsd:unsignedByte) \
                ('65535'^^xsd:unsignedShort '1'^^xsd:int) ('4294967295'^^xsd:unsignedInt '1'^^xsd:long) \
                ('1'^^xsd:unsignedByte '2'^^xsd:short) } FILTER(?a > ?b) }""";

        assertThat(execute(query),
                containsInAnyOrder(row(literal("200", xsdUnsignedByteIri), literal("100", xsdShortIri)),
                        row(literal("65535", xsdUnsignedShortIri), literal("1", xsdIntIri)),
                        row(literal("4294967295", xsdUnsignedIntIri), literal("1", xsdLongIri))));
    }


    @Test
    @DisplayName("a box holding an unsigned or a signed datatype is compared with a signed one by value")
    void boxedOperand() throws Exception
    {
        // the branches of different classes make the value of IF a box
        String query = "SELECT ?c WHERE { VALUES ?c { true false } "
                + "BIND(IF(?c, '1'^^xsd:unsignedByte, '2'^^xsd:short) AS ?x) FILTER(?x < '3'^^xsd:short) }";

        assertThat(execute(query),
                containsInAnyOrder(row(literal("true", xsdBooleanIri)), row(literal("false", xsdBooleanIri))));
    }
}
