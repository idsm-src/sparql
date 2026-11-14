package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
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
 * The classes of the xsd:dateTime and xsd:date values of one timezone offset. The base class of an offset holds every
 * valid value of the offset, the canonical ones with an empty lexical form, so a value of the canonical class of the
 * offset, a canonical constant and a canonical value of the offset held in a class of all offsets are the terms the
 * base class holds: they are found by a pattern, sameTerm and VALUES, joined, united without duplicates, tested by
 * EXISTS, OPTIONAL and MINUS, and a function that takes its argument in the canonical class of an offset gets the value
 * from any of these classes. A value of another offset and a constant in another than the canonical form stay apart.
 *
 * <p>
 * Every datatype has a table per class, mapped by the predicate of the same name under the prefix of the datatype: the
 * base class of the offset 0 ({@code base}: the items 1 with a canonical value, 2 with the same value in another form
 * and 3 with another canonical value), the composite and the scalar base class of all offsets ({@code general} and
 * {@code scalarGeneral}: the items 11 and 21 with the canonical value, 12 and 22 with the value in the offset +01:00,
 * 13 and 23 with the other form of the value), the box ({@code box}: the items 31, 32 and 33 alike), the canonical
 * class of the offset 0 ({@code canonical}: the item 41) and the composite and the scalar canonical class of all
 * offsets ({@code composite} and {@code scalar}: the items 51 and 61 with the canonical value, 52 and 62 with the value
 * in the offset +01:00). The function {@code year} takes its argument in the canonical class of the offset 0.
 */
public class InZoneClassTest
{
    /**
     * A datatype with the classes of its values.
     */
    private enum Kind
    {
        /**
         * The xsd:dateTime datatype.
         */
        DATE_TIME(
                "datetime",
                xsdDateTimeIri,
                "timestamptz",
                "value at time zone 'UTC'",
                "sparql.zoneddatetime",
                "2020-01-01T10:00:00Z",
                "2020-01-01T10:00:00.0Z",
                "2020-01-01T11:00:00+01:00",
                "2021-06-01T10:00:00Z",
                "2020-01-01T10:00:00Z",
                "2021-06-01T10:00:00Z",
                DateTimeInZoneBaseClass.get(0),
                genDateTime,
                genScalarDateTime,
                DateTimeInZoneClass.get(0),
                xsdDateTime,
                xsdScalarDateTime,
                "xsd:date",
                new TypedLiteral("2020-01-01Z", xsdDateIri)),

        /**
         * The xsd:date datatype.
         */
        DATE(
                "date",
                xsdDateIri,
                "date",
                "value",
                "sparql.zoneddate",
                "2020-01-01Z",
                "2020-01-01+00:00",
                "2020-01-01+01:00",
                "2021-06-01Z",
                "2020-01-01",
                "2021-06-01",
                DateInZoneBaseClass.get(0),
                genDate,
                genScalarDate,
                DateInZoneClass.get(0),
                xsdDate,
                xsdScalarDate,
                "xsd:dateTime",
                new TypedLiteral("2020-01-01T00:00:00Z", xsdDateTimeIri));


        /**
         * Name of the datatype in the names of its tables, of its function and in the prefix of its predicates.
         */
        private final String name;

        /**
         * IRI of the datatype.
         */
        private final Iri datatype;

        /**
         * SQL type of a value without its offset.
         */
        private final String sqlType;

        /**
         * SQL expression of the argument {@code value} of that type to take the year of.
         */
        private final String sqlYearSource;

        /**
         * SQL type of a value with its offset.
         */
        private final String zonedType;

        /**
         * The canonical form of the value in the offset 0.
         */
        private final String canonical;

        /**
         * Another form of the same value in the offset 0.
         */
        private final String nonCanonical;

        /**
         * The canonical form of the value in the offset +01:00.
         */
        private final String shifted;

        /**
         * The canonical form of another value in the offset 0.
         */
        private final String another;

