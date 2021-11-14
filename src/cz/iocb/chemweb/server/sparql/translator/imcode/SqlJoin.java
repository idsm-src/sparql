package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.google.common.collect.Lists;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final List<SqlIntercode> childs;
    private final List<UsedVariables> variables;
    private final List<Map<Column, Column>> columnMaps;


    protected SqlJoin(List<SqlIntercode> childs, List<UsedVariables> variables, List<Map<Column, Column>> columnMaps)
    {
        super(variables.get(variables.size() - 1), childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.variables = variables;
        this.columnMaps = columnMaps;
    }


    public static SqlIntercode join(SqlIntercode left, SqlIntercode right)
    {
        return join(left, right, null);
    }


    protected static SqlIntercode join(SqlIntercode left, SqlIntercode right, Set<String> restrictions)
    {
        /* special cases */

        if(left == SqlNoSolution.get() || right == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(left == SqlEmptySolution.get())
            return right.restrict(restrictions);

        if(right == SqlEmptySolution.get())
            return left.restrict(restrictions);


        if(left instanceof SqlUnion || right instanceof SqlUnion)
        {
            List<SqlIntercode> leftChilds;

            if(left instanceof SqlUnion)
                leftChilds = ((SqlUnion) left).getChilds();
            else
                leftChilds = Lists.newArrayList(left);


            List<SqlIntercode> rightChilds;

            if(right instanceof SqlUnion)
                rightChilds = ((SqlUnion) right).getChilds();
            else
                rightChilds = Lists.newArrayList(right);


            SqlIntercode union = SqlNoSolution.get();

            for(SqlIntercode leftChild : leftChilds)
                for(SqlIntercode rightChild : rightChilds)
                    union = SqlUnion.union(union, join(leftChild, rightChild, restrictions));

            return union;
        }


        /* standard join */

        if(restrictions == null)
        {
            restrictions = new HashSet<String>();
            restrictions.addAll(left.getVariables().getNames());
            restrictions.addAll(right.getVariables().getNames());
        }

        ArrayList<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        if(left instanceof SqlJoin)
            childs.addAll(((SqlJoin) left).childs);
        else
            childs.add(left);

        if(right instanceof SqlJoin)
            childs.addAll(((SqlJoin) right).childs);
        else
            childs.add(right);

        return join(childs, restrictions);
    }


    private static SqlIntercode join(List<SqlIntercode> childs, Set<String> restrictions)
    {
        DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

        loop:
        while(true)
        {
            for(int i = 0; i < childs.size(); i++)
            {
                for(int j = i + 1; j < childs.size(); j++)
                {
                    SqlIntercode iSql = childs.get(i);
                    SqlIntercode jSql = childs.get(j);

                    if(!(iSql instanceof SqlTableAccess))
                        continue;

                    if(!(jSql instanceof SqlTableAccess))
                        continue;

                    SqlTableAccess left = (SqlTableAccess) iSql;
                    SqlTableAccess right = (SqlTableAccess) jSql;

                    //TODO: add unjoinable check
                    //if(!SqlTableAccess.areCompatible(schema, left, right))
                    //    return SqlNoSolution.get();

                    List<ColumnPair> variantA = SqlTableAccess.canBeDroped(schema, left, right);

                    if(variantA != null)
                    {
                        HashSet<String> subrestrictions = getRestrictions(childs, i, j, restrictions);
                        SqlIntercode merge = SqlTableAccess.merge(left, right, variantA, subrestrictions);

                        if(merge == SqlNoSolution.get())
                            return SqlNoSolution.get();

                        childs.set(j, merge);
                        childs.remove(i);
                        continue loop;
                    }


                    List<ColumnPair> variantB = SqlTableAccess.canBeDroped(schema, right, left);

                    if(variantB != null)
                    {
                        HashSet<String> subrestrictions = getRestrictions(childs, i, j, restrictions);
                        SqlIntercode merge = SqlTableAccess.merge(right, left, variantB, subrestrictions);

                        if(merge == SqlNoSolution.get())
                            return SqlNoSolution.get();

                        childs.set(i, merge);
                        childs.remove(j);
                        continue loop;
                    }


                    if(SqlTableAccess.canBeMerged(schema, left, right))
                    {
                        HashSet<String> subrestrictions = getRestrictions(childs, i, j, restrictions);
                        SqlIntercode merge = SqlTableAccess.merge(left, right, subrestrictions);

                        if(merge == SqlNoSolution.get())
                            return SqlNoSolution.get();

                        childs.set(i, merge);
                        childs.remove(j);
                        continue loop;
                    }
                }
            }

            break loop;
        }

        if(childs.size() == 1)
            return childs.get(0);

        UsedVariables variables = childs.get(0).getVariables();
        List<UsedVariables> variablesChain = new ArrayList<UsedVariables>(childs.size() - 1);
        List<Map<Column, Column>> columnMaps = new ArrayList<Map<Column, Column>>(childs.size() - 1);

        for(int i = 1; i < childs.size(); i++)
        {
            Map<Column, Column> map = new HashMap<Column, Column>();
            variables = getJoinUsedVariables(variables, childs.get(i).getVariables(), leftTable, rightTable, null, map);

            if(variables == null)
                return SqlNoSolution.get();

            if(i == childs.size() - 1)
                variables = variables.restrict(restrictions);

            variablesChain.add(variables);
            columnMaps.add(map);
        }

        return new SqlJoin(childs, variablesChain, columnMaps);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        HashMap<String, Integer> variableCounts = new HashMap<String, Integer>();

        for(SqlIntercode child : childs)
            for(String var : child.getVariables().getNames())
                variableCounts.put(var, variableCounts.get(var) == null ? 1 : variableCounts.get(var) + 1);

        for(Entry<String, Integer> entry : variableCounts.entrySet())
            if(entry.getValue() > 1)
                childRestrictions.add(entry.getKey());


        SqlIntercode result = SqlEmptySolution.get();

        for(int i = 0; i < childs.size() - 1; i++)
            result = join(result, childs.get(i).optimize(childRestrictions, reduced), childRestrictions);

        result = join(result, childs.get(childs.size() - 1).optimize(childRestrictions, reduced), restrictions);

        return result;
    }


    @Override
    public String translate()
    {
        String translate = childs.get(0).translate();
        UsedVariables leftVariables = childs.get(0).getVariables();

        for(int i = 1; i < childs.size(); i++)
        {
            Set<Column> columns = variables.get(i - 1).getNonConstantColumns();
            Map<Column, Column> map = columnMaps.get(i - 1);

            StringBuilder builder = new StringBuilder();

            builder.append("SELECT ");

            if(!columns.isEmpty())
                builder.append(columns.stream().map(c -> map.get(c) + " AS " + c).collect(joining(", ")));
            else
                builder.append("1");

            builder.append(" FROM (");
            builder.append(translate);
            builder.append(" ) AS ");
            builder.append(leftTable);

            builder.append(", (");
            builder.append(childs.get(i).translate());
            builder.append(" ) AS ");
            builder.append(rightTable);

            String condition = generateJoinCondition(leftVariables, childs.get(i).getVariables(), leftTable,
                    rightTable);

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }

            leftVariables = variables.get(i - 1);
            translate = builder.toString();
        }

        return translate;
    }


    private static HashSet<String> getRestrictions(List<SqlIntercode> childs, int i, int j, Set<String> restrictions)
    {
        if(restrictions == null)
            return null;

        HashSet<String> subrestrictions = new HashSet<String>(restrictions);

        for(int k = 0; k < childs.size(); k++)
            if(k != i && k != j)
                subrestrictions.addAll(childs.get(k).getVariables().getNames());

        return subrestrictions;
    }
}
