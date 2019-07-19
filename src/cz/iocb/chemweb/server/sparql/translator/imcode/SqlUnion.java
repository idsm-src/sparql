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



public class SqlUnion extends SqlIntercode
{
    private final ArrayList<SqlIntercode> childs;


    protected SqlUnion(UsedVariables variables, ArrayList<SqlIntercode> childs)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));
        this.childs = childs;
    }


    public static SqlIntercode union(SqlIntercode left, SqlIntercode right)
    {
        /* special cases */

        if(left == SqlNoSolution.get())
            return right;

        if(right == SqlNoSolution.get())
            return left;


        /* standard union */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : pairs)
        {
            String varName = pair.getName();
            boolean canBeNull = pair.getLeftVariable() == null || pair.getLeftVariable().canBeNull()
                    || pair.getRightVariable() == null || pair.getRightVariable().canBeNull();

            UsedVariable newVar = new UsedVariable(varName, canBeNull);

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                    newVar.addClass(pairedClass.getRightClass());
                else if(pairedClass.getRightClass() == null)
                    newVar.addClass(pairedClass.getLeftClass());
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                    newVar.addClass(pairedClass.getLeftClass());
                else
                    newVar.addClass(pairedClass.getLeftClass().getGeneralClass());
            }

            variables.add(newVar);
        }


        ArrayList<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        if(left instanceof SqlUnion)
            childs.addAll(((SqlUnion) left).childs);
        else
            childs.add(left);


        if(right instanceof SqlUnion)
            childs.addAll(((SqlUnion) right).childs);
        else
            childs.add(right);


        return new SqlUnion(variables, childs);
    }


    public final ArrayList<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        SqlIntercode result = SqlNoSolution.get();

        for(SqlIntercode child : childs)
            result = union(result, child.optimize(request, restrictions, reduced));

        return result;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(" UNION ALL ");

            SqlIntercode child = childs.get(i);

            builder.append("SELECT ");
            boolean hasSelect = false;

            for(UsedVariable variable : variables.getValues())
            {
                String varName = variable.getName();

                for(ResourceClass resClass : variable.getClasses())
                {
                    UsedVariable childVariable = child.getVariables().get(varName);

                    Set<ResourceClass> childResClasses = childVariable != null ? childVariable.getCompatible(resClass) :
                            new HashSet<ResourceClass>();

                    for(int j = 0; j < resClass.getPatternPartsCount(); j++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(childResClasses.size() == 0)
                        {
                            builder.append("NULL::");
                            builder.append(resClass.getSqlType(j));
                            builder.append(" AS ");
                        }
                        else if(!childResClasses.contains(resClass))
                        {
                            if(childResClasses.size() > 1)
                                builder.append("COALESCE(");

                            boolean hasAlternative = false;

                            for(ResourceClass childResClass : childResClasses)
                            {
                                appendComma(builder, hasAlternative);
                                hasAlternative = true;

                                //TODO: do not use CASE check if it is not needed
                                builder.append(childResClass.getGeneralisedPatternCode(null, varName, j, true));
                            }

                            if(childResClasses.size() > 1)
                                builder.append(")");

                            builder.append(" AS ");
                        }

                        builder.append(resClass.getSqlColumn(varName, j));
                    }
                }
            }

            if(!hasSelect)
                builder.append("1");


            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(") AS tab");
            builder.append(i);
        }

        return builder.toString();
    }
}
