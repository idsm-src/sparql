package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;
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
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedLiteralMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
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



public class SparqlDatabaseConfiguration
{
    protected final Iri serviceIri;
    protected final Iri descriptionGraphIri;

    protected DatabaseSchema databaseSchema;
    protected DataSource connectionPool;
    protected boolean autoAddToDefaultGraph;

    protected Map<String, String> prefixes = new HashMap<>();
    protected Map<Iri, Datatype> dataTypeMap = new HashMap<>();
    protected List<UserIriClass> iriClasses = new ArrayList<>();
    protected Map<String, UserIriClass> iriClassMap = new HashMap<>();

    private final List<Iri> services = new ArrayList<>();
    protected Map<Iri, Set<Iri>> graphs = new HashMap<>();
    protected Map<Iri, List<QuadMapping>> mappings = new HashMap<>();
    protected Map<Iri, Map<String, ProcedureDefinition>> procedures = new HashMap<>();
    protected Map<Iri, Map<String, FunctionDefinition>> functions = new HashMap<>();

    protected final IriCache iriCache = new IriCache(10000);


    public SparqlDatabaseConfiguration(String service, String descriptionGraph, DataSource connectionPool,
            DatabaseSchema schema, boolean autoAddToDefaultGraph) throws SQLException
    {
        Iri serviceIri = service != null ? new Iri(service) : null;
        Iri descriptionGraphIri = descriptionGraph != null ? new Iri(descriptionGraph) :
                service != null ? new Iri(service + "#ServiceDescription") : null;

        this.serviceIri = serviceIri;
        this.descriptionGraphIri = descriptionGraphIri;
        this.connectionPool = connectionPool;
        this.databaseSchema = schema;
        this.autoAddToDefaultGraph = autoAddToDefaultGraph;

        addEmptyService(serviceIri);

        addDatatype(xsdBooleanType);
        addDatatype(xsdShortType);
        addDatatype(xsdIntType);
        addDatatype(xsdLongType);
        addDatatype(xsdIntegerType);
        addDatatype(xsdDecimalType);
        addDatatype(xsdFloatType);
        addDatatype(xsdDoubleType);
        addDatatype(xsdStringType);
        addDatatype(xsdDayTimeDurationType);
        addDatatype(xsdDateType);
        addDatatype(xsdDateTimeType);
        addDatatype(rdfLangStringType);
    }


    public SparqlDatabaseConfiguration(String service, String descriptionGraph, DataSource connectionPool,
            DatabaseSchema schema) throws SQLException
    {
        this(service, descriptionGraph, connectionPool, schema, true);
    }


    public SparqlDatabaseConfiguration(String service, DataSource connectionPool, DatabaseSchema schema,
            boolean autoAddToDefaultGraph) throws SQLException
    {
        this(service, null, connectionPool, schema, autoAddToDefaultGraph);
    }


