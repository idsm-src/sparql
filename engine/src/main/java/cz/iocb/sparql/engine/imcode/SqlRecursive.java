package cz.iocb.sparql.engine.imcode;

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
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlRecursive extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    public final SqlIntercode init;
    public final SqlIntercode next;
    public final VariableBinding endBinding;
    public final Variable joinVar;
    public final Variable beginVar;
    public final List<Column> iv;
    public final List<Column> ev;
    public final Variable graphVar;


    protected SqlRecursive(VariableBindings bindings, SqlIntercode init, SqlIntercode next, VariableBinding endBinding,
            Variable joinVar, Variable beginVar, List<Column> iv, List<Column> ev, Variable graphVar)
    {
        super(bindings, init.isDeterministic() && next.isDeterministic());
        this.init = init;
        this.next = next;
        this.endBinding = endBinding;
        this.joinVar = joinVar;
        this.beginVar = beginVar;
        this.iv = iv;
        this.ev = ev;
        this.graphVar = graphVar;
    }


    public static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar)
    {
        return create(request, init, next, beginVar, joinVar, endVar, graphVar, null);
    }


    protected static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, Restrictions restrictions)
    {
        //TODO: accept also variables other than those directly participating in recursion

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : init.getVariableBindings().getValues())
        {
            if(binding.getVariable().equals(beginVar) || binding.getVariable().equals(graphVar))
            {
                VariableBinding v = new VariableBinding(binding.getVariable(), binding.canBeNull());

                for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
                {
                    List<Column> safeNames = e.getKey().createColumns(request.getColumnMap(), v.getVariable());

                    v.addMapping(e.getKey(), IntStream.range(0, e.getValue().size()).mapToObj(i -> {
                        Column c = e.getValue().get(i);

                        if(c instanceof ConstantColumn)
                            return c;
                        else
                            return map.computeIfAbsent(c, _ -> safeNames.get(i));
                    }).toList());
                }

                bindings.add(v);
            }
        }

        VariableBinding endBinding = createEndVariableBinding(request, endVar, init, next);

        if(endBinding != null)
            bindings.add(endBinding);


        VariableBindings tmp = new VariableBindings();

        if(init.getVariableBindings().get(beginVar) != null)
            tmp.add(init.getVariableBindings().get(beginVar));

        if(init.getVariableBindings().get(graphVar) != null)
            tmp.add(init.getVariableBindings().get(graphVar));

        List<Column> innerColumns = new ArrayList<>(tmp.getNonConstantColumns());
        List<Column> outerColumns = innerColumns.stream().map(c -> map.get(c)).toList();

        return new SqlRecursive(bindings.restrict(restrictions), init, next, endBinding, joinVar, beginVar,
                innerColumns, outerColumns, graphVar);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        Restrictions childRestrictions = new Restrictions(restrictions);

        childRestrictions.add(endBinding.getVariable());
        childRestrictions.add(joinVar); //TODO: set appropriate resource classes

        if(graphVar != null)
            childRestrictions.add(graphVar);

        if(beginVar != null)
            childRestrictions.add(beginVar);

        Variable endVar = endBinding.getVariable();

        SqlIntercode initOpt = init.optimize(request, childRestrictions, reduced, evalServices);
        SqlIntercode nextOpt = next.optimize(request, childRestrictions, reduced, evalServices);


        if(initOpt.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(beginVar != null && initOpt instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, Set.of(beginVar));

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<>();

                for(SqlIntercode child : segs)
                    childs.add(create(request, child, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                            restrictions));

                return SqlUnion.union(request, childs).optimize(request, restrictions, true, evalServices);
            }
        }

        while(nextOpt instanceof SqlUnion union)
        {
            VariableBinding endBinding = createEndVariableBinding(request, endVar, initOpt, nextOpt);

            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                if((new VariableBindingPair(endBinding, child.getVariableBindings().get(joinVar))).isJoinable())
                    childs.add(child);

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs).optimize(request, childRestrictions, true, evalServices);
        }


        while(nextOpt instanceof SqlUnion union)
        {
            Set<ResourceClass> classes = new HashSet<>(initOpt.getVariable(endVar).getClasses());

            while(true)
            {
                Set<ResourceClass> additional = new HashSet<>(classes);

                for(SqlIntercode c : union.getChilds())
                    if(c.getVariable(joinVar) != null && !areDisjunct(classes, c.getVariable(joinVar).getClasses()))
                        additional.addAll(c.getVariable(endVar).getClasses());

                if(additional.equals(classes))
                    break;

                classes.addAll(additional);
            }

            List<SqlIntercode> childs = union.getChilds().stream().filter(
                    c -> c.getVariable(joinVar) != null && !areDisjunct(classes, c.getVariable(joinVar).getClasses()))
                    .toList();

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs);
        }


        if(nextOpt.equals(SqlNoSolution.get()))
            return SqlDistinct.create(request, initOpt, initOpt.getVariableBindings().getVariables());

        if(!(new VariableBindingPair(initOpt.getVariableBindings().get(endVar),
                nextOpt.getVariableBindings().get(joinVar))).isJoinable())
            return SqlDistinct.create(request, initOpt, initOpt.getVariableBindings().getVariables());


        if(initOpt == init && nextOpt == next && restrictions.isOptimized(bindings))
            return this;

        return create(request, initOpt, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        List<ResourceClass> endVarClasses = new LinkedList<>(endBinding.getClasses()); // to stable order

        StringBuilder builder = new StringBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(ev.stream().map(Object::toString).collect(joining(", ")));

        boolean hasRecursionVariable = !ev.isEmpty();

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = endBinding.getMapping(resClass);

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

        VariableBinding initEndBinding = init.getVariableBindings().get(endBinding.getVariable());

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = initEndBinding.deriveMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endBinding.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasInitSelect);
                hasInitSelect = true;

                builder.append(columns.get(j));
                builder.append(" AS ");
                builder.append(endBinding.getMapping(resClass).get(j));
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

        VariableBinding nextEndBinding = next.getVariableBindings().get(endBinding.getVariable());

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = nextEndBinding != null ? nextEndBinding.deriveMapping(resClass) : null;

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endBinding.getMapping(resClass).get(j) instanceof ConstantColumn)
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

        VariableBindings tmp = new VariableBindings(bindings);
        tmp.remove(endBinding.getVariable());
        tmp.add(new VariableBinding(joinVar, endBinding.getMappings(), endBinding.canBeNull()));

        String condition = generateJoinCondition(tmp, next.getVariableBindings(), leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(") SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM recursion");

        return builder.toString();
    }


    private static VariableBinding createEndVariableBinding(Request request, Variable endVar, SqlIntercode init,
            SqlIntercode next)
    {
        VariableBinding initEndBinding = init.getVariableBindings().get(endVar);
        VariableBinding nextEndBinding = next.getVariableBindings().get(endVar);

        Set<ResourceClass> resClasses = new HashSet<>();

        if(initEndBinding != null)
            resClasses.addAll(initEndBinding.getClasses());

        if(nextEndBinding != null)
            resClasses.addAll(nextEndBinding.getClasses());

        VariableBinding endBinding = new VariableBinding(endVar, false);

        for(ResourceClass resClass : ResourceClass.getDisjunctClasses(resClasses))
            endBinding.addMapping(resClass, resClass.createColumns(request.getColumnMap(), endVar));

        //TODO: handle constant columns

        return endBinding;
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

        if(!Objects.equals(graphVar, imcode.graphVar))
            return false;

        if(!Objects.equals(beginVar, imcode.beginVar))
            return false;

        if(!Objects.equals(joinVar, imcode.joinVar))
            return false;

        if(!Objects.equals(endBinding.getVariable(), imcode.endBinding.getVariable()))
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
        return Objects.hash(graphVar, beginVar, joinVar, endBinding, init, next);
    }
}
