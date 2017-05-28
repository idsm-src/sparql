package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import java.util.ArrayList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlUnion extends SqlIntercode
{
    private final ArrayList<SqlIntercode> childs;


    protected SqlUnion(UsedVariables variables, ArrayList<SqlIntercode> childs)
    {
        super(variables);
        this.childs = childs;
    }


    public static SqlIntercode union(SqlIntercode left, SqlIntercode right)
    {
        /* special cases */

        if(left instanceof SqlNoSolution)
        {
            boolean included = true;

            for(UsedVariable var : left.getVariables().getValues())
                if(right.getVariables().get(var.getName()) == null)
                    included = false;

            if(included)
                return right;


            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : right.getVariables().getValues())
                variables.add(variable);

            for(UsedVariable variable : left.getVariables().getValues())
                if(variables.get(variable.getName()) == null)
                    variables.add(variable);

            if(right instanceof SqlNoSolution)
                return new SqlNoSolution();

            if(!(right instanceof SqlUnion))
                return new SqlUnion(variables, Lists.newArrayList(right));

            SqlUnion union = (SqlUnion) right;
            return new SqlUnion(variables, union.childs);
        }


        if(right instanceof SqlNoSolution)
        {
            boolean included = true;

            for(UsedVariable var : right.getVariables().getValues())
                if(left.getVariables().get(var.getName()) == null)
                    included = false;

            if(included)
                return left;


            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : left.getVariables().getValues())
                variables.add(variable);

            for(UsedVariable variable : right.getVariables().getValues())
                if(variables.get(variable.getName()) == null)
                    variables.add(variable);

            if(!(left instanceof SqlUnion))
                return new SqlUnion(variables, Lists.newArrayList(left));

            SqlUnion union = (SqlUnion) left;
            return new SqlUnion(variables, union.childs);
        }


        /* standard union */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : pairs)
        {
            String varName = pair.getName();
            boolean canBeNull = pair.getLeftVariable() != null && pair.getLeftVariable().canBeNull()
                    || pair.getRightVariable() != null && pair.getRightVariable().canBeNull();

            UsedVariable newVar = new UsedVariable(varName, canBeNull);

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                    newVar.addClass(pairedClass.getRightClass());
                else
                    newVar.addClass(pairedClass.getLeftClass());
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
                    boolean defined = child.getVariables().contains(varName, resClass);

                    for(int j = 0; j < resClass.getPartsCount(); j++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(!defined)
                        {
                            builder.append("NULL::");
                            builder.append(resClass.getSqlType(j));
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
