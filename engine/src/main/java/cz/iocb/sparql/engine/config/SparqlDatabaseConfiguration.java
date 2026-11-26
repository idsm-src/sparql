package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfDirLangStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.common.IriResolver;
import cz.iocb.sparql.engine.database.CatalogStatistics;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.DefaultStatistics;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.ConstantMapping;
import cz.iocb.sparql.engine.mapping.ConstantTripleTermMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedLiteralMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedTermMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.EnumUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.parser.Rdf;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.IriCache;
import info.adams.ryu.RyuDouble;
import info.adams.ryu.RyuFloat;



/**
 * Describes one SPARQL endpoint deployment: prefixes, datatypes, user IRI classes, virtual tables, quad mappings of the
 * database and virtual tables, procedures, extension functions and federated services. Deployments subclass it and
 * register their definitions in the constructor; the endpoint instantiates it through JNDI, which requires a
 * {@code (String service, DataSource, DatabaseSchema)} constructor.
 */
public class SparqlDatabaseConfiguration implements ClassRelations
{
    /**
     * Namespace of the terms of the SPARQL service description.
     */
    private static final String serviceDescriptionNamespace = "http://www.w3.org/ns/sparql-service-description#";

    /**
     * Namespace of the entailment regimes.
     */
    private static final String entailmentNamespace = "http://www.w3.org/ns/entailment/";

    /**
     * Namespace of the formats of the results.
     */
    private static final String formatNamespace = "http://www.w3.org/ns/formats/";

    /**
     * Namespace of the versions of SPARQL.
     */
    private static final String versionNamespace = "http://www.w3.org/ns/sparql#";

    /**
     * IRI of this endpoint's service, or null.
     */
    protected final Iri serviceIri;

    /**
     * IRI of the graph holding the service description, or null.
     */
    protected final Iri descriptionGraphIri;

    /**
     * Catalog facts of the target database.
     */
    protected DatabaseSchema databaseSchema;

    /**
     * Connection pool of the target database.
     */
    protected DataSource connectionPool;

    /**
     * Whether named-graph mappings are also added to the default graph.
     */
    protected boolean autoAddToDefaultGraph;

    /**
     * Prefixes by name.
     */
    protected Map<String, String> prefixes = new HashMap<>();

    /**
     * Datatypes by IRI.
     */
    protected Map<Iri, Datatype> dataTypeMap = new HashMap<>();

    /**
     * User IRI classes ordered by check cost.
     */
    protected List<UserIriClass> iriClasses = new ArrayList<>();

    /**
     * User IRI classes by name.
     */
    protected Map<String, UserIriClass> iriClassMap = new HashMap<>();

    /**
     * Pairs of registered user IRI classes declared to share IRIs although neither is a subclass of the other
     * ({@link #addIriClassOverlap}), as a symmetric adjacency: the classes each class may overlap with.
     */
    protected Map<UserIriClass, Set<UserIriClass>> iriClassOverlaps = new HashMap<>();

    /**
     * This service followed by the imported services.
     */
    private final List<Iri> services = new ArrayList<>();

    /**
     * Quad mappings per service.
     */
    protected Map<Iri, List<QuadMapping>> mappings = new HashMap<>();

    /**
     * Procedures per service, by IRI.
     */
    protected Map<Iri, Map<String, ProcedureDefinition>> procedures = new HashMap<>();

    /**
     * Extension functions per service, by IRI.
     */
    protected Map<Iri, Map<String, FunctionDefinition>> functions = new HashMap<>();

    /**
     * Definitions of the virtual tables the mappings may read, by table, in registration order.
     */
    protected Map<VirtualTable, VirtualTableDefinition> virtualTables = new LinkedHashMap<>();

    /**
     * Cache of IRI class detections shared by all requests.
     */
    protected final IriCache iriCache = new IriCache(10000);

    /**
     * Statistics of the tables the mappings read, by table: those registered by the deployment and those read from the
     * catalog on first use (see {@link #getTableStatistics}).
     */
    protected final Map<SourceTable, TableStatistics> tableStatistics = new ConcurrentHashMap<>();

    /**
     * Function through which the extended statistics of the tables are read, or null (see
     * {@link #setExtendedStatisticsFunction}).
     */
    protected Function extendedStatisticsFunction = null;

    /**
     * Tables whose rows the translation of a query may read (see {@link #addDictionaryTable}).
     */
    protected final Set<SourceTable> dictionaryTables = new HashSet<>();

    /**
     * Tables whose accesses the translation of a query asks for a row (see {@link #addProbedTable}).
     */
    protected final Set<SourceTable> probedTables = new HashSet<>();

    /**
     * Whether the engine chooses the order and the methods of the joins it generates (see {@link #setJoinOptimizer}).
     */
    protected boolean joinOptimizer = false;

    /**
     * Whether the planner is made to keep the joins the join optimizer chooses by plan advice (see
     * {@link #setPlanAdvice}).
     */
    protected boolean planAdvice = false;

    /**
     * Settings of the calls of the remote SERVICE endpoints that have no settings of their own.
     */
    protected ServiceSettings serviceSettings = ServiceSettings.DEFAULT;

    /**
     * Settings of the calls of particular remote SERVICE endpoints, by endpoint IRI.
     */
    protected Map<Iri, ServiceSettings> endpointSettings = new HashMap<>();


    /**
     * Creates a configuration with the built-in datatypes and no mappings.
     *
     * @param service IRI of the endpoint's own service, or null
     * @param descriptionGraph IRI of the graph holding the service description; defaults to
     *            {@code service#ServiceDescription}
     * @param connectionPool the connection pool of the database
     * @param schema the database schema; it is copied, so the facts about the virtual tables of this configuration do
     *            not leak into the original
     * @param autoAddToDefaultGraph if true, every mapping registered for a named graph is also added to the default
     *            graph (union default graph)
     *
     * @throws SQLException on database errors
     */
    public SparqlDatabaseConfiguration(String service, String descriptionGraph, DataSource connectionPool,
            DatabaseSchema schema, boolean autoAddToDefaultGraph) throws SQLException
    {
        Iri serviceIri = service != null ? new Iri(service) : null;
        Iri descriptionGraphIri = descriptionGraph != null ? new Iri(descriptionGraph) :
                service != null ? new Iri(service + "#ServiceDescription") : null;

        this.serviceIri = serviceIri;
        this.descriptionGraphIri = descriptionGraphIri;
        this.connectionPool = connectionPool;
        this.databaseSchema = schema != null ? new DatabaseSchema(schema) : null;
        this.autoAddToDefaultGraph = autoAddToDefaultGraph;

        addEmptyService(serviceIri);

        addDatatype(xsdBooleanType);
        addDatatype(xsdByteType);
        addDatatype(xsdUnsignedByteType);
        addDatatype(xsdShortType);
        addDatatype(xsdUnsignedShortType);
        addDatatype(xsdIntType);
        addDatatype(xsdUnsignedIntType);
        addDatatype(xsdLongType);
        addDatatype(xsdUnsignedLongType);
        addDatatype(xsdIntegerType);
        addDatatype(xsdNonPositiveIntegerType);
        addDatatype(xsdNegativeIntegerType);
        addDatatype(xsdNonNegativeIntegerType);
        addDatatype(xsdPositiveIntegerType);
        addDatatype(xsdDecimalType);
        addDatatype(xsdFloatType);
        addDatatype(xsdDoubleType);
        addDatatype(xsdDateTimeType);
        addDatatype(xsdDateType);
        addDatatype(xsdDayTimeDurationType);
        addDatatype(xsdStringType);
        addDatatype(rdfLangStringType);
        addDatatype(rdfDirLangStringType);
    }


    /**
     * Creates a configuration with a union default graph.
     *
     * @param service the service IRI
     * @param descriptionGraph IRI of the service description graph, or null for the default
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @throws SQLException on database errors
     */
    public SparqlDatabaseConfiguration(String service, String descriptionGraph, DataSource connectionPool,
            DatabaseSchema schema) throws SQLException
    {
        this(service, descriptionGraph, connectionPool, schema, true);
    }


    /**
     * Creates a configuration whose description graph defaults to {@code service#ServiceDescription}.
     *
     * @param service the service IRI
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @param autoAddToDefaultGraph whether named-graph mappings are also added to the default graph
     * @throws SQLException on database errors
     */
    public SparqlDatabaseConfiguration(String service, DataSource connectionPool, DatabaseSchema schema,
            boolean autoAddToDefaultGraph) throws SQLException
    {
        this(service, null, connectionPool, schema, autoAddToDefaultGraph);
    }


