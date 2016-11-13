package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeElement;
import cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type.TypeOperations;



/**
 * State of semantic check, consisting of variable types, its occurrences and semantic errors that were inferred.
 * Semantic errors that appear in the path are collected in a set. This type is being built on a tree structure of path.
 *
 */
public class InferenceState
{
    /** HashMap of variables and its global types */
    private final HashMap<String, TypeElement> variableTypes = new HashMap<>();
    /** HashMap of variables and its occurrences with local types */
    private final HashMap<String, HashSet<Occurrence>> variableOccurences = new HashMap<>();


    public final ClassesInfo ontology;

    public InferenceState(ClassesInfo ontology)
    {
        this.ontology = ontology;
    }


    /**
     * Add variable and occurrence to the structure of occurrences.
     *
     * @param variable variable name
     * @param occurence occurrence object
     */
    public void addVariableOccurence(String variable, Occurrence occurence)
    {
        if(!this.variableOccurences.containsKey(variable))
            this.variableOccurences.put(variable, new HashSet<>());
        this.variableOccurences.get(variable).add(occurence);
    }


    /**
     * Get all occurrences of a variable.
     *
     * @param variable variable
     * @return set of occurrences
     */
    public HashSet<Occurrence> getVariableOccurences(String variable)
    {
        return this.variableOccurences.containsKey(variable) ? this.variableOccurences.get(variable) : null;
    }

    /**
     * @return all occurrences of all variables
     */
    public HashMap<String, HashSet<Occurrence>> getOccurences()
    {
        return this.variableOccurences;
    }

    /**
     * Remove all variable types collected so far.
     */
    public void clearVariableTypes()
    {
        this.variableTypes.clear();
    }

    /**
     * Remove all variable occurrences collected so far.
     */
    public void clearVariableOccurences()
    {
        this.variableOccurences.clear();
    }

    /**
     * @return variables and its types
     */
    public HashMap<String, TypeElement> getVariableTypes()
    {
        return this.variableTypes;
    }

    /**
     * @param variable variable
     * @return type of a variable, or null if type does not exist.
     */
    public TypeElement getVariableType(String variable)
    {
        return this.variableTypes.containsKey(variable) ? this.variableTypes.get(variable) : null;
    }

    /**
     * Create intersection of variable type with new type.
     *
     * @param variable variable
     * @param type new type to be added
     */
    public void intersectVariableTypes(String variable, TypeElement type)
    {
        if(this.variableTypes.containsKey(variable))
        {
            TypeElement typeA = this.variableTypes.get(variable);
            this.variableTypes.put(variable, TypeOperations.intersectVariableTypes(typeA, type, ontology));
        }
        else
            this.variableTypes.put(variable, type);
    }

    /**
     * Create union of variable type with new type.
     *
     * @param variable variable
     * @param type new type to be added
     */
    public void unionVariableTypes(String variable, TypeElement type)
    {
        if(this.variableTypes.containsKey(variable))
        {
            TypeElement typeA = this.variableTypes.get(variable);
            this.variableTypes.put(variable, TypeOperations.unionVariableTypes(typeA, type, ontology));
        }
        else
            this.variableTypes.put(variable, type);
    }

    /**
     * Intersect this state with another state - merge occurrences, errors and connect types as intersections.
     *
     * @param state state that will be intersected with this.
     */
    public void intersectWithState(InferenceState state)
    {
        this.joinWithState(state);
        for(Entry<String, TypeElement> kvp : state.getVariableTypes().entrySet())
        {
            if(this.variableTypes.containsKey(kvp.getKey()))
                this.intersectVariableTypes(kvp.getKey(), kvp.getValue());
            else
                this.variableTypes.put(kvp.getKey(), kvp.getValue());
        }
    }

    /**
     * Union this state with another state - merge occurrences, errors and connect types as unions.
     *
     * @param state state that will be united with this.
     */
    public void unionWithState(InferenceState state)
    {
        this.joinWithState(state);
        state.getVariableTypes().entrySet().stream().forEach(kvp -> {
            if(this.variableTypes.containsKey(kvp.getKey()))
                this.unionVariableTypes(kvp.getKey(), kvp.getValue());
            else
                this.variableTypes.put(kvp.getKey(), kvp.getValue());
        });
    }

    /**
     * Merge occurrences and errors of this state with another.
     *
     * @param state state that will be merged.
     */
    private void joinWithState(InferenceState state)
    {
        for(Entry<String, HashSet<Occurrence>> kvp : state.getOccurences().entrySet())
        {
            if(this.variableOccurences.containsKey(kvp.getKey()))
                this.variableOccurences.get(kvp.getKey()).addAll(kvp.getValue());
            else
                // this.variableOccurences.put(kvp.getKey(), kvp.getValue());
                this.variableOccurences.put(kvp.getKey(), new HashSet<Occurrence>(kvp.getValue()));
        }
    }
}
