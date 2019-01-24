package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlRecursive extends SqlIntercode
{
    private static final String leftTable = "tab0";
    private static final String rightTable = "tab1";

    private final SqlIntercode init;
    private final SqlIntercode next;
    private final UsedVariable endVar;
    private final String joinName;
    private final String subjectName;
    private final Node cndNode;


    protected SqlRecursive(UsedVariables variables, SqlIntercode init, SqlIntercode next, UsedVariable endVar,
            String joinName, String subjectName, Node cndNode)
    {
        super(variables);
        this.init = init;
        this.next = next;
        this.endVar = endVar;
        this.joinName = joinName;
        this.subjectName = subjectName;
        this.cndNode = cndNode;
    }


    public static SqlIntercode create(SqlIntercode init, SqlIntercode next, Node subject, BlankNode joinNode,
            VariableOrBlankNode endNode, Node cndNode)
    {
        String subjectName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getName() : null;
        String joinName = joinNode.getName();
        String endName = endNode.getName();


        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : init.getVariables().getValues())
            if(!var.getName().equals(joinName))
                variables.add(var);


        UsedVariable endVar = new UsedVariable(endName, false);
        variables.add(endVar);

        Set<ResourceClass> initEndClasses = init.getVariables().get(joinName).getClasses();
        Set<ResourceClass> nextEndClasses = next.getVariables().get(endName).getClasses();

        for(ResourceClass initClass : initEndClasses)
        {
            if(nextEndClasses.contains(initClass))
                endVar.addClass(initClass);
            else if(nextEndClasses.contains(initClass.getGeneralClass()))
                endVar.addClass(initClass.getGeneralClass());
            else if(nextEndClasses.stream().noneMatch(r -> r.getGeneralClass() == initClass))
                endVar.addClass(initClass);
        }

        for(ResourceClass nextClass : nextEndClasses)
        {
            if(initEndClasses.contains(nextClass))
                continue;
            else if(initEndClasses.contains(nextClass.getGeneralClass()))
                endVar.addClass(nextClass.getGeneralClass());
            else if(initEndClasses.stream().noneMatch(r -> r.getGeneralClass() == nextClass))
                endVar.addClass(nextClass);
        }

        return new SqlRecursive(variables, init, next, endVar, joinName, subjectName, cndNode);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("WITH RECURSIVE recursion(");

        boolean hasDeclaration = buildInitColumns(builder, null);

        for(ResourceClass resClass : endVar.getClasses())
        {
            for(int i = 0; i < resClass.getPatternPartsCount(); i++)
            {
                appendComma(builder, hasDeclaration);
                hasDeclaration = true;
                builder.append(resClass.getSqlColumn(endVar.getName(), i));
            }
        }


        builder.append(") AS (SELECT ");

        boolean hasInitSelect = buildInitColumns(builder, null);

        for(ResourceClass resClass : endVar.getClasses())
        {
            Set<ResourceClass> initResClasses = init.getVariables().get(joinName).getCompatible(resClass);

            for(int j = 0; j < resClass.getPatternPartsCount(); j++)
            {
                appendComma(builder, hasInitSelect);
                hasInitSelect = true;

                if(initResClasses.size() == 0)
                {
                    builder.append("NULL::");
                    builder.append(resClass.getSqlType(j));
                }
                else if(initResClasses.contains(resClass))
                {
                    builder.append(resClass.getSqlColumn(joinName, j));
                }
                else
                {
                    if(initResClasses.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass childResClass : initResClasses)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        //TODO: do not use CASE check if it is not needed
                        builder.append(childResClass.getGeneralisedPatternCode(null, joinName, j, true));
                    }

                    if(initResClasses.size() > 1)
                        builder.append(")");
                }

                builder.append(" AS ");
                builder.append(resClass.getSqlColumn(endVar.getName(), j));
            }
        }


        builder.append(" FROM (");
        builder.append(init.translate());
        builder.append(") AS tab");

        builder.append(" UNION SELECT ");

        boolean hasUnionSelect = buildInitColumns(builder, leftTable);

        for(ResourceClass resClass : endVar.getClasses())
        {
            Set<ResourceClass> endResClasses = next.getVariables().get(endVar.getName()).getCompatible(resClass);

            for(int j = 0; j < resClass.getPatternPartsCount(); j++)
            {
                appendComma(builder, hasUnionSelect);
                hasUnionSelect = true;

                if(endResClasses.size() == 0)
                {
                    builder.append("NULL::");
                    builder.append(resClass.getSqlType(j));
                }
                else if(!endResClasses.contains(resClass))
                {
                    if(endResClasses.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass childResClass : endResClasses)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        //TODO: do not use CASE check if it is not needed
                        builder.append(childResClass.getGeneralisedPatternCode(rightTable, endVar.getName(), j, true));
                    }

                    if(endResClasses.size() > 1)
                        builder.append(")");
                }
                else
                {
                    builder.append(rightTable);
                    builder.append('.');
                    builder.append(resClass.getSqlColumn(endVar.getName(), j));
                }

                builder.append(" AS ");
                builder.append(resClass.getSqlColumn(endVar.getName(), j));
            }
        }


        builder.append(" FROM recursion AS ");
        builder.append(leftTable);

        builder.append(", (");
        builder.append(next.translate());
        builder.append(") AS ");
        builder.append(rightTable);

        builder.append(" WHERE ");


        boolean restricted = false;
        String endName = endVar.getName();

        for(PairedClass pairedClass : getPairs(endVar, next.getVariables().get(joinName)))
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

                    builder.append(leftTable).append('.').append(resClass.getSqlColumn(endName, i));
                    builder.append(" = ");
                    builder.append(rightTable).append('.').append(resClass.getSqlColumn(joinName, i));
                }
            }
            else
            {
                ResourceClass resClass = pairedClass.getLeftClass().getGeneralClass();

                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendAnd(builder, i > 0);

                    builder.append(pairedClass.getLeftClass().getGeneralisedPatternCode(leftTable, endName, i, false));
                    builder.append(" = ");
                    builder.append(
                            pairedClass.getRightClass().getGeneralisedPatternCode(rightTable, joinName, i, false));
                }
            }

            builder.append(")");
        }


        builder.append(") SELECT ");

        boolean hasFinalSelect = buildInitColumns(builder, null);

        if(cndNode == null)
        {
            for(ResourceClass resClass : endVar.getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasFinalSelect);
                    hasFinalSelect = true;
                    builder.append(resClass.getSqlColumn(endVar.getName(), i));
                }
            }
        }

        if(!hasFinalSelect)
            builder.append(" 1 ");


        builder.append(" FROM recursion");

        if(cndNode != null)
        {
            builder.append(" WHERE ");

            if(cndNode instanceof VariableOrBlankNode)
            {
                boolean hasVariant = false;

                for(PairedClass pairedClass : getPairs(init.getVariables().get(subjectName), endVar))
                {
                    appendOr(builder, hasVariant);
                    hasVariant = true;

                    builder.append("(");

                    if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                    {
                        ResourceClass resClass = pairedClass.getLeftClass();

                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, i > 0);

                            builder.append(resClass.getSqlColumn(subjectName, i));
                            builder.append(" = ");
                            builder.append(resClass.getSqlColumn(endVar.getName(), i));
                        }
                    }
                    else
                    {
                        ResourceClass resClass = pairedClass.getLeftClass().getGeneralClass();

                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, i > 0);

                            builder.append(
                                    pairedClass.getLeftClass().getGeneralisedPatternCode(null, subjectName, i, false));
                            builder.append(" = ");
                            builder.append(pairedClass.getRightClass().getGeneralisedPatternCode(null, endVar.getName(),
                                    i, false));
                        }
                    }

                    builder.append(")");
                }
            }
            else
            {
                boolean hasVariant = false;

                for(ResourceClass resClass : endVar.getClasses())
                {
                    if(resClass.match(cndNode))
                    {
                        appendAnd(builder, hasVariant);
                        hasVariant = true;

                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendAnd(builder, i > 0);
                            builder.append(resClass.getSqlColumn(endVar.getName(), i));
                            builder.append(" = ");
                            builder.append(resClass.getPatternCode(cndNode, i));
                        }
                    }
                }
            }
        }

        return builder.toString();
    }


    public static List<PairedClass> getPairs(UsedVariable left, UsedVariable right)
    {
        ArrayList<PairedClass> pairs = new ArrayList<PairedClass>();

        for(ResourceClass leftClass : left.getClasses())
        {
            if(right.getClasses().contains(leftClass))
                pairs.add(new PairedClass(leftClass, leftClass));
            else if(right.getClasses().contains(leftClass.getGeneralClass()))
                pairs.add(new PairedClass(leftClass, leftClass.getGeneralClass()));
        }

        for(ResourceClass rightClass : right.getClasses())
        {
            if(!left.getClasses().contains(rightClass) && left.getClasses().contains(rightClass.getGeneralClass()))
                pairs.add(new PairedClass(rightClass.getGeneralClass(), rightClass));
        }

        return pairs;
    }


    private boolean buildInitColumns(StringBuilder builder, String table)
    {
        boolean hasDeclaration = false;

        for(UsedVariable var : init.getVariables().getValues())
        {
            if(var.getName() == joinName)
                continue;

            for(ResourceClass resClass : var.getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasDeclaration);
                    hasDeclaration = true;

                    if(table != null)
                        builder.append(table).append('.');

                    builder.append(resClass.getSqlColumn(var.getName(), i));
                }
            }
        }

        return hasDeclaration;
    }
}
