package cz.iocb.chemweb.server.sparql.semanticcheck.ontology;

import java.util.Set;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.Constraint;



/**
 * Abstract class that defines what should be known for ontology classes to perform a semantic check. Class defines a
 * special value for "any Class" - for situations, where it is impossible to get a correct information about classes,
 * but it is needed to use some value while checking the query. It is a simple string with value "any". It also defines
 * a "property type" - a type that is assigned to variables in predicate positions. Each variable gets a type of
 * Constraint with value "&lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#Property&gt;"
 *
 */
public abstract class ClassesInfo implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /** Any class value */
    public static final String anyClass = "any";
    /** Type of variable at property position */
    public static final Constraint propertyType = new Constraint(
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>");

    /**
     * Confirms if passed identifier represents a class.
     *
     * @param classname IRI of class (with &lt; and &gt; signs)
     * @return True if classname is a valid class.
     */
    public abstract boolean isClass(String classname);

    /**
     * Determine if classes are disjoint.
     *
     * @param class1 First class
     * @param class2 Second class
     * @return True if disjoint
     */
    public abstract boolean isDisjoint(String class1, String class2);

    /**
     * Determine if one class is a subclass of second class.
     *
     * @param classBig Class that is tested to be superclass
     * @param classSmall Class that is tested to be subclass
     * @return True if classSmall is a subclass of classBig
     */
    public abstract boolean isSubset(String classSmall, String classBig);


    public abstract Set<String> getUnionSubClasses(String classname);


    public abstract String getNegatedClass(String classname);
}
