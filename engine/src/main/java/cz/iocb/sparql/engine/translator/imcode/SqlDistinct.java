package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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



public final class SqlDistinct extends SqlIntercode
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
            Restrictions restrictions)
    {
        var variables = child.getVariables().restrict(new Restrictions(distinctVariables)).restrict(restrictions);
        return new SqlDistinct(variables, child, distinctVariables);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child.optimize(request, new Restrictions(distinctVariables), true, evalServices);


        if(optChild == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(optChild == SqlEmptySolution.get())
            return SqlEmptySolution.get();

        if(optChild.isDistinct(request, distinctVariables))
            return optChild.optimize(request, restrictions, true, evalServices);

        if(optChild instanceof SqlUnion union)
        {
            DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();
            optChild = reduceDistinctUnion(request, union, schema);
        }

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = expandUnionByResourceClasses(request, union, distinctVariables);

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

                for(SqlIntercode child : segs)
                    childs.add(create(request, child, distinctVariables, restrictions));

                return SqlUnion.union(request, childs).optimize(request, restrictions, true, evalServices);
            }
        }

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
                return join.optimize(request, restrictions, true, evalServices);
        }


        if(optChild == child && restrictions.isOptimized(variables))
            return this;

        return create(request, optChild, distinctVariables, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        SqlIntercode child = this.child;
        Restrictions restrictions = new Restrictions(distinctVariables);
        UsedVariables vars = child.getVariables().restrict(restrictions);

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

        Set<Column> groupColumns = child.getVariables().restrict(restrictions).getNonConstantColumns();

        Set<Column> hashColumns = new HashSet<Column>();

        for(String var : stringLiterals)
            hashColumns.addAll(child.getVariables().get("#hash_" + var).getNonConstantColumns());

        builder.append(hashColumns.stream().map(Object::toString).collect(joining(", ")));

        if(!hashColumns.isEmpty() && !groupColumns.isEmpty())
            builder.append(", ");

        if(!groupColumns.isEmpty())
            builder.append(groupColumns.stream().map(Object::toString).collect(joining(", ")));
        else if(hashColumns.isEmpty())
            builder.append("1");

        return builder.toString();
    }


    protected static List<SqlIntercode> expandUnionByResourceClasses(Request request, SqlUnion union,
            Set<String> distinctVariables)
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

        List<SqlIntercode> list = new ArrayList<SqlIntercode>();

        for(Pair<List<Set<ResourceClass>>, List<SqlIntercode>> s : sorts)
        {
            SqlIntercode item = SqlUnion.union(request, s.getValue());

            if(item instanceof SqlUnion subUnion)
                list.addAll(expandUnionByConstantColumns(request, subUnion, distinctVariables));
            else
                list.add(item);
        }

        return list;
    }


    private static List<SqlIntercode> expandUnionByConstantColumns(Request request, SqlUnion union,
            Set<String> distinctVariables)
    {
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
            list.add(SqlUnion.union(request, l));

        return list;
    }


    private static SqlIntercode reduceDistinctUnion(Request request, SqlUnion union, DatabaseSchema schema)
    {
        ArrayList<SqlIntercode> optChilds = new ArrayList<SqlIntercode>(union.getChilds());

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

        if(optChilds.size() == union.getChilds().size())
            return union;

        return SqlUnion.union(request, optChilds);
    }


    public final SqlIntercode getChild()
    {
        return child;
    }


    @Override
    public boolean isDistinct(Request request, Collection<String> selected)
    {
        return selected.containsAll(distinctVariables);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("distinct");
        builder.append(distinctVariables.stream().collect(joining(" ", " ", "")));

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlDistinct imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(distinctVariables, imcode.distinctVariables))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(distinctVariables, child);
    }
}