        /**
         * SQL constant of the value.
         */
        private final String sqlValue;

        /**
         * SQL constant of the other value.
         */
        private final String sqlAnother;

        /**
         * The base class of the offset 0.
         */
        private final ResourceClass baseClass;

        /**
         * The composite base class of all offsets.
         */
        private final ResourceClass generalClass;

        /**
         * The scalar base class of all offsets.
         */
        private final ResourceClass scalarGeneralClass;

        /**
         * The canonical class of the offset 0.
         */
        private final ResourceClass canonicalClass;

        /**
         * The composite canonical class of all offsets.
         */
        private final ResourceClass compositeClass;

        /**
         * The scalar canonical class of all offsets.
         */
        private final ResourceClass scalarClass;

        /**
         * The cast to the other datatype.
         */
        private final String cast;

        /**
         * The value cast to the other datatype.
         */
        private final TypedLiteral castValue;


        /**
         * Creates the kind.
         *
         * @param name name of the datatype in the names of its tables, function and prefix
         * @param datatype IRI of the datatype
         * @param sqlType SQL type of a value without its offset
         * @param sqlYearSource SQL expression of the argument {@code value} of that type to take the year of
         * @param zonedType SQL type of a value with its offset
         * @param canonical the canonical form of the value in the offset 0
         * @param nonCanonical another form of the same value in the offset 0
         * @param shifted the canonical form of the value in the offset +01:00
         * @param another the canonical form of another value in the offset 0
         * @param sqlValue SQL constant of the value
         * @param sqlAnother SQL constant of the other value
         * @param baseClass the base class of the offset 0
         * @param generalClass the composite base class of all offsets
         * @param scalarGeneralClass the scalar base class of all offsets
         * @param canonicalClass the canonical class of the offset 0
         * @param compositeClass the composite canonical class of all offsets
         * @param scalarClass the scalar canonical class of all offsets
         * @param cast the cast to the other datatype
         * @param castValue the value cast to the other datatype
         */
        private Kind(String name, Iri datatype, String sqlType, String sqlYearSource, String zonedType,
                String canonical, String nonCanonical, String shifted, String another, String sqlValue,
                String sqlAnother, ResourceClass baseClass, ResourceClass generalClass,
                ResourceClass scalarGeneralClass, ResourceClass canonicalClass, ResourceClass compositeClass,
                ResourceClass scalarClass, String cast, TypedLiteral castValue)
        {
            this.name = name;
            this.datatype = datatype;
            this.sqlType = sqlType;
            this.sqlYearSource = sqlYearSource;
            this.zonedType = zonedType;
            this.canonical = canonical;
            this.nonCanonical = nonCanonical;
            this.shifted = shifted;
            this.another = another;
            this.sqlValue = sqlValue;
            this.sqlAnother = sqlAnother;
            this.baseClass = baseClass;
            this.generalClass = generalClass;
            this.scalarGeneralClass = scalarGeneralClass;
            this.canonicalClass = canonicalClass;
            this.compositeClass = compositeClass;
            this.scalarClass = scalarClass;
            this.cast = cast;
            this.castValue = castValue;
        }


        /**
         * The literal of the datatype.
         *
         * @param form the lexical form
         * @return the literal of the datatype
         */
        private TypedLiteral literal(String form)
        {
            return new TypedLiteral(form, datatype);
        }


        /**
         * The literal of the datatype as written in a query.
         *
         * @param form the lexical form
         * @return the literal of the datatype as written in a query
         */
        private String constant(String form)
        {
            return "\"" + form + "\"^^<" + datatype.getValue() + ">";
        }
    }


    /**
     * The year of the value.
     */
    private static final TypedLiteral year = new TypedLiteral("2020", xsdIntegerIri);

    /**
     * The year of the other value.
     */
    private static final TypedLiteral anotherYear = new TypedLiteral("2021", xsdIntegerIri);