    /**
     * JNDI-compatible constructor: union default graph and default description graph.
     *
     * @param service the service IRI
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @throws SQLException on database errors
     */
    protected SparqlDatabaseConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        this(service, connectionPool, schema, true);
    }


    /**
     * Registers a prefix available to queries and to {@link #createIriMapping(String)}; redefining it with a different
     * IRI is an error.
     *
     * @param prefix the prefix
     * @param iri IRI of the service
     */
    public void addPrefix(String prefix, String iri)
    {
        String previous = prefixes.get(prefix);

        if(previous == null)
            prefixes.put(prefix, iri);
        else if(!previous.equals(iri))
            throw new IllegalArgumentException(
                    "prefix definition conflict: " + prefix + ": " + iri + " != " + previous);
    }


    /**
     * Registers a datatype; redefining its IRI with a different datatype is an error.
     *
     * @param dataType the datatype
     */
    public void addDatatype(Datatype dataType)
    {
        Datatype previous = dataTypeMap.get(dataType.getTypeIri());

        if(previous == null)
        {
            dataTypeMap.put(dataType.getTypeIri(), dataType);
        }
        else if(!previous.equals(dataType))
        {
            throw new IllegalArgumentException("datatype definition conflict for '" + dataType.getTypeIri() + "'");
        }
    }


    /**
     * Registers a user IRI class. Two registered classes that are neither equal nor related (one a subclass of the
     * other) are taken as disjoint, which lets the translator prune joins and comparisons between them, and lets the
     * detection of the class of an IRI ({@link cz.iocb.sparql.engine.request.Request#getIriClass}) skip the classes
     * disjoint with a class already found; classes that do share IRIs are declared by
     * {@link #addIriClassOverlap(UserIriClass, UserIriClass)}. Classes are kept ordered by
     * {@link UserIriClass#getCheckCost}, so the detection tries the cheap ones first. The database has to compile the
     * regular expressions a new class hands to the extension (see {@link #checkSqlRegexps}).
     *
     * @param iriClass the user IRI class
     * @throws IllegalArgumentException if a different class is already registered under the same name, or if the
     *             extension does not compile a regular expression of the class
     * @throws SQLRuntimeException on database errors
     */
    public void addIriClass(UserIriClass iriClass)
    {
        addIriClass(iriClass, true);
    }


    /**
     * Registers a user IRI class (see {@link #addIriClass(UserIriClass)}), checking its regular expressions only when
     * asked to, as a class registered by another configuration over the same database has been checked already.
     *
     * @param iriClass the user IRI class
     * @param check whether the regular expressions of a new class are checked
     * @throws IllegalArgumentException if a different class is already registered under the same name, or if the
     *             extension does not compile a regular expression of the class
     * @throws SQLRuntimeException on database errors
     */
    private void addIriClass(UserIriClass iriClass, boolean check)
    {
        UserIriClass previous = iriClassMap.get(iriClass.getResourceName());

        if(previous == null)
        {
            if(check)
                checkSqlRegexps(iriClass);

            int possition = (int) iriClasses.stream().filter(c -> c.getCheckCost() <= iriClass.getCheckCost()).count();
            iriClasses.add(possition, iriClass);
            iriClassMap.put(iriClass.getResourceName(), iriClass);
        }
        else if(!previous.equals(iriClass))
        {
            throw new IllegalArgumentException(
                    "resource class definition conflict for iri class '" + iriClass.getResourceName() + "'");
        }
    }


    /**
     * Checks that the extension compiles every regular expression the class hands to it (see
     * {@link UserIriClass#getSqlRegexps}): {@code sparql.regex_string} returns NULL for an expression PCRE2 does not
     * compile, which would turn every conversion to the class with the representability check into an unbound value.
     * What PCRE2 compiles depends on its version, so the database of the deployment is asked; without a connection pool
     * nothing is checked.
     *
     * @param iriClass the user IRI class
     * @throws IllegalArgumentException if the extension does not compile a regular expression of the class
     * @throws SQLRuntimeException on database errors
     */
    private void checkSqlRegexps(UserIriClass iriClass)
    {
        List<String> regexps = iriClass.getSqlRegexps();

        if(connectionPool == null || regexps.isEmpty())
            return;

        try(Connection connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT sparql.regex_string('', ?)"))
        {
            for(String regexp : regexps)
            {
                statement.setString(1, regexp);

                try(ResultSet result = statement.executeQuery())
                {
                    if(!result.next() || result.getObject(1) == null)
                        throw new IllegalArgumentException("the extension does not compile the regular expression '"
                                + regexp + "' of iri class '" + iriClass.getResourceName() + "'");
                }
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    /**
     * Declares that two registered user IRI classes, neither of which is a subclass of the other, may share IRIs. The
     * declaration concerns exactly these two classes and does not extend to their subclasses, which stay disjoint with
     * the other class unless declared too. It belongs to the configuration, as whether two classes overlap depends on
     * the classes registered together, and the configuration supplies it to the class algebra as the
     * {@link ClassRelations}.
     *
     * @param a one user IRI class
     * @param b the other user IRI class
     * @throws IllegalArgumentException if a class is not registered, or the classes are equal or related
     */
    public void addIriClassOverlap(UserIriClass a, UserIriClass b)
    {
        for(UserIriClass iriClass : List.of(a, b))
            if(!iriClass.equals(iriClassMap.get(iriClass.getResourceName())))
                throw new IllegalArgumentException("iri class '" + iriClass.getResourceName() + "' is not registered");

        if(a.isSubclassOf(b) || b.isSubclassOf(a))
            throw new IllegalArgumentException("iri classes '" + a.getResourceName() + "' and '" + b.getResourceName()
                    + "' are equal or related, so they overlap anyway");

        iriClassOverlaps.computeIfAbsent(a, _ -> new HashSet<>()).add(b);
        iriClassOverlaps.computeIfAbsent(b, _ -> new HashSet<>()).add(a);
    }


    /**
     * True if the two user IRI classes, which are neither equal nor related, are declared to share IRIs by
     * {@link #addIriClassOverlap(UserIriClass, UserIriClass)}. The configuration is passed as the
     * {@link ClassRelations} to the class algebra wherever disjointness is decided.
     */
    @Override
    public boolean mayOverlap(UserIriClass a, UserIriClass b)
    {
        Set<UserIriClass> overlaps = iriClassOverlaps.get(a);

        return overlaps != null && overlaps.contains(b);
    }


    /**
     * Registers a virtual table: a table computed by an SQL query that quad mappings may read like a database table.
     * Every generated statement reading the table declares it in a {@code WITH} clause, and the facts stated by the
     * definition (nullable columns, keys, foreign keys, unjoinable columns) are merged into the database schema for the
     * optimiser. Registering the same table again with an equal definition is ignored.
     *
     * @param table the virtual table
     * @param definition the definition of the virtual table
     * @throws IllegalArgumentException if the table is already registered with a different definition
     * @throws IllegalStateException if the configuration has no database schema
     */
    public void addVirtualTable(VirtualTable table, VirtualTableDefinition definition)
    {
        checkSchema("a virtual table");

        VirtualTableDefinition previous = virtualTables.get(table);

        if(previous == null)
        {
            virtualTables.put(table, definition);
            databaseSchema.addVirtualTable(table, definition);
        }
        else if(!previous.equals(definition))
        {
            throw new IllegalArgumentException("virtual table definition conflict for '" + table.getName() + "'");
        }
    }


    /**
     * Checks that every virtual table among the given tables is registered.
     *
     * @param tables the tables of a quad mapping
     * @throws IllegalArgumentException if a virtual table is not registered
     */
    private void checkVirtualTables(List<SourceTable> tables)
    {
        for(SourceTable table : tables)
            if(table instanceof VirtualTable virtual && !virtualTables.containsKey(virtual))
                throw new IllegalArgumentException("virtual table '" + table.getName() + "' is not defined");
    }


    /**
     * Column-based IRI mapping of the class over the given column specifications (see
     * {@link #getColumns(ResourceClass, String...)}).
     *
     * @param iriClass the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the class over the given column specifications (see
     *         {@link #getColumns(ResourceClass, String...)})
     */
    public TermMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, getColumns(iriClass, columns));
    }


    /**
     * Column-based IRI mapping of the class over the given columns.
     *
     * @param iriClass the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the class over the given columns
     */
    public TermMapping createIriMapping(IriClass iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(iriClass, columns);
    }


    /**
     * Column-based IRI mapping of the named class over the given columns.
     *
     * @param iriClass the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the named class over the given columns
     */
    public TermMapping createIriMapping(String iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClass), columns);
    }


    /**
     * Column-based IRI mapping of the named class over the given column specifications (see
     * {@link #getColumns(ResourceClass, String...)}).
     *
     * @param iriClassName name of the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the named class over the given column specifications (see
     *         {@link #getColumns(ResourceClass, String...)})
     */
    public TermMapping createIriMapping(String iriClassName, String... columns)
    {
        IriClass iriClass = getIriClass(iriClassName);

        return new ParametrisedIriMapping(iriClass, getColumns(iriClass, columns));
    }


    /**
     * Constant IRI mapping whose IRI class and columns are detected lazily.
     *
     * @param iri the IRI
     * @return constant IRI mapping whose IRI class and columns are detected lazily
     */
    public ConstantIriMapping createIriMapping(Iri iri)
    {
        return new ConstantIriMapping(iri);
    }


    /**
     * Constant IRI mapping from {@code <iri>} or from a prefixed name resolved by the registered prefixes.
     *
     * @param value the specification text
     * @return constant IRI mapping from {@code <iri>} or from a prefixed name resolved by the registered prefixes
     */
    public ConstantIriMapping createIriMapping(String value)
    {
        String iri = null;

        if(value.startsWith("<"))
        {
            iri = value.substring(1, value.length() - 1);
        }
        else
        {
            String[] parts = value.split(":", 2);

            if(parts.length != 2 && !(parts.length == 1 && value.endsWith(":")))
                throw new IllegalArgumentException("invalid iri value: '" + value + "'");

            String prefix = prefixes.get(parts[0]);

            if(prefix == null)
                throw new IllegalArgumentException("unknown prefix '" + parts[0] + "'");

            iri = prefix + (parts.length == 2 ? parts[1] : "");
        }

        return createIriMapping(new Iri(iri));
    }


    /**
     * Column-based mapping of a class without a mapping of its own, the box, a class derived from the box or a triple
     * term class, over the given column specifications (see {@link #getColumns(ResourceClass, String...)}).
     *
     * @param resourceClass the resource class
     * @param columns the columns
     * @return column-based mapping of the class over the given column specifications
     */
    public TermMapping createTermMapping(ResourceClass resourceClass, String... columns)
    {
        return new ParametrisedTermMapping(resourceClass, getColumns(resourceClass, columns));
    }


    /**
     * Column-based mapping of a class without a mapping of its own, the box, a class derived from the box or a triple
     * term class, over the given columns.
     *
     * @param resourceClass the resource class
     * @param columns the columns
     * @return column-based mapping of the class over the given columns
     */
    public TermMapping createTermMapping(ResourceClass resourceClass, List<Column> columns)
    {
        return new ParametrisedTermMapping(resourceClass, columns);
    }


    /**
     * Constant triple term mapping whose class and columns are detected lazily.
     *
     * @param term the triple term
     * @return constant triple term mapping whose class and columns are detected lazily
     */
    public ConstantTripleTermMapping createTripleTermMapping(TripleTerm term)
    {
        return new ConstantTripleTermMapping(term);
    }


    /**
     * Column-based blank node mapping over the given column specifications (see
     * {@link #getColumns(ResourceClass, String...)}).
     *
     * @param blankNodeClass the blank node class
     * @param columns the columns
     * @return column-based blank node mapping over the given column specifications (see
     *         {@link #getColumns(ResourceClass, String...)})
     */
    public TermMapping createBlankNodeMapping(BlankNodeClass blankNodeClass, String... columns)
    {
        return new ParametrisedBlankNodeMapping(blankNodeClass, getColumns(blankNodeClass, columns));
    }


    /**
     * Column-based literal mapping over the given column specifications (see
     * {@link #getColumns(ResourceClass, String...)}).
     *
     * @param literalClass the literal class
     * @param columns the columns
     * @return column-based literal mapping over the given column specifications (see
     *         {@link #getColumns(ResourceClass, String...)})
     */
    public TermMapping createLiteralMapping(LiteralClass literalClass, String... columns)
    {
        return new ParametrisedLiteralMapping(literalClass, getColumns(literalClass, columns));
    }


    /**
     * Constant literal mapping in the given class.
     *
     * @param literalClass the literal class
     * @param literal the literal
     * @return constant literal mapping in the given class
     */
    public TermMapping createLiteralMapping(LiteralClass literalClass, Literal literal)
    {
        return new ConstantLiteralMapping(literalClass, literal);
    }


    /**
     * Constant xsd:string literal mapping.
     *
     * @param value the specification text
     * @return constant xsd:string literal mapping
     */
    public TermMapping createLiteralMapping(String value)
    {
        return new ConstantLiteralMapping(xsdString, new TypedLiteral(value, xsdStringIri));
    }


    /**
     * Constant xsd:boolean literal mapping.
     *
     * @param value the specification text
     * @return constant xsd:boolean literal mapping
     */
    public TermMapping createLiteralMapping(boolean value)
    {
        return new ConstantLiteralMapping(xsdBoolean, new TypedLiteral(Boolean.toString(value), xsdBooleanIri));
    }


    /**
     * Constant xsd:short literal mapping.
     *
     * @param value the specification text
     * @return constant xsd:short literal mapping
     */
    public TermMapping createLiteralMapping(short value)
    {
        return new ConstantLiteralMapping(xsdShort, new TypedLiteral(Short.toString(value), xsdShortIri));
    }


    /**
     * Constant xsd:int literal mapping.
     *
     * @param value the specification text
     * @return constant xsd:int literal mapping
     */
    public TermMapping createLiteralMapping(int value)
    {
        return new ConstantLiteralMapping(xsdInt, new TypedLiteral(Integer.toString(value), xsdIntIri));
    }


    /**
     * Constant xsd:long literal mapping.
     *
     * @param value the specification text
     * @return constant xsd:long literal mapping
     */
    public TermMapping createLiteralMapping(long value)
    {
        return new ConstantLiteralMapping(xsdLong, new TypedLiteral(Long.toString(value), xsdLongIri));
    }


    /**
     * Constant xsd:float literal mapping in the shortest round-trip lexical form.
     *
     * @param value the specification text
     * @return constant xsd:float literal mapping in the shortest round-trip lexical form
     */
    public TermMapping createLiteralMapping(float value)
    {
        return new ConstantLiteralMapping(xsdFloat, new TypedLiteral(RyuFloat.floatToString(value), xsdFloatIri));
    }


    /**
     * Constant xsd:double literal mapping in the shortest round-trip lexical form.
     *
     * @param value the specification text
     * @return constant xsd:double literal mapping in the shortest round-trip lexical form
     */
    public TermMapping createLiteralMapping(double value)
    {
        return new ConstantLiteralMapping(xsdDouble, new TypedLiteral(RyuDouble.doubleToString(value), xsdDoubleIri));
    }


    /**
     * Registers a service IRI with no mappings, procedures or functions yet.
     *
     * @param service the service IRI
     */
    protected void addEmptyService(Iri service)
    {
        services.add(service);
        mappings.put(service, new ArrayList<>());
        procedures.put(service, new HashMap<>());
        functions.put(service, new HashMap<>());
    }


    /**
     * Adds a mapping to the given service, registering the service if new; duplicates are ignored.
     *
     * @param service the service IRI
     * @param mapping the quad mapping
     */
    private void addQuadMapping(Iri service, QuadMapping mapping)
    {
        if(!services.contains(service))
            addEmptyService(service);

        if(mappings.get(service).contains(mapping))
            return;

        mappings.get(service).add(mapping);
    }


    /**
     * Adds a procedure to the given service, registering the service if new.
     *
     * @param service the service IRI
     * @param procedure the procedure definition
     */
    private void addProcedure(Iri service, ProcedureDefinition procedure)
    {
        if(!services.contains(service))
            addEmptyService(service);

        procedures.get(service).put(procedure.getProcedureName(), procedure);
    }


    /**
     * Adds a function to the given service, registering the service if new.
     *
     * @param service the service IRI
     * @param function the function
     */
    private void addFunction(Iri service, FunctionDefinition function)
    {
        if(!services.contains(service))
            addEmptyService(service);

        functions.get(service).put(function.getFunctionName(), function);
    }


    /**
     * SPARQL orders and compares strings by unicode code points, whereas PostgreSQL orders a character column by its
     * collation. A mapped column with any other collation would therefore make ORDER BY and the relational operators
     * return a different order than the specification prescribes, and a different one than the same term gets when the
     * engine keeps it boxed in sparql.rdfbox.
     *
     * @param tables the tables
     * @param terms the term mappings to check
     */
    private void checkColumnCollations(List<SourceTable> tables, TermMapping... terms)
    {
        for(TermMapping term : terms)
        {
            if(!(term instanceof ParametrisedMapping) || term.getColumns(null) == null)
                continue;

            for(Column column : term.getColumns(null))
            {
                for(SourceTable table : tables)
                {
                    String collation = databaseSchema.getForeignCollation(table, column);

                    if(collation != null)
                        throw new IllegalArgumentException("column " + table + "." + column + " uses the " + collation
                                + " collation, but SPARQL orders and compares strings by unicode code"
                                + " points; declare the column with the \"C\" collation");
                }
            }
        }
    }


    /**
     * Registers a single-table quad mapping of this service (see {@link SingleTableQuadMapping}). With
     * {@code autoAddToDefaultGraph}, a named-graph mapping is also added to the default graph. The graph and the
     * predicate are constant IRIs or columns of a class that can hold IRIs, such as the box. The columns of the table
     * referenced by the mappings and the conditions are checked against the database schema: they have to exist with
     * the types they are declared with. Mapped character columns must use a code-point collation.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     * @param distinct distinct flag of each table
     * @throws IllegalArgumentException if the graph or the predicate mapping cannot hold an IRI, or if a referenced
     *             column of the table is unknown to the schema or declared with another type
     * @throws IllegalStateException if the mapping has a table and the configuration has no database schema
     */
    public void addQuadMapping(SourceTable table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions, boolean distinct)
    {
        if(table != null)
            checkSchema("a mapping of a table");

        checkVirtualTables(table == null ? List.of() : List.of(table));
        checkIriMapping(graph);
        checkIriMapping(predicate);
        checkMapping(table, graph);
        checkMapping(table, subject);
        checkMapping(table, predicate);
        checkMapping(table, object);
        checkConditions(table, conditions);
        checkColumnCollations(table == null ? List.of() : List.of(table), graph, subject, predicate, object);

        mappings.get(serviceIri)
                .add(new SingleTableQuadMapping(table, graph, subject, predicate, object, conditions, distinct));

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri)
                    .add(new SingleTableQuadMapping(table, null, subject, predicate, object, conditions, distinct));
    }


    /**
     * Registers a single-table quad mapping without the distinct flag.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     */
    public void addQuadMapping(SourceTable table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object, Conditions conditions)
    {
        addQuadMapping(table, graph, subject, predicate, object, conditions, false);
    }


    /**
     * Registers an unconditional single-table quad mapping.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(SourceTable table, TermMapping graph, TermMapping subject, TermMapping predicate,
            TermMapping object)
    {
        addQuadMapping(table, graph, subject, predicate, object, new Conditions(true));
    }


    /**
     * Registers a quad made of constants only (no table).
     *
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(ConstantIriMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object)
    {
        addQuadMapping(null, graph, subject, predicate, object);
    }


    /**
     * Registers a join quad mapping of this service (see {@link JoinTableQuadMapping}): the tables are joined as a tree
     * by the joins, each of which names its two tables or joins the two adjacent tables at its position, and the graph,
     * the subject, the predicate and the object are taken from the tables at the given indexes. With
     * {@code autoAddToDefaultGraph}, a named-graph mapping is also added to the default graph. The graph and the
     * predicate are constant IRIs or columns of a class that can hold IRIs, such as the box. The columns referenced by
     * the mappings, the join columns and the conditions are checked against the database schema: they have to exist in
     * their tables with the types they are declared with.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graphTable index of the table the graph is taken from
     * @param graph the graph mapping, or null for the default graph
     * @param subjectTable index of the table the subject is taken from
     * @param subject the subject mapping
     * @param predicateTable index of the table the predicate is taken from
     * @param predicate the predicate mapping
     * @param objectTable index of the table the object is taken from
     * @param object the object mapping
     * @param conditions conditions on each table
     * @param distinct distinct flag of each table
     * @throws IllegalArgumentException if the joins do not connect the tables as a tree, if a term is taken from a
     *             table the mapping does not have, if the conditions or the distinct flags are not given for each
     *             table, if the graph or the predicate mapping cannot hold an IRI, or if a referenced column is unknown
     *             to the schema or declared with another type
     * @throws IllegalStateException if the configuration has no database schema
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, int graphTable,
            TermMapping graph, int subjectTable, TermMapping subject, int predicateTable, TermMapping predicate,
            int objectTable, TermMapping object, List<Conditions> conditions, List<Boolean> distinct)
    {
        checkSchema("a mapping of a table");
        checkVirtualTables(tables);
        checkIriMapping(graph);
        checkIriMapping(predicate);

        List<JoinColumns> joins = checkJoins(tables, joinColumnsPairs);

        for(int table : List.of(graphTable, subjectTable, predicateTable, objectTable))
            if(table < 0 || table >= tables.size())
                throw new IllegalArgumentException("a join mapping takes a term from a table it does not have");

        if(conditions.size() != tables.size() || distinct.size() != tables.size())
            throw new IllegalArgumentException(
                    "a join mapping needs the conditions and the distinct flag of each of its tables");

        checkMapping(tables.get(graphTable), graph);
        checkMapping(tables.get(subjectTable), subject);
        checkMapping(tables.get(predicateTable), predicate);
        checkMapping(tables.get(objectTable), object);

        for(JoinColumns join : joins)
            checkJoinColumns(tables.get(join.getLeftTable()), tables.get(join.getRightTable()), join);

        for(int i = 0; i < conditions.size(); i++)
            checkConditions(tables.get(i), conditions.get(i));

        checkColumnCollations(tables, graph, subject, predicate, object);

        mappings.get(serviceIri).add(new JoinTableQuadMapping(tables, joins, graphTable, graph, subjectTable, subject,
                predicateTable, predicate, objectTable, object, conditions, distinct));

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri).add(new JoinTableQuadMapping(tables, joins, graphTable, null, subjectTable,
                    subject, predicateTable, predicate, objectTable, object, conditions, distinct));
    }


    /**
     * Registers an unconditional join quad mapping taking its terms from the tables at the given indexes, without
     * distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graphTable index of the table the graph is taken from
     * @param graph the graph mapping, or null for the default graph
     * @param subjectTable index of the table the subject is taken from
     * @param subject the subject mapping
     * @param predicateTable index of the table the predicate is taken from
     * @param predicate the predicate mapping
     * @param objectTable index of the table the object is taken from
     * @param object the object mapping
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, int graphTable,
            TermMapping graph, int subjectTable, TermMapping subject, int predicateTable, TermMapping predicate,
            int objectTable, TermMapping object)
    {
        addQuadMapping(tables, joinColumnsPairs, graphTable, graph, subjectTable, subject, predicateTable, predicate,
                objectTable, object, Collections.nCopies(tables.size(), new Conditions(true)),
                Collections.nCopies(tables.size(), false));
    }


    /**
     * Registers a join quad mapping taking the graph, the subject and the predicate from the first table and the object
     * from the last one.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     * @param distinct distinct flag of each table
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object, List<Conditions> conditions,
            List<Boolean> distinct)
    {
        addQuadMapping(tables, joinColumnsPairs, 0, graph, 0, subject, 0, predicate, tables.size() - 1, object,
                conditions, distinct);
    }


    /**
     * Registers a join quad mapping without distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions conditions on each table
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        addQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object, conditions,
                Collections.nCopies(tables.size(), false));
    }


    /**
     * Registers an unconditional join quad mapping.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, TermMapping graph,
            TermMapping subject, TermMapping predicate, TermMapping object)
    {
        addQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object,
                Collections.nCopies(tables.size(), new Conditions(true)));
    }


    /**
     * Registers a two-table join mapping joined on one column pair, which is typed from the database schema (see
     * {@link #getColumn(SourceTable, String)}).
     *
     * @param subjectTable table of the subject
     * @param objectTable table of the object
     * @param subjectTableJoinColumn join column of the subject table
     * @param objectTableJoinColumn join column of the object table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(SourceTable subjectTable, SourceTable objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, TermMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object)
    {
        addQuadMapping(List.of(subjectTable, objectTable),
                List.of(new JoinColumns(getColumn(subjectTable, subjectTableJoinColumn),
                        getColumn(objectTable, objectTableJoinColumn))),
                graph, subject, predicate, object);
    }


    /**
     * Registers a two-table join mapping joined on one column pair, which is typed from the database schema (see
     * {@link #getColumn(SourceTable, String)}), with conditions on each table.
     *
     * @param subjectTable table of the subject
     * @param objectTable table of the object
     * @param subjectTableJoinColumn join column of the subject table
     * @param objectTableJoinColumn join column of the object table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param subjectCondition conditions on the subject table
     * @param objectCondition conditions on the object table
     */
    public void addQuadMapping(SourceTable subjectTable, SourceTable objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, TermMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object, Conditions subjectCondition, Conditions objectCondition)
    {
        addQuadMapping(List.of(subjectTable, objectTable),
                List.of(new JoinColumns(getColumn(subjectTable, subjectTableJoinColumn),
                        getColumn(objectTable, objectTableJoinColumn))),
                graph, subject, predicate, object, List.of(subjectCondition, objectCondition));
    }


    /**
     * Registers a procedure of this service.
     *
     * @param procedure the procedure definition
     */
    public void addProcedure(ProcedureDefinition procedure)
    {
        procedures.get(serviceIri).put(procedure.getProcedureName(), procedure);
    }


    /**
     * Registers an extension function of this service.
     *
     * @param function the function
     */
    public void addFunction(FunctionDefinition function)
    {
        functions.get(serviceIri).put(function.getFunctionName(), function);
    }


    /**
     * Imports another configuration: its datatypes and IRI classes are added, its mappings, procedures and functions
     * become available under its service IRIs (for SERVICE patterns) or, with {@code merge}, its own service is merged
     * into this one together with its prefixes, and the settings of the remote endpoints it knows are adopted for the
     * endpoints this configuration has no settings of its own for (its defaults are not imported).
     *
     * @param other the configuration to import
     * @param merge whether to merge the other service into this one
     */
    public void addService(SparqlDatabaseConfiguration other, boolean merge)
    {
        if(merge)
            for(Entry<String, String> entry : other.getPrefixes().entrySet())
                addPrefix(entry.getKey(), entry.getValue());

        for(Datatype dataType : other.getDatatypes())
            addDatatype(dataType);

        // the classes of a configuration over the same database have been checked already
        for(UserIriClass iriClass : other.getIriClasses())
            addIriClass(iriClass, other.connectionPool != connectionPool);

        for(Entry<UserIriClass, Set<UserIriClass>> entry : other.iriClassOverlaps.entrySet())
            for(UserIriClass overlapping : entry.getValue())
                addIriClassOverlap(entry.getKey(), overlapping);

        for(Entry<VirtualTable, VirtualTableDefinition> entry : other.getVirtualTables().entrySet())
            addVirtualTable(entry.getKey(), entry.getValue());

        for(Entry<Iri, ServiceSettings> entry : other.endpointSettings.entrySet())
            endpointSettings.putIfAbsent(entry.getKey(), entry.getValue());

        for(Iri service : other.getServices())
        {
            Iri target = service == null && merge ? getServiceIri() : service;

            for(QuadMapping original : other.getMappings(service))
                addQuadMapping(target, original);

            for(ProcedureDefinition procedure : other.getProcedures(service).values())
                addProcedure(target, procedure);

            for(FunctionDefinition function : other.getFunctions(service).values())
                addFunction(target, function);
        }
    }


    /**
     * Adds the quads of the SPARQL 1.2 service description (features, the supported language with its versions, result
     * formats, extension functions and procedure properties) to the description graph; the SPARQL 1.1 language term is
     * kept for older clients. The terms are given by their IRIs, whatever prefixes the configuration registers. The
     * functions and procedures registered for the service after the call are not described.
     *
     * @throws IllegalStateException if the configuration has no service IRI
     */
    public void addBasicServiceDescription()
    {
        if(serviceIri == null)
            throw new IllegalStateException("a service description needs a service IRI");

        ConstantIriMapping graph = createIriMapping(descriptionGraphIri);
        ConstantIriMapping endpoint = createIriMapping(serviceIri);
        ConstantIriMapping type = createIriMapping(new Iri(Rdf.TYPE));


        addQuadMapping(graph, endpoint, type, sd("Service"));
        addQuadMapping(graph, endpoint, sd("endpoint"), endpoint);

        addQuadMapping(graph, endpoint, sd("feature"), sd("BasicFederatedQuery"));
        addQuadMapping(graph, endpoint, sd("feature"), sd("EmptyGraphs"));

        if(autoAddToDefaultGraph)
            addQuadMapping(graph, endpoint, sd("feature"), sd("UnionDefaultGraph"));

        addQuadMapping(graph, endpoint, sd("defaultEntailmentRegime"),
                createIriMapping(new Iri(entailmentNamespace + "Simple")));
        addQuadMapping(graph, endpoint, sd("supportedLanguage"), sd("SPARQLQuery"));
        addQuadMapping(graph, endpoint, sd("supportedLanguage"), sd("SPARQL11Query"));

        for(String version : List.of("1.2", "1.2-basic", "1.1"))
            addQuadMapping(graph, endpoint, sd("supportedVersion"),
                    createIriMapping(new Iri(versionNamespace + "version-" + version)));

        for(String format : List.of("SPARQL_Results_XML", "SPARQL_Results_JSON", "SPARQL_Results_CSV",
                "SPARQL_Results_TSV", "RDF_JSON", "RDF_XML", "Turtle", "TriG", "N-Triples", "N-Quads"))
            addQuadMapping(graph, endpoint, sd("resultFormat"), createIriMapping(new Iri(formatNamespace + format)));

        for(FunctionDefinition def : functions.get(serviceIri).values())
        {
            ConstantIriMapping function = createIriMapping(new Iri(def.getFunctionName()));
            addQuadMapping(graph, endpoint, sd("extensionFunction"), function);
            addQuadMapping(graph, function, type, sd("Function"));
        }


        for(String iri : getProcedureProperties())
        {
            ConstantIriMapping procedure = createIriMapping(new Iri(iri));
            addQuadMapping(graph, endpoint, sd("propertyFeature"), procedure);
            addQuadMapping(graph, procedure, type, sd("Feature"));
        }
    }


    /**
     * IRIs of the properties of the procedures of the service: the names of the procedures, the names of their
     * parameters that do not start with {@code #}, and the names of the results of the procedures that are not simple.
     *
     * @return IRIs of the properties of the procedures of the service
     */
    private Set<String> getProcedureProperties()
    {
        Set<String> properties = new HashSet<>();

        for(ProcedureDefinition def : getProcedures(serviceIri).values())
        {
            properties.add(def.getProcedureName());

            for(ParameterDefinition parameter : def.getParameters())
                if(!parameter.getParamName().startsWith("#"))
                    properties.add(parameter.getParamName());

            if(!def.isSimple())
                for(ResultDefinition result : def.getResults())
                    properties.add(result.getResultName());
        }

        return properties;
    }


    /**
     * IRIs of the properties that a query editor offers to complete, which the endpoint lists for its {@code info}
     * request: the predicates of the quad mappings of the service that are known without reading the data (the constant
     * ones and the members of an enumerated IRI class taken from a column) and the properties of the procedures of the
     * service. A deployment that knows the other predicates, like from the statistics of its data, overrides the
     * method; the endpoint asks for the hints once.
     *
     * @return IRIs of the properties that a query editor offers to complete
     */
    public Set<Iri> getPropertyHints()
    {
        Set<Iri> properties = new HashSet<>();

        for(QuadMapping mapping : getMappings(serviceIri))
            properties.addAll(getHints(mapping.getPredicate()));

        for(String iri : getProcedureProperties())
            properties.add(new Iri(iri));

        return properties;
    }


    /**
     * IRIs of the classes that a query editor offers to complete, which the endpoint lists for its {@code info}
     * request: the objects of the quad mappings of the service with the constant predicate {@code rdf:type} that are
     * known without reading the data (the constant ones and the members of an enumerated IRI class taken from a
     * column). A deployment that knows the other classes, like from the statistics of its data, overrides the method;
     * the endpoint asks for the hints once.
     *
     * @return IRIs of the classes that a query editor offers to complete
     */
    public Set<Iri> getClassHints()
    {
        Iri type = new Iri(Rdf.TYPE);
        Set<Iri> classes = new HashSet<>();

        for(QuadMapping mapping : getMappings(serviceIri))
            if(mapping.getPredicate() instanceof ConstantIriMapping predicate && type.equals(predicate.getValue()))
                classes.addAll(getHints(mapping.getObject()));

        return classes;
    }


    /**
     * IRIs that a term mapping gives without reading the data: the IRI of a constant mapping and the members of the
     * enumerated IRI class of a mapping of columns.
     *
     * @param mapping the term mapping
     * @return IRIs that the term mapping gives without reading the data
     */
    private static Set<Iri> getHints(TermMapping mapping)
    {
        if(mapping instanceof ConstantIriMapping constant && constant.getValue() instanceof Iri iri)
            return Set.of(iri);

        if(mapping instanceof ParametrisedIriMapping parametrised
                && parametrised.getResourceClass() instanceof EnumUserIriClass enumeration)
            return enumeration.getMembers();

        return Set.of();
    }


    /**
     * Constant IRI mapping of a term of the SPARQL service description.
     *
     * @param name the local name of the term
     * @return constant IRI mapping of the term
     */
    private ConstantIriMapping sd(String name)
    {
        return createIriMapping(new Iri(serviceDescriptionNamespace + name));
    }


    /**
     * Parses a column specification that carries its type: {@code (expression)::type} is an SQL expression of the given
     * type, whose parenthesis closes right before the type, {@code 'literal'::type} a typed constant and
     * {@code null::type} (in any letter case) a typed NULL constant. Anything else is the name of a column of a table,
     * whose type is not part of the specification; such a name is accepted only by
     * {@link #getColumns(ResourceClass, String...)}, which types it by the resource class of the mapped term, and by
     * {@link #getColumn(SourceTable, String)}, which types it from the database schema. A mapping of a term checks that
     * its columns have the SQL types of its resource class; the expression has to evaluate to its type in a collation
     * that orders by code points, which nothing checks, as the text of an expression is not checked against the
     * database (and neither is the SQL that a user IRI class writes for a query).
     *
     * @param value the specification text
     * @return the parsed column
     * @throws IllegalArgumentException if an expression lacks its type or is not one parenthesised expression, or the
     *             specification is a column name
     */
    public static Column getColumn(String value)
    {
        Column column = parseColumn(value);

        if(column == null)
            throw new IllegalArgumentException(
                    "column name " + value + " needs a table or a resource class to get its type");

        return column;
    }


    /**
     * Parses a column specification that carries its type (see {@link #getColumn(String)}); a column name, which
     * carries none, yields null.
     *
     * @param value the specification text
     * @return the parsed column, or null for a column name
     * @throws IllegalArgumentException if an expression lacks its type or is not one parenthesised expression
     */
    private static Column parseColumn(String value)
    {
        if(value.matches("(?s)\\(.*\\)::[_a-zA-Z0-9.]+"))
        {
            String type = value.replaceFirst("(?s)^\\(.*\\)::([_a-zA-Z0-9.]+)$", "$1");

            // the type is that of the whole expression only if the parenthesis closes right before it
            if(getClosingParenthesis(value) != value.length() - type.length() - 3)
                throw new IllegalArgumentException(
                        "expression column " + value + " is not one parenthesised expression: use (expression)::type");

            return new ExpressionColumn(new SqlCode(value), SqlType.of(type));
        }
        else if(value.startsWith("("))
            throw new IllegalArgumentException(
                    "expression column " + value + " lacks its type: use (expression)::type");
        else if(value.matches("(?s)'(?:[^']|'')*'::[_a-zA-Z0-9.]+"))
            return new ValueColumn(value.replaceFirst("(?s)^'(.*)'::[_a-zA-Z0-9.]+$", "$1").replaceAll("''", "'"),
                    SqlType.of(value.replaceFirst("(?s)^'.*'::([_a-zA-Z0-9.]+)$", "$1")));
        else if(value.matches("(?i)null::[_a-zA-Z0-9.]+"))
            return new NullColumn(SqlType.of(value.replaceFirst("^(?i)null::([_a-zA-Z0-9.]+)$", "$1")));
        else
            return null;
    }


    /**
     * Index of the parenthesis that closes the one an SQL expression starts with. The string constants (also those with
     * escapes, {@code E'...'}) and the quoted identifiers are skipped, so a parenthesis inside them does not count.
     *
     * @param expression the SQL expression, starting with a parenthesis
     * @return index of the closing parenthesis, or -1 if it is not closed
     */
    private static int getClosingParenthesis(String expression)
    {
        int depth = 0;

        for(int i = 0; i < expression.length(); i++)
        {
            char c = expression.charAt(i);

            if(c == '(')
            {
                depth++;
            }
            else if(c == ')')
            {
                if(--depth == 0)
                    return i;
            }
            else if(c == '\'' || c == '"')
            {
                // a constant with escapes is introduced by E standing alone, not by the end of an identifier
                boolean escapes = c == '\'' && i > 0 && Character.toUpperCase(expression.charAt(i - 1)) == 'E'
                        && (i == 1 || !Character.isLetterOrDigit(expression.charAt(i - 2))
                                && expression.charAt(i - 2) != '_' && expression.charAt(i - 2) != '$');

                // a doubled quote stands for itself and the quoted text goes on
                for(i++; i < expression.length(); i++)
                {
                    if(escapes && expression.charAt(i) == '\\')
                        i++;
                    else if(expression.charAt(i) == c && i + 1 < expression.length() && expression.charAt(i + 1) == c)
                        i++;
                    else if(expression.charAt(i) == c)
                        break;
                }
            }
        }

        return -1;
    }


    /**
     * Parses a column specification (see {@link #getColumn(String)}), typing a column name by the given type.
     *
     * @param value the specification text
     * @param type SQL type of the column when the specification is a column name
     * @return the parsed column
     */
    private static Column getColumn(String value, SqlType type)
    {
        Column column = parseColumn(value);

        return column != null ? column : new TableColumn(value, type);
    }


    /**
     * Parses the column specifications of the columns representing a term of the resource class (see
     * {@link #getColumn(String)}), one per column of the class: a column name gets the SQL type of the class at its
     * position.
     *
     * @param resourceClass the resource class of the term
     * @param values the column specifications
     * @return the parsed columns
     * @throws IllegalArgumentException if the number of specifications differs from the number of columns of the class
     */
    public static List<Column> getColumns(ResourceClass resourceClass, String... values)
    {
        if(values.length != resourceClass.getColumnCount())
            throw new IllegalArgumentException("wrong number of columns for class " + resourceClass.getResourceName());

        List<Column> columns = new ArrayList<>(values.length);

        for(int i = 0; i < values.length; i++)
            columns.add(getColumn(values[i], resourceClass.getSqlTypes().get(i)));

        return columns;
    }


    /**
     * Parses a column specification of a column of the given table (see {@link #getColumn(String)}): a column name gets
     * its SQL type from the database schema.
     *
     * @param table the table
     * @param value the specification text
     * @return the parsed column
     * @throws IllegalStateException if the configuration has no database schema
     * @throws IllegalArgumentException if the schema does not know the column of the table
     */
    public Column getColumn(SourceTable table, String value)
    {
        Column column = parseColumn(value);

        if(column != null)
            return column;

        if(databaseSchema == null)
            throw new IllegalStateException(
                    "column " + value + " of table " + table + " cannot be typed without a database schema");

        TableColumn tableColumn = databaseSchema.getColumn(table, value);

        if(tableColumn == null)
            throw new IllegalArgumentException("unknown column \"" + value + "\" of table " + table);

        return tableColumn;
    }


    /**
     * Parses column specifications of columns of the given table, see {@link #getColumn(SourceTable, String)}.
     *
     * @param table the table
     * @param values the column specifications
     * @return the parsed columns
     * @throws IllegalStateException if the configuration has no database schema
     * @throws IllegalArgumentException if the schema does not know a column of the table
     */
    public List<Column> getColumns(SourceTable table, String... values)
    {
        List<Column> columns = new ArrayList<>(values.length);

        for(String value : values)
            columns.add(getColumn(table, value));

        return columns;
    }


    /**
     * Checks that the configuration has a database schema, which the columns of a table are checked against and which
     * the translation of a query reads the facts of the tables from.
     *
     * @param subject what needs the schema, for the message
     * @throws IllegalStateException if the configuration has no database schema
     */
    private void checkSchema(String subject)
    {
        if(databaseSchema == null)
            throw new IllegalStateException(subject + " needs a database schema");
    }


    /**
     * Checks that a column of the table referenced by a mapping or a condition is known to the database schema with the
     * type it is declared with; constants and expressions are not checked. A configuration mapping a table has a
     * database schema (see {@link #checkSchema}).
     *
     * @param table the table
     * @param column the column
     * @throws IllegalArgumentException if the column is unknown to the schema or declared with another type
     */
    private void checkColumn(SourceTable table, Column column)
    {
        if(!(column instanceof TableColumn))
            return;

        SqlType type = databaseSchema.getColumnType(table, column);

        if(type == null)
            throw new IllegalArgumentException("unknown column " + column + " of table " + table);

        if(!type.equals(column.getType()))
            throw new IllegalArgumentException("column " + column + " of table " + table + " is declared as "
                    + column.getType() + " but has type " + type);
    }


    /**
     * Checks the columns of a term mapping: a column of the table has to be known to the database schema with the type
     * it is declared with (see {@link #checkColumn}), and every column, an expression and a constant as well, has to
     * have the SQL type of the resource class of the mapping at its position, as the engine reads and compares it so; a
     * mapping without columns has nothing to check.
     *
     * @param table the table of the mapping, or null
     * @param mapping the term mapping
     * @throws IllegalArgumentException if a column of the table is unknown to the schema or declared with another type,
     *             or if a column has another SQL type than the resource class at its position
     */
    private void checkMapping(SourceTable table, TermMapping mapping)
    {
        if(!(mapping instanceof ParametrisedMapping))
            return;

        ResourceClass resourceClass = mapping.getResourceClass(null);
        List<Column> columns = mapping.getColumns(null);

        for(int i = 0; i < columns.size(); i++)
        {
            Column column = columns.get(i);

            if(table != null)
                checkColumn(table, column);

            if(!column.getType().equals(resourceClass.getSqlTypes().get(i)))
                throw new IllegalArgumentException("column " + column + " of type " + column.getType()
                        + " cannot represent the class " + resourceClass.getResourceName() + ", whose SQL type is "
                        + resourceClass.getSqlTypes().get(i));
        }
    }


    /**
     * Checks that a graph or predicate mapping can hold an IRI: a constant has to be an IRI, a column mapping has to be
     * of a class that is not disjoint with the IRIs; a null graph mapping (the default graph) passes.
     *
     * @param mapping the graph or predicate mapping, or null
     * @throws IllegalArgumentException if the mapping cannot hold an IRI
     */
    private void checkIriMapping(TermMapping mapping)
    {
        if(mapping instanceof ConstantMapping constant ? !(constant.getValue() instanceof Iri) :
                mapping != null && ResourceClass.areDisjunct(this, mapping.getResourceClass(null), iri))
            throw new IllegalArgumentException("the mapping of a graph or a predicate cannot hold an IRI");
    }


    /**
     * Checks the columns of conditions against the database schema (see {@link #checkColumn}); conditions without a
     * table have nothing to check.
     *
     * @param table the table of the conditions, or null
     * @param conditions the conditions
     */
    private void checkConditions(SourceTable table, Conditions conditions)
    {
        if(table == null)
            return;

        for(Column column : conditions.getNonConstantColumns())
            checkColumn(table, column);
    }


    /**
     * Checks that the joins of a join mapping connect its tables as a tree: there is one fewer of them than tables,
     * each joins two different tables of the mapping, and every table is reached from the first one.
     *
     * @param tables the tables
     * @param joinColumnsPairs the joins of the tables
     * @return the joins, each naming its two tables (see {@link JoinColumns#at})
     * @throws IllegalArgumentException if the joins do not connect the tables as a tree
     */
    private static List<JoinColumns> checkJoins(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs)
    {
        if(joinColumnsPairs.size() != tables.size() - 1)
            throw new IllegalArgumentException("a join mapping needs one join fewer than tables");

        List<JoinColumns> joins = new ArrayList<>(joinColumnsPairs.size());

        for(int i = 0; i < joinColumnsPairs.size(); i++)
        {
            JoinColumns join = joinColumnsPairs.get(i).at(i);

            if(join.getLeftTable() < 0 || join.getLeftTable() >= tables.size() || join.getRightTable() < 0
                    || join.getRightTable() >= tables.size() || join.getLeftTable() == join.getRightTable())
                throw new IllegalArgumentException("a join of a join mapping has to join two of its tables");

            joins.add(join);
        }

        Set<Integer> reached = new HashSet<>();
        reached.add(0);

        for(boolean extended = true; extended;)
        {
            extended = false;

            for(JoinColumns join : joins)
                if(reached.contains(join.getLeftTable()) != reached.contains(join.getRightTable()))
                    extended = reached.add(join.getLeftTable()) | reached.add(join.getRightTable());
        }

        if(reached.size() != tables.size())
            throw new IllegalArgumentException("the joins of a join mapping have to connect all its tables");

        return joins;
    }


    /**
     * Checks the columns of both sides of join columns against the database schema (see {@link #checkColumn}).
     *
     * @param leftTable the left table
     * @param rightTable the right table
     * @param joinColumns the join columns
     */
    private void checkJoinColumns(SourceTable leftTable, SourceTable rightTable, JoinColumns joinColumns)
    {
        for(Column column : joinColumns.getLeftColumns())
            checkColumn(leftTable, column);

        for(Column column : joinColumns.getRightColumns())
            checkColumn(rightTable, column);
    }


    /**
     * IRI of this endpoint's service, or null.
     *
     * @return IRI of this endpoint's service, or null
     */
    public Iri getServiceIri()
    {
        return serviceIri;
    }


    /**
     * Default base IRI of the queries of this endpoint, which the relative IRIs of a query are resolved against until
     * the query declares a BASE of its own (the default base IRI of section 5.1.4 of RFC 3986): the IRI of this
     * endpoint's service.
     *
     * @return default base IRI of the queries, or null when the endpoint has no service IRI or one that is not absolute
     */
    public Iri getBaseIri()
    {
        if(serviceIri != null && IriResolver.isIriReference(serviceIri.getValue())
                && IriResolver.hasScheme(serviceIri.getValue()))
            return serviceIri;

        return null;
    }


    /**
     * CONSTRUCT query returning the service description graph, or the empty graph when the configuration has no
     * description graph.
     *
     * @return CONSTRUCT query returning the service description graph
     */
    public String getServiceDescriptionQuery()
    {
        if(descriptionGraphIri == null)
            return "construct {} where {}";

        return "construct {?s ?p ?o} where { graph " + descriptionGraphIri + " {?s ?p ?o}}";
    }


    /**
     * This service (first) and the imported federated services.
     *
     * @return this service (first) and the imported federated services
     */
    public List<Iri> getServices()
    {
        return services;
    }


    /**
     * Sets the settings of the calls of the remote SERVICE endpoints that have no settings of their own; initially
     * {@link ServiceSettings#DEFAULT}. An endpoint given settings of its own keeps them, so they have to be derived
     * from the new defaults again if they should follow them.
     *
     * @param settings the settings
     */
    public void setServiceSettings(ServiceSettings settings)
    {
        this.serviceSettings = Objects.requireNonNull(settings);
    }


    /**
     * Sets the settings of the calls of a remote SERVICE endpoint, replacing the defaults of the configuration for the
     * calls naming the endpoint by the IRI (in the SERVICE clause or by the value of its variable). Null removes the
     * settings of the endpoint.
     *
     * @param endpoint IRI of the endpoint
     * @param settings the settings, or null
     */
    public void setServiceSettings(Iri endpoint, ServiceSettings settings)
    {
        if(settings == null)
            endpointSettings.remove(endpoint);
        else
            endpointSettings.put(endpoint, settings);
    }


    /**
     * Settings of the calls of the remote SERVICE endpoints that have no settings of their own.
     *
     * @return settings of the calls of the remote SERVICE endpoints that have no settings of their own
     */
    public ServiceSettings getServiceSettings()
    {
        return serviceSettings;
    }


    /**
     * Settings of the calls of the remote SERVICE endpoint: its own settings, or the defaults of the configuration.
     *
     * @param endpoint IRI of the endpoint
     * @return settings of the calls of the remote SERVICE endpoint
     */
    public ServiceSettings getServiceSettings(Iri endpoint)
    {
        return endpointSettings.getOrDefault(endpoint, serviceSettings);
    }


    /**
     * Prefixes by name.
     *
     * @return prefixes by name
     */
    public Map<String, String> getPrefixes()
    {
        return prefixes;
    }


    /**
     * All registered datatypes.
     *
     * @return all registered datatypes
     */
    public Collection<Datatype> getDatatypes()
    {
        return dataTypeMap.values();
    }


    /**
     * Datatype of the IRI, or null if not registered.
     *
     * @param iri IRI of the service
     * @return datatype of the IRI, or null if not registered
     */
    public Datatype getDatatype(Iri iri)
    {
        return dataTypeMap.get(iri);
    }


    /**
     * User IRI classes ordered by check cost.
     *
     * @return user IRI classes ordered by check cost
     */
    public List<UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    /**
     * User IRI class of the given name; fails if unknown.
     *
     * @param name the name
     * @return user IRI class of the given name; fails if unknown
     */
    public UserIriClass getIriClass(String name)
    {
        UserIriClass iriClass = iriClassMap.get(name);

        if(iriClass == null)
            throw new IllegalArgumentException("unknown iri class: '" + name + "'");

        return iriClass;
    }


    /**
     * Quad mappings of the given service.
     *
     * @param iri IRI of the service
     * @return quad mappings of the given service
     */
    public List<QuadMapping> getMappings(Iri iri)
    {
        return mappings.get(iri);
    }


    /**
     * Procedures of the given service, by IRI.
     *
     * @param iri IRI of the service
     * @return procedures of the given service, by IRI
     */
    public Map<String, ProcedureDefinition> getProcedures(Iri iri)
    {
        return procedures.get(iri);
    }


    /**
     * Extension functions of the given service, by IRI.
     *
     * @param iri IRI of the service
     * @return extension functions of the given service, by IRI
     */
    public Map<String, FunctionDefinition> getFunctions(Iri iri)
    {
        return functions.get(iri);
    }


    /**
     * Catalog facts of the target database.
     *
     * @return catalog facts of the target database
     */
    public DatabaseSchema getDatabaseSchema()
    {
        return databaseSchema;
    }


    /**
     * Definition of the virtual table, or null when the table is not registered.
     *
     * @param table the virtual table
     * @return definition of the virtual table, or null when the table is not registered
     */
    public VirtualTableDefinition getVirtualTableDefinition(VirtualTable table)
    {
        return virtualTables.get(table);
    }


    /**
     * Registered virtual tables with their definitions, in registration order.
     *
     * @return registered virtual tables with their definitions, in registration order
     */
    public Map<VirtualTable, VirtualTableDefinition> getVirtualTables()
    {
        return virtualTables;
    }


    /**
     * Connection pool of the target database.
     *
     * @return connection pool of the target database
     */
    public DataSource getConnectionPool()
    {
        return connectionPool;
    }


    /**
     * Configuration-wide cache of IRI class detections shared by all requests.
     *
     * @return configuration-wide cache of IRI class detections shared by all requests
     */
    public final IriCache getIriCache()
    {
        return iriCache;
    }


    /**
     * Registers the statistics of a table the mappings read, in place of those of the catalog (see
     * {@link #getTableStatistics}).
     *
     * @param table the table
     * @param statistics the statistics of the table
     */
    public void addTableStatistics(SourceTable table, TableStatistics statistics)
    {
        tableStatistics.put(table, statistics);
    }


    /**
     * Statistics of a table the mappings read: those registered for it, else the statistics of the catalog for a
     * database table (see {@link CatalogStatistics}) and the defaults of the planner for any other table (see
     * {@link DefaultStatistics}), kept from then on.
     *
     * @param table the table
     * @return statistics of the table
     */
    public TableStatistics getTableStatistics(SourceTable table)
    {
        return tableStatistics.computeIfAbsent(table, t -> t instanceof DatabaseTable databaseTable ?
                new CatalogStatistics(databaseTable) : new DefaultStatistics());
    }


    /**
     * Sets the function through which the engine reads the numbers of the distinct combinations of the values of
     * several columns that the extended statistics of the database keep for a table ({@code CREATE STATISTICS} of the
     * kind {@code ndistinct}), like those of the columns of an IRI made of several parts (see
     * {@link CatalogStatistics}). The database shows them in the view {@code pg_stats_ext} to the owner of the table
     * only, so a role that may only read the tables needs a function declared {@code SECURITY DEFINER} by their owner:
     * it takes the schema and the name of a table (both of the type {@code name}) and returns the text forms of the
     * {@code n_distinct} of the extended statistics of the table ({@code SETOF text}), like
     *
     * <pre>
     * CREATE FUNCTION stats.ndistinct(schema_name name, table_name name) RETURNS SETOF text
     * LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp
     * AS $$ SELECT n_distinct::text FROM pg_stats_ext
     *     WHERE schemaname = schema_name AND tablename = table_name AND n_distinct IS NOT NULL
     *     AND has_table_privilege(session_user, format('%I.%I', schema_name, table_name), 'SELECT') $$;
     * REVOKE EXECUTE ON FUNCTION stats.ndistinct(name, name) FROM PUBLIC;
     * GRANT EXECUTE ON FUNCTION stats.ndistinct(name, name) TO reader;
     * </pre>
     *
     * which gives no more than the numbers, of the tables the role logged in to the database may read; the statements
     * of {@link ExtendedStatistics} make the function together with the statistics the configuration needs. Without a
     * function the engine reads the view itself, which serves a role owning the tables. The statistics of a table are
     * kept by the configuration once they have been read, so the function is to be set before the configuration serves
     * requests.
     *
     * @param schema the schema of the function
     * @param name the name of the function
     */
    public void setExtendedStatisticsFunction(String schema, String name)
    {
        extendedStatisticsFunction = new Function(schema, name);
    }


    /**
     * The function through which the engine reads the extended statistics of the tables (see
     * {@link #setExtendedStatisticsFunction}), or null when it reads the view {@code pg_stats_ext} itself.
     *
     * @return the function through which the engine reads the extended statistics of the tables, or null
     */
    public Function getExtendedStatisticsFunction()
    {
        return extendedStatisticsFunction;
    }


    /**
     * Declares a dictionary table: a table whose rows give the values of one of its keys for those of another one, like
     * a table of the terms of a dataset with their numbers, and are neither changed nor removed while the configuration
     * serves requests. The translation of a query then reads the row that constants select by a key of the table and
     * takes the row by the key the access to the table is joined by instead (see
     * {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}), so that a table referencing that key is restricted by
     * constants and the dictionary table is not read by the query at all. What the translation reads is kept for the
     * request only.
     *
     * @param table the table
     * @throws IllegalStateException if the configuration has no database schema
     */
    public void addDictionaryTable(DatabaseTable table)
    {
        checkSchema("a dictionary table");

        dictionaryTables.add(table);
    }


    /**
     * True if the table is declared to be a dictionary table (see {@link #addDictionaryTable}).
     *
     * @param table the table
     * @return true if the table is declared to be a dictionary table, false otherwise
     */
    public boolean isDictionaryTable(SourceTable table)
    {
        return dictionaryTables.contains(table);
    }


    /**
     * Declares a probed table: a table whose accesses are asked for a row when a query is translated. An access that
     * constants restrict in the leading columns of an index of the table is looked up by those constants, and one that
     * has no such row gives no solution (see {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}). It serves tables
     * that several mappings split the data into by something a query seldom tells, like two tables of quads for the two
     * kinds of objects: a pattern matches all such mappings, and a join of several such patterns has a branch for every
     * combination of their tables, whereas a predicate mostly has rows in one of the tables, which the lookup tells.
     * What the translation finds is kept for the request only.
     *
     * @param table the table
     * @throws IllegalStateException if the configuration has no database schema
     */
    public void addProbedTable(DatabaseTable table)
    {
        checkSchema("a probed table");

        probedTables.add(table);
    }


    /**
     * True if the table is declared to be a probed table (see {@link #addProbedTable}).
     *
     * @param table the table
     * @return true if the table is declared to be a probed table, false otherwise
     */
    public boolean isProbedTable(SourceTable table)
    {
        return probedTables.contains(table);
    }


    /**
     * Turns the join optimizer of the engine on or off: when on, the engine chooses the order and the methods of the
     * joins it generates from the statistics of the tables (see {@link #getTableStatistics}) and makes the planner keep
     * them, instead of leaving them to the planner (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer}). Off by
     * default.
     *
     * @param enabled whether the join optimizer is on
     */
    public void setJoinOptimizer(boolean enabled)
    {
        joinOptimizer = enabled;
    }


    /**
     * True if the engine chooses the order and the methods of the joins it generates (see {@link #setJoinOptimizer}).
     *
     * @return true if the engine chooses the order and the methods of the joins it generates, false otherwise
     */
    public boolean isJoinOptimizerEnabled()
    {
        return joinOptimizer;
    }


    /**
     * Chooses how the planner is made to keep the joins the join optimizer chooses (see {@link #setJoinOptimizer}).
     * Without plan advice, the default, the statement runs under settings that make the planner keep the order of the
     * joins as written and hash every join it can, and an index lookup is written as a lateral subquery fenced by
     * {@code OFFSET 0}, which the database evaluates in the process reading the outer rows, or, where the database may
     * evaluate the statement by parallel workers, merged into the join and compared by conditions it cannot hash (see
     * {@link cz.iocb.sparql.engine.request.Request#isParallel}). With plan advice, the lookups are plain joins of the
     * statement, which the database can hand to the workers and cache by the values they are looked up by (Memoize),
     * and the advice of the module {@code pg_plan_advice} makes the planner look them up by nested loops (see
     * {@link cz.iocb.sparql.engine.database.PlanAdvice}); a statement with a lookup is planned once more for it, for
     * the planner to tell the identifiers of its relations, and once again when a lookup is memoized. The module comes
     * with PostgreSQL 19 and later and has to be loaded in the sessions of the data source, by
     * {@code session_preload_libraries} or {@code shared_preload_libraries}, as only a superuser may load it in a
     * session. The tables looked up are given aliases of their own, so the expressions of the mappings must not qualify
     * the columns by the names of their tables.
     *
     * @param enabled whether the planner is made to keep the joins by plan advice
     */
    public void setPlanAdvice(boolean enabled)
    {
        planAdvice = enabled;
    }


    /**
     * True if the planner is made to keep the joins the join optimizer chooses by plan advice (see
     * {@link #setPlanAdvice}).
     *
     * @return true if the planner is made to keep the joins by plan advice, false otherwise
     */
    public boolean isPlanAdviceEnabled()
    {
        return planAdvice;
    }


    /**
     * Condition {@code column = v1 OR column = v2 ...} over column specifications: the values have to carry their type
     * (see {@link #getColumn(String)}) and a column name takes the type of the first value.
     *
     * @param column the column specification
     * @param values the column specifications of the values, at least one
     * @return condition {@code column = v1 OR column = v2 ...}
     * @throws IllegalArgumentException if there is no value, or a value is a column name
     */
    public Conditions createAreEqualCondition(String column, String... values)
    {
        return createAreEqualCondition(getColumnOfValues(column, values), getColumns(values));
    }


    /**
     * Condition {@code column = v1 OR column = v2 ...} over column specifications of columns of the table (see
     * {@link #getColumn(SourceTable, String)}).
     *
     * @param table the table
     * @param column the column specification
     * @param values the column specifications of the values
     * @return condition {@code column = v1 OR column = v2 ...}
     */
    public Conditions createAreEqualCondition(SourceTable table, String column, String... values)
    {
        return createAreEqualCondition(getColumn(table, column), getColumns(table, values));
    }


    /**
     * Condition {@code column = v1 OR column = v2 ...}.
     *
     * @param column the column
     * @param values the values
     * @return condition {@code column = v1 OR column = v2 ...}
     */
    private static Conditions createAreEqualCondition(Column column, List<Column> values)
    {
        Conditions result = new Conditions(false);

        for(Column value : values)
        {
            Condition condition = new Condition();
            condition.addAreEqual(column, value);
            result.add(condition);
        }

        return result;
    }


    /**
     * Condition {@code column != v1 AND column != v2 ...} over column specifications: the values have to carry their
     * type (see {@link #getColumn(String)}) and a column name takes the type of the first value.
     *
     * @param column the column specification
     * @param values the column specifications of the values, at least one
     * @return condition {@code column != v1 AND column != v2 ...}
     * @throws IllegalArgumentException if there is no value, or a value is a column name
     */
    public Conditions createAreNotEqualCondition(String column, String... values)
    {
        return createAreNotEqualCondition(getColumnOfValues(column, values), getColumns(values));
    }


    /**
     * Condition {@code column != v1 AND column != v2 ...} over column specifications of columns of the table (see
     * {@link #getColumn(SourceTable, String)}).
     *
     * @param table the table
     * @param column the column specification
     * @param values the column specifications of the values
     * @return condition {@code column != v1 AND column != v2 ...}
     */
    public Conditions createAreNotEqualCondition(SourceTable table, String column, String... values)
    {
        return createAreNotEqualCondition(getColumn(table, column), getColumns(table, values));
    }


    /**
     * Condition {@code column != v1 AND column != v2 ...}.
     *
     * @param column the column
     * @param values the values
     * @return condition {@code column != v1 AND column != v2 ...}
     */
    private static Conditions createAreNotEqualCondition(Column column, List<Column> values)
    {
        Condition condition = new Condition();

        for(Column value : values)
            condition.addAreNotEqual(column, value);

        return new Conditions(condition);
    }


    /**
     * Parses the column specification of a column compared with values that carry their type: a column name takes the
     * type of the first value.
     *
     * @param column the column specification
     * @param values the column specifications of the values, at least one
     * @return the parsed column
     * @throws IllegalArgumentException if there is no value, or a value is a column name
     */
    private static Column getColumnOfValues(String column, String... values)
    {
        if(values.length == 0)
            throw new IllegalArgumentException("no value to compare column " + column + " with");

        return getColumn(column, getColumn(values[0]).getType());
    }


    /**
     * Parses column specifications that carry their type, see {@link #getColumn(String)}.
     *
     * @param values the column specifications
     * @return the parsed columns
     * @throws IllegalArgumentException if a specification is a column name
     */
    private static List<Column> getColumns(String... values)
    {
        List<Column> columns = new ArrayList<>(values.length);

        for(String value : values)
            columns.add(getColumn(value));

        return columns;
    }


    /**
     * Condition {@code column IS NOT NULL} over a column specification that carries its type (see
     * {@link #getColumn(String)}).
     *
     * @param column the column specification
     * @return condition {@code column IS NOT NULL}
     * @throws IllegalArgumentException if the specification is a column name
     */
    public Conditions createIsNotNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNotNull(getColumn(column));
        return new Conditions(condition);
    }


    /**
     * Condition {@code column IS NOT NULL} over a column specification of a column of the table (see
     * {@link #getColumn(SourceTable, String)}).
     *
     * @param table the table
     * @param column the column specification
     * @return condition {@code column IS NOT NULL}
     */
    public Conditions createIsNotNullCondition(SourceTable table, String column)
    {
        Condition condition = new Condition();
        condition.addIsNotNull(getColumn(table, column));
        return new Conditions(condition);
    }


    /**
     * Condition {@code column IS NULL} over a column specification that carries its type (see
     * {@link #getColumn(String)}).
     *
     * @param column the column specification
     * @return condition {@code column IS NULL}
     * @throws IllegalArgumentException if the specification is a column name
     */
    public Conditions createIsNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNull(getColumn(column));
        return new Conditions(condition);
    }


    /**
     * Condition {@code column IS NULL} over a column specification of a column of the table (see
     * {@link #getColumn(SourceTable, String)}).
     *
     * @param table the table
     * @param column the column specification
     * @return condition {@code column IS NULL}
     */
    public Conditions createIsNullCondition(SourceTable table, String column)
    {
        Condition condition = new Condition();
        condition.addIsNull(getColumn(table, column));
        return new Conditions(condition);
    }
}
