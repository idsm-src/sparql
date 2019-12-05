package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlMerge extends SqlIntercode
{
    private final String variable1;
    private final String variable2;
    private final SqlIntercode context;


    protected SqlMerge(UsedVariables variables, String variable1, String variable2, SqlIntercode context)
    {
        super(variables, context.isDeterministic());

        this.variable1 = variable1;
        this.variable2 = variable2;
        this.context = context;
    }


    public static SqlIntercode create(String variable1, String variable2, SqlIntercode context, boolean checkOnly)
    {
        if(context == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(context instanceof SqlUnion)
        {
            SqlIntercode union = SqlNoSolution.get();

            for(SqlIntercode child : ((SqlUnion) context).getChilds())
                union = SqlUnion.union(union, create(variable1, variable2, child, checkOnly));

            return union;
        }


        UsedVariables usedVariables1 = new UsedVariables();
        UsedVariables usedVariables2 = new UsedVariables();

        for(UsedVariable variable : context.getVariables().getValues())
        {
            if(!variable.getName().equals(variable1) && !variable.getName().equals(variable2))
                usedVariables1.add(variable);
            else if(variable.getName().equals(variable1) && !checkOnly)
                usedVariables1.add(variable);
            else if(variable.getName().equals(variable2))
                usedVariables2.add(new UsedVariable(variable1, variable.getClasses(), variable.canBeNull()));
        }

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(usedVariables1, usedVariables2);
        UsedVariables variables = SqlJoin.getUsedVariable(pairs, null);

        if(variables == null)
            return SqlNoSolution.get();

        return new SqlMerge(variables, variable1, variable2, context);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        HashSet<String> contextRestrictions = new HashSet<String>(restrictions);
        contextRestrictions.add(variable1);
        contextRestrictions.add(variable2);

        SqlIntercode optimizedContext = context.optimize(request, contextRestrictions, reduced);

        if(context.getVariables().get(variable1) == null && !restrictions.contains(variable1))
            return context.optimize(request, restrictions, reduced);

        if(optimizedContext.getVariables().get(variable2) == null)
            return context.optimize(request, restrictions, reduced);

        return create(variable1, variable2, optimizedContext, !restrictions.contains(variable1));
    }


    @Override
    public String translate()
    {
        UsedVariable usedVariable1 = context.getVariables().get(variable1);
        UsedVariable usedVariable2 = context.getVariables().get(variable2);


        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        boolean hasSelect = false;

        if(variables.get(variable1) != null)
        {
            for(ResourceClass resClass : variables.get(variable1).getClasses())
            {
                Set<ResourceClass> emptySet = new HashSet<ResourceClass>();

                boolean leftCanBeNull = usedVariable1 == null ? true : usedVariable1.canBeNull();
                boolean rightCanBeNull = usedVariable2 == null ? true : usedVariable2.canBeNull();
                Set<ResourceClass> classes1 = usedVariable1 != null ? usedVariable1.getCompatible(resClass) : emptySet;
                Set<ResourceClass> classes2 = usedVariable2 != null ? usedVariable2.getCompatible(resClass) : emptySet;


                if(leftCanBeNull && !rightCanBeNull)
                    classes1 = emptySet;
                else if(!leftCanBeNull && rightCanBeNull)
                    classes2 = emptySet;
                else if(!leftCanBeNull && !rightCanBeNull && !classes1.isEmpty())
                    classes2 = emptySet;


                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    generateMergeSelectVariable(builder, variable1, variable2, resClass, i, classes1, classes2);
                }
            }
        }

        for(UsedVariable variable : variables.getValues())
        {
            if(!variable.getName().equals(variable1) && !variable.getName().equals(variable2))
            {
                for(ResourceClass resClass : variable.getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(resClass.getSqlColumn(variable.getName(), i));
                    }
                }
            }
        }

        if(!hasSelect)
            builder.append(" 1 ");


        builder.append(" FROM ");
        builder.append("(");
        builder.append(context.translate());
        builder.append(") AS tab");


        if(usedVariable1 != null && usedVariable2 != null)
        {
            builder.append(" WHERE ");

            UsedPairedVariable paired = new UsedPairedVariable(variable1, usedVariable1, usedVariable2);

            for(ResourceClass class1 : usedVariable1.getClasses())
            {
                if(usedVariable2.getClasses().contains(class1))
                    paired.addClasses(class1, class1);
                else if(usedVariable2.getClasses().contains(class1.getGeneralClass()))
                    paired.addClasses(class1, class1.getGeneralClass());
                else if(usedVariable2.getClasses().stream().noneMatch(r -> r.getGeneralClass() == class1))
                    paired.addClasses(class1, null);
            }

            for(ResourceClass class2 : usedVariable2.getClasses())
            {
                if(usedVariable1.getClasses().contains(class2))
                    ;
                else if(usedVariable1.getClasses().contains(class2.getGeneralClass()))
                    paired.addClasses(class2.getGeneralClass(), class2);
                else if(usedVariable1.getClasses().stream().noneMatch(r -> r.getGeneralClass() == class2))
                    paired.addClasses(null, class2);
            }


            boolean restricted = false;

            if(usedVariable1.canBeNull())
            {
                boolean use = false;
                builder.append("(");

                for(ResourceClass resClass : usedVariable1.getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendAnd(builder, use);
                        use = true;

                        builder.append(resClass.getSqlColumn(variable1, i));
                        builder.append(" IS NULL");
                    }
                }

                builder.append(")");

                assert use;
                restricted = true;
            }

            if(usedVariable2.canBeNull())
            {
                appendOr(builder, restricted);
                restricted = true;

                boolean use = false;
                builder.append("(");

                for(ResourceClass resClass : usedVariable2.getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendAnd(builder, use);
                        use = true;

                        builder.append(resClass.getSqlColumn(variable2, i));
                        builder.append(" IS NULL");
                    }
                }

                builder.append(")");

                assert use;
            }

            for(PairedClass pairedClass : paired.getClasses())
            {
                if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                {
                    appendOr(builder, restricted);
                    restricted = true;

                    builder.append("(");

                    if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                    {
                        ResourceClass resClass = pairedClass.getLeftClass();

                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, i > 0);

                            builder.append(resClass.getSqlColumn(variable1, i));
                            builder.append(" = ");
                            builder.append(resClass.getSqlColumn(variable2, i));
                        }
                    }
                    else
                    {
                        ResourceClass resClass = pairedClass.getLeftClass().getGeneralClass();

                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, i > 0);

                            builder.append(
                                    pairedClass.getLeftClass().getGeneralisedPatternCode(null, variable1, i, false));
                            builder.append(" = ");
                            builder.append(
                                    pairedClass.getRightClass().getGeneralisedPatternCode(null, variable2, i, false));
                        }
                    }

                    builder.append(")");
                }
            }

            assert restricted;
        }


        return builder.toString();
    }


    private static void generateMergeSelectVariable(StringBuilder builder, String variable1, String variable2,
            ResourceClass resourceClass, int part, Set<ResourceClass> resourceClasses1,
            Set<ResourceClass> resourceClasses2)
    {
        if(resourceClasses1.size() + resourceClasses2.size() > 1)
            builder.append("COALESCE(");

        boolean hasVariant = false;

        for(ResourceClass leftClass : resourceClasses1)
        {
            appendComma(builder, hasVariant);
            hasVariant = true;

            if(leftClass == resourceClass)
                builder.append(leftClass.getSqlColumn(variable1, part));
            else
                builder.append(leftClass.getGeneralisedPatternCode(null, variable1, part, true));
        }

        for(ResourceClass rightClass : resourceClasses2)
        {
            appendComma(builder, hasVariant);
            hasVariant = true;

            if(rightClass == resourceClass)
                builder.append(rightClass.getSqlColumn(variable2, part));
            else
                builder.append(rightClass.getGeneralisedPatternCode(null, variable2, part, true));
        }

        if(resourceClasses1.size() + resourceClasses2.size() > 1)
            builder.append(") AS ").append(resourceClass.getSqlColumn(variable1, part));
    }
}
