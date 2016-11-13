package cz.iocb.chemweb.server.sparql.semanticcheck.ontology;

import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;



/**
 * Abstract class that defines what should be known of properties in ontology to perform a semantic check.
 *
 */
public abstract class PropertiesInfo implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Confirms if passed identifier represents a property.
     *
     * @param property IRI of a property (with &lt; and &gt; signs)
     * @return True if IRI is a property
     */
    public abstract boolean isProperty(String property);

    /**
     * Get type of domain of a property. It can be conjunction or disjunction of multiple classes.
     *
     * @param property Property IRI (with &lt; and &gt; signs)
     * @return Type of a property domain, or null if there is no information.
     */
    public abstract TypeElement getDomain(String property);

    /**
     * Get type of range of a property. It can be conjunction or disjunction of multiple classes.
     *
     * @param property Property IRI (with &lt; and &gt; signs)
     * @return Type of a property range, or null if there is no information.
     */
    public abstract TypeElement getRange(String property);


    private transient ClassesInfo classInfo;

    public ClassesInfo getClassInfo()
    {
        return classInfo;
    }

    public void setClassInfo(ClassesInfo classInfo)
    {
        this.classInfo = classInfo;
    }
}
