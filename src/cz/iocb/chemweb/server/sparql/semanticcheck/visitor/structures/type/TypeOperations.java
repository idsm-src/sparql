package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.semanticcheck.ontology.ClassesInfo;



/**
 * Basic type operations: Intersect and union types.
 *
 */
public class TypeOperations
{
    /**
     * Intersect (use And) types. This is a simple implementation that does not simplify structure or checks for
     * duplicates in child nodes or other.
     *
     * @param mergeTo Tree under which 'other' will be merged
     * @param other Tree that will be merged under 'mergeTo'
     * @return new merged type. 'mergeTo' and 'other' remain unchanged.
     *
     */
    public static TypeElement intersectVariableTypes(TypeElement mergeTo, TypeElement other, ClassesInfo ontology)
    {
        if(mergeTo.value != null && mergeTo.value.equals(ClassesInfo.anyClass))
            return other;

        if(other.value != null && other.value.equals(ClassesInfo.anyClass))
            return mergeTo;


        AndOperator result = new AndOperator(null);


        if(mergeTo instanceof AndOperator)
            result.sons.addAll(((AndOperator) mergeTo).sons);
        else
            result.sons.add(mergeTo);

        if(other instanceof AndOperator)
            result.sons.addAll(((AndOperator) other).sons);
        else
            result.sons.add(other);

        return simplifyIntersection(result, ontology);
    }


    private static TypeElement simplifyIntersection(AndOperator and, ClassesInfo ontology)
    {
        SubClassEvaluation eval = new SubClassEvaluation();

        and.sons = eliminateSame(and.sons, ontology);

        ArrayList<TypeElement> sons = new ArrayList<>();

        for(TypeElement son : and.sons)
            if(and.sons.stream().noneMatch((s) -> (s != son && eval.isSubClass(s, son, ontology))))
                sons.add(son);

        and.sons = sons;

        if(sons.size() == 1)
            return sons.get(0);
        else
            return and;
    }


    /**
     * Union (use Or) types. This is a simple implementation that does not simplify structure or checks for duplicates
     * in child nodes or other.
     *
     * @param mergeTo Tree under which 'other' will be merged
     * @param other Tree that will be merged under 'mergeTo'
     * @return new merged type. 'mergeTo' and 'other' remain unchanged.
     */
    public static TypeElement unionVariableTypes(TypeElement mergeTo, TypeElement other, ClassesInfo ontology)
    {
        if(mergeTo.value != null && mergeTo.value.equals(ClassesInfo.anyClass))
            return mergeTo;

        if(other.value != null && other.value.equals(ClassesInfo.anyClass))
            return other;


        OrOperator result = new OrOperator(null);


        if(mergeTo instanceof OrOperator)
            result.sons.addAll(((OrOperator) mergeTo).sons);
        else
            result.sons.add(mergeTo);

        if(other instanceof OrOperator)
            result.sons.addAll(((OrOperator) other).sons);
        else
            result.sons.add(other);

        return simplifyUnion(result, ontology);
    }


    private static TypeElement simplifyUnion(OrOperator or, ClassesInfo ontology)
    {
        SubClassEvaluation eval = new SubClassEvaluation();

        ArrayList<TypeElement> sons = new ArrayList<>();

        or.sons = eliminateSame(or.sons, ontology);

        for(TypeElement son : or.sons)
            if(or.sons.stream().noneMatch((s) -> (s != son && eval.isSubClass(son, s, ontology))))
                sons.add(son);

        or.sons = sons;

        if(sons.size() == 1)
            return sons.get(0);
        else
            return or;
    }


    public static TypeElement getBaseClass(String classname, ClassesInfo ontology)
    {
        Set<String> info = ontology.getUnionSubClasses(classname);

        if(info == null)
        {
            String negatedClass = ontology.getNegatedClass(classname);

            if(negatedClass == null)
                return new Constraint(classname);

            NegOperator neg = new NegOperator(classname);
            neg.son = getBaseClass(negatedClass, ontology);
            return neg;
        }
        else
        {
            OrOperator result = new OrOperator(classname);

            for(String sub : info)
                result.sons.add(getBaseClass(sub, ontology));

            return result;
        }
    }


    private static ArrayList<TypeElement> eliminateSame(List<TypeElement> sons, ClassesInfo ontology)
    {
        SubClassEvaluation eval = new SubClassEvaluation();

        ArrayList<TypeElement> result = new ArrayList<>();

        loop:
        for(TypeElement son : sons)
        {
            for(TypeElement s : result)
            {
                if(eval.isSubClass(son, s, ontology) && eval.isSubClass(son, s, ontology))
                    continue loop;
            }

            result.add(son);
        }

        return result;
    }
}