    /**
     * Engine of the configuration mapping the tables.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code in_zone_class_test} schema with the tables and the function of every datatype and the
     * configuration mapping them, see the comment of the class.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema in_zone_class_test");

            for(Kind kind : Kind.values())
            {
                String table = "in_zone_class_test." + kind.name;
                String value = "'" + kind.sqlValue + "'::" + kind.sqlType;
                String zoned = kind.zonedType + "_create(" + value + ", ";
                String type = "^^<" + kind.datatype.getValue() + ">'";

                statement.execute("create function " + table + "_year(value " + kind.sqlType + ") returns numeric "
                        + "language sql strict as $$ select extract(year from " + kind.sqlYearSource + ")::numeric $$");

                statement.execute("create table " + table + "_base (id int primary key, value " + kind.sqlType
                        + " not null, lexical varchar not null)");
                statement.execute("insert into " + table + "_base values (1, " + value + ", ''), (2, " + value + ", '"
                        + kind.nonCanonical + "'), (3, '" + kind.sqlAnother + "', '')");

                statement.execute("create table " + table + "_general (id int primary key, value " + kind.sqlType
                        + " not null, zone int4 not null, lexical varchar not null)");
                statement.execute("insert into " + table + "_general values (11, " + value + ", 0, ''), (12, " + value
                        + ", 3600, ''), (13, " + value + ", 0, '" + kind.nonCanonical + "')");

                statement.execute("create table " + table + "_scalar_general (id int primary key, value "
                        + kind.zonedType + " not null, lexical varchar not null)");
                statement.execute("insert into " + table + "_scalar_general values (21, " + zoned + "0), ''), (22, "
                        + zoned + "3600), ''), (23, " + zoned + "0), '" + kind.nonCanonical + "')");

                statement.execute("create table " + table + "_box (id int primary key, value sparql.rdfbox not null)");
                statement.execute(
                        "insert into " + table + "_box values (31, '\"" + kind.canonical + "\"" + type + "), (32, '\""
                                + kind.shifted + "\"" + type + "), (33, '\"" + kind.nonCanonical + "\"" + type + ")");

                statement.execute("create table " + table + "_canonical (id int primary key, value " + kind.sqlType
                        + " not null)");
                statement.execute("insert into " + table + "_canonical values (41, " + value + ")");

                statement.execute("create table " + table + "_composite (id int primary key, value " + kind.sqlType
                        + " not null, zone int4 not null)");
                statement.execute(
                        "insert into " + table + "_composite values (51, " + value + ", 0), (52, " + value + ", 3600)");

                statement.execute("create table " + table + "_scalar (id int primary key, value " + kind.zonedType
                        + " not null)");
                statement.execute(
                        "insert into " + table + "_scalar values (61, " + zoned + "0)), (62, " + zoned + "3600))");
            }
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        for(Kind kind : Kind.values())
        {
            map(config, graph, kind, "base", "base", kind.baseClass, "value", "lexical");
            map(config, graph, kind, "general", "general", kind.generalClass, "value", "zone", "lexical");
            map(config, graph, kind, "scalar_general", "scalarGeneral", kind.scalarGeneralClass, "value", "lexical");
            map(config, graph, kind, "box", "box", box, "value");
            map(config, graph, kind, "canonical", "canonical", kind.canonicalClass, "value");
            map(config, graph, kind, "composite", "composite", kind.compositeClass, "value", "zone");
            map(config, graph, kind, "scalar", "scalar", kind.scalarClass, "value");

            config.addFunction(new FunctionDefinition("http://example.org/" + kind.name + "/year",
                    new Function("in_zone_class_test", kind.name + "_year"), xsdInteger, List.of(kind.canonicalClass),
                    true, true));
        }

        engine = new Engine(config);
    }


    /**
     * Maps the rows of a table of the datatype as triples of the items, the predicate and the values of the class.
     *
     * @param config the configuration
     * @param graph the graph of the triples
     * @param kind the datatype
     * @param table name of the table without the name of the datatype
     * @param predicate local name of the predicate
     * @param resClass class of the values
     * @param columns columns of the values
     */
    private static void map(SparqlDatabaseConfiguration config, ConstantIriMapping graph, Kind kind, String table,
            String predicate, ResourceClass resClass, String... columns)
    {
        config.addQuadMapping(new DatabaseTable("in_zone_class_test", kind.name + "_" + table), graph,
                config.createIriMapping("item", "id"),
                config.createIriMapping("<http://example.org/" + kind.name + "/" + predicate + ">"),
                config.createTermMapping(resClass, columns));
    }


