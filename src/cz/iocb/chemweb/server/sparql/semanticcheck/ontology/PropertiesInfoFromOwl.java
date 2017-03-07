/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */

package cz.iocb.chemweb.server.sparql.semanticcheck.ontology;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import cz.iocb.chemweb.server.db.ConnectionPool;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.AndOperator;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.OrOperator;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeOperations;



/**
 * Implementation of properties info provider, it searches for information in database ontologies. For initialization of
 * this object, it needs a working connection to database server.
 *
 */
public class PropertiesInfoFromOwl extends PropertiesInfo implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /** SPARQL query to obtain properties with their ranges */
    private static final String rangeQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?PROPERTY ?CLASS where { ?PROPERTY rdfs:range ?CLASS }";

    /** SPARQL query to obtain properties with their domains */
    private static final String domainQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?PROPERTY ?CLASS where { ?PROPERTY rdfs:domain ?CLASS }";

    /** SPARQL query to obtain every property from ontology */
    private static final String propertiesQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?PROPERTY where { ?PROPERTY rdf:type rdf:Property.}";

    /** Query that obtains information about properties with DataRange domain */
    private static final String datarangeDomainQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "SELECT ?PROPERTY, ?TYPE where { ?PROPERTY rdfs:domain ?R. ?R rdf:type owl:DataRange. "
            + "?R owl:oneOf / rdf:rest* / rdf:first ?VALUE. bind (datatype(?VALUE) as ?TYPE) }";

    /** Query that obtains information about properties with DataRange range */
    private static final String datarangeRangeQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "SELECT ?PROPERTY, ?TYPE where { ?PROPERTY rdfs:range ?R. ?R rdf:type owl:DataRange. "
            + "?R owl:oneOf / rdf:rest* / rdf:first ?VALUE. bind (datatype(?VALUE) as ?TYPE) }";


    /** Set of property IRIs */
    private final Set<String> properties;
    /** Set of properties with range types */
    private final Map<String, TypeElement> propertyRange;
    /** Set of properties with domain types */
    private final Map<String, TypeElement> propertyDomain;



    static private PropertiesInfoFromOwl instance;

    public static PropertiesInfoFromOwl getInstance()
            throws FileNotFoundException, IOException, PropertyVetoException, SQLException
    {
        if(instance == null)
        {
            synchronized(PropertiesInfoFromOwl.class)
            {
                if(instance == null)
                {
                    try (Connection connection = ConnectionPool.getConnection())
                    {
                        instance = new PropertiesInfoFromOwl(connection, ClassesInfoFromOwl.getInstance());
                    }
                }
            }
        }

        return instance;
    }



    private PropertiesInfoFromOwl(Connection connection, ClassesInfo classInfo) throws SQLException
    {
        setClassInfo(classInfo);

        propertyRange = buildMapWithAnd(QueryDatabase.extractMapFromCols(QueryDatabase.query(rangeQuery, connection)));
        propertyDomain = buildMapWithAnd(
                QueryDatabase.extractMapFromCols(QueryDatabase.query(domainQuery, connection)));
        properties = QueryDatabase.extractSetFromCol(QueryDatabase.query(propertiesQuery, connection), 0);
        editDomRngDataRangeVals(connection);
    }

    /**
     * Build a structure that represents intersection of multiple classes.
     *
     * @param structure classes to be connected
     * @return Types for each entry
     */
    private Map<String, TypeElement> buildMapWithAnd(Map<String, Set<String>> structure)
    {
        if(structure == null)
            return null;

        Map<String, TypeElement> result = new HashMap<>();
        for(Map.Entry<String, Set<String>> entry : structure.entrySet())
        {
            String key = entry.getKey();
            if(entry.getValue().size() > 1)
            {
                AndOperator and = new AndOperator(null);
                for(String val : entry.getValue())
                    // and.sons.add(new Constraint(val));
                    and.sons.add(TypeOperations.getBaseClass(val, getClassInfo()));

                result.put(key, and);
            }
            else if(entry.getValue().size() == 1)
                // result.put(key, new
                // Constraint(entry.getValue().iterator().next()));
                result.put(key, TypeOperations.getBaseClass(entry.getValue().iterator().next(), getClassInfo()));
        }
        return result;
    }

    /**
     * Build a structure that represents union of multiple classes.
     *
     * @param structure classes to be connected
     * @return Types for each entry
     */
    private Map<String, TypeElement> buildMapWithOr(Map<String, Set<String>> structure)
    {
        if(structure == null)
            return null;

        Map<String, TypeElement> result = new HashMap<>();
        for(Map.Entry<String, Set<String>> entry : structure.entrySet())
        {
            String key = entry.getKey();
            if(entry.getValue().size() > 1)
            {
                OrOperator or = new OrOperator(null);
                for(String val : entry.getValue())
                    // or.sons.add(new Constraint(val));
                    or.sons.add(TypeOperations.getBaseClass(val, getClassInfo()));
                result.put(key, or);
            }
            else if(entry.getValue().size() == 1)
                // result.put(key, new
                // Constraint(entry.getValue().iterator().next()));
                result.put(key, TypeOperations.getBaseClass(entry.getValue().iterator().next(), getClassInfo()));
        }
        return result;
    }

    /**
     * Retrieve information about properties with DataRange dom or rng and update the structures. Otherwise, there would
     * be blank nodes that are subclasses of nothing.
     */
    private void editDomRngDataRangeVals(Connection connection) throws SQLException
    {
        Map<String, TypeElement> rng = buildMapWithOr(
                QueryDatabase.extractMapFromCols(QueryDatabase.query(datarangeRangeQuery, connection)));
        Map<String, TypeElement> dom = buildMapWithOr(
                QueryDatabase.extractMapFromCols(QueryDatabase.query(datarangeDomainQuery, connection)));

        if(rng != null && !rng.isEmpty())
            for(Map.Entry<String, TypeElement> entry : rng.entrySet())
            {
                String key = entry.getKey();
                this.propertyRange.put(key, entry.getValue());
            }
        if(dom != null && !dom.isEmpty())
            for(Map.Entry<String, TypeElement> entry : dom.entrySet())
            {
                String key = entry.getKey();
                this.propertyDomain.put(key, entry.getValue());
            }
    }

    /**
     * Print properties information to the standard output. It can be used for debugging purposes.
     */
    public void printStructures()
    {
        System.out.println(this.properties.size() + "Properties in the set");
        this.properties.stream().sorted().forEach(
                a -> System.out.println(a + ":" + this.propertyDomain.get(a) + " -> " + this.propertyRange.get(a)));
    }

    @Override
    public boolean isProperty(String property)
    {
        if(this.properties != null && property != null)
            return this.properties.contains(property);
        else
            return false;
    }

    @Override
    public TypeElement getDomain(String property)
    {
        if(this.propertyDomain != null && property != null && this.propertyDomain.containsKey(property))
            return this.propertyDomain.get(property);
        else
            return null;
    }

    @Override
    public TypeElement getRange(String property)
    {
        if(this.propertyRange != null && property != null && this.propertyRange.containsKey(property))
            return this.propertyRange.get(property);
        else
            return null;
    }
}
