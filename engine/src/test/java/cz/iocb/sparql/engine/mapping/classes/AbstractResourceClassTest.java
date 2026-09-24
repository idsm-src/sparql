package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.ITable;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



/**
 * Base of the tests of one literal class. A subclass supplies the class and a table of lexical forms with the expected
 * constant columns (an empty list for invalid forms); the tests then check {@code match}, {@code toColumns} and, in the
 * database, that converting to every superclass and back yields the original columns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractResourceClassTest
{
    /**
     * Expected columns of an invalid lexical form.
     */
    protected static final List<String> invalid = List.of();

    /**
     * DbUnit connection to the test database.
     */
    static private IDatabaseConnection conn;

    /**
     * The tested class.
     */
    private LiteralClass literalClass;

    /**
     * Datatype IRI of the tested class.
     */
    private Iri iri;

    /**
     * Lexical forms with their expected constant columns (empty for invalid forms).
     */
    private Map<String, List<Column>> values;


    /**
     * Creates the test; {@code types} are the SQL types of the expected columns, {@code values} maps lexical forms to
     * the expected column values.
     */
    public AbstractResourceClassTest(LiteralClass literalClass, List<String> types, Map<String, List<String>> values)
    {
        this.literalClass = literalClass;
        this.iri = literalClass.getDatatype().getTypeIri();


        this.values = values.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> IntStream
                .range(0, e.getValue().size()).mapToObj(i -> constant(e.getValue().get(i), types.get(i))).toList()));
    }


    /**
     * Opens the DbUnit connection.
     */
    @BeforeAll
    static void init() throws DatabaseUnitException, SQLException
    {
        conn = new DatabaseConnection(Database.getPool().getConnection());
        conn.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
    }


    /**
     * Every lexical form with whether it is valid.
     */
    protected Stream<Arguments> toMatchArguments()
    {
        return values.entrySet().stream()
                .map(e -> Arguments.of(new TypedLiteral(e.getKey(), iri), !e.getValue().isEmpty()));
    }


    /**
     * The class matches exactly the valid lexical forms.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("toMatchArguments")
    void matchTest(Literal literal, boolean expected)
    {
        assertEquals(literalClass.match(null, literal), expected);
    }


    /**
     * The valid lexical forms with their expected columns.
     */
    protected Stream<Arguments> toColumnsArguments()
    {
        return values.entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .map(e -> Arguments.of(new TypedLiteral(e.getKey(), iri), e.getValue()));
    }


    /**
     * The class produces the expected constant columns.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("toColumnsArguments")
    void toColumnsTest(Literal literal, List<Column> expected)
    {
        assertEquals(expected, literalClass.toColumns(null, literal));
    }


    /**
     * Every superclass with every valid lexical form and all combinations of the conversion flags.
     */
    protected Stream<Arguments> toGeneralArguments()
    {
        List<Arguments> data = new ArrayList<>();

        for(PrimitiveResourceClass superClass : literalClass.getSuperClasses())
            for(Entry<String, List<Column>> e : values.entrySet())
                if(!e.getValue().isEmpty())
                    for(Boolean canBeNull : List.of(true, false))
                        for(Boolean checkOptional : List.of(true, false))
                            data.add(Arguments.of(superClass, e.getKey(), canBeNull, checkOptional));

        return data.stream();
    }


    /**
     * Converting the columns to the superclass and back gives the original values when evaluated by the database.
     */
    @ParameterizedTest(name = "{0}({1},{2},{3})")
    @MethodSource("toGeneralArguments")
    void classCastTest(ResourceClass superClass, String value, boolean canBeNull, boolean checkOptional)
            throws DatabaseUnitException, SQLException
    {
        List<Column> cols = literalClass.toColumns(null, new TypedLiteral(value, iri));
        List<Column> res1 = literalClass.toGeneralClass(superClass, cols, canBeNull);
        List<Column> res2 = literalClass.fromGeneralClass(superClass, res1, checkOptional);

        ITable t1 = conn.createQueryTable(null,
                cols.stream().map(c -> c.toString() + " AS c").collect(Collectors.joining(", ", "SELECT ", "")));

        ITable t2 = conn.createQueryTable(null,
                res2.stream().map(c -> c.toString() + " AS c").collect(Collectors.joining(", ", "SELECT ", "")));

        try
        {
            Assertion.assertEquals(t1, t2);
        }
        catch(Throwable e)
        {
            System.err.println(
                    cols.stream().map(c -> c.toString() + " AS c").collect(Collectors.joining(", ", "SELECT ", "")));

            System.err.println(
                    res2.stream().map(c -> c.toString() + " AS c").collect(Collectors.joining(", ", "SELECT ", "")));

            System.err.println();

            throw e;
        }
    }
}