    /**
     * Rows of the query, in which the placeholders {@code $C}, {@code $N} and {@code $S} stand for the value of the
     * datatype in the canonical form, in another form and in the offset +01:00.
     *
     * @param kind the datatype
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(Kind kind, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        String text = "PREFIX : <http://example.org/" + kind.name
                + "/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
                + query.replace("$C", kind.constant(kind.canonical)).replace("$N", kind.constant(kind.nonCanonical))
                        .replace("$S", kind.constant(kind.shifted));

        try(Request request = engine.getRequest(); Result result = request.execute(text))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Rows of the items whose value by the predicate satisfies the condition on the variable {@code ?d}.
     *
     * @param kind the datatype
     * @param predicate local name of the predicate
     * @param condition the rest of the group graph pattern
     * @return rows of the items
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> select(Kind kind, String predicate, String condition)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        return execute(kind, "SELECT ?s WHERE { ?s :" + predicate + " ?d " + condition + " }");
    }


    /**
     * The item.
     *
     * @param id identifier of the item
     * @return the item
     */
    private static RdfTerm item(int id)
    {
        return new Iri("http://example.org/item/" + id);
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


    /**
     * Row of the items.
     *
     * @param ids identifiers of the items
     * @return row of the items
     */
    private static List<RdfTerm> items(int... ids)
    {
        return Arrays.stream(ids).mapToObj(InZoneClassTest::item).toList();
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("a canonical constant is a term of the base class")
    void constants(Kind kind) throws Exception
    {
        assertThat(execute(kind, "SELECT ?s WHERE { ?s :base $C }"), contains(items(1)));
        assertThat(execute(kind, "SELECT ?s WHERE { ?s :base $N }"), contains(items(2)));
        assertThat(execute(kind, "SELECT ?s WHERE { ?s :base $S }"), empty());

        assertThat(select(kind, "base", "FILTER(sameTerm(?d, $C))"), contains(items(1)));
        assertThat(select(kind, "base", "FILTER(sameTerm(?d, $N))"), contains(items(2)));
        assertThat(select(kind, "base", "FILTER(sameTerm(?d, $S))"), empty());
        assertThat(select(kind, "base", "FILTER(?d = $C)"), containsInAnyOrder(items(1), items(2)));

        assertThat(execute(kind, "SELECT ?s ?d WHERE { VALUES ?d { $C $N $S } ?s :base ?d }"), containsInAnyOrder(
                row(item(1), kind.literal(kind.canonical)), row(item(2), kind.literal(kind.nonCanonical))));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("the base class is joined with every class holding its terms")
    void joins(Kind kind) throws Exception
    {
        String query = "SELECT ?s ?t WHERE { ?s :base ?d . ?t :%s ?d }";

        assertThat(execute(kind, query.formatted("canonical")), contains(items(1, 41)));
        assertThat(execute(kind, query.formatted("composite")), contains(items(1, 51)));
        assertThat(execute(kind, query.formatted("scalar")), contains(items(1, 61)));
        assertThat(execute(kind, query.formatted("general")), containsInAnyOrder(items(1, 11), items(2, 13)));
        assertThat(execute(kind, query.formatted("scalarGeneral")), containsInAnyOrder(items(1, 21), items(2, 23)));
        assertThat(execute(kind, query.formatted("box")), containsInAnyOrder(items(1, 31), items(2, 33)));

        // the union of the base class and the canonical class of its offset is held in the base class
        assertThat(execute(kind, "SELECT ?s ?t WHERE { { ?s :base ?d } UNION { ?s :canonical ?d } ?t :composite ?d }"),
                containsInAnyOrder(items(1, 51), items(41, 51)));
        assertThat(execute(kind, "SELECT ?s ?t WHERE { { ?s :base ?d } UNION { ?s :composite ?d } ?t :canonical ?d }"),
                containsInAnyOrder(items(1, 41), items(51, 41)));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("a union of the base class and a canonical class holds a term once")
    void unions(Kind kind) throws Exception
    {
        RdfTerm canonical = kind.literal(kind.canonical);
        RdfTerm nonCanonical = kind.literal(kind.nonCanonical);
        RdfTerm shifted = kind.literal(kind.shifted);
        RdfTerm another = kind.literal(kind.another);

        String query = "SELECT DISTINCT ?d WHERE { { ?s :base ?d } UNION { ?s :%s ?d } }";

        assertThat(execute(kind, query.formatted("canonical")),
                containsInAnyOrder(row(canonical), row(nonCanonical), row(another)));
        assertThat(execute(kind, query.formatted("composite")),
                containsInAnyOrder(row(canonical), row(nonCanonical), row(another), row(shifted)));
        assertThat(execute(kind, query.formatted("scalar")),
                containsInAnyOrder(row(canonical), row(nonCanonical), row(another), row(shifted)));
        assertThat(execute(kind, query.formatted("general")),
                containsInAnyOrder(row(canonical), row(nonCanonical), row(another), row(shifted)));

        assertThat(
                execute(kind,
                        "SELECT ?d (COUNT(*) AS ?c) WHERE { { ?s :base ?d } UNION { ?s :canonical ?d } } GROUP BY ?d"),
                containsInAnyOrder(row(canonical, new TypedLiteral("2", xsdIntegerIri)),
                        row(nonCanonical, new TypedLiteral("1", xsdIntegerIri)),
                        row(another, new TypedLiteral("1", xsdIntegerIri))));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("a term of the current solution is looked up in the base class")
    void exists(Kind kind) throws Exception
    {
        String lookup = "FILTER EXISTS { SELECT ?t WHERE { ?t :%s ?d } LIMIT 1 }";

        assertThat(select(kind, "canonical", lookup.formatted("base")), contains(items(41)));
        assertThat(select(kind, "composite", lookup.formatted("base")), contains(items(51)));
        assertThat(select(kind, "scalar", lookup.formatted("base")), contains(items(61)));
        assertThat(select(kind, "general", lookup.formatted("base")), containsInAnyOrder(items(11), items(13)));
        assertThat(select(kind, "scalarGeneral", lookup.formatted("base")), containsInAnyOrder(items(21), items(23)));
        assertThat(select(kind, "box", lookup.formatted("base")), containsInAnyOrder(items(31), items(33)));

        assertThat(select(kind, "base", lookup.formatted("canonical")), contains(items(1)));
        assertThat(select(kind, "base", lookup.formatted("composite")), contains(items(1)));
        assertThat(select(kind, "base", lookup.formatted("general")), containsInAnyOrder(items(1), items(2)));
        assertThat(select(kind, "base", lookup.formatted("box")), containsInAnyOrder(items(1), items(2)));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("OPTIONAL and MINUS compare the base class with the canonical classes")
    void optionalAndMinus(Kind kind) throws Exception
    {
        String optional = "SELECT ?s ?t WHERE { ?s :base ?d OPTIONAL { ?t :%s ?d } }";

        assertThat(execute(kind, optional.formatted("canonical")),
                containsInAnyOrder(items(1, 41), row(item(2), null), row(item(3), null)));
        assertThat(execute(kind, optional.formatted("composite")),
                containsInAnyOrder(items(1, 51), row(item(2), null), row(item(3), null)));

        assertThat(select(kind, "base", "MINUS { ?t :canonical ?d }"), containsInAnyOrder(items(2), items(3)));
        assertThat(select(kind, "base", "MINUS { ?t :scalar ?d }"), containsInAnyOrder(items(2), items(3)));
        assertThat(select(kind, "canonical", "MINUS { ?t :base ?d }"), empty());
        assertThat(select(kind, "composite", "MINUS { ?t :base ?d }"), contains(items(52)));
        assertThat(select(kind, "general", "MINUS { ?t :base ?d }"), contains(items(12)));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("a function taking a value of an offset takes it from every class holding it")
    void functionArgument(Kind kind) throws Exception
    {
        String query = "SELECT ?s ?y WHERE { ?s :%s ?d BIND(:year(?d) AS ?y) }";

        assertThat(execute(kind, query.formatted("canonical")), contains(row(item(41), year)));
        assertThat(execute(kind, query.formatted("base")),
                containsInAnyOrder(row(item(1), year), row(item(2), year), row(item(3), anotherYear)));
        assertThat(execute(kind, query.formatted("composite")),
                containsInAnyOrder(row(item(51), year), row(item(52), null)));
        assertThat(execute(kind, query.formatted("scalar")),
                containsInAnyOrder(row(item(61), year), row(item(62), null)));
        assertThat(execute(kind, query.formatted("general")),
                containsInAnyOrder(row(item(11), year), row(item(12), null), row(item(13), year)));
        assertThat(execute(kind, query.formatted("scalarGeneral")),
                containsInAnyOrder(row(item(21), year), row(item(22), null), row(item(23), year)));
        assertThat(execute(kind, query.formatted("box")),
                containsInAnyOrder(row(item(31), year), row(item(32), null), row(item(33), year)));

        assertThat(execute(kind, "SELECT (:year($C) AS ?c) (:year($N) AS ?n) (:year($S) AS ?s) WHERE { }"),
                contains(row(year, year, null)));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind.class)
    @DisplayName("a constant in another than the canonical form is a term of no canonical class")
    void nonCanonicalConstant(Kind kind) throws Exception
    {
        assertThat(select(kind, "canonical", "FILTER(sameTerm(?d, $N))"), empty());
        assertThat(select(kind, "composite", "FILTER(sameTerm(?d, $N))"), empty());
        assertThat(select(kind, "scalar", "FILTER(sameTerm(?d, $N))"), empty());
        assertThat(select(kind, "general", "FILTER(sameTerm(?d, $N))"), contains(items(13)));
        assertThat(select(kind, "scalarGeneral", "FILTER(sameTerm(?d, $N))"), contains(items(23)));
        assertThat(select(kind, "box", "FILTER(sameTerm(?d, $N))"), contains(items(33)));

        assertThat(execute(kind, "SELECT ?s WHERE { VALUES ?d { $C $N } ?s :composite ?d }"), contains(items(51)));
        assertThat(execute(kind, "SELECT ?s WHERE { VALUES ?d { $C $N } ?s :general ?d }"),
                containsInAnyOrder(items(11), items(13)));

        // the value of the constant is the value of its offset
        assertThat(execute(kind, "SELECT (STR($N) AS ?l) (" + kind.cast + "($N) AS ?v) (TZ($N) AS ?z) WHERE { }"),
                contains(row(new TypedLiteral(kind.nonCanonical, xsdStringIri), kind.castValue,
                        new TypedLiteral("Z", xsdStringIri))));
        assertThat(
                execute(kind,
                        "SELECT ?v (" + kind.cast + "(?v) AS ?c) WHERE { { ?s :general ?v } UNION { VALUES ?v { $N } } "
                                + "FILTER(sameTerm(?v, $N)) }"),
                contains(row(kind.literal(kind.nonCanonical), kind.castValue),
                        row(kind.literal(kind.nonCanonical), kind.castValue)));
    }
}
