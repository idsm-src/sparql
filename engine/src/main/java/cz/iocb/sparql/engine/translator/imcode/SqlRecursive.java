package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlRecursive extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    public final SqlIntercode init;
    public final SqlIntercode next;
    public final UsedVariable endVar;
    public final String joinName;
    public final String beginName;
    public final List<Column> iv;
    public final List<Column> ev;
    public final String graphName;


    protected SqlRecursive(UsedVariables variables, SqlIntercode init, SqlIntercode next, UsedVariable endVar,
            String joinName, String beginName, List<Column> iv, List<Column> ev, String graphName)
    {
        super(variables, init.isDeterministic() && next.isDeterministic());
        this.init = init;
        this.next = next;
        this.endVar = endVar;
        this.joinName = joinName;
        this.beginName = beginName;
        this.iv = iv;
        this.ev = ev;
        this.graphName = graphName;
    }


    public static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, String beginName,
            String joinName, String endName, String graphName)
    {
        return create(request, init, next, beginName, joinName, endName, graphName, null);
    }


    protected static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, String beginName,
            String joinName, String endName, String graphName, Restrictions restrictions)
    {
        Map<Column, Column> map = new HashMap<>();
        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : init.getVariables().getValues())
        {
            if(var.getName().equals(beginName) || var.getName().equals(graphName))
            {
                UsedVariable v = new UsedVariable(var.getName(), var.canBeNull());

                for(Entry<ResourceClass, List<Column>> e : var.getMappings().entrySet())
                {
                    List<Column> safeNames = e.getKey().createColumns(request.getColumnMap(), v.getName());

                    v.addMapping(e.getKey(), IntStream.range(0, e.getValue().size()).mapToObj(i -> {
                        Column c = e.getValue().get(i);

                        if(c instanceof ConstantColumn)
                            return c;
                        else
                            return map.computeIfAbsent(c, k -> safeNames.get(i));
                    }).toList());
                }

                variables.add(v);
            }
        }

        UsedVariable endVar = createEndVar(request, endName, init, next);

        if(endVar != null)
            variables.add(endVar);


        UsedVariables tmp = new UsedVariables(init.getVariables());
        tmp.remove(endName);

        List<Column> innerColumns = new ArrayList<>(tmp.getNonConstantColumns());
        List<Column> outerColumns = innerColumns.stream().map(c -> map.get(c)).toList();


        return new SqlRecursive(variables.restrict(restrictions), init, next, endVar, joinName, beginName, innerColumns,
                outerColumns, graphName);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        Restrictions childRestrictions = new Restrictions(restrictions);

        childRestrictions.add(endVar.getName());
        childRestrictions.add(joinName); //TODO: set appropriate resource classes

        if(graphName != null)
            childRestrictions.add(graphName);

        if(beginName != null)
            childRestrictions.add(beginName);

        String endName = endVar.getName();

        SqlIntercode initOpt = init.optimize(request, childRestrictions, reduced, evalServices);
        SqlIntercode nextOpt = next.optimize(request, childRestrictions, reduced, evalServices);


        if(initOpt == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(beginName != null && initOpt instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, Set.of(beginName));

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

                for(SqlIntercode child : segs)
                    childs.add(create(request, child, nextOpt, beginName, joinName, endVar.getName(), graphName,
                            restrictions));

                return SqlUnion.union(request, childs).optimize(request, restrictions, true, evalServices);
            }
        }

        while(nextOpt instanceof SqlUnion union)
        {
            UsedVariable endVar = createEndVar(request, endName, initOpt, nextOpt);

            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
                if((new UsedPairedVariable(endVar, child.getVariables().get(joinName))).isJoinable())
                    childs.add(child);

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs).optimize(request, childRestrictions, true, evalServices);
        }


        while(nextOpt instanceof SqlUnion union)
        {
            Set<ResourceClass> classes = new HashSet<>(initOpt.getVariable(endName).getClasses());

            while(true)
            {
                Set<ResourceClass> additional = new HashSet<>(classes);

                for(SqlIntercode c : union.getChilds())
                    if(c.getVariable(joinName) != null && !areDisjunct(classes, c.getVariable(joinName).getClasses()))
                        additional.addAll(c.getVariable(endName).getClasses());

                if(additional.equals(classes))
                    break;

                classes.addAll(additional);
            }

            List<SqlIntercode> childs = union.getChilds().stream().filter(
                    c -> c.getVariable(joinName) != null && !areDisjunct(classes, c.getVariable(joinName).getClasses()))
                    .toList();

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs);
        }


        if(nextOpt == SqlNoSolution.get())
            return SqlDistinct.create(request, initOpt, initOpt.getVariables().getNames());

        if(!(new UsedPairedVariable(initOpt.getVariables().get(endName), nextOpt.getVariables().get(joinName)))
                .isJoinable())
            return SqlDistinct.create(request, initOpt, initOpt.getVariables().getNames());


        if(initOpt == init && nextOpt == next && restrictions.isOptimized(variables))
            return this;

        return create(request, initOpt, nextOpt, beginName, joinName, endVar.getName(), graphName, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        List<ResourceClass> endVarClasses = new LinkedList<ResourceClass>(endVar.getClasses()); // to stable order

        StringBuilder builder = new StringBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(ev.stream().map(Object::toString).collect(joining(", ")));

        boolean hasRecursionVariable = !ev.isEmpty();

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = endVar.getMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(columns.get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasRecursionVariable);
                hasRecursionVariable = true;

                builder.append(columns.get(j));
            }
        }

        if(!hasRecursionVariable)
            builder.append("\"@none\"");

        builder.append(") AS (SELECT ");

        builder.append(iv.stream().map(Object::toString).collect(joining(", ")));

        boolean hasInitSelect = !iv.isEmpty();

        UsedVariable initEndVariable = init.getVariables().get(endVar.getName());

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = initEndVariable.deriveMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endVar.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasInitSelect);
                hasInitSelect = true;

                builder.append(columns.get(j));
                builder.append(" AS ");
                builder.append(endVar.getMapping(resClass).get(j));
            }
        }

        if(!hasInitSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(init.translate(request));
        builder.append(") AS tab");

        builder.append(" UNION SELECT ");

        builder.append(ev.stream().map(c -> c.fromTable(leftTable).toString()).collect(joining(", ")));

        boolean hasUnionSelect = !ev.isEmpty();

        UsedVariable nextEndVariable = next.getVariables().get(endVar.getName());

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = nextEndVariable != null ? nextEndVariable.deriveMapping(resClass) : null;

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endVar.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasUnionSelect);
                hasUnionSelect = true;

                if(columns != null)
                    builder.append(columns.get(j).fromTable(rightTable));
                else
                    builder.append("NULL");
            }
        }

        if(!hasUnionSelect)
            builder.append("1");

        builder.append(" FROM recursion AS ");
        builder.append(leftTable);

        builder.append(", (");
        builder.append(next.translate(request));
        builder.append(") AS ");
        builder.append(rightTable);

        UsedVariables tmp = new UsedVariables(variables);
        tmp.remove(endVar.getName());
        tmp.add(new UsedVariable(joinName, endVar.getMappings(), endVar.canBeNull()));

        String condition = generateJoinCondition(tmp, next.getVariables(), leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(") SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM recursion");

        return builder.toString();
    }


    private static UsedVariable createEndVar(Request request, String endName, SqlIntercode init, SqlIntercode next)
    {
        UsedVariable initEndVar = init.getVariables().get(endName);
        UsedVariable nextEndVar = next.getVariables().get(endName);

        Set<ResourceClass> resClasses = new HashSet<ResourceClass>();

        if(initEndVar != null)
            resClasses.addAll(initEndVar.getClasses());

        if(nextEndVar != null)
            resClasses.addAll(nextEndVar.getClasses());

        UsedVariable endVar = new UsedVariable(endName, false);

        for(ResourceClass resClass : ResourceClass.getDisjunctClasses(resClasses))
            endVar.addMapping(resClass, resClass.createColumns(request.getColumnMap(), endName));

        //TODO: handle constant columns

        return endVar;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return init.hasServiceSubpattern() || next.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("recurse");

        indentChild(builder, indent, false);
        init.generateExplanation(builder, getIndent(indent, false));

        indentChild(builder, indent, true);
        next.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlRecursive imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(graphName, imcode.graphName))
            return false;

        if(!Objects.equals(beginName, imcode.beginName))
            return false;

        if(!Objects.equals(joinName, imcode.joinName))
            return false;

        if(!Objects.equals(endVar.getName(), imcode.endVar.getName()))
            return false;

        if(!Objects.equals(init, imcode.init))
            return false;

        if(!Objects.equals(next, imcode.next))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(graphName, beginName, joinName, endVar, init, next);
    }
}
