package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Pair;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class SqlDistinct extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<String> distinctVariables;


    protected SqlDistinct(UsedVariables variables, SqlIntercode child, Set<String> distinctVariables)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.distinctVariables = distinctVariables;
    }


    public static SqlIntercode create(Request request, SqlIntercode child, Set<String> distinct)
    {
        return create(request, child, distinct, null);
    }


    protected static SqlIntercode create(Request request, SqlIntercode child, Set<String> distinctVariables,
            Set<String> restrictions)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child == SqlEmptySolution.get())
            return SqlEmptySolution.get();

        if(child instanceof SqlTableAccess access && access.isDistinct(request, distinctVariables))
            return child.optimize(request, restrictions, true);


        /* standard distinct */

        UsedVariables variables = new UsedVariables();

        for(UsedVariable v : child.getVariables().getValues())
            if(distinctVariables.contains(v.getName()) && (restrictions == null || restrictions.contains(v.getName())))
                variables.add(v);

        return new SqlDistinct(variables, child, distinctVariables);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        HashSet<String> childRestriction = new HashSet<String>(restrictions);
        childRestriction.addAll(distinctVariables);

        SqlIntercode optChild = child.optimize(request, childRestriction, true);


        if(optChild instanceof SqlTableAccess access && access.isDistinct(request, distinctVariables))
            return optChild.optimize(request, restrictions, reduced);


        if(optChild instanceof SqlJoin join)
        {
            boolean canBeEliminated = true;
            SqlIntercode extra = null;

            for(SqlIntercode child : join.getChilds())
            {
                if(child instanceof SqlTableAccess access)
                {
                    if(access.isDistinct(request, Set.of()))
                        continue;

                    if(extra == null && access.isDistinct(request, distinctVariables))
                    {
                        extra = access;
                        continue;
                    }
                }

                canBeEliminated = false;
                break;
            }

            if(canBeEliminated)
                return join.optimize(request, restrictions, reduced);
        }


        if(optChild instanceof SqlUnion union)
            return expandUnionByResourceClasses(request, union, distinctVariables, restrictions);

        return create(request, optChild, distinctVariables, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        SqlIntercode child = this.child;

        UsedVariables vars = child.getVariables().restrict(distinctVariables);

        Set<String> stringLiterals = new HashSet<String>();

        for(UsedVariable v : vars.getValues())
            for(ResourceClass c : v.getClasses())
                if(SqlExpressionIntercode.isStringLiteral(c))
                    stringLiterals.add(v.getName());

        for(String var : stringLiterals)
            child = SqlBind.bind(request, "#hash_" + var, SqlBuiltinCall.create(request, "_strhash", false,
                    List.of(SqlVariable.create(var, child.getVariables()))), child);


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS tab");

        builder.append(" GROUP BY ");

        Set<Column> groupColumns = child.getVariables().restrict(distinctVariables).getNonConstantColumns();

        Set<Column> hashColumns = new HashSet<Column>();

        for(String var : stringLiterals)
            hashColumns.addAll(child.getVariables().get("#hash_" + var).getNonConstantColumns());

        builder.append(hashColumns.stream().map(Object::toString).collect(joining(", ")));

        if(!hashColumns.isEmpty() && !groupColumns.isEmpty())
            builder.append(", ");


        if(!groupColumns.isEmpty())
            builder.append(groupColumns.stream().map(Object::toString).collect(joining(", ")));
        else if(hashColumns.isEmpty())
            builder.append("1"); // TODO: remove this possibility by an optimization

        return builder.toString();
    }


    private static SqlIntercode expandUnionByResourceClasses(Request request, SqlUnion union,
            Set<String> distinctVariables, Set<String> restrictions)
    {
        List<Pair<List<Set<ResourceClass>>, List<SqlIntercode>>> sorts = new ArrayList<>();

        for(SqlIntercode child : union.getChilds())
        {
            List<Set<ResourceClass>> newKey = new ArrayList<Set<ResourceClass>>();

            for(String varName : distinctVariables)
            {
                Set<ResourceClass> classes = new HashSet<ResourceClass>();

                UsedVariable unionVar = union.getVariable(varName);
                UsedVariable var = child.getVariable(varName);

                if(var == null || var.canBeNull())
                    classes.add(null);

                if(var != null)
                    for(ResourceClass r : var.getClasses())
                        classes.add(unionVar.getClasses().contains(r.getGeneralClass()) ? r.getGeneralClass() : r);

                newKey.add(classes);
            }

            List<SqlIntercode> newValue = new ArrayList<SqlIntercode>();
            newValue.add(child);

            for(int i = 0; i < sorts.size(); i++)
            {
                Pair<List<Set<ResourceClass>>, List<SqlIntercode>> pair = sorts.get(i);
                List<Set<ResourceClass>> key = pair.getKey();
                List<SqlIntercode> value = pair.getValue();

                boolean isCompatible = true;

                for(int j = 0; j < key.size(); j++)
                {
                    Set<ResourceClass> a = key.get(j);
                    Set<ResourceClass> b = newKey.get(j);

                    if(a.stream().noneMatch(x -> b.contains(x)))
                        isCompatible = false;
                }

                if(isCompatible)
                {
                    for(int j = 0; j < newKey.size(); j++)
                        newKey.get(j).addAll(key.get(j));

                    newValue.addAll(value);

                    sorts.remove(i);
                    i = -1;
                }
            }

            sorts.add(new Pair<List<Set<ResourceClass>>, List<SqlIntercode>>(newKey, newValue));
        }

        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        List<SqlIntercode> list = new ArrayList<SqlIntercode>();

        for(Pair<List<Set<ResourceClass>>, List<SqlIntercode>> s : sorts)
        {
            SqlIntercode item = SqlUnion.union(reduceDistinctUnion(s.getValue(), schema));

            if(item instanceof SqlUnion subUnion)
                list.add(expandUnionByConstantColumns(request, subUnion, distinctVariables, restrictions));
            else
                list.add(create(request, item, distinctVariables, restrictions));
        }

        return SqlUnion.union(list);
    }


    private static SqlIntercode expandUnionByConstantColumns(Request request, SqlUnion union,
            Set<String> distinctVariables, Set<String> restrictions)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        Map<SqlIntercode, List<Column>> values = new HashMap<SqlIntercode, List<Column>>();

        for(SqlIntercode child : union.getChilds())
            values.put(child, new ArrayList<Column>());

        for(String varName : distinctVariables)
        {
            UsedVariable var = union.getVariable(varName);

            if(var != null && !var.canBeNull() && var.getClasses().size() == 1)
            {
                ResourceClass rc = var.getResourceClass();
                int[] counts = new int[rc.getColumnCount()];

                for(SqlIntercode child : union.getChilds())
                    if(child.getMapping(varName, rc) != null)
                        for(int i = 0; i < rc.getColumnCount(); i++)
                            if(child.getMapping(varName, rc).get(i) instanceof ConstantColumn)
                                counts[i]++;

                for(int i = 0; i < rc.getColumnCount(); i++)
                    if(counts[i] == union.getChilds().size())
                        for(SqlIntercode child : union.getChilds())
                            values.get(child).add(child.getMapping(varName, rc).get(i));
            }
        }

        Map<List<Column>, List<SqlIntercode>> rev = new HashMap<List<Column>, List<SqlIntercode>>();

        for(Entry<SqlIntercode, List<Column>> e : values.entrySet())
            rev.computeIfAbsent(e.getValue(), k -> new ArrayList<SqlIntercode>()).add(e.getKey());

        List<SqlIntercode> list = new ArrayList<SqlIntercode>();

        for(List<SqlIntercode> l : rev.values())
            list.add(create(request, SqlUnion.union(reduceDistinctUnion(l, schema)), distinctVariables, restrictions));

        return SqlUnion.union(list);
    }


    private static ArrayList<SqlIntercode> reduceDistinctUnion(List<SqlIntercode> childs, DatabaseSchema schema)
    {
        ArrayList<SqlIntercode> optChilds = new ArrayList<SqlIntercode>(childs);

        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlTableAccess left)
            {
                for(int j = 0; j < i; j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        SqlIntercode merged = SqlTableAccess.tryReduceDistinctUnion(schema, left, right);

                        if(merged != null)
                        {
                            optChilds.set(i, merged);
                            optChilds.remove(j);
                            i -= 2;
                            break;
                        }
                    }
                }
            }
        }

        return optChilds;
    }


    public final SqlIntercode getChild()
    {
        return child;
    }
}
