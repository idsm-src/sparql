package cz.iocb.chemweb.server.sparql.translator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.postgresql.PostgresSchema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;



public abstract class SparqlDatabaseConfiguration
{
    protected DatabaseSchema schema;
    protected DataSource connectionPool;

    protected HashMap<String, String> prefixes = new HashMap<String, String>();
    protected LinkedHashMap<String, UserIriClass> iriClasses = new LinkedHashMap<String, UserIriClass>();
    protected List<QuadMapping> mappings = new ArrayList<QuadMapping>();
    protected LinkedHashMap<String, ProcedureDefinition> procedures = new LinkedHashMap<String, ProcedureDefinition>();


    protected SparqlDatabaseConfiguration(DataSource connectionPool) throws SQLException
    {
        this.connectionPool = connectionPool;
        schema = new PostgresSchema(connectionPool);
    }


    public void addIriClass(UserIriClass iriClass)
    {
        iriClasses.put(iriClass.getName(), iriClass);
    }


    public NodeMapping createIriMapping(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, Arrays.asList(columns));
    }


    public NodeMapping createIriMapping(String iriClassName, String... columns)
    {
        return new ParametrisedIriMapping(getIriClass(iriClassName), Arrays.asList(columns));
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
            iri = prefixes.get(parts[0]) + (parts.length > 1 ? parts[1] : "");
        }

        IriClass iriClass = null;

        for(ResourceClass c : iriClasses.values())
        {
            if(c instanceof UserIriClass && ((UserIriClass) c).match(iri))
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


    public NodeMapping createLiteralMapping(LiteralClass literalClass, String... column)
    {
        return new ParametrisedLiteralMapping(literalClass, Arrays.asList(column));
    }


    public NodeMapping createLiteralMapping(LiteralClass literalClass, Literal literal)
    {
        return new ConstantLiteralMapping(literalClass, literal);
    }


    public NodeMapping createLiteralMapping(String value)
    {
        return new ConstantLiteralMapping(BuiltinClasses.xsdString, new Literal(value, BuiltinTypes.xsdStringIri));
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


    public void addQuadMapping(String table, NodeMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        QuadMapping map = new QuadMapping(table, graph, subject, predicate, object);
        mappings.add(map);
    }


    public void addQuadMapping(String table, NodeMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, String condition)
    {
        QuadMapping map = new QuadMapping(table, graph, subject, predicate, object, condition);
        mappings.add(map);
    }


    public UserIriClass getIriClass(String name)
    {
        return iriClasses.get(name);
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


    public LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return procedures;
    }


    public DatabaseSchema getSchema()
    {
        return schema;
    }


    public DataSource getConnectionPool()
    {
        return connectionPool;
    }
}