    protected SparqlDatabaseConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        this(service, connectionPool, schema, true);
    }


    public void addPrefix(String prefix, String iri)
    {
        String previous = prefixes.get(prefix);

        if(previous == null)
            prefixes.put(prefix, iri);
        else if(!previous.equals(iri))
            throw new IllegalArgumentException(
                    "prefix definition conflict: " + prefix + ": " + iri + " != " + previous);
    }


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


    public void addIriClass(UserIriClass iriClass)
    {
        UserIriClass previous = iriClassMap.get(iriClass.getName());

        if(previous == null)
        {
            int possition = (int) iriClasses.stream().filter(c -> c.getCheckCost() <= iriClass.getCheckCost()).count();
            iriClasses.add(possition, iriClass);
            iriClassMap.put(iriClass.getName(), iriClass);
        }
        else if(!previous.equals(iriClass))
        {
            throw new IllegalArgumentException(
                    "resource class definition conflict for iri class '" + iriClass.getName() + "'");
        }
    }


    public TermMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, getColumns(columns));
    }


    public TermMapping createIriMapping(IriClass iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(iriClass, columns);
    }


    public TermMapping createIriMapping(String iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClass), columns);
    }


    public TermMapping createIriMapping(String iriClassName, String... columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClassName), getColumns(columns));
    }


    public ConstantIriMapping createIriMapping(Iri iri)
    {
        return new ConstantIriMapping(iri);
    }


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


    public TermMapping createBlankNodeMapping(BlankNodeClass blankNodeClass, String... columns)
    {
        return new ParametrisedBlankNodeMapping(blankNodeClass, getColumns(columns));
    }


    public TermMapping createLiteralMapping(LiteralClass literalClass, String... columns)
    {
        return new ParametrisedLiteralMapping(literalClass, getColumns(columns));
    }


    public TermMapping createLiteralMapping(LiteralClass literalClass, Literal literal)
    {
        return new ConstantLiteralMapping(literalClass, literal);
    }


    public TermMapping createLiteralMapping(String value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdString, new TypedLiteral(value, xsdStringIri));
    }


    public TermMapping createLiteralMapping(boolean value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdBoolean,
                new TypedLiteral(Boolean.toString(value), xsdBooleanIri));
    }


    public TermMapping createLiteralMapping(short value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdShort,
                new TypedLiteral(Short.toString(value), xsdShortIri));
    }


    public TermMapping createLiteralMapping(int value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdInt, new TypedLiteral(Integer.toString(value), xsdIntIri));
    }


    public TermMapping createLiteralMapping(long value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdLong, new TypedLiteral(Long.toString(value), xsdLongIri));
    }


    public TermMapping createLiteralMapping(float value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdFloat,
                new TypedLiteral(Float.toString(value), xsdFloatIri));
    }


    public TermMapping createLiteralMapping(double value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdDouble,
                new TypedLiteral(Double.toString(value), xsdDoubleIri));
    }


    protected void addEmptyService(Iri service)
    {
        services.add(service);
        mappings.put(service, new ArrayList<>());
        graphs.put(service, new HashSet<>());
        procedures.put(service, new HashMap<>());
        functions.put(service, new HashMap<>());
    }


    private void addQuadMapping(Iri service, QuadMapping mapping)
    {
        if(!services.contains(service))
            addEmptyService(service);

        if(mappings.get(service).contains(mapping))
            return;

        mappings.get(service).add(mapping);

        if(mapping.getGraph() != null)
            graphs.get(service).add((Iri) mapping.getGraph().getValue());
    }


    private void addProcedure(Iri service, ProcedureDefinition procedure)
    {
        if(!services.contains(service))
            addEmptyService(service);

        procedures.get(service).put(procedure.getProcedureName(), procedure);
    }


    private void addFunction(Iri service, FunctionDefinition function)
    {
        if(!services.contains(service))
            addEmptyService(service);

        functions.get(service).put(function.getFunctionName(), function);
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object, Conditions conditions)
    {
        mappings.get(serviceIri).add(new SingleTableQuadMapping(table, graph, subject, predicate, object, conditions));

        if(graph != null)
            graphs.get(serviceIri).add((Iri) graph.getValue());

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri)
                    .add(new SingleTableQuadMapping(table, null, subject, predicate, object, conditions));
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object)
    {
        addQuadMapping(table, graph, subject, predicate, object, new Conditions(true));
    }


    public void addQuadMapping(ConstantIriMapping graph, TermMapping subject, ConstantIriMapping predicate,
            TermMapping object)
    {
        addQuadMapping(null, graph, subject, predicate, object);
    }


    public void addQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object, List<Conditions> conditions)
    {
        mappings.get(serviceIri)
                .add(new JoinTableQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object, conditions));

        if(graph != null)
            graphs.get(serviceIri).add((Iri) graph.getValue());

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri).add(
                    new JoinTableQuadMapping(tables, joinColumnsPairs, null, subject, predicate, object, conditions));
    }


    public void addQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            TermMapping subject, ConstantIriMapping predicate, TermMapping object)
    {
        addQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object,
                Collections.nCopies(tables.size(), new Conditions(true)));
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, String type, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object)
    {
        addQuadMapping(List.of(subjectTable, objectTable), List.of(
                new JoinColumns(new TableColumn(subjectTableJoinColumn), new TableColumn(objectTableJoinColumn), type)),
                graph, subject, predicate, object);
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, String type, ConstantIriMapping graph, TermMapping subject,
            ConstantIriMapping predicate, TermMapping object, Conditions subjectCondition, Conditions objectCondition)
    {
        addQuadMapping(
                List.of(subjectTable, objectTable), List.of(new JoinColumns(new TableColumn(subjectTableJoinColumn),
                        new TableColumn(objectTableJoinColumn), type)),
                graph, subject, predicate, object, List.of(subjectCondition, objectCondition));
    }


    public void addProcedure(ProcedureDefinition procedure)
    {
        procedures.get(serviceIri).put(procedure.getProcedureName(), procedure);
    }


    public void addFunction(FunctionDefinition function)
    {
        functions.get(serviceIri).put(function.getFunctionName(), function);
    }


    public void addService(SparqlDatabaseConfiguration other, boolean merge)
    {
        if(merge)
            for(Entry<String, String> entry : other.getPrefixes().entrySet())
                addPrefix(entry.getKey(), entry.getValue());

        for(Datatype dataType : other.getDatatypes())
            addDatatype(dataType);

        for(UserIriClass iriClass : other.getIriClasses())
            addIriClass(iriClass);

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


    public void addDatasetServiceDescription()
    {
        //FIXME: code depends on prefix definitions

        ConstantIriMapping graph = createIriMapping(descriptionGraphIri);
        ConstantIriMapping endpoint = createIriMapping(serviceIri);

        //FIXME: use blank node
        ConstantIriMapping defaultDataset = createIriMapping("<" + serviceIri.getValue() + "#default-dataset>");
        ConstantIriMapping availableGraphs = createIriMapping("<" + serviceIri.getValue() + "#available-graphs>");
        ConstantIriMapping defaultGraph = createIriMapping("<" + serviceIri.getValue() + "#DefaultGraph>");

        addQuadMapping(graph, endpoint, createIriMapping("sd:availableGraphs"), availableGraphs);
        addQuadMapping(graph, availableGraphs, createIriMapping("rdf:type"), createIriMapping("sd:GraphCollection"));

        addQuadMapping(graph, endpoint, createIriMapping("sd:defaultDataset"), defaultDataset);
        addQuadMapping(graph, defaultDataset, createIriMapping("rdf:type"), createIriMapping("sd:Dataset"));

        addQuadMapping(graph, defaultDataset, createIriMapping("sd:defaultGraph"), defaultGraph);
        addQuadMapping(graph, defaultGraph, createIriMapping("rdf:type"), createIriMapping("sd:Graph"));

        for(Iri namedGraph : graphs.get(serviceIri))
        {
            ConstantIriMapping subject = new ConstantIriMapping(namedGraph);

            addQuadMapping(graph, defaultDataset, createIriMapping("sd:namedGraph"), subject);

            addQuadMapping(graph, subject, createIriMapping("rdf:type"), createIriMapping("sd:NamedGraph"));
            addQuadMapping(graph, subject, createIriMapping("rdf:name"), subject);
            addQuadMapping(graph, subject, createIriMapping("sd:entailmentRegime"), createIriMapping("ent:Simple"));

            //FIXME: use blank node
            String iri = ((Iri) subject.getValue()).getValue();
            ConstantIriMapping namedGraphGraph = createIriMapping(
                    "<" + iri + (iri.contains("#") ? "" : "#") + "Graph>");
            addQuadMapping(graph, subject, createIriMapping("sd:graph"), namedGraphGraph);
            addQuadMapping(graph, namedGraphGraph, createIriMapping("rdf:type"), createIriMapping("sd:Graph"));
        }
    }


    public static Column getColumn(String value)
    {
        if(value.startsWith("("))
            return new ExpressionColumn(value);
        else if(value.matches("'.*'::[_a-zA-Z0-9.]+"))
            return new ConstantColumn(value.replaceFirst("^'(.*)'::[_a-zA-Z0-9.]+", "$1").replaceAll("''", "'"),
                    value.replaceFirst("^'.*'::([_a-zA-Z0-9.]+)$", "$1"));
        else
            return new TableColumn(value);
    }


    public static List<Column> getColumns(String... values)
    {
        List<Column> columns = new ArrayList<>(values.length);

        for(String value : values)
            columns.add(getColumn(value));

        return columns;
    }


    public Iri getServiceIri()
    {
        return serviceIri;
    }


    public String getServiceDescriptionQuery()
    {
        return "construct {?s ?p ?o} where { graph " + descriptionGraphIri + " {?s ?p ?o}}";
    }


    public List<Iri> getServices()
    {
        return services;
    }


    public Map<String, String> getPrefixes()
    {
        return prefixes;
    }


    public Collection<Datatype> getDatatypes()
    {
        return dataTypeMap.values();
    }


    public Datatype getDatatype(Iri iri)
    {
        return dataTypeMap.get(iri);
    }


    public List<UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    public UserIriClass getIriClass(String name)
    {
        UserIriClass iriClass = iriClassMap.get(name);

        if(iriClass == null)
            throw new IllegalArgumentException("unknown iri class: '" + name + "'");

        return iriClass;
    }


    public List<QuadMapping> getMappings(Iri iri)
    {
        return mappings.get(iri);
    }


    public Set<Iri> getGraphs(Iri iri)
    {
        return graphs.get(iri);
    }


    public Map<String, ProcedureDefinition> getProcedures(Iri iri)
    {
        return procedures.get(iri);
    }


    public Map<String, FunctionDefinition> getFunctions(Iri iri)
    {
        return functions.get(iri);
    }


    public DatabaseSchema getDatabaseSchema()
    {
        return databaseSchema;
    }


    public DataSource getConnectionPool()
    {
        return connectionPool;
    }


    public final IriCache getIriCache()
    {
        return iriCache;
    }


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


    public Conditions createAreNotEqualCondition(String column, String... values)
    {
        Condition condition = new Condition();

        for(String value : values)
            condition.addAreNotEqual(getColumn(column), getColumn(value));

        return new Conditions(condition);
    }


    public Conditions createIsNotNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNotNull(getColumn(column));
        return new Conditions(condition);
    }


    public Conditions createIsNullCondition(String column)
    {
        Condition condition = new Condition();
        condition.addIsNull(getColumn(column));
        return new Conditions(condition);
    }
}
