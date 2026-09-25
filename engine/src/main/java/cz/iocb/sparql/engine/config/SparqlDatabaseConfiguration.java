package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
import java.util.Set;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedLiteralMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
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
public class SparqlDatabaseConfiguration
{
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
     * Registers a user IRI class. Classes are kept ordered by {@link UserIriClass#getCheckCost}, so IRI class detection
     * tries the cheap ones first.
     *
     * @param iriClass the user IRI class
     */
    public void addIriClass(UserIriClass iriClass)
    {
        UserIriClass previous = iriClassMap.get(iriClass.getResourceName());

        if(previous == null)
        {
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
     * Registers a virtual table: a table computed by an SQL query that quad mappings may read like a database table.
     * Every generated statement reading the table declares it in a {@code WITH} clause, and the facts stated by the
     * definition (nullable columns, keys, foreign keys, unjoinable columns) are merged into the database schema for the
     * optimiser. Registering the same table again with an equal definition is ignored.
     *
     * @param table the virtual table
     * @param definition the definition of the virtual table
     * @throws IllegalArgumentException if the table is already registered with a different definition
     */
    public void addVirtualTable(VirtualTable table, VirtualTableDefinition definition)
    {
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
     * Column-based IRI mapping of the class over the given column specifications (see {@link #getColumn}).
     *
     * @param iriClass the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the class over the given column specifications (see {@link #getColumn})
     */
    public TermMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, getColumns(columns));
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
     * Column-based IRI mapping of the named class over the given column specifications (see {@link #getColumn}).
     *
     * @param iriClassName name of the user IRI class
     * @param columns the columns
     * @return column-based IRI mapping of the named class over the given column specifications (see {@link #getColumn})
     */
    public TermMapping createIriMapping(String iriClassName, String... columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClassName), getColumns(columns));
    }


    /**
     * Constant IRI mapping whose IRI class and columns are detected lazily.
     *
     * @param iri IRI of the service
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
     * Column-based blank node mapping over the given column specifications (see {@link #getColumn}).
     *
     * @param blankNodeClass the blank node class
     * @param columns the columns
     * @return column-based blank node mapping over the given column specifications (see {@link #getColumn})
     */
    public TermMapping createBlankNodeMapping(BlankNodeClass blankNodeClass, String... columns)
    {
        return new ParametrisedBlankNodeMapping(blankNodeClass, getColumns(columns));
    }


    /**
     * Column-based literal mapping over the given column specifications (see {@link #getColumn}).
     *
     * @param literalClass the literal class
     * @param columns the columns
     * @return column-based literal mapping over the given column specifications (see {@link #getColumn})
     */
    public TermMapping createLiteralMapping(LiteralClass literalClass, String... columns)
    {
        return new ParametrisedLiteralMapping(literalClass, getColumns(columns));
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
     * {@code autoAddToDefaultGraph}, a named-graph mapping is also added to the default graph. Mapped character columns
     * must use a code-point collation.
     *
     * @param table the table
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     * @param distinct distinct flag of each table
     */
    public void addQuadMapping(SourceTable table, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object, Conditions conditions, boolean distinct)
    {
        checkVirtualTables(table == null ? List.of() : List.of(table));
        checkColumnCollations(table == null ? List.of() : List.of(table), subject, predicate, object);

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
    public void addQuadMapping(SourceTable table, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object, Conditions conditions)
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
    public void addQuadMapping(SourceTable table, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object)
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
     * Registers a join quad mapping of this service (see {@link JoinTableQuadMapping}): graph, subject and predicate
     * come from the first table, the object from the last one. With {@code autoAddToDefaultGraph}, a named-graph
     * mapping is also added to the default graph.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     * @param distinct distinct flag of each table
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions,
            List<Boolean> distinct)
    {
        checkVirtualTables(tables);
        checkColumnCollations(tables, subject, predicate, object);

        mappings.get(serviceIri).add(new JoinTableQuadMapping(tables, joinColumnsPairs, graph, subject, predicate,
                object, conditions, distinct));

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri).add(new JoinTableQuadMapping(tables, joinColumnsPairs, null, subject, predicate,
                    object, conditions, distinct));
    }


    /**
     * Registers a join quad mapping without distinct flags.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param conditions the conditions
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        addQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object, conditions,
                Collections.nCopies(tables.size(), false));
    }


    /**
     * Registers an unconditional join quad mapping.
     *
     * @param tables the tables
     * @param joinColumnsPairs join columns between adjacent tables
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(List<SourceTable> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object)
    {
        addQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object,
                Collections.nCopies(tables.size(), new Conditions(true)));
    }


    /**
     * Registers a two-table join mapping joined on one column pair of the given SQL type.
     *
     * @param subjectTable table of the subject
     * @param objectTable table of the object
     * @param subjectTableJoinColumn join column of the subject table
     * @param objectTableJoinColumn join column of the object table
     * @param type SQL type of the join columns
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public void addQuadMapping(SourceTable subjectTable, SourceTable objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, String type, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object)
    {
        addQuadMapping(List.of(subjectTable, objectTable),
                List.of(new JoinColumns(new TableColumn(subjectTableJoinColumn), new TableColumn(objectTableJoinColumn),
                        SqlType.of(type))),
                graph, subject, predicate, object);
    }


    /**
     * Registers a two-table join mapping joined on one column pair, with conditions on each table.
     *
     * @param subjectTable table of the subject
     * @param objectTable table of the object
     * @param subjectTableJoinColumn join column of the subject table
     * @param objectTableJoinColumn join column of the object table
     * @param type SQL type of the join columns
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     * @param subjectCondition conditions on the subject table
     * @param objectCondition conditions on the object table
     */
    public void addQuadMapping(SourceTable subjectTable, SourceTable objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, String type, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object, Conditions subjectCondition, Conditions objectCondition)
    {
        addQuadMapping(List.of(subjectTable, objectTable),
                List.of(new JoinColumns(new TableColumn(subjectTableJoinColumn), new TableColumn(objectTableJoinColumn),
                        SqlType.of(type))),
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
     * Imports another configuration: its datatypes and IRI classes are added, and its mappings, procedures and
     * functions become available under its service IRIs (for SERVICE patterns) or, with {@code merge}, its own service
     * is merged into this one together with its prefixes.
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

        for(UserIriClass iriClass : other.getIriClasses())
            addIriClass(iriClass);

        for(Entry<VirtualTable, VirtualTableDefinition> entry : other.getVirtualTables().entrySet())
            addVirtualTable(entry.getKey(), entry.getValue());

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
     * Adds the quads of the SPARQL 1.1 service description (features, result formats, extension functions and procedure
     * properties) to the description graph. Requires the {@code rdf}, {@code sd}, {@code ent} and {@code format}
     * prefixes.
     */
    public void addBasicServiceDescription()
    {
        //FIXME: code depends on prefix definitions

        ConstantIriMapping graph = createIriMapping(descriptionGraphIri);
        ConstantIriMapping endpoint = createIriMapping(serviceIri);


        addQuadMapping(graph, endpoint, createIriMapping("rdf:type"), createIriMapping("sd:Service"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:endpoint"), endpoint);

        addQuadMapping(graph, endpoint, createIriMapping("sd:feature"), createIriMapping("sd:BasicFederatedQuery"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:feature"), createIriMapping("sd:EmptyGraphs"));

        if(autoAddToDefaultGraph)
            addQuadMapping(graph, endpoint, createIriMapping("sd:feature"), createIriMapping("sd:UnionDefaultGraph"));

        addQuadMapping(graph, endpoint, createIriMapping("sd:defaultEntailmentRegime"), createIriMapping("ent:Simple"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:supportedLanguage"), createIriMapping("sd:SPARQL11Query"));

        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"),
                createIriMapping("format:SPARQL_Results_XML"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"),
                createIriMapping("format:SPARQL_Results_JSON"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"),
                createIriMapping("format:SPARQL_Results_CSV"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"),
                createIriMapping("format:SPARQL_Results_TSV"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:RDF_JSON"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:RDF_XML"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:Turtle"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:TriG"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:N-Triples"));
        addQuadMapping(graph, endpoint, createIriMapping("sd:resultFormat"), createIriMapping("format:N-Quads"));

        for(FunctionDefinition def : functions.get(serviceIri).values())
        {
            ConstantIriMapping function = createIriMapping(new Iri(def.getFunctionName()));
            addQuadMapping(graph, endpoint, createIriMapping("sd:extensionFunction"), function);
            addQuadMapping(graph, function, createIriMapping("rdf:type"), createIriMapping("sd:Function"));
        }


        Set<String> propertyIris = new HashSet<>();

        for(ProcedureDefinition def : getProcedures(getServiceIri()).values())
        {
            propertyIris.add(def.getProcedureName());

            for(ParameterDefinition parameter : def.getParameters())
                if(!parameter.getParamName().startsWith("#"))
                    propertyIris.add(parameter.getParamName());

            if(!def.isSimple())
                for(ResultDefinition result : def.getResults())
                    propertyIris.add(result.getResultName());
        }

        for(String iri : propertyIris)
        {
            ConstantIriMapping procedure = createIriMapping(new Iri(iri));
            addQuadMapping(graph, endpoint, createIriMapping("sd:propertyFeature"), procedure);
            addQuadMapping(graph, procedure, createIriMapping("rdf:type"), createIriMapping("sd:Feature"));
        }
    }


    /**
     * Parses a column specification: {@code (expression)} is an SQL expression, {@code 'literal'::type} a typed
     * constant, {@code null::type} (in any letter case) a typed NULL constant, anything else a column name.
     *
     * @param value the specification text
     * @return the parsed column
     */
    public static Column getColumn(String value)
    {
        if(value.startsWith("("))
            return new ExpressionColumn(value);
        else if(value.matches("'.*'::[_a-zA-Z0-9.]+"))
            return new ValueColumn(value.replaceFirst("^'(.*)'::[_a-zA-Z0-9.]+", "$1").replaceAll("''", "'"),
                    SqlType.of(value.replaceFirst("^'.*'::([_a-zA-Z0-9.]+)$", "$1")));
        else if(value.matches("(?i)null::[_a-zA-Z0-9.]+"))
            return new NullColumn(SqlType.of(value.replaceFirst("^(?i)null::([_a-zA-Z0-9.]+)$", "$1")));
        else
            return new TableColumn(value);
    }


    /**
     * Parses column specifications, see {@link #getColumn}.
     *
     * @param values the column specifications
     * @return the parsed columns
     */
    public static List<Column> getColumns(String... values)
    {
        List<Column> columns = new ArrayList<>(values.length);

        for(String value : values)
            columns.add(getColumn(value));

        return columns;
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
     * CONSTRUCT query returning the service description graph.
     *
     * @return CONSTRUCT query returning the service description graph
     */
    public String getServiceDescriptionQuery()
    {
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
     * Condition {@code column = v1 OR column = v2 ...} over column specifications (see {@link #getColumn}).
     *
     * @param column the column specification
     * @param values the column specifications
     * @return condition {@code column = v1 OR column = v2 ...} over column specifications (see {@link #getColumn})
     */
    public Conditions createAreEqualCondition(String column, String... values)
    {
        Conditions result = new Conditions(false);

        for(String value : values)
        {
            Condition condition = new Condition();
            condition.addAreEqual(getColumn(column), getColumn(value));
            result.add(condition);
        }

        return result;
    }


    /**
     * Condition {@code column != v1 AND column != v2 ...} over column specifications (see {@link #getColumn}).
     *
     * @param column the column specification
     * @param values the column specifications
     * @return condition {@code column != v1 AND column != v2 ...} over column specifications (see {@link #getColumn})
     */
    public Conditions createAreNotEqualCondition(String column, String... values)
    {
        Condition condition = new Condition();

        for(String value : values)
            condition.addAreNotEqual(getColumn(column), getColumn(value));

        return new Conditions(condition);
    }


    /**
     * Condition {@code column IS NOT NULL}.
     *
     * @param column the column specification
     * @return condition {@code column IS NOT NULL}
     */
    public Conditions createIsNotNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNotNull(getColumn(column));
        return new Conditions(condition);
    }


    /**
     * Condition {@code column IS NULL}.
     *
     * @param column the column specification
     * @return condition {@code column IS NULL}
     */
    public Conditions createIsNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNull(getColumn(column));
        return new Conditions(condition);
    }
}
