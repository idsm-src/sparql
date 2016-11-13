package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference;

import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeOperations;



/**
 * Types that can be inferred from a path for subject and object of a triple. Subject type is implied by path domain.
 * Object type is implied by path range. Semantic errors that appear in the path are collected in a set. Special
 * "omissible" value says if current path block can be omitted while searching for a pattern in database using repeated
 * path. This type is being built on a tree structure of path.
 *
 */
public class PathInferredTypes
{
    /** Type inferred for subject */
    private TypeElement typesSubject;
    /** Type inferred for object */
    private TypeElement typesObject;
    /** Subpath used */
    private Path subpath;
    /** True if this block can be omitted */
    private boolean omissible = false;

    private final ClassesInfo ontology;

    public PathInferredTypes(ClassesInfo ontology)
    {
        this.ontology = ontology;
    }


    /**
     * Swap types of subject and object
     */
    public void swapSubjectObject()
    {
        TypeElement tmp = this.typesObject;
        this.typesObject = this.typesSubject;
        this.typesSubject = tmp;
    }

    /**
     * Indicator if subpath can be omitted in path.
     *
     * @return True if this subpath can be omitted in path.
     */
    public boolean isOmissible()
    {
        return this.omissible;
    }

    /**
     * Set if subpath can be omitted or not
     *
     * @param o true if subpath can be omitted
     */
    public void setOmmissible(boolean o)
    {
        this.omissible = o;
    }

    /**
     * Get subject type.
     *
     * @return Type of subject
     */
    public TypeElement getSubject()
    {
        return this.typesSubject;
    }

    /**
     * Get object type.
     *
     * @return Type of object
     */
    public TypeElement getObject()
    {
        return this.typesObject;
    }

    /**
     * Set subject type value.
     *
     * @param t subject type
     */
    public void setSubject(TypeElement t)
    {
        this.typesSubject = t;
    }

    /**
     * Set object type value
     *
     * @param t object type
     */
    public void setObject(TypeElement t)
    {
        this.typesObject = t;
    }

    /**
     * Intersect subject type with given type element.
     *
     * @param type type to be added
     */
    public void addToSubjectIntersect(TypeElement type)
    {
        if(this.typesSubject == null)
            this.typesSubject = type;
        else
            this.typesSubject = TypeOperations.intersectVariableTypes(this.typesSubject, type, ontology);
    }

    /**
     * Union subject type with given type element.
     *
     * @param type type to be added
     */
    public void addToSubjectUnion(TypeElement type)
    {
        if(this.typesSubject == null)
            this.typesSubject = type;
        else
            this.typesSubject = TypeOperations.unionVariableTypes(this.typesSubject, type, ontology);
    }

    /**
     * Intersect object type with given type element.
     *
     * @param type type to be added
     */
    public void addToObjectIntersect(TypeElement type)
    {
        if(this.typesObject == null)
            this.typesObject = type;
        else
            this.typesObject = TypeOperations.intersectVariableTypes(this.typesObject, type, ontology);
    }

    /**
     * Union object type with given type element.
     *
     * @param type type to be added
     */
    public void addToObjectUnion(TypeElement type)
    {
        if(this.typesObject == null)
            this.typesObject = type;
        else
            this.typesObject = TypeOperations.unionVariableTypes(this.typesObject, type, ontology);
    }

    /**
     * Get subpath object
     *
     * @return subpath
     */
    public Path getSubpath()
    {
        return this.subpath;
    }

    /**
     * Set subpath object
     *
     * @param subpath subpath
     */
    public void setSubpath(Path subpath)
    {
        this.subpath = subpath;
    }
}
