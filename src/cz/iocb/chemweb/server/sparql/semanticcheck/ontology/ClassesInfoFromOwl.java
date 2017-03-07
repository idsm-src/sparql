package cz.iocb.chemweb.server.sparql.semanticcheck.ontology;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import cz.iocb.chemweb.server.db.ConnectionPool;



/**
 * Implementation of class info provider, it searches for information in database ontologies. For initialization of this
 * object, it needs a working connection to database server.
 *
 */
public class ClassesInfoFromOwl extends ClassesInfo implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /** Query to retrieve all classes from ontology. */
    private static final String classesQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?CLASS where { ?CLASS rdf:type owl:Class. }";

    /** Query to retrieve all classes with its transitive subclasses. */
    private static final String subClassesQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?CLASS ?SUBCLASS where { ?SUBCLASS rdf:type owl:Class. ?SUBCLASS rdfs:subClassOf* ?CLASS. }";

    private static final String disjointClassesQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?C1 ?C2 where { ?C1 rdfs:subClassOf* ?X. ?X owl:disjointWith ?Y. ?C2 rdfs:subClassOf* ?Y. "
            + "filter(?X != owl:Nothing) filter(?Y != owl:Nothing) }";

    private static final String unionSubClassesQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select ?UNION ?SUBCLASS where { " // the outer select is a workround due to a virtuoso issue with the rdf:rest* construction
            + "select  ?UNION ?SUBCLASS where { ?UNION owl:unionOf / rdf:rest* / rdf:first ?SUBCLASS. filter(isBlank(?UNION)) } }";

    private static final String negatedClassQuery = "sparql define input:storage virtrdf:PubchemQuadStorage "
            + "select  ?NEG ?CLASS where { ?NEG owl:complementOf ?CLASS. filter(isBlank(?NEG))}";


    /** Set of classes IRIs */
    private final Set<String> classes;

    /** Set of classes and their subclasses */
    private final Map<String, Set<String>> subClassOf;

    /** Set of classes and their disjoint classes */
    private final Map<String, Set<String>> disjointClasses;

    private final Map<String, Set<String>> unionSubClasses;

    private final Map<String, Set<String>> negatedClass;



    static private ClassesInfoFromOwl instance;

    public static ClassesInfoFromOwl getInstance()
            throws FileNotFoundException, IOException, PropertyVetoException, SQLException
    {
        if(instance == null)
        {
            synchronized(ClassesInfoFromOwl.class)
            {
                if(instance == null)
                {
                    try (Connection connection = ConnectionPool.getConnection())
                    {
                        instance = new ClassesInfoFromOwl(connection);
                    }
                }
            }
        }

        return instance;
    }


    /**
     * Query database and extract information about classes and subclasses. It is needed to have a working connection to
     * database server.
     *
     * @param param additional parameters
     * @return True if all structures were correctly built.
     * @throws SQLException
     */
    private ClassesInfoFromOwl(Connection connection) throws SQLException
    {
        classes = QueryDatabase.extractSetFromCol(QueryDatabase.query(classesQuery, connection), 0);
        subClassOf = QueryDatabase.extractMapFromCols(QueryDatabase.query(subClassesQuery, connection));
        disjointClasses = QueryDatabase.extractMapFromCols(QueryDatabase.query(disjointClassesQuery, connection));
        unionSubClasses = QueryDatabase.extractMapFromCols(QueryDatabase.query(unionSubClassesQuery, connection));
        negatedClass = QueryDatabase.extractMapFromCols(QueryDatabase.query(negatedClassQuery, connection));
    }

    /**
     * Print classes information to the standard output. It can be used for debugging purposes.
     */
    public void printClasses()
    {
        System.out.println(this.classes.size() + "Classes in the set");
        this.classes.stream().sorted().forEach(a -> System.out.println(a));

        System.out.println("SubClasses:");
        for(Map.Entry<String, Set<String>> entry : subClassOf.entrySet())
        {
            System.out.println(entry.getKey());
            for(String s : entry.getValue())
                System.out.println("- " + s);
        }
    }

    @Override
    public boolean isClass(String classname)
    {
        return classname != null && this.classes.contains(classname);
    }

    @Override
    public boolean isDisjoint(String class1, String class2)
    {
        if(class1.equals(anyClass) || class2.equals(anyClass))
            return false;

        // return !(isSubset(class1, class2) || isSubset(class2, class1))

        if(!this.disjointClasses.containsKey(class1))
            return false;

        return this.disjointClasses.get(class1).contains(class2);
    }

    @Override
    public boolean isSubset(String classSmall, String classBig)
    {
        if(classSmall.equals(anyClass) || classBig.equals(anyClass))
            return false;

        if(classBig.equals(classSmall))
            return true;

        return this.subClassOf.containsKey(classBig) && this.subClassOf.get(classBig).contains(classSmall);
    }

    @Override
    public Set<String> getUnionSubClasses(String classname)
    {
        return unionSubClasses.get(classname);
    }

    @Override
    public String getNegatedClass(String classname)
    {
        Set<String> set = negatedClass.get(classname);

        if(set == null)
            return null;

        return negatedClass.get(classname).iterator().next();
    }
}
