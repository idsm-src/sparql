package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;



/**
 * Evaluation of 'compatibility' between two more complicated type structures. Given an
 * {@link cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.inference.Occurrence} type and a type inferred
 * from a sub query, it sends leaf constraints of occurrence type into {@link ConstraintEvaluation} to know if a
 * constraint is compatible with a type tree. It implements short-circuit evaluation, and so it evaluates compatibility
 * of two type trees.
 *
 */
public class OccurenceEvaluation implements Visitor<Boolean>
{
    /** Type of a variable in a sub query */
    TypeElement queryType;
    /** ontology information */
    ClassesInfo ontology;
    /** ConstraintEvaluation visitor object */
    ConstraintEvaluation ceval;

    public OccurenceEvaluation()
    {
        this.ceval = new ConstraintEvaluation();
    }

    /**
     * Check if type inferred from an @link cz.iocb.chemweb.server.sparql.semanticcheck
     * .visitor.structures.inference.Occurrence} is compatible with sub query type.
     *
     * @param variableType type of variable from its occurrence
     * @param queryType type of variable from sub query
     * @param ontology ontology information
     * @return true if occurrence constraint is compatible with type from sub query
     */
    public Boolean visitTripletOccurence(TypeElement variableType, TypeElement queryType, ClassesInfo ontology)
    {
        this.queryType = queryType;
        this.ontology = ontology;
        return variableType.accept(this);
    }

    /**
     * Send constraint with inferred type to a {@link ConstraintEvaluation} visitor.
     *
     * @param constraint leaf constraint
     * @return true if compatible (= not disjoint)
     */
    @Override
    public Boolean visit(Constraint constraint)
    {
        // if(ceval != null && ontology != null && constraint != null)
        return ceval.visitTree(this.queryType, constraint.value, ontology);
        // else
        // return false;
    }

    @Override
    public Boolean visit(NegOperator neg)
    {
        // if(ceval != null && ontology != null && neg != null)
        return ceval.visitTree(this.queryType, neg.value, ontology);
        // else
        // return false;
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
