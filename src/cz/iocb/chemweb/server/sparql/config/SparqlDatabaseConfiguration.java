package cz.iocb.chemweb.server.sparql.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.JoinTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedBlankNodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.SingleTableQuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.FunctionDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public abstract class SparqlDatabaseConfiguration
{
    private int blankNodeSegment = 1;

    protected DatabaseSchema databaseSchema;
    protected DataSource connectionPool;

    protected HashMap<String, String> prefixes = new HashMap<String, String>();
    protected LinkedHashMap<String, UserIriClass> iriClasses = new LinkedHashMap<String, UserIriClass>();
    protected LinkedHashMap<String, BlankNodeClass> blankNodeClasses = new LinkedHashMap<String, BlankNodeClass>();
    protected List<QuadMapping> mappings = new ArrayList<QuadMapping>();
    protected Set<ConstantIriMapping> graphs = new HashSet<ConstantIriMapping>();
    protected LinkedHashMap<String, ProcedureDefinition> procedures = new LinkedHashMap<String, ProcedureDefinition>();
    protected LinkedHashMap<String, FunctionDefinition> functions = new LinkedHashMap<String, FunctionDefinition>();

    protected boolean strictDefaultGraph = false;


    protected SparqlDatabaseConfiguration(DataSource connectionPool) throws SQLException
    {
        this.connectionPool = connectionPool;
        databaseSchema = new DatabaseSchema(connectionPool);
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


    public void addIriClass(UserIriClass iriClass)
    {
        iriClasses.put(iriClass.getName(), iriClass);
    }


    public void addBlankNodeClass(String name, UserIntBlankNodeClass blankNodeClass)
    {
        blankNodeClasses.put(name, blankNodeClass);
    }


    public void addBlankNodeClass(String name, UserStrBlankNodeClass blankNodeClass)
    {
        blankNodeClasses.put(name, blankNodeClass);
    }


    public UserIntBlankNodeClass getNewIntBlankNodeClass()
    {
        return new UserIntBlankNodeClass(blankNodeSegment++);
    }


    public NodeMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, getColumns(columns));
    }


    public NodeMapping createIriMapping(String iriClassName, String... columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClassName), getColumns(columns));
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
            String[] parts = value.split(":");

            if(parts.length != 2 && !(parts.length == 1 && value.endsWith(":")))
                throw new IllegalArgumentException("invalid iri value: '" + value + "'");

            String prefix = prefixes.get(parts[0]);

            if(prefix == null)
                throw new IllegalArgumentException("unknown prefix '" + parts[0] + "'");

            iri = prefix + (parts.length == 2 ? parts[1] : "");
        }

        IriClass iriClass = null;

        for(ResourceClass c : iriClasses.values())
        {
            if(c instanceof UserIriClass && ((UserIriClass) c).match(iri, connectionPool))
            {
                if(iriClass != null)
                    throw new RuntimeException("ambigous iri class for " + value);

                iriClass = (UserIriClass) c;
            }
        }

        assert iriClass != null;

        if(iriClass == null)
            iriClass = BuiltinClasses.unsupportedIri;

        return new ConstantIriMapping(iriClass, new IRI(iri));
    }


    public NodeMapping createBlankNodeMapping(BlankNodeClass blankNodeClass, String... columns)
    {
        return new ParametrisedBlankNodeMapping(blankNodeClass, getColumns(columns));
    }


    public NodeMapping createBlankNodeMapping(String blankNodeClassName, String... columns)
    {
        return new ParametrisedBlankNodeMapping(getBlankNodeClass(blankNodeClassName), getColumns(columns));
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
        return new ConstantLiteralMapping(BuiltinClasses.xsdString, new Literal(value, BuiltinTypes.xsdStringIri));
    }


    public NodeMapping createLiteralMapping(boolean value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdBoolean,
                new Literal(Boolean.toString(value), BuiltinTypes.xsdBooleanIri));
    }


    public NodeMapping createLiteralMapping(short value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdShort,
                new Literal(Short.toString(value), BuiltinTypes.xsdShortIri));
    }


    public NodeMapping createLiteralMapping(int value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdInt,
                new Literal(Integer.toString(value), BuiltinTypes.xsdIntIri));
    }


    public NodeMapping createLiteralMapping(long value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdLong,
                new Literal(Long.toString(value), BuiltinTypes.xsdLongIri));
    }


    public NodeMapping createLiteralMapping(float value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdFloat,
                new Literal(Float.toString(value), BuiltinTypes.xsdFloatIri));
    }


    public NodeMapping createLiteralMapping(double value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdDouble,
                new Literal(Double.toString(value), BuiltinTypes.xsdDoubleIri));
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        mappings.add(new SingleTableQuadMapping(table, graph, subject, predicate, object));

        if(graph != null)
            graphs.add(graph);
    }


    public void addQuadMapping(Table table, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, String condition)
    {
        mappings.add(new SingleTableQuadMapping(table, graph, subject, predicate, object, condition));

        if(graph != null)
            graphs.add(graph);
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        mappings.add(new JoinTableQuadMapping(subjectTable, objectTable,
                Arrays.asList(new TableColumn(subjectTableJoinColumn)),
                Arrays.asList(new TableColumn(objectTableJoinColumn)), graph, subject, predicate, object));

        if(graph != null)
            graphs.add(graph);
    }


    public void addQuadMapping(Table subjectTable, Table objectTable, String subjectTableJoinColumn,
            String objectTableJoinColumn, ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, String subjectCondition, String objectCondition)
    {
        mappings.add(new JoinTableQuadMapping(subjectTable, objectTable,
                Arrays.asList(new TableColumn(subjectTableJoinColumn)),
                Arrays.asList(new TableColumn(objectTableJoinColumn)), graph, subject, predicate, object,
                subjectCondition, objectCondition));

        if(graph != null)
            graphs.add(graph);
    }


    public void addProcedure(ProcedureDefinition procedure)
    {
        procedures.put(procedure.getProcedureName(), procedure);
    }


    public void addFunction(FunctionDefinition function)
    {
        functions.put(function.getFunctionName(), function);
    }


    public static List<Column> getColumns(String[] values)
    {
        List<Column> columns = new ArrayList<Column>(values.length);

        for(String value : values)
        {
            if(value.startsWith("("))
                columns.add(new ExpressionColumn(value));
            if(value.matches(".*::[_a-zA-Z0-9]+"))
                columns.add(new ConstantColumn(value));
            else
                columns.add(new TableColumn(value));
        }

        return columns;
    }


    public UserIriClass getIriClass(String name)
    {
        UserIriClass iriClass = iriClasses.get(name);

        if(iriClass == null)
            throw new IllegalArgumentException("unknown iri class: '" + name + "'");

        return iriClass;
    }


    public BlankNodeClass getBlankNodeClass(String name)
    {
        BlankNodeClass blankNodeClass = blankNodeClasses.get(name);

        if(blankNodeClass == null)
            throw new IllegalArgumentException("unknown blank node class: '" + name + "'");

        return blankNodeClass;
    }


    public HashMap<String, String> getPrefixes()
    {
        return prefixes;
    }


    public LinkedHashMap<String, UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    public List<QuadMapping> getMappings()
    {
        return mappings;
    }


    public Set<ConstantIriMapping> getGraphs()
    {
        return graphs;
    }


    public LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return procedures;
    }


    public LinkedHashMap<String, FunctionDefinition> getFunctions()
    {
        return functions;
    }


    public DatabaseSchema getDatabaseSchema()
    {
        return databaseSchema;
    }


    public DataSource getConnectionPool()
    {
        return connectionPool;
    }


    public boolean isStrictDefaultGraph()
    {
        return strictDefaultGraph;
    }


    public void setStrictDefaultGraph(boolean strict)
    {
        strictDefaultGraph = strict;
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
}
