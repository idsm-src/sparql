package cz.iocb.chemweb.server.sparql.config;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdDateTimeType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdDateType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdDayTimeDurationType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdDecimalType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdDoubleType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdFloatType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdIntType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdLongType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdShortType;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdStringType;
import static java.util.Arrays.asList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Condition;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedBlankNodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.SingleTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DataType;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.FunctionDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public abstract class SparqlDatabaseConfiguration
{
    private final IRI serviceIri;
    private final IRI descriptionGraphIri;

    protected DatabaseSchema databaseSchema;
    protected DataSource connectionPool;
    protected boolean autoAddToDefaultGraph;

    protected HashMap<String, String> prefixes = new HashMap<String, String>();
    protected HashMap<IRI, DataType> dataTypeMap = new HashMap<IRI, DataType>();
    protected ArrayList<UserIriClass> iriClasses = new ArrayList<UserIriClass>();
    protected HashMap<String, UserIriClass> iriClassMap = new HashMap<String, UserIriClass>();

    private final List<IRI> services = new ArrayList<IRI>();
    protected HashMap<IRI, List<QuadMapping>> mappings = new HashMap<IRI, List<QuadMapping>>();
    protected HashMap<IRI, HashSet<ConstantIriMapping>> graphs = new HashMap<IRI, HashSet<ConstantIriMapping>>();
    protected HashMap<IRI, HashMap<String, ProcedureDefinition>> procedures = new HashMap<IRI, HashMap<String, ProcedureDefinition>>();
    protected HashMap<IRI, HashMap<String, FunctionDefinition>> functions = new HashMap<IRI, HashMap<String, FunctionDefinition>>();


    protected SparqlDatabaseConfiguration(String service, DataSource connectionPool, DatabaseSchema schema,
            boolean autoAddToDefaultGraph) throws SQLException
    {
        IRI serviceIri = service == null ? null : new IRI(service);
        IRI descriptionGraphIri = service == null ? null : new IRI(service + "#ServiceDescription");

        this.serviceIri = serviceIri;
        this.descriptionGraphIri = descriptionGraphIri;
        this.connectionPool = connectionPool;
        this.databaseSchema = schema;
        this.autoAddToDefaultGraph = autoAddToDefaultGraph;

        addEmptyService(serviceIri);

        addDataType(xsdBooleanType);
        addDataType(xsdShortType);
        addDataType(xsdIntType);
        addDataType(xsdLongType);
        addDataType(xsdIntegerType);
        addDataType(xsdDecimalType);
        addDataType(xsdFloatType);
        addDataType(xsdDoubleType);
        addDataType(xsdStringType);
        addDataType(xsdDayTimeDurationType);
        addDataType(xsdDateType);
        addDataType(xsdDateTimeType);
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


    public void addDataType(DataType dataType)
    {
        DataType previous = dataTypeMap.get(dataType.getTypeIri());

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


    public NodeMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, getColumns(columns));
    }


    public NodeMapping createIriMapping(IriClass iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(iriClass, columns);
    }


    public NodeMapping createIriMapping(String iriClass, List<Column> columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClass), columns);
    }


    public NodeMapping createIriMapping(String iriClassName, String... columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClassName), getColumns(columns));
    }


    public ConstantIriMapping createIriMapping(IRI iri)
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

        return new ConstantIriMapping(new IRI(iri));
    }


    public NodeMapping createBlankNodeMapping(BlankNodeClass blankNodeClass, String... columns)
    {
        return new ParametrisedBlankNodeMapping(blankNodeClass, getColumns(columns));
    }


    public NodeMapping createLiteralMapping(LiteralClass literalClass, String... columns)
    {
        return new ParametrisedLiteralMapping(literalClass, getColumns(columns));
    }


    public NodeMapping createLiteralMapping(LiteralClass literalClass, Literal literal)
    {
        return new ConstantLiteralMapping(literalClass, literal);
    }


    public NodeMapping createLiteralMapping(String value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdString, new Literal(value, xsdStringType));
    }


    public NodeMapping createLiteralMapping(boolean value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdBoolean,
                new Literal(Boolean.toString(value), xsdBooleanType));
    }


    public NodeMapping createLiteralMapping(short value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdShort, new Literal(Short.toString(value), xsdShortType));
    }


    public NodeMapping createLiteralMapping(int value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdInt, new Literal(Integer.toString(value), xsdIntType));
    }


    public NodeMapping createLiteralMapping(long value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdLong, new Literal(Long.toString(value), xsdLongType));
    }


    public NodeMapping createLiteralMapping(float value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdFloat, new Literal(Float.toString(value), xsdFloatType));
    }


    public NodeMapping createLiteralMapping(double value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdDouble, new Literal(Double.toString(value), xsdDoubleType));
    }


    protected void addEmptyService(IRI service)
    {
        services.add(service);
        mappings.put(service, new ArrayList<QuadMapping>());
        graphs.put(service, new HashSet<ConstantIriMapping>());
        procedures.put(service, new HashMap<String, ProcedureDefinition>());
        functions.put(service, new HashMap<String, FunctionDefinition>());
    }


    private void addQuadMapping(IRI service, QuadMapping mapping)
    {
        if(!services.contains(service))
            addEmptyService(service);

        if(mappings.get(service).contains(mapping))
            return;

        mappings.get(service).add(mapping);

        if(mapping.getGraph() != null)
            graphs.get(service).add(mapping.getGraph());
    }


    private void addProcedure(IRI service, ProcedureDefinition procedure)
    {
        if(!services.contains(service))
            addEmptyService(service);

        procedures.get(service).put(procedure.getProcedureName(), procedure);
    }


    private void addFunction(IRI service, FunctionDefinition function)
    {
        if(!services.contains(service))
            addEmptyService(service);

        functions.get(service).put(function.getFunctionName(), function);
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        mappings.get(serviceIri).add(new SingleTableQuadMapping(table, graph, subject, predicate, object));

        if(graph != null)
            graphs.get(serviceIri).add(graph);

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri).add(new SingleTableQuadMapping(table, null, subject, predicate, object));
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, Condition conditions)
    {
        mappings.get(serviceIri).add(new SingleTableQuadMapping(table, graph, subject, predicate, object, conditions));

        if(graph != null)
            graphs.get(serviceIri).add(graph);

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri)
                    .add(new SingleTableQuadMapping(table, null, subject, predicate, object, conditions));
    }


    public void addQuadMapping(ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        addQuadMapping(null, graph, subject, predicate, object);
    }


    public void addQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object)
    {
        mappings.get(serviceIri)
                .add(new JoinTableQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object));

        if(graph != null)
            graphs.get(serviceIri).add(graph);

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri)
                    .add(new JoinTableQuadMapping(tables, joinColumnsPairs, null, subject, predicate, object));
    }


    public void addQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object, List<Condition> conditions)
    {
        mappings.get(serviceIri)
                .add(new JoinTableQuadMapping(tables, joinColumnsPairs, graph, subject, predicate, object, conditions));

        if(graph != null)
            graphs.get(serviceIri).add(graph);

        if(graph != null && autoAddToDefaultGraph)
            mappings.get(serviceIri).add(
                    new JoinTableQuadMapping(tables, joinColumnsPairs, null, subject, predicate, object, conditions));
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        addQuadMapping(asList(subjectTable, objectTable), asList(
                new JoinColumns(new TableColumn(subjectTableJoinColumn), new TableColumn(objectTableJoinColumn))),
                graph, subject, predicate, object);
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, Condition subjectCondition, Condition objectCondition)
    {
        addQuadMapping(asList(subjectTable, objectTable),
                asList(new JoinColumns(new TableColumn(subjectTableJoinColumn),
                        new TableColumn(objectTableJoinColumn))),
                graph, subject, predicate, object, asList(subjectCondition, objectCondition));
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

        for(UserIriClass iriClass : other.getIriClasses())
            addIriClass(iriClass);

        for(IRI service : other.getServices())
        {
            IRI target = service == null && merge ? getServiceIri() : service;

            for(QuadMapping original : other.getMappings(service))
            {
                if(original instanceof SingleTableQuadMapping)
                {
                    SingleTableQuadMapping map = (SingleTableQuadMapping) original;

                    SingleTableQuadMapping mapping = new SingleTableQuadMapping(map.getTable(),
                            (ConstantIriMapping) remap(map.getGraph()), remap(map.getSubject()),
                            (ConstantIriMapping) remap(map.getPredicate()), remap(map.getObject()), map.getCondition());

                    addQuadMapping(target, mapping);
                }
                else if(original instanceof JoinTableQuadMapping)
                {
                    JoinTableQuadMapping map = (JoinTableQuadMapping) original;

                    JoinTableQuadMapping mapping = new JoinTableQuadMapping(map.getTables(), map.getJoinColumnsPairs(),
                            (ConstantIriMapping) remap(map.getGraph()), remap(map.getSubject()),
                            (ConstantIriMapping) remap(map.getPredicate()), remap(map.getObject()),
                            map.getConditions());

                    addQuadMapping(target, mapping);
                }
                else
                {
                    throw new IllegalArgumentException();
                }
            }

            for(Entry<String, ProcedureDefinition> entry : other.getProcedures(service).entrySet())
            {
                ProcedureDefinition original = entry.getValue();
                ProcedureDefinition definition = new ProcedureDefinition(original.getProcedureName(),
                        original.getSqlProcedure());

                for(ParameterDefinition parameter : original.getParameters())
                    definition.addParameter(new ParameterDefinition(parameter.getParamName(),
                            remap(parameter.getParameterClass()), parameter.getDefaultValue()));

                for(ResultDefinition result : original.getResults())
                    definition.addResult(new ResultDefinition(result.getResultName(), remap(result.getResultClass()),
                            result.getSqlTypeFields()));

                addProcedure(target, definition);
            }

            for(Entry<String, FunctionDefinition> entry : other.getFunctions(service).entrySet())
            {
                FunctionDefinition original = entry.getValue();

                List<ResourceClass> arguments = new ArrayList<ResourceClass>(original.getArgumentClasses().size());

                for(ResourceClass argument : original.getArgumentClasses())
                    arguments.add(remap(argument));

                FunctionDefinition definition = new FunctionDefinition(original.getFunctionName(),
                        original.getSqlFunction(), remap(original.getResultClass()), arguments, original.canBeNull(),
                        original.isDeterministic());

                addFunction(target, definition);
            }
        }
    }


    public void addServiceDescription()
    {
        //FIXME: Code depends on prefix definitions.

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
            ConstantIriMapping function = createIriMapping(new IRI(def.getFunctionName()));
            addQuadMapping(graph, endpoint, createIriMapping("sd:extensionFunction"), function);
            addQuadMapping(graph, function, createIriMapping("rdf:type"), createIriMapping("sd:Function"));
        }

        for(ProcedureDefinition def : procedures.get(serviceIri).values())
        {
            ConstantIriMapping procedure = createIriMapping(new IRI(def.getProcedureName()));
            addQuadMapping(graph, endpoint, createIriMapping("sd:propertyFeature"), procedure);
            addQuadMapping(graph, procedure, createIriMapping("rdf:type"), createIriMapping("sd:Feature"));
        }

        //FIXME: use blank node
        ConstantIriMapping defaultDataset = createIriMapping("<" + serviceIri.getValue() + "#DefaultDataset>");
        ConstantIriMapping defaultGraph = createIriMapping("<" + serviceIri.getValue() + "#DefaultGraph>");

        addQuadMapping(graph, endpoint, createIriMapping("sd:defaultDataset"), defaultDataset);
        addQuadMapping(graph, defaultDataset, createIriMapping("rdf:type"), createIriMapping("sd:Dataset"));

        addQuadMapping(graph, defaultDataset, createIriMapping("sd:defaultGraph"), defaultGraph);
        addQuadMapping(graph, defaultGraph, createIriMapping("rdf:type"), createIriMapping("sd:Graph"));

        for(ConstantIriMapping namedGraph : graphs.get(serviceIri))
        {
            addQuadMapping(graph, defaultDataset, createIriMapping("sd:namedGraph"), namedGraph);

            addQuadMapping(graph, namedGraph, createIriMapping("rdf:type"), createIriMapping("sd:NamedGraph"));
            addQuadMapping(graph, namedGraph, createIriMapping("rdf:name"), namedGraph);
            addQuadMapping(graph, namedGraph, createIriMapping("sd:entailmentRegime"), createIriMapping("ent:Simple"));

            //FIXME: use blank node
            String iri = ((IRI) namedGraph.getValue()).getValue();
            ConstantIriMapping namedGraphGraph = createIriMapping(
                    "<" + iri + (iri.contains("#") ? "" : "#") + "Graph>");
            addQuadMapping(graph, namedGraph, createIriMapping("sd:graph"), namedGraphGraph);
            addQuadMapping(graph, namedGraphGraph, createIriMapping("rdf:type"), createIriMapping("sd:Graph"));
        }
    }


    public static Column getColumn(String value)
    {
        if(value.startsWith("("))
            return new ExpressionColumn(value);
        else if(value.matches(".*::[_a-zA-Z0-9.]+"))
            return new ConstantColumn(value);
        else
            return new TableColumn(value);
    }


    public static List<Column> getColumns(String[] values)
    {
        List<Column> columns = new ArrayList<Column>(values.length);

        for(String value : values)
            columns.add(getColumn(value));

        return columns;
    }


    public IRI getServiceIri()
    {
        return serviceIri;
    }


    public IRI getDescriptionGraphIri()
    {
        return descriptionGraphIri;
    }


    public List<IRI> getServices()
    {
        return services;
    }


    public HashMap<String, String> getPrefixes()
    {
        return prefixes;
    }


    public Collection<DataType> getDataTypes()
    {
        return dataTypeMap.values();
    }


    public DataType getDataType(IRI iri)
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


    public List<QuadMapping> getMappings(IRI iri)
    {
        return mappings.get(iri);
    }


    public Set<ConstantIriMapping> getGraphs(IRI iri)
    {
        return graphs.get(iri);
    }


    public HashMap<String, ProcedureDefinition> getProcedures(IRI iri)
    {
        return procedures.get(iri);
    }


    public HashMap<String, FunctionDefinition> getFunctions(IRI iri)
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


    protected void setConstraints() throws SQLException
    {
        try(Connection connection = getConnectionPool().getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery(
                        "select parent_schema, parent_table, parent_columns, foreign_schema, foreign_table, foreign_columns "
                                + "from constraints.foreign_keys"))
                {
                    while(result.next())
                    {
                        Table parentTable = new Table(result.getString(1), result.getString(2));
                        List<Column> parentColumns = getColumns((String[]) result.getArray(3).getArray());

                        Table foreignTable = new Table(result.getString(4), result.getString(5));
                        List<Column> foreignColumns = getColumns((String[]) result.getArray(6).getArray());

                        databaseSchema.addForeignKeys(parentTable, parentColumns, foreignTable, foreignColumns);
                    }
                }
            }


            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery(
                        "select left_schema, left_table, left_columns, right_schema, right_table, right_columns "
                                + "from constraints.unjoinable_columns"))
                {
                    while(result.next())
                    {
                        Table leftTable = new Table(result.getString(1), result.getString(2));
                        List<Column> leftColumns = getColumns((String[]) result.getArray(3).getArray());

                        Table rightTable = new Table(result.getString(4), result.getString(5));
                        List<Column> rightColumns = getColumns((String[]) result.getArray(6).getArray());

                        databaseSchema.addUnjoinableColumns(leftTable, leftColumns, rightTable, rightColumns);
                        databaseSchema.addUnjoinableColumns(rightTable, rightColumns, leftTable, leftColumns);
                    }
                }
            }
        }
    }


    private ResourceClass remap(ResourceClass resourceClass)
    {
        if(resourceClass instanceof UserIriClass)
            return getIriClass(resourceClass.getName());

        return resourceClass;
    }


    private NodeMapping remap(NodeMapping mapping)
    {
        if(mapping instanceof ConstantIriMapping)
        {
            ConstantIriMapping original = (ConstantIriMapping) mapping;
            return createIriMapping(((IRI) original.getValue()).toString());
        }

        if(mapping instanceof ParametrisedIriMapping)
        {
            ParametrisedIriMapping original = (ParametrisedIriMapping) mapping;
            return new ParametrisedIriMapping((IriClass) remap(original.getResourceClass()), original.getColumns());
        }

        return mapping;
    }


    public Condition createAreEqualCondition(String column, String value)
    {
        Condition conditions = new Condition();
        conditions.addAreEqual(getColumn(column), getColumn(value));
        return conditions;
    }


    public Condition createAreNotEqualCondition(String column, String... values)
    {
        Condition conditions = new Condition();

        for(String value : values)
            conditions.addAreNotEqual(getColumn(column), getColumn(value));

        return conditions;
    }


    public Condition createIsNotNullCondition(String column)
    {
        Condition conditions = new Condition();
        conditions.addIsNotNull(getColumn(column));
        return conditions;
    }


    public Condition createIsNullCondition(String column)
    {
        Condition conditions = new Condition();
        conditions.addIsNull(getColumn(column));
        return conditions;
    }
}
