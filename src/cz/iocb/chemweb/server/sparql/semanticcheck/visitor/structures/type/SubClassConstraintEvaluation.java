package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;



public class SubClassConstraintEvaluation implements Visitor<Boolean>
{
    String smallClass;

    ClassesInfo ontology;

    public Boolean isSubClass(String smallClass, TypeElement bigClass, ClassesInfo ontology)
    {
        this.smallClass = smallClass;
        this.ontology = ontology;
        return bigClass.accept(this);
    }

    @Override
    public Boolean visit(Constraint constraint)
    {
        return ontology.isSubset(smallClass, constraint.value);
    }

    @Override
    public Boolean visit(NegOperator negation)
    {
        return ontology.isSubset(smallClass, negation.value);
    }

    @Override
    public Boolean visit(AndOperator and)
    {
        return and.sons.stream().allMatch((son) -> son.accept(this));
    }

    @Override
    public Boolean visit(OrOperator or)
    {
        return or.sons.stream().anyMatch((son) -> son.accept(this));
    }
}
