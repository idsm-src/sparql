package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;



/**
 * Evaluates 'satisfiability' of a type tree considering one specific class value. Given a specific class constraint,
 * the structure is evaluated by "is_Disjoint" method provided by
 * {@link cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo}. It implements short-circuit evaluation.
 *
 */
public class ConstraintEvaluation implements Visitor<Boolean>
{
    /** class type constraint being considered */
    String typeChecked;
    /** ontology information about classes */
    ClassesInfo ontology;

    /**
     * Evaluate tree with given constraint.
     *
     * @param tree Tree to be evaluated
     * @param typeChecked Constraint on type
     * @param ontology ontology information object
     * @return True if constraint is compatible with this tree
     */
    public Boolean visitTree(TypeElement tree, String typeChecked, ClassesInfo ontology)
    {
        this.typeChecked = typeChecked;
        this.ontology = ontology;
        return tree.accept(this);
    }

    /**
     * Check if constraint is disjoint with considered type.
     *
     * @param constraint leaf constraint
     * @return true if compatible (= not disjoint)
     */
    @Override
    public Boolean visit(Constraint constraint)
    {
        // if(ontology != null && constraint != null)
        return !ontology.isDisjoint(constraint.value, typeChecked);
        // else
        // return false;
    }

    @Override
    public Boolean visit(NegOperator negation)
    {
        // if(ontology == null || negation == null)
        // return false;

        return !ontology.isDisjoint(negation.value, typeChecked);
    }

    /**
     * First false from child will cause false return.
     *
     * @param and and operator
     * @return true if intersection is compatible
     */
    @Override
    public Boolean visit(AndOperator and)
    {
        return and.sons.stream().noneMatch((son) -> !son.accept(this));
    }

    /**
     * First true from child will cause true return.
     *
     * @param or or operator
     * @return true if union is compatible
     */
    @Override
    public Boolean visit(OrOperator or)
    {
        return or.sons.stream().anyMatch((son) -> son.accept(this));
    }
}
