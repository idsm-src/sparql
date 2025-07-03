package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlRecursive extends SqlIntercode
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
            String joinName, String endName, String graphName, Set<String> restrictions)
    {
        if(init == SqlNoSolution.get())
            return SqlNoSolution.get();

        while(next instanceof SqlUnion union)
        {
            UsedVariable endVar = createEndVar(request, endName, init, next);

            List<SqlIntercode> childs = union.getChilds().stream()
                    .filter(c -> (new UsedPairedVariable(endVar, c.getVariables().get(joinName))).isJoinable())
                    .toList();

            if(childs.size() == union.getChilds().size())
                break;

            next = SqlUnion.union(request, childs);
        }

        while(next instanceof SqlUnion union)
        {
            Set<ResourceClass> classes = new HashSet<>(init.getVariable(endName).getClasses());

            while(true)
            {
                Set<ResourceClass> additional = new HashSet<>(classes);

                for(SqlIntercode child : union.getChilds())
                    if(child.getVariable(joinName).getClasses().stream()
                            .anyMatch(c -> classes.contains(c) || classes.contains(c.getGeneralClass())))
                        additional.addAll(child.getVariable(endName).getClasses());

                if(additional.equals(classes))
                    break;

                classes.addAll(additional);
            }

            List<SqlIntercode> childs = union.getChilds().stream().filter(x -> x.getVariable(joinName).getClasses()
                    .stream().anyMatch(c -> classes.contains(c) || classes.contains(c.getGeneralClass()))).toList();

            if(childs.size() == union.getChilds().size())
                break;

            next = SqlUnion.union(request, childs);
        }

        if(next == SqlNoSolution.get())
            return SqlDistinct.create(request, init,
                    Stream.of(beginName, endName, graphName).filter(n -> n != null).collect(toSet()));

        if(!(new UsedPairedVariable(init.getVariables().get(endName), next.getVariables().get(joinName))).isJoinable())
            return SqlDistinct.create(request, init,
                    Stream.of(beginName, endName, graphName).filter(n -> n != null).collect(toSet()));


        /* standard recursion */
        Map<Column, Column> map = new HashMap<>();
        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : init.getVariables().getValues())
        {
            if(!var.getName().equals(endName))
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
        variables.add(endVar);


        UsedVariables tmp = new UsedVariables(init.getVariables());
        tmp.remove(endName);

        List<Column> innerColumns = new ArrayList<>(tmp.getNonConstantColumns());
        List<Column> outerColumns = innerColumns.stream().map(c -> map.get(c)).toList();


        return new SqlRecursive(variables.restrict(restrictions), init, next, endVar, joinName, beginName, innerColumns,
                outerColumns, graphName);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced, boolean evalServices)
    {
        if(restrictions == null)
            return this;

        Set<String> childRestrictions = new HashSet<String>(restrictions);

        childRestrictions.add(endVar.getName());
        childRestrictions.add(joinName);

        if(graphName != null)
            childRestrictions.add(graphName);

        if(beginName != null)
            childRestrictions.add(beginName);

        SqlIntercode initOpt = init.optimize(request, childRestrictions, reduced, evalServices);
        SqlIntercode nextOpt = next.optimize(request, childRestrictions, reduced, evalServices);

        if(beginName != null && initOpt instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, Set.of(beginName));
            return SqlUnion.union(request, segs.stream().map(
                    s -> create(request, s, nextOpt, beginName, joinName, endVar.getName(), graphName, restrictions))
                    .toList());
        }

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
            List<Column> columns = initEndVariable.toResource(resClass);

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
            List<Column> columns = nextEndVariable.toResource(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endVar.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasUnionSelect);
                hasUnionSelect = true;

                builder.append(columns.get(j).fromTable(rightTable));
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
        UsedVariable endVar = new UsedVariable(endName, false);

        //TODO: handle constant columns

        Set<ResourceClass> initEndClasses = init.getVariables().get(endName).getClasses();
        Set<ResourceClass> nextEndClasses = next.getVariables().get(endName).getClasses();

        for(ResourceClass initClass : initEndClasses)
        {
            if(nextEndClasses.contains(initClass))
                endVar.addMapping(initClass, initClass.createColumns(request.getColumnMap(), endName));
            else if(nextEndClasses.contains(initClass.getGeneralClass()))
                endVar.addMapping(initClass.getGeneralClass(),
                        initClass.getGeneralClass().createColumns(request.getColumnMap(), endName));
            else if(nextEndClasses.stream().noneMatch(r -> r.getGeneralClass() == initClass))
                endVar.addMapping(initClass, initClass.createColumns(request.getColumnMap(), endName));
        }

        for(ResourceClass nextClass : nextEndClasses)
        {
            if(initEndClasses.contains(nextClass))
                continue;
            else if(initEndClasses.contains(nextClass.getGeneralClass()))
                endVar.addMapping(nextClass.getGeneralClass(),
                        nextClass.getGeneralClass().createColumns(request.getColumnMap(), endName));
            else if(initEndClasses.stream().noneMatch(r -> r.getGeneralClass() == nextClass))
                endVar.addMapping(nextClass, nextClass.createColumns(request.getColumnMap(), endName));
        }

        return endVar;
    }
}
