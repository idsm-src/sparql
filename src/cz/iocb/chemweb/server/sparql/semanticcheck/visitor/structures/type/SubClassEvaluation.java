package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;



public class SubClassEvaluation implements Visitor<Boolean>
{
    TypeElement bigClass;

    ClassesInfo ontology;

    SubClassConstraintEvaluation ceval;

    public SubClassEvaluation()
    {
        this.ceval = new SubClassConstraintEvaluation();
    }

    public Boolean isSubClass(TypeElement smallClass, TypeElement bigClass, ClassesInfo ontology)
    {
        this.bigClass = bigClass;
        this.ontology = ontology;
        return smallClass.accept(this);
    }

    @Override
    public Boolean visit(Constraint constraint)
    {
        return ceval.isSubClass(constraint.value, this.bigClass, ontology);
    }

    @Override
    public Boolean visit(NegOperator neg)
    {
        return ceval.isSubClass(neg.value, this.bigClass, ontology);
    }

    @Override
    public Boolean visit(AndOperator and)
    {
        return and.sons.stream().anyMatch((son) -> son.accept(this));
    }

    @Override
    public Boolean visit(OrOperator or)
    {
        return or.sons.stream().allMatch((son) -> son.accept(this));
    }
}
